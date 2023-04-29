package org.example.dpnrepair.parser.ast;

import java.util.List;

public class Constraint {
    public static final String ZED = "Z";
    private String first;
    private String second;
    private boolean strict = false;
    private long value;
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

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
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

    public String toStringWithoutZed() {
        String res = "";
        if(first.equals(ZED)) {
            res += second;
            res += strict ? " > " : " >= ";
            res += (-value);
        } else if (second.equals(ZED)) {
            res += first;
            res += strict ? " < " : " <= ";
            res += value;
        } else {
            res += (first + " - " + second);
            res += strict ? " < " : " <= ";
            res += value;
        }
        return res;
    }

    public boolean canCompareTo(Constraint other) {
        return (getFirst().equals(other.getFirst()) || getFirst().equals(other.getSecond())) &&
                (getSecond().equals(other.getFirst()) || getSecond().equals(other.getSecond()));
    }

    public boolean isEqualTo(Constraint other) {
        return getValue() == other.getValue() && isStrict() == other.isStrict();
    }

    public boolean isLessThan(Constraint other) {
        long k1 = getValue();
        long k2 = other.getValue();
        boolean first = k1 < k2;
        boolean second = false;
        if (k1 == k2) {
            second = isStrict() && !other.isStrict();
        }

        return first || second;
    }

    public boolean isLessThanOrEqualTo(Constraint other) {
        return isLessThan(other) || isEqualTo(other);
    }

}
