package javakit.reflect;
import javakit.resolver.Resolver;
import java.lang.reflect.Type;

/**
 * This class represents a generic Java Type: Class, ParameterizedType, TypeVariable, GenericArrayType.
 */
public class JavaType extends JavaDecl {

    /**
     * Constructor.
     */
    protected JavaType(Resolver anOwner, JavaDecl aPar, Object aType)
    {
        // Do normal version
        super(anOwner, aPar, aType);

    }

    /**
     * Init object.
     */
    protected void initObject(Object anObj)  { }

    /**
     * Returns whether is a enum reference.
     */
    public boolean isEnum()  { return false; }

    /**
     * Returns whether is a interface reference.
     */
    public boolean isInterface()  { return false; }

    /**
     * Returns whether is an array.
     */
    public boolean isArray()  { return false; }

    /**
     * Returns the Array item type (if Array).
     */
    public JavaType getArrayItemType()  { return null; }

}
