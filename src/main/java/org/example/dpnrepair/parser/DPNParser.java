package org.example.dpnrepair.parser;

import org.example.dpnrepair.exceptions.DPNParserException;
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
    private final Document xml;
    private DPN dpn;
    private static final List<String> ALLOWED_TAGS = Arrays.asList(Tags.PNML, Tags.NET, Tags.NAME, Tags.TEXT, Tags.PAGE, Tags.PLACE, Tags.GRAPHICS, Tags.POSITION, Tags.DIMENSION, Tags.TRANSITION, Tags.FILL, Tags.WRITE_VAR, Tags.READ_VAR, Tags.ARC, Tags.ARC_TYPE, Tags.FINAL_MARKINGS, Tags.INITIAL_MARKINGS, Tags.MARKING, Tags.VARIABLES, Tags.VARIABLE);

    private final Map<String, String> idsCollected = new HashMap<>();

    public DPNParser(String file) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        xml = builder.parse(new File(file));
        xml.normalizeDocument();
    }

    public DPN getDpn() {
        return dpn;
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

        addZed();
        checkConstraintVariables();
        fillTransitionsWithArcs();
    }

    private void parseNet(Node net) throws DPNParserException {
        if (!Tags.NET.equals(net.getNodeName())) {
            throw new DPNParserException("\"" + Tags.NET + "\" tag is required after \"" + Tags.PNML + "\" tag");
        }

        dpn.setId(extractId(net));
        boolean parsedPage = false;
        for (int i = 0; i < net.getChildNodes().getLength(); i++) {
            Node n = net.getChildNodes().item(i);
            if (isSupported(n)) {
                if (isName(n)) {
                    String name = parseTextName(n);
                    dpn.setName(name);
                } else if (Tags.PAGE.equals(n.getNodeName())) {
                    parsePage(n);
                    parsedPage = true;
                } else if (Tags.INITIAL_MARKINGS.equals(n.getNodeName())) {
                    dpn.setInitialMarking(parseMarking(n, dpn.getPlaces().size()));
                } else if (Tags.FINAL_MARKINGS.equals(n.getNodeName())) {
                    dpn.setFinalMarking(parseMarking(n, dpn.getPlaces().size()));
                } else if (Tags.VARIABLES.equals(n.getNodeName())) {
                    parseVariables(n);
                }
            }
        }
        if (!parsedPage) {
            throw new DPNParserException("\"" + Tags.PAGE + "\" is missing");
        }
    }

    private void parsePage(Node page) throws DPNParserException {
        for (int i = 0; i < page.getChildNodes().getLength(); i++) {
            Node n = page.getChildNodes().item(i);
            if (isSupported(n)) {
                switch (n.getNodeName()) {
                    case Tags.PLACE:
                        Place p = parsePlace(n);
                        dpn.addPlace(p);
                        break;
                    case Tags.TRANSITION:
                        Transition t = parseTransition(n);
                        dpn.addTransition(t);
                        break;
                    case Tags.ARC:
                        Arc a = parseArc(n);
                        dpn.addArc(a);
                        break;
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
                    String name = parseTextName(n);
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
        if (!isValidId(idNode.getTextContent().trim())) {
            throw new DPNParserException("Invalid \"" + Attributes.ID + "\". It must be all lowercase, starting " +
                    "with letters or underscore and followed by letters, numbers, underscore or dash");
        }
        String id = idNode.getTextContent().trim();
        if (idsCollected.containsKey(id)) {
            throw new DPNParserException("Id \"" + id + "\" already used for tag \"" + idsCollected.get(id) + "\"");
        }
        idsCollected.put(id, node.getNodeName());
        return id;
    }

    private Graphics parseGraphics(Node graphics) {
        Graphics g = new Graphics();
        boolean notEmpty = false;
        for (int i = 0; i < graphics.getChildNodes().getLength(); i++) {
            Node n = graphics.getChildNodes().item(i);
            if (isSupported(n)) {
                switch (n.getNodeName()) {
                    case Tags.POSITION:
                        Position p = parsePosition(n);
                        if (p != null) {
                            notEmpty = true;
                            g.setPosition(p);
                        }
                        break;
                    case Tags.DIMENSION:
                        Dimension d = parseDimension(n);
                        if (d != null) {
                            notEmpty = true;
                            g.setDimension(d);
                        }
                        break;
                    case Tags.FILL:
                        notEmpty = true;
                        g.setFill(n.getTextContent().trim());
                        break;
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
            p.setX(Float.parseFloat(x.getTextContent().trim()));
        }

        Node y = attr.getNamedItem(Attributes.Y);
        if (y != null) {
            p.setY(Float.parseFloat(y.getTextContent().trim()));
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
            d.setWidth(Float.parseFloat(width.getTextContent().trim()));
        }

        Node height = attr.getNamedItem(Attributes.Y);
        if (height != null) {
            d.setHeight(Float.parseFloat(height.getTextContent().trim()));
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
                    t.setName(parseTextName(n));
                } else if (isGraphics(n)) {
                    Graphics g = parseGraphics(n);
                    t.setGraphics(g);
                } else if (Tags.READ_VAR.equals(n.getNodeName())) {
                    readVars.add(n.getTextContent().trim());
                } else if (Tags.WRITE_VAR.equals(n.getNodeName())) {
                    writtenVars.add(n.getTextContent().trim());
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

        String guard = guardNode.getTextContent().trim();
        return GuardToConstraintConverter.convert(guard, readVars, writeVars);
    }

    private Arc parseArc(Node arc) throws DPNParserException {
        Arc a = new Arc();
        a.setId(extractId(arc));
        for (int i = 0; i < arc.getChildNodes().getLength(); i++) {
            Node n = arc.getChildNodes().item(i);
            if (isSupported(n)) {
                if (isName(n)) {
                    String tokens = parseTextName(n);
                    a.setTokens(Integer.parseInt(tokens));
                } else if (Tags.ARC_TYPE.equals(n.getNodeName())) {
                    a.setArctype(n.getTextContent().trim().trim());
                }
            }
        }

        Node sourceNode = arc.getAttributes().getNamedItem(Attributes.SOURCE);
        if (sourceNode == null) {
            throw new DPNParserException("\"" + Tags.ARC + "\" tag must have a \"" + Attributes.SOURCE + "\" attribute");
        }
        String sourceName = sourceNode.getTextContent().trim();

        if (dpn.getPlaces().containsKey(sourceName)) {
            a.setInput(true);
        } else if (dpn.getTransitions().containsKey(sourceName)) {
            a.setInput(false);
        } else {
            throw new DPNParserException("\"" + sourceName + "\" in arc \"" + a.getName() + "\" isn't a valid place nor transition");
        }
        a.setSource(sourceName);

        Node targetNode = arc.getAttributes().getNamedItem(Attributes.TARGET);
        if (targetNode == null) {
            throw new DPNParserException("\"" + Tags.ARC + "\" tag must have a \"" + Attributes.TARGET + "\" attribute");
        }
        String targetName = targetNode.getTextContent().trim();
        if (!dpn.getPlaces().containsKey(targetName) && !dpn.getTransitions().containsKey(targetName)) {
            throw new DPNParserException("\"" + targetName + "\" in arc \"" + a.getName() + "\" isn't a valid place nor transition");
        }
        a.setTarget(targetName);

        return a;
    }

    private Marking parseMarking(Node initMarking, int placesNumber) throws DPNParserException {
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
                        int tokenNumber = Integer.parseInt(parseTextName(sub_node));
                        tokenNumber = Math.max(tokenNumber, 0);
                        m.addPlaceWithToken(refPlace.getTextContent().trim(), tokenNumber);
                    }
                }
            }
        }
        if (m.getPlaceTokenMap().size() != placesNumber) {
            throw new DPNParserException("Tag \"" + Tags.MARKING + "\" must contain as much elements as the number of places");
        }
        return m;
    }

    private void parseVariables(Node variables) throws DPNParserException {
        for (int i = 0; i < variables.getChildNodes().getLength(); i++) {
            Node n = variables.getChildNodes().item(i);
            if (isSupported(n) && Tags.VARIABLE.equals(n.getNodeName())) {
                dpn.addVariable(parseVariable(n));
            }
        }
    }

    private Variable parseVariable(Node variable) throws DPNParserException {
        Variable v = new Variable();
        for (int i = 0; i < variable.getChildNodes().getLength(); i++) {
            Node n = variable.getChildNodes().item(i);
            if (isSupported(n)) {
                if (isName(n)) {
                    String name = parseTextName(n);
                    if (!isValidVariableName(name)) {
                        throw new DPNParserException("Invalid name for variable " + name);
                    }
                    v.setName(name);
                } else if (isGraphics(n)) {
                    v.setGraphics(parseGraphics(n));
                }
            }
        }

        Node minValueNode = variable.getAttributes().getNamedItem(Attributes.MIN_VALUE);
        long minValue = minValueNode == null ? Long.MIN_VALUE : Long.parseLong(minValueNode.getTextContent().trim().trim());
        v.setMinValue(minValue);

        Node maxValueNode = variable.getAttributes().getNamedItem(Attributes.MAX_VALUE);
        long maxValue = maxValueNode == null ? Long.MAX_VALUE : Long.parseLong(maxValueNode.getTextContent().trim().trim());
        if (maxValue < minValue) {
            throw new DPNParserException(Attributes.MAX_VALUE + " lower than " + Attributes.MIN_VALUE + " detected for variable " + v.getName());
        }
        v.setMaxValue(maxValue);

        Node initialValueNode = variable.getAttributes().getNamedItem(Attributes.INITIAL_VALUE);
        long initialValue = initialValueNode == null ? 0L : Long.parseLong(initialValueNode.getTextContent().trim().trim());
        if (initialValue < minValue || initialValue > maxValue) {
            throw new DPNParserException(Attributes.INITIAL_VALUE + " not in range [" + minValue + ", " + maxValue + "] variable " + v.getName());
        }
        v.setInitialValue(initialValue);
        return v;
    }

    private String parseTextName(Node name) throws DPNParserException {
        for (int i = 0; i < name.getChildNodes().getLength(); i++) {
            Node n = name.getChildNodes().item(i);
            if (isSupported(n) && Tags.TEXT.equals(n.getNodeName())) {
                return n.getTextContent().trim();
            }
        }
        throw new DPNParserException("Expected tag \"" + Tags.TEXT + "\" inside \"" + Tags.NAME + "\" tag");
    }

    private void addZed() {
        Variable zed = new Variable();
        zed.setName("Z");
        zed.setInitialValue(0L);
        dpn.addVariable(zed);
    }

    private void checkConstraintVariables() throws DPNParserException {
        for (Transition t : dpn.getTransitions().values()) {
            Constraint c = t.getGuard();
            boolean firstOk = dpn.getVariables().containsKey(c.getFirst());
            boolean secondOk = dpn.getVariables().containsKey(c.getSecond());
            if (!firstOk) {
                throw new DPNParserException("Variable \"" + c.getFirst() + "\" is not defined for transition " + t.getId());
            }
            if (!secondOk) {
                throw new DPNParserException("Variable \"" + c.getSecond() + "\" is not defined for transition " + t.getId());
            }
        }
    }

    private void fillTransitionsWithArcs() {
        for (Arc arc : dpn.getArcs()) {
            Transition t = null;
            if (arc.isInput()) {
                t = dpn.getTransitions().get(arc.getTarget());
                t.addEnabling(arc.getSource(), arc.getTokens());
            }else {
                t = dpn.getTransitions().get(arc.getSource());
                t.addOutput(arc.getTarget(), arc.getTokens());
            }
        }
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

    private boolean isValidVariableName(String id) {
        Pattern p = Pattern.compile("^[a-z][a-z0-9_]*$");
        Matcher m = p.matcher(id);
        return m.matches();
    }
}
