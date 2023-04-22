package org.example.dpnrepair.parser.ast;

import java.util.List;

public class Constraint {
    public static final String ZETA = "Z";
    private String first;
    private String second;
    private boolean strict = false;
    private Long value;
    private List<String> read;
    private List<String> written;

    public String getFirst() {
        return first;
    }

    public void setFirst(String first) {
        this.first = first;
    }

    public String getSecond() {
        return second;
    }

    public void setSecond(String second) {
        this.second = second;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }

    public List<String> getRead() {
        return read;
    }

    public void setRead(List<String> read) {
        this.read = read;
    }

    public List<String> getWritten() {
        return written;
    }

    public void setWritten(List<String> written) {
        this.written = written;
    }

    @Override
    public String toString() {
        String op = strict ? "<" : "<=";
        return first + " - " + second + " " + op + " " + value;
    }
}
