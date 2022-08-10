/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
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
        if (isTeaVM)
            return super.invokeMethod(anObj, javaMethod, theArgs);

        // Get method
        Method meth = javaMethod.getMethod();

        // If VarArgs, need to repackage args
        if (meth.isVarArgs())
            theArgs = new Object[] { theArgs };

        // Invoke
        return meth.invoke(anObj, theArgs);
    }

    /**
     * Invokes a constructor on given class with given args.
     */
    @Override
    public Object invokeConstructor(Class<?> aClass, JavaConstructor javaConstructor, Object[] theArgs) throws Exception
    {
        if (isTeaVM)
            return super.invokeConstructor(aClass, javaConstructor, theArgs);

        // Get method
        Constructor<?> constructor = javaConstructor.getConstructor();

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