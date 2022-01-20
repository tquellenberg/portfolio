package name.abuchen.portfolio.datatransfer.json;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;

public class JSecurityMetaData
{

    private String uuid;
    private String onlineId;

    private String name;
    private String currencyCode = CurrencyUnit.EUR;
    private String targetCurrencyCode;

    private String note;

    private String isin;
    private String tickerSymbol;
    private String wkn;
    private String calendar;

    private List<JTaxonomyClassification> taxonomies = new ArrayList<>();

    public JSecurityMetaData(String isin, String name)
    {
        this.isin = isin;
        this.name = name;
    }

    public JSecurityMetaData(Security security, List<JTaxonomyClassification> taxonomies)
    {
        this.uuid = security.getUUID();
        this.onlineId = security.getOnlineId();
        this.name = security.getName();
        this.currencyCode = security.getCurrencyCode();
        this.targetCurrencyCode = security.getTargetCurrencyCode();
        this.note = security.getNote();
        this.isin = security.getIsin();
        this.tickerSymbol = security.getTickerSymbol();
        this.wkn = security.getWkn();
        this.calendar = security.getCalendar();
        this.taxonomies.addAll(taxonomies);
    }

    public String getUuid()
    {
        return uuid;
    }

    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }

    public String getOnlineId()
    {
        return onlineId;
    }

    public void setOnlineId(String onlineId)
    {
        this.onlineId = onlineId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getCurrencyCode()
    {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode)
    {
        this.currencyCode = currencyCode;
    }

    public String getTargetCurrencyCode()
    {
        return targetCurrencyCode;
    }

    public void setTargetCurrencyCode(String targetCurrencyCode)
    {
        this.targetCurrencyCode = targetCurrencyCode;
    }

    public String getNote()
    {
        return note;
    }

    public void setNote(String note)
    {
        this.note = note;
    }

    public String getIsin()
    {
        return isin;
    }

    public void setIsin(String isin)
    {
        this.isin = isin;
    }

    public String getTickerSymbol()
    {
        return tickerSymbol;
    }

    public void setTickerSymbol(String tickerSymbol)
    {
        this.tickerSymbol = tickerSymbol;
    }

    public String getWkn()
    {
        return wkn;
    }

    public void setWkn(String wkn)
    {
        this.wkn = wkn;
    }

    public String getCalendar()
    {
        return calendar;
    }

    public void setCalendar(String calendar)
    {
        this.calendar = calendar;
    }

}
