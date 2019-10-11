package com.kurtzhi.Segine;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import com.kurtzhi.Segine.Logger.TraceLevel;
import com.kurtzhi.Segine.ManagedObjectHelper.MethodType;

/*
 * You can define actions either on transitions or on entry or exit of a state. 
 * Transition actions are executed when the transition is taken as response to 
 * an event. The entry and exit action of a state are executed when the state 
 * machine enters or exits the state due to a taken transition. In case of 
 * hierarchical states, multiple entry and exit actions can be executed. 
 */

class Action {
    final private static String ActionExprPattern = "^([a-zA-Z][\\w]*\\.[a-zA-Z][\\w]*(\\,[a-zA-Z][\\w]*\\.[a-zA-Z][\\w]*)*)*";
    final private static String ObjFuncDivPattern = "\\.";
    final private static String EvtsDivPattern = ",";
    protected StateMachine sm;
    protected ActionQueue actionQueue = new ActionQueue();

    public static void Execute(StateMachineRuntime runtime, Action action) {
        if (action == null) {
            return;
        }

        StateMachine sm = action.sm;

        for (ActionExpr ae : action.actionQueue.exprs) {
            ManagedObjectHelper moh = sm.getManagedObjectHelper(ae.id);
            if (moh == null) {
                Logger.log(TraceLevel.Error, Action.class.getSimpleName(),
                        "Wrong managed object identifier \"" + ae.id
                                + "\" specified");
            }
            Method method = moh.getMethod(MethodType.VOID, ae.name);
            if (method == null) {
                Logger.log(TraceLevel.Error, Action.class.getSimpleName(),
                        "Wrong method name \"" + ae.name + "\"  specified");
            }

            ManagedObject mo = runtime.getManagedObject(ae.id);
            try {
                method.invoke(mo);
            } catch (IllegalAccessException e) {
                Logger.log(TraceLevel.Error, Action.class.getSimpleName(),
                        e.getMessage());
            } catch (IllegalArgumentException e) {
                Logger.log(TraceLevel.Error, Action.class.getSimpleName(),
                        e.getMessage());
            } catch (InvocationTargetException e) {
                Logger.log(TraceLevel.Error, Action.class.getSimpleName(),
                        e.getMessage());
            }
        }
    }

    protected Action(String expr) {
        expr = expr.replaceAll(" ", "");
        if (expr.matches(ActionExprPattern)) {
            String[] strs;
            if (expr.length() > 0) {
                String[] actions = expr.split(EvtsDivPattern);
                for (String action : actions) {
                    strs = action.split(ObjFuncDivPattern);
                    actionQueue.exprs.add(new ActionExpr(strs[0], strs[1]));
                }
            }
        }
    }

    private class ActionExpr {
        public String id;
        public String name;

        public ActionExpr(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private class ActionQueue {
        ArrayList<ActionExpr> exprs = new ArrayList<ActionExpr>();
    }
}