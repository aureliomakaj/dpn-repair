package org.example.dpnrepair.semantics;

import org.example.dpnrepair.CanonicalFormUtilities;
import org.example.dpnrepair.exceptions.DPNParserException;
import org.example.dpnrepair.parser.GuardToConstraintConverter;
import org.example.dpnrepair.parser.ast.*;

import java.util.*;

public class ConstraintGraph {
    private int nodeCounter = 0;
    private List<Node> nodes;
    private Node initialNode;
    private List<Arc> arcs;


    public ConstraintGraph(DPN dpn) {
        nodes = new ArrayList<>();
        initialNode = null;
        arcs = new ArrayList<>();
        computeGraph(dpn);
    }

    private void computeGraph(DPN dpn) {
        initialNode = computeInitialNode(dpn);
        nodes.add(initialNode);
    }

    private void visitDpn(DPN dpn) {
        Queue<Node> queue = new ArrayDeque<>();
        queue.add(initialNode);
        while (!queue.isEmpty()) {
            Node iter = queue.remove();

        }
    }

    private Node computeInitialNode(DPN dpn) {
        Set<Constraint> initialConstraintSet = new HashSet<>();
        for (Variable v : dpn.getVariables().values()) {
            List<Constraint> assignment = null;
            try {
                assignment = GuardToConstraintConverter.convertEquality(v.getName() + " = " + v.getInitialValue(), new ArrayList<>(), Arrays.asList(v.getName()));
                initialConstraintSet.addAll(assignment);
            } catch (DPNParserException e) {
                // Hand made string, shouldn't reach this point
            }
        }
        DifferenceConstraintSet initCanonicalForm = CanonicalFormUtilities.getCanonicalForm(
                new DifferenceConstraintSet(initialConstraintSet, dpn.getVariables())
        );
        return new Node(dpn.getInitialMarking(), initCanonicalForm);
    }

    class Node {
        private int id;
        private String place;
        private final Marking marking;
        private final DifferenceConstraintSet canonicalForm;
        private boolean visited = false;

        public Node(Marking marking, DifferenceConstraintSet canonicalForm) {
            this.id = ++nodeCounter;
            this.marking = marking;
            this.canonicalForm = canonicalForm;
        }

        public Marking getMarking() {
            return marking;
        }

        public DifferenceConstraintSet getCanonicalForm() {
            return canonicalForm;
        }

        public boolean isVisited() {
            return visited;
        }

        public void setVisited(boolean visited) {
            this.visited = visited;
        }
    }

    class Arc {
        private Node origin;
        private Transition t;
        private Node destination;
        private EdgeType type;
    }

    enum EdgeType {
        DISCOVERY_EDGE,
        BACK_EDGE
    }
}
