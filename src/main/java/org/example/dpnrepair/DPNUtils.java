package org.example.dpnrepair;

import org.example.dpnrepair.parser.ast.Arc;
import org.example.dpnrepair.parser.ast.DPN;
import org.example.dpnrepair.parser.ast.Marking;
import org.example.dpnrepair.parser.ast.Transition;
import org.example.dpnrepair.semantics.ConstraintGraph;
import org.example.dpnrepair.semantics.ReachabilityGraph;

import java.util.*;
import java.util.stream.Collectors;

public class DPNUtils {
    public static List<Transition> getEnabledTransitions(Collection<Transition> transitions, Marking marking) {
        return transitions.stream()
                .filter(transition -> transition.isEnabled(marking))
                .collect(Collectors.toList());
    }

    /**
     * Get the transitions in all paths (M_0, C_0) -> (M, C)
     */
    public static Set<Transition> getPreviousTransitions(
            DPN dpn, ConstraintGraph cg, ConstraintGraph.Node current, boolean filterSilent
    ) {

        Set<ConstraintGraph.Arc> pool = cg.getArcs()
                .stream()
                .filter(arc -> (!filterSilent || !arc.isSilent()) && arc.getDestination() == current.getId() && arc.getOrigin() != arc.getDestination())
                .collect(Collectors.toSet());

        Set<Transition> result = new HashSet<>();
        do {
            Set<ConstraintGraph.Arc> newPool = new HashSet<>();
            for (ConstraintGraph.Arc arc : pool) {
                result.add(dpn.getTransitions().get(arc.getTransition()));
                newPool.addAll(cg.getArcs()
                        .stream()
                        .filter(item -> (!filterSilent || !arc.isSilent()) && item.getDestination() == arc.getOrigin() && arc.getOrigin() != arc.getDestination())
                        .collect(Collectors.toList()));
            }
            pool = newPool;
        } while (!pool.isEmpty());
        return result;
    }

    public static boolean hasCycles(DPN dpn) {
        class Node {
            private final Marking marking;

            public Node(Marking marking) {
                this.marking = marking;
            }

            public Marking getMarking() {
                return marking;
            }

        }

        class Arc {
            private int origin;
            private int destination;
        }
        Set<Node> nodes = new HashSet<>();
        Node initialNode = new Node(dpn.getInitialMarking());
        nodes.add(initialNode);

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

                // Compute new marking
                Marking nextMarking = iter.getMarking().clone();
                nextMarking.removeTokens(enabledTransition.getEnabling());
                nextMarking.addTokens(enabledTransition.getOutput());

                Node destination = new Node(nextMarking);

            }
        }

        return false;


        return false;
    }
}
