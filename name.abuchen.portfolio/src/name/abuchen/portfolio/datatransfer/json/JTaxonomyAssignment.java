package name.abuchen.portfolio.datatransfer.json;

public class JTaxonomyAssignment
{

    private String key;

    private String name;

    private int weight;

    public JTaxonomyAssignment(String key, String name, int weight)
    {
        super();
        this.key = key;
        this.name = name;
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
    
    public String getName()
    {
        return name;
    }

}
