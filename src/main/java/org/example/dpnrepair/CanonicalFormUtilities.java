package org.example.dpnrepair;

import org.example.dpnrepair.parser.ast.Constraint;
import org.example.dpnrepair.parser.ast.Variable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CanonicalFormUtilities {
    public static class OrderPair {
        private final long value;
        private final int loose; // 0 / 1

        public OrderPair(long value, int loose) {
            this.value = value;
            this.loose = loose;
        }

        public long getValue() {
            return value;
        }

        public int getLoose() {
            return loose;
        }

        public boolean isEqualTo(OrderPair other) {
            return getValue() == other.getValue() && getLoose() == other.getLoose();
        }

        public boolean isLessThan(OrderPair other) {
            long k1 = getValue();
            long k2 = other.getValue();
            boolean first = k1 < k2;
            boolean second = false;
            if (k1 == k2) {
                second = getLoose() < other.getLoose();
            }

            return first || second;
        }

        public boolean isLessThanOrEqualTo(OrderPair other) {
            return isLessThan(other) || isEqualTo(other);
        }

        public OrderPair min(OrderPair second) {
            return isLessThanOrEqualTo(second) ? this : second;
        }

        public OrderPair sum(OrderPair second) {
            // MAX_VALUE is used as infinity, but if we add something to it, it is not valid
            long sum = value == Long.MAX_VALUE || second.getValue() == Long.MAX_VALUE ? Long.MAX_VALUE : value + second.getValue();
            return new OrderPair(sum, Math.min(loose, second.getLoose()));
        }
    }

    /**
     * Computete Canonical Form of a difference constraint set if it is consistent, null otherwise
     * @param differenceConstraintSet
     * @return
     */
    public static DifferenceConstraintSet getCanonicalForm(DifferenceConstraintSet differenceConstraintSet) {
        int varSize = differenceConstraintSet.getVariables().keySet().size();
        OrderPair[][] diffMatrix = initializeMatrix(varSize);
        String[] vars = differenceConstraintSet.getVariables().keySet().toArray(new String[0]);
        Map<String, Integer> varsToInt = IntStream.range(0, varSize)
                .boxed()
                .collect(Collectors.toMap(i -> vars[i], Function.identity()));

        fillFromConstraints(differenceConstraintSet.getConstraintSet(), diffMatrix, varsToInt);
        applyFloydWarshall(diffMatrix, varSize);
        if (checkConsistency(diffMatrix, varSize)) {
            return diffMatrixToConstraintSet(diffMatrix, vars, differenceConstraintSet.getVariables());
        }
        return null;
    }

    private static OrderPair[][] initializeMatrix(int varSize) {
        OrderPair[][] diffMatrix = new OrderPair[varSize][varSize];
        for (int i = 0; i < varSize; i++) {
            for (int j = 0; j < varSize; j++) {
                if (i == j) {
                    diffMatrix[i][j] = new OrderPair(0L, 1);
                } else {
                    diffMatrix[i][j] = new OrderPair(Long.MAX_VALUE, 1);
                }
            }
        }
        return diffMatrix;
    }

    private static void fillFromConstraints(Set<Constraint> constraintSet, OrderPair[][] diffMatrix, Map<String, Integer> varsToInt) {
        for (Constraint c : constraintSet) {
            OrderPair inMatrix = diffMatrix[varsToInt.get(c.getFirst())][varsToInt.get(c.getSecond())];
            OrderPair current = new OrderPair(c.getValue(), c.isStrict() ? 0 : 1);
            if (current.isLessThan(inMatrix)) {
                diffMatrix[varsToInt.get(c.getFirst())][varsToInt.get(c.getSecond())] = current;
            }
        }
    }

    private static void applyFloydWarshall(OrderPair[][] diffMatrix, int varSize) {
        for (int k = 0; k < varSize; k++) {
            for (int i = 0; i < varSize; i++) {
                for (int j = 0; j < varSize; j++) {
                    diffMatrix[i][j] = diffMatrix[i][j].min(diffMatrix[i][k].sum(diffMatrix[k][j]));
                }
            }
        }
    }

    private static boolean checkConsistency(OrderPair[][] diffMatrix, int varSize) {
        OrderPair check = new OrderPair(0L, 1);
        for (int i = 0; i < varSize; i++) {
            if (diffMatrix[i][i].isLessThan(check)) {
                return false;
            }
        }
        return true;
    }

    private static DifferenceConstraintSet diffMatrixToConstraintSet(OrderPair[][] diffMatrix, String [] intToVar, Map<String, Variable> variables) {
        Set<Constraint> constraintSet = new HashSet<>();
        for (int i = 0; i < diffMatrix.length; i++) {
            for (int j = 0; j < diffMatrix.length; j++) {
                OrderPair pair = diffMatrix[i][j];
                if(pair.getValue() != Float.MAX_VALUE && i != j){
                    Constraint c = new Constraint();
                    c.setFirst(intToVar[i]);
                    c.setSecond(intToVar[j]);
                    c.setValue(pair.getValue());
                    c.setStrict(pair.getLoose() == 0);
                    constraintSet.add(c);
                }
            }
        }
        return new DifferenceConstraintSet(constraintSet, variables);
    }
}
