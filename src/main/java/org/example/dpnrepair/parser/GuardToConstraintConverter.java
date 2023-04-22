package org.example.dpnrepair.parser;

import org.example.dpnrepair.parser.ast.Constraint;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuardToConstraintConverter {
    private final String LT = "<";
    private final String LEQ = "<=";
    private final String GT = ">";
    private final String GEQ = ">=";
    private final String MINUS = "-";

    public Constraint convert(String guard, List<String> readVars, List<String> writeVars) throws DPNParserException {
        Constraint constraint = new Constraint();
        constraint.setRead(readVars);
        constraint.setWritten(writeVars);
        parseGuard(guard, constraint);
        return constraint;
    }

    private void parseGuard(String guard, Constraint constraint) throws DPNParserException {
        guard = guard.replaceAll("\\s+", "");
        if (guard.charAt(0) == '(' && guard.charAt(guard.length() - 1) == ')') {
            guard = guard.substring(1, guard.length() - 1);
        }
        String token;
        if (guard.contains(LEQ)) {
            token = LEQ;
        } else if (guard.contains(LT)) {
            token = LT;
        } else if (guard.contains(GEQ)) {
            token = GEQ;
        } else if (guard.contains(GT)) {
            token = GT;
        } else {
            throw new DPNParserException("Invalid operator in guard \"" + guard + "\"");
        }

        String[] parts = guard.split(token);
        if (parts.length != 2) {
            throw new DPNParserException("Invalid guard \"" + guard + "\"");
        }

        if (parts[0].contains(MINUS)) {
            // X - Y
            String[] vars = parts[0].split(MINUS);
            long value = Long.valueOf(parts[1]);
            if (isLeastToken(token)) {
                // X - Y < K
                constraint.setFirst(vars[0]);
                constraint.setSecond(vars[1]);
                constraint.setValue(value);
            } else {
                // X - Y > K   =>   Y - X < -k
                constraint.setFirst(vars[1]);
                constraint.setSecond(vars[0]);
                constraint.setValue(-value);
            }

            if (token == LT || token == GT) {
                constraint.setStrict(true);
            }
        } else if (isVariable(parts[1])){
            if (isLeastToken(token)){
                // X < Y   =>   X - Y < 0
                constraint.setFirst(parts[0]);
                constraint.setSecond(parts[1]);
                constraint.setValue(0L);
                constraint.setStrict(token == LT);
            }else{
                // X > Y   =>   Y - X < 0
                constraint.setFirst(parts[1]);
                constraint.setSecond(parts[0]);
                constraint.setValue(0L);
                constraint.setStrict(token == GT);
            }
        } else {
            if (isLeastToken(token)){
                // X < K   =>   X - Z < K
                constraint.setFirst(parts[0]);
                constraint.setSecond(Constraint.ZETA);
                constraint.setValue(Long.valueOf(parts[1]));
                constraint.setStrict(token == LT);
            }else{
                // X > K   =>   Z - X < -k
                constraint.setFirst(Constraint.ZETA);
                constraint.setSecond(parts[0]);
                constraint.setValue(-(Long.valueOf(parts[1])));
                constraint.setStrict(token == GT);
            }
        }
    }

    private boolean isVariable(String part) {
        Pattern p = Pattern.compile("^[a-z][a-z0-9_]*$");
        Matcher m = p.matcher(part);
        return m.matches();
    }

    private boolean isLeastToken(String token) {
        return token == LT || token == LEQ;
    }
}
