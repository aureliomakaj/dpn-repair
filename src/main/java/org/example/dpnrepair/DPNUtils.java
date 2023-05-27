package org.example.dpnrepair;

import org.example.dpnrepair.parser.ast.DPN;
import org.example.dpnrepair.parser.ast.Marking;
import org.example.dpnrepair.parser.ast.Transition;
import org.example.dpnrepair.semantics.ConstraintGraph;

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
            DPN dpn, ConstraintGraph cg, ConstraintGraph.Node initial, ConstraintGraph.Node current
    ) {

        Set<ConstraintGraph.Arc> pool = cg.getArcs()
                .stream()
                .filter(arc -> arc.getDestination() == current.getId() && arc.getOrigin() != arc.getDestination())
                .collect(Collectors.toSet());

        Set<Transition> result = new HashSet<>();
        do {
            Set<ConstraintGraph.Arc> newPool = new HashSet<>();
            for (ConstraintGraph.Arc arc: pool) {
                result.add(dpn.getTransitions().get(arc.getTransition()));
                newPool.addAll(cg.getArcs()
                        .stream()
                        .filter(item -> item.getDestination() == arc.getOrigin() && arc.getOrigin() != arc.getDestination())
                        .collect(Collectors.toList()));
            }
            pool = newPool;
        } while (!pool.isEmpty());
        return result;
    }
}
