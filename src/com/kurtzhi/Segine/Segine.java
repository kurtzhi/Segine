package com.kurtzhi.Segine;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import com.kurtzhi.Segine.Logger.TraceLevel;

public class Segine {
    private static Map<String, StateMachine> sms = new HashMap<String, StateMachine>();
    private static Map<String, Semaphore> smLocks = new HashMap<String, Semaphore>();

    public static synchronized StateMachine createStateMachine(String identifier) {
        if (sms.containsKey(identifier)) {
            Logger.log(TraceLevel.Error, Segine.class.getSimpleName(),
                    "Duplicate state machine identifier \"" + identifier + "\"");
            return null;
        }

        Semaphore lock = new Semaphore(1);
        smLocks.put(identifier, lock);
        lockStateMachine(identifier);

        StateMachine sm = new StateMachine(identifier);
        sms.put(identifier, sm);

        return sm;
    }

    public static StateMachine lookupStateMachine(String identifier) {
        lockStateMachine(identifier);
        StateMachine sm = sms.get(identifier);
        unlockStateMachine(identifier);
        return sm;
    }

    protected static void unlockStateMachine(String identifier) {
        Semaphore lock;
        if ((lock = smLocks.get(identifier)) != null) {
            lock.release();
        }
    }

    private static void lockStateMachine(String identifier) {
        Semaphore lock;
        if ((lock = smLocks.get(identifier)) != null) {
            try {
                lock.acquire();
            } catch (InterruptedException e) {
                Logger.log(TraceLevel.Error, Segine.class.getSimpleName(),
                        e.getMessage());
            }
        }
    }
}