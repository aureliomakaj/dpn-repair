package org.example.dpnrepair;

import org.example.dpnrepair.parser.ast.Constraint;
import org.example.dpnrepair.parser.ast.Variable;
import org.example.dpnrepair.semantics.DifferenceConstraintSet;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class CanonicalFormUtilitiesTest {
    @Test
    void when_inconsistent_system_then_no_canonical_form() {
        /*
         * a - b < 0
         * b - a <= 0
         */
        Variable a = new Variable();
        a.setName("a");
        Variable b = new Variable();
        b.setName("b");

        Constraint first = new Constraint();
        first.setFirst(a.getName());
        first.setSecond(b.getName());
        first.setValue(0);
        first.setStrict(true);

        Constraint second = new Constraint();
        second.setFirst(b.getName());
        second.setSecond(a.getName());
        second.setValue(0);
        second.setStrict(false);

        Map<String, Variable> variableMap = new HashMap<>();
        variableMap.put(a.getName(), a);
        variableMap.put(b.getName(), b);

        DifferenceConstraintSet dcs = new DifferenceConstraintSet(new HashSet<>(Arrays.asList(first, second)), variableMap);
        assertNull(CanonicalFormUtilities.getCanonicalForm(dcs));

    }

    @Test
    void when_consistent_system_then_canonical_form() {
        assertNotNull(CanonicalFormUtilities.getCanonicalForm(getDifferenceConstraintSetOne()));
    }

    @Test
    void when_consistent_system_2_then_canonical_form() {
        assertNotNull(CanonicalFormUtilities.getCanonicalForm(getDifferenceConstraintSetTwo()));
    }

    @Test
    void when_adding_constraint_only_read_breaks_consistency_then_no_canonical_form() {
        DifferenceConstraintSet origin = getDifferenceConstraintSetOne();
        // a < 2    =>   a - Z < 2
        Constraint c = new Constraint();
        c.setFirst("a");
        c.setSecond(Constraint.ZED);
        c.setValue(2);
        c.setStrict(true);
        c.setRead(Arrays.asList("a", Constraint.ZED));
        DifferenceConstraintSet out = CanonicalFormUtilities.addConstraint(origin, c, origin.getVariables());
        assertNull(out);
    }
    @Test
    void when_adding_constraint_only_read_is_consistent_then_canonical_form() {
        DifferenceConstraintSet origin = getDifferenceConstraintSetOne();
        // a >= 7    =>   Z - a <= -7
        Constraint c = new Constraint();
        c.setFirst(Constraint.ZED);
        c.setSecond("a");
        c.setValue(-7);
        c.setStrict(false);
        c.setRead(Arrays.asList("a", Constraint.ZED));
        DifferenceConstraintSet out = CanonicalFormUtilities.addConstraint(origin, c, origin.getVariables());
        assertNotNull(out);
    }

    @Test
    void when_adding_constraint_with_write_is_consistent_then_canonical_form() {
        DifferenceConstraintSet origin = getDifferenceConstraintSetOne();
        // a >= 7    =>   Z - a <= -7
        Constraint c = new Constraint();
        c.setFirst(Constraint.ZED);
        c.setSecond("a");
        c.setValue(-7);
        c.setStrict(false);
        c.setRead(Collections.singletonList(Constraint.ZED));
        c.setWritten(Collections.singletonList("a"));
        DifferenceConstraintSet out = CanonicalFormUtilities.addConstraint(origin, c, origin.getVariables());
        assertNotNull(out);
    }
    @Test
    void when_adding_constraint_with_write_breaks_consistency_then_no_canonical_form() {
        DifferenceConstraintSet origin = getDifferenceConstraintSetOne();
        // b - a < 2
        Constraint c = new Constraint();
        c.setFirst("b");
        c.setSecond("a");
        c.setValue(2);
        c.setStrict(true);
        c.setRead(Collections.singletonList("b"));
        c.setWritten(Collections.singletonList("a"));
        DifferenceConstraintSet out = CanonicalFormUtilities.addConstraint(origin, c, origin.getVariables());
        assertNull(out);
    }

    private DifferenceConstraintSet getDifferenceConstraintSetOne() {
        /*
         * a >= 5
         * b <= 10
         * a - b < 0
         * a - b < 1
         */
        Variable a = new Variable();
        a.setName("a");
        Variable b = new Variable();
        b.setName("b");
        Variable z = new Variable();
        z.setName("Z");

        Constraint first = new Constraint();
        first.setFirst(z.getName());
        first.setSecond(a.getName());
        first.setValue(-5);
        first.setStrict(false);

        Constraint second = new Constraint();
        second.setFirst(b.getName());
        second.setSecond(z.getName());
        second.setValue(10);
        second.setStrict(false);

        Constraint third = new Constraint();
        third.setFirst(a.getName());
        third.setSecond(b.getName());
        third.setValue(0);
        third.setStrict(true);

        Constraint forth = new Constraint();
        forth.setFirst(a.getName());
        forth.setSecond(b.getName());
        forth.setValue(1);
        forth.setStrict(true);

        Map<String, Variable> variableMap = new HashMap<>();
        variableMap.put(a.getName(), a);
        variableMap.put(b.getName(), b);
        variableMap.put(z.getName(), z);

        return new DifferenceConstraintSet(new HashSet<>(Arrays.asList(first, second, third, forth)), variableMap);
    }

    private DifferenceConstraintSet getDifferenceConstraintSetTwo() {
        /*
         * a - b < 2
         * b > 0
         * b <= 2
         * a >= 1
         */
        Variable a = new Variable();
        a.setName("a");
        Variable b = new Variable();
        b.setName("b");
        Variable z = new Variable();
        z.setName("Z");

        Constraint first = new Constraint();
        first.setFirst(a.getName());
        first.setSecond(b.getName());
        first.setValue(2);
        first.setStrict(true);

        Constraint second = new Constraint();
        second.setFirst(z.getName());
        second.setSecond(b.getName());
        second.setValue(0);
        second.setStrict(true);

        Constraint third = new Constraint();
        third.setFirst(b.getName());
        third.setSecond(z.getName());
        third.setValue(2);
        third.setStrict(false);

        Constraint forth = new Constraint();
        forth.setFirst(z.getName());
        forth.setSecond(a.getName());
        forth.setValue(-1);
        forth.setStrict(false);

        Map<String, Variable> variableMap = new HashMap<>();
        variableMap.put(a.getName(), a);
        variableMap.put(b.getName(), b);
        variableMap.put(z.getName(), z);

        return new DifferenceConstraintSet(new HashSet<>(Arrays.asList(first, second, third, forth)), variableMap);
    }
}
