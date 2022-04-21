package name.abuchen.portfolio.datatransfer.json;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.Strings;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.datatransfer.ImportAction.Context;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.SecurityCache;
import name.abuchen.portfolio.datatransfer.actions.InsertAction;
import name.abuchen.portfolio.json.impl.LocalDateSerializer;
import name.abuchen.portfolio.json.impl.LocalTimeSerializer;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.model.SecurityProperty.Type;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.Taxonomy.AssignmentVisitor;
import name.abuchen.portfolio.util.TradeCalendarManager;

public class JsonSecurityExtractor implements Extractor
{

    private static final Gson GSON = new GsonBuilder() //
                    .registerTypeAdapter(LocalDate.class, new LocalDateSerializer())
                    .registerTypeAdapter(LocalTime.class, new LocalTimeSerializer())
                    .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY).setPrettyPrinting().create();

    private Client client;

    public JsonSecurityExtractor(Client client)
    {
        this.client = client;
    }

    public Client getClient()
    {
        return client;
    }

    @Override
    public String getLabel()
    {
        return "Json Security Extractor";
    }

    @Override
    public List<Item> extract(SecurityCache securityCache, InputFile file, List<Exception> errors)
    {
        List<Item> result = new ArrayList<>();
        FileReader fileReader;
        try
        {
            fileReader = new FileReader(file.getFile(), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            errors.add(e);
            return result;
        }
        for (JSecurityMetaData jSecurityMetaData : parseJson(fileReader, errors))
        {
            String isinToImport = jSecurityMetaData.getIsin();
            String nameToImport = jSecurityMetaData.getName();
            if (!Strings.isNullOrEmpty(isinToImport) && !Strings.isNullOrEmpty(nameToImport))
            {
                Security s = securityCache.lookup(isinToImport, null, null, nameToImport, () -> {
                    PortfolioLog.info(MessageFormat.format(
                                    "Create new security ''{0}'' with ISIN ''{1}''.", //$NON-NLS-1$
                                    nameToImport, isinToImport));
                    Security newSecurity = new Security();
                    newSecurity.setCurrencyCode(client.getBaseCurrency());
                    return newSecurity;
                });

                result.add(new Extractor.SecurityItem(s)
                {
                    @Override
                    public Status apply(ImportAction action, Context context)
                    {
                        if (action instanceof InsertAction)
                        {
                            importSecurityMetaData(jSecurityMetaData, getSecurity(), client.getTaxonomies());
                        }
                        return super.apply(action, context);
                    }
                });
            }
        }
        return result;
    }

    public void importSecurityMetaData(JSecurityMetaData jSecurity, Security security, List<Taxonomy> taxonomies)
    {
        if (jSecurity.getIsin() != null)
            security.setIsin(trim(jSecurity.getIsin()));
        if (!Strings.isNullOrEmpty(trim(jSecurity.getName())))
            security.setName(trim(jSecurity.getName()));
        if (jSecurity.getOnlineId() != null)
            security.setOnlineId(trim(jSecurity.getOnlineId()));
        if (jSecurity.getCurrencyCode() != null)
            security.setCurrencyCode(trim(jSecurity.getCurrencyCode()));
        if (jSecurity.getTargetCurrencyCode() != null)
            security.setTargetCurrencyCode(trim(jSecurity.getTargetCurrencyCode()));
        if (jSecurity.getNote() != null)
            security.setNote(jSecurity.getNote());
        if (jSecurity.getTickerSymbol() != null)
            security.setTickerSymbol(trim(jSecurity.getTickerSymbol()));
        if (jSecurity.getWkn() != null)
            security.setWkn(trim(jSecurity.getWkn()));
        if (jSecurity.getCalendar() != null)
        {
            String calendarCode = trim(jSecurity.getCalendar());
            if (TradeCalendarManager.getInstance(calendarCode) != null || Strings.isNullOrEmpty(calendarCode))
                security.setCalendar(calendarCode);
            else
                PortfolioLog.warning(MessageFormat.format("Unknown calendar code ''{0}''", //$NON-NLS-1$
                                calendarCode));
        }
        if (jSecurity.getFeed() != null)
            security.setFeed(trim(jSecurity.getFeed()));
        if (jSecurity.getFeedURL() != null)
            security.setFeedURL(trim(jSecurity.getFeedURL()));
        if (jSecurity.getLatestFeed() != null)
            security.setLatestFeed(trim(jSecurity.getLatestFeed()));
        if (jSecurity.getLatestFeedURL() != null)
            security.setLatestFeedURL(trim(jSecurity.getLatestFeedURL()));
        if (jSecurity.getIsActive() != null)
            security.setRetired(! jSecurity.getIsActive());
        
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
            Optional<Taxonomy> existingTaxonomy = findTaxonomy(taxonomies, jTaxonomy.getKey(), jTaxonomy.getName());
            if (existingTaxonomy.isPresent())
            {
                Taxonomy taxonomy = existingTaxonomy.get();

                // Clear old assignments
                taxonomy.foreach(new AssignmentVisitor((c, a) -> {
                    if (a.getInvestmentVehicle().equals(security))
                        c.removeAssignment(a);
                }));
                // Add new assignments
                List<JTaxonomyAssignment> jAssignments = jTaxonomy.getAssignments();
                fixWeights(jAssignments);
                for (JTaxonomyAssignment jAssignment : jAssignments)
                {
                    String key = jAssignment.getKey();
                    String name = jAssignment.getName();
                    Optional<Classification> existingClassification = findClassification(taxonomy, key, name);
                    if (existingClassification.isPresent())
                    {
                        existingClassification.get()
                                        .addAssignment(new Assignment(security, jAssignment.getWeightValue()));
                    }
                    else
                    {
                        PortfolioLog.warning(MessageFormat.format(
                                        "Classification with key ''{0}'' and name ''{1}'' not found.", //$NON-NLS-1$
                                        key, name));
                    }
                }
            }
            else
            {
                PortfolioLog.warning(MessageFormat.format("Taxonomy with key ''{0}'' name ''{1}'' not found.", //$NON-NLS-1$
                                jTaxonomy.getKey(), jTaxonomy.getName()));
            }
        }
    }

    private void fixWeights(List<JTaxonomyAssignment> assignments)
    {
        if (assignments.isEmpty())
            return;

        // Only one assignment => 100%
        if (assignments.size() == 1)
        {
            assignments.get(0).setWeight(100.0);
            return;
        }

        // All assignments without weight => equal weights
        if (assignments.stream().allMatch(a -> a.getWeight() == 0))
        {
            double weight = 100.0 / assignments.size();
            assignments.stream().forEach(a -> a.setWeight(weight));
            return;
        }

        // Some assignments with zero or negative weight => remove
        assignments.removeIf(a -> a.getWeight() <= 0.01);

        // sum of weights not 100% => adjust with factor
        Integer weightSum = assignments.stream().map(JTaxonomyAssignment::getWeightValue).reduce(0, Integer::sum);
        if (weightSum != Classification.ONE_HUNDRED_PERCENT)
        {
            double factor = (double) Classification.ONE_HUNDRED_PERCENT / (double) weightSum;
            assignments.stream().forEach(a -> a.setWeight(a.getWeight() * factor));
        }
    }

    private Optional<Taxonomy> findTaxonomy(List<Taxonomy> allTaxonomies, String key, String name)
    {
        // Find by key
        if (!Strings.isNullOrEmpty(key))
        {
            Optional<Taxonomy> taxonomyByKey = allTaxonomies.stream().filter(t -> t.getKey().equals(key)).findAny();
            if (taxonomyByKey.isPresent())
                return taxonomyByKey;
        }

        // Find by name
        if (!Strings.isNullOrEmpty(name))
            return allTaxonomies.stream().filter(t -> t.getName().equals(name)).findAny();

        return Optional.empty();
    }

    private Optional<Classification> findClassification(Taxonomy taxonomy, String key, String name)
    {
        // Find by key
        if (!Strings.isNullOrEmpty(key))
        {
            Classification classification = taxonomy.getClassificationByKey(key);
            if (classification != null)
                return Optional.of(classification);
        }

        // Find by name
        if (!Strings.isNullOrEmpty(name))
            return taxonomy.getAllClassifications().stream().filter(c -> c.getName().equals(name)).findAny();

        return Optional.empty();
    }

    public List<JSecurityMetaData> parseJson(Reader file, List<Exception> errors)
    {
        JSecurities fromJson;
        try
        {
            fromJson = GSON.fromJson(file, JSecurities.class);
        }
        catch (JsonSyntaxException | JsonIOException e)
        {
            errors.add(e);
            PortfolioLog.error(e);
            return Collections.emptyList();
        }
        if (fromJson.getVersion().equals(JSecurities.VERSION_1_0)
                        && fromJson.getType().endsWith(JSecurities.SECURITY_META_DATA))
        {
            return fromJson.getSecurities();
        }
        else
        {
            errors.add(new IllegalArgumentException("Wrong file version or file type.")); //$NON-NLS-1$
            PortfolioLog.error("Wrong file version or file type."); //$NON-NLS-1$
        }
        return Collections.emptyList();
    }
}
