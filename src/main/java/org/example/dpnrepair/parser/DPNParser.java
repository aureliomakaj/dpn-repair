package org.example.dpnrepair.parser;

import org.example.dpnrepair.parser.ast.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DPNParser {
    private Document xml;
    private DPN dpn;
    private static final List<String> ALLOWED_TAGS = Arrays.asList(Tags.PNML, Tags.NET, Tags.NAME, Tags.TEXT, Tags.PAGE, Tags.PLACE, Tags.GRAPHICS, Tags.POSITION, Tags.DIMENSION, Tags.TRANSITION, Tags.FILL, Tags.WRITE_VAR, Tags.READ_VAR, Tags.ARC_TYPE, Tags.FINAL_MARKINGS, Tags.INITIAL_MARKINGS, Tags.MARKINGS, Tags.VARIABLES, Tags.VARIABLE);


    public DPNParser(String file) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        xml = builder.parse(new File(file));
        xml.normalizeDocument();
    }

    public void parse() throws DPNParserException {
        dpn = new DPN();
        Node pnml = xml.getDocumentElement();
        if (!Tags.PNML.equals(pnml.getNodeName())) {
            throw new DPNParserException("\"" + Tags.PNML + "\" tag is required as root element");
        }

        if (!pnml.hasChildNodes()) {
            throw new DPNParserException("Empty \"" + Tags.PNML + "\" tag found");
        }

        for (int i = 0; i < pnml.getChildNodes().getLength(); i++) {
            Node n = pnml.getChildNodes().item(i);
            if (isSupported(n)) {
                parseNet(n);
            }
        }
    }

    private void parseNet(Node net) throws DPNParserException {
        if (!Tags.NET.equals(net.getNodeName())) {
            throw new DPNParserException("\"" + Tags.NET + "\" tag is required after \"" + Tags.PNML + "\" tag");
        }

        for (int i = 0; i < net.getChildNodes().getLength(); i++) {
            Node n = net.getChildNodes().item(i);
            if (isSupported(n)) {
                if (isName(n)) {
                    String name = parseName(n);
                    dpn.setName(name);
                } else if (Tags.PAGE.equals(n.getNodeName())) {
                    parsePage(n);
                }
            }
        }
    }

    private void parsePage(Node page) throws DPNParserException {
        for (int i = 0; i < page.getChildNodes().getLength(); i++) {
            Node n = page.getChildNodes().item(i);
            if (isSupported(n)) {
                if (Tags.PLACE.equals(n.getNodeName())) {
                    Place p = parsePlace(n);
                    dpn.addPlace(p);
                } else if (Tags.TRANSITION.equals(n.getNodeName())) {
                    Transition t = parseTransition(n);
                }
            }
        }
    }

    private Place parsePlace(Node place) throws DPNParserException {
        Place p = new Place();
        p.setId(extractId(place));

        for (int i = 0; i < place.getChildNodes().getLength(); i++) {
            Node n = place.getChildNodes().item(i);
            if (isSupported(n)) {
                if (isName(n)) {
                    String name = parseName(n);
                    p.setName(name);
                } else if (isGraphics(n)) {
                    Graphics g = parseGraphics(n);
                    p.setGraphics(g);
                }
            }
        }

        return p;
    }

    private String extractId(Node node) throws DPNParserException {
        Node idNode = node.getAttributes().getNamedItem(Attributes.ID);
        if (idNode == null) {
            throw new DPNParserException("\"" + Attributes.ID + "\" attribute expected for node \"" +
                    node.getNodeName() + "\"");
        }
        if (!isValidId(idNode.getTextContent())) {
            throw new DPNParserException("Invalid \"" + Attributes.ID + "\". It must be all lowercase, starting " +
                    "with letters or underscore and followed by letters, numbers, underscore or dash");
        }

        return idNode.getTextContent();
    }

    private Graphics parseGraphics(Node graphics) {
        Graphics g = new Graphics();
        boolean notEmpty = false;
        for (int i = 0; i < graphics.getChildNodes().getLength(); i++) {
            Node n = graphics.getChildNodes().item(i);
            if (isSupported(n)) {
                if (Tags.POSITION.equals(n.getNodeName())) {
                    Position p = parsePosition(n);
                    if (p != null) {
                        notEmpty = true;
                        g.setPosition(p);
                    }
                } else if (Tags.DIMENSION.equals(n.getNodeName())) {
                    Dimension d = parseDimension(n);
                    if (d != null) {
                        notEmpty = true;
                        g.setDimension(d);
                    }
                } else if (Tags.FILL.equals(n.getNodeName())) {
                    notEmpty = true;
                    g.setFill(n.getTextContent());
                }
            }
        }
        return notEmpty ? g : null;
    }

    private Position parsePosition(Node position) {
        Position p = new Position();
        NamedNodeMap attr = position.getAttributes();
        if (attr.getLength() != 2) {
            return null;
        }

        Node x = attr.getNamedItem("x");
        if (x != null) {
            p.setX(Float.valueOf(x.getTextContent()));
        }

        Node y = attr.getNamedItem("y");
        if (y != null) {
            p.setY(Float.valueOf(y.getTextContent()));
        }

        return p;
    }

    private Dimension parseDimension(Node dimension) {
        Dimension d = new Dimension();
        NamedNodeMap attr = dimension.getAttributes();
        if (attr.getLength() != 2) {
            return null;
        }

        Node width = attr.getNamedItem("x");
        if (width != null) {
            d.setWidth(Float.valueOf(width.getTextContent()));
        }

        Node height = attr.getNamedItem("y");
        if (height != null) {
            d.setHeight(Float.valueOf(height.getTextContent()));
        }

        return d;
    }

    private Transition parseTransition(Node transition) {
        return null;
    }

    private String parseName(Node name) {
        for (int i = 0; i < name.getChildNodes().getLength(); i++) {
            Node n = name.getChildNodes().item(i);
            if (isSupported(n) && Tags.TEXT.equals(n.getNodeName())) {
                return n.getTextContent();
            }
        }
        return null;
    }

    private boolean isSupported(Node node) {
        return ALLOWED_TAGS.contains(node.getNodeName());
    }

    private boolean isName(Node node) {
        return Tags.NAME.equals(node.getNodeName());
    }

    private boolean isGraphics(Node node) {
        return Tags.GRAPHICS.equals(node.getNodeName());
    }

    private boolean isValidId(String id) {
        Pattern p = Pattern.compile("^[a-z_][a-z0-9_-]*$");
        Matcher m = p.matcher(id);
        return m.matches();
    }
}
