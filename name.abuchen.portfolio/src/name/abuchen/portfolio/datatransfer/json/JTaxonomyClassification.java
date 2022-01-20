package name.abuchen.portfolio.datatransfer.json;

import java.util.List;

public class JTaxonomyClassification
{

    private List<String> path;

    private int weight;

    private String color;

    public JTaxonomyClassification(List<String> path, int weight, String color)
    {
        super();
        this.setColor(color);
        this.setPath(path);
        this.weight = weight;
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
