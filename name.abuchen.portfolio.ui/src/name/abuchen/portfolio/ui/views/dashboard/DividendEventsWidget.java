package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent.DividendEvent;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.SWTHelper;
import name.abuchen.portfolio.ui.views.SecurityListView;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.TextUtil;

/**
 * List of {@link DividendEvent}
 * This widget can be configured by {@link ReportingPeriodConfig}, {@link ClientFilterConfig} and {@link SecurityHoldingsConfig}
 * The list is shortened automatically if longer than MAX_ENTRIES_TO_SHOW
 *
 */
public class DividendEventsWidget extends WidgetDelegate<List<DividendEventsWidget.DividendEventEntry>>
{

    private static final int MAX_ENTRIES_TO_SHOW = 10;

    public enum SecurityHoldingsFilter
    {
        ALL(Messages.LabelAllSecurities),
        HELD(Messages.SecurityFilterSharesHeldNotZero),
        NOT_HELD(Messages.SecurityFilterSharesHeldEqualZero);

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
            super(delegate, Messages.SecurityListFilter, SecurityHoldingsFilter.class, Dashboard.Config.SECURITY_HOLDINGS_FILTER, Policy.EXACTLY_ONE);
        }
    }

    static class DividendEventEntry implements Comparable<DividendEventEntry>
    {
        Security security;
        DividendEvent dividendEvent;

        DividendEventEntry(Security security, DividendEvent dividendEvent)
        {
            this.security = security;
            this.dividendEvent = dividendEvent;
        }

        @Override
        public int compareTo(DividendEventEntry o)
        {
            int compareTo = dividendEvent.getPaymentDate().compareTo(o.dividendEvent.getPaymentDate());
            if (compareTo == 0)
            {
                compareTo = String.CASE_INSENSITIVE_ORDER.compare(security.getName(), o.security.getName());
            }
            return compareTo;
        }
    }

    private Composite container;

    private Label title;
    private Label[] dateLabels;
    private Label[] nameLabels;
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
        GridLayoutFactory.fillDefaults().numColumns(3).margins(5, 5).spacing(3, 3).applyTo(container);
        container.setBackground(parent.getBackground());

        title = new Label(container, SWT.NONE);
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        title.setBackground(container.getBackground());
        GridDataFactory.fillDefaults().span(3, 1).grab(true, false).applyTo(title);

        createTableControls(MAX_ENTRIES_TO_SHOW);

        return container;
    }

    private void createTableControls(int size)
    {
        nameLabels = new Label[size];
        dateLabels = new Label[size];
        amoutLabels = new Label[size];

        for (int ii = 0; ii < size; ii++)
        {
            dateLabels[ii] = new Label(container, SWT.NONE);
            dateLabels[ii].setBackground(container.getBackground());
            nameLabels[ii] = new Label(container, SWT.NONE);
            nameLabels[ii].setBackground(container.getBackground());
            amoutLabels[ii] = new Label(container, SWT.RIGHT);
            amoutLabels[ii].setBackground(container.getBackground());

            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(amoutLabels[ii]);
        }
    }

    @Override
    public Supplier<List<DividendEventEntry>> getUpdateTask()
    {
        return () -> {
            ClientFilter clientFilter = get(ClientFilterConfig.class).getSelectedFilter();
            Client filteredClient = clientFilter.filter(getClient());
            Interval interval = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now());
            SecurityHoldingsFilter securityFilter = get(SecurityHoldingsConfig.class).getValue();

            List<DividendEventEntry> dividendEntries = filteredClient.getSecurities().stream()
                            .filter(security -> filterSecurity(filteredClient, security, securityFilter))
                            .flatMap(security -> security.getEvents().stream()
                                            .filter(DividendEvent.class::isInstance)
                                            .map(DividendEvent.class::cast)
                                            .filter(de -> de.getPaymentDate() != null && interval.contains(de.getPaymentDate()))
                                            .map(de -> new DividendEventEntry(security, de)))
                            .sorted()
                            .collect(Collectors.toList());

            return dividendEntries;
        };
    }

    /**
     * Reduce the list to fit into MAX_ENTRIES_TO_SHOW
     */
    private List<DividendEventEntry> reduceList(List<DividendEventEntry> dividendPayments)
    {
        long entriesBeforeNow = dividendPayments.stream()
                        .filter(p -> p.dividendEvent.getPaymentDate().isBefore(LocalDate.now()))
                        .count();
        // Remove from beginning
        while (dividendPayments.size() > MAX_ENTRIES_TO_SHOW && entriesBeforeNow > 3)
        {
            dividendPayments.remove(0);
            entriesBeforeNow--;
        }
        // Remove from end
        if (dividendPayments.size() > MAX_ENTRIES_TO_SHOW) {
            dividendPayments = dividendPayments.subList(0, MAX_ENTRIES_TO_SHOW);
        }
        return dividendPayments;
    }
    
    private boolean filterSecurity(Client client, Security security, SecurityHoldingsFilter securityFilter) {
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
     *  FIXME: copied from {@link SecurityListView}
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
    public void update(List<DividendEventEntry> dividendPayments)
    {
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        
        dividendPayments = reduceList(dividendPayments);

        for (int ii = 0; ii < MAX_ENTRIES_TO_SHOW; ii++)
        {
            if (ii < dividendPayments.size())
            {
                Security security = dividendPayments.get(ii).security;
                DividendEvent dividendEvent = dividendPayments.get(ii).dividendEvent;

                LocalDate date = dividendEvent.getPaymentDate();
                dateLabels[ii].setText(Values.Date.format(date));

                String securityName = String.format("%.25s", TextUtil.tooltip(security.getName())); //$NON-NLS-1$
                nameLabels[ii].setText(securityName);

                Money amount = dividendEvent.getAmount();
                amoutLabels[ii].setText(Values.Money.format(amount, getClient().getBaseCurrency()));
            }
            else
            {
                dateLabels[ii].setText(SWTHelper.EMPTY_LABEL);
                nameLabels[ii].setText(SWTHelper.EMPTY_LABEL);
                amoutLabels[ii].setText(SWTHelper.EMPTY_LABEL);
            }
        }
        container.layout(true);
    }

}
