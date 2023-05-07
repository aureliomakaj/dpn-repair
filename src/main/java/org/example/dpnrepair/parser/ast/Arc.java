package org.example.dpnrepair.parser.ast;

public class Arc {
    private String id;
    private String name;
    private String source;
    private String target;
    private String arctype;
    private boolean input = true; // true is input arc, false is output arc

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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String destination) {
        this.target = destination;
    }

    public String getArctype() {
        return arctype;
    }

    public void setArctype(String arctype) {
        this.arctype = arctype;
    }

    public boolean isInput() {
        return input;
    }

    public void setInput(boolean input) {
        this.input = input;
    }
}
