package org.example.dpnrepair.semantics;

import org.example.dpnrepair.CanonicalFormUtilities;
import org.example.dpnrepair.DPNUtils;
import org.example.dpnrepair.SmtSolverUtilities;
import org.example.dpnrepair.parser.ast.Constraint;
import org.example.dpnrepair.parser.ast.DPN;
import org.example.dpnrepair.parser.ast.Transition;
import org.example.dpnrepair.parser.ast.Variable;
import org.sosy_lab.java_smt.api.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DPNRepairCyclic extends DPNRepairAcyclic {

    private final SolverContext solverContext;
    private Set<DifferenceConstraintSet> visitedForms;
    private Map<Integer, DifferenceConstraintSet> integerDifferenceConstraintSetMap;
    private int number;
    public DPNRepairCyclic(DPN dpn, SolverContext solverContext) {
        super(dpn);
        this.solverContext = solverContext;
    }

    public void repair() {
        priorityQueue.add(new RepairDPN(toRepair));
        RepairDPN net;
        int skip = 0;
        while (true) {
            net = priorityQueue.remove();
            iterations++;
            setVisited(net.dpn);
            ConstraintGraph cg = new ConstraintGraph(net.dpn);
            Map<Integer, Boolean> soundnessCheck = new HashMap<>();
            Map<ConstraintGraph.Node, Set<DifferenceConstraintSet>> nodeMap = new HashMap<>();
            if (cg.isDataAwareSound()) {
                nodeMap = computeNodesCoreachability(toRepair, cg);
                soundnessCheck = checkSoundness(cg, nodeMap);
                if (soundnessCheck.values().stream().allMatch(value -> value)) {
                    modifiedTransitions = net.modifiedTransitions;
                    if(skip == 0) {
                        break;
                    } else {
                        skip--;
                    }
                }
            }
            fixDead(net, cg);
            fixMissing(net, cg);
            fixCoreachabilityNodes(net, cg, soundnessCheck, nodeMap);
        }
        this.repaired = net.dpn;
        this.distance = net.modifiedTransitions.size();
    }

    private int findKeyOfConstraint(DifferenceConstraintSet s) {
        for (Map.Entry<Integer, DifferenceConstraintSet> entry: integerDifferenceConstraintSetMap.entrySet()) {
            if(entry.getValue().equals(s)) {
                return entry.getKey();
            }
        }
        return -1;
    }

    protected void fixCoreachabilityNodes(RepairDPN net, ConstraintGraph cg, Map<Integer, Boolean> soundnessCheck,
                                          Map<ConstraintGraph.Node, Set<DifferenceConstraintSet>> nodeCoreachMap) {
        Map<Integer, ConstraintGraph.Node> nodeMap = cg.getNodes()
                .stream()
                .collect(
                        Collectors.toMap(ConstraintGraph.Node::getId, Function.identity())
                );
        List<Integer> ids = soundnessCheck.entrySet()
                .stream()
                .filter(entry -> !entry.getValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        for (Integer nodeId : ids) {
            ConstraintGraph.Node curr = nodeMap.get(nodeId);
            List<Transition> enabledTransitions = DPNUtils.getEnabledTransitions(net.dpn.getTransitions().values(), curr.getMarking());
            Set<DifferenceConstraintSet> setOfDifferenceConstraintSets = nodeCoreachMap.get(curr)
                    .stream()
                    .map(differenceConstraintSet -> CanonicalFormUtilities.difference(curr.getCanonicalForm(), differenceConstraintSet))
                    .flatMap(Collection::stream)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Set<Transition> previousTransitions = DPNUtils.getPreviousTransitions(net.dpn, cg, curr, false);
            for (DifferenceConstraintSet differenceConstraintSet : setOfDifferenceConstraintSets) {
                for (Transition enabledTransition : enabledTransitions) {
                    forwardRepair(net, enabledTransition, differenceConstraintSet);
                }
                for (Transition t : previousTransitions) {
                    backwardRepair(net, t, differenceConstraintSet);
                }
            }
        }
    }

    // "replace with a constraint of C"
    protected void forwardRepair(RepairDPN net, Transition t, DifferenceConstraintSet c) {
        // Let N′′ := (P, T, F, V, αI , guard′′) be a copy of N′.
        RepairDPN firstCopy = makeCopy(net);
        // Let y − x op k be the guard of t.
        Constraint guard = t.getGuard();
        // y − x ≤ k′ in C
        Constraint guardUnderlyingNet = getConstraintFromDifferenceConstraintSet(guard.getFirst(), guard.getSecond(), c);
        // if y − x ≤ k′ in C is such that k′ = 0 or y is Z or x is Z then
        if ((!guardUnderlyingNet.isStrict() && guardUnderlyingNet.getValue() == 0) || guard.getFirst().equals(Constraint.ZED) || guard.getSecond().equals(Constraint.ZED)) {
            // guard′′(t) := y − x ≤ k′
            Transition t1 = firstCopy.dpn.getTransitions().get(t.getId());
            t1.setGuard(guardUnderlyingNet.clone());
            // Increase changes only if there is a difference between guard and guardUnderlyingNet
            if (guard.isStrict() != guardUnderlyingNet.isStrict() || guard.getValue() != guardUnderlyingNet.getValue()) {
                firstCopy.modifiedTransitions.add(t.getId());
                firstCopy.changes++;
            }
            updatePriorityQueue(firstCopy);
        } else {
            guard = t.getGuard();
            // Let y − Z op′ k′ be the constraint in C.
            guardUnderlyingNet = getConstraintFromDifferenceConstraintSet(guard.getFirst(), Constraint.ZED, c);
            // guard′′(t) := y − Z op′ k′
            Transition t2 = firstCopy.dpn.getTransitions().get(t.getId());
            t2.setGuard(guardUnderlyingNet.clone());
            firstCopy.modifiedTransitions.add(t.getId());
            firstCopy.changes++;
            // UpdateQ(N′′)
            updatePriorityQueue(firstCopy);

            // Let N′′ be a new copy of N′
            RepairDPN secondCopy = makeCopy(net);
            guard = t.getGuard();
            // Let Z − x op′ k′ be the constraint in C.
            guardUnderlyingNet = getConstraintFromDifferenceConstraintSet(Constraint.ZED, guard.getSecond(), c);
            Transition t3 = secondCopy.dpn.getTransitions().get(t.getId());
            // guard′′(t) := Z − x op′ k′
            t3.setGuard(guardUnderlyingNet.clone());
            secondCopy.modifiedTransitions.add(t.getId());
            secondCopy.changes++;
            // UpdateQ(N′′)
            updatePriorityQueue(secondCopy);
        }
    }

    // "replace with an opposite constraint of C"
    protected void backwardRepair(RepairDPN net, Transition t, DifferenceConstraintSet c) {
        // Let N′′ := (P, T, F, V, αI , guard′) be a copy of N′.
        RepairDPN firstCopy = makeCopy(net);
        // Let x − y op k be the guard of t.
        Constraint guard = t.getGuard();
        // y − x op′ k′ in C
        Constraint guardUnderlyingNet = getConstraintFromDifferenceConstraintSet(guard.getSecond(), guard.getFirst(), c);
        // if y − x op′ k′ in C is such that k′ = 0 or y is Z or x is Z then
        if ((guardUnderlyingNet.getValue() == 0) || guard.getFirst().equals(Constraint.ZED) || guard.getSecond().equals(Constraint.ZED)) {
            Constraint guardCopy = guard.clone();
            guardCopy.setStrict(!guardUnderlyingNet.isStrict()); // Switch strictness
            Transition t2 = firstCopy.dpn.getTransitions().get(t.getId());
            t2.setGuard(guardCopy);
            firstCopy.modifiedTransitions.add(t.getId());
            firstCopy.changes++;
            updatePriorityQueue(firstCopy);
        } else {
            // y − Z op′ k′ in C
            Constraint guardUnderlyingNet2 = getConstraintFromDifferenceConstraintSet(guard.getSecond(), Constraint.ZED, c);
            // if y − Z op′ k′ in C is such that k′ != +Inf then
            if (guardUnderlyingNet2.getValue() != Long.MAX_VALUE) {
                Constraint guardCopy = guard.clone();
                guardCopy.setFirst(Constraint.ZED);
                guardCopy.setValue(guardUnderlyingNet2.getNegatedValue()); // Negate value
                guardCopy.setStrict(!guardUnderlyingNet2.isStrict()); // Switch strictness
                Transition t2 = firstCopy.dpn.getTransitions().get(t.getId());
                t2.setGuard(guardCopy);
                firstCopy.modifiedTransitions.add(t.getId());
                firstCopy.changes++;
                updatePriorityQueue(firstCopy);
            }

            // Let N′′ be a new copy of N′
            RepairDPN secondCopy = makeCopy(net);
            // Z − x op′ k′ in C
            Constraint guardUnderlyingNet3 = getConstraintFromDifferenceConstraintSet(Constraint.ZED, guard.getFirst(), c);
            // if Z − x op′ k′ in C is such that k′ != +Inf then
            if (guardUnderlyingNet3.getValue() != Long.MAX_VALUE) {
                Constraint guardCopy = guard.clone();
                guardCopy.setSecond(Constraint.ZED);
                guardCopy.setStrict(!guardUnderlyingNet3.isStrict()); // Switch strictness
                guardCopy.setValue(guardUnderlyingNet3.getNegatedValue()); // Negate value
                Transition t2 = secondCopy.dpn.getTransitions().get(t.getId());
                t2.setGuard(guardCopy);
                secondCopy.modifiedTransitions.add(t.getId());
                secondCopy.changes++;
                updatePriorityQueue(secondCopy);
            }
        }

    }

    public Map<ConstraintGraph.Node, Set<DifferenceConstraintSet>> computeNodesCoreachability(DPN dpn, ConstraintGraph cg) {
        visitedForms = new HashSet<>();
        integerDifferenceConstraintSetMap = new HashMap<>();
        number = 0;
        for (ConstraintGraph.Node n: cg.getNodes().stream().sorted(Comparator.comparingInt(ConstraintGraph.Node::getId)).collect(Collectors.toList())) {
            if(!visitedForms.contains(n.getCanonicalForm())) {
                number += 1;
                visitedForms.add(n.getCanonicalForm());
                integerDifferenceConstraintSetMap.put(number, n.getCanonicalForm());
            }
        }

        Map<Integer, ConstraintGraph.Node> idNodeMap = cg.getNodes()
                .stream()
                .collect(Collectors.toMap(ConstraintGraph.Node::getId, Function.identity()));

        Map<ConstraintGraph.Node, Set<DifferenceConstraintSet>> nodeMap = new HashMap<>();
        Set<ConstraintGraph.Node> nodes = new TreeSet<>(Comparator.comparingInt(ConstraintGraph.Node::getId));
        nodes.addAll(cg.getNodes());
        for (ConstraintGraph.Node node : nodes) {
            if (node.isFinal()) {
                nodeMap.put(node, new HashSet<>(Collections.singleton(node.getCanonicalForm())));
            } else {
                nodeMap.put(node, new HashSet<>());
            }
        }
        Map<ConstraintGraph.Node, Set<DifferenceConstraintSet>> nodeMapPrev = new HashMap<>();
        int iterations = 0;
        while (!equalMaps(nodeMap, nodeMapPrev)) {
            iterations++;
            nodeMapPrev = new HashMap<>(nodeMap);
            nodeMap = new HashMap<>();
            for (ConstraintGraph.Node node : cg.getNodes()) {
                nodeMap.put(node, computeCoReach(nodeMapPrev, node, dpn, cg, idNodeMap));
            }
        }

        return nodeMap;
    }

    public Map<Integer, Boolean> checkSoundness(ConstraintGraph cg,
                                                Map<ConstraintGraph.Node, Set<DifferenceConstraintSet>> nodeMap) {
        Map<Integer, Boolean> nodeCoreachabilityMap = new HashMap<>();
        for (ConstraintGraph.Node node : cg.getNodes()) {
            boolean res = areDifferenceConstraintSetsEqual(nodeMap.get(node), node.getCanonicalForm());
            nodeCoreachabilityMap.put(node.getId(), res);
        }
        return nodeCoreachabilityMap;
    }

    private boolean equalMaps(
            Map<ConstraintGraph.Node, Set<DifferenceConstraintSet>> first,
            Map<ConstraintGraph.Node, Set<DifferenceConstraintSet>> second) {
        return !first.isEmpty() && first.entrySet()
                .stream()
                .allMatch(entry -> {
                    Set<DifferenceConstraintSet> firstsSet = first.get(entry.getKey());
                    Set<DifferenceConstraintSet> secondSet = second.get(entry.getKey());
                    if (firstsSet == null || secondSet == null) {
                        return false;
                    }
                    Set<DifferenceConstraintSet> difference = (new HashSet<>(firstsSet));
                    difference.retainAll(secondSet);
                    return difference.size() == firstsSet.size() && difference.size() == secondSet.size();
                });
    }

    private Set<DifferenceConstraintSet> computeCoReach(
            Map<ConstraintGraph.Node, Set<DifferenceConstraintSet>> nodeMap,
            ConstraintGraph.Node node,
            DPN dpn,
            ConstraintGraph cg,
            Map<Integer, ConstraintGraph.Node> idNodeMap) {
        Set<DifferenceConstraintSet> result = nodeMap.get(node).stream().map(DifferenceConstraintSet::clone).collect(Collectors.toSet());
        List<ConstraintGraph.Arc> arcs = cg.getArcs()
                .stream()
                .filter(arc -> arc.getOrigin() == node.getId())
                .collect(Collectors.toList());

        Set<ConstraintGraph.Node> readOrSilent = new HashSet<>();
        class WriteElement {
            private ConstraintGraph.Node node;
            private List<String> writeVars;
        }
        Set<WriteElement> write = new HashSet<>();
        for (ConstraintGraph.Arc arc : arcs) {
            Transition t = dpn.getTransitions().get(arc.getTransition());
            if (t.getGuard().getWritten().size() == 0 || arc.isSilent()) {
                readOrSilent.add(idNodeMap.get(arc.getDestination()));
            } else {
                WriteElement we = new WriteElement();
                we.node = idNodeMap.get(arc.getDestination());
                we.writeVars = t.getGuard().getWritten();
                write.add(we);
            }
        }

        for (ConstraintGraph.Node nodePrime : readOrSilent) {
            result.addAll(nodeMap.get(nodePrime));
        }

        for (WriteElement we : write) {
            for (DifferenceConstraintSet constraintSet : nodeMap.get(we.node)) {
                DifferenceConstraintSet canonical = CanonicalFormUtilities.getCanonicalForm(
                        this.intersect(computeExists(we.writeVars, constraintSet), node.getCanonicalForm())
                );
                if (canonical != null) {
                    if (!visitedForms.contains(canonical)) {
                        visitedForms.add(canonical);
                        number += 1;
                        integerDifferenceConstraintSetMap.put(number, canonical);
                    }
                    result.add(canonical);
                }
            }
        }
        return result;
    }

    public DifferenceConstraintSet computeExists(
            List<String> writeList,
            DifferenceConstraintSet nodeMap) {
        Set<String> writeVars = new HashSet<>(writeList);
        Map<String, Variable> resVariables = nodeMap.getVariables()
                .values()
                .stream()
                .filter(var -> !writeVars.contains(var.getName()))
                .map(Variable::clone)
                .collect(Collectors.toMap(Variable::getName, Function.identity()));

        List<Constraint> resConstraints = nodeMap.getConstraintSet()
                .stream()
                .filter(constraint -> !writeVars.contains(constraint.getFirst()) && !writeVars.contains(constraint.getSecond()))
                .map(Constraint::clone)
                .collect(Collectors.toList());

        return new DifferenceConstraintSet(new HashSet<>(resConstraints), resVariables);
    }

    public DifferenceConstraintSet intersect(DifferenceConstraintSet first, DifferenceConstraintSet second) {
        Set<Variable> variableSet = new HashSet<>(first.getVariables().values());
        variableSet.addAll(second.getVariables().values());

        List<Constraint> constraintList = new ArrayList<>(first.getConstraintSet());
        constraintList.addAll(second.getConstraintSet().stream().map(Constraint::clone).collect(Collectors.toList()));

        return new DifferenceConstraintSet(
                new HashSet<>(constraintList),
                variableSet.stream().collect(Collectors.toMap(Variable::getName, Function.identity()))
        );
    }

    public boolean areDifferenceConstraintSetsEqual(Set<DifferenceConstraintSet> result, DifferenceConstraintSet origin) {
        FormulaManager fm = solverContext.getFormulaManager();
        BooleanFormulaManager bfm = fm.getBooleanFormulaManager();
        List<BooleanFormula> orFormulaOperands = new ArrayList<>();
        for (DifferenceConstraintSet set : result) {
            orFormulaOperands.add(SmtSolverUtilities.getSmtFormula(fm, set));
        }
        BooleanFormula orFormula = bfm.or(orFormulaOperands);
        BooleanFormula originSetFormula = SmtSolverUtilities.getSmtFormula(fm, origin);
        // not ( S1  or S2 or ... Sn <=> So )
        BooleanFormula finalFormula = bfm.not(bfm.equivalence(originSetFormula, orFormula));

        try (ProverEnvironment prover = solverContext.newProverEnvironment(SolverContext.ProverOptions.GENERATE_MODELS)) {
            prover.addConstraint(finalFormula);
            // Is there a set of values such that the systems are different?
            // If no such values exist then the systems are equivalent
            return prover.isUnsat();
        } catch (SolverException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
