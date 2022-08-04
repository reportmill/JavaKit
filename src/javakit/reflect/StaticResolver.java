package javakit.reflect;

/**
 * Provide reflection info for TeaVM.
 */
public class StaticResolver {

    /**
     * Returns the declared methods for given class.
     */
    public static JavaMethod[] getMethodsForClass(Resolver aResolver, String aClassName)
    {
        switch (aClassName) {
            case "java.lang.String": return getMethodsForJavaLangString(aResolver);
            default: return new JavaMethod[0];
        }
    }

    /**
     * Returns the declared methods for given class.
     */
    public static JavaConstructor[] getConstructorsForClass(Resolver aResolver, String aClassName)
    {
        switch (aClassName) {
            case "java.lang.String": return getConstructorsForJavaLangString(aResolver);
            default:
                JavaConstructor.ConstructorBuilder cb = new JavaConstructor.ConstructorBuilder(aResolver, aClassName);
                return new JavaConstructor[] { cb.build() };
        }
    }

    /**
     * Returns methods for java.lang.String.
     */
    public static JavaMethod[] getMethodsForJavaLangString(Resolver aResolver)
    {
        JavaMethod.MethodBuilder mb = new JavaMethod.MethodBuilder(aResolver, "java.lang.String");
        JavaMethod[] methods = new JavaMethod[2];

        methods[0] = mb.name("length").returnType(int.class).build();
        methods[1] = mb.name("replace").paramTypes(String.class,String.class).returnType(String.class).build();
        return methods;
    }

    /**
     * Returns constructors for java.lang.String.
     */
    public static JavaConstructor[] getConstructorsForJavaLangString(Resolver aResolver)
    {
        JavaConstructor.ConstructorBuilder cb = new JavaConstructor.ConstructorBuilder(aResolver, "java.lang.String");
        JavaConstructor[] constructors = new JavaConstructor[2];

        constructors[0] = cb.build();
        constructors[1] = cb.paramTypes(String.class).build();
        return constructors;
    }

    /**
     * Invokes methods for Java.lang.String.
     */
    public static Object invokeMethod(Object anObj, String anId, Object ... theArgs) throws Exception
    {
        switch (anId) {
            case "java.lang.String.length()": return ((java.lang.String) anObj).length();
            case "java.lang.String.replace(java.lang.String,java.lang.String)":
                return ((java.lang.String) anObj).replace((java.lang.String)theArgs[0],(java.lang.String)theArgs[1]);
            default: throw new NoSuchMethodException("Unknown method: " + anId);
        }
    }

    /**
     * Invokes constructor for Java.lang.String.
     */
    public static Object invokeConstructor(Class<?> aClass, String anId, Object ... theArgs) throws Exception
    {
        switch (anId) {
            case "java.lang.String()": return new String();
            case "java.lang.String(java.lang.String)": return theArgs[0];
            default: throw new NoSuchMethodException("Unknown constructor: " + anId);
        }
    }
}
