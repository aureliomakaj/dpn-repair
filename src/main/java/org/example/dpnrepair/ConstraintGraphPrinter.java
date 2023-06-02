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
    private ConstraintGraph cg;

    public ConstraintGraphPrinter(ConstraintGraph cg) {
        this.cg = cg;
    }

    public void writeToXML(String filename) throws ParserConfigurationException, IOException, TransformerException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        // root elements
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("graphml");
        rootElement.setAttribute("xmlns", "http://graphml.graphdrawing.org/xmlns");
        rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        rootElement.setAttribute("xsi:schemaLocation", "http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd");
        doc.appendChild(rootElement);

        Element edgeKey = doc.createElement("key");
        edgeKey.setAttribute("id", "edge_key");
        edgeKey.setAttribute("for", "edge");
        edgeKey.setAttribute("attr.name", "weight");
        edgeKey.setAttribute("attr.type", "string");
        rootElement.appendChild(edgeKey);

        Element graph = doc.createElement("graph");
        graph.setAttribute("id", "G");
        graph.setAttribute("edgedefault", "directed");
        rootElement.appendChild(graph);

        for (ConstraintGraph.Node node: cg.getNodes()) {
            Element nodeElement = doc.createElement("node");
            nodeElement.setAttribute("id", String.valueOf(node.getId()));
            String content = node.getMarking().getPlaceTokenMap().entrySet().stream().filter(e -> e.getValue() != 0).map(Map.Entry::getKey).collect(Collectors.joining(", "));
            for (Constraint c: node.getCanonicalForm().getConstraintSet()) {
                content = content.concat("\n").concat(c.toString());
            }
            nodeElement.setTextContent(content);
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


        try (FileOutputStream output = new FileOutputStream(filename.concat(".graphml"))) {
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
     * @param filename
     */
    public void writeRaw(String filename) {
        String separator = "------------------------\n";
        List<ConstraintGraph.Node> nodeList = cg.getNodes().stream().collect(Collectors.toList());
        nodeList.sort(Comparator.comparingInt(ConstraintGraph.Node::getId));

        StringBuilder content = new StringBuilder("************* CONSTRAINT GRAPH ***************** \n \n \n");
        for (ConstraintGraph.Node node: nodeList) {
            content.append(separator);
            String markingStr = node
                    .getMarking()
                    .getPlaceTokenMap()
                    .entrySet()
                    .stream()
                    .filter(e -> e.getValue() != 0)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.joining(", "));

            content.append("NodeId ").append(node.getId()).append("\n");
            content.append("Marking ").append(markingStr).append("\n");
            content.append(separator);
            for (Constraint c: node.getCanonicalForm().getConstraintSet()) {
                content.append(c.toString()).append("\n");
            }
            content.append(separator);
            content.append("\n\n\n");
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(content.toString());
            writer.close();
            System.out.println("Content written to the file successfully.");
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the file: " + e.getMessage());
        }
    }
}
