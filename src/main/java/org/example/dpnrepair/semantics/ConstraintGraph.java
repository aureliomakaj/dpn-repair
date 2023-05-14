package org.example.dpnrepair.semantics;

import org.example.dpnrepair.CanonicalFormUtilities;
import org.example.dpnrepair.exceptions.DPNParserException;
import org.example.dpnrepair.parser.GuardToConstraintConverter;
import org.example.dpnrepair.parser.ast.*;

import java.util.*;
import java.util.stream.Collectors;

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
        visitDpn(dpn);
    }

    private void visitDpn(DPN dpn) {
        Queue<Node> queue = new ArrayDeque<>();
        queue.add(initialNode);
        while (!queue.isEmpty()) {
            Node iter = queue.remove();
            for (Transition enabledTransition : getEnabledTransitions(dpn.getTransitions().values(), iter.getMarking())) {
                Arc arc = new Arc();
                arc.origin = iter.id;
                arc.transition = enabledTransition.getId();
                // Compute new marking
                Marking nextMarking = iter.getMarking().clone();
                nextMarking.removeTokens(enabledTransition.getEnabling());
                nextMarking.addTokens(enabledTransition.getOutput());
                // Guard holds
                DifferenceConstraintSet newDiffSetCanonical = CanonicalFormUtilities.addConstraint(
                        iter.getCanonicalForm(), enabledTransition.getGuard(), dpn.getVariables()
                );

                if(newDiffSetCanonical != null) {
                    addNode(nextMarking, newDiffSetCanonical, arc, queue);
                }

                if(enabledTransition.getGuard().getWritten().size() == 0) {
                    Arc silentArc = new Arc();
                    silentArc.origin = iter.id;
                    silentArc.transition = enabledTransition.getId();
                    silentArc.silent = true;
                    // Silent transition
                    DifferenceConstraintSet silentSetCanonical = CanonicalFormUtilities.addConstraint(
                            iter.getCanonicalForm(), enabledTransition.getGuard().getNegated(), dpn.getVariables()
                    );

                    if(silentSetCanonical != null) {
                        addNode(iter.getMarking().clone(), silentSetCanonical, silentArc, queue);
                    }
                }
            }
        }
    }

    private void addNode(Marking marking, DifferenceConstraintSet differenceConstraintSet, Arc arc, Queue<Node> queue) {
        Node newNode = new Node(marking, differenceConstraintSet);
        Optional<Node> optionalInserted = getIfAlreadyInserted(newNode);
        if (optionalInserted.isPresent()) {
            arc.destination = optionalInserted.get().id;
            arcs.add(arc);
        } else {
            queue.add(newNode);
            nodes.add(newNode);
            arc.destination = newNode.id;
            arcs.add(arc);
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

    private List<Transition> getEnabledTransitions(Collection<Transition> transitions, Marking marking) {
        return transitions.stream()
                .filter(transition -> transition.isEnabled(marking))
                .collect(Collectors.toList());
    }

    private Optional<Node> getIfAlreadyInserted(Node node) {
        return nodes.stream().filter(n -> n.equals(node)).findFirst();
    }

    class Node {
        private final int id;
        private final Marking marking;
        private final DifferenceConstraintSet canonicalForm;
        private boolean visited = false;
        private boolean finalNode = false;
        private boolean deadNode = false;

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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return Objects.equals(marking, node.marking) && Objects.equals(canonicalForm, node.canonicalForm);
        }

        @Override
        public int hashCode() {
            return Objects.hash(marking, canonicalForm);
        }
    }

    class Arc {
        private int origin;
        private String transition;
        private int destination;
        private boolean silent = false;
        private EdgeType type;
    }

    enum EdgeType {
        DISCOVERY_EDGE,
        BACK_EDGE
    }
}
