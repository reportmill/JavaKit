/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import snap.util.MethodUtils;

import java.lang.reflect.*;

/**
 * A class that manages all the JavaDecls for a project.
 */
public class ResolverSys extends Resolver {

    /**
     * Constructor.
     */
    public ResolverSys()  { super(null); }

    /**
     * Constructor.
     */
    public ResolverSys(ClassLoader aClassLoader)
    {
        super(aClassLoader);
    }

    /**
     * Invokes a method on given object for name and args.
     */
    @Override
    public Object invokeMethod(Object anObj, JavaMethod javaMethod, Object[] theArgs) throws Exception
    {
        // Get method
        Method meth = javaMethod.getMethod();

        // Invoke
        return meth.invoke(anObj, theArgs);
    }

    /**
     * Invokes a constructor on given class with given args.
     */
    @Override
    public Object invokeConstructor(Class<?> aClass, Object[] theArgs) throws Exception
    {
        // Get parameter classes
        Class<?>[] paramClasses = new Class[theArgs.length];
        for (int i = 0, iMax = theArgs.length; i < iMax; i++) {
            Object arg = theArgs[i];
            paramClasses[i] = arg != null ? arg.getClass() : null;
        }

        // Get method
        Constructor<?> constructor = MethodUtils.getConstructor(aClass, paramClasses);

        // Invoke method
        return constructor.newInstance(theArgs);
    }

    /**
     * Needed for TeaVM.
     */
    public Type getGenericSuperClassForClass(Class<?> aClass)
    {
        return aClass.getGenericSuperclass();
    }

    /**
     * Needed for TeaVM.
     */
    public TypeVariable<?>[] getTypeParametersForClass(Class<?> aClass)
    {
        return aClass.getTypeParameters();
    }

    /**
     * Needed for TeaVM.
     */
    public Class<?>[] getDeclaredClassesForClass(Class<?> aClass)
    {
        return aClass.getDeclaredClasses();
    }

    /**
     * Needed for TeaVM.
     */
    public Type getGenericTypeForField(Field aField)
    {
        return aField.getGenericType();
    }

    /**
     * Needed for TeaVM.
     */
    public Type getGenericReturnTypeForMethod(Method aMethod)
    {
        return aMethod.getGenericReturnType();
    }

    /**
     * Needed for TeaVM.
     */
    public TypeVariable<?>[] getTypeParametersForExecutable(Member aMember)
    {
        Executable exec = (Executable) aMember;
        return exec.getTypeParameters();
    }

    /**
     * Needed for TeaVM.
     */
    public Type[] getGenericParameterTypesForExecutable(Member aMember)
    {
        // Get GenericParameterTypes (this can fail https://bugs.openjdk.java.net/browse/JDK-8075483))
        Executable exec = (Executable) aMember;
        Type[] paramTypes = exec.getGenericParameterTypes();
        if (paramTypes.length < exec.getParameterCount())
            paramTypes = exec.getParameterTypes();
        return paramTypes;
    }

    /**
     * Needed for TeaVM.
     */
    public boolean isDefaultMethod(Method aMethod)
    {
        return aMethod.isDefault();
    }
}