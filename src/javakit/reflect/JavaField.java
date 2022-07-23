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

}
