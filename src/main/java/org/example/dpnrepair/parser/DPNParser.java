package org.example.dpnrepair.parser;

import org.example.dpnrepair.parser.ast.DPN;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class DPNParser {
    private Document xml;
    private DPN dpn;
    private static final String[] ALLOWED_TAGS = {Tags.PNML, Tags.NET, Tags.NAME, Tags.TEXT, Tags.PAGE, Tags.PLACE, Tags.GRAPHICS, Tags.POSITION, Tags.DIMENSION, Tags.TRANSITION, Tags.FILL, Tags.WRITE_VAR, Tags.READ_VAR, Tags.ARC_TYPE, Tags.FINAL_MARKINGS, Tags.INITIAL_MARKINGS, Tags.MARKINGS, Tags.VARIABLES, Tags.VARIABLE};

    public DPNParser(String file) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        xml = builder.parse(new File(file));
        xml.getDocumentElement().normalize();
    }

    public void parse() throws DPNParserException {
        dpn = new DPN();
        Node pnml = xml.getDocumentElement();
        if(!Tags.PNML.equals(pnml.getNodeName())){
            throw new DPNParserException("\"" + Tags.PNML + "\" tag is required as root element");
        }

        if(!pnml.hasChildNodes()){
            throw new DPNParserException("Empty \"" + Tags.PNML + "\" tag found");
        }

        parseNet(pnml.getChildNodes().item(0));
    }

    private void parseNet(Node net) throws DPNParserException {
        if(!Tags.NET.equals(net.getNodeName())){
            throw new DPNParserException("\"" + Tags.NET + "\" tag is required after \"" + Tags.PNML + "\" tag");
        }
        int a = 0;
    }
}
