package org.example.dpnrepair.parser.ast;

public class Transition {
    private String id;
    private String name;
    private Constraint guard;
    private Graphics graphics;

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
}
