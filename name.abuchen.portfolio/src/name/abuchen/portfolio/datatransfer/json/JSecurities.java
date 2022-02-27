package name.abuchen.portfolio.datatransfer.json;

import java.util.ArrayList;
import java.util.List;

public class JSecurities
{

    private String version = "1.0"; //$NON-NLS-1$
    private String type = "SecurityMetaData"; //$NON-NLS-1$

    private List<JSecurityMetaData> securities = new ArrayList<>();

    public void addSecurity(JSecurityMetaData jSecurityMetaData)
    {
        securities.add(jSecurityMetaData);
    }
    
    public String getVersion()
    {
        return version;
    }
    
    public String getType()
    {
        return type;
    }

    public List<JSecurityMetaData> getSecurities()
    {
        return securities;
    }
}
