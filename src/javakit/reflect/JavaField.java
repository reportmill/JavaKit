/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import javakit.resolver.Resolver;

import java.lang.reflect.*;

/**
 * This class represents a JavaClass Field.
 */
public class JavaField extends JavaMember {

    /**
     * Constructor.
     */
    public JavaField(Resolver anOwner, JavaDecl aPar, Field aField)
    {
        super(anOwner, aPar, aField);

        _type = DeclType.Field;
        _evalType = _resolver.getTypeDecl(aField.getGenericType());
    }

    /**
     * Returns a string representation of suggestion.
     */
    @Override
    public String getSuggestionString()
    {
        StringBuffer sb = new StringBuffer(getSimpleName());

        JavaType evalType = getEvalType();
        if (evalType != null)
            sb.append(" : ").append(evalType.getSimpleName());
        String className = getClassSimpleName();
        if (className != null)
            sb.append(" - ").append(className);

        // Return string
        return sb.toString();
    }

    /**
     * Returns a name suitable to describe declaration.
     */
    @Override
    public String getPrettyName()
    {
        String className = getClassName();
        String fieldName = getName();
        return className + '.' + fieldName;
    }

    /**
     * Returns a name unique for matching declarations.
     */
    @Override
    public String getMatchName()
    {
        String className = getClassName();
        String fieldName = getName();
        return className + '.' + fieldName;
    }
}
