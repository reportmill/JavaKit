/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

/**
 * This class provides utility methods for JavaClass.
 */
public class JavaClassUtils {

    /**
     * Returns a compatible method for given name and param types.
     */
    public static List<JavaField> getFieldsForMatcher(JavaClass aClass, Matcher aMatcher)
    {
        // Create return list of prefix fields
        List<JavaField> fieldsWithPrefix = new ArrayList<>();

        // Iterate over classes
        for (JavaClass cls = aClass; cls != null; cls = cls.getSuperClass()) {

            // Get Class fields
            List<JavaField> fields = cls.getFields();
            for (JavaField field : fields)
                if (aMatcher.reset(field.getName()).lookingAt())
                    fieldsWithPrefix.add(field);

            // Should iterate over class interfaces, too
        }

        // Return list of prefix fields
        return fieldsWithPrefix;
    }

    /**
     * Returns methods that match given matcher.
     */
    public static List<JavaMethod> getMethodsForMatcher(JavaClass aClass, Matcher aPrefix)
    {
        // Create return list of prefix methods
        List<JavaMethod> methodsWithPrefix = new ArrayList<>();

        // Iterate over classes
        for (JavaClass cls = aClass; cls != null; cls = cls.getSuperClass()) {

            // Get Class methods
            List<JavaMethod> methods = cls.getMethods();
            for (JavaMethod method : methods)
                if (aPrefix.reset(method.getName()).lookingAt())
                    methodsWithPrefix.add(method);

            // If interface, iterate over class interfaces, too (should probably do this anyway to catch default methods).
            if (cls.isInterface()) {
                JavaClass[] interfaces = cls.getInterfaces();
                for (JavaClass interf : interfaces) {
                    List<JavaMethod> moreMethods = getMethodsForMatcher(interf, aPrefix);
                    methodsWithPrefix.addAll(moreMethods);
                }
            }
        }

        // Return list of prefix methods
        return methodsWithPrefix;
    }

    /**
     * Returns a compatible constructor for given name and param types.
     */
    public static JavaConstructor getCompatibleConstructor(JavaClass aClass, JavaType[] theTypes)
    {
        List<JavaConstructor> constructors = aClass.getConstructors();
        JavaConstructor constructor = null;
        int rating = 0;

        // Iterate over constructors to find highest rating
        for (JavaConstructor constr : constructors) {
            int rtg = JavaExecutable.getMatchRatingForTypes(constr, theTypes);
            if (rtg > rating) {
                constructor = constr;
                rating = rtg;
            }
        }

        // Return
        return constructor;
    }

    /**
     * Returns a compatible method for given name and param types.
     */
    public static JavaMethod getCompatibleMethod(JavaClass aClass, String aName, JavaType[] theTypes)
    {
        List<JavaMethod> methods = aClass.getMethods();
        JavaMethod method = null;
        int rating = 0;

        // Iterate over methods to find highest rating
        for (JavaMethod meth : methods) {
            if (meth.getName().equals(aName)) {
                int rtg = JavaExecutable.getMatchRatingForTypes(meth, theTypes);
                if (rtg > rating) {
                    method = meth;
                    rating = rtg;
                }
            }
        }

        // Return
        return method;
    }

    /**
     * Returns a compatible method for given name and param types.
     */
    public static JavaMethod getCompatibleMethodDeep(JavaClass aClass, String aName, JavaType[] theTypes)
    {
        // Search this class and superclasses for compatible method
        for (JavaClass cls = aClass; cls != null; cls = cls.getSuperClass()) {
            JavaMethod decl = getCompatibleMethod(cls, aName, theTypes);
            if (decl != null)
                return decl;
        }
        return null;
    }

    /**
     * Returns a compatible method for given name and param types.
     */
    public static JavaMethod getCompatibleMethodAll(JavaClass aClass, String aName, JavaType[] theTypes)
    {
        // Search this class and superclasses for compatible method
        JavaMethod decl = getCompatibleMethodDeep(aClass, aName, theTypes);
        if (decl != null)
            return decl;

        // Search this class and superclasses for compatible interface
        for (JavaClass cls = aClass; cls != null; cls = cls.getSuperClass()) {
            for (JavaClass infc : cls.getInterfaces()) {
                decl = getCompatibleMethodAll(infc, aName, theTypes);
                if (decl != null)
                    return decl;
            }
        }

        // If this class is Interface, check Object
        if (aClass.isInterface()) {
            JavaClass objClass = aClass.getJavaClassForClass(Object.class);
            return getCompatibleMethodDeep(objClass, aName, theTypes);
        }

        // Return null since compatible method not found
        return null;
    }

    /**
     * Returns a compatible methods for given name and param types.
     */
    public static List<JavaMethod> getCompatibleMethods(JavaClass aClass, String aName, JavaType[] theTypes)
    {
        List<JavaMethod> matches = Collections.EMPTY_LIST;
        List<JavaMethod> methods = aClass.getMethods();

        // Iterate over methods to find highest rating
        for (JavaMethod method : methods) {
            if (method.getName().equals(aName)) {
                int rtg = JavaExecutable.getMatchRatingForTypes(method, theTypes);
                if (rtg > 0) {
                    if (matches == Collections.EMPTY_LIST)
                        matches = new ArrayList<>();
                    matches.add(method);
                }
            }
        }

        // Return
        return matches;
    }

    /**
     * Returns a compatible method for given name and param types.
     */
    public static List<JavaMethod> getCompatibleMethodsDeep(JavaClass aClass, String aName, JavaType[] theTypes)
    {
        // Search this class and superclasses for compatible method
        List<JavaMethod> matches = Collections.EMPTY_LIST;

        // Iterate over this class and parents
        for (JavaClass cls = aClass; cls != null; cls = cls.getSuperClass()) {
            List<JavaMethod> decls = getCompatibleMethods(cls, aName, theTypes);
            if (decls.size() > 0) {
                if (matches == Collections.EMPTY_LIST)
                    matches = decls;
                else matches.addAll(decls);
            }
        }

        // Return
        return matches;
    }

    /**
     * Returns a compatible method for given name and param types.
     */
    public static List<JavaMethod> getCompatibleMethodsAll(JavaClass aClass, String aName, JavaType[] theTypes)
    {
        // Search this class and superclasses for compatible method
        List<JavaMethod> matches = Collections.EMPTY_LIST;
        List<JavaMethod> methods = getCompatibleMethodsDeep(aClass, aName, theTypes);
        if (methods.size() > 0)
            matches = methods;

        // Search this class and superclasses for compatible interface
        for (JavaClass cls = aClass; cls != null; cls = cls.getSuperClass()) {
            for (JavaClass infc : cls.getInterfaces()) {
                methods = getCompatibleMethodsAll(infc, aName, theTypes);
                if (methods.size() > 0) {
                    if (matches == Collections.EMPTY_LIST) matches = methods;
                    else matches.addAll(methods);
                }
            }
        }

        // If this class is Interface, check Object
        if (aClass.isInterface()) {
            JavaClass objDecl = aClass.getJavaClassForClass(Object.class);
            methods = getCompatibleMethodsDeep(objDecl, aName, theTypes);
            if (methods.size() > 0) {
                if (matches == Collections.EMPTY_LIST) matches = methods;
                else matches.addAll(methods);
            }
        }

        // Remove supers and duplicates
        for (int i = 0; i < matches.size(); i++) {
            JavaMethod method = matches.get(i);
            for (JavaMethod superMethod = method.getSuper(); superMethod != null; superMethod = superMethod.getSuper())
                matches.remove(superMethod);
            for (int j = i + 1; j < matches.size(); j++)
                if (matches.get(j) == method)
                    matches.remove(j);
        }

        // Return null since compatible method not found
        return matches;
    }
}
