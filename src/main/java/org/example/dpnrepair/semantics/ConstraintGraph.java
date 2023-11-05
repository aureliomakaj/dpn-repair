package org.example.dpnrepair.semantics;

import org.example.dpnrepair.CanonicalFormUtilities;
import org.example.dpnrepair.DPNUtils;
import org.example.dpnrepair.exceptions.DPNParserException;
import org.example.dpnrepair.parser.GuardToConstraintConverter;
import org.example.dpnrepair.parser.ast.*;

import java.util.*;
import java.util.function.Function;
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
    private List<Integer> deadNodes = new ArrayList<>();

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

    public List<Integer> getDeadNodes() {
        return deadNodes;
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

    public void setDeadNodes(List<Integer> deadNodes) {
        this.deadNodes = deadNodes;
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
     *
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
                // Create an arc for each enabled transition, with current node as origin
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

                // Add only if constraint set is consistent.
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

                // If transition doesn't contain write variables
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

                    // Add only if constraint set is consistent
                    if (silentSetCanonical != null) {
                        fillQueue(iter.getMarking().clone(), silentSetCanonical, silentArc, queue, dpn.getFinalMarking());
                    }
                }
            }
        }
        findDeadNodes();
        if (!deadNodes.isEmpty()) {
            dataAwareSound = false;
        }

        if (dataAwareSound) {
            verifyLeftTokens();
        }
        if (dataAwareSound) {
            dataAwareSound = !hasMissingTransitions(dpn);
        }
    }

    private void verifyLeftTokens() {
        Set<Node> finals = nodes.stream().filter(Node::isFinal).collect(Collectors.toSet());
        for (Node n : nodes) {
            dataAwareSound = dataAwareSound && finals
                    .stream()
                    .noneMatch(finalNode -> n.getMarking().greaterThanOrEqual(finalNode.getMarking()) && !n.equals(finalNode));
        }
    }

    private void fillQueue(Marking marking, DifferenceConstraintSet differenceConstraintSet, Arc arc, Queue<Node> queue,
                           Marking finalMarking) {
        Node newNode = new Node(marking, differenceConstraintSet);
        if (marking.equals(finalMarking)) {
            newNode.finalNode = true;
        }
        // Check if the node has already been found
        Optional<Node> optionalInserted = getAlreadyInserted(newNode);
        if (optionalInserted.isPresent()) {
            // Add only the arc
            arc.destination = optionalInserted.get().id;
            arcs.add(arc);
        } else {
            // Add the new node and arc
            queue.add(newNode);
            nodes.add(newNode);
            arc.destination = newNode.id;
            arcs.add(arc);
        }
    }

    /**
     * Initial node is computed by considering all initial values (represented as x = k1, y = k2, ...)
     * and transforming these equalities in difference constraints
     */
    private Node computeInitialNode(DPN dpn) {
        Set<Constraint> initialConstraintSet = new HashSet<>();
        for (Variable v : dpn.getVariables().values()) {
            List<Constraint> assignment;
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

    private void findDeadNodes() {
        // Map each node with it's outgoing arcs
        Map<Integer, List<Arc>> outgoingArcsForNodes = new HashMap<>();
        for (Arc a : arcs) {
            outgoingArcsForNodes.putIfAbsent(a.origin, new ArrayList<>());
            // Add only if arc is self pointing
            if (a.origin != a.destination) {
                outgoingArcsForNodes.get(a.origin).add(a);
            }
        }

        // A dead node is just a node without outgoing arcs
        this.deadNodes = outgoingArcsForNodes
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public boolean isDataAwareSound() {
        return dataAwareSound;
    }

    private boolean hasMissingTransitions(DPN dpn) {
        Set<String> dpnTransitions = new HashSet<>(dpn.getTransitions().keySet());
        Set<String> graphTransitions = arcs.stream().filter(arc -> !arc.isSilent()).map(Arc::getTransition).collect(Collectors.toSet());
        return !graphTransitions.containsAll(dpnTransitions);
    }

    public Map<Integer, ConstraintGraph.Node> getNodesMappedById() {
        return getNodes()
                .stream()
                .collect(
                        Collectors.toMap(ConstraintGraph.Node::getId, Function.identity())
                );
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

        public boolean isFinal() {
            return finalNode;
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

    public static class Arc {
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
}
