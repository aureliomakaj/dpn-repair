package org.example.dpnrepair.parser.ast;

public class Graphics implements Cloneable {
    private Position position;
    private Dimension dimension;
    private String fill; // Hex

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public void setDimension(Dimension dimension) {
        this.dimension = dimension;
    }

    public String getFill() {
        return fill;
    }

    public void setFill(String fill) {
        this.fill = fill;
    }

    @Override
    public Graphics clone() {
        try {
            Graphics cloned = (Graphics) super.clone();
            cloned.setDimension(dimension.clone());
            cloned.setFill(fill);
            cloned.setPosition(position.clone());
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
