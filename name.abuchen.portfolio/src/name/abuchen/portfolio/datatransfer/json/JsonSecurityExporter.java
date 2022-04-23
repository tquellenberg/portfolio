package name.abuchen.portfolio.datatransfer.json;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import name.abuchen.portfolio.json.impl.LocalDateSerializer;
import name.abuchen.portfolio.json.impl.LocalTimeSerializer;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.ClientSettings;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.model.Taxonomy;

public class JsonSecurityExporter
{
    private static final Gson GSON = new GsonBuilder() //
                    .registerTypeAdapter(LocalDate.class, new LocalDateSerializer())
                    .registerTypeAdapter(LocalTime.class, new LocalTimeSerializer())
                    .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY).setPrettyPrinting().create();

    public String exportSecurityMetaData(List<Security> securities, List<Taxonomy> taxonomies,
                    ClientSettings clientSettings)
    {
        JSecurities jSecurities = new JSecurities();
        for (Security security : securities)
        {
            JSecurityMetaData data = mapSecurity(security);
            data.setAttributes(mapAttributes(security, clientSettings));
            data.setTaxonomies(mapTaxonomy(security, taxonomies));
            jSecurities.addSecurity(data);
        }
        return GSON.toJson(jSecurities);
    }

    private JSecurityMetaData mapSecurity(Security security)
    {
        JSecurityMetaData data = new JSecurityMetaData(security.getIsin(), security.getName());

        data.setOnlineId(security.getOnlineId());
        data.setCurrencyCode(security.getCurrencyCode());
        data.setTargetCurrencyCode(security.getTargetCurrencyCode());
        data.setNote(security.getNote());
        data.setTickerSymbol(security.getTickerSymbol());
        data.setWkn(security.getWkn());
        data.setCalendar(security.getCalendar());
        data.setFeed(security.getFeed());
        data.setFeedURL(security.getFeedURL());
        data.setLatestFeed(security.getLatestFeed());
        data.setLatestFeedURL(security.getLatestFeedURL());
        data.setIsActive(!security.isRetired());

        data.setProperties(mapProperties(security));

        return data;
    }

    private Map<SecurityProperty.Type, Map<String, String>> mapProperties(Security security)
    {
        Map<SecurityProperty.Type, Map<String, String>> properties = new HashMap<>();
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
        return properties;
    }

    private Map<String, String> mapAttributes(Security security, ClientSettings clientSettings)
    {
        Map<String, String> attributes = new HashMap<>();
        for (Map.Entry<String, Object> entry : security.getAttributes().getMap().entrySet())
        {
            String key = entry.getKey();
            Object val = entry.getValue();
            clientSettings.getAttributeTypes().filter(at -> at.getId().equals(key)).findAny()
                            .ifPresent(at -> attributes.put(key, at.getConverter().toString(val)));
        }
        return attributes;
    }

    private List<JTaxonomy> mapTaxonomy(Security security, List<Taxonomy> taxonomies)
    {
        List<JTaxonomy> jJTaxonomies = new ArrayList<>();
        for (Taxonomy taxonomy : taxonomies)
        {
            JTaxonomy jTaxonomy = new JTaxonomy(taxonomy.getKey(), taxonomy.getName());
            for (Classification classification : taxonomy.getClassifications(security))
            {
                Optional<Assignment> assignment = classification.getAssignments().stream() //
                                .filter(a -> a.getInvestmentVehicle().equals(security)) //
                                .findAny();
                int weight = assignment.map(a -> a.getWeight()).orElse(0);
                jTaxonomy.addAssignment(
                                new JTaxonomyAssignment(classification.getKey(), classification.getName(), weight));
            }
            if (jTaxonomy.hasAssignment())
            {
                jJTaxonomies.add(jTaxonomy);
            }
        }
        return jJTaxonomies;
    }
}
