package org.example.dpnrepair.semantics;

import org.example.dpnrepair.parser.ast.DPN;

import java.util.PriorityQueue;
import java.util.Set;

public interface DPNRepair {
    DPN getRepaired();

    void repair();

    PriorityQueue<DPNRepairAcyclic.RepairDPN> getPriorityQueue();

    int getDistance();

    Set<DPN> getVisitedDpn();

    Set<String> getModifiedTransitions();

    int getIterations();
}
