package name.abuchen.portfolio.snapshot.security;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord.Periodicity;

@SuppressWarnings("nls")
public class DividendCalculationTest
{
    Account account;
    Security security;
    CurrencyConverter converter;

    @Before
    public void setup()
    {
        this.account = new Account();
        this.security = new Security("ADIDAS ORD", "EUR");
        this.converter = new TestCurrencyConverter();
    }

    /**
     * Creates a dividend payment of 100$ with 10 Shares -> 10$/Share
     * 
     * @param date
     * @return
     */
    public CalculationLineItem createDividendTransaction(LocalDateTime date)
    {
        return createDividendTransaction(date, 100, 10);
    }

    private CalculationLineItem createDividendTransaction(LocalDateTime date, int amount, int shares)
    {
        return createDividendTransaction(date, amount, shares, security.getCurrencyCode());
    }

    private CalculationLineItem createDividendTransaction(LocalDateTime date, int amount, int shares,
                    String currencyCode)
    {
        AccountTransaction t = new AccountTransaction();
        t.setType(AccountTransaction.Type.DIVIDENDS);
        t.setSecurity(security);
        t.setDateTime(date);
        t.setAmount(Values.Amount.factorize(amount));
        t.setShares(Values.Share.factorize(shares));
        t.setCurrencyCode(currencyCode);

        return CalculationLineItem.of(account, t);
    }

    @Test
    public void noTransactionTest()
    {
        List<CalculationLineItem> transactions = new ArrayList<>();

        DividendCalculation dc = Calculation.perform(DividendCalculation.class, converter, security, transactions);

        assertEquals(0, dc.getNumOfEvents());
        assertEquals(Periodicity.NONE, dc.getPeriodicity());
        assertEquals(0.0, dc.getRateOfReturnPerYear(), 0.0);
    }

    @Test
    public void oneTransactionTest()
    {

        List<CalculationLineItem> transactions = new ArrayList<>();
        transactions.add(createDividendTransaction(LocalDateTime.of(2019, 01, 15, 12, 00)));

        DividendCalculation dividends = Calculation.perform(DividendCalculation.class, converter, security,
                        transactions);

        assertEquals(1, dividends.getNumOfEvents());
        assertEquals(Periodicity.UNKNOWN, dividends.getPeriodicity());
        assertEquals(transactions.get(0).getDateTime().toLocalDate(), dividends.getLastDividendPayment());

    }

    @Test
    public void periodicityAnualTest()
    {

        List<CalculationLineItem> transactions = new ArrayList<>();
        transactions.add(createDividendTransaction(LocalDateTime.of(2019, 01, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2020, 01, 15, 12, 00)));

        DividendCalculation dividends = Calculation.perform(DividendCalculation.class, converter, security,
                        transactions);

        assertEquals(Periodicity.ANNUAL, dividends.getPeriodicity());
    }

    @Test
    public void periodicityAnnualWithGap()
    {

        List<CalculationLineItem> transactions = new ArrayList<>();
        transactions.add(createDividendTransaction(LocalDateTime.of(2015, 01, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2016, 01, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2017, 01, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2018, 01, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2020, 01, 15, 12, 00)));

        DividendCalculation dividends = Calculation.perform(DividendCalculation.class, converter, security,
                        transactions);

        assertEquals(Periodicity.ANNUAL, dividends.getPeriodicity());
    }

    @Test
    public void periodicityTransitionToQuaterly()
    {

        List<CalculationLineItem> transactions = new ArrayList<>();
        transactions.add(createDividendTransaction(LocalDateTime.of(2016, 01, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2017, 01, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2017, 04, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2017, 07, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2017, 10, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2018, 01, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2018, 04, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2018, 07, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2018, 10, 15, 12, 00)));

        DividendCalculation dividends = Calculation.perform(DividendCalculation.class, converter, security,
                        transactions);

        assertEquals(Periodicity.QUARTERLY, dividends.getPeriodicity());
    }

    @Test
    public void periodicitySemiAnnualTest()
    {

        List<CalculationLineItem> transactions = new ArrayList<>();
        transactions.add(createDividendTransaction(LocalDateTime.of(2019, 01, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2019, 07, 15, 12, 00)));

        DividendCalculation dividends = Calculation.perform(DividendCalculation.class, converter, security,
                        transactions);

        assertEquals(Periodicity.SEMIANNUAL, dividends.getPeriodicity());
    }

    @Test
    public void periodicityQuarterlyTest()
    {

        List<CalculationLineItem> transactions = new ArrayList<>();
        transactions.add(createDividendTransaction(LocalDateTime.of(2019, 01, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2019, 04, 15, 12, 00)));

        DividendCalculation dividends = Calculation.perform(DividendCalculation.class, converter, security,
                        transactions);

        assertEquals(Periodicity.QUARTERLY, dividends.getPeriodicity());
    }

    @Test
    public void periodicityMonthlyTest()
    {

        List<CalculationLineItem> transactions = new ArrayList<>();
        transactions.add(createDividendTransaction(LocalDateTime.of(2019, 01, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2019, 02, 15, 12, 00)));

        DividendCalculation dividends = Calculation.perform(DividendCalculation.class, converter, security,
                        transactions);

        assertEquals(Periodicity.MONTHLY, dividends.getPeriodicity());
    }

    @Test
    public void dividendPerShareTest()
    {
        List<CalculationLineItem> transactions = new ArrayList<>();

        transactions.add(createDividendTransaction(LocalDateTime.of(2017, 07, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2017, 10, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2018, 01, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2018, 04, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2018, 07, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2018, 10, 15, 12, 00)));

        DividendCalculation dividends = Calculation.perform(DividendCalculation.class, converter, security,
                        transactions);

        Quote dividendPerShare = dividends.getYearlyDividendPerShare();
        assertEquals(new BigDecimal("40.0"), dividendPerShare.toBigDecimal());
    }

    @Test
    public void dividendPerShareDifferentPositionsTest()
    {
        List<CalculationLineItem> transactions = new ArrayList<>();

        transactions.add(createDividendTransaction(LocalDateTime.of(2017, 07, 15, 12, 00), 1 * 10, 10));
        transactions.add(createDividendTransaction(LocalDateTime.of(2017, 10, 15, 12, 00), 5 * 50, 50));

        transactions.add(createDividendTransaction(LocalDateTime.of(2018, 01, 15, 12, 00), 6 * 50, 50));
        transactions.add(createDividendTransaction(LocalDateTime.of(2018, 04, 15, 12, 00), 7 * 80, 80));
        transactions.add(createDividendTransaction(LocalDateTime.of(2018, 07, 15, 12, 00), 8 * 50, 50));
        transactions.add(createDividendTransaction(LocalDateTime.of(2018, 10, 15, 12, 00), 10 * 100, 100));

        DividendCalculation dividends = Calculation.perform(DividendCalculation.class, converter, security,
                        transactions);

        Quote dividendPerShare = dividends.getYearlyDividendPerShare();
        assertEquals(new BigDecimal("31.0"), dividendPerShare.toBigDecimal());
    }

    @Test
    public void dividendPerShareDifferentCurrencyTest()
    {
        List<CalculationLineItem> transactions = new ArrayList<>();

        transactions.add(createDividendTransaction(LocalDateTime.of(2017, 07, 15, 12, 00), 1 * 10, 10, "USD"));
        transactions.add(createDividendTransaction(LocalDateTime.of(2017, 10, 15, 12, 00), 5 * 50, 50, "USD"));

        transactions.add(createDividendTransaction(LocalDateTime.of(2018, 01, 15, 12, 00), 6 * 50, 50, "USD"));
        transactions.add(createDividendTransaction(LocalDateTime.of(2018, 04, 15, 12, 00), 7 * 80, 80, "USD"));
        transactions.add(createDividendTransaction(LocalDateTime.of(2018, 07, 15, 12, 00), 8 * 50, 50, "USD"));
        transactions.add(createDividendTransaction(LocalDateTime.of(2018, 10, 15, 12, 00), 10 * 100, 100, "USD"));

        DividendCalculation dividends = Calculation.perform(DividendCalculation.class, converter, security,
                        transactions);

        Quote dividendPerShare = dividends.getYearlyDividendPerShare();

        ExchangeRate rate = converter.getRate(LocalDateTime.of(2018, 10, 15, 12, 00), "USD");
        BigDecimal resultInEUR = new BigDecimal("31.0").multiply(rate.getValue());
        assertEquals(resultInEUR.doubleValue(), dividendPerShare.toBigDecimal().doubleValue(), 0.00001);
    }

    @Test
    public void rateOfReturnCalculationTest()
    {

        List<CalculationLineItem> transactions = new ArrayList<>();
        // We buy some shares, 1000$, 10 Shares -> 100/Share

        transactions.add(CalculationLineItem.of(new Portfolio(),
                        new PortfolioTransaction(LocalDateTime.of(2019, 01, 14, 12, 00), security.getCurrencyCode(),
                                        Values.Amount.factorize(1000L), security, Values.Share.factorize(10L), Type.BUY,
                                        0L, 0L)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2019, 01, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2020, 01, 15, 12, 00)));

        // We need to calculate the costs, in order to get the average return
        @SuppressWarnings("unused")
        CostCalculation cost = Calculation.perform(CostCalculation.class, converter, security, transactions);
        DividendCalculation dividends = Calculation.perform(DividendCalculation.class, converter, security,
                        transactions);

        assertEquals(0.1, dividends.getRateOfReturnPerYear(), 0.0);
    }
}
