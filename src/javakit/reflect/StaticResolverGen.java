package javakit.reflect;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Stream;

/**
 *
 */
public class StaticResolverGen {

    // StringBuffer
    private static StringBuffer _sb = new StringBuffer();

    private static Resolver  _resolver = new ResolverSys(snap.view.View.class.getClassLoader());

    /**
     * Prints the preamble.
     */
    public void printPreamble()
    {
        appendln("package javakit.reflect;");
        appendln("import javakit.reflect.JavaField.FieldBuilder;");
        appendln("import javakit.reflect.JavaMethod.MethodBuilder;");
        appendln("import javakit.reflect.JavaConstructor.ConstructorBuilder;");
        appendln("import snap.util.SnapUtils;");
        appendln("import java.io.PrintStream;");
        appendln("import java.util.function.DoubleUnaryOperator;");
        appendln("import java.util.stream.DoubleStream;");
        appendln("");
        appendln("/**");
        appendln(" * Provide reflection info for TeaVM.");
        appendln(" */");
        appendln("public class StaticResolver {");
        appendln("");
        appendln("    // Shared field, method, constructor builders");
        appendln("    private static FieldBuilder fb = new FieldBuilder();");
        appendln("    private static MethodBuilder mb = new MethodBuilder();");
        appendln("    private static ConstructorBuilder cb = new ConstructorBuilder();");
        appendln("");
    }

    /**
     * Prints the getFieldsForClass().
     */
    public void printGetFieldsForClass()
    {
        appendln("    /**");
        appendln("     * Returns the declared fields for given class.");
        appendln("     */");
        appendln("    public static JavaField[] getFieldsForClass(Resolver aResolver, String aClassName)");
        appendln("    {");
        appendln("        fb.init(aResolver, aClassName);");
        appendln("");
        appendln("        switch (aClassName) {");
        appendln("");
        appendln("            // Handle java.lang.System");
        appendln("            case \"java.lang.System\":");
        appendln("                fb.name(\"out\").type(PrintStream.class).save();");
        appendln("                return fb.name(\"err\").type(PrintStream.class).buildAll();");
        appendln("");
        appendln("            // Handle anything else");
        appendln("            default: return new JavaField[0];");
        appendln("        }");
        appendln("    }");
        appendln("");
    }

    /**
     * Prints getMethodsForClass() method.
     */
    public void printGetMethodsForClassForClasses(Class<?>[] theClasses)
    {
        appendln("/**");
        appendln(" * Returns the declared methods for given class.");
        appendln(" */");
        appendln("public static JavaMethod[] getMethodsForClass(Resolver aResolver, String aClassName)");
        appendln("{");
        appendln("    mb.init(aResolver, aClassName);");
        appendln("");
        appendln("    switch (aClassName) {");

        for (Class<?> cls : theClasses)
            printGetMethodsForClassForClass(cls);

        // Append trailer
        appendln("");
        appendln("        // Handle anything else");
        appendln("        default: return new JavaMethod[0];");
        appendln("    }");
        appendln("}");
    }

    /**
     * Prints getMethodsForClass() method.
     */
    public void printGetMethodsForClassForClass(Class aClass)
    {
        String className = aClass.getName();
        appendln("");
        append("        // Handle ").appendln(className);
        appendln("        case \"" + className + "\":");

        // Get methods
        Method[] methods = aClass.getDeclaredMethods();
        Stream<Method> methodsStream = Stream.of(methods);
        methodsStream = methodsStream.filter(m -> isValidMethod(m));
        methods = methodsStream.toArray(size -> new Method[size]);

        for (int i = 0, iMax = methods.length; i < iMax; i++) {
            Method method = methods[i];
            printGetMethodsForClassForClassMethod(method, i + 1 == iMax);
        }
    }

    boolean isValidMethod(Method m)
    {
        if (!Modifier.isPublic(m.getModifiers())) return false;
        if (!_whiteList.contains(m.getName())) return false;
        JavaClass javaClass = _resolver.getJavaClassForClass(m.getDeclaringClass());
        if (_blackList.contains(new JavaMethod(_resolver, javaClass, m).getId())) return false;
        return true;
    }

    /**
     * Prints getMethodsForClass() method.
     */
    public void printGetMethodsForClassForClassMethod(Method aMethod, boolean isLast)
    {
        // Preface
        append("            ");
        if (isLast)
            append("return ");

        // Method name
        String methodName = aMethod.getName();
        append("mb.name(\"" + methodName + "\")");

        // Parameters
        Class<?>[] paramTypes = aMethod.getParameterTypes();
        if (paramTypes.length > 0) {
            append(".paramTypes(");
            for (int i = 0, iMax = paramTypes.length; i < iMax; i++) {
                String className = className(paramTypes[i]);
                append(className).append(".class");
                if (i + 1 < iMax) append(",");
            }
            append(")");
        }

        // Return type
        Class<?> returnType = aMethod.getReturnType();
        if (returnType != null) {
            String className = className(returnType);
            append(".returnType(").append(className).append(".class").append(")");
        }

        // Append Save()/BuildAll()
        if (isLast)
            appendln(".buildAll();");
        else appendln(".save();");
    }

    /**
     * Prints getConstructorsForClass() method.
     */
    public void printGetConstructorsForClassForClasses(Class<?>[] theClasses)
    {
        appendln("/**");
        appendln(" * Returns the declared constructors for given class.");
        appendln(" */");
        appendln("public static JavaConstructor[] getConstructorsForClass(Resolver aResolver, String aClassName)");
        appendln("{");
        appendln("    cb.init(aResolver, aClassName);");
        appendln("");
        appendln("    switch (aClassName) {");

        for (Class<?> cls : theClasses)
            printGetConstructorsForClassForClass(cls);

        // Append trailer
        appendln("");
        appendln("        // Handle anything else");
        appendln("        default: return cb.save().buildAll();");
        appendln("    }");
        appendln("}");
    }

    /**
     * Prints getConstructorsForClass() method.
     */
    public void printGetConstructorsForClassForClass(Class aClass)
    {
        JavaClass javaClass = _resolver.getJavaClassForClass(aClass);
        JavaConstructor[] constructors = javaClass.getConstructors().toArray(new JavaConstructor[0]);
        Stream<JavaConstructor> constrStream = Stream.of(constructors);
        constrStream = constrStream.filter(c -> isValidConstructor(c));
        constructors = constrStream.toArray(size -> new JavaConstructor[size]);
        if (constructors.length == 0)
            return;

        String className = aClass.getName();
        appendln("");
        append("        // Handle ").appendln(className);
        appendln("        case \"" + className + "\":");

        for (int i = 0, iMax = constructors.length; i < iMax; i++) {
            Constructor constructor = constructors[i].getConstructor();
            if (!Modifier.isPublic(constructor.getModifiers())) continue;
            printGetConstructorsForClassForConstructor(constructor, i + 1 == iMax);
        }
    }

    boolean isValidConstructor(JavaConstructor c)
    {
        if (!Modifier.isPublic(c.getModifiers())) return false;
        if (c.getParamTypes().length == 0) return false;
        if (_blackList.contains(c.getId())) return false;
        return true;
    }

    /**
     * Prints getMethodsForClass() method.
     */
    public void printGetConstructorsForClassForConstructor(Constructor aConstructor, boolean isLast)
    {
        // Preface
        append("            ");
        if (isLast)
            append("return ");
        append("cb");

        // Parameters
        Class<?>[] paramTypes = aConstructor.getParameterTypes();
        if (paramTypes.length > 0) {
            append(".paramTypes(");
            for (int i = 0, iMax = paramTypes.length; i < iMax; i++) {
                String className = className(paramTypes[i]);
                append(className).append(".class");
                if (i + 1 < iMax) append(",");
            }
            append(")");
        }

        // Append Save()/BuildAll()
        if (isLast)
            appendln(".buildAll();");
        else appendln(".save();");
    }

    /**
     * Prints invokeMethod() method.
     */
    public void printInvokeMethodForClasses(Class<?>[] theClasses)
    {
        appendln("/**");
        appendln(" * Invokes methods for given method id, object and args.");
        appendln(" */");
        appendln("public static Object invokeMethod(String anId, Object anObj, Object ... theArgs) throws Exception");
        appendln("{");
        appendln("    switch (anId) {");

        for (Class<?> cls : theClasses)
            printInvokeMethodForClass(cls);

        // Append trailer
        appendln("");
        appendln("        // Handle anything else");
        appendln("        default: throw new NoSuchMethodException(\"Unknown method: \" + anId);");
        appendln("    }");
        appendln("}");
    }

    /**
     * Prints invokeMethod() method.
     */
    public void printInvokeMethodForClass(Class aClass)
    {
        String className = className(aClass);
        appendln("");
        append("        // Handle ").appendln(className);

        JavaClass javaClass = _resolver.getJavaClassForClass(aClass);
        JavaMethod[] methods = javaClass.getMethods().toArray(new JavaMethod[0]);
        //Method[] methods = aClass.getDeclaredMethods();

        for (int i = 0, iMax = methods.length; i < iMax; i++) {
            JavaMethod method = methods[i];
            if (!isValidMethod(method)) continue;
            if (method.getSuper() != null)
                continue;
            printInvokeMethodForClassMethod(method);
        }
    }

    boolean isValidMethod(JavaMethod m)
    {
        if (!Modifier.isPublic(m.getModifiers())) return false;
        if (!_whiteList.contains(m.getName())) return false;
        if (_blackList.contains(m.getId())) return false;
        return true;
    }

    /**
     * Prints invokeMethod() for method.
     */
    public void printInvokeMethodForClassMethod(JavaMethod aMethod)
    {
        appendln("        case \"" + aMethod.getId() + "\":");

        Method meth = aMethod.getMethod();
        Class<?> returnType = meth.getReturnType();

        // Preface
        append("            ");
        if (returnType != void.class)
            append("return ");

        // If static just do ClassName.
        String castClassName = meth.getDeclaringClass().getName();
        if (Modifier.isStatic(meth.getModifiers()))
            append(castClassName);

        // Else If instance method, add cast: "((pkg.pkg.ClassName) anObj).name("
        else append("((").append(castClassName).append(") anObj)");

        // Append .name(
        append(".").append(meth.getName()).append("(");

        // Parameters
        Class<?>[] paramTypes = meth.getParameterTypes();
        for (int i = 0, iMax = paramTypes.length; i < iMax; i++) {
            appendParamType(paramTypes[i], i);
            if (i + 1 < iMax) append(",");
        }

        // Append Save()/BuildAll()
        if (returnType == void.class)
            appendln("); return null;");
        else appendln(");");
    }

    public void appendParamType(Class<?> aClass, int anIndex)
    {
        if (aClass == double.class)
            append("doubleVal(theArgs[").append(anIndex).append("])");
        else if (aClass == int.class)
            append("intVal(theArgs[").append(anIndex).append("])");
        else if (aClass == float.class)
            append("floatVal(theArgs[").append(anIndex).append("])");
        else if (aClass == boolean.class)
            append("boolVal(theArgs[").append(anIndex).append("])");
        else if (aClass == Object.class)
            append("theArgs[").append(anIndex).append("]");
        else if (aClass == Object[].class)
            append("(").append("Object[]").append(") ").append("theArgs[").append(anIndex).append("]");
        else {
            String className = className(aClass);
            append("(").append(className).append(") ").append("theArgs[").append(anIndex).append("]");
        }
    }

    /**
     * Prints invokeConstructor() method.
     */
    public void printInvokeConstructorForClasses(Class<?>[] theClasses)
    {
        appendln("/**");
        appendln(" * Invokes constructors for given constructor id and args.");
        appendln(" */");
        appendln("public static Object invokeConstructor(String anId, Object ... theArgs) throws Exception");
        appendln("{");
        appendln("    switch (anId) {");

        for (Class<?> cls : theClasses)
            printInvokeConstructorForClass(cls);

        // Append trailer
        appendln("");
        appendln("        // Handle anything else");
        appendln("        default: throw new NoSuchMethodException(\"Unknown constructor: \" + anId);");
        appendln("    }");
        appendln("}");
    }

    /**
     * Prints invokeConstructor() method.
     */
    public void printInvokeConstructorForClass(Class aClass)
    {
        JavaClass javaClass = _resolver.getJavaClassForClass(aClass);
        JavaConstructor[] constructors = javaClass.getConstructors().toArray(new JavaConstructor[0]);
        Stream<JavaConstructor> constrStream = Stream.of(constructors);
        constrStream = constrStream.filter(c -> isValidConstructor(c));
        constructors = constrStream.toArray(size -> new JavaConstructor[size]);
        if (constructors.length == 0)
            return;

        String className = className(aClass);
        appendln("");
        append("        // Handle ").appendln(className);

        for (int i = 0, iMax = constructors.length; i < iMax; i++) {
            JavaConstructor constructor = constructors[i];
            printInvokeConstructorForConstructor(constructor);
        }
    }

    /**
     * Prints invokeConstructor() for method.
     */
    public void printInvokeConstructorForConstructor(JavaConstructor aConstructor)
    {
        appendln("        case \"" + aConstructor.getId() + "\":");

        Constructor constructor = aConstructor.getConstructor();

        // Preface
        append("            return new ");

        // If static just do ClassName.
        String castClassName = constructor.getDeclaringClass().getName();
        append(castClassName).append("(");

        // Parameters
        Class<?>[] paramTypes = constructor.getParameterTypes();
        for (int i = 0, iMax = paramTypes.length; i < iMax; i++) {
            appendParamType(paramTypes[i], i);
            if (i + 1 < iMax) append(",");
        }

        // Append close
        appendln(");");
    }

    // Append method
    StaticResolverGen append(int aVal) { _sb.append(aVal); return this;}
    StaticResolverGen append(String aStr) { _sb.append(aStr); return this;}
    StaticResolverGen appendln(String aStr) { _sb.append(aStr).append('\n'); return this;}
    String className(Class<?> aClass) {
        if (aClass.isPrimitive() || aClass.isArray())
            return _resolver.getJavaClassForClass(aClass).getClassName();
        return aClass.getName();
    }

    /**
     * Prints the postamble.
     */
    public void printPostamble()
    {
        appendln("");
        appendln("    // Conveniences");
        appendln("    private static boolean boolVal(Object anObj)  { return SnapUtils.boolValue(anObj); }");
        appendln("    private static int intVal(Object anObj)  { return SnapUtils.intValue(anObj); }");
        appendln("    private static double doubleVal(Object anObj)  { return SnapUtils.doubleValue(anObj); }");
        appendln("    private static float floatVal(Object anObj)  { return SnapUtils.floatValue(anObj); }");
        appendln("}");
    }

    /**
     * Prints packages for class.
     */
    public static void printClassesForPackage(String packageName)
    {
        // Get JRE site
        WebURL jreURL = WebURL.getURL(List.class);
        WebSite jreSite = jreURL.getSite();

        // Get class files
        WebFile pkgDir = jreSite.getFile(packageName);
        List<WebFile> files = pkgDir.getFiles();
        Stream<WebFile> filesStream = files.stream();
        Stream<WebFile> classFilesStream = filesStream.filter(f -> f.getType().equals("class") && f.getName().indexOf('$') <0);
        WebFile[] classFiles = classFilesStream.toArray(size -> new WebFile[size]);

        // Print
        for (int i = 0; i < classFiles.length; i++) {
            WebFile classFile = classFiles[i];
            System.out.println(classFile.getPath().substring(1).replace("/", ".") + ",");
        }
    }

    /**
     * Standard main implementation.
     */
    public static void main(String[] args)
    {
        //printClassesForPackage("/java/util");

        StaticResolverGen codeGen = new StaticResolverGen();
        codeGen.printPreamble();
        codeGen.printGetFieldsForClass();
        codeGen.printGetMethodsForClassForClasses(_javaUtilClasses);
        codeGen.printInvokeMethodForClasses(_javaUtilClasses);
        codeGen.printGetConstructorsForClassForClasses(_javaUtilClasses);
        codeGen.printInvokeConstructorForClasses(_javaUtilClasses);
        codeGen.printPostamble();

        WebFile webFile = WebURL.getURL("/tmp/StaticResolver.java").createFile(false);
        webFile.setText(_sb.toString());
        webFile.save();
    }

    // Packages
    private static Class[]  _javaUtilClasses = {

            java.lang.Object.class,
            java.lang.String.class,
            java.lang.Number.class,
            //java.lang.StringBuffer.class,
            //java.lang.StringBuilder.class,
            java.lang.System.class,
            java.lang.Math.class,

            java.util.Arrays.class,
            //java.util.BitSet.class,
            //java.util.Collection.class,
            //java.util.Collections.class,
            //java.util.Comparator.class,
            //java.util.Date.class,
            //java.util.EventListener.class,
            //java.util.EventObject.class,
            java.util.List.class,
            java.util.Map.class,
            //java.util.Objects.class,
            java.util.Random.class,
            java.util.Set.class,

            java.io.PrintStream.class,

            java.util.stream.DoubleStream.class,

            snap.view.Button.class,
            snap.view.View.class,
            snap.view.ViewAnim.class,
            snap.view.ViewOwner.class,
    };

    // WhiteList
    private static String[] _whiteListStrings = {

            // Object
            "clone", "equals", "getClass", "hashCode", "toString",

            // String
            "charAt", "compareTo", "compareToIgnoreCase", "concat", "contains", "endsWith", "equals", "equalsIgnoreCase",
            "format", "getBytes", "indexOf", "isEmpty", "join", "lastIndexOf", "length", "matches", "replace",
            "replaceAll", "split", "startsWith", "substring", "toLowerCase", "toCharArray", "toUpperCase", "trim",
            "valueOf",

            // Number
            "byteValue", "doubleValue", "floatValue", "intValue", "longValue", "shortValue",

            // System
            "arraycopy", "currentTimeMillis", "getProperties", "getProperty", "identityHashCode", "nanoTime",

            // Math
            "abs", "acos", "atan", "atan2", "cbrt", "ceil", "copySign", "cos", "cosh", "exp", "floor", "floorDiv",
            "floorMod", "hypot", "log", "log10", "max", "min", "pow", "random", "round", "sin", "sinh", "sqrt", "tan",
            "tanh", "toDegrees", "toRadians",

            // Arrays

            // List

            // Map

            // Random
            "nextInt", "nextDouble",

            // Set

            // PrintStream
            "print", "println",

            // DoubleStream
            "of", "map", "filter", "toArray",

            // Button
            "setTitle",

            // View
            "setPrefWidth", "setPrefHeight", "setPrefSize", "setBorder", "setFill", "getAnim",
            "setRotate", "setScaleX", "setScaleY", "setScale", "setTransX", "setTransY",

            // ViewAnim
            "play",

            // ViewOwner
            "setWindowVisible",
    };
    private static Set<String>  _whiteList = new HashSet<>(Arrays.asList(_whiteListStrings));

    private static String[] _blackListStrings = {

            "java.lang.String.getBytes(int,int,byte[],int)",

            "java.util.Map.replaceAll(java.util.function.BiFunction)",

            "java.io.PrintStream.println(char[])",
            "java.io.PrintStream.format(java.util.Locale,java.lang.String,java.lang.Object[])",
            "java.io.PrintStream.format(java.lang.String,java.lang.Object[])",
            "java.io.PrintStream.print(boolean)",
            "java.io.PrintStream.print(float)",

            "java.lang.String(java.lang.StringBuffer)",
            "java.lang.String(byte[],int)",
            "java.lang.String(byte[],int,int,int)",
            "java.io.PrintStream(java.lang.String)",
            "java.io.PrintStream(java.lang.String,java.lang.String)",
            "java.io.PrintStream(java.io.File,java.lang.String)",
            "java.io.PrintStream(java.io.File)",
    };
    private static Set<String>  _blackList = new HashSet<>(Arrays.asList(_blackListStrings));

}
