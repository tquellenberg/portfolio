package name.abuchen.portfolio.datatransfer.json;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.SecurityProperty;

public class JSecurityMetaData
{
    private String isin;

    private String name;
    private String onlineId;
    private String currencyCode;

    private String targetCurrencyCode;

    private String note;
    private String tickerSymbol;
    private String wkn;
    private String calendar;

    private String feed;
    private String feedURL;

    private String latestFeed;
    private String latestFeedURL;

    private Map<SecurityProperty.Type, Map<String, String>> properties;
    private Map<String, String> attributes;

    private List<JTaxonomy> taxonomies;

    private Boolean isActive;

    public JSecurityMetaData(String isin, String name)
    {
        this.isin = isin;
        this.name = name;
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

    public List<JTaxonomy> getTaxonomies()
    {
        if (taxonomies == null)
            return Collections.emptyList();
        return taxonomies;
    }

    public void setTaxonomies(List<JTaxonomy> taxonomies)
    {
        this.taxonomies = taxonomies;
    }

    public Map<String, String> getAttributes()
    {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes)
    {
        this.attributes = attributes;
    }

    public String getFeed()
    {
        return feed;
    }

    public void setFeed(String feed)
    {
        this.feed = feed;
    }

    public String getFeedURL()
    {
        return feedURL;
    }

    public void setFeedURL(String feedURL)
    {
        this.feedURL = feedURL;
    }

    public String getLatestFeed()
    {
        return latestFeed;
    }

    public void setLatestFeed(String latestFeed)
    {
        this.latestFeed = latestFeed;
    }

    public String getLatestFeedURL()
    {
        return latestFeedURL;
    }

    public void setLatestFeedURL(String latestFeedURL)
    {
        this.latestFeedURL = latestFeedURL;
    }

    public Map<SecurityProperty.Type, Map<String, String>> getProperties()
    {
        return properties;
    }

    public void setProperties(Map<SecurityProperty.Type, Map<String, String>> properties)
    {
        this.properties = properties;
    }

    public Boolean getIsActive()
    {
        return isActive;
    }

    public void setIsActive(Boolean isActive)
    {
        this.isActive = isActive;
    }
}
