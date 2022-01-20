package name.abuchen.portfolio.datatransfer.json;

import java.util.List;

public class JTaxonomyAssignment
{

    private String key;

    private List<String> path;

    private int weight;

    public JTaxonomyAssignment(String key, List<String> path, int weight)
    {
        super();
        this.key = key;
        this.setPath(path);
        this.weight = weight;
    }

    public String getKey()
    {
        return key;
    }

    public int getWeight()
    {
        return weight;
    }

    public void setWeight(int weight)
    {
        this.weight = weight;
    }

    public List<String> getPath()
    {
        return path;
    }

    public void setPath(List<String> path)
    {
        this.path = path;
    }

}
