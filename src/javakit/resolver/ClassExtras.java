/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;

import java.lang.reflect.*;
import java.util.*;

import snap.util.ClassUtils;

/**
 * Utility methods for Class.
 */
public class ClassExtras extends ClassUtils {

    /**
     * Returns a field for a parent class and a name.
     */
    public static Field getField(Class aClass, String aName)
    {
        Class cls = aClass.isPrimitive() ? fromPrimitive(aClass) : aClass;
        Field field = getDeclaredField(cls, aName);
        if (field != null)
            return field;

        // Check superclass
        Class sclass = cls.getSuperclass();
        if (sclass != null)
            field = getField(sclass, aName);
        if (field != null)
            return field;

        // Check interfaces
        for (Class c : cls.getInterfaces()) {
            field = getField(c, aName);
            if (field != null)
                return field;
        }

        // Return null since not found
        return null;
    }

    /**
     * Returns a field for a parent class and a name.
     */
    public static Field getDeclaredField(Class aClass, String aName)
    {
        Field fields[] = aClass.getDeclaredFields();
        for (Field field : fields)
            if (field.getName().equals(aName))
                return field;
        return null;
    }

    /**
     * Returns all methods for given class and subclasses that start with given prefix.
     */
    public static Method[] getMethods(Class aClass, String aPrefix)
    {
        List meths = new ArrayList();
        getMethods(aClass, aPrefix.toLowerCase(), meths, true);
        return (Method[]) meths.toArray(new Method[0]);
    }

    /**
     * Returns the method for given class, name and parameter types.
     */
    private static void getMethods(Class aClass, String aPrefix, List theMethods, boolean doPrivate)
    {
        Class cls = aClass.isPrimitive() ? fromPrimitive(aClass) : aClass;
        Method methods[] = cls.getDeclaredMethods();
        for (Method meth : methods) {
            if (meth.isSynthetic()) continue;
            if (meth.getName().toLowerCase().startsWith(aPrefix) && (doPrivate || !Modifier.isPrivate(meth.getModifiers())))
                theMethods.add(meth);
        }

        // If interface, recurse for extended interfaces
        if (cls.isInterface()) {
            for (Class cl : cls.getInterfaces())
                getMethods(cl, aPrefix, theMethods, false);
        }

        // Otherwise, recurse for extended superclass
        else if ((cls = cls.getSuperclass()) != null)
            getMethods(cls, aPrefix, theMethods, false);
    }

    /**
     * Returns the method for given class, name and parameter types.
     */
    public static Constructor getConstructor(Class aClass, Class theClasses[])
    {
        // Get methods that match name/args (just return if null, no args or singleton)
        if (aClass.isInterface()) return null;
        Constructor methods[] = getConstructors(aClass, theClasses, null);
        if (methods == null) return null;
        if (theClasses.length == 0 || methods.length == 1) return methods[0];

        // Rate compatible constructors and return the most compatible
        Constructor method = null;
        int rating = 0;
        for (Constructor meth : methods) {
            int rtg = getRating(meth.getParameterTypes(), theClasses, meth.isVarArgs());
            if (rtg > rating) {
                method = meth;
                rating = rtg;
            }
        }
        return method;
    }

    /**
     * Returns the method for given class, name and parameter types.
     */
    private static Constructor[] getConstructors(Class aClass, Class theClasses[], Constructor theResult[])
    {
        Class cls = aClass.isPrimitive() ? fromPrimitive(aClass) : aClass;
        Constructor methods[] = getDeclaredConstructors(cls, theClasses, theResult);
        if ((cls = cls.getSuperclass()) != null)
            methods = getConstructors(cls, theClasses, methods);
        return methods;
    }

    /**
     * Returns the declared method for a given class, name and parameter types array.
     */
    private static Constructor[] getDeclaredConstructors(Class aClass, Class theClasses[], Constructor theResult[])
    {
        // Iterate over declared constructors and if compatible, add to results
        Constructor methods[] = aClass.getDeclaredConstructors();
        for (Constructor meth : methods)
            if (isCompatible(meth.getParameterTypes(), theClasses, meth.isVarArgs())) {
                theResult = theResult != null ? Arrays.copyOf(theResult, theResult.length + 1) : new Constructor[1];
                theResult[theResult.length - 1] = meth;
            }
        return theResult;
    }

    /**
     * Returns a declared inner class for a given class and a name, checking super classes as well.
     */
    public static Class getClass(Class aClass, String aName)
    {
        // Make sure class is non-primitive
        Class cls = aClass.isPrimitive() ? fromPrimitive(aClass) : aClass;

        // Check declared inner classes
        Class cls2 = getDeclaredClass(cls, aName);
        if (cls2 != null)
            return cls2;

        // Check superclass
        Class sclass = cls.getSuperclass();
        if (sclass != null)
            cls2 = getClass(sclass, aName);
        if (cls2 != null)
            return cls2;

        // Check interfaces
        for (Class c : cls.getInterfaces()) {
            cls2 = getClass(c, aName);
            if (cls2 != null)
                return cls2;
        }

        // Return null since class not found
        return null;
    }

    /**
     * Returns a class for a parent class and a name.
     */
    public static Class getDeclaredClass(Class aClass, String aName)
    {
        for (Class cls : aClass.getDeclaredClasses())
            if (cls.getSimpleName().equals(aName))
                return cls;
        return null;
    }

}