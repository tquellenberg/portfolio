package name.abuchen.portfolio.datatransfer.json;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.json.impl.LocalDateSerializer;
import name.abuchen.portfolio.json.impl.LocalTimeSerializer;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.model.SecurityProperty.Type;
import name.abuchen.portfolio.model.Taxonomy;

public class SecurityMetaDataTransfer
{

    private static final Gson GSON = new GsonBuilder() //
                    .registerTypeAdapter(LocalDate.class, new LocalDateSerializer())
                    .registerTypeAdapter(LocalTime.class, new LocalTimeSerializer())
                    .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY).setPrettyPrinting().create();

    public String exportSecurityMetaData(Security security, List<Taxonomy> taxonomies)
    {
        JSecurities jSecurities = new JSecurities();
        jSecurities.addSecurity(new JSecurityMetaData(security, getClassifications(security, taxonomies)));
        String json = GSON.toJson(jSecurities);
        System.out.println(json);
        return json;
    }

    private List<JTaxonomy> getClassifications(Security security, List<Taxonomy> taxonomies)
    {
        List<JTaxonomy> jJTaxonomies = new ArrayList<>();
        for (Taxonomy taxonomy : taxonomies)
        {
            JTaxonomy jTaxonomy = new JTaxonomy(taxonomy.getName());
            for (Classification classification : taxonomy.getClassifications(security))
            {
                List<String> path = classification.getPathToRoot().stream().map(c -> c.getName())
                                .collect(Collectors.toList());
                Optional<Assignment> assignment = classification.getAssignments().stream() //
                                .filter(a -> a.getInvestmentVehicle().equals(security)) //
                                .findAny();
                int weight = assignment.map(a -> a.getWeight()).orElse(0);
                jTaxonomy.addAssignment(new JTaxonomyAssignment(classification.getId(), path, weight));
            }
            if (jTaxonomy.hasAssignment())
            {
                jJTaxonomies.add(jTaxonomy);
            }
        }
        return jJTaxonomies;
    }

    public void importSecurityMetaData(String json, Security security, List<Taxonomy> taxonomies)
    {
        JSecurityMetaData jSecurity = GSON.fromJson(json, JSecurities.class).getSecurities().get(0);
        if (security.getIsin() == null)
        {
            security.setIsin(jSecurity.getIsin());
        }
        security.setName(jSecurity.getName());
        security.setOnlineId(jSecurity.getOnlineId());
        security.setTargetCurrencyCode(jSecurity.getTargetCurrencyCode());

        security.setNote(jSecurity.getNote());
        security.setTickerSymbol(jSecurity.getTickerSymbol());
        security.setWkn(jSecurity.getWkn());
        security.setCalendar(jSecurity.getCalendar());

        security.setFeed(jSecurity.getFeed());
        security.setFeedURL(jSecurity.getFeedURL());
        security.setLatestFeed(jSecurity.getLatestFeed());
        security.setLatestFeedURL(jSecurity.getLatestFeedURL());

        Map<Type, Map<String, String>> properties = jSecurity.getProperties();
        if (properties != null)
        {
            for (Map.Entry<SecurityProperty.Type, Map<String, String>> typeEntry : properties.entrySet())
            {
                Type type = typeEntry.getKey();
                for (Map.Entry<String, String> entry : typeEntry.getValue().entrySet())
                {
                    security.addProperty(new SecurityProperty(type, entry.getKey(), entry.getValue()));
                }
            }
        }

        Map<String, Object> attributes = jSecurity.getAttributes();
        if (attributes != null)
        {
            for (Map.Entry<String, Object> entry : attributes.entrySet())
            {
                security.getAttributes().put(new AttributeType(entry.getKey()), entry.getValue());
            }
        }

        for (JTaxonomy jTaxonomy : jSecurity.getTaxonomies())
        {
            Optional<Taxonomy> taxonomyByName = taxonomies.stream().filter(t -> t.getName().equals(jTaxonomy.getName()))
                            .findAny();
            if (taxonomyByName.isPresent())
            {
                Taxonomy taxonomy = taxonomyByName.get();
                for (JTaxonomyAssignment jAssignment : jTaxonomy.getAssignments())
                {
                    String id = jAssignment.getId();
                    List<String> path = jAssignment.getPath();
                    Classification classification = findClassification(taxonomy, id, path);
                    if (classification != null)
                    {
                        classification.addAssignment(new Assignment(security, jAssignment.getWeight()));
                    }
                    else
                    {
                        PortfolioLog.warning(MessageFormat.format(
                                        "Classification with id ''{0}'' and path ''{1}'' not found.", //$NON-NLS-1$
                                        id, String.join(",", path))); //$NON-NLS-1$
                    }
                }
            }
            else
            {
                PortfolioLog.warning(MessageFormat.format("Taxonomy with name ''{0}'' not found.", //$NON-NLS-1$
                                jTaxonomy.getName()));
            }
        }
    }

    private Classification findClassification(Taxonomy taxonomy, String id, List<String> path)
    {
        // Find by id
        if (!Strings.isNullOrEmpty(id))
        {
            Classification classification = taxonomy.getClassificationById(id);
            if (classification != null)
                return classification;
        }

        // Find by path
        if (path != null && path.size() >= 1)
        {
            Classification node = taxonomy.getRoot();
            for (String pathName : path.subList(1, path.size()))
            {
                Optional<Classification> findFirst = node.getChildren().stream() //
                                .filter(c -> c.getName().equals(pathName)) //
                                .findFirst();
                if (findFirst.isPresent())
                {
                    node = findFirst.get();
                }
                else
                {
                    return null;
                }
            }
            return node;
        }

        return null;
    }
}
