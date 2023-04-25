package org.example.dpnrepair;

import org.example.dpnrepair.parser.ast.Constraint;
import org.example.dpnrepair.parser.ast.Variable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CanonicalForm {
    private final List<Constraint> constraintList;
    private final Map<String, Integer> varsToInt = new HashMap<>();
    private final String[] vars;
    private boolean computed = false;
    private boolean isConsistent;
    private OrderPair[][] diffMatrix;

    private class OrderPair {
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
            return new OrderPair(value + second.getValue(), Math.min(loose, second.getLoose()));
        }
    }

    public CanonicalForm(List<Constraint> constraintList, Map<String, Variable> variables) {
        this.constraintList = constraintList;
        vars = variables.keySet().toArray(new String[0]);
        for (int i = 0; i < vars.length; i++) {
            varsToInt.put(vars[i], i);
        }
    }

    public boolean isConsistent() {
        if (computed) {
            return isConsistent;
        }

        buildDifferenceMatrix();
        return isConsistent;
    }

    private void buildDifferenceMatrix() {
        initializeMatrix();
        fillFromConstraints();
        applyFloydWarshall();
        checkConsistency();
    }

    private void initializeMatrix() {
        int varSize = vars.length;
        diffMatrix = new OrderPair[varSize][varSize];
        for (int i = 0; i < varSize; i++) {
            for (int j = 0; j < varSize; j++) {
                if (i == j) {
                    diffMatrix[i][j] = new OrderPair(0L, 1);
                } else {
                    diffMatrix[i][j] = new OrderPair(Long.MAX_VALUE, 1);
                }
            }
        }
    }

    private void fillFromConstraints() {
        for (Constraint c : constraintList) {
            OrderPair inMatrix = diffMatrix[varsToInt.get(c.getFirst())][varsToInt.get(c.getSecond())];
            OrderPair current = new OrderPair(c.getValue(), c.isStrict() ? 0 : 1);
            if (current.isLessThan(inMatrix)) {
                diffMatrix[varsToInt.get(c.getFirst())][varsToInt.get(c.getSecond())] = current;
            }
        }
    }

    private void applyFloydWarshall() {
        for (int i = 0; i < vars.length; i++) {
            for (int j = 0; j < vars.length; j++) {
                for (int k = 0; k < vars.length; k++) {
                    diffMatrix[i][j] = diffMatrix[i][j].min(diffMatrix[i][k].sum(diffMatrix[k][j]));
                }
            }
        }
    }

    private void checkConsistency() {
        OrderPair check = new OrderPair(0L, 1);
        this.computed = true;
        for (int i = 0; i < vars.length; i++) {
            if(diffMatrix[i][i].isLessThan(check)){
                this.isConsistent = false;
                return;
            }
        }
        this.isConsistent = true;
    }
}
