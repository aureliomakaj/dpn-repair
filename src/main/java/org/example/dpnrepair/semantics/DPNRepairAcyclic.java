package org.example.dpnrepair.semantics;

import org.example.dpnrepair.ConstraintGraphPrinter;
import org.example.dpnrepair.DPNPrinter;
import org.example.dpnrepair.DPNUtils;
import org.example.dpnrepair.parser.ast.Constraint;
import org.example.dpnrepair.parser.ast.DPN;
import org.example.dpnrepair.parser.ast.Transition;

import javax.xml.soap.Node;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DPNRepairAcyclic {
    protected final DPN toRepair;
    protected DPN repaired;
    PriorityQueue<RepairDPN> priorityQueue;
    protected int distance = 0;

    protected final Set<Set<String>> visitedDpn = new HashSet<>();


    public DPNRepairAcyclic(DPN dpn) {
        this.toRepair = dpn;
        priorityQueue = new PriorityQueue<>(Comparator.comparingInt(o -> o.modifiedTransitions.size()));
    }

    public DPN getRepaired() {
        return repaired;
    }

    public int getDistance() {
        return distance;
    }

    public void repair() {
        priorityQueue.add(new RepairDPN(toRepair));
        RepairDPN net;
        while (true) {
            net = priorityQueue.remove();
            setVisited(net.dpn);
            ConstraintGraph cg = new ConstraintGraph(net.dpn);
            if (cg.isDataAwareSound()) {
                ConstraintGraphPrinter a = new ConstraintGraphPrinter(cg);
                a.writeRaw("solution");
                break;
            }
            fixDead(net, cg);
            fixMissing(net, cg);
        }
        this.repaired = net.dpn;
        this.distance = net.modifiedTransitions.size();
        DPNPrinter dpnPrinter = new DPNPrinter(this.repaired);
        dpnPrinter.writeTransitions("solution_transitions.txt");
    }

    /**
     * In our algorithm, the only thing that can change between different DPNs
     * are the guards of transitions, thus we base on that to see if a dpn
     * has already been visited
     */
    protected void setVisited(DPN dpn) {
        visitedDpn.add(getDPNKey(dpn));
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
        if (!visitedDpn.contains(getDPNKey(net.dpn))) {
//            net.visited = true;
            priorityQueue.add(net);
        }
    }

    protected void fixDead(RepairDPN net, ConstraintGraph cg) {
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
            Set<Transition> previousTransitions = DPNUtils.getPreviousTransitions(net.dpn, cg, curr, false);
            for (Transition t : previousTransitions) {
                backwardRepair(net, t, curr.getCanonicalForm());
            }
        }
    }

    protected void forwardRepair(RepairDPN net, Transition t, DifferenceConstraintSet c) {
        RepairDPN copy = makeCopy(net);

        Constraint guard = t.getGuard();
        Constraint guardUnderlyingNet = getConstraintFromDifferenceConstraintSet(guard.getFirst(), guard.getSecond(), c);
        Transition t2 = copy.dpn.getTransitions().get(t.getId());
        t2.setGuard(guardUnderlyingNet.clone());
        copy.modifiedTransitions.add(t.getId());
        copy.changes++;
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

        return constraint.orElse(new Constraint(first, second));
    }

    protected void backwardRepair(RepairDPN net, Transition t, DifferenceConstraintSet c) {
        RepairDPN copy = makeCopy(net);

        Constraint guard = t.getGuard();
        Constraint guardUnderlyingNet = getConstraintFromDifferenceConstraintSet(guard.getSecond(), guard.getFirst(), c);
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
        ConstraintGraph.Node initial = cg.getInitialNode();
        Set<String> missingTransitions = getMissingTransitions(net.dpn, cg);
        for (String t : missingTransitions) {
            Transition missingTransition = net.dpn.getTransitions().get(t);
            List<ConstraintGraph.Node> nodes = getFiring(cg, missingTransition);
            for (ConstraintGraph.Node node : nodes) {
                forwardRepair(net, missingTransition, node.getCanonicalForm());
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

    protected void backForwardRepair(RepairDPN net, Transition t) {
        RepairDPN copy = makeCopy(net);

        Transition t2 = copy.dpn.getTransitions().get(t.getId());
        Constraint newGuard = t2.getGuard().clone();
        newGuard.setValue(Long.MAX_VALUE);
        newGuard.setStrict(false);
        t2.setGuard(newGuard);
        copy.modifiedTransitions.add(t.getId());
        copy.changes++;
        updatePriorityQueue(copy);
    }

    public void checkCoreachabilitySoundness(DPN dpn, ConstraintGraph cg) {
        Map<ConstraintGraph.Node, Set<DifferenceConstraintSet>> nodeMap = new HashMap<>();
        for (ConstraintGraph.Node node : cg.getNodes()) {
            if (node.isFinal()) {
                nodeMap.put(node, new HashSet<>(Collections.singleton(node.getCanonicalForm())));
            } else {
                nodeMap.put(node, new HashSet<>());
            }
        }
        Map<ConstraintGraph.Node, Set<DifferenceConstraintSet>> nodeMapPrev = new HashMap<>();
        while (!equalMaps(nodeMap, nodeMapPrev)) {
            nodeMapPrev.putAll(nodeMap);
            nodeMap = new HashMap<>();

        }
    }

    private boolean equalMaps(
            Map<ConstraintGraph.Node, Set<DifferenceConstraintSet>> first,
            Map<ConstraintGraph.Node, Set<DifferenceConstraintSet>> second) {
        return !first.isEmpty() && first.entrySet()
                .stream()
                .allMatch(entry -> {
                    Set<DifferenceConstraintSet> firstsSet = first.get(entry.getKey());
                    Set<DifferenceConstraintSet> secondSet = second.get(entry.getKey());
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
        Set<DifferenceConstraintSet> result = nodeMap.get(node);
        List<ConstraintGraph.Arc> arcs = cg.getArcs()
                .stream()
                .filter(arc -> arc.getOrigin() == node.getId())
                .collect(Collectors.toList());

        Set<ConstraintGraph.Node> readOrSilent = new HashSet<>();
        Set<ConstraintGraph.Node> write = new HashSet<>();
        for (ConstraintGraph.Arc arc : arcs) {
            Transition t = dpn.getTransitions().get(arc.getTransition());
            if (t.getGuard().getWritten().size() == 0 || arc.isSilent()) {
                readOrSilent.add(idNodeMap.get(arc.getTransition()));
            } else {
                write.add(idNodeMap.get(arc.getTransition()));
            }
        }
        for (ConstraintGraph.Node nodePrime : readOrSilent) {
            result.addAll(nodeMap.get(nodePrime));
        }
        for (ConstraintGraph.Node nodePrime : write) {
            //
        }
        return result;
    }

    private DifferenceConstraintSet computeExists() {
        // TODO
        return null;
    }

    static class RepairDPN {
        DPN dpn;
        int changes = 0;
        Set<String> modifiedTransitions = new HashSet<>();

        public RepairDPN(DPN dpn) {
            this.dpn = dpn;
        }

    }
}
