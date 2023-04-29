package org.example.dpnrepair;

import org.example.dpnrepair.parser.ast.Constraint;
import org.example.dpnrepair.parser.ast.Variable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CanonicalFormUtilitiesTest {
    @Test
    void when_inconsistent_system_then_is_not_consistent() {
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
    void when_consistent_system_then_is_consistent() {
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

        DifferenceConstraintSet dcs = new DifferenceConstraintSet(new HashSet<>(Arrays.asList(first, second, third, forth)), variableMap);
        DifferenceConstraintSet out = CanonicalFormUtilities.getCanonicalForm(dcs);
        for(Constraint c : out.getConstraintSet()) {
            System.out.println(c.toStringWithoutZed());
        }
        assertNotNull(out);
    }

    @Test
    void when_consistent_system_2_then_is_consistent() {
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

        DifferenceConstraintSet dcs = new DifferenceConstraintSet(new HashSet<>(Arrays.asList(first, second, third, forth)), variableMap);
        assertNotNull(CanonicalFormUtilities.getCanonicalForm(dcs));
    }
}
