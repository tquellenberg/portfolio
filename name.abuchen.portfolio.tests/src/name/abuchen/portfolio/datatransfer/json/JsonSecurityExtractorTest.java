package name.abuchen.portfolio.datatransfer.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonSyntaxException;

import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Attributes;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty.Type;
import name.abuchen.portfolio.model.Taxonomy;

public class JsonSecurityExtractorTest
{

    private JsonSecurityExtractor extractor;
    private List<Exception> errors;

    private List<Taxonomy> taxonomy;
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

    private Reader reader(String filename)
    {
        return new InputStreamReader(JsonSecurityExtractorTest.class.getResourceAsStream(filename),
                        StandardCharsets.UTF_8);
    }

    @Before
    public void setup()
    {
        extractor = new JsonSecurityExtractor(null);
        errors = new ArrayList<>();
        taxonomy = buildTaxonomy();
    }

    @Test
    public void parseJson() throws IOException, URISyntaxException
    {
        JsonSecurityExtractor extractor = new JsonSecurityExtractor(null);
        List<Exception> errors = new ArrayList<>();
        List<JSecurityMetaData> securityMetaData = extractor.parseJson(reader("complex.json"), errors);

        assertEquals(1, securityMetaData.size());
        assertEquals("US0378331005", securityMetaData.get(0).getIsin());
    }

    @Test
    public void parseMinimalJson()
    {
        JSecurityMetaData securityMetaData = extractor.parseJson(reader("minimal.json"), errors).get(0);

        assertNull(securityMetaData.getFeed());
        assertNull(securityMetaData.getNote());
        assertNull(securityMetaData.getProperties());
        assertNull(securityMetaData.getAttributes());

        assertEquals(0, securityMetaData.getTaxonomies().size());
    }

    @Test
    public void parseSyntaxError()
    {
        List<JSecurityMetaData> securityMetaData = extractor.parseJson(reader("syntaxError.txt"), errors);

        assertEquals(0, securityMetaData.size());
        assertEquals(1, errors.size());

        Exception exception = errors.get(0);
        assertTrue(exception instanceof JsonSyntaxException);
    }

    @Test
    public void importSecurity() throws IOException, URISyntaxException
    {
        List<JSecurityMetaData> parseJson = extractor.parseJson(reader("complex.json"), errors);

        Security s = new Security();
        extractor.importSecurityMetaData(parseJson.get(0), s, taxonomy);

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

    @Test
    public void updateSecurity()
    {
        // Existing Security
        Security s = new Security();
        s.setIsin("US0378331005");
        s.setName("Apple Inc");
        s.setCalendar("de");
        s.setNote("Meine Anmerkungen...");
        s.setPropertyValue(Type.FEED, "GENERIC-JSON-DATE", "abs");

        // Json Update
        JSecurityMetaData jsonSecurity = new JSecurityMetaData("US0378331005", "Changed Name");
        jsonSecurity.setCalendar(null); // Not included in json
        jsonSecurity.setWkn("APC.DE"); // Not set in original security
        jsonSecurity.setNote(""); // Empty string
        jsonSecurity.setProperties(Collections.emptyMap()); // Not null but
                                                            // empty parameter
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("logo", "data:image/png;base64,iVBORw0KGgoAAAANSUhEU...");
        jsonSecurity.setAttributes(attributes);

        extractor.importSecurityMetaData(jsonSecurity, s, taxonomy);

        assertEquals("Changed Name", s.getName());
        assertEquals("de", s.getCalendar());
        assertEquals("", s.getNote());
        // leave parameter unchanged (only add and change possible)
        assertEquals("abs", s.getPropertyValue(Type.FEED, "GENERIC-JSON-DATE").get());
        assertTrue(s.getAttributes().exists(new AttributeType("logo")));
    }
}
