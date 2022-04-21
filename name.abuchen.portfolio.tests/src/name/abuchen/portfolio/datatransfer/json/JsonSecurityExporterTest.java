package name.abuchen.portfolio.datatransfer.json;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.google.common.io.Resources;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.Classification.Assignment;

public class JsonSecurityExporterTest
{

    private Taxonomy taxonomy1;
    private Classification classification31;
    private Classification classification32;

    private List<Taxonomy> buildTaxonomies()
    {
        List<Taxonomy> taxonomies = new ArrayList<Taxonomy>();

        taxonomy1 = new Taxonomy("Regionen (MSCI)");
        taxonomies.add(taxonomy1);

        Classification classification = new Classification("1234", "Regionen (MSCI)");
        classification.setKey("regions-msci");
        taxonomy1.setRootNode(classification);

        Classification classification2 = new Classification("", "Welt");
        classification2.setKey("RW");
        classification2.setParent(classification);
        classification.addChild(classification2);

        classification31 = new Classification("", "Amerikas");
        classification31.setKey("RW1");
        classification31.setParent(classification2);
        classification2.addChild(classification31);
        classification31.setColor("#ffff00");

        classification32 = new Classification("", "Europa");
        classification32.setKey("RW2");
        classification32.setParent(classification2);
        classification2.addChild(classification32);
        classification32.setColor("#ff00ff");

        Taxonomy taxonomy2 = new Taxonomy("Branchen (GICS, Sektoren)");
        Classification classification3 = new Classification("1234", "Branchen (GICS, Sektoren)");
        classification3.setKey("industry-gics-1st-level");
        taxonomy2.setRootNode(classification3);
        taxonomies.add(taxonomy2);

        return taxonomies;
    }

    @Test
    public void exportSecurity() throws IOException
    {
        String json = Resources.toString(JsonSecurityExporterTest.class.getResource("simple.json"),
                        StandardCharsets.UTF_8);

        Security security = new Security("Apple Inc", "US0378331005", "APC.DE", null);
        security.setCurrencyCode("EUR");
        security.setNote("Meine Anmerkungen...");
        security.setWkn("");

        List<Taxonomy> taxonomies = buildTaxonomies();
        // Assignment
        classification31.addAssignment(new Assignment(security, 9000));
        classification32.addAssignment(new Assignment(security, 1000));

        JsonSecurityExporter exporter = new JsonSecurityExporter();
        String exportSecurityMetaData = exporter.exportSecurityMetaData(Collections.singletonList(security), taxonomies);

        assertEquals(json, exportSecurityMetaData);
    }

}
