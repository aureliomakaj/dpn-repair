package org.example.dpnrepair;

import org.example.dpnrepair.parser.ast.Constraint;
import org.example.dpnrepair.parser.ast.DPN;
import org.example.dpnrepair.parser.ast.Transition;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DPNPrinter {
    private static final String ID = "id";
    private DPN dpn;

    public DPNPrinter(DPN cg) {
        this.dpn = cg;
    }

    public void writeTransitions(String filename) {
        String separator = "------------------------\n";
        List<Transition> nodeList = new ArrayList<>(dpn.getTransitions().values());
        nodeList.sort(Comparator.comparing(Transition::getName));
        StringBuilder content = new StringBuilder("************* CONSTRAINT GRAPH ***************** \n \n \n");
        for (Transition transition: nodeList) {
            content.append(separator);

            content.append("Transition ").append(transition.getName()).append("\n");
            content.append(separator);
            content.append(transition.getGuard().toStringWithoutZed());
            content.append("\n");
            content.append(separator);
            content.append("\n\n\n");
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(content.toString());
            System.out.println("Content written to the file successfully.");
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the file: " + e.getMessage());
        }
    }
}
