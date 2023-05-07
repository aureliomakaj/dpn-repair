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
    private final List<Arc> inputArcs = new ArrayList<>();
    private final List<Arc> outputArcs = new ArrayList<>();
    private Marking initialMarking;
    private Marking finalMarking;
    private final Map<String, Variable> variables = new HashMap<>();
    private final Map<String, List<String[]>> adjacentList = new HashMap<>();

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

    public void addInputArc(Arc arc) {
        if (arc != null) {
            this.inputArcs.add(arc);
        }
    }

    public void addOutputArc(Arc arc) {
        if (arc != null) {
            this.outputArcs.add(arc);
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

    public Map<String, List<String[]>> getAdjacentList() {
        return adjacentList;
    }

    /**
     * Build an adjacent list for the representation of the graph
     */
    public void elaborateAdjacentList() {
        Map<String, List<String>> transToPlace = new HashMap<>();
        for (Arc out : outputArcs) {
            transToPlace.putIfAbsent(out.getSource(), new ArrayList<>());
            transToPlace.get(out.getSource()).add(out.getTarget());
        }
        for (Arc in : inputArcs) {
            adjacentList.putIfAbsent(in.getSource(), new ArrayList<>());
            if (transToPlace.get(in.getTarget()) != null) {
                for (String targetPlace : transToPlace.get(in.getTarget())) {
                    adjacentList.get(in.getSource()).add(new String[]{targetPlace, in.getTarget()});
                }
            }
        }
    }

}
