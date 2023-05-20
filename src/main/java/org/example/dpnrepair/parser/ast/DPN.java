package org.example.dpnrepair.parser.ast;

import org.example.dpnrepair.EdgeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DPN implements Cloneable {
    private String id;
    private String name;
    private final Map<String, Place> places = new HashMap<>();
    private Map<String, Transition> transitions = new HashMap<>();
    private final List<Arc> arcs = new ArrayList<>();
    private Marking initialMarking;
    private Marking finalMarking;
    private final Map<String, Variable> variables = new HashMap<>();

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

    @Override
    public DPN clone() {
        try {
            DPN cloned = (DPN) super.clone();
            cloned.transitions = new HashMap<>();
            for (Transition t: transitions.values()){
                cloned.addTransition(t.clone());
            }
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
