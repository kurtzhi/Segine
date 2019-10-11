package com.kurtzhi.test;

import com.kurtzhi.Segine.Event;
import com.kurtzhi.Segine.EventProvider;

public class Notification extends EventProvider {
    final public static Event Replenish = new Event("re");
    final public static Event Selling = new Event("se");

    @Override
    public void init() {
    }

    @Override
    public void dispose() {
    }
}
