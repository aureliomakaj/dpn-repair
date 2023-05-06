package org.example.dpnrepair.parser.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DPN {
    private String id;
    private String name;
    private final Map<String, Place> places = new HashMap<>();
    private final Map<String, Transition> transitions = new HashMap<>();
    private final List<Arc> arcs = new ArrayList<>();
    private Marking initialMarking;
    private Marking finalMarking;
    private final Map<String, Variable> variables = new HashMap<>();
    private final Map<String, List<String[]>> adjacentList = new HashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Place> getPlaces() {
        return places;
    }

    public void addPlace(Place p) {
        if (p != null) {
            this.places.put(p.getId(), p);
        }
    }

    public Map<String, Transition> getTransitions() {
        return transitions;
    }

    public void addTransition(Transition t) {
        if (t != null) {
            this.transitions.put(t.getId(), t);
        }
    }

    public List<Arc> getArcs() {
        return arcs;
    }

    public void addArc(Arc arc) {
        if (arc != null) {
            this.arcs.add(arc);
        }
    }

    public Marking getInitialMarking() {
        return initialMarking;
    }

    public void setInitialMarking(Marking initialMarking) {
        this.initialMarking = initialMarking;
    }

    public Marking getFinalMarking() {
        return finalMarking;
    }

    public void setFinalMarking(Marking finalMarking) {
        this.finalMarking = finalMarking;
    }

    public Map<String, Variable> getVariables() {
        return variables;
    }

    public void addVariable(Variable variable) {
        if (variable != null) {
            this.variables.put(variable.getName(), variable);
        }
    }

    public void elaborateAdjacentList() {
        Map<String, String> placeToTransit = new HashMap<>();
        Map<String, String> transitToPlace = new HashMap<>();
        for (Arc a : arcs) {
            if (places.get(a.getSource()) != null) {
                placeToTransit.put(a.getSource(), a.getTarget());
            }
            if (transitions.get(a.getSource()) != null) {
                transitToPlace.put(a.getSource(), a.getTarget());
            }
        }
        for(Map.Entry<String, String> entry: placeToTransit.entrySet()) {
            adjacentList.putIfAbsent(entry.getKey(), new ArrayList<>());
            adjacentList.get(entry.getKey()).add(new String[]{transitToPlace.get(entry.getValue()), entry.getValue()});
        }
    }

}
