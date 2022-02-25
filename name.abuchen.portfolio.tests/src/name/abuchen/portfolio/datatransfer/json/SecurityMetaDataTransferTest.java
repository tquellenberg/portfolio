package name.abuchen.portfolio.datatransfer.json;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.common.io.Resources;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;

public class SecurityMetaDataTransferTest
{

    @Test
    public void exportSecurity() throws IOException {
        String json = Resources.toString(SecurityMetaDataTransfer.class.getResource("US0378331005.json"), StandardCharsets.UTF_8);
        
        Security security = new Security("Apple Inc", "US0378331005", "APC.DE", "feed");
        security.setCurrencyCode("EUR");
        security.setNote("Meine Anmerkungen...");
        security.setWkn("");
        List<Taxonomy> taxonomies = new ArrayList<Taxonomy>();

        Taxonomy taxonomy = new Taxonomy("Regionen (MSCI)");
        taxonomies.add(taxonomy);

        Classification classification = new Classification("1234", "Regionen (MSCI)");
        taxonomy.setRootNode(classification);

        Classification classification2 = new Classification("", "Welt");
        classification2.setParent(classification);
        classification.addChild(classification2);
        
        Classification classification31 = new Classification("", "Amerikas");
        classification31.setParent(classification2);
        classification2.addChild(classification31);
        classification31.setColor("#ffff00");
        classification31.addAssignment(new Assignment(security, 9000));

        Classification classification32 = new Classification("", "Europa");
        classification32.setParent(classification2);
        classification2.addChild(classification32);
        classification32.setColor("#ff00ff");
        classification32.addAssignment(new Assignment(security, 1000));

        
        Taxonomy taxonomy2 = new Taxonomy("Branchen (GICS, Sektoren)");
        taxonomy2.setRootNode(new Classification("1234", "Branchen (GICS, Sektoren)"));
        taxonomies.add(taxonomy2);
        
        SecurityMetaDataTransfer meta = new SecurityMetaDataTransfer();
        String exportSecurityMetaData = meta.exportSecurityMetaData(security, taxonomies);
        
        assertEquals(json, exportSecurityMetaData);
    }
}
