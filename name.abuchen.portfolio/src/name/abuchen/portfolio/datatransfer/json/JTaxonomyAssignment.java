package name.abuchen.portfolio.datatransfer.json;

import name.abuchen.portfolio.money.Values;

public class JTaxonomyAssignment
{

    private String key;

    private String name;

    // percent in the range of 0..100
    private double weight;

    public JTaxonomyAssignment(String key, String name, int weightValue)
    {
        super();
        this.key = key;
        this.name = name;
        setWeightValue(weightValue);
    }

    public String getKey()
    {
        return key;
    }

    // 0..100
    public double getWeight()
    {
        return weight;
    }

    // 0..100
    public void setWeight(double weight)
    {
        this.weight = weight;
    }

    // 0..(100 * Values.Weight.factor())
    public void setWeightValue(int weightValue)
    {
        this.weight = weightValue / Values.Weight.divider();
    }

    // 0..(100 * Values.Weight.factor())
    public int getWeightValue()
    {
        return (int) Values.Weight.factorize(weight);
    }

    public String getName()
    {
        return name;
    }
}
