package org.example.dpnrepair.parser.ast;

import org.example.dpnrepair.EdgeType;
import org.example.dpnrepair.semantics.ConstraintGraph;

import java.util.*;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DPN net = (DPN) o;
        return net.places.keySet().equals(this.places.keySet()) && this.sameTransitions(net.transitions, this.transitions) && net.arcs.equals(this.arcs);
    }

    private boolean sameTransitions(Map<String, Transition> first, Map<String, Transition> second) {
        if(!first.keySet().equals(second.keySet())){
            return false;
        }
        for (Map.Entry<String, Transition> f: first.entrySet()) {
            if(!second.get(f.getKey()).equals(f.getValue())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(places, transitions, arcs);
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
