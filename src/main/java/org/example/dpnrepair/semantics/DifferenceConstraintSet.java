package org.example.dpnrepair.semantics;

import org.example.dpnrepair.parser.ast.Constraint;
import org.example.dpnrepair.parser.ast.Variable;

import java.util.*;

public class DifferenceConstraintSet {
    private final Set<Constraint> constraintSet;
    private final Map<String, Variable> variables;

    public DifferenceConstraintSet(Set<Constraint> constraintSet, Map<String, Variable> variables) {
        this.constraintSet = constraintSet;
        this.variables = variables;
    }

    public Set<Constraint> getConstraintSet() {
        return constraintSet;
    }

    public Map<String, Variable> getVariables() {
        return variables;
    }
}
