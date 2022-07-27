/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import java.lang.reflect.GenericArrayType;

/**
 * This class represents a Java GenericArrayType.
 */
public class JavaGenericArrayType extends JavaType {

    /**
     * Constructor.
     */
    public JavaGenericArrayType(Resolver anOwner, JavaDecl aPar, GenericArrayType aGenArrayType)
    {
        // Do normal version
        super(anOwner, aPar, aGenArrayType);

        // Set type/id
        _type = DeclType.GenArrayType;
        _id = ResolverUtils.getIdForGenericArrayType(aGenArrayType);

        // Set type info
        _name = _simpleName = aGenArrayType.getTypeName();
        _evalType = getJavaClassForClass(Object[].class);
    }

    /**
     * Override to return false.
     */
    @Override
    public boolean isResolvedType()  { return false; }
}
