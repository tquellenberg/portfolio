package name.abuchen.portfolio.datatransfer.json;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import name.abuchen.portfolio.json.impl.LocalDateSerializer;
import name.abuchen.portfolio.json.impl.LocalTimeSerializer;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;

public class SecurityMetaDataTransfer
{

    private static final Gson gSON = new GsonBuilder() //
                    .registerTypeAdapter(LocalDate.class, new LocalDateSerializer())
                    .registerTypeAdapter(LocalTime.class, new LocalTimeSerializer())
                    .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY).create();

    public void exportSecurityMetaData(Security security, Client client)
    {
        JSecurityMetaData securityMetaData = new JSecurityMetaData(security, getClassifications(security, client));
        String json = gSON.toJson(securityMetaData);
        System.out.println(json);
    }

    private List<JTaxonomyClassification> getClassifications(Security security, Client client)
    {
        List<JTaxonomyClassification> classifications = new ArrayList<>();
        for (Taxonomy taxonomy : client.getTaxonomies())
        {
            for (Classification classification : taxonomy.getClassifications(security))
            {
                List<String> path = classification.getPathToRoot().stream().map(c -> c.getName()).collect(Collectors.toList());
                int weight = classification.getWeight();
                String color = classification.getColor();
                classifications.add(new JTaxonomyClassification(path, weight, color));
            }
        }
        return classifications;
    }
}
