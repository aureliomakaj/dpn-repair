package org.example.dpnrepair.parser.ast;

import java.util.HashMap;
import java.util.Map;

public class Transition implements Cloneable {
    private String id;
    private String name;
    private Constraint guard;
    private Graphics graphics;
    // Place -> Token map. What is required to fire the transition
    private final Map<String, Integer> enabling = new HashMap<>();
    // Place -> Token map. Output of transition
    private final Map<String, Integer> output = new HashMap<>();

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

    public Constraint getGuard() {
        return guard;
    }

    public void setGuard(Constraint guard) {
        this.guard = guard;
    }

    public Graphics getGraphics() {
        return graphics;
    }

    public void setGraphics(Graphics graphics) {
        this.graphics = graphics;
    }

    public void addEnabling(String placeId, int token) {
        this.enabling.put(placeId, token);
    }

    public Map<String, Integer> getEnabling() {
        return enabling;
    }

    public Map<String, Integer> getOutput() {
        return output;
    }

    public void addOutput(String placeId, int token) {
        this.output.put(placeId, token);
    }

    public boolean isEnabled(Marking marking) {
        return enabling.entrySet()
                .stream()
                .allMatch( entry -> marking.getPlaceTokenMap().get(entry.getKey()) >= entry.getValue());
    }

    @Override
    public Transition clone() {
        try {
             Transition cloned = (Transition) super.clone();
             cloned.setGuard(guard.clone());
             return cloned;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
