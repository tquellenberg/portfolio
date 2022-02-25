package name.abuchen.portfolio.datatransfer.json;

import java.util.List;

public class JTaxonomyClassification
{

    private String id;

    private List<String> path;

    private int weight;

    private String color;

    public JTaxonomyClassification(String id, List<String> path, int weight, String color)
    {
        super();
        this.id = id;
        this.setColor(color);
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

    public String getColor()
    {
        return color;
    }

    public void setColor(String color)
    {
        this.color = color;
    }

}
