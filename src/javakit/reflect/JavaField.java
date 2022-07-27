/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import java.lang.reflect.*;

/**
 * This class represents a JavaClass Field.
 */
public class JavaField extends JavaMember {

    /**
     * Constructor.
     */
    public JavaField(Resolver anOwner, JavaClass aDeclaringClass, Field aField)
    {
        super(anOwner, aDeclaringClass, aField);

        _type = DeclType.Field;
        _evalType = _resolver.getTypeDecl(aField.getGenericType());
    }

    /**
     * Returns a string representation of suggestion.
     */
    @Override
    public String getSuggestionString()
    {
        // Get SimpleName, EvalType.SimpleName and DeclaringClass.SimpleName
        String simpleName = getSimpleName();
        JavaType evalType = getEvalType();
        String evalTypeName = evalType != null ? evalType.getSimpleName() : null;
        JavaClass declaringClass = getDeclaringClass();
        String declaringClassSimpleName = declaringClass.getSimpleName();

        // Construct string: SimpleName : EvalType.SimpleName - DeclaringCLass.SimpleName
        StringBuffer sb = new StringBuffer(simpleName);
        if (evalTypeName != null)
            sb.append(" : ").append(evalTypeName);
        sb.append(" - ").append(declaringClassSimpleName);

        // Return
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
