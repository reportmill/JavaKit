package javakit.reflect;
import javakit.reflect.JavaField.FieldBuilder;
import javakit.reflect.JavaMethod.MethodBuilder;
import javakit.reflect.JavaConstructor.ConstructorBuilder;
import java.io.PrintStream;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.DoubleStream;

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

        switch (aClassName) {
            case "java.lang.Object": return getMethodsForJavaLangObject(mb);
            case "java.lang.Class": return getMethodsForJavaLangClass(mb);
            case "java.lang.String": return getMethodsForJavaLangString(mb);
            case "java.io.PrintStream": return getMethodsForJavaIoPrintStream(mb);
            case "java.util.stream.DoubleStream": return getMethodsForJavaUtilStreamDoubleStream(mb);
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
                ConstructorBuilder cb = new ConstructorBuilder(aResolver, aClassName);
                return new JavaConstructor[] { cb.build() };
        }
    }

    /**
     * Returns methods for java.lang.Object.
     */
    public static JavaMethod[] getMethodsForJavaLangObject(MethodBuilder mb)
    {
        JavaMethod[] methods = new JavaMethod[1];
        methods[0] = mb.name("getClass").returnType(Class.class).build();
        return methods;
    }

    /**
     * Returns methods for java.lang.Class.
     */
    public static JavaMethod[] getMethodsForJavaLangClass(MethodBuilder mb)
    {
        JavaMethod[] methods = new JavaMethod[2];
        methods[0] = mb.name("getName").returnType(String.class).build();
        methods[1] = mb.name("getSimpleName").returnType(String.class).build();
        return methods;
    }

    /**
     * Returns methods for java.lang.String.
     */
    public static JavaMethod[] getMethodsForJavaLangString(MethodBuilder mb)
    {
        JavaMethod[] methods = new JavaMethod[2];
        methods[0] = mb.name("length").returnType(int.class).build();
        methods[1] = mb.name("replace").paramTypes(String.class,String.class).returnType(String.class).build();
        return methods;
    }

    /**
     * Returns methods for java.io.PrintStream.
     */
    public static JavaMethod[] getMethodsForJavaIoPrintStream(MethodBuilder mb)
    {
        JavaMethod[] methods = new JavaMethod[2];
        methods[0] = mb.name("print").paramTypes(Object.class).build();
        methods[1] = mb.name("println").paramTypes(Object.class).build();
        return methods;
    }

    /**
     * Returns methods for java.util.stream.DoubleStream.
     */
    public static JavaMethod[] getMethodsForJavaUtilStreamDoubleStream(MethodBuilder mb)
    {
        JavaMethod[] methods = new JavaMethod[3];
        methods[0] = mb.name("of").paramTypes(double[].class).returnType(DoubleStream.class).build();
        methods[1] = mb.name("map").paramTypes(DoubleUnaryOperator.class).returnType(DoubleStream.class).build();
        methods[2] = mb.name("toArray").returnType(double[].class).build();
        return methods;
    }

    /**
     * Returns constructors for java.lang.String.
     */
    public static JavaConstructor[] getConstructorsForJavaLangString(Resolver aResolver)
    {
        ConstructorBuilder cb = new ConstructorBuilder(aResolver, "java.lang.String");
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
            case "java.lang.String.length()": return ((String) anObj).length();
            case "java.lang.String.replace(java.lang.String,java.lang.String)":
                return ((java.lang.String) anObj).replace((String) theArgs[0],(String) theArgs[1]);
            case "java.io.PrintStream.print(java.lang.Object)":
                ((PrintStream) anObj).print(theArgs[0]); return theArgs[0];
            case "java.util.stream.DoubleStream.of(double[])":
                return DoubleStream.of((double[]) theArgs[0]);
            case "java.util.stream.DoubleStream.map(java.util.function.DoubleUnaryOperator)":
                return ((DoubleStream) anObj).map((DoubleUnaryOperator) theArgs[0]);
            case "java.util.stream.DoubleStream.toArray()":
                return ((DoubleStream) anObj).toArray();
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
