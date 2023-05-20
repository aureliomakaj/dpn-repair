package org.example.dpnrepair.semantics;

import org.example.dpnrepair.DPNUtils;
import org.example.dpnrepair.parser.ast.DPN;
import org.example.dpnrepair.parser.ast.Transition;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DPNRepairAcyclic {
    private DPN toRepair;
    private DPN repaired;
    PriorityQueue<RepairDPN> priorityQueue;


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
                break;
            }

        }
        this.repaired = net.dpn;
    }

    private void updatePriorityQueue(RepairDPN net) {
        if (!net.visited) {
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
                //
            }
        }
    }

    private void forwardRepair(RepairDPN net, Transition t, DifferenceConstraintSet c) {
        RepairDPN copy = new RepairDPN(net.dpn.clone());

    }

    class RepairDPN {
        DPN dpn;
        boolean visited = false;
        int differentGuards = 0;

        public RepairDPN(DPN dpn) {
            this.dpn = dpn;
        }
    }
}
