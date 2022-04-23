package name.abuchen.portfolio.datatransfer.json;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Resources;

import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.AttributeType.LimitPriceConverter;
import name.abuchen.portfolio.model.Attributes;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LimitPrice;
import name.abuchen.portfolio.model.LimitPrice.RelationalOperator;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;

public class JsonSecurityExporterTest
{

    private Taxonomy taxonomy1;
    private Classification classification31;
    private Classification classification32;

    private Client client;

    @Before
    public void setuo()
    {
        client = new Client();
    }

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
        String exportSecurityMetaData = exporter.exportSecurityMetaData(Collections.singletonList(security), taxonomies,
                        client.getSettings());

        assertEquals(json, exportSecurityMetaData);
    }

    @Test
    public void exportSecurityAttributes() throws IOException
    {
        String json = Resources.toString(JsonSecurityExporterTest.class.getResource("withAttribute.json"),
                        StandardCharsets.UTF_8);

        Security security = new Security("Apple Inc", "US0378331005", "APC.DE", null);
        security.setCurrencyCode("EUR");
        security.setNote("Meine Anmerkungen...");
        security.setWkn("");

        AttributeType attributeType = new AttributeType("b918a6df-aa5d-4b10-8cde-8adf7e10bfd2");
        attributeType.setName("Limit");
        attributeType.setColumnLabel("Limt");
        attributeType.setConverter(LimitPriceConverter.class);
        attributeType.setType(LimitPrice.class);
        attributeType.setTarget(Security.class);

        client.getSettings().addAttributeType(attributeType);

        Attributes attributes = new Attributes();
        attributes.put(attributeType, new LimitPrice(RelationalOperator.GREATER, 14000000000L));
        security.setAttributes(attributes);

        JsonSecurityExporter exporter = new JsonSecurityExporter();
        String exportSecurityMetaData = exporter.exportSecurityMetaData(Collections.singletonList(security),
                        Collections.emptyList(), client.getSettings());

        assertEquals(json, exportSecurityMetaData);
    }

}
