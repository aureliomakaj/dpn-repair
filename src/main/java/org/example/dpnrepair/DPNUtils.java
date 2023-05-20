package org.example.dpnrepair;

import org.example.dpnrepair.parser.ast.Marking;
import org.example.dpnrepair.parser.ast.Transition;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class DPNUtils {
    public static List<Transition> getEnabledTransitions(Collection<Transition> transitions, Marking marking) {
        return transitions.stream()
                .filter(transition -> transition.isEnabled(marking))
                .collect(Collectors.toList());
    }
}
