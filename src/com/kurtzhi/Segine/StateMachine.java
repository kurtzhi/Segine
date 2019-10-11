package com.kurtzhi.Segine;

import java.lang.reflect.InvocationTargetException;

import com.kurtzhi.Segine.Logger.TraceLevel;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StateMachine {
    private final String topIdentifier = "top";
    private String identifier;
    private State top;
    private State currentState;
    private Map<String, EventProviderHelper> eventProviderHelpers = new HashMap<String, EventProviderHelper>();
    private Map<String, ManagedObjectHelper> managedObjectHelpers = new HashMap<String, ManagedObjectHelper>();
    private ArrayList<String> eventProviderIds = new ArrayList<String>();
    private ArrayList<String> managedObjectIds = new ArrayList<String>();
    private Map<Class<? extends EventProvider>, String> eventProviderIdMap = new HashMap<Class<? extends EventProvider>, String>();
    private Map<Class<? extends ManagedObject>, String> managedObjectIdMap = new HashMap<Class<? extends ManagedObject>, String>();
    private Map<String, State> states = new HashMap<String, State>();
    private boolean initializedFlag = false;
    private ArrayList<String> events = new ArrayList<String>();

    protected StateMachine(String identifier) {
        this.identifier = identifier;
        top = new State(topIdentifier, null);
        top.sm = this;
        states.put(topIdentifier, top);
    }

    protected boolean registerEvent(String event) {
        if (!events.contains(event)) {
            events.add(event);
            return true;
        }
        return false;
    }

    private void disposeAssociatedObjects(StateMachineRuntime runtime) {
        Method m;
        try {
            for (String epId : eventProviderIds) {
                EventProviderHelper eph;
                if ((eph = eventProviderHelpers.get(epId)) != null
                        && (m = eph.disposeMethod) != null) {
                    m.invoke(runtime.eps.get(epId));
                }
            }
            for (String moId : managedObjectIds) {
                ManagedObjectHelper moh;
                if ((moh = managedObjectHelpers.get(moId)) != null
                        && (m = moh.disposeMethod) != null) {
                    m.invoke(runtime.mos.get(moId));
                }
            }
        } catch (IllegalAccessException e) {
            Logger.log(TraceLevel.Error, StateMachine.class.getSimpleName(),
                    e.getMessage());
        } catch (IllegalArgumentException e) {
            Logger.log(TraceLevel.Error, StateMachine.class.getSimpleName(),
                    e.getMessage());
        } catch (InvocationTargetException e) {
            Logger.log(TraceLevel.Error, StateMachine.class.getSimpleName(),
                    e.getMessage());
        }
    }

    private void initAssociatedObjects(StateMachineRuntime runtime) {
        Method m;
        try {
            for (String epId : eventProviderIds) {
                EventProviderHelper eph;
                if ((eph = eventProviderHelpers.get(epId)) != null
                        && (m = eph.initMethod) != null) {
                    m.invoke(runtime.eps.get(epId));
                }
            }
            for (String moId : managedObjectIds) {
                ManagedObjectHelper moh;
                if ((moh = managedObjectHelpers.get(moId)) != null
                        && (m = moh.initMethod) != null) {
                    m.invoke(runtime.mos.get(moId));
                }
            }
        } catch (IllegalAccessException e) {
            Logger.log(TraceLevel.Error, StateMachine.class.getSimpleName(),
                    e.getMessage());
        } catch (IllegalArgumentException e) {
            Logger.log(TraceLevel.Error, StateMachine.class.getSimpleName(),
                    e.getMessage());
        } catch (InvocationTargetException e) {
            Logger.log(TraceLevel.Error, StateMachine.class.getSimpleName(),
                    e.getMessage());
        }
    }

    protected void start(StateMachineRuntime runtime) {
        if (top.sub == null) {
            return;
        }

        initAssociatedObjects(runtime);

        State originState = currentState = top;
        while (originState != null && runtime.isEventsExisting()) {
            originState = State.switchState(originState, runtime);
            if (originState == top) {
                break;
            }
            if (originState != null) {
                currentState = originState;
            }
        }

        disposeAssociatedObjects(runtime);
    }

    public StateMachineRuntime createStateMachineRuntime() {
        return new StateMachineRuntime(this);
    }

    public String getCurrentState() {
        return currentState.identifier;
    }

    public void acceptEvent(StateMachineRuntime runtime, Event[] evts) {
        int length = evts.length;
        String[] evtIds = new String[length];
        String id;
        for (int i = 0; i < length; i++) {
            id = evts[i].identifier;
            if (events.contains(id)) {
                evtIds[i] = id;
            } else {
                Logger.log(TraceLevel.Error,
                        StateMachine.class.getSimpleName(),
                        "Unrecognized event \"" + id + "\" in state machine");
                return;
            }
        }

        runtime.addEvent(evtIds);
    }

    public void createIncomingAssociation(String identifier,
            Class<? extends EventProvider> provider) {
        if (!initializedFlag) {
            _initializeEventProviderHelper(this, identifier, provider);
        }
    }

    public void createOutgoingAssociation(String identifier,
            Class<? extends ManagedObject> managedObj) {
        if (!initializedFlag) {
            _initializeManagedObjectHelper(this, identifier, managedObj);
        }
    }

    public boolean isInitialized() {
        return initializedFlag;
    }

    public State getTopState() {
        return this.top;
    }

    public State getState(String identifier) {
        return states.get(identifier);
    }

    public State createState(String identifier, StateType type) {
        if (type != null) {
            if (states.containsKey(identifier)) {
                Logger.log(TraceLevel.Error,
                        StateMachine.class.getSimpleName(),
                        "State with name \"" + identifier
                                + "\" already existing");
                return null;
            }
            State state = new State(identifier, type);
            state.sm = this;
            states.put(identifier, state);
            return state;
        }

        return null;
    }

    public Action createAction(String actionExpr) {
        Action action = null;
        if (!initializedFlag) {
            action = new Action(actionExpr);
            action.sm = this;
        }

        return action;
    }

    public Guard createGuard(String evtExpr, String lgcExpr) {
        Guard guard = null;
        if (!initializedFlag) {
            LogicExpression expr = null;
            if (lgcExpr != null && lgcExpr.length() > 0) {
                expr = new LogicExpression(lgcExpr);
                expr.sm = this;
            }
            guard = new Guard(evtExpr, expr);
            guard.sm = this;
        }

        return guard;
    }

    public void createTransition(State fromState, State toState, Guard guard,
            Action action) {
        if (!initializedFlag) {
            fromState.sm = toState.sm = this;
            State.addTransition(fromState, guard, action, toState);
        }
    }

    public String getManagedObjectIdentifier(Class<? extends ManagedObject> clz) {
        return managedObjectIdMap.get(clz);
    }

    public String getEventProviderIdentifier(Class<? extends EventProvider> clz) {
        return eventProviderIdMap.get(clz);
    }

    public ManagedObjectHelper getManagedObjectHelper(String identifier) {
        return managedObjectHelpers.get(identifier);
    }

    public EventProviderHelper getEventProviderHelper(String identifier) {
        return eventProviderHelpers.get(identifier);
    }

    public void register() {
        initializedFlag = true;
        Segine.unlockStateMachine(identifier);
    }

    private static void _initializeEventProviderHelper(StateMachine sm,
            String identifier, Class<? extends EventProvider> ep) {
        if (sm.eventProviderIds.contains(identifier)) {
            Logger.log(TraceLevel.Error, StateMachine.class.getSimpleName(),
                    "Duplicate identifier \"" + identifier
                            + "\" in same state machine");
        } else if (sm.eventProviderIdMap.containsKey(ep)) {
            Logger.log(TraceLevel.Error, StateMachine.class.getSimpleName(),
                    "Event provider \"" + ep.getSimpleName()
                            + "\" is already exists in state machine");
        } else {
            if (sm.eventProviderHelpers.containsKey(identifier)) {
                Logger.log(TraceLevel.Error,
                        StateMachine.class.getSimpleName(),
                        "Duplicate event provider identifier \"" + identifier
                                + "\"");
            } else {
                sm.eventProviderIds.add(identifier);
                sm.eventProviderIdMap.put(ep, identifier);
                sm.eventProviderHelpers.put(identifier,
                        EventProviderHelper.NewInstance(sm, ep));
            }
        }
    }

    private static void _initializeManagedObjectHelper(StateMachine sm,
            String identifier, Class<? extends ManagedObject> mo) {
        if (sm.managedObjectIds.contains(identifier)) {
            Logger.log(TraceLevel.Error, StateMachine.class.getSimpleName(),
                    "Duplicate identifier \"" + identifier
                            + "\" in same state machine");
        } else if (sm.managedObjectIdMap.containsKey(mo)) {
            Logger.log(TraceLevel.Error, StateMachine.class.getSimpleName(),
                    "Managed object \"" + mo.getSimpleName()
                            + "\" is already exists in state machine");
        } else {
            if (sm.managedObjectHelpers.containsKey(identifier)) {
                Logger.log(TraceLevel.Error,
                        StateMachine.class.getSimpleName(),
                        "Duplicate managed object identifier \"" + identifier
                                + "\"");
            } else {
                sm.managedObjectIds.add(identifier);
                sm.managedObjectIdMap.put(mo, identifier);
                sm.managedObjectHelpers.put(identifier,
                        ManagedObjectHelper.NewInstance(mo));
            }
        }
    }
}