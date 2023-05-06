package org.example.dpnrepair.semantics;

import org.example.dpnrepair.parser.ast.Marking;

import java.util.Map;

public class State {
    private final Marking marking;
    private final Assignment assignment;

    public State(Marking marking, Assignment assignment) {
        this.marking = marking;
        this.assignment = assignment;
    }

    public Marking getMarking() {
        return marking;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    class Assignment {
        // Variable - value mapping
        private final Map<String, Long> values;

        public Assignment(Map<String, Long> values) {
            this.values = values;
        }

        public Map<String, Long> getValues() {
            return values;
        }
    }
}
