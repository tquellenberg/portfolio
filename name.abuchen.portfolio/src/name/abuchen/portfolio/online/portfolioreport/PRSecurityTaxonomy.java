package name.abuchen.portfolio.online.portfolioreport;

public class PRSecurityTaxonomy
{
    private String taxonomyUuid;
    private String rootTaxonomyUuid;
    private int weight;

    public String getTaxonomyUuid()
    {
        return taxonomyUuid;
    }

    public void setTaxonomyUuid(String taxonomyUuid)
    {
        this.taxonomyUuid = taxonomyUuid;
    }

    public String getRootTaxonomyUuid()
    {
        return rootTaxonomyUuid;
    }

    public void setRootTaxonomyUuid(String rootTaxonomyUuid)
    {
        this.rootTaxonomyUuid = rootTaxonomyUuid;
    }

    public int getWeight()
    {
        return weight;
    }

    public void setWeight(int weight)
    {
        this.weight = weight;
    }
}
