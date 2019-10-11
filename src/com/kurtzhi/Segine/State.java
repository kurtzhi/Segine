package com.kurtzhi.Segine;

import com.kurtzhi.Segine.Logger.TraceLevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class State {
    private Map<String, ArrayList<String>> evts = new HashMap<String, ArrayList<String>>();
    private Map<String, ExpressionWrapper> evtToExpr = new HashMap<String, ExpressionWrapper>();
    private Map<String, ActionState> exprToActionState = new HashMap<String, ActionState>();
    private Action entryAction;
    private Action exitAction;
    protected StateMachine sm;
    protected StateType type;
    protected State sub;
    protected State sup;
    protected String identifier;

    public void addSubState(State to) {
        _addSubState(this, to);
    }

    public void addEntryAction(Action action) {
        _addEntryAction(this, action);
    }

    public void addExitAction(Action action) {
        _addExitAction(this, action);
    }

    protected State(String identifier, StateType type) {
        this.identifier = identifier;
        this.type = type;
    }

    protected static State switchState(State originState,
            StateMachineRuntime runtime) {
        State newState = null;
        ArrayList<String> events = null;

        if (originState.sub != null) {
            newState = originState.sub;
            if (newState.entryAction != null) {
                Action.Execute(runtime, newState.entryAction);
            }
            return newState;
        }

        if (originState.type == StateType.INITIAL) {
            return _trySwitchState(runtime, null, originState, originState);
        } else if (originState.type == StateType.FINAL) {
            return _trySwitchState(runtime, null, originState, originState.sup);
        } else {
            events = runtime.popEvents();
        }

        if (originState.exitAction != null) {
            Action.Execute(runtime, originState.exitAction);
        }

        String evtId;
        if (events != null && events.size() > 0) {
            Map<String, ArrayList<String>> evts = originState.evts;
            Iterator<String> iterator = evts.keySet().iterator();
            while (iterator.hasNext()) {
                evtId = iterator.next();
                if (evtId.equals("*") || events.containsAll(evts.get(evtId))) {
                    if ((newState = _trySwitchState(runtime, evtId,
                            originState, originState)) != null) {
                        return newState;
                    }
                }
            }

            // event *
            evtId = Event.expressionToIdentifer((String) null);
            if ((newState = _trySwitchState(runtime, evtId, originState,
                    originState)) != null) {
                return newState;
            }

            // parent state
            if (originState.sup != null
                    && originState.sup != originState.sm.getTopState()) {
                // exit action on parent state
                if (originState.sup.exitAction != null) {
                    Action.Execute(runtime, originState.sup.exitAction);
                }

                // match specific event(s)
                evts = originState.sup.evts;
                iterator = originState.sup.evts.keySet().iterator();
                while (iterator.hasNext()) {
                    evtId = iterator.next();
                    if (evtId.equals("*")
                            || events.containsAll(evts.get(evtId))) {
                        if ((newState = _trySwitchState(runtime, evtId,
                                originState, originState.sup)) != null) {
                            return newState;
                        }
                    }
                }

                // match all events (*)
                evtId = Event.expressionToIdentifer((String) null);
                if ((newState = _trySwitchState(runtime, evtId, originState,
                        originState.sup)) != null) {
                    return newState;
                }
            }
        }

        return newState;
    }

    /*
     * Transitions are state switches that are executed in response of an event 
     * that was fired onto the state machine. You can define per state and 
     * event which transition is taken and therefore which state to go to. 
     */
    protected static void addTransition(State from, Guard guard, Action action,
            State to) {
        if (null == from || null == to) {
            Logger.log(TraceLevel.Error, State.class.getSimpleName(),
                    "Either from state \"" + from.identifier
                            + "\" or to state \"" + to.identifier
                            + "\" is null");
            return;
        }
        if (null == from.sup) {
            Logger.log(TraceLevel.Error, State.class.getSimpleName(),
                    "From state \"" + from.identifier
                            + "\"'s parent state cannot be null");
            return;
        }
        if (from == from.sm.getTopState()) {
            Logger.log(TraceLevel.Error, State.class.getSimpleName(),
                    "Cannot set transition for top state \"" + from.identifier
                            + "\"");
            return;
        } else if (from.type == StateType.INITIAL
                && from.evtToExpr.size() == 1) {
            Logger.log(TraceLevel.Error, State.class.getSimpleName(),
                    "Cannot set more than one transition for initial state \""
                            + from.identifier + "\"");
            return;
        } else if (from.type == StateType.FINAL) {
            Logger.log(TraceLevel.Error, State.class.getSimpleName(),
                    "Cannot set outgoing transitions for final state \""
                            + from.identifier + "\"");
            return;
        }

        if (to.type == StateType.INITIAL || to == from.sm.getTopState()) {
            Logger.log(TraceLevel.Error, State.class.getSimpleName(),
                    "Transition target state \"" + to.identifier
                            + "\" cannot be top and initial");
            return;
        }

        if (to.sup != null && to.sup != from.sup) {
            Logger.log(
                    TraceLevel.Error,
                    State.class.getSimpleName(),
                    "Transition state\""
                            + from.identifier
                            + "\" and \""
                            + to.identifier
                            + "\" cannot connect since they belong to different state \""
                            + from.sup.identifier + "\" and \""
                            + to.sup.identifier + "\"");
            return;
        }

        String evtId = guard == null ? null : guard.evtId;
        LogicExpression lgcExpr = guard == null ? null : guard.lgcExpr;
        String lgcExprId = lgcExpr == null ? null : lgcExpr.identifier;
        if (evtId == null && from.type != StateType.INITIAL
                && from.type != StateType.FINAL) {
            evtId = Event.expressionToIdentifer(evtId);
        }

        ExpressionWrapper exprWrapper = from.evtToExpr.get(evtId);
        if (exprWrapper != null) {
            if (exprWrapper.lgcExprIds.contains(lgcExprId)) {
                Logger.log(TraceLevel.Error, State.class.getSimpleName(),
                        "Duplicate guard \"" + lgcExprId + "\" in state");
                return;
            }

            int sn = exprWrapper.lgcExprs.size();
            String key = evtId + "-" + sn;
            ActionState as = from.exprToActionState.get(key);
            if (as != null) {
                if (as.toState.identifier == to.identifier) {
                    Logger.log(TraceLevel.Error, State.class.getSimpleName(),
                            "Duplicate transition from \"" + from.identifier
                                    + "\" to \"" + to.identifier
                                    + "\" dectected");
                } else {
                    Logger.log(TraceLevel.Error, State.class.getSimpleName(),
                            "Ambiguous target state for same condition \""
                                    + lgcExprId + "\"");
                }
                return;
            } else {
                exprWrapper.lgcExprIds.add(lgcExprId);
                exprWrapper.lgcExprs.add(lgcExpr);
                from.exprToActionState.put(key, new ActionState(action,
                        to));
            }
        } else {
            if (evtId != null) {
                exprWrapper = new ExpressionWrapper();
                exprWrapper.lgcExprIds.add(lgcExprId);
                exprWrapper.lgcExprs.add(lgcExpr);
            } else {
                exprWrapper = null;
            }

            from.evts.put(evtId, Event.identifierToEvents(evtId));

            from.evtToExpr.put(evtId, exprWrapper);
            from.exprToActionState.put(evtId + "-" + 0, new ActionState(
                    action, to));
        }

        to.sup = from.sup;
    }

    private static void _addSubState(State state, State sub) {
        if (state.type == StateType.INITIAL || state.type == StateType.FINAL) {
            Logger.log(
                    TraceLevel.Error,
                    State.class.getSimpleName(),
                    "State \""
                            + state.identifier
                            + "\" is either initial or final, not allowed add substate for it");
            return;
        }

        if (sub == state.sm.getTopState() || sub.type == StateType.FINAL) {
            Logger.log(
                    TraceLevel.Error,
                    State.class.getSimpleName(),
                    "State \""
                            + sub.identifier
                            + "\" is either top or final, not allowed to be used as substate");
            return;
        }

        if (!state.sm.isInitialized()) {
            if (sub.sm == state.sm) {
                if (state.sub == null) {
                    State rgState;
                    if ((rgState = state.sm.getState(sub.identifier)) != null) {
                        if (null == rgState.sup) {
                            state.sub = sub;
                            sub.sup = state;
                        } else {
                            if (state == rgState.sup) {
                                Logger.log(TraceLevel.Error,
                                        State.class.getSimpleName(),
                                        "Same substate \"" + sub.identifier
                                                + "\" already exist");
                            } else {
                                Logger.log(TraceLevel.Error,
                                        State.class.getSimpleName(),
                                        "Substate \"" + sub.identifier
                                                + "\" in multi states detected");
                            }
                        }
                    }
                } else {
                    Logger.log(TraceLevel.Error, State.class.getSimpleName(),
                            "State \"" + state.identifier
                                    + "\" cannot have more than one substate");
                    return;
                }
            } else {
                Logger.log(
                        TraceLevel.Error,
                        State.class.getSimpleName(),
                        "\""
                                + state.identifier
                                + "\" and \""
                                + sub.identifier
                                + "\" are states of different state machines, and cannot connect");
            }
        } else {
            // TODO : state machine not yet initialized
        }
    }

    private static void _addEntryAction(State state, Action action) {
        if (state == state.sm.getTopState() || state.type == StateType.INITIAL
                || state.type == StateType.FINAL) {
            Logger.log(
                    TraceLevel.Error,
                    State.class.getSimpleName(),
                    "State \""
                            + state.identifier
                            + "\" should be top or initial or final, entry action not allowed");
        }

        if (!state.sm.isInitialized()) {
            if (state.entryAction != null) {
                Logger.log(TraceLevel.Error, State.class.getSimpleName(),
                        "Multi entry action specified for state \""
                                + state.identifier + "\"");
            } else {
                state.entryAction = action;
            }
        }
    }

    private static void _addExitAction(State state, Action action) {
        if (state == state.sm.getTopState() || state.type == StateType.INITIAL
                || state.type == StateType.FINAL) {
            Logger.log(
                    TraceLevel.Error,
                    State.class.getSimpleName(),
                    "State \""
                            + state.identifier
                            + "\" should be top or initial or final, exit action not allowed");
        }

        if (!state.sm.isInitialized()) {
            if (state.exitAction != null) {
                Logger.log(TraceLevel.Error, State.class.getSimpleName(),
                        "Multi exit action specified for state \""
                                + state.identifier + "\"");
            } else {
                state.exitAction = action;
            }
        }
    }

    private static State _trySwitchState(StateMachineRuntime runtime,
            String evtId, State originState, State realState) {
        State newState = null;
        ExpressionWrapper beq = realState.evtToExpr.get(evtId);
        if (beq == null) {
            ActionState as = realState.exprToActionState.get(evtId + "-"
                    + 0);
            if (as == null) {
                return null;
            }

            Action.Execute(runtime, as.action);
            newState = as.toState;
            /*
             * if (originState.exitAction != null) { Action.Execute(runtime,
             * originState.exitAction); } if (newState != realState && realState
             * == originState.sup && realState.exitAction != null) {
             * Action.Execute(runtime, realState.exitAction); }
             */

            if (newState.entryAction != null) {
                Action.Execute(runtime, newState.entryAction);
            }

            return newState;
        }

        int length = beq.lgcExprs.size();
        LogicExpression lgcExpr = null;
        for (int i = 0; i < length; i++) {
            lgcExpr = beq.lgcExprs.get(i);
            if (lgcExpr == null || LogicExpression.Test(runtime, lgcExpr)) {
                ActionState as = realState.exprToActionState.get(evtId
                        + "-" + i);
                Action.Execute(runtime, as.action);
                newState = as.toState;
                /*
                 * if (originState.exitAction != null) { Action.Execute(runtime,
                 * originState.exitAction); }
                 * 
                 * if (newState != realState && realState == originState.sup &&
                 * realState.exitAction != null) { Action.Execute(runtime,
                 * realState.exitAction); }
                 */

                if (newState.entryAction != null) {
                    Action.Execute(runtime, newState.entryAction);
                }
                break;
            }
        }

        return newState;
    }

    private static class ActionState {
        protected Action action;
        protected State toState;

        protected ActionState(Action action, State toState) {
            this.action = action;
            this.toState = toState;
        }
    }

    private static class ExpressionWrapper {
        protected ArrayList<String> lgcExprIds = new ArrayList<String>();
        protected ArrayList<LogicExpression> lgcExprs = new ArrayList<LogicExpression>();
    }
}