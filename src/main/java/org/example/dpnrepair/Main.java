package org.example.dpnrepair;

import org.example.dpnrepair.parser.DPNParser;
import org.example.dpnrepair.exceptions.DPNParserException;

import org.example.dpnrepair.parser.ast.DPN;
import org.example.dpnrepair.parser.ast.Dimension;
import org.example.dpnrepair.parser.ast.Graphics;
import org.example.dpnrepair.parser.ast.Position;
import org.example.dpnrepair.semantics.ConstraintGraph;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Objects;


public class Main {
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, DPNParserException {

        DPNParser parser = new DPNParser("src/main/resources/figure1-dpn.pnml");
        parser.parse();
        DPN d = parser.getDpn().clone();
        ConstraintGraph cg = new ConstraintGraph(parser.getDpn());
        System.out.println("Finished");
    }
}