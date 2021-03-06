package javakit.parse;

import javakit.reflect.JavaDecl;

/**
 * An class to represent expressions that include an operator (math, logical, etc.).
 */
public class JExprMath extends JExpr {

    // The operator
    public Op op;

    // Constants for op
    public enum Op {
        Add, Subtract, Multiply, Divide, Mod,
        Equal, NotEqual, LessThan, GreaterThan, LessThanOrEqual, GreaterThanOrEqual,
        Or, And, Not, BitOr, BitXOr, BitAnd, Conditional, Assign,
        ShiftLeft, ShiftRight, ShiftRightUnsigned,
        PreIncrement, PreDecrement, Negate, BitComp, PostIncrement, PostDecrement
    }

    /**
     * Creates a new expression.
     */
    public JExprMath()
    {
    }

    /**
     * Creates a new expression for given op and LeftHand expression.
     */
    public JExprMath(Op anOp, JExpr aFirst)
    {
        op = anOp;
        addOperand(aFirst);
    }

    /**
     * Creates a new expression for given op and LeftHand/RightHand expressions.
     */
    public JExprMath(Op anOp, JExpr aFirst, JExpr aSecond)
    {
        op = anOp;
        addOperand(aFirst);
        addOperand(aSecond);
    }

    /**
     * Returns the op.
     */
    public Op getOp()
    {
        return op;
    }

    /**
     * Returns the operand count.
     */
    public int getOperandCount()
    {
        return getChildCount();
    }

    /**
     * Returns the specified operand.
     */
    public JExpr getOperand(int anIndex)
    {
        return (JExpr) getChild(anIndex);
    }

    /**
     * Adds an operand.
     */
    public void addOperand(JExpr anExpr)
    {
        addChild(anExpr, -1);
    }

    /**
     * Sets the specified operand.
     */
    public void setOperand(JExpr anExpr, int anIndex)
    {
        if (anIndex < getChildCount()) replaceChild(getChild(anIndex), anExpr);
        else addChild(anExpr, -1);
    }

    /**
     * Returns the class name for expression.
     */
    protected JavaDecl getDeclImpl()
    {
        switch (op) {
            case Add:
            case Subtract:
            case Multiply:
            case Divide:
            case Mod:
                return getEvalTypeMath();
            case Equal:
            case NotEqual:
            case LessThan:
            case GreaterThan:
            case LessThanOrEqual:
            case GreaterThanOrEqual:
            case Or:
            case And:
            case Not:
                return getJavaDecl(boolean.class);
            case Conditional:
                return getEvalTypeConditional();
            case Assign:
                return getOperand(0).getEvalType();
            case BitOr:
            case BitXOr:
            case BitAnd:
                return getOperand(0).getEvalType();
            case ShiftLeft:
            case ShiftRight:
            case ShiftRightUnsigned:
                return getOperand(0).getEvalType();
            case PreIncrement:
            case PreDecrement:
            case Negate:
            case BitComp:
                return getOperand(0).getEvalType();
            case PostIncrement:
            case PostDecrement:
                return getOperand(0).getEvalType();
            default:
                return getJavaDecl(boolean.class);
        }
    }

    /**
     * Returns the class name for math expression.
     */
    private JavaDecl getEvalTypeMath()
    {
        // Get operand eval types (just return if either is null)
        int cc = getChildCount();
        JavaDecl e1 = cc > 0 ? getOperand(0).getEvalType() : null;
        JavaDecl e2 = cc > 1 ? getOperand(1).getEvalType() : null;
        if (e1 == null || e1 == e2) return e2;
        if (e2 == null) return e1;

        // Handle promotions: String, Double, Float, Long, Int
        String c1 = e1.getClassName(), c2 = e2.getClassName();
        if (isString(c1)) return e1;
        if (isString(c2)) return e2;
        if (isDouble(c1)) return e1;
        if (isDouble(c2)) return e2;
        if (isFloat(c1)) return e1;
        if (isFloat(c2)) return e2;
        if (isLong(c1)) return e1;
        if (isLong(c2)) return e2;
        if (isInt(c1)) return e1;
        if (isInt(c2)) return e2;
        return e1;
    }

    /**
     * Returns whether type names are numbers.
     */
    private boolean isString(String aName)
    {
        return aName.equals("java.lang.String");
    }

    private boolean isDouble(String aName)
    {
        return aName.equals("double") || aName.equals("java.lang.Double");
    }

    private boolean isFloat(String aName)
    {
        return aName.equals("float") || aName.equals("java.lang.Float");
    }

    private boolean isLong(String aName)
    {
        return aName.equals("long") || aName.equals("java.lang.Long");
    }

    private boolean isInt(String aName)
    {
        return aName.equals("int") || aName.equals("java.lang.Integer");
    }

    /**
     * Returns common ancestor of two decls.
     */
    private JavaDecl getEvalTypeConditional()
    {
        if (getChildCount() < 3) return getJavaDecl(Object.class);
        JavaDecl d0 = getOperand(1).getEvalType(), d1 = getOperand(2).getEvalType();
        if (d0 == null) return d1;
        if (d1 == null) return d0;
        return d0.getCommonAncestor(d1);
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()
    {
        return op + "Expr";
    }

    /**
     * Returns the Op string for op.
     */
    public static String getOpString(Op anOp)
    {
        switch (anOp) {
            case Add:
                return "+";
            case Subtract:
                return "-";
            case Multiply:
                return "*";
            case Divide:
                return "/";
            case Mod:
                return "%";
            case Equal:
                return "==";
            case NotEqual:
                return "!=";
            case LessThan:
                return "<";
            case GreaterThan:
                return ">";
            case LessThanOrEqual:
                return "<=";
            case GreaterThanOrEqual:
                return ">=";
            case Or:
                return "||";
            case And:
                return "&&";
            case Not:
                return "!";
            case BitOr:
                return "|";
            case BitXOr:
                return "^";
            case BitAnd:
                return "&";
            case Conditional:
                return "?";
            case Assign:
                return "=";
            case ShiftLeft:
                return "<<";
            case ShiftRight:
                return ">>";
            case ShiftRightUnsigned:
                return ">>>";
            case PreIncrement:
                return "++";
            case PreDecrement:
                return "--";
            case Negate:
                return "-";
            case BitComp:
                return "<DUNNO>";
            case PostIncrement:
                return "++";
            case PostDecrement:
                return "--";
            default:
                throw new RuntimeException("JExprMath: Unknown Op: " + anOp);
        }
    }

}