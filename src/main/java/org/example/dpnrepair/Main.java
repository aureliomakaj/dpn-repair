package org.example.dpnrepair;

import org.example.dpnrepair.parser.DPNParser;
import org.example.dpnrepair.exceptions.DPNParserException;

import org.example.dpnrepair.parser.ast.*;
import org.example.dpnrepair.semantics.*;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;


public class Main {
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, DPNParserException, TransformerException, InvalidConfigurationException {
        String filePath = null;
        boolean cyclic = false;
        boolean writeOutputFiles = true;
        // Check if there are arguments passed
        if (args.length > 0) {
            // Loop through the arguments to find the "--filename" parameter and its value
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("--file-path")) {
                    filePath = args[i + 1];
                }
                if (args[i].equals("--cyclic")) {
                    cyclic = true;
                }
                if (args[i].equals("--no-output-files")) {
                    writeOutputFiles = false;
                }

            }
        } else {
            System.out.println("No command-line arguments provided.");
        }

        if (filePath == null) {
            System.out.println("No model specified");
            return;
        }

        DPNParser parser = new DPNParser(filePath);
        parser.parse();

        ConstraintGraph cg = new ConstraintGraph(parser.getDpn());

        if (writeOutputFiles) {
            ConstraintGraphPrinter cgp = new ConstraintGraphPrinter(filePath, cg);
            cgp.writeRaw();
        }

        long startTime = System.nanoTime();
        DPNRepair repair;
        if (!cyclic) {
            repair = new DPNRepairAcyclic(parser.getDpn());
        } else {
            repair = new DPNRepairCyclic(parser.getDpn(), SmtSolverFactory.getSmtSolver());
        }
        repair.repair();
        long endTime = System.nanoTime();
        long durationInNano = endTime - startTime;
        double durationInSeconds = (double) durationInNano / 1_000_000_000.0;
        ConstraintGraph cg2 = new ConstraintGraph(repair.getRepaired());

        if (writeOutputFiles) {
            ConstraintGraphPrinter cgp2 = new ConstraintGraphPrinter(filePath, cg2, true);
            cgp2.writeRaw();
            cgp2.writeToXML();
        }

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