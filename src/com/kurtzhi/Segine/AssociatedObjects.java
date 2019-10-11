package com.kurtzhi.Segine;

abstract class AssociatedObjects {
    String identifier;
    StateMachineRuntime smrt;

    abstract public void init();

    abstract public void dispose();
}
