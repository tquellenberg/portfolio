package name.abuchen.portfolio.datatransfer.json;

import java.util.ArrayList;
import java.util.List;

public class JSecurities
{

    public static final String VERSION_1_0 = "1.0"; //$NON-NLS-1$
    public static final String SECURITY_META_DATA = "SecurityMetaData"; //$NON-NLS-1$
    
    private String version = VERSION_1_0;
    private String type = SECURITY_META_DATA; 

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
