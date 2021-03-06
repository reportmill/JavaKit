package javakit.parse;

import javakit.reflect.JavaDecl;
import javakit.reflect.JavaClass;

import java.util.Collections;
import java.util.List;

/**
 * A JNode for Enum constants.
 */
public class JEnumConst extends JMemberDecl {
    // The args
    List<JExpr> _args = Collections.EMPTY_LIST;

    // The class or interface body
    String _classBody;

    /**
     * Returns the arguments.
     */
    public List<JExpr> getArgs()
    {
        return _args;
    }

    /**
     * Sets the arguments.
     */
    public void setArgs(List<JExpr> theArgs)
    {
        if (_args != null) for (JExpr arg : _args) removeChild(arg);
        _args = theArgs;
        if (_args != null) for (JExpr arg : _args) addChild(arg, -1);
    }

    /**
     * Returns the class decl.
     */
    public String getClassBody()
    {
        return _classBody;
    }

    /**
     * Sets the class decl.
     */
    public void setClassBody(String aBody)
    {
        _classBody = aBody;
    }

    /**
     * Get class name from parent enum declaration.
     */
    protected JavaDecl getDeclImpl()
    {
        // Get enum name, enclosing JClassDecl and it's JavaDeclClass (can be null if enum hasn't been compiled yet)
        String name = getName();
        JClassDecl cdecl = (JClassDecl) getParent();
        JavaClass jdecl = cdecl.getDecl();
        if (jdecl == null)
            return null;

        // Get JavaDecl for enum constant, which is just a field of enum class
        JavaDecl edecl = jdecl.getField(name);
        return edecl;
    }

    /**
     * Override to resolve enum id.
     */
    protected JavaDecl getDeclImpl(JNode aNode)
    {
        if (aNode == _id) return getDecl();
        return super.getDeclImpl(aNode);
    }

}