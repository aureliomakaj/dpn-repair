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
        String figure = args[0];
        DPNParser parser = new DPNParser(figure);
        parser.parse();
        ConstraintGraph cg = new ConstraintGraph(parser.getDpn());
        long startTime = System.nanoTime();
//        DPNRepairAcyclic repair = new DPNRepairAcyclic(parser.getDpn());
        DPNRepairCyclic repair = new DPNRepairCyclic(parser.getDpn(), SmtSolverFactory.getSmtSolver());
        repair.repair();
        long endTime = System.nanoTime();
        long durationInNano = endTime - startTime;
        double durationInSeconds = (double) durationInNano / 1_000_000_000.0;

        System.out.println("Finished with the following details:");
        System.out.println("- Distance: " + repair.getDistance());
        System.out.println("- Execution time: " + durationInSeconds + " seconds");
        System.out.println("- Iterations: " + repair.getIterations());
        System.out.println("- Priority queue size: " + repair.getPriorityQueue().size());
        System.out.println("- Visited DPNs: " + repair.getVisitedDpn().size());
        System.out.println("*** Modified guards: ");
        for (String guard : repair.getModifiedTransitions()) {
            Transition original = parser.getDpn().getTransitions().get(guard);
            Transition modified = repair.getRepaired().getTransitions().get(guard);
            System.out.println("Guard [" + guard + "] from (" + original.getGuard() + ") to (" + modified.getGuard() + ")");
        }
    }
}