package org.example.dpnrepair.parser;

import org.example.dpnrepair.parser.ast.*;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DPNParser {
    private Document xml;
    private DPN dpn;
    private GuardToConstraintConverter guardConverter = new GuardToConstraintConverter();
    private static final List<String> ALLOWED_TAGS = Arrays.asList(Tags.PNML, Tags.NET, Tags.NAME, Tags.TEXT, Tags.PAGE, Tags.PLACE, Tags.GRAPHICS, Tags.POSITION, Tags.DIMENSION, Tags.TRANSITION, Tags.FILL, Tags.WRITE_VAR, Tags.READ_VAR, Tags.ARC, Tags.ARC_TYPE, Tags.FINAL_MARKINGS, Tags.INITIAL_MARKINGS, Tags.MARKING, Tags.VARIABLES, Tags.VARIABLE);


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
                } else if (Tags.INITIAL_MARKINGS.equals(n.getNodeName())) {
                    dpn.setInitialMarking(parseMarking(n));
                } else if (Tags.FINAL_MARKINGS.equals(n.getNodeName())) {
                    dpn.setFinalMarking(parseMarking(n));
                } else if (Tags.VARIABLES.equals(n.getNodeName())) {
                    dpn.setVariables(parseVariables(n));
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
                    dpn.addTransition(t);
                } else if (Tags.ARC.equals(n.getNodeName())) {
                    Arc a = parseArc(n);
                    dpn.addArc(a);
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

        Node x = attr.getNamedItem(Attributes.X);
        if (x != null) {
            p.setX(Float.valueOf(x.getTextContent()));
        }

        Node y = attr.getNamedItem(Attributes.Y);
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

        Node width = attr.getNamedItem(Attributes.X);
        if (width != null) {
            d.setWidth(Float.valueOf(width.getTextContent()));
        }

        Node height = attr.getNamedItem(Attributes.Y);
        if (height != null) {
            d.setHeight(Float.valueOf(height.getTextContent()));
        }

        return d;
    }

    private Transition parseTransition(Node transition) throws DPNParserException {
        Transition t = new Transition();
        t.setId(extractId(transition));

        List<String> readVars = new ArrayList<>();
        List<String> writtenVars = new ArrayList<>();

        for (int i = 0; i < transition.getChildNodes().getLength(); i++) {
            Node n = transition.getChildNodes().item(i);
            if (isSupported(n)) {
                if (isName(n)) {
                    t.setName(parseName(n));
                } else if (isGraphics(n)) {
                    Graphics g = parseGraphics(n);
                    t.setGraphics(g);
                } else if (Tags.READ_VAR.equals(n.getNodeName())) {
                    readVars.add(n.getTextContent());
                } else if (Tags.WRITE_VAR.equals(n.getNodeName())) {
                    writtenVars.add(n.getTextContent());
                }
            }
        }

        Constraint c = extractConstraintFromGuard(transition, readVars, writtenVars);
        t.setGuard(c);
        return t;
    }

    private Constraint extractConstraintFromGuard(Node transition, List<String> readVars, List<String> writeVars) throws DPNParserException {
        Node guardNode = transition.getAttributes().getNamedItem(Attributes.GUARD);
        if (guardNode == null) {
            throw new DPNParserException("\"" + Tags.TRANSITION + "\" tag must contain \"" + Attributes.GUARD + "\" attribute");
        }

        String guard = guardNode.getTextContent();
        return guardConverter.convert(guard, readVars, writeVars);
    }

    private Arc parseArc(Node arc) throws DPNParserException {
        Arc a = new Arc();
        a.setId(extractId(arc));
        for (int i = 0; i < arc.getChildNodes().getLength(); i++) {
            Node n = arc.getChildNodes().item(i);
            if (isSupported(n)) {
                if (isName(n)) {
                    a.setName(parseName(n));
                } else if (Tags.ARC_TYPE.equals(n.getNodeName())) {
                    a.setArctype(n.getTextContent().trim());
                }
            }
        }

        Node sourceNode = arc.getAttributes().getNamedItem(Attributes.SOURCE);
        if (sourceNode == null) {
            throw new DPNParserException("\"" + Tags.ARC + "\" tag must have a \"" + Attributes.SOURCE + "\" attribute");
        }
        a.setSource(sourceNode.getTextContent());

        Node targetNode = arc.getAttributes().getNamedItem(Attributes.TARGET);
        if (targetNode == null) {
            throw new DPNParserException("\"" + Tags.ARC + "\" tag must have a \"" + Attributes.TARGET + "\" attribute");
        }
        a.setTarget(targetNode.getTextContent());

        return a;
    }

    private Marking parseMarking(Node initMarking) throws DPNParserException {
        Marking m = new Marking();
        for (int i = 0; i < initMarking.getChildNodes().getLength(); i++) {
            Node n = initMarking.getChildNodes().item(i);
            if (isSupported(n) && Tags.MARKING.equals(n.getNodeName())) {
                for (int j = 0; j < n.getChildNodes().getLength(); j++) {
                    Node sub_node = n.getChildNodes().item(j);
                    if (isSupported(n) && Tags.PLACE.equals(sub_node.getNodeName())) {
                        Node refPlace = sub_node.getAttributes().getNamedItem(Attributes.ID_REF);
                        if (refPlace == null) {
                            throw new DPNParserException("\"" + Tags.PLACE + "\" attribute inside \"" + Tags.MARKING +
                                    "\" must have an \"" + Attributes.ID_REF + "\" attribute");
                        }
                        m.addPlaceIds(refPlace.getTextContent());
                    }
                }
            }
        }
        return m;
    }

    private Map<String, Variable> parseVariables(Node variables) throws DPNParserException {
        Map<String, Variable> vars = new HashMap<>();
        for (int i = 0; i < variables.getChildNodes().getLength(); i++) {
            Node n = variables.getChildNodes().item(i);
            if (isSupported(n) && Tags.VARIABLE.equals(n.getNodeName())) {
                Variable v = parseVariable(n);
                vars.put(v.getName(), v);
            }
        }
        return vars;
    }

    private Variable parseVariable(Node variable) throws DPNParserException {
        Variable v = new Variable();
        for (int i = 0; i < variable.getChildNodes().getLength(); i++) {
            Node n = variable.getChildNodes().item(i);
            if (isSupported(n)) {
                if (isName(n)) {
                    v.setName(parseName(n));
                } else if (isGraphics(n)) {
                    v.setGraphics(parseGraphics(n));
                }
            }
        }

        Node minValueNode = variable.getAttributes().getNamedItem(Attributes.MIN_VALUE);
        long minValue = minValueNode == null ? Long.MIN_VALUE : Long.parseLong(minValueNode.getTextContent().trim());
        v.setMinValue(minValue);

        Node maxValueNode = variable.getAttributes().getNamedItem(Attributes.MAX_VALUE);
        long maxValue = maxValueNode == null ? Long.MAX_VALUE : Long.parseLong(maxValueNode.getTextContent().trim());
        if(maxValue < minValue){
            throw new DPNParserException(Attributes.MAX_VALUE + " lower than " + Attributes.MIN_VALUE + " detected for variable " + v.getName());
        }
        v.setMaxValue(maxValue);

        Node initialValueNode = variable.getAttributes().getNamedItem(Attributes.INITIAL_VALUE);
        long initialValue = initialValueNode == null ? 0L : Long.parseLong(initialValueNode.getTextContent().trim());
        if(initialValue < minValue || initialValue > maxValue){
            throw new DPNParserException(Attributes.INITIAL_VALUE + " not in range [" + minValue + ", " + maxValue + "] variable " + v.getName());
        }
        v.setInitialValue(initialValue);
        return v;
    }

    private String parseName(Node name) throws DPNParserException {
        for (int i = 0; i < name.getChildNodes().getLength(); i++) {
            Node n = name.getChildNodes().item(i);
            if (isSupported(n) && Tags.TEXT.equals(n.getNodeName())) {
                return n.getTextContent();
            }
        }
        throw new DPNParserException("Expected tag \"" + Tags.TEXT + "\" inside \"" + Tags.NAME + "\" tag");
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

    private boolean isValidVariable(String id) {
        Pattern p = Pattern.compile("^[a-z][a-z0-9_]*$");
        Matcher m = p.matcher(id);
        return m.matches();
    }
}
