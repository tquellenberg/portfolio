package name.abuchen.portfolio.datatransfer.json;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import name.abuchen.portfolio.json.impl.LocalDateSerializer;
import name.abuchen.portfolio.json.impl.LocalTimeSerializer;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;

public class JsonSecurityExporter
{
    private static final Gson GSON = new GsonBuilder() //
                    .registerTypeAdapter(LocalDate.class, new LocalDateSerializer())
                    .registerTypeAdapter(LocalTime.class, new LocalTimeSerializer())
                    .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY).setPrettyPrinting().create();

    public String exportSecurityMetaData(List<Security> securities, List<Taxonomy> taxonomies)
    {
        JSecurities jSecurities = new JSecurities();
        for (Security security : securities)
        {
            jSecurities.addSecurity(new JSecurityMetaData(security, getClassifications(security, taxonomies)));
        }
        return GSON.toJson(jSecurities);
    }

    private List<JTaxonomy> getClassifications(Security security, List<Taxonomy> taxonomies)
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
                jTaxonomy.addAssignment(new JTaxonomyAssignment(classification.getKey(), classification.getName(), weight));
            }
            if (jTaxonomy.hasAssignment())
            {
                jJTaxonomies.add(jTaxonomy);
            }
        }
        return jJTaxonomies;
    }
}
