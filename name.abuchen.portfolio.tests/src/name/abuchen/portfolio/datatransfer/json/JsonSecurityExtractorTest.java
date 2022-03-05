package name.abuchen.portfolio.datatransfer.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Attributes;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty.Type;
import name.abuchen.portfolio.model.Taxonomy;

public class JsonSecurityExtractorTest
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
    public void parseJson() throws IOException, URISyntaxException
    {
        JsonSecurityExtractor extractor = new JsonSecurityExtractor(null);
        InputStreamReader reader = new InputStreamReader(
                        JsonSecurityExtractorTest.class.getResourceAsStream("complex.json"), StandardCharsets.UTF_8);

        List<Exception> errors = new ArrayList<>();
        List<JSecurityMetaData> securityMetaData = extractor.parseJson(reader, errors);

        assertEquals(1, securityMetaData.size());
        assertEquals("US0378331005", securityMetaData.get(0).getIsin());
    }

    @Test
    public void importSecurity() throws IOException, URISyntaxException
    {
        JsonSecurityExtractor extractor = new JsonSecurityExtractor(null);
        InputStreamReader reader = new InputStreamReader(
                        JsonSecurityExtractorTest.class.getResourceAsStream("complex.json"), StandardCharsets.UTF_8);

        List<Exception> errors = new ArrayList<>();
        List<JSecurityMetaData> parseJson = extractor.parseJson(reader, errors);

        List<Taxonomy> taxonomy = buildTaxonomy();
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
}
