package org.example.dpnrepair.semantics;

import org.example.dpnrepair.CanonicalFormUtilities;
import org.example.dpnrepair.DPNUtils;
import org.example.dpnrepair.exceptions.DPNParserException;
import org.example.dpnrepair.parser.GuardToConstraintConverter;
import org.example.dpnrepair.parser.ast.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class for representing the Constraint Graph of a DPN
 */
public class ConstraintGraph {
    private int nodeCounter = 0;
    private Set<Node> nodes;
    private Node initialNode;
    private Set<Arc> arcs;

    private boolean dataAwareSound = true;

    private boolean unbounded = false;
    private List<Integer> deadlocks = new ArrayList<>();

    public int getNodeCounter() {
        return nodeCounter;
    }

    public Set<Node> getNodes() {
        return nodes;
    }

    public Node getInitialNode() {
        return initialNode;
    }

    public Set<Arc> getArcs() {
        return arcs;
    }

    public boolean isUnbounded() {
        return unbounded;
    }

    public List<Integer> getDeadlocks() {
        return deadlocks;
    }

    public void setNodeCounter(int nodeCounter) {
        this.nodeCounter = nodeCounter;
    }

    public void setNodes(Set<Node> nodes) {
        this.nodes = nodes;
    }

    public void setInitialNode(Node initialNode) {
        this.initialNode = initialNode;
    }

    public void setArcs(Set<Arc> arcs) {
        this.arcs = arcs;
    }

    public void setDataAwareSound(boolean dataAwareSound) {
        this.dataAwareSound = dataAwareSound;
    }

    public void setUnbounded(boolean unbounded) {
        this.unbounded = unbounded;
    }

    public void setDeadlocks(List<Integer> deadlocks) {
        this.deadlocks = deadlocks;
    }

    public ConstraintGraph(DPN dpn) {
        nodes = new HashSet<>();
        initialNode = null;
        arcs = new HashSet<>();
        computeGraph(dpn);
    }

    private void computeGraph(DPN dpn) {
        initialNode = computeInitialNode(dpn);
        nodes.add(initialNode);
        visitDpn(dpn);
    }

    /**
     * Starting from the initial node, visit the DPN and check if it is
     * data-aware sound
     * @param dpn
     */
    private void visitDpn(DPN dpn) {
        // Nodes found but still to be expanded
        Queue<Node> queue = new ArrayDeque<>();
        queue.add(initialNode);
        while (!queue.isEmpty()) {
            // Pick a node
            Node iter = queue.remove();
            List<Transition> enabledTransitions = DPNUtils.getEnabledTransitions(dpn.getTransitions().values(), iter.getMarking());
            for (Transition enabledTransition : enabledTransitions) {
                Arc arc = new Arc();
                arc.origin = iter.id;
                arc.transition = enabledTransition.getId();

                // Compute new marking
                Marking nextMarking = iter.getMarking().clone();
                nextMarking.removeTokens(enabledTransition.getEnabling());
                nextMarking.addTokens(enabledTransition.getOutput());

                // Guard holds case
                DifferenceConstraintSet newDiffSetCanonical = CanonicalFormUtilities.addConstraint(
                        iter.getCanonicalForm(), enabledTransition.getGuard(), dpn.getVariables()
                );

                // Proceed only if constraint graph is consistent.
                if (newDiffSetCanonical != null) {
                    // If still data-aware sound, check for unboundness
                    if (dataAwareSound && isUnbounded(nextMarking, newDiffSetCanonical)) {
                        // The net is unbounded
                        unbounded = true;
                        dataAwareSound = false;
                    }
                    // Add node and arc
                    fillQueue(nextMarking, newDiffSetCanonical, arc, queue, dpn.getFinalMarking());
                }

                // If transition doesn't write any variable
                if (enabledTransition.getGuard().getWritten().size() == 0) {
                    // Silent transition
                    Arc silentArc = new Arc();
                    silentArc.origin = iter.id;
                    silentArc.transition = enabledTransition.getId();
                    silentArc.silent = true;

                    // Difference constraint set by using negated guard
                    DifferenceConstraintSet silentSetCanonical = CanonicalFormUtilities.addConstraint(
                            iter.getCanonicalForm(), enabledTransition.getGuard().getNegated(), dpn.getVariables()
                    );

                    // Proceed only if consistent
                    if (silentSetCanonical != null) {
                        fillQueue(iter.getMarking().clone(), silentSetCanonical, silentArc, queue, dpn.getFinalMarking());
                    }
                }
            }
        }

        findDeadlocks();
        if (!deadlocks.isEmpty()) {
            this.dataAwareSound = false;
        }
    }

    private void fillQueue(Marking marking, DifferenceConstraintSet differenceConstraintSet, Arc arc, Queue<Node> queue,
                           Marking finalMarking) {
        Node newNode = new Node(marking, differenceConstraintSet);
        if (marking.equals(finalMarking)) {
            newNode.finalNode = true;
        }
        Optional<Node> optionalInserted = getAlreadyInserted(newNode);
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
                assignment = GuardToConstraintConverter.convertEquality(
                        v.getName() + " = " + v.getInitialValue(), new ArrayList<>(), Collections.singletonList(v.getName())
                );
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

    private Optional<Node> getAlreadyInserted(Node node) {
        return nodes.stream().filter(n -> n.equals(node)).findFirst();
    }

    //∃(M* ,C*) ∈ S s.t. m > M*  AND canonicalForm = C*
    private boolean isUnbounded(Marking m, DifferenceConstraintSet canonicalForm) {
        return nodes.stream()
                .anyMatch(n -> m.greaterThan(n.getMarking()) && canonicalForm.equals(n.getCanonicalForm()));
    }

    private void findDeadlocks() {
        Map<Integer, List<Arc>> outgoingArcsForNodes = new HashMap<>();
        for (Arc a : arcs) {
            outgoingArcsForNodes.putIfAbsent(a.origin, new ArrayList<>());
            // Add only if arc is self pointing
            if (a.origin != a.destination) {
                outgoingArcsForNodes.get(a.origin).add(a);
            }
        }
        this.deadlocks = outgoingArcsForNodes
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public boolean isDataAwareSound() {
        return dataAwareSound;
    }

    public class Node {
        private final int id;
        private final Marking marking;
        private final DifferenceConstraintSet canonicalForm;
        private boolean visited = false;
        private boolean finalNode = false;
        public Node(Marking marking, DifferenceConstraintSet canonicalForm) {
            this.id = ++nodeCounter;
            this.marking = marking;
            this.canonicalForm = canonicalForm;
        }

        public int getId() {
            return id;
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

    public class Arc {
        private int origin;
        private String transition;
        private int destination;
        private boolean silent = false;

        public int getOrigin() {
            return origin;
        }

        public String getTransition() {
            return transition;
        }

        public int getDestination() {
            return destination;
        }

        public boolean isSilent() {
            return silent;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Arc arc = (Arc) o;
            return origin == arc.origin && destination == arc.destination && silent == arc.silent && Objects.equals(transition, arc.transition);
        }

        @Override
        public int hashCode() {
            return Objects.hash(origin, transition, destination, silent);
        }
    }

    enum EdgeType {
        DISCOVERY_EDGE,
        BACK_EDGE
    }
}
