package name.abuchen.portfolio.datatransfer.json;

import java.util.List;

public class JTaxonomyAssignment
{

    private String id;

    private List<String> path;

    private int weight;

    public JTaxonomyAssignment(String id, List<String> path, int weight)
    {
        super();
        this.id = id;
        this.setPath(path);
        this.weight = weight;
    }

    public String getId()
    {
        return id;
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
