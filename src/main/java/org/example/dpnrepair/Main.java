package org.example.dpnrepair;

import org.example.dpnrepair.parser.DPNParser;
import org.example.dpnrepair.exceptions.DPNParserException;

import org.example.dpnrepair.parser.ast.*;
import org.example.dpnrepair.semantics.ConstraintGraph;
import org.example.dpnrepair.semantics.DPNRepairAcyclic;
import org.example.dpnrepair.semantics.DPNRepairCyclic;
import org.example.dpnrepair.semantics.DifferenceConstraintSet;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.*;


public class Main {
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, DPNParserException, TransformerException, InvalidConfigurationException, InvalidConfigurationException {

//        String figure = "src/main/resources/figure1-dpn.pnml";
//        String figure = "src/main/resources/figure-2-dpn.pnml";
//        String figure = "src/main/resources/figure-3-dpn.pnml";
//        String figure = "src/main/resources/figure-4-dpn.pnml";
//        String figure = "src/main/resources/wf-1-deadlock-dpn.pnml";
//        String figure = "src/main/resources/wf-1-deadlock-free-dpn.pnml";
//        String figure = "src/main/resources/wf-2-miss-trans-dpn.pnml";
//        String figure = "src/main/resources/wf-3-deadlock-dpn.pnml";
        String figure = "src/main/resources/img-prof-1-dpn.pnml";
//        String figure = "src/main/resources/img-prof-no-ciclo-dpn.pnml";
        DPNParser parser = new DPNParser(figure);
        parser.parse();
//        System.out.println("HAS CYCLES: " + DPNUtils.hasCycles(parser.getDpn()));
//        ConstraintGraph cg = new ConstraintGraph(parser.getDpn());
//        ConstraintGraphPrinter cgPrinter = new ConstraintGraphPrinter(cg);
//        cgPrinter.writeToXML("img-prof-1-graph", false);
//        cgPrinter.writeRaw("img-prof-1-graph-raw.txt");
//        DPNRepairAcyclic dpnRepairAcyclic = new DPNRepairAcyclic(parser.getDpn());
//        dpnRepairAcyclic.repair();
        DifferenceConstraintSet a = CanonicalFormUtilities.getCanonicalForm(getC6());
        DPNRepairCyclic dpnRepairCyclic = new DPNRepairCyclic(parser.getDpn(), SmtSolverFactory.getZ3SmtSolver());
        DifferenceConstraintSet b = CanonicalFormUtilities.getCanonicalForm(getC5());
        boolean res  = dpnRepairCyclic.areDifferenceConstraintSetsEqual(new HashSet<>(Collections.singleton(a)),a );
        dpnRepairCyclic.repair();

//        DifferenceConstraintSet res = dpnRepairCyclic.computeExists(Collections.singletonList("y"), a);
//        DifferenceConstraintSet res2 = CanonicalFormUtilities.getCanonicalForm(dpnRepairCyclic.intersect(res, b));

//        dpnRepairCyclic.repair();
//        ConstraintGraph cg2 = new ConstraintGraph(dpnRepairCyclic.getRepaired());
        System.out.println("Finished");
    }

    private static DifferenceConstraintSet getC6() {

        Variable x = new Variable();
        x.setName("x");
        Variable y = new Variable();
        y.setName("y");
        Variable z = new Variable();
        z.setName("Z");

        Constraint first = new Constraint();
        first.setFirst(z.getName());
        first.setSecond(x.getName());
        first.setValue(0);
        first.setStrict(false);

        Constraint second = new Constraint();
        second.setFirst(y.getName());
        second.setSecond(x.getName());
        second.setValue(10);
        second.setStrict(true);

        Constraint third = new Constraint();
        third.setFirst(x.getName());
        third.setSecond(z.getName());
        third.setValue(10);
        third.setStrict(true);

        Constraint forth = new Constraint();
        forth.setFirst(y.getName());
        forth.setSecond(z.getName());
        forth.setValue(10);
        forth.setStrict(true);

        Constraint fifth = new Constraint();
        fifth.setFirst(x.getName());
        fifth.setSecond(y.getName());
        fifth.setValue(0);
        fifth.setStrict(true);

        Constraint sixth = new Constraint();
        sixth.setFirst(z.getName());
        sixth.setSecond(y.getName());
        sixth.setValue(0);
        sixth.setStrict(true);

        Map<String, Variable> variableMap = new HashMap<>();
        variableMap.put(x.getName(), x);
        variableMap.put(y.getName(), y);
        variableMap.put(z.getName(), z);

        return new DifferenceConstraintSet(new HashSet<>(Arrays.asList(first, second, third, forth, fifth, sixth)), variableMap);
    }

    private static DifferenceConstraintSet getC5() {
        Variable x = new Variable();
        x.setName("x");
        Variable y = new Variable();
        y.setName("y");
        Variable z = new Variable();
        z.setName("Z");

        Constraint first = new Constraint();
        first.setFirst(z.getName());
        first.setSecond(x.getName());
        first.setValue(0);
        first.setStrict(false);

        Constraint second = new Constraint();
        second.setFirst(x.getName());
        second.setSecond(y.getName());
        second.setValue(0);
        second.setStrict(true);

        Constraint third = new Constraint();
        third.setFirst(z.getName());
        third.setSecond(y.getName());
        third.setValue(-10);
        third.setStrict(false);

        Map<String, Variable> variableMap = new HashMap<>();
        variableMap.put(x.getName(), x);
        variableMap.put(y.getName(), y);
        variableMap.put(z.getName(), z);

        return new DifferenceConstraintSet(new HashSet<>(Arrays.asList(first, second, third)), variableMap);
    }
}