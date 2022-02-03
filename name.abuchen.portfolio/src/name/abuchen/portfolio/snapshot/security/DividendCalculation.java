package name.abuchen.portfolio.snapshot.security;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord.Periodicity;
import name.abuchen.portfolio.util.Dates;

/* package */class DividendCalculation extends Calculation
{
    private static final int AMOUNT_FRACTION_TO_QUOTE = BigInteger.TEN
                    .pow(Values.Quote.precision() - Values.AmountFraction.precision()).intValue();

    /**
     * A dividend payment.
     */
    private static class Payment
    {
        /**
         * Amount of the payment.
         */
        public final Money amount;
        /**
         * Date of the payment.
         */
        public final LocalDate date;
        /**
         * Year of the payment.
         */
        public final int year;
        /**
         * Rate of return for this payment.
         */
        public final double rateOfReturn;
        /**
         * Dividend per share.
         */
        public final Quote dividendPerShare;

        /**
         * Constructs an instance.
         *
         * @param converter
         *            currency converter
         * @param t
         *            {@link DividendTransaction}
         * @param security
         *            {@link Security}
         */
        public Payment(CurrencyConverter converter, CalculationLineItem.DividendPayment t, Security security)
        {
            this.amount = t.getGrossValue().with(converter.at(t.getDateTime()));
            LocalDateTime time = t.getDateTime();
            this.year = time.getYear();
            this.date = time.toLocalDate();
            Quote dividendPerShare = Quote.of(t.getGrossValue().getCurrencyCode(),
                            t.getDividendPerShare() * AMOUNT_FRACTION_TO_QUOTE);
            this.dividendPerShare = converter.convert(t.getDateTime().toLocalDate(), dividendPerShare);

            // try to set rate of return, default is NaN
            double rr = Double.NaN;
            if (security != null)
            {
                // calculate the rate of return, but do NOT use the method on
                // the DividendPayment class. Why? The DividendPayment looks
                // only at the payment, but the payment might only be for a
                // partial position (for example if the security is held in
                // multiple accounts). The moving average cost is always the
                // total costs.

                Money movingAverageCost = t.getMovingAverageCost();
                if (movingAverageCost != null && !movingAverageCost.isZero())
                    rr = t.getGrossValueAmount() / (double) movingAverageCost.getAmount();

                // check if it is valid (non 0)
                if (rr == 0)
                {
                    // else use the security price at that date
                    SecurityPrice p = security.getSecurityPrice(date);
                    // getSecurityPrice may return an empty price value, so
                    // check that
                    long pValue = p.getValue();
                    if (pValue != 0)
                    {
                        double sharePriceAmount = ((double) pValue) / Values.Quote.factor()
                                        * Values.AmountFraction.factor();
                        rr = t.getDividendPerShare() / sharePriceAmount;
                    }
                }
            }
            this.rateOfReturn = rr;
        }
    }

    private final List<Payment> payments = new ArrayList<>();
    private Periodicity periodicity;
    private MutableMoney sum;
    private double rateOfReturnPerYear;
    private Quote yearlyDividendPerShare;

    @Override
    public void finish(CurrencyConverter converter, List<CalculationLineItem> lineItems)
    {
        if (payments.isEmpty())
        {
            this.periodicity = Periodicity.NONE;
            this.yearlyDividendPerShare = Quote.of(getTermCurrency(), 0);
            return;
        }

        // first sort
        Collections.sort(payments, (r, l) -> r.date.compareTo(l.date));

        // get first and last payment date
        LocalDate firstPayment = payments.get(0).date;
        LocalDate lastPayment = payments.get(payments.size() - 1).date;

        // calculate total sum of all payments
        double sumRateOfReturn = 0;
        for (Payment p : payments)
        {
            // add to total sum
            sum.add(p.amount);
            sumRateOfReturn += p.rateOfReturn;
        }

        int years = lastPayment.getYear() - firstPayment.getYear() + 1;
        this.rateOfReturnPerYear = sumRateOfReturn / years;

        this.periodicity = determinePeriodicity(firstPayment, lastPayment);

        this.yearlyDividendPerShare = currentYearlyDividendPerShare(converter, lastPayment);
    }

    public Quote getYearlyDividendPerShare()
    {
        return yearlyDividendPerShare;
    }

    /**
     * Sum of all payments per share in the last 12 months.
     */
    private Quote currentYearlyDividendPerShare(CurrencyConverter converter, LocalDate lastPayment)
    {
        // small offset to 365 because payment dates may vary slightly
        LocalDate rangeStart = lastPayment.minusDays(365 - 20);

        long yearlyDividendPerYear = payments.stream() //
                        .filter(p -> p.date.isAfter(rangeStart)) //
                        .mapToLong(q -> q.dividendPerShare.getAmount()) //
                        .sum();
        return Quote.of(getTermCurrency(), yearlyDividendPerYear);
    }

    private Periodicity determinePeriodicity(LocalDate firstPayment, LocalDate lastPayment)
    {
        int significantCount = 0;
        int insignificantYears = 0;

        // now walk through individual years
        for (int year = firstPayment.getYear(); year <= lastPayment.getYear(); year++)
        {
            int countPerYear = 0;
            long sumPerYear = 0;
            LocalDate lastDate = null;

            // first calc sum only for this year
            for (Payment p : payments)
            {
                if (p.year == year)
                {
                    countPerYear++;
                    sumPerYear += p.amount.getAmount();
                }
            }

            // skip years with no dividend payments
            if (countPerYear == 0)
            {
                insignificantYears++;
                continue;
            }

            // calc expected amount for this year
            double expectedAmount = sumPerYear / (double) countPerYear;

            // then calc significance
            for (Payment p : payments)
            {
                if (p.year == year)
                {
                    // check if dividend contributes the expected amount (if
                    // it is not a very small extraordinary payment below 30% of
                    // the expected one)
                    double significance = p.amount.getAmount() / expectedAmount;
                    if (significance > 0.3)
                    {
                        // check, if dividends were recorded for multiple
                        // accounts at the same date
                        if (lastDate == null || !p.date.equals(lastDate))
                        {
                            significantCount++;
                        }
                    }
                    lastDate = p.date;
                }
            }
        }

        if (significantCount <= 0)
            return Periodicity.UNKNOWN;

        // days in current time range
        int days = Dates.daysBetween(firstPayment, lastPayment) - (insignificantYears * 365);
        long daysBetweenPayments = Math.round(days / (double) (significantCount - 1));

        // just check payments inbetween one year
        if (daysBetweenPayments < 430)
        {
            if (daysBetweenPayments > 270)
            {
                return Periodicity.ANNUAL;
            }
            else if (daysBetweenPayments > 130)
            {
                return Periodicity.SEMIANNUAL;
            }
            else if (daysBetweenPayments > 60)
            {
                return Periodicity.QUARTERLY;
            }
            else if (daysBetweenPayments > 20)
            {
                return Periodicity.MONTHLY;
            }
        }
        return Periodicity.UNKNOWN;
    }

    public LocalDate getLastDividendPayment()
    {
        return payments.isEmpty() ? null : payments.get(payments.size() - 1).date;
    }

    public int getNumOfEvents()
    {
        return payments.size();
    }

    public List<Payment> getPayments()
    {
        return payments;
    }

    public Periodicity getPeriodicity()
    {
        return periodicity;
    }

    public double getRateOfReturnPerYear()
    {
        return rateOfReturnPerYear;
    }

    public Money getSum()
    {
        return sum.toMoney();
    }

    @Override
    public void setTermCurrency(String termCurrency)
    {
        super.setTermCurrency(termCurrency);
        this.sum = MutableMoney.of(termCurrency);
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.DividendPayment t)
    {
        // construct new payment and add it to the list
        payments.add(new Payment(converter, t, getSecurity()));
    }
}
