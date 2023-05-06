package org.example.dpnrepair.semantics;

import org.example.dpnrepair.parser.ast.DPN;
import org.example.dpnrepair.parser.ast.Marking;
import org.example.dpnrepair.parser.ast.Transition;

import java.util.ArrayList;
import java.util.List;

public class ConstraintGraph {

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

    }

    class Node {
        private Marking marking;
        private DifferenceConstraintSet canonicalForm;
        private boolean visited = false;
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
