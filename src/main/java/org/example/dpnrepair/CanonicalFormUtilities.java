package org.example.dpnrepair;

import org.example.dpnrepair.parser.ast.Constraint;
import org.example.dpnrepair.parser.ast.Variable;
import org.example.dpnrepair.semantics.DifferenceConstraintSet;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CanonicalFormUtilities {

    /**
     * Compute Canonical Form of a difference constraint set if it is consistent, null otherwise
     *
     * @return DifferenceConstraintSet
     */
    public static DifferenceConstraintSet getCanonicalForm(DifferenceConstraintSet differenceConstraintSet) {
        int varSize = differenceConstraintSet.getVariables().keySet().size();
        OrderPair[][] diffMatrix = initializeMatrix(varSize);
        String[] vars = differenceConstraintSet.getVariables().keySet().toArray(new String[0]);
        Map<String, Integer> varsToInt = IntStream.range(0, varSize)
                .boxed()
                .collect(Collectors.toMap(i -> vars[i], Function.identity()));

        boolean ok = fillFromConstraints(differenceConstraintSet.getConstraintSet(), diffMatrix, varsToInt);
        if (!ok) {
            return null;
        }
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
                    // Use Long.MAX_VALUE to represent +Inf
                    diffMatrix[i][j] = new OrderPair(Long.MAX_VALUE, 1);
                }
            }
        }
        return diffMatrix;
    }

    private static boolean fillFromConstraints(Set<Constraint> constraintSet, OrderPair[][] diffMatrix, Map<String, Integer> varsToInt) {
        for (Constraint c : constraintSet) {
            OrderPair inMatrix = diffMatrix[varsToInt.get(c.getFirst())][varsToInt.get(c.getSecond())];
            OrderPair current = new OrderPair(c.getValue(), c.isStrict() ? 0 : 1);
            if (current.getValue() == Long.MIN_VALUE) {
                return false;
            }
            if (current.isLessThan(inMatrix)) {
                diffMatrix[varsToInt.get(c.getFirst())][varsToInt.get(c.getSecond())] = current;
            }
        }
        return true;
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

    private static DifferenceConstraintSet diffMatrixToConstraintSet(OrderPair[][] diffMatrix, String[] intToVar,
                                                                     Map<String, Variable> variables) {
        Set<Constraint> constraintSet = new HashSet<>();
        for (int i = 0; i < diffMatrix.length; i++) {
            for (int j = 0; j < diffMatrix.length; j++) {
                OrderPair pair = diffMatrix[i][j];
                if (pair.getValue() != Long.MAX_VALUE && i != j) {
                    Constraint c = new Constraint();
                    c.setFirst(intToVar[i]);
                    c.setSecond(intToVar[j]);
                    c.setValue(pair.getValue());
                    c.setStrict(pair.getLoose() == 0);
                    c.addRead(c.getFirst());
                    c.addRead(c.getSecond());
                    constraintSet.add(c);
                }
            }
        }
        return new DifferenceConstraintSet(constraintSet, variables);
    }

    /**
     * Add a constraint and return the canonical form, if consistent, null otherwise
     */
    public static DifferenceConstraintSet addConstraint(DifferenceConstraintSet constraintSet, Constraint toBeAdd,
                                                        Map<String, Variable> toBeAddVariables) {
        if (toBeAdd.getRead().size() == 2) {
            DifferenceConstraintSet newSet = union(constraintSet, Collections.singletonList(toBeAdd), toBeAddVariables);
            return getCanonicalForm(newSet);
        } else {
            Variable first = toBeAddVariables.get(toBeAdd.getFirst());
            Variable second = toBeAddVariables.get(toBeAdd.getSecond());
            Map<String, Variable> newToBeAddVariables = new HashMap<>(toBeAddVariables);
            Variable firstAsWritten = null;
            Variable secondAsWritten = null;
            List<String> writtenVars = new ArrayList<>();
            Constraint newConstraint = toBeAdd.clone();
            if (toBeAdd.getWritten().contains(first.getName())) {
                firstAsWritten = new Variable();
                firstAsWritten.setName(first.getName() + "_w");
                writtenVars.add(first.getName());
            }
            if (toBeAdd.getWritten().contains(second.getName())) {
                secondAsWritten = new Variable();
                secondAsWritten.setName(second.getName() + "_w");
                writtenVars.add(second.getName());
            }
            if (firstAsWritten != null) {
                newConstraint.setFirst(firstAsWritten.getName());
                newToBeAddVariables.put(firstAsWritten.getName(), firstAsWritten);
            }
            if (secondAsWritten != null) {
                newConstraint.setSecond(secondAsWritten.getName());
                newToBeAddVariables.put(secondAsWritten.getName(), secondAsWritten);
            }
            // Add the new constraint and compute canonical form
            DifferenceConstraintSet canonicalForm = getCanonicalForm(
                    union(constraintSet, Collections.singletonList(newConstraint), newToBeAddVariables)
            );
            if (canonicalForm == null) {
                return null;
            }

            // Canonical form is consistent, thus remove old variable occurrences and rename the fresh ones
            Set<Constraint> finalConstraintSet = new HashSet<>(canonicalForm.getConstraintSet());

            finalConstraintSet = finalConstraintSet.stream()
                    // C′ := C′ \ {x′ − y′ op k′ | x′ in write(c) or y′ in write(c)}
                    .filter(item -> !writtenVars.contains(item.getFirst()) && !writtenVars.contains(item.getSecond()))
                    // Rename fresh variables to replace the old ones
                    .peek(item -> {
                        if (isTempWritten(item.getFirst())) {
                            // Remove _w from the end
                            item.setFirst(rewriteTempWritten(item.getFirst()));
                        }
                        if (isTempWritten(item.getSecond())) {
                            item.setSecond(rewriteTempWritten(item.getSecond()));
                        }
                        item.setWritten(item.getWritten().stream().map(s -> {
                            if (isTempWritten(s)) {
                                return rewriteTempWritten(s);
                            }
                            return s;
                        }).collect(Collectors.toList()));

                        item.setRead(item.getRead().stream().map(s -> {
                            if (isTempWritten(s)) {
                                return rewriteTempWritten(s);
                            }
                            return s;
                        }).collect(Collectors.toList()));
                    })
                    .collect(Collectors.toSet());

            // To the same of above for the variable set
            Map<String, Variable> finalVarMap = new HashMap<>(canonicalForm.getVariables());
            finalVarMap = finalVarMap.entrySet()
                    .stream()
                    .filter(entry -> !writtenVars.contains(entry.getKey()))
                    .collect(Collectors.toMap(//Rename fresh variables to replace the old ones
                            entry -> {
                                if (isTempWritten(entry.getKey())) {
                                    return rewriteTempWritten(entry.getKey());
                                }
                                return entry.getKey();
                            },
                            entry -> {
                                if (isTempWritten(entry.getKey())) {
                                    entry.getValue().setName(rewriteTempWritten(entry.getValue().getName()));
                                }
                                return entry.getValue();
                            }
                    ));

            return new DifferenceConstraintSet(finalConstraintSet, finalVarMap);
        }
    }
    
    private static boolean isTempWritten(String variable) {
        return variable.endsWith("_w");
    }
    private static String rewriteTempWritten(String variable) {
        return variable.substring(0, variable.length() - 2);
    }

    public static DifferenceConstraintSet union(DifferenceConstraintSet constraintSet, List<Constraint> toBeAdd,
                                                Map<String, Variable> toBeAddVariables) {
        Set<Constraint> newConstraintSet = new HashSet<>(constraintSet.getConstraintSet());
        Map<String, Variable> newVariableMap = new HashMap<>(constraintSet.getVariables());
        for (Constraint c : toBeAdd) {
            Variable first = toBeAddVariables.get(c.getFirst());
            Variable second = toBeAddVariables.get(c.getSecond());
            newConstraintSet.add(c);
            newVariableMap.putIfAbsent(first.getName(), first);
            newVariableMap.putIfAbsent(second.getName(), second);
        }
        return new DifferenceConstraintSet(newConstraintSet, newVariableMap);
    }

    // Performs S = first - second
    public static Set<DifferenceConstraintSet> difference(DifferenceConstraintSet first, DifferenceConstraintSet second) {
        Set<DifferenceConstraintSet> result = new HashSet<>();
        second.getConstraintSet()
                .forEach(constraint -> {
                    Map<String, Variable> toBeAddVariable = new HashMap<>();
                    toBeAddVariable.put(constraint.getFirst(), second.getVariables().get(constraint.getFirst()));
                    toBeAddVariable.put(constraint.getSecond(), second.getVariables().get(constraint.getSecond()));
                    result.add(addConstraint(first, constraint.getNegated(), toBeAddVariable));
                });

        return result;
    }
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
            // MAX_VALUE is used as infinity
            long sum = value == Long.MAX_VALUE || second.getValue() == Long.MAX_VALUE ? Long.MAX_VALUE : value + second.getValue();
            return new OrderPair(sum, Math.min(loose, second.getLoose()));
        }
    }

}
