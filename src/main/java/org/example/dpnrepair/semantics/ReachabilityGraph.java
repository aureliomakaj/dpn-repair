package org.example.dpnrepair.semantics;

import org.example.dpnrepair.CanonicalFormUtilities;
import org.example.dpnrepair.DPNUtils;
import org.example.dpnrepair.exceptions.DPNParserException;
import org.example.dpnrepair.parser.GuardToConstraintConverter;
import org.example.dpnrepair.parser.ast.*;

import java.util.*;
import java.util.stream.Collectors;

public class ReachabilityGraph {
    private int nodeCounter = 0;
    private Set<ReachabilityGraph.Node> nodes;
    private ReachabilityGraph.Node initialNode;
    private Set<ReachabilityGraph.Arc> arcs;

    private boolean dataAwareSound = true;

    private boolean unbounded = false;
    private List<Integer> deadlocks = new ArrayList<>();

    public int getNodeCounter() {
        return nodeCounter;
    }

    public Set<ReachabilityGraph.Node> getNodes() {
        return nodes;
    }

    public ReachabilityGraph.Node getInitialNode() {
        return initialNode;
    }

    public Set<ReachabilityGraph.Arc> getArcs() {
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

    public void setNodes(Set<ReachabilityGraph.Node> nodes) {
        this.nodes = nodes;
    }

    public void setInitialNode(ReachabilityGraph.Node initialNode) {
        this.initialNode = initialNode;
    }

    public void setArcs(Set<ReachabilityGraph.Arc> arcs) {
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

    public ReachabilityGraph(DPN dpn) {
        nodes = new HashSet<>();
        initialNode = null;
        arcs = new HashSet<>();
    }

    private boolean hasCycles(DPN dpn) {
        initialNode = computeInitialNode(dpn);
        nodes.add(initialNode);

        // Nodes found but still to be expanded
        Queue<ReachabilityGraph.Node> queue = new ArrayDeque<>();
        queue.add(initialNode);
        while (!queue.isEmpty()) {
            // Pick a node
            ReachabilityGraph.Node iter = queue.remove();
            List<Transition> enabledTransitions = DPNUtils.getEnabledTransitions(dpn.getTransitions().values(), iter.getMarking());
            for (Transition enabledTransition : enabledTransitions) {
                ReachabilityGraph.Arc arc = new Arc();
                arc.origin = iter.id;

                // Compute new marking
                Marking nextMarking = iter.getMarking().clone();
                nextMarking.removeTokens(enabledTransition.getEnabling());
                nextMarking.addTokens(enabledTransition.getOutput());

                Node destination = new Node(nextMarking);

                if ()
            }
        }

        return false;
    }


    private ReachabilityGraph.Node computeInitialNode(DPN dpn) {
        return new Node(dpn.getInitialMarking());
    }



}
