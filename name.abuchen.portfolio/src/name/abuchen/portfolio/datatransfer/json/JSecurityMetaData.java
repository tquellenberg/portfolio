package name.abuchen.portfolio.datatransfer.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import name.abuchen.portfolio.model.Security;
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
    private Map<String, Object> attributes;

    private List<JTaxonomy> taxonomies = new ArrayList<>();

    public JSecurityMetaData(String isin, String name)
    {
        this.isin = isin;
        this.name = name;
    }

    public JSecurityMetaData(Security security, List<JTaxonomy> taxonomies)
    {
        this.onlineId = security.getOnlineId();
        this.name = security.getName();
        this.currencyCode = security.getCurrencyCode();
        this.targetCurrencyCode = security.getTargetCurrencyCode();
        this.note = security.getNote();
        this.isin = security.getIsin();
        this.tickerSymbol = security.getTickerSymbol();
        this.wkn = security.getWkn();
        this.calendar = security.getCalendar();
        this.feed = security.getFeed();
        this.feedURL = security.getFeedURL();
        this.latestFeed = security.getLatestFeed();
        this.latestFeedURL = security.getLatestFeedURL();

        this.properties = new HashMap<>();
        for (SecurityProperty.Type type : SecurityProperty.Type.values())
        {
            Map<String, String> map = security.getProperties() //
                            .filter(p -> p.getType() == type) //
                            .collect(Collectors.toMap(SecurityProperty::getName, SecurityProperty::getValue));
            if (!map.isEmpty())
            {
                properties.put(type, map);
            }
        }

        this.attributes = security.getAttributes().getMap();
        this.taxonomies.addAll(taxonomies);
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

    public Map<String, Object> getAttributes()
    {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes)
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
}
