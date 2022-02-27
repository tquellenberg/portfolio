package name.abuchen.portfolio.datatransfer.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.common.io.Resources;

import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Attributes;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty.Type;
import name.abuchen.portfolio.model.Taxonomy;

public class SecurityMetaDataTransferTest
{

    private Taxonomy taxonomy1;
    private Classification classification31;
    private Classification classification32;

    private List<Taxonomy> buildTaxonomy()
    {
        List<Taxonomy> taxonomies = new ArrayList<Taxonomy>();

        taxonomy1 = new Taxonomy("Regionen (MSCI)");
        taxonomies.add(taxonomy1);

        Classification classification = new Classification("1234", "Regionen (MSCI)");
        taxonomy1.setRootNode(classification);

        Classification classification2 = new Classification("", "Welt");
        classification2.setParent(classification);
        classification.addChild(classification2);

        classification31 = new Classification("", "Amerikas");
        classification31.setParent(classification2);
        classification2.addChild(classification31);
        classification31.setColor("#ffff00");

        classification32 = new Classification("", "Europa");
        classification32.setParent(classification2);
        classification2.addChild(classification32);
        classification32.setColor("#ff00ff");

        Taxonomy taxonomy2 = new Taxonomy("Branchen (GICS, Sektoren)");
        taxonomy2.setRootNode(new Classification("1234", "Branchen (GICS, Sektoren)"));
        taxonomies.add(taxonomy2);

        return taxonomies;
    }

    @Test
    public void exportSecurity() throws IOException
    {
        String json = Resources.toString(SecurityMetaDataTransfer.class.getResource("simple.json"),
                        StandardCharsets.UTF_8);

        Security security = new Security("Apple Inc", "US0378331005", "APC.DE", null);
        security.setCurrencyCode("EUR");
        security.setNote("Meine Anmerkungen...");
        security.setWkn("");

        List<Taxonomy> taxonomy = buildTaxonomy();
        // Assignment
        classification31.addAssignment(new Assignment(security, 9000));
        classification32.addAssignment(new Assignment(security, 1000));

        SecurityMetaDataTransfer meta = new SecurityMetaDataTransfer();
        String exportSecurityMetaData = meta.exportSecurityMetaData(security, taxonomy);

        assertEquals(json, exportSecurityMetaData);
    }

    @Test
    public void importSecurity() throws IOException
    {
        String json = Resources.toString(SecurityMetaDataTransfer.class.getResource("complex.json"),
                        StandardCharsets.UTF_8);

        SecurityMetaDataTransfer meta = new SecurityMetaDataTransfer();

        List<Taxonomy> taxonomy = buildTaxonomy();
        Security s = new Security();
        meta.importSecurityMetaData(json, s, taxonomy);

        assertEquals("Apple Inc", s.getName());
        assertEquals("035ffa2ff94b49a08ede0637bdfcbc2d", s.getOnlineId());
        assertEquals("US0378331005", s.getIsin());
        assertEquals("EUR", s.getCurrencyCode());
        assertEquals("Meine Anmerkungen...", s.getNote());
        assertEquals("APC.DE", s.getTickerSymbol());
        assertEquals("", s.getWkn());
        assertEquals("de", s.getCalendar());
        
        Attributes attributes = s.getAttributes();
        assertTrue(attributes.exists(new AttributeType("logo")));
        
        assertEquals("abc", s.getPropertyValue(Type.FEED, "GENERIC-JSON-DATE").get());

        List<Classification> classifications = taxonomy1.getClassifications(s);
        assertEquals(2, classifications.size());
        classifications.sort((c1, c2) -> Integer.compare(c1.getAssignments().get(0).getWeight(),
                        c2.getAssignments().get(0).getWeight()));
        assertEquals(1000, classifications.get(0).getAssignments().get(0).getWeight());
        assertEquals(9000, classifications.get(1).getAssignments().get(0).getWeight());
    }
}
