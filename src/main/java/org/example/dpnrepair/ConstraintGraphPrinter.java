package org.example.dpnrepair;

import org.example.dpnrepair.parser.ast.Constraint;
import org.example.dpnrepair.semantics.ConstraintGraph;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class ConstraintGraphPrinter {
    private static final String ID = "id";
    private final ConstraintGraph cg;
    private final String path;
    private boolean repaired = false;

    public ConstraintGraphPrinter(String path, ConstraintGraph cg) {
        this.cg = cg;
        this.path = path;
    }

    public ConstraintGraphPrinter(String path, ConstraintGraph cg, boolean repaired) {
        this.cg = cg;
        this.path = path;
        this.repaired = repaired;
    }

    public void writeToXML() throws ParserConfigurationException, TransformerException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        // root elements
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("graphml");
        rootElement.setAttribute("xmlns", "http://graphml.graphdrawing.org/xmlns");
        rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        rootElement.setAttribute("xsi:schemaLocation", "http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd");
        doc.appendChild(rootElement);

        Element nodeKey = doc.createElement("key");
        nodeKey.setAttribute("id", "node_key");
        nodeKey.setAttribute("for", "node");
        nodeKey.setAttribute("attr.type", "string");
        nodeKey.setAttribute("attr.name", "label");
        rootElement.appendChild(nodeKey);

        Element edgeKey = doc.createElement("key");
        edgeKey.setAttribute("id", "edge_key");
        edgeKey.setAttribute("for", "edge");
//        edgeKey.setAttribute("attr.name", "weight");
        edgeKey.setAttribute("attr.type", "string");
        rootElement.appendChild(edgeKey);

        Element graph = doc.createElement("graph");
        graph.setAttribute("id", "G");
        graph.setAttribute("edgedefault", "directed");
        rootElement.appendChild(graph);

        Map<Integer, String> nodePlaceStr = cg.getNodes().stream().collect(Collectors.toMap(ConstraintGraph.Node::getId, node -> node.getMarking().getPlaceStringRepresentation()));

        for (ConstraintGraph.Node node: cg.getNodes()) {
            Element nodeElement = doc.createElement("node");
            nodeElement.setAttribute("id", String.valueOf(node.getId()));

            Element subElement = doc.createElement("data");
            subElement.setAttribute("key", "node_key");
            subElement.setTextContent(nodePlaceStr.get(node.getId()));
            nodeElement.appendChild(subElement);
            graph.appendChild(nodeElement);
        }

        for (ConstraintGraph.Arc arc: cg.getArcs()) {
            Element edgeElement = doc.createElement("edge");
            edgeElement.setAttribute("source", String.valueOf(arc.getOrigin()));
            edgeElement.setAttribute("target", String.valueOf(arc.getDestination()));

            Element subElement = doc.createElement("data");
            subElement.setAttribute("key", "edge_key");
            String content = arc.isSilent() ? "tau_" : "";
            subElement.setTextContent(content.concat(arc.getTransition()));
            edgeElement.appendChild(subElement);
            graph.appendChild(edgeElement);
        }

        File originalFile = new File(path);
        String parentDirectory = originalFile.getParent();
        String repaired = this.repaired ? "-repaired" : "";
        File newFile = new File(parentDirectory, originalFile.getName() + repaired + "-cg.graphml");
        try (FileOutputStream output = new FileOutputStream(newFile)) {
            writeXml(doc, output);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // write doc to output stream
    private static void writeXml(Document doc,
                                 OutputStream output)
            throws TransformerException {

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(output);

        transformer.transform(source, result);

    }

    /**
     * Just write the nodes (marking + constraint graph) in a file
     */
    public void writeRaw() {
        String separator = "------------------------\n";
        List<ConstraintGraph.Node> nodeList = new ArrayList<>(cg.getNodes());
        nodeList.sort(Comparator.comparingInt(ConstraintGraph.Node::getId));

        StringBuilder content = new StringBuilder("************* CONSTRAINT GRAPH ***************** \n \n \n");
        for (ConstraintGraph.Node node: nodeList) {
            content.append(separator);
            String markingStr = node
                    .getMarking()
                    .getPlaceStringRepresentation();

            content.append("NodeId ").append(node.getId()).append("\n");
            content.append("Marking ").append(markingStr).append("\n");
            content.append(separator);
            for (Constraint c: node.getCanonicalForm().getConstraintSet()) {
                content.append(c.toString()).append("\n");
            }
            content.append(separator);
            content.append("\n\n\n");
        }
        File originalFile = new File(path);
        String parentDirectory = originalFile.getParent();
        String repaired = this.repaired ? "-repaired" : "";
        File newFile = new File(parentDirectory, originalFile.getName() + repaired + "-cg-raw.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(newFile))) {
            writer.write(content.toString());
            writer.close();
            System.out.println("Content written to the file successfully.");
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the file: " + e.getMessage());
        }
    }
}
