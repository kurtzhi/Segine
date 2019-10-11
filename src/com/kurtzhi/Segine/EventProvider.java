package com.kurtzhi.Segine;

abstract public class EventProvider extends AssociatedObjects {
    final public void trig(Event[] events) {
        if (smrt != null && events != null && events.length > 0) {
            smrt.sm.acceptEvent(smrt, events);
            if (!smrt.isSaStarted) {
                new Thread(new Runnable() {
                    public void run() {
                        smrt.isSaStarted = true;
                        smrt.sm.start(smrt);
                    }
                }).start();
            }
        }
    }
}