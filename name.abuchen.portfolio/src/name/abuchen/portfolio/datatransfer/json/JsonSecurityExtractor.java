package name.abuchen.portfolio.datatransfer.json;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.datatransfer.SecurityCache;
import name.abuchen.portfolio.datatransfer.actions.InsertAction;
import name.abuchen.portfolio.datatransfer.ImportAction.Context;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.json.impl.LocalDateSerializer;
import name.abuchen.portfolio.json.impl.LocalTimeSerializer;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.SecurityProperty.Type;

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
        try
        {
            for (JSecurityMetaData jSecurityMetaData : parseJson(file.getFile().toPath()))
            {
                String isinToImport = jSecurityMetaData.getIsin();
                if (!Strings.isNullOrEmpty(isinToImport))
                {
                    Security s = securityCache.lookup(isinToImport, "", "", "", () -> {
                        Security newSecurity = new Security();
                        newSecurity.setCurrencyCode(client.getBaseCurrency());
                        newSecurity.setIsin(isinToImport);
                        return newSecurity;
                    });

                    result.add(new Extractor.SecurityItem(s) {
                        @Override
                        public Status apply(ImportAction action, Context context)
                        {
                            if (action instanceof InsertAction) {
                                importSecurityMetaData(jSecurityMetaData, getSecurity(), client.getTaxonomies());                                
                            }
                            return super.apply(action, context);
                        }
                    });
                }
            }
        }
        catch (IOException e)
        {
            errors.add(e);
        }
        return result;
    }

    public void importSecurityMetaData(JSecurityMetaData jSecurity, Security security, List<Taxonomy> taxonomies)
    {
        security.setIsin(jSecurity.getIsin());
        security.setName(jSecurity.getName());
        security.setOnlineId(jSecurity.getOnlineId());
        security.setCurrencyCode(jSecurity.getCurrencyCode());
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

    public List<JSecurityMetaData> parseJson(Path file) throws IOException
    {
        String json = Files.readString(file, StandardCharsets.UTF_8);
        JSecurities fromJson = GSON.fromJson(json, JSecurities.class);
        if (fromJson.getVersion().equals(JSecurities.VERSION_1_0)
                        && fromJson.getType().endsWith(JSecurities.SECURITY_META_DATA))
        {
            return fromJson.getSecurities();
        }
        else
        {
            PortfolioLog.error("Wrong file version or file type."); //$NON-NLS-1$
        }
        return Collections.emptyList();
    }
}
