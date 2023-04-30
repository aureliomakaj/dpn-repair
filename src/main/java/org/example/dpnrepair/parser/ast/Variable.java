package org.example.dpnrepair.parser.ast;

public class Variable implements Cloneable{
    private String name;
    private long minValue;
    private long maxValue;
    private long initialValue;
    private Graphics graphics;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getMinValue() {
        return minValue;
    }

    public void setMinValue(long minValue) {
        this.minValue = minValue;
    }

    public long getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(long maxValue) {
        this.maxValue = maxValue;
    }

    public long getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(long initialValue) {
        this.initialValue = initialValue;
    }

    public Graphics getGraphics() {
        return graphics;
    }

    public void setGraphics(Graphics graphics) {
        this.graphics = graphics;
    }

    public Variable clone() {
        try {
            return (Variable) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
