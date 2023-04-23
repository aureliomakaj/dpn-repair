package org.example.dpnrepair.parser;

import org.example.dpnrepair.parser.ast.DPN;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class DPNParserTest {

    private final String RES_DIR = "src/test/resources/org/example/dpnrepair/parser/";

    @Test
    void when_missing_root_then_throw_dpnparserexception() {
        DPNParser parser = initialize("test-1.pnml");
        DPNParserException e = assertThrows(DPNParserException.class, () -> parser.parse());
        assertEquals("\"pnml\" tag is required as root element", e.getMessage());
    }

    @Test
    void when_missing_net_then_throw_dpnparserexception() {
        DPNParser parser = initialize("test-2.pnml");
        DPNParserException e = assertThrows(DPNParserException.class, () -> parser.parse());
        assertEquals("Empty \"pnml\" tag found", e.getMessage());
    }
    @Test
    void when_missing_net_id_then_throw_dpnparserexception() {
        DPNParser parser = initialize("test-3.pnml");
        DPNParserException e = assertThrows(DPNParserException.class, () -> parser.parse());
        assertEquals("\"id\" attribute expected for node \"net\"", e.getMessage());
    }


    private DPNParser initialize(String filename) {
        DPNParser parser = null;
        try {
            parser = new DPNParser(RES_DIR + filename);
        } catch (ParserConfigurationException e) {
            //
        } catch (IOException e) {
            //
        } catch (SAXException e) {
            //
        }
        return parser;
    }
}