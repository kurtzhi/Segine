package com.kurtzhi.Segine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.kurtzhi.Segine.Logger.TraceLevel;

public class StateMachineRuntime {
    protected ArrayList<String> associateObjectIdentifiers = new ArrayList<String>();
    protected Map<String, EventProvider> eps = new HashMap<String, EventProvider>();
    protected Map<String, ManagedObject> mos = new HashMap<String, ManagedObject>();
    protected StateMachine sm;
    protected boolean isSaStarted = false;
    private ArrayList<String> events = new ArrayList<String>();
    private boolean evtsExistingFlag = false;

    public ManagedObject getManagedObject(String identifier) {
        return mos.get(identifier);
    }

    public void linkAssociatedInstance(Object obj) {
        Class<?> clz = obj.getClass();
        if (obj instanceof EventProvider) {
            @SuppressWarnings("unchecked")
            String id = sm
                    .getEventProviderIdentifier((Class<? extends EventProvider>) clz);
            EventProvider evtProvider = (EventProvider) obj;
            addEventProvider(id, evtProvider);
            evtProvider.smrt = this;
        } else if (obj instanceof ManagedObject) {
            @SuppressWarnings("unchecked")
            String id = sm
                    .getManagedObjectIdentifier((Class<? extends ManagedObject>) clz);
            ManagedObject mngObject = (ManagedObject) obj;
            addManagedObject(id, mngObject);
            mngObject.smrt = this;
        } else {
            Logger.log(
                    TraceLevel.Error,
                    StateMachineRuntime.class.getSimpleName(),
                    "Unrecognized associated instance type \""
                            + clz.getSimpleName() + "\" in state machine");
        }
    }

    protected StateMachineRuntime(StateMachine sm) {
        this.sm = sm;
    }

    protected void addEvent(String[] evts) {
        events.addAll(Arrays.asList(evts));
        evtsExistingFlag = true;
    }

    protected boolean isEventsExisting() {
        return evtsExistingFlag;
    }

    protected ArrayList<String> popEvents() {
        ArrayList<String> ret = new ArrayList<String>();
        ret.addAll(events);
        events = new ArrayList<String>();
        evtsExistingFlag = false;
        return ret;
    }

    protected void addEventProvider(String identifier, EventProvider ep) {
        if (associateObjectIdentifiers.contains(identifier)) {
            Logger.log(TraceLevel.Error,
                    StateMachineRuntime.class.getSimpleName(),
                    "Duplicate identifier \"" + identifier
                            + "\" in same state machine");
        } else {
            if (eps.containsKey(identifier)) {
                Logger.log(TraceLevel.Error,
                        StateMachineRuntime.class.getSimpleName(),
                        "Duplicate event provider identifier \"" + identifier
                                + "\"");
            } else {
                associateObjectIdentifiers.add(identifier);
                eps.put(identifier, ep);
            }
        }
    }

    protected void addManagedObject(String identifier, ManagedObject mo) {
        if (associateObjectIdentifiers.contains(identifier)) {
            Logger.log(TraceLevel.Error,
                    StateMachineRuntime.class.getSimpleName(),
                    "Duplicate identifier \"" + identifier
                            + "\" in same state machine");
        } else {
            if (mos.containsKey(identifier)) {
                Logger.log(TraceLevel.Error,
                        StateMachineRuntime.class.getSimpleName(),
                        "Duplicate managed object identifier \"" + identifier
                                + "\"");
            } else {
                associateObjectIdentifiers.add(identifier);
                mos.put(identifier, mo);
            }
        }
    }
}
