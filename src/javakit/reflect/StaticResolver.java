/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import javakit.reflect.JavaField.FieldBuilder;
import javakit.reflect.JavaMethod.MethodBuilder;
import javakit.reflect.JavaConstructor.ConstructorBuilder;
import snap.util.SnapUtils;
import java.io.PrintStream;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.DoubleStream;

/**
 * Provide reflection info for TeaVM.
 */
public class StaticResolver {

    // A shared field builder
    private static FieldBuilder fb = new FieldBuilder();

    // A shared method builder
    private static MethodBuilder mb = new MethodBuilder();

    // A shared constructor builder
    private static ConstructorBuilder cb = new ConstructorBuilder();

    /**
     * Returns the declared fields for given class.
     */
    public static JavaField[] getFieldsForClass(Resolver aResolver, String aClassName)
    {
        fb.init(aResolver, aClassName);

        switch (aClassName) {

            // Handle java.lang.System
            case "java.lang.System":
                fb.name("out").type(PrintStream.class).save();
                return fb.name("err").type(PrintStream.class).buildAll();

            // Handle anything else
            default: return new JavaField[0];
        }
    }

    /**
     * Returns the declared methods for given class.
     */
    public static JavaMethod[] getMethodsForClass(Resolver aResolver, String aClassName)
    {
        mb.init(aResolver, aClassName);

        switch (aClassName) {

            // Handle java.lang.Object
            case "java.lang.Object":
                return mb.name("getClass").returnType(Class.class).buildAll();

            // Handle java.lang.Class
            case "java.lang.Class":
                mb.name("getName").returnType(String.class).save();
                return mb.name("getSimpleName").returnType(String.class).buildAll();

            // Handle java.lang.String
            case "java.lang.String":
                mb.name("length").returnType(int.class).save();
                return mb.name("replace").paramTypes(String.class,String.class).returnType(String.class).buildAll();

            // Handle java.io.PrintStream
            case "java.io.PrintStream":
                mb.name("print").paramTypes(Object.class).save();
                return mb.name("println").paramTypes(Object.class).buildAll();

            // Handle java.util.stream.DoubleStream
            case "java.util.stream.DoubleStream":
                mb.name("of").paramTypes(double[].class).returnType(DoubleStream.class).save();
                mb.name("map").paramTypes(DoubleUnaryOperator.class).returnType(DoubleStream.class).save();
                return mb.name("toArray").returnType(double[].class).buildAll();

            // Handle snap.view.Button
            case "snap.view.Button":
                mb.name("setPrefSize").paramTypes(double.class,double.class).save();
                return mb.name("setTitle").paramTypes(String.class).buildAll();

            // Handle snap.view.View
            case "snap.view.View":
                return mb.name("getAnim").paramTypes(int.class).returnType(snap.view.ViewAnim.class).buildAll();

            // Handle snap.view.ViewAnim
            case "snap.view.ViewAnim":
                mb.name("setRotate").paramTypes(double.class).returnType(snap.view.ViewAnim.class).save();
                mb.name("setScaleX").paramTypes(double.class).returnType(snap.view.ViewAnim.class).save();
                mb.name("setScaleY").paramTypes(double.class).returnType(snap.view.ViewAnim.class).save();
                return mb.name("play").buildAll();

            // Handle snap.view.ViewOwner
            case "snap.view.ViewOwner":
                return mb.name("setWindowVisible").paramTypes(boolean.class).buildAll();

            // Handle anything else
            default: return new JavaMethod[0];
        }
    }

    /**
     * Returns the declared methods for given class.
     */
    public static JavaConstructor[] getConstructorsForClass(Resolver aResolver, String aClassName)
    {
        cb.init(aResolver, aClassName);

        switch (aClassName) {

            // Handle java.lang.String
            case "java.lang.String":
                return cb.save().paramTypes(String.class).buildAll();

            // Handle snap.view.Button
            case "snap.view.Button":
                return cb.save().paramTypes(String.class).buildAll();

            // Handle ViewOwner
            case "snap.view.ViewOwner":
                return cb.save().paramTypes(snap.view.View.class).buildAll();

            // Handle anything else
            default: return cb.save().buildAll();
        }
    }

    /**
     * Invokes methods for Java.lang.String.
     */
    public static Object invokeMethod(Object anObj, String anId, Object ... theArgs) throws Exception
    {
        switch (anId) {

            // Handle java.lang.String
            case "java.lang.String.length()": return ((String) anObj).length();
            case "java.lang.String.replace(java.lang.String,java.lang.String)":
                return ((java.lang.String) anObj).replace((String) theArgs[0],(String) theArgs[1]);

            // Handle java.io.PrintStream
            case "java.io.PrintStream.print(java.lang.Object)":
                ((PrintStream) anObj).print(theArgs[0]); return theArgs[0];
            case "java.io.PrintStream.println(java.lang.Object)":
                ((PrintStream) anObj).println(theArgs[0]); return theArgs[0];

            // Handle java.util.stream.DoubleStream
            case "java.util.stream.DoubleStream.of(double[])":
                return DoubleStream.of((double[]) theArgs[0]);
            case "java.util.stream.DoubleStream.map(java.util.function.DoubleUnaryOperator)":
                return ((DoubleStream) anObj).map((DoubleUnaryOperator) theArgs[0]);
            case "java.util.stream.DoubleStream.toArray()":
                return ((DoubleStream) anObj).toArray();

            // Handle snap.view.Button
            case "snap.view.Button.setPrefSize(double,double)":
                ((snap.view.Button) anObj).setPrefSize(doubleVal(theArgs[0]),doubleVal(theArgs[1])); return null;

            // Handle snap.view.View
            case "snap.view.View.getAnim(int)":
                return ((snap.view.View) anObj).getAnim(intVal(theArgs[0]));

            // Handle snap.view.ViewAnim
            case "snap.view.ViewAnim.setRotate(double)":
                return ((snap.view.ViewAnim) anObj).setRotate(doubleVal(theArgs[0]));
            case "snap.view.ViewAnim.setScaleX(double)":
                return ((snap.view.ViewAnim) anObj).setScaleX(doubleVal(theArgs[0]));
            case "snap.view.ViewAnim.setScaleY(double)":
                return ((snap.view.ViewAnim) anObj).setScaleY(doubleVal(theArgs[0]));
            case "snap.view.ViewAnim.play()":
                ((snap.view.ViewAnim) anObj).play(); return null;

            // Handle snap.view.ViewOwner
            case "snap.view.ViewOwner.setWindowVisible(boolean)":
                ((snap.view.ViewOwner) anObj).setWindowVisible(boolVal(theArgs[0])); return null;

            // Handle anything else
            default: throw new NoSuchMethodException("Unknown method: " + anId);
        }
    }

    /**
     * Invokes constructor for Java.lang.String.
     */
    public static Object invokeConstructor(String anId, Object ... theArgs) throws Exception
    {
        switch (anId) {

            // Handle java.lang.String
            case "java.lang.String(java.lang.String)":
                return theArgs[0];

            // Handle snap.view.Button(java.lang.String)
            case "snap.view.Button(java.lang.String)":
                return new snap.view.Button((String) theArgs[0]);

            // Handle snap.view.ViewOwner
            case "snap.view.ViewOwner(snap.view.View)":
                return new snap.view.ViewOwner((snap.view.View) theArgs[0]);

            // Handle anything else
            default: throw new NoSuchMethodException("Unknown constructor: " + anId);
        }
    }

    // Conveniences
    private static boolean boolVal(Object anObj)  { return SnapUtils.boolValue(anObj); }
    private static int intVal(Object anObj)  { return SnapUtils.intValue(anObj); }
    private static double doubleVal(Object anObj)  { return SnapUtils.doubleValue(anObj); }
}
