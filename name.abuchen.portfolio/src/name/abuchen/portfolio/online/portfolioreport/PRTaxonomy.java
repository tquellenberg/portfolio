package name.abuchen.portfolio.online.portfolioreport;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Objects;

public class PRTaxonomy
{
    private String uuid;
    private String parentUuid;
    private String rootUuid;
    private String name;
    private String code;

    public static List<PRTaxonomy> findWithParent(List<PRTaxonomy> all, String parentUuid)
    {
        return all.stream().filter(t -> Objects.equal(t.getParentUuid(), parentUuid)).collect(Collectors.toList());
    }

    public boolean isRootNode()
    {
        return parentUuid == null;
    }

    public String getUuid()
    {
        return uuid;
    }

    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }

    public String getParentUuid()
    {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid)
    {
        this.parentUuid = parentUuid;
    }

    public String getRootUuid()
    {
        return rootUuid;
    }

    public void setRootUuid(String rootUuid)
    {
        this.rootUuid = rootUuid;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

}
