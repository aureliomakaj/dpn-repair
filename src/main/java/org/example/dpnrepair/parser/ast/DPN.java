package org.example.dpnrepair.parser.ast;

import java.util.List;
import java.util.Map;

public class DPN {
    private String id;
    private String name;
    private Map<String, Place> places;
    private Map<String, Transition> transitions;
    private List<Arc> arcs;
    private Marking initialMarking;
    private Marking finalMarking;
    private Map<String, Integer> variables;

}
