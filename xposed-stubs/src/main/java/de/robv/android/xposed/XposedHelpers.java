package de.robv.android.xposed;
public final class XposedHelpers {
    public static Class<?> findClass(String className, ClassLoader classLoader) { throw new RuntimeException("Stub"); }
    public static XC_MethodHook.Unhook findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) { throw new RuntimeException("Stub"); }
    public static XC_MethodHook.Unhook findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) { throw new RuntimeException("Stub"); }
    public static void setStaticObjectField(Class<?> clazz, String fieldName, Object value) { throw new RuntimeException("Stub"); }
    public static void setObjectField(Object obj, String fieldName, Object value) { throw new RuntimeException("Stub"); }
}
