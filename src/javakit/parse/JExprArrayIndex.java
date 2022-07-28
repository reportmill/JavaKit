package javakit.parse;

import javakit.reflect.JavaDecl;

/**
 * A JExpr subclass for ArrayIndex expressions.
 */
public class JExprArrayIndex extends JExpr {

    // The expression for array
    JExpr _arrayExpr;

    // The expression for array index
    JExpr _indexExpr;

    /**
     * Creates a new ArrayIndex.
     */
    public JExprArrayIndex(JExpr anArrayExpr, JExpr anIndexExpr)
    {
        setIndexExpr(anIndexExpr);
    }

    /**
     * Returns the array expression.
     */
    public JExpr getArrayExpr()
    {
        return _arrayExpr;
    }

    /**
     * Sets the index expression.
     */
    public void setArrayExpr(JExpr anExpr)
    {
        if (_arrayExpr == null) addChild(_arrayExpr = anExpr, 0);
        else replaceChild(_arrayExpr, _arrayExpr = anExpr);
    }

    /**
     * Returns the index expression.
     */
    public JExpr getIndexExpr()
    {
        return _indexExpr;
    }

    /**
     * Sets the index expression.
     */
    public void setIndexExpr(JExpr anExpr)
    {
        replaceChild(_indexExpr, _indexExpr = anExpr);
    }

    /**
     * Returns the part name.
     **/
    public String getNodeString()
    {
        return "ArrayIndex";
    }

    /**
     * Tries to resolve the class name for this node.
     */
    protected JavaDecl getDeclImpl()
    {
        Class pclass = _arrayExpr != null ? _arrayExpr.getEvalTypeRealClass() : null;
        Class iclass = pclass != null && pclass.isArray() ? pclass.getComponentType() : Object.class;
        return getJavaDecl(iclass);
    }

}
