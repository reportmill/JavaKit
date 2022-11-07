package javakit.resolver;

/**
 * This JavaDecl subclass represents reserved words
 */
public class JavaWord extends JavaDecl {

    /**
     * Constructor.
     */
    protected JavaWord(Resolver aResolver, String aName)
    {
        super(aResolver, DeclType.Word);
        _name = _simpleName = aName;
    }
}
