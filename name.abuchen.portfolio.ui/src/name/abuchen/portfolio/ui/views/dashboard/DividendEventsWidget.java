package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent.DividendEvent;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.SWTHelper;
import name.abuchen.portfolio.ui.views.SecurityListView;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.TextUtil;

/**
 * List of dividend payments. The data is collected from
 * {@Link AccountTransaction} and {@link DividendEvent}. This widget can be
 * configured by {@link ReportingPeriodConfig}, {@link ClientFilterConfig} and
 * {@link SecurityHoldingsConfig} The list is shortened automatically if longer
 * than MAX_ENTRIES_TO_SHOW
 */
public class DividendEventsWidget extends WidgetDelegate<List<DividendEventsWidget.DividendPaymentEntry>>
{

    private static final int MAX_ENTRIES_TO_SHOW = 10;

    public enum SecurityHoldingsFilter
    {
        ALL(Messages.LabelAllSecurities), HELD(Messages.SecurityFilterSharesHeldNotZero), NOT_HELD(
                        Messages.SecurityFilterSharesHeldEqualZero);

        private String label;

        private SecurityHoldingsFilter(String label)
        {
            this.label = label;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    static class SecurityHoldingsConfig extends EnumBasedConfig<SecurityHoldingsFilter>
    {
        public SecurityHoldingsConfig(WidgetDelegate<?> delegate)
        {
            super(delegate, Messages.SecurityListFilter, SecurityHoldingsFilter.class,
                            Dashboard.Config.SECURITY_HOLDINGS_FILTER, Policy.EXACTLY_ONE);
        }
    }

    static class DividendPaymentEntry implements Comparable<DividendPaymentEntry>
    {
        final String securityName;
        final String securityUuid;
        final LocalDate paydate;
        final long shares;
        final Money amountPerShare;

        DividendPaymentEntry(String securityName, String securityUuid, LocalDate paydate, Money amountPerShare, long shares)
        {
            this.securityName = securityName;
            this.securityUuid = securityUuid;
            this.paydate = paydate;
            this.amountPerShare = amountPerShare;
            this.shares = shares;
        }

        @Override
        public int compareTo(DividendPaymentEntry o)
        {
            int compareTo = paydate.compareTo(o.paydate);
            if (compareTo == 0)
            {
                compareTo = String.CASE_INSENSITIVE_ORDER.compare(securityName, o.securityName);
            }
            return compareTo;
        }
    }

    private Composite container;

    private Label title;

    private Font normalFont;
    private Font boldFont;
    private Font italicFont;
    
    private static final int NO_OF_COLUMNS = 4;
    private Label[] dateLabels;
    private Label[] nameLabels;
    private Label[] sharesLabels;
    private Label[] amoutLabels;

    public DividendEventsWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);

        addConfig(new ReportingPeriodConfig(this));
        addConfig(new ClientFilterConfig(this));
        addConfig(new SecurityHoldingsConfig(this));
    }

    @Override
    public Control getTitleControl()
    {
        return title;
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(NO_OF_COLUMNS).margins(5, 5).spacing(3, 3).applyTo(container);
        container.setBackground(parent.getBackground());

        title = new Label(container, SWT.NONE);
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        title.setBackground(container.getBackground());
        GridDataFactory.fillDefaults().span(NO_OF_COLUMNS, 1).grab(true, false).applyTo(title);

        createTableControls(MAX_ENTRIES_TO_SHOW);
        initFonts();

        return container;
    }

    private void initFonts()
    {
        LocalResourceManager resources = new LocalResourceManager(JFaceResources.getResources(), container);
        normalFont = dateLabels[0].getFont();
        boldFont = resources.createFont(FontDescriptor.createFrom(normalFont).setStyle(SWT.BOLD));
        italicFont = resources.createFont(FontDescriptor.createFrom(normalFont).setStyle(SWT.ITALIC));
    }

    private void createTableControls(int size)
    {
        nameLabels = new Label[size];
        dateLabels = new Label[size];
        sharesLabels = new Label[size];
        amoutLabels = new Label[size];

        for (int ii = 0; ii < size; ii++)
        {
            dateLabels[ii] = new Label(container, SWT.NONE);
            dateLabels[ii].setBackground(container.getBackground());
            nameLabels[ii] = new Label(container, SWT.NONE);
            nameLabels[ii].setBackground(container.getBackground());
            sharesLabels[ii] = new Label(container, SWT.NONE);
            sharesLabels[ii].setBackground(container.getBackground());
            amoutLabels[ii] = new Label(container, SWT.RIGHT);
            amoutLabels[ii].setBackground(container.getBackground());

            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(amoutLabels[ii]);
        }
    }

    @Override
    public Supplier<List<DividendPaymentEntry>> getUpdateTask()
    {
        return () -> {
            ClientFilter clientFilter = get(ClientFilterConfig.class).getSelectedFilter();
            Client filteredClient = clientFilter.filter(getClient());
            Interval interval = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now());
            SecurityHoldingsFilter securityFilter = get(SecurityHoldingsConfig.class).getValue();

            List<DividendPaymentEntry> dividendEntries = new ArrayList<>();

            // DividendEventEntry from AccountTransaction
            filteredClient.getAllTransactions().stream().map(TransactionPair::getTransaction)
                            .filter(AccountTransaction.class::isInstance) //
                            .map(AccountTransaction.class::cast) //
                            .filter(at -> at.getType() == AccountTransaction.Type.DIVIDENDS) //
                            .map(at -> dividendEventEntry(at)) //
                            .forEachOrdered(dividendEntries::add);

            // DividendEventEntry from DividendEvent
            filteredClient.getSecurities().stream()
                            .filter(security -> filterSecurity(filteredClient, security, securityFilter))
                            .flatMap(security -> security.getEvents().stream() //
                                            .filter(DividendEvent.class::isInstance) //
                                            .map(DividendEvent.class::cast)
                                            .filter(de -> de.getPaymentDate() != null
                                                            && interval.contains(de.getPaymentDate()))
                                            .map(de -> dividendEventEntry(security, de)))
                            .filter(de -> ! contains(dividendEntries, de))
                            .forEachOrdered(dividendEntries::add);

            Collections.sort(dividendEntries);
            
            return dividendEntries;
        };
    }
    
    /**
     * Fuzzy check of pay date.
     */
    private boolean contains(List<DividendPaymentEntry> dividendEntries, DividendPaymentEntry newEntry) {
        for (DividendPaymentEntry dividendPaymentEntry : dividendEntries)
        {
            if (dividendPaymentEntry.securityUuid.equals(newEntry.securityUuid)) {
                if (Math.abs(ChronoUnit.DAYS.between(dividendPaymentEntry.paydate, newEntry.paydate)) <= 7) {
                    return true;
                }
            }
        }
        return false;
    }

    private DividendPaymentEntry dividendEventEntry(AccountTransaction at)
    {
        Money amount = at.getMonetaryAmount();
        long shares = at.getShares();
        if (shares > 0) {
            // per share
            amount = Money.of(amount.getCurrencyCode(), (amount.getAmount() * Values.Share.factor()) / shares);
        }
        return new DividendPaymentEntry(at.getSecurity().getName(), at.getSecurity().getUUID(),
                        at.getDateTime().toLocalDate(), amount, shares);
    }

    private DividendPaymentEntry dividendEventEntry(Security security, DividendEvent de)
    {
        return new DividendPaymentEntry(security.getName(), security.getUUID(),
                        de.getPaymentDate(), de.getAmount(), 0L);
    }

    /**
     * Reduce the list to fit into MAX_ENTRIES_TO_SHOW
     */
    private List<DividendPaymentEntry> reduceList(List<DividendPaymentEntry> dividendPayments)
    {
        long entriesBeforeNow = dividendPayments.stream().filter(p -> p.paydate.isBefore(LocalDate.now())).count();
        // Remove from beginning
        while (dividendPayments.size() > MAX_ENTRIES_TO_SHOW && entriesBeforeNow > 3)
        {
            dividendPayments.remove(0);
            entriesBeforeNow--;
        }
        // Remove from end
        if (dividendPayments.size() > MAX_ENTRIES_TO_SHOW)
        {
            dividendPayments = dividendPayments.subList(0, MAX_ENTRIES_TO_SHOW);
        }
        return dividendPayments;
    }

    private boolean filterSecurity(Client client, Security security, SecurityHoldingsFilter securityFilter)
    {
        switch (securityFilter)
        {
            case ALL:
                return true;
            case HELD:
                return getSharesHeld(client, security) > 0;
            case NOT_HELD:
                return getSharesHeld(client, security) == 0;
            default:
                return false;
        }
    }

    /**
     * FIXME: copied from {@link SecurityListView}
     */
    private long getSharesHeld(Client client, Security security)
    {
        // collect all shares and return a value greater 0
        return Math.max(security.getTransactions(client).stream()
                        .filter(t -> t.getTransaction() instanceof PortfolioTransaction) //
                        .map(t -> (PortfolioTransaction) t.getTransaction()) //
                        .mapToLong(t -> {
                            switch (t.getType())
                            {
                                case BUY:
                                case DELIVERY_INBOUND:
                                    return t.getShares();
                                case SELL:
                                case DELIVERY_OUTBOUND:
                                    return -t.getShares();
                                default:
                                    return 0L;
                            }
                        }).sum(), 0);
    }

    @Override
    public void update(List<DividendPaymentEntry> dividendPayments)
    {
        title.setText(TextUtil.tooltip(getWidget().getLabel()));

        dividendPayments = reduceList(dividendPayments);

        for (int ii = 0; ii < MAX_ENTRIES_TO_SHOW; ii++)
        {
            if (ii < dividendPayments.size())
            {
                DividendPaymentEntry dividendEventEntry = dividendPayments.get(ii);

                dateLabels[ii].setText(Values.Date.format(dividendEventEntry.paydate));
                nameLabels[ii].setText(String.format("%.25s", TextUtil.tooltip(dividendEventEntry.securityName))); //$NON-NLS-1$
                String noOfShares = SWTHelper.EMPTY_LABEL;
                if (dividendEventEntry.shares > 0) {
                    noOfShares = Values.Share.format(dividendEventEntry.shares) + " x "; //$NON-NLS-1$
                }
                sharesLabels[ii].setText(noOfShares);
                amoutLabels[ii].setText(Values.Money.format(dividendEventEntry.amountPerShare, getClient().getBaseCurrency()));
                
                dateLabels[ii].setFont(getFontByDate(dividendEventEntry.paydate));
            }
            else
            {
                dateLabels[ii].setText(SWTHelper.EMPTY_LABEL);
                nameLabels[ii].setText(SWTHelper.EMPTY_LABEL);
                sharesLabels[ii].setText(SWTHelper.EMPTY_LABEL);                
                amoutLabels[ii].setText(SWTHelper.EMPTY_LABEL);
                
                dateLabels[ii].setFont(normalFont);
            }
        }
        container.layout(true);
        container.update();
    }

    private Font getFontByDate(LocalDate date) {
        ;
        if (date.isBefore(LocalDate.now())) {
            return italicFont;
        }
        if (date.isEqual(LocalDate.now())) {
            return boldFont;
        }
        return normalFont;
    }
}
