/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.shell;
import java.lang.reflect.*;
import java.util.*;
import javakit.parse.*;
import javakit.reflect.JavaClass;
import javakit.reflect.JavaDecl;
import javakit.reflect.Resolver;
import static javakit.shell.JSEvalExprUtils.*;
import snap.parse.Parser;
import snap.util.*;

/**
 * A class to evaluate expressions.
 */
public class JSEvalExpr {

    // The current "this" object
    protected Object  _thisObj;

    // A map of local variables
    private Map<String, Object>  _locals = new HashMap<>();

    // A parser to parse expressions
    private static Parser  _exprParser = JavaParser.getShared().getExprParser();

    // A Resolver
    protected Resolver  _resolver;

    /**
     * Constructor.
     */
    public JSEvalExpr()
    {

    }

    /**
     * Evaluate expression.
     */
    public Object eval(String anExpr)
    {
        // Get JExpr for string
        _exprParser.setInput(anExpr);
        JExpr expr = _exprParser.parseCustom(JExpr.class);
        expr.setResolver(_resolver);

        // Eval expression
        Object value;
        try {
            value = evalExpr(expr);
        }

        // Handle exceptions
        catch (Exception e) {
            return e;
        }

        // Return
        return value;
    }

    /**
     * Evaluate JExpr.
     */
    public Object evalExpr(JExpr anExpr) throws Exception
    {
        Object thisObj = thisObject();
        return evalExpr(thisObj, anExpr);
    }

    /**
     * Evaluates given expression on given object reference.
     */
    public Object evalExpr(Object anOR, JExpr anExpr) throws Exception
    {
        // Handle Literal
        if (anExpr instanceof JExprLiteral)
            return evalJExprLiteral((JExprLiteral) anExpr);

        // Handle variable
        if (anExpr instanceof JExprId)
            return evalJExprId(anOR, (JExprId) anExpr);

        // Handle method call
        if (anExpr instanceof JExprMethodCall)
            return evalJExprMethodCall(anOR, (JExprMethodCall) anExpr);

        // Handle math expression
        if (anExpr instanceof JExprMath)
            return evalJExprMath(anOR, (JExprMath) anExpr);

        // Handle array dereference
        if (anExpr instanceof JExprArrayIndex)
            return evalJExprArrayIndex(anOR, (JExprArrayIndex) anExpr);

        // Handle expression chain
        if (anExpr instanceof JExprChain)
            return evalJExprChain(anOR, (JExprChain) anExpr);

        // Handle alloc expression
        if(anExpr instanceof JExprAlloc)
            return evalJExprAlloc(anOR, (JExprAlloc) anExpr);

        //if(aExpr instanceof JExpr.CastExpr) writeJExprCast((JExpr.CastExpr)aExpr);
        //if(aExpr instanceof JExprId) writeJExprId((JExprId)aExpr);
        //if(aExpr instanceof JExpr.InstanceOfExpr) writeJExprInstanceOf((JExpr.InstanceOfExpr)aExpr);
        //if(aExpr instanceof JExprLambda) writeJExprLambda((JExprLambda)aExpr);
        //if(aExpr instanceof JExprMethodRef) writeJExprMethodRef((JExprMethodRef)aExpr);
        //if(aExpr instanceof JExprType) writeJExprType((JExprType)aExpr); */

        // Complain
        throw new RuntimeException("JSEvalExpr.evalExpr: Unsupported expression " + anExpr.getClass());
    }

    /**
     * Evaluate JExprLiteral.
     */
    private Object evalJExprLiteral(JExprLiteral aLiteral) throws Exception
    {
        switch (aLiteral.getLiteralType()) {
            case Boolean: return (Boolean) aLiteral.getValue();
            case Integer: return (Integer) aLiteral.getValue();
            case Long: return (Long) aLiteral.getValue();
            case Float: return (Float) aLiteral.getValue();
            case Double: return (Double) aLiteral.getValue();
            case Character: return (Character) aLiteral.getValue();
            case String: return (String) aLiteral.getValue();
            case Null: return null;
            default: throw new RuntimeException("No Literal Type");
        }
    }

    /**
     * Evaluate JExprId.
     */
    private Object evalJExprId(Object anOR, JExprId anId) throws Exception
    {
        String name = anId.getName();
        return evalName(anOR, name);
    }

    /**
     * Evaluate JIdentifier.
     */
    private Object evalName(Object anOR, String aName) throws Exception
    {
        // If name is "this", return ThisObject
        if (aName == null) return null;
        if (aName.equals("this")) return thisObject();

        // Check for local variable
        if (isLocalVar(aName))
            return getLocalVarValue(aName);

        // Check for field
        if (isField(anOR, aName))
            return getFieldValue(anOR, aName);

        // Check for class name
        Class cls = getClassForName(anOR, aName);
        if (cls != null)
            return cls;

        // Complain
        throw new RuntimeException("Identifier not found: " + aName);
    }

    /**
     * Evaluate JExprMethodCall.
     */
    private Object evalJExprMethodCall(Object anOR, JExprMethodCall anExpr) throws Exception
    {
        // If object null, throw NullPointerException
        if (anOR == null)
            return null;

        // Get method name
        String methodName = anExpr.getName();

        // Get arg info
        Object thisObj = thisObject();
        int argCount = anExpr.getArgCount();
        Object[] argValues = new Object[argCount];

        // Iterate over arg expressions and get evaluated values
        for (int i = 0; i < argCount; i++) {
            JExpr argExpr = anExpr.getArg(i);
            argValues[i] = evalExpr(thisObj, argExpr);
        }

        // Invoke method and return
        Object val = invokeMethod(anOR, methodName, argValues);
        return val;
    }

    /**
     * Evaluate JExprArrayIndex.
     */
    private Object evalJExprArrayIndex(Object anOR, JExprArrayIndex anExpr) throws Exception
    {
        // Get Array
        JExpr arrayExpr = anExpr.getArrayExpr();
        Object arrayObj = evalExpr(anOR, arrayExpr);
        if (!isArray(arrayObj))
            return null;

        // Get Index
        Object thisObj = thisObject();
        JExpr indexExpr = anExpr.getIndexExpr();
        Object indexObj = evalExpr(thisObj, indexExpr); //if (!isPrimitive(indexObj)) return null;
        int index = intValue(indexObj);

        // Return Array value at index
        return Array.get(arrayObj, index);
    }

    /**
     * Evaluate JExprChain.
     */
    protected Object evalJExprAlloc(Object anOR, JExprAlloc anExpr) throws Exception
    {
        // Get real class for expression
        JavaDecl exprDecl = anExpr.getDecl();
        JavaClass javaClass = exprDecl.getEvalClass();
        Class<?> realClass = javaClass.getRealClass();

        // Handle array
        if (realClass.isArray()) {

            // Get Index
            JExpr dimensionExpr = anExpr.getArrayDims(); // Should be a list
            if (dimensionExpr != null) {

                // Get dimension
                Object thisObj = thisObject();
                Object dimensionObj = evalExpr(thisObj, dimensionExpr);
                int arrayLen = intValue(dimensionObj);

                // Create/return array
                Class<?> compClass = realClass.getComponentType();
                return Array.newInstance(compClass, arrayLen);
            }
        }

        // Get arg info
        Object thisObj = thisObject();
        List<JExpr> argExprs = anExpr.getArgs();
        int argCount = argExprs.size();
        Object[] argValues = new Object[argCount];

        // Iterate over arg expressions and get evaluated values
        for (int i = 0; i < argCount; i++) {
            JExpr argExpr = argExprs.get(i);
            argValues[i] = evalExpr(thisObj, argExpr);
        }

        // Create new instance and return
        Object newInstance = invokeConstructor(realClass, argValues);
        return newInstance;
    }

    /**
     * Evaluate JExprChain.
     */
    private Object evalJExprChain(Object anOR, JExprChain anExpr) throws Exception
    {
        Object val = anOR; //Object or = anOR;

        // Iterate over chain
        for (int i = 0, iMax = anExpr.getExprCount(); i < iMax; i++) {
            JExpr expr = anExpr.getExpr(i);
            val = evalExpr(val, expr); //val = evalExpr(or, expr);
            //if(val instanceof ObjectReference) or = (ObjectReference)val;
        }

        // Return
        return val;
    }

    /**
     * Evaluate JExprMath.
     */
    private Object evalJExprMath(Object anOR, JExprMath anExpr) throws Exception
    {
        // Get Op and OpCount
        JExprMath.Op op = anExpr.getOp();
        int opCount = anExpr.getOperandCount();

        // Handle Assign special
        if (op == JExprMath.Op.Assign)
            return evalJExprMathAssign(anOR, anExpr);

        // Get first value
        JExpr expr1 = anExpr.getOperand(0);
        Object val1 = evalExpr(anOR, expr1);

        // Handle Unary
        if (opCount == 1) {

            if (op == JExprMath.Op.Not) {
                if (isBoolean(val1)) {
                    boolean val = boolValue(val1);
                    return mirrorOf(!val);
                }
                throw new RuntimeException("Logical Not MathExpr not boolean: " + anExpr.toString());
            }

            if (op == JExprMath.Op.Negate) { // Need to not promote everything to double
                if (isPrimitive(val1)) {
                    double val = doubleValue(val1);
                    return mirrorOf(-val);
                }
                throw new RuntimeException("Numeric Negate MathExpr not numeric: " + anExpr.toString());
            }

            else switch (op) {
                case Not:
                default: throw new RuntimeException("Operator not supported " + anExpr.getOp());
                // PreIncrement, PreDecrement, BitComp, PostIncrement, PostDecrement
            }
        }

        // Handle Binary
        else if (opCount == 2) {
            JExpr expr2 = anExpr.getOperand(1);
            Object val2 = evalExpr(anOR, expr2);
            switch (op) {
                case Add: return add(val1, val2);
                case Subtract: return subtract(val1, val2);
                case Multiply: return multiply(val1, val2);
                case Divide: return divide(val1, val2);
                case Mod: return mod(val1, val2);
                case Equal:
                case NotEqual:
                case LessThan:
                case GreaterThan:
                case LessThanOrEqual:
                case GreaterThanOrEqual: return compareNumeric(val1, val2, op);
                case Or:
                case And: return compareLogical(val1, val2, op);
                default: throw new RuntimeException("Operator not supported " + anExpr.getOp());
                // BitOr, BitXOr, BitAnd, InstanceOf, ShiftLeft, ShiftRight, ShiftRightUnsigned,
            }
        }

        // Handle ternary
        else if (opCount == 3 && op == JExprMath.Op.Conditional) {
            if (!isPrimitive(val1)) throw new RuntimeException("Ternary conditional expr not bool: " + expr1);
            boolean result = boolValue(val1);
            JExpr expr = result ? anExpr.getOperand(1) : anExpr.getOperand(2);
            return evalExpr(anOR, expr);
        }

        // Complain
        throw new RuntimeException("Invalid MathExpr " + anExpr.toString());
    }

    /**
     * Handle JExprMath Assign.
     */
    private Object evalJExprMathAssign(Object anOR, JExprMath anExpr) throws Exception
    {
        // Get value expression/value
        JExpr valExpr = anExpr.getOperand(1);
        Object value = evalExpr(anOR, valExpr);

        // Get name expression/name
        JExpr leftSideExpr = anExpr.getOperand(0);

        // Handle array
        if (leftSideExpr instanceof JExprArrayIndex) {

            // Get name
            JExprArrayIndex arrayIndexExpr = (JExprArrayIndex) leftSideExpr;
            JExpr arrayNameExpr = arrayIndexExpr.getArrayExpr();
            String arrayName = arrayNameExpr.getName();

            // Get Index
            Object thisObj = thisObject();
            JExpr indexExpr = arrayIndexExpr.getIndexExpr();
            Object indexObj = evalExpr(thisObj, indexExpr); //if (!isPrimitive(indexObj)) return null;
            int index = intValue(indexObj);

            // Set value
            setLocalVarArrayValueAtIndex(arrayName, value, index);
            return value;
        }

        // Get name of variable
        String varName = leftSideExpr.getName();

        // Set local var value and return value
        setLocalVarValue(varName, value);
        return value;
    }

    /**
     * Handle JExprMath Assign.
     */
    Object evalJExprMathAssign(Object anOR, String aName, JExprMath anExpr) throws Exception
    {
        Object value = evalExpr(anOR, anExpr);
        setLocalVarValue(aName, value);
        return value;
    }

    /**
     * Return the current this object.
     */
    public Object thisObject()  { return _thisObj; }

    /**
     * Returns whether there is a local variable for name.
     */
    public boolean isLocalVar(String aName)
    {
        return _locals.keySet().contains(aName);
    }

    /**
     * Returns a local variable value by name.
     */
    public Object getLocalVarValue(String aName)
    {
        return _locals.get(aName);
        // StackFrame frame = anApp.getCurrentFrame();
        // LocalVariable lvar = frame.visibleVariableByName(name);
        // if (lvar != null) return frame.getValue(lvar);
    }

    /**
     * Sets a local variable value by name.
     */
    public void setLocalVarValue(String aName, Object aValue)
    {
        _locals.put(aName, aValue);
    }

    /**
     * Sets a local variable value by name.
     */
    public void setLocalVarArrayValueAtIndex(String aName, Object aValue, int anIndex)
    {
        Object array = getLocalVarValue(aName);
        Array.set(array, anIndex, aValue);
    }

    /**
     * Returns whether name is a field of given object.
     */
    public boolean isField(Object anObj, String aName)
    {
        Class cls = anObj instanceof Class ? (Class) anObj : anObj.getClass();
        Field field = ClassUtils.getFieldForName(cls, aName);
        return field != null;
    }

    /**
     * Returns the value of the field.
     */
    public Object getFieldValue(Object anObj, String aName)
    {
        Class cls = anObj instanceof Class ? (Class) anObj : anObj.getClass();
        Field field = ClassUtils.getFieldForName(cls, aName);
        try {
            return field.get(anObj);
        } catch (Exception e) {
            return null;
        }
        //ReferenceType refType = anOR.referenceType();
        //Field field = refType.fieldByName(name);
        //if(field!=null) return anOR.getValue(field);
    }

    /**
     * Invoke method.
     */
    public Object invokeMethod(Object anObj, String aName, Object[] theArgs) throws Exception
    {
        // Get object class
        Class<?> objClass = anObj.getClass(); // anObj instanceof Class? (Class)anObj : anObj.getClass();

        // Get parameter classes
        Class<?>[] paramClasses = new Class[theArgs.length];
        for (int i = 0, iMax = theArgs.length; i < iMax; i++) {
            Object arg = theArgs[i];
            paramClasses[i] = arg != null ? arg.getClass() : null;
        }

        // Get method
        Method meth = MethodUtils.getMethodBest(objClass, aName, paramClasses);

        // Invoke method
        return meth.invoke(anObj, theArgs);
    }

    /**
     * Invoke constructor.
     */
    public Object invokeConstructor(Class<?> aClass, Object[] theArgs) throws Exception
    {
        // Get parameter classes
        Class<?>[] paramClasses = new Class[theArgs.length];
        for (int i = 0, iMax = theArgs.length; i < iMax; i++) {
            Object arg = theArgs[i];
            paramClasses[i] = arg != null ? arg.getClass() : null;
        }

        // Get method
        Constructor<?> constructor = MethodUtils.getConstructor(aClass, paramClasses);

        // Invoke method
        return constructor.newInstance(theArgs);
    }

    /**
     * Returns a class for given name.
     */
    protected Class<?> getClassForName(Object anOR, String aName)
    {
        JavaClass javaClass = getJavaClassForName(anOR, aName);
        Class<?> realClass = javaClass != null ? javaClass.getRealClass() : null;
        return realClass;
    }

    /**
     * Returns a class for given name.
     */
    protected JavaClass getJavaClassForName(Object anOR, String aName)
    {
        // Look for inner class
        Class<?> realClass = anOR instanceof Class ? (Class<?>) anOR : anOR.getClass();
        JavaClass javaClass = _resolver.getJavaClassForClass(realClass);
        String innerClassName = javaClass.getName() + '$' + aName;
        JavaClass cls = javaClass.getInnerClassForName(innerClassName);
        if (cls != null)
            return cls;

        // Look for root level class
        JavaClass rootLevelClass = _resolver.getJavaClassForName(aName);
        if (rootLevelClass != null)
            return rootLevelClass;

        // Return not found
        return null;
    }

    /**
     * Returns a new evaluator for given object.
     */
    public static JSEvalExpr getExprEvaluatorForThisObject(Object anObj)
    {
        JSEvalExpr eval = new JSEvalExpr();
        eval._thisObj = anObj;
        return eval;
    }
}