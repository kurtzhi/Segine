package com.kurtzhi.Segine;

import java.util.ArrayList;
import java.util.Arrays;

public class Event {
    final static private String EventExprPattern = "^([a-zA-Z][\\w]*(\\,[a-zA-Z][\\w]*)*)*";
    final private static String EvtsDivPattern = ",";
    protected String identifier;

    public Event(String identifier) {
        this.identifier = identifier.replaceAll(" ", "");
    }

    protected static String expressionToIdentifer(String expr) {
        if (_isExpressionMatch(expr)) {
            if (expr.length() > 0) {
                String[] strArr = expr.split(EvtsDivPattern);
                Arrays.sort(strArr);
                expr = Arrays.toString(strArr);
                return expr.substring(1, expr.length() - 1).replaceAll(" ", "");
            }
        }

        return "*";
    }

    protected static String eventsToIdentifier(ArrayList<String> events) {
        String expr = Arrays.toString(events.toArray());
        expr = expr.substring(1, expr.length() - 1);
        return expressionToIdentifer(expr);
    }

    protected static ArrayList<String> expressionToEvents(String expr) {
        if (_isExpressionMatch(expr)) {
            String[] strArr = expr.split(EvtsDivPattern);
            ArrayList<String> evts = new ArrayList<String>();
            evts.addAll(Arrays.asList(strArr));
            return evts;
        }

        return null;
    }

    protected static ArrayList<String> identifierToEvents(String identifer) {
        if (identifer == null || identifer.length() == 0) {
            return null;
        }

        return expressionToEvents(identifer);
    }

    private static boolean _isExpressionMatch(String expr) {
        if (expr != null && expr.length() > 0 && expr.matches(EventExprPattern)) {
            return true;
        }

        return false;
    }
}
