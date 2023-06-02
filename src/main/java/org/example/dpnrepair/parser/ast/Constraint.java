package org.example.dpnrepair.parser.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Constraint implements Cloneable {
    public static final String ZED = "Z";
    private String first;
    private String second;
    private boolean strict = false;
    private long value;
    private List<String> read = new ArrayList<>();
    private List<String> written = new ArrayList<>();

    public Constraint() {}

    public Constraint(String first, String second) {
        this.first = first;
        this.second = second;
        value = Long.MAX_VALUE;
        read.add(first);
        read.add(second);
    }

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

    public long getNegatedValue() {
        if (value == Long.MAX_VALUE) {
            return Long.MIN_VALUE;
        } else if (value == Long.MIN_VALUE) {
            return Long.MAX_VALUE;
        } else {
            return -value;
        }
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

    public void addRead(String read) {
        if (!read.isEmpty()) {
            this.read.add(read);
        }
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
        if (first.equals(ZED)) {
            res += second;
            res += strict ? " > " : " >= ";
            res += this.getNegatedValue();
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

    public Constraint getNegated() {
        Constraint c = new Constraint();
        c.setFirst(this.second);
        c.setSecond(this.first);
        c.setStrict(!this.strict);
        c.setValue(this.getNegatedValue());
        c.setRead(this.read);
        c.setWritten(this.written);
        return c;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Constraint that = (Constraint) o;
        return strict == that.strict && value == that.value && Objects.equals(first, that.first) && Objects.equals(second, that.second) && Objects.equals(read, that.read) && Objects.equals(written, that.written);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second, strict, value, read, written);
    }

    @Override
    public Constraint clone() {
        try {
            Constraint out = (Constraint) super.clone();
            out.setRead(new ArrayList<>(this.read));
            out.setWritten(new ArrayList<>(this.written));
            return out;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
