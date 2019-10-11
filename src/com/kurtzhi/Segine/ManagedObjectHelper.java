package com.kurtzhi.Segine;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

class ManagedObjectHelper {
    private Map<String, Method> noArgBoolMethods = new HashMap<String, Method>();
    private Map<String, Method> noArgIntMethods = new HashMap<String, Method>();
    private Map<String, Method> noArgVoidMethods = new HashMap<String, Method>();
    protected Method initMethod;
    protected Method disposeMethod;

    public Method getMethod(MethodType type, String name) {
        switch (type) {
        case BOOL:
            return noArgBoolMethods.get(name);

        case INT:
            return noArgIntMethods.get(name);

        case VOID:
            return noArgVoidMethods.get(name);

        default:
            return null;
        }
    }

    public static enum MethodType {
        BOOL, INT, VOID
    }

    private ManagedObjectHelper() {
    }

    protected static ManagedObjectHelper NewInstance(
            Class<? extends ManagedObject> managedObjectClass) {
        ManagedObjectHelper moh = new ManagedObjectHelper();
        Method[] methods = managedObjectClass.getDeclaredMethods();
        String returnType = null;
        String name = null;
        for (Method method : methods) {
            if (method.getParameterTypes().length == 0
                    && method.getModifiers() == Modifier.PUBLIC) {
                returnType = method.getReturnType().getSimpleName();
                name = method.getName();
                if (returnType == "boolean") {
                    moh.noArgBoolMethods.put(name, method);
                } else if (returnType == "int") {
                    moh.noArgIntMethods.put(name, method);
                } else if (returnType == "void") {
                    if (name.equals("init")) {
                        moh.initMethod = method;
                    } else if (name.equals("dispose")) {
                        moh.disposeMethod = method;
                    } else {
                        moh.noArgVoidMethods.put(name, method);
                    }
                }
            }
        }

        return moh;
    }
}
