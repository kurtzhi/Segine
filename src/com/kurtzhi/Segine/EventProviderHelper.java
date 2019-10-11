package com.kurtzhi.Segine;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import com.kurtzhi.Segine.Logger.TraceLevel;

class EventProviderHelper {
    private ArrayList<String> events = new ArrayList<String>();
    protected Method initMethod;
    protected Method disposeMethod;

    private EventProviderHelper() {
    }

    protected static EventProviderHelper NewInstance(StateMachine sa,
            Class<? extends EventProvider> eventProviderClass) {

        EventProviderHelper eph = new EventProviderHelper();

        Field[] Fields = eventProviderClass.getDeclaredFields();
        Class<?> eventClass = Event.class;
        Event event = null;
        for (Field field : Fields) {
            if (field.getType() == eventClass
                    && field.getModifiers() == (Modifier.PUBLIC
                            | Modifier.STATIC | Modifier.FINAL)) {
                try {
                    event = (Event) field.get(null);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    Logger.log(TraceLevel.Error,
                            EventProviderHelper.class.getSimpleName(),
                            e.getMessage());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    Logger.log(TraceLevel.Error,
                            EventProviderHelper.class.getSimpleName(),
                            e.getMessage());
                }
                if (event != null) {
                    if (sa.registerEvent(event.identifier)) {
                        eph.events.add(event.identifier);
                    } else {
                        Logger.log(TraceLevel.Error,
                                EventProviderHelper.class.getSimpleName(),
                                "Duplicate event \"" + event.identifier
                                        + "\" in same state machine");
                    }
                }
            }
        }

        Method[] methods = eventProviderClass.getDeclaredMethods();
        String returnType = null;
        String name = null;
        for (Method method : methods) {
            if (method.getParameterTypes().length == 0
                    && method.getModifiers() == Modifier.PUBLIC) {
                returnType = method.getReturnType().getSimpleName();
                name = method.getName();
                if (returnType == "void") {
                    if (name.equals("init")) {
                        eph.initMethod = method;
                    } else if (name.equals("dispose")) {
                        eph.disposeMethod = method;
                    }
                }
            }
        }

        return eph;
    }
}
