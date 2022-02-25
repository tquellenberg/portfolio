package name.abuchen.portfolio.datatransfer.json;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import name.abuchen.portfolio.json.impl.LocalDateSerializer;
import name.abuchen.portfolio.json.impl.LocalTimeSerializer;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;

public class SecurityMetaDataTransfer
{

    private static final Gson gSON = new GsonBuilder() //
                    .registerTypeAdapter(LocalDate.class, new LocalDateSerializer())
                    .registerTypeAdapter(LocalTime.class, new LocalTimeSerializer())
                    .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY).setPrettyPrinting().create();

    public String exportSecurityMetaData(Security security, List<Taxonomy> taxonomies)
    {
        JSecurityMetaData securityMetaData = new JSecurityMetaData(security, getClassifications(security, taxonomies));
        String json = gSON.toJson(securityMetaData);
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
                String color = classification.getColor();
                jTaxonomy.addClassification(new JTaxonomyClassification(classification.getId(), path, weight, color));
            }
            if (jTaxonomy.hasClassification()) {
                jJTaxonomies.add(jTaxonomy);
            }
        }
        return jJTaxonomies;
    }
}
