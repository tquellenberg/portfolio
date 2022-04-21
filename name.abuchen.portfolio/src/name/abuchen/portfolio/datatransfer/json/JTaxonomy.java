package name.abuchen.portfolio.datatransfer.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class JTaxonomy
{
    @SerializedName("taxonomyKey")
    private String key;

    @SerializedName("taxonomyName")
    private String name;
    
    private List<JTaxonomyAssignment> assignments;

    public JTaxonomy(String key, String name)
    {
        this.key = key;
        this.name = name;
    }

    public void addAssignment(JTaxonomyAssignment jTaxonomyClassification)
    {
        if (assignments == null)
        {
            assignments = new ArrayList<JTaxonomyAssignment>();
        }
        assignments.add(jTaxonomyClassification);
    }

    public boolean hasAssignment()
    {
        return assignments != null && !assignments.isEmpty();
    }

    public String getKey()
    {
        return key;
    }
    
    public String getName()
    {
        return name;
    }

    public List<JTaxonomyAssignment> getAssignments()
    {
        if (assignments == null)
            return Collections.emptyList();
        return assignments;
    }
}
