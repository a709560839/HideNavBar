package com.jj.hidenavbar.xposed;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectUtils {

    public static Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }

    public static Object getObjectField(Object obj, String fieldName) throws Exception {
        return getField(obj.getClass(), fieldName).get(obj);
    }

    public static int getIntField(Object obj, String fieldName) throws Exception {
        return getField(obj.getClass(), fieldName).getInt(obj);
    }

    public static boolean getBooleanField(Object obj, String fieldName) throws Exception {
        return getField(obj.getClass(), fieldName).getBoolean(obj);
    }

    public static void setBooleanField(Object obj, String fieldName, boolean value) throws Exception {
        getField(obj.getClass(), fieldName).setBoolean(obj, value);
    }

    public static int getStaticIntField(Class<?> clazz, String fieldName) throws Exception {
        return getField(clazz, fieldName).getInt(null);
    }

    public static Object callMethod(Object obj, String methodName, Object... args) throws Exception {
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i].getClass(); // Simplistic parameter type matching
        }
        Method method = obj.getClass().getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(obj, args);
    }

    // Explicit call for no-arg method
    public static Object callMethodNoArgs(Object obj, String methodName) throws Exception {
        Method method = obj.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(obj);
    }
}
