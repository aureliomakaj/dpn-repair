package org.example.dpnrepair.semantics;

import org.example.dpnrepair.ConstraintGraphPrinter;
import org.example.dpnrepair.DPNUtils;
import org.example.dpnrepair.parser.ast.Constraint;
import org.example.dpnrepair.parser.ast.DPN;
import org.example.dpnrepair.parser.ast.Transition;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DPNRepairAcyclic {
    private DPN toRepair;
    private DPN repaired;
    PriorityQueue<RepairDPN> priorityQueue;
    private int distance = 0;


    public DPNRepairAcyclic(DPN dpn) {
        this.toRepair = dpn;
        priorityQueue = new PriorityQueue<>(Comparator.comparingInt(o -> o.differentGuards));
    }

    public void repair() {
        priorityQueue.add(new RepairDPN(toRepair));
        RepairDPN net;
        while (true) {
            net = priorityQueue.remove();
            ConstraintGraph cg = new ConstraintGraph(net.dpn);
            if (cg.isDataAwareSound()) {
                ConstraintGraphPrinter a = new ConstraintGraphPrinter(cg);
                a.writeRaw("solution");
                break;
            }
            fixDead(net, cg);
        }
        this.repaired = net.dpn;
        this.distance = net.differentGuards;
    }

    private void updatePriorityQueue(RepairDPN net) {
        if (!net.visited) {
            net.visited = true;
            priorityQueue.add(net);
        }
    }

    private void fixDead(RepairDPN net, ConstraintGraph cg) {
        ConstraintGraph.Node initial = cg.getInitialNode();
        Map<Integer, ConstraintGraph.Node> nodeMap = cg.getNodes()
                .stream()
                .collect(
                        Collectors.toMap(ConstraintGraph.Node::getId, Function.identity())
                );

        for (Integer nodeId : cg.getDeadlocks()) {
            ConstraintGraph.Node curr = nodeMap.get(nodeId);
            List<Transition> enabledTransitions = DPNUtils.getEnabledTransitions(net.dpn.getTransitions().values(), curr.getMarking());
            for (Transition enabledTransition : enabledTransitions) {
                forwardRepair(net, enabledTransition, curr.getCanonicalForm());
            }
            Set<Transition> previousTransitions = DPNUtils.getPreviousTransitions(
                    net.dpn, cg, initial, curr
            );
            for (Transition t : previousTransitions) {
                backwardRepair(net, t, curr.getCanonicalForm());
            }
        }
    }

    private void forwardRepair(RepairDPN net, Transition t, DifferenceConstraintSet c) {
        RepairDPN copy = new RepairDPN(net.dpn.clone(), net.differentGuards);
        Constraint guard = t.getGuard();
        Constraint guardUnderlyingNet = getConstraintFromDifferenceConstraintSet(guard.getFirst(), guard.getSecond(), c);
        Transition t2 = copy.dpn.getTransitions().get(t.getId());
        t2.setGuard(guardUnderlyingNet.clone());
        copy.differentGuards++;
        updatePriorityQueue(copy);
    }

    private Constraint getConstraintFromDifferenceConstraintSet(String first, String second, DifferenceConstraintSet c) {
        Optional<Constraint> constraint = c.getConstraintSet()
                .stream()
                .filter(constr -> first.equals(constr.getFirst()) && second.equals(constr.getSecond()))
                .findFirst();

        return constraint.orElse(new Constraint(first, second));
    }

    private void backwardRepair(RepairDPN net, Transition t, DifferenceConstraintSet c) {
        RepairDPN copy = new RepairDPN(net.dpn.clone(), net.differentGuards);
        Constraint guard = t.getGuard();
        Constraint guardUnderlyingNet = getConstraintFromDifferenceConstraintSet(guard.getFirst(), guard.getSecond(), c);
        if (guardUnderlyingNet.getValue() != Long.MAX_VALUE) {
            Constraint copyOfUnderlying = guardUnderlyingNet.clone();
            copyOfUnderlying.setValue(-copyOfUnderlying.getValue()); // Negate value
            copyOfUnderlying.setStrict(!guardUnderlyingNet.isStrict()); // Switch strictness
            Transition t2 = copy.dpn.getTransitions().get(t.getId());
            t2.setGuard(copyOfUnderlying);
            copy.differentGuards++;
            updatePriorityQueue(copy);
        }
    }

    class RepairDPN {
        DPN dpn;
        boolean visited = false;
        int differentGuards = 0;

        public RepairDPN(DPN dpn) {
            this.dpn = dpn;
        }

        public RepairDPN(DPN dpn, int differentGuards) {
            this.dpn = dpn;
            this.differentGuards = differentGuards;
        }
    }
}
