package org.example.dpnrepair.semantics;

import org.example.dpnrepair.parser.ast.Constraint;
import org.example.dpnrepair.parser.ast.DPN;
import org.example.dpnrepair.parser.ast.Transition;

public class DPNRepairCyclic extends DPNRepairAcyclic {

    public DPNRepairCyclic(DPN dpn) {
        super(dpn);
    }

    protected void forwardRepair(RepairDPN net, Transition t, DifferenceConstraintSet c) {
        RepairDPN firstCopy = makeCopy(net);

        Constraint guard = t.getGuard();
        Constraint guardUnderlyingNet = getConstraintFromDifferenceConstraintSet(guard.getFirst(), guard.getSecond(), c);
        if (guardUnderlyingNet.getValue() == 0) {
            Transition t1 = firstCopy.dpn.getTransitions().get(t.getId());
            t1.setGuard(guardUnderlyingNet.clone());
            if (guard.getValue() != guardUnderlyingNet.getValue()) {
                firstCopy.modifiedTransitions.add(t.getId());
                firstCopy.changes++;
            }
            updatePriorityQueue(firstCopy);
        }

        RepairDPN secondCopy = makeCopy(net);
        guard = t.getGuard();
        guardUnderlyingNet = getConstraintFromDifferenceConstraintSet(guard.getFirst(), Constraint.ZED, c);
        Transition t2 = secondCopy.dpn.getTransitions().get(t.getId());
        t2.setGuard(guardUnderlyingNet.clone());
        secondCopy.modifiedTransitions.add(t.getId());
        secondCopy.changes++;
        updatePriorityQueue(secondCopy);

        RepairDPN thirdCopy = makeCopy(net);
        guard = t.getGuard();
        guardUnderlyingNet = getConstraintFromDifferenceConstraintSet(Constraint.ZED, guard.getSecond(), c);
        Transition t3 = thirdCopy.dpn.getTransitions().get(t.getId());
        t3.setGuard(guardUnderlyingNet.clone());
        thirdCopy.modifiedTransitions.add(t.getId());
        thirdCopy.changes++;
        updatePriorityQueue(thirdCopy);
    }

    protected void backwardRepair(RepairDPN net, Transition t, DifferenceConstraintSet c) {
        RepairDPN firstCopy = makeCopy(net);

        Constraint guard = t.getGuard();
        Constraint guardUnderlyingNet = getConstraintFromDifferenceConstraintSet(guard.getSecond(), guard.getFirst(), c);
        if (guardUnderlyingNet.getValue() == 0) {
            Constraint guardCopy = guard.clone();
            guardCopy.setStrict(!guardUnderlyingNet.isStrict()); // Switch strictness
            Transition t2 = firstCopy.dpn.getTransitions().get(t.getId());
            t2.setGuard(guardCopy);
            firstCopy.modifiedTransitions.add(t.getId());
            firstCopy.changes++;
            updatePriorityQueue(firstCopy);
        }

        RepairDPN secondCopy = makeCopy(net);
        Constraint guardUnderlyingNet2 = getConstraintFromDifferenceConstraintSet(guard.getSecond(), Constraint.ZED, c);
        if (guardUnderlyingNet2.getValue() != Long.MAX_VALUE) {
            Constraint guardCopy = guard.clone();
            guardCopy.setFirst(Constraint.ZED);
            guardCopy.setStrict(!guardUnderlyingNet2.isStrict()); // Switch strictness
            Transition t2 = secondCopy.dpn.getTransitions().get(t.getId());
            t2.setGuard(guardCopy);
            secondCopy.modifiedTransitions.add(t.getId());
            secondCopy.changes++;
            updatePriorityQueue(secondCopy);
        }


        RepairDPN thirdCopy = makeCopy(net);
        Constraint guardUnderlyingNet3 = getConstraintFromDifferenceConstraintSet(Constraint.ZED, guard.getFirst(), c);
        if (guardUnderlyingNet3.getValue() != Long.MAX_VALUE) {
            Constraint guardCopy = guard.clone();
            guardCopy.setSecond(Constraint.ZED);
            guardCopy.setStrict(!guardUnderlyingNet3.isStrict()); // Switch strictness
            Transition t2 = thirdCopy.dpn.getTransitions().get(t.getId());
            t2.setGuard(guardCopy);
            thirdCopy.modifiedTransitions.add(t.getId());
            thirdCopy.changes++;
            updatePriorityQueue(thirdCopy);
        }
    }

}
