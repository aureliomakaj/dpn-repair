package org.example.dpnrepair;

import org.example.dpnrepair.parser.ast.Constraint;
import org.example.dpnrepair.parser.ast.Variable;
import org.example.dpnrepair.semantics.DifferenceConstraintSet;
import org.sosy_lab.java_smt.api.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmtSolverUtilities {
    public static BooleanFormula getSmtFormula(FormulaManager formulaManager, DifferenceConstraintSet differenceConstraintSet) {
        IntegerFormulaManager ifm = formulaManager.getIntegerFormulaManager();
        BooleanFormulaManager bfm = formulaManager.getBooleanFormulaManager();
        List<BooleanFormula> andFormulaOperands = new ArrayList<>();
        Map<String, NumeralFormula.IntegerFormula> integerVariablesMap = new HashMap<>();
        for(String variable: differenceConstraintSet.getVariables().keySet()) {
            integerVariablesMap.put(variable, ifm.makeVariable(variable));
        }
        for(Constraint constraint: differenceConstraintSet.getConstraintSet()) {
            andFormulaOperands.add(getSmtFormula(ifm, integerVariablesMap, constraint));
        }

        return bfm.and(andFormulaOperands);
    }

    public static BooleanFormula getSmtFormula(
            IntegerFormulaManager ifm,
            Map<String, NumeralFormula.IntegerFormula> integerVariablesMap,
            Constraint constraint) {

        if (constraint.isStrict()) {
            // x - y <= k
            return ifm.lessOrEquals(
                    ifm.subtract(
                            integerVariablesMap.get(constraint.getFirst()),
                            integerVariablesMap.get(constraint.getSecond())
                    ),
                    ifm.makeNumber(constraint.getValue())
            );
        } else {
            // x - y < k
            return ifm.lessThan(
                    ifm.subtract(
                            integerVariablesMap.get(constraint.getFirst()),
                            integerVariablesMap.get(constraint.getSecond())
                    ),
                    ifm.makeNumber(constraint.getValue())
            );
        }
    }
}
