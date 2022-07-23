package javakit.reflect;
import javakit.resolver.Resolver;

import java.lang.reflect.*;

/**
 * This class represents a Java Method.
 */
public class JavaMethod extends JavaExecutable {

    // Whether method is Default method
    private boolean  _default;

    /**
     * Constructor.
     */
    public JavaMethod(Resolver anOwner, JavaDecl aPar, Method aMethod)
    {
        super(anOwner, aPar, aMethod);

        // Set type
        _type = DeclType.Method;

        // Get TypeVars
        TypeVariable[] typeVars = aMethod.getTypeParameters();
        _typeVars = new JavaTypeVariable[typeVars.length];
        for (int i = 0, iMax = typeVars.length; i < iMax; i++)
            _typeVars[i] = new JavaTypeVariable(_resolver, this, typeVars[i]);
        _varArgs = aMethod.isVarArgs();

        // Get Return Type
        Type returnType = aMethod.getReturnType();
        _evalType = _resolver.getTypeDecl(returnType);

        // Get GenericParameterTypes (this can fail https://bugs.openjdk.java.net/browse/JDK-8075483))
        Type[] paramTypes = aMethod.getGenericParameterTypes();
        if (paramTypes.length < aMethod.getParameterCount())
            paramTypes = aMethod.getParameterTypes();
        _paramTypes = new JavaType[paramTypes.length];
        for (int i = 0, iMax = paramTypes.length; i < iMax; i++)
            _paramTypes[i] = _resolver.getTypeDecl(paramTypes[i]);

        // Set default
        _default = aMethod.isDefault();
    }

    /**
     * Returns whether Method is default type.
     */
    public boolean isDefault()  { return _default; }
}
