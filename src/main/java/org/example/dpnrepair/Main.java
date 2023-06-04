package org.example.dpnrepair;

import org.example.dpnrepair.parser.DPNParser;
import org.example.dpnrepair.exceptions.DPNParserException;

import org.example.dpnrepair.parser.ast.DPN;
import org.example.dpnrepair.parser.ast.Dimension;
import org.example.dpnrepair.parser.ast.Graphics;
import org.example.dpnrepair.parser.ast.Position;
import org.example.dpnrepair.semantics.ConstraintGraph;
import org.example.dpnrepair.semantics.DPNRepairAcyclic;
import org.example.dpnrepair.semantics.DPNRepairCyclic;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.Objects;


public class Main {
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, DPNParserException, TransformerException {

        String figure = "src/main/resources/figure1-dpn.pnml";
//        String figure = "src/main/resources/figure-2-dpn.pnml";
//        String figure = "src/main/resources/figure-3-dpn.pnml";
//        String figure = "src/main/resources/figure-4-dpn.pnml";
        DPNParser parser = new DPNParser(figure);
        parser.parse();
//        ConstraintGraph cg = new ConstraintGraph(parser.getDpn());
//        ConstraintGraphPrinter cgPrinter = new ConstraintGraphPrinter(cg);
//        cgPrinter.writeRaw("figure-3-cg.txt");
//        DPNRepairAcyclic dpnRepairAcyclic = new DPNRepairAcyclic(parser.getDpn());
//        dpnRepairAcyclic.repair();
        DPNRepairCyclic dpnRepairCyclic = new DPNRepairCyclic(parser.getDpn());
        dpnRepairCyclic.repair();
        ConstraintGraph cg2 = new ConstraintGraph(dpnRepairCyclic.getRepaired());
        System.out.println("Finished");
    }
}