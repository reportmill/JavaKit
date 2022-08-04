package javakit.reflect;
import javakit.reflect.JavaField.FieldBuilder;
import javakit.reflect.JavaMethod.MethodBuilder;
import java.io.PrintStream;

/**
 * Provide reflection info for TeaVM.
 */
public class StaticResolver {

    /**
     * Returns the declared fields for given class.
     */
    public static JavaField[] getFieldsForClass(Resolver aResolver, String aClassName)
    {
        switch (aClassName) {
            case "java.lang.System": {
                FieldBuilder fb = new FieldBuilder(aResolver, aClassName);
                JavaField[] fields = new JavaField[2];
                fields[0] = fb.name("out").type(PrintStream.class).build();
                fields[1] = fb.name("err").type(PrintStream.class).build();
                return fields;
            }
            default: return new JavaField[0];
        }
    }

    /**
     * Returns the declared methods for given class.
     */
    public static JavaMethod[] getMethodsForClass(Resolver aResolver, String aClassName)
    {
        JavaMethod.MethodBuilder mb = new JavaMethod.MethodBuilder(aResolver, aClassName);
        System.out.println("Get methods for " + aClassName);

        switch (aClassName) {
            case "java.lang.Object": return getMethodsForJavaLangObject(aResolver, mb);
            case "java.lang.Class": return getMethodsForJavaLangClass(aResolver, mb);
            case "java.lang.String": return getMethodsForJavaLangString(aResolver, mb);
            case "java.io.PrintStream": return getMethodsForJavaIoPrintStream(aResolver, mb);
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
     * Returns methods for java.lang.Object.
     */
    public static JavaMethod[] getMethodsForJavaLangObject(Resolver aResolver, MethodBuilder mb)
    {
        JavaMethod[] methods = new JavaMethod[2];
        methods[0] = mb.name("getClass").returnType(Class.class).build();
        return methods;
    }

    /**
     * Returns methods for java.lang.Class.
     */
    public static JavaMethod[] getMethodsForJavaLangClass(Resolver aResolver, MethodBuilder mb)
    {
        JavaMethod[] methods = new JavaMethod[2];
        methods[0] = mb.name("getName").returnType(String.class).build();
        methods[1] = mb.name("getSimpleName").returnType(String.class).build();
        return methods;
    }

    /**
     * Returns methods for java.lang.String.
     */
    public static JavaMethod[] getMethodsForJavaLangString(Resolver aResolver, MethodBuilder mb)
    {
        JavaMethod[] methods = new JavaMethod[2];
        methods[0] = mb.name("length").returnType(int.class).build();
        methods[1] = mb.name("replace").paramTypes(String.class,String.class).returnType(String.class).build();
        return methods;
    }

    /**
     * Returns methods for java.io.PrintStream.
     */
    public static JavaMethod[] getMethodsForJavaIoPrintStream(Resolver aResolver, MethodBuilder mb)
    {
        JavaMethod[] methods = new JavaMethod[2];
        methods[0] = mb.name("print").paramTypes(Object.class).build();
        methods[1] = mb.name("println").paramTypes(Object.class).build();
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
        System.out.println("Invoke: " + anId);
        switch (anId) {
            case "java.lang.String.length()": return ((String) anObj).length();
            case "java.lang.String.replace(java.lang.String,java.lang.String)":
                return ((java.lang.String) anObj).replace((String) theArgs[0],(String) theArgs[1]);
            case "java.io.PrintStream.print(java.lang.Object)":
                ((PrintStream) anObj).print(theArgs[0]); return theArgs[0];
            case "java.io.PrintStream.println(java.lang.Object)":
                ((PrintStream) anObj).print(theArgs[0]); return theArgs[0];
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
