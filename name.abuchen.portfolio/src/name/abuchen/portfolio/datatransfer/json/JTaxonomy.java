package name.abuchen.portfolio.datatransfer.json;

import java.util.ArrayList;
import java.util.List;

public class JTaxonomy
{
    private String name;
    private List<JTaxonomyClassification> classifications = new ArrayList<>();

    public JTaxonomy(String name)
    {
        this.name = name;
    }

    public void addClassification(JTaxonomyClassification jTaxonomyClassification)
    {
        classifications.add(jTaxonomyClassification);
    }

    public boolean hasClassification()
    {
        return !classifications.isEmpty();
    }
    
    public String getName()
    {
        return name;
    }
}
