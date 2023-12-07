package org.example.dpnrepair.semantics;

import org.example.dpnrepair.DPNUtils;
import org.example.dpnrepair.parser.ast.Constraint;
import org.example.dpnrepair.parser.ast.DPN;
import org.example.dpnrepair.parser.ast.Transition;

import java.util.*;
import java.util.stream.Collectors;

public class DPNRepairAcyclic {
    protected final DPN toRepair;
    protected DPN repaired;
    PriorityQueue<RepairDPN> priorityQueue;
    protected int distance = 0;
    protected final Set<DPN> visitedDpn = new HashSet<>();
    protected Set<String> modifiedTransitions;
    protected int iterations = 0;


    public DPNRepairAcyclic(DPN dpn) {
        this.toRepair = dpn;
        priorityQueue = new PriorityQueue<>(Comparator.comparingInt(o -> o.modifiedTransitions.size()));
    }

    public DPN getRepaired() {
        return repaired;
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
            if (cg.isDataAwareSound()) {
                modifiedTransitions = net.modifiedTransitions;
                if (skip == 0) {
                    break;
                } else {
                    skip--;
                }
            }
            fixDead(net, cg);
            fixMissing(net, cg);
        }
        this.repaired = net.dpn;
        this.distance = net.modifiedTransitions.size();
    }

    public PriorityQueue<RepairDPN> getPriorityQueue() {
        return priorityQueue;
    }

    public int getDistance() {
        return distance;
    }

    public Set<DPN> getVisitedDpn() {
        return visitedDpn;
    }

    public Set<String> getModifiedTransitions() {
        return modifiedTransitions;
    }

    public int getIterations() {
        return iterations;
    }

    /**
     * In our algorithm, the only thing that can change between different DPNs
     * are the guards of transitions, thus we base on that to see if a dpn
     * has already been visited
     */
    protected void setVisited(DPN dpn) {
        visitedDpn.add(dpn);
    }

    protected Set<String> getDPNKey(DPN dpn) {
        return dpn.getTransitions()
                .values()
                .stream()
                .map(Transition::getGuard)
                .map(Constraint::toString)
                .collect(Collectors.toSet());
    }

    protected void updatePriorityQueue(RepairDPN net) {
        if (!visitedDpn.contains(net.dpn)) {
//            if(priorityQueue.size() < 1000) {
                priorityQueue.add(net);
//            }
        }
    }

    protected void fixDead(RepairDPN net, ConstraintGraph cg) {
        Map<Integer, ConstraintGraph.Node> nodeMap = cg.getNodesMappedById();

        for (Integer nodeId : cg.getDeadNodes()) {
            ConstraintGraph.Node curr = nodeMap.get(nodeId);
            // Let FW := {t ∈ T | M[t⟩M′ for some marking M′} be the set of all non-silent
            // transitions that can fire from marking M in the underlying Petri Net
            List<Transition> enabledTransitions = DPNUtils.getEnabledTransitions(net.dpn.getTransitions().values(), curr.getMarking());
            for (Transition enabledTransition : enabledTransitions) {
                forwardRepair(net, enabledTransition, curr.getCanonicalForm());
            }
            // Let BW be the set of transitions in all paths (M0,C0) -> (M,C).
            Set<Transition> previousTransitions = DPNUtils.getPreviousTransitions(net.dpn, cg, curr, false);
            for (Transition t : previousTransitions) {
                backwardRepair(net, t, curr.getCanonicalForm());
            }
        }
    }

    // "replace with the same constraint of C"
    protected void forwardRepair(RepairDPN net, Transition t, DifferenceConstraintSet c) {
        // Let N′′ := (P, T, F, V, αI , guard′′) be a copy of N′.
        RepairDPN copy = makeCopy(net);

        // Let y − x op k be the guard of t.
        Constraint guard = t.getGuard();
        // Let y − x op′ k′ be the corresponding constraint in C.
        Constraint guardUnderlyingNet = getConstraintFromDifferenceConstraintSet(guard.getFirst(), guard.getSecond(), c);
        Transition t2 = copy.dpn.getTransitions().get(t.getId());
        // guard′′(t) := y − x op′ k′
        t2.setGuard(guardUnderlyingNet.clone());
        copy.modifiedTransitions.add(t.getId());
        copy.changes++;
        // UpdateQ(N′′)
        updatePriorityQueue(copy);
    }

    protected RepairDPN makeCopy(RepairDPN net) {
        RepairDPN copy = new RepairDPN(net.dpn.clone());
        copy.modifiedTransitions = new HashSet<>(net.modifiedTransitions);
        copy.changes = net.changes;
        return copy;
    }

    protected Constraint getConstraintFromDifferenceConstraintSet(String first, String second, DifferenceConstraintSet c) {
        Optional<Constraint> constraint = c.getConstraintSet()
                .stream()
                .filter(constr -> first.equals(constr.getFirst()) && second.equals(constr.getSecond()))
                .findFirst();

        return constraint
                .orElse(new Constraint(first, second));
    }

    // “replace with the opposite constraint of C''
    protected void backwardRepair(RepairDPN net, Transition t, DifferenceConstraintSet c) {
        // Let N′′ := (P, T, F, V, αI , guard′) be a copy of N′.
        RepairDPN copy = makeCopy(net);
        // Let y − x op k be the guard of t.
        Constraint guard = t.getGuard();
        // x − y op′ k′ in C
        Constraint guardUnderlyingNet = getConstraintFromDifferenceConstraintSet(guard.getSecond(), guard.getFirst(), c);
        // if x − y op′ k′ in C is such that k′ != +Inf then
        if (guardUnderlyingNet.getValue() != Long.MAX_VALUE) {
            Constraint guardCopy = guard.clone();
            guardCopy.setValue(guardUnderlyingNet.getNegatedValue()); // Negate value
            guardCopy.setStrict(!guardUnderlyingNet.isStrict()); // Switch strictness
            Transition t2 = copy.dpn.getTransitions().get(t.getId());
            t2.setGuard(guardCopy);
            copy.modifiedTransitions.add(t.getId());
            copy.changes++;
            updatePriorityQueue(copy);
        }
    }

    protected void fixMissing(RepairDPN net, ConstraintGraph cg) {
//        ConstraintGraph.Node initial = cg.getInitialNode();
        // Let Missing be the set of missing transitions in CGN′ .
        Set<String> missingTransitions = getMissingTransitions(net.dpn, cg);
        for (String t : missingTransitions) {
            Transition missingTransition = net.dpn.getTransitions().get(t);
            // Let Nodes be the set of nodes (M,C) of the CGN′ such that t can fire from
            // marking M in the underlying Petri Net
            List<ConstraintGraph.Node> nodes = getFiring(cg, missingTransition);
            for (ConstraintGraph.Node node : nodes) {
                forwardRepair(net, missingTransition, node.getCanonicalForm());
                // Let BW be the set of non-silent transitions in all paths (M0,C0) -> (M,C).
                Set<Transition> previousTransitions = DPNUtils.getPreviousTransitions(net.dpn, cg, node, true);
                for (Transition previousTransition : previousTransitions) {
                    backForwardRepair(net, previousTransition);
                }
            }
        }
    }

    protected Set<String> getMissingTransitions(DPN dpn, ConstraintGraph cg) {
        Set<String> currentTransitions = cg.getArcs()
                .stream()
                .filter(arc -> !arc.isSilent())
                .map(ConstraintGraph.Arc::getTransition)
                .collect(Collectors.toSet());

        Set<String> transitions = new HashSet<>(dpn.getTransitions().keySet());
        transitions.removeAll(currentTransitions);
        return transitions;
    }

    protected List<ConstraintGraph.Node> getFiring(ConstraintGraph cg, Transition transition) {
        return cg.getNodes()
                .stream()
                .filter(node -> transition.isEnabled(node.getMarking()))
                .collect(Collectors.toList());
    }

    // “make the guard true''
    protected void backForwardRepair(RepairDPN net, Transition t) {
        // Let N′′ := (P, T, F, V, αI , guard′) be a copy of N′.
        RepairDPN copy = makeCopy(net);

        // Let y − x op k be the guard of t
        Transition t2 = copy.dpn.getTransitions().get(t.getId());
        // guard′′(t) := y − x ≤ +Inf
        Constraint newGuard = t2.getGuard().clone();
        newGuard.setValue(Long.MAX_VALUE);
        newGuard.setStrict(false);
        t2.setGuard(newGuard);
        copy.modifiedTransitions.add(t.getId());
        copy.changes++;
        updatePriorityQueue(copy);
    }

    static class RepairDPN {
        DPN dpn;
        int changes = 0;
        Set<String> modifiedTransitions = new HashSet<>();

        public RepairDPN(DPN dpn) {
            this.dpn = dpn;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RepairDPN net = (RepairDPN) o;
            return net.dpn.equals(this.dpn);
        }
    }
}
