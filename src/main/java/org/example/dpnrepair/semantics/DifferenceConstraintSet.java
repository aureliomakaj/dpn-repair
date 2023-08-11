package org.example.dpnrepair.semantics;

import org.example.dpnrepair.parser.ast.Constraint;
import org.example.dpnrepair.parser.ast.Variable;

import java.util.*;

public class DifferenceConstraintSet implements Cloneable {
    private Set<Constraint> constraintSet;
    private Map<String, Variable> variables;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DifferenceConstraintSet that = (DifferenceConstraintSet) o;
        return Objects.equals(constraintSet, that.constraintSet) && variables.keySet().equals(that.variables.keySet());
    }

    @Override
    public int hashCode() {
        return Objects.hash(constraintSet, variables);
    }

    @Override
    public DifferenceConstraintSet clone() {
        try {
            DifferenceConstraintSet out = (DifferenceConstraintSet) super.clone();
            out.constraintSet = new HashSet<>(constraintSet);
            out.variables = new HashMap<>(variables);
            return out;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
