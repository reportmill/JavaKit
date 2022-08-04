/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.shell;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.DoubleUnaryOperator;
import javakit.parse.*;
import javakit.reflect.*;
import static javakit.shell.JSEvalExprUtils.*;
import snap.util.*;

/**
 * A class to evaluate expressions.
 */
public class JSEvalExpr {

    // The current "this" object
    protected Object  _thisObj;

    // A map of local variables
    private Map<String, Object>  _locals = new HashMap<>();

    // A Resolver
    protected Resolver  _resolver;

    /**
     * Constructor.
     */
    public JSEvalExpr()
    {

    }

    /**
     * Evaluate JExpr.
     */
    public Object evalExpr(JExpr anExpr) throws Exception
    {
        _resolver = anExpr.getResolver();
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
            return evalLiteralExpr((JExprLiteral) anExpr);

        // Handle variable
        if (anExpr instanceof JExprId)
            return evalIdentifierExpr(anOR, (JExprId) anExpr);

        // Handle method call
        if (anExpr instanceof JExprMethodCall)
            return evalExprMethodCall(anOR, (JExprMethodCall) anExpr);

        // Handle math expression
        if (anExpr instanceof JExprMath)
            return evalMathExpr(anOR, (JExprMath) anExpr);

        // Handle array dereference
        if (anExpr instanceof JExprArrayIndex)
            return evalArrayIndexExpr(anOR, (JExprArrayIndex) anExpr);

        // Handle expression chain
        if (anExpr instanceof JExprChain)
            return evalExprChain(anOR, (JExprChain) anExpr);

        // Handle alloc expression
        if(anExpr instanceof JExprAlloc)
            return evalExprAlloc(anOR, (JExprAlloc) anExpr);

        //if(aExpr instanceof JExpr.CastExpr) writeJExprCast((JExpr.CastExpr)aExpr);
        //if(aExpr instanceof JExprId) writeJExprId((JExprId)aExpr);
        //if(aExpr instanceof JExpr.InstanceOfExpr) writeJExprInstanceOf((JExpr.InstanceOfExpr)aExpr);

        // Handle lambda expression
        if(anExpr instanceof JExprLambda)
            return anExpr;

        //if(aExpr instanceof JExprMethodRef) writeJExprMethodRef((JExprMethodRef)aExpr);
        //if(aExpr instanceof JExprType) writeJExprType((JExprType)aExpr); */

        // Complain
        throw new RuntimeException("JSEvalExpr.evalExpr: Unsupported expression " + anExpr.getClass());
    }

    /**
     * Evaluate JExprLiteral.
     */
    private Object evalLiteralExpr(JExprLiteral aLiteral) throws Exception
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
    private Object evalIdentifierExpr(Object anOR, JExprId anId) throws Exception
    {
        // Get identifier name
        String name = anId.getName();

        // Evaluate to see if it has value
        Object value = evalName(anOR, name);

        // If not found, but ExprId evaluates to class, return class
        if (value == null) {
            JavaDecl decl = anId.getDecl();
            if (decl instanceof JavaClass)
                return ((JavaClass) decl).getRealClass();
        }

        // Return
        return value;
    }

    /**
     * Evaluate JIdentifier.
     */
    private Object evalName(Object anOR, String aName) throws Exception
    {
        // If name is "this", return ThisObject
        if (aName == null) return null;
        if (aName.equals("this")) return thisObject();

        // Handle array length
        if (aName.equals("length") && isArray(anOR))
            return Array.getLength(anOR);

        // Check for local variable
        if (isLocalVar(aName))
            return getLocalVarValue(aName);

        // Bogus for TeaVM
        if (anOR == System.class && SnapUtils.isTeaVM)
            return aName.equals("err") ? System.err : System.out;

        // Look for field
        if (anOR != null && !SnapUtils.isTeaVM) {
            Class cls = anOR instanceof Class ? (Class) anOR : anOR.getClass();
            Field field = ClassUtils.getFieldForName(cls, aName);
            if (field != null)
                return field.get(anOR);
        }

        // Check for class name
        Class cls = getClassForName(anOR, aName);
        if (cls != null)
            return cls;

        // Complain
        return null;
        //throw new RuntimeException("Identifier not found: " + aName);
    }

    /**
     * Evaluate JExprMethodCall.
     */
    private Object evalExprMethodCall(Object anOR, JExprMethodCall anExpr) throws Exception
    {
        // If object null, throw NullPointerException
        if (anOR == null)
            return null;

        // Get method
        JavaMethod method = anExpr.getDecl();
        if (method == null)
            throw new NoSuchMethodException("JEvalExpr: Method not found for " + anExpr.getName());

        // Get arg info
        Object thisObj = thisObject();
        int argCount = anExpr.getArgCount();
        Object[] argValues = new Object[argCount];

        // Iterate over arg expressions and get evaluated values
        for (int i = 0; i < argCount; i++) {
            JExpr argExpr = anExpr.getArg(i);
            argValues[i] = evalExpr(thisObj, argExpr);
        }

        // Invoke method
        Object value = _resolver.invokeMethod(anOR, method, argValues);
        return value;
    }

    /**
     * Evaluate JExprArrayIndex.
     */
    private Object evalArrayIndexExpr(Object anOR, JExprArrayIndex anExpr) throws Exception
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
     * Evaluate JExprAlloc.
     */
    protected Object evalExprAlloc(Object anOR, JExprAlloc anExpr) throws Exception
    {
        // Get real class for expression
        JavaDecl exprDecl = anExpr.getDecl();
        JavaClass javaClass = exprDecl.getEvalClass();
        Class<?> realClass = javaClass.getRealClass();

        // Handle array
        if (realClass.isArray()) {

            // Handle inits
            List<JExpr> initsExpr = anExpr.getArrayInits();
            if (initsExpr != null && initsExpr.size() > 0) {

                // Create array
                int arrayLen = initsExpr.size();
                Class<?> compClass = realClass.getComponentType();
                Object array = Array.newInstance(compClass, arrayLen);
                Object thisObj = thisObject();

                // Iterate over arg expressions and get evaluated values
                for (int i = 0; i < arrayLen; i++) {
                    JExpr initExpr = initsExpr.get(i);
                    Object initValue = evalExpr(thisObj, initExpr);
                    initValue = castOrConvertValueToPrimitiveClass(initValue, compClass);
                    Array.set(array, i, initValue);
                }

                // Return
                return array;
            }

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

        // Special case
        List<JExpr> argExprs = anExpr.getArgs();
        int argCount = argExprs.size();
        if (argCount == 0)
            return realClass.newInstance();

        // Get constructor
        JavaConstructor javaConstructor = (JavaConstructor) exprDecl;

        // Get arg info
        Object thisObj = thisObject();
        Object[] argValues = new Object[argCount];

        // Iterate over arg expressions and get evaluated values
        for (int i = 0; i < argCount; i++) {
            JExpr argExpr = argExprs.get(i);
            argValues[i] = evalExpr(thisObj, argExpr);
        }

        // Invoke constructor
        Object newInstance = _resolver.invokeConstructor(realClass, javaConstructor, argValues);
        return newInstance;
    }

    /**
     * Evaluate JExprChain.
     */
    private Object evalExprChain(Object anOR, JExprChain anExpr) throws Exception
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
    private Object evalMathExpr(Object anOR, JExprMath anExpr) throws Exception
    {
        // Get Op and OpCount
        JExprMath.Op op = anExpr.getOp();
        int opCount = anExpr.getOperandCount();

        // Handle Assign special
        if (op == JExprMath.Op.Assign)
            return evalAssignExpr(anOR, anExpr);

        // Get first value
        JExpr expr1 = anExpr.getOperand(0);
        String exprName = expr1 instanceof JExprId ? expr1.getName() : null;
        Object val1 = evalExpr(anOR, expr1);

        // Handle Unary
        if (opCount == 1) {

            switch (op) {

                // Handle Not
                case Not: {
                    if (!isBoolean(val1))
                        throw new RuntimeException("Logical Not MathExpr not boolean: " + anExpr);
                    boolean val = boolValue(val1);
                    return !val;
                }

                // Handle Negate
                case Negate: {
                    if (!isPrimitive(val1))
                        throw new RuntimeException("Numeric Negate Expr not numeric: " + anExpr);
                    double val = doubleValue(val1);
                    return -val;
                }

                // Handle Increment
                case PreIncrement: {
                    if (!isPrimitive(val1))
                        throw new RuntimeException("Numeric PreIncrement Expr not numeric: " + anExpr);
                    Object val2 = add(val1, 1);
                    setLocalVarValue(exprName, val2);
                    return val2;
                }

                // Handle Decrement
                case PreDecrement: {
                    if (!isPrimitive(val1))
                        throw new RuntimeException("Numeric PreDecrement Expr not numeric: " + anExpr);
                    Object val2 = add(val1, -1);
                    setLocalVarValue(exprName, val2);
                    return val2;
                }

                // Handle Increment
                case PostIncrement: {
                    if (!isPrimitive(val1))
                        throw new RuntimeException("Numeric PostIncrement Expr not numeric: " + anExpr);
                    setLocalVarValue(exprName, add(val1, 1));
                    return val1;
                }

                // Handle Decrement
                case PostDecrement: {
                    if (!isPrimitive(val1))
                        throw new RuntimeException("Numeric PostDecrement Expr not numeric: " + anExpr);
                    setLocalVarValue(exprName, add(val1, -1));
                    return val1;
                }

                // Handle unknown (BitComp?)
                default: throw new RuntimeException("Operator not supported " + anExpr.getOp());
            }
        }

        // Handle Binary
        else if (opCount == 2) {

            // Get second expression and value
            JExpr expr2 = anExpr.getOperand(1);
            Object val2 = evalExpr(anOR, expr2);

            // Handle binary op
            switch (op) {

                // Handle add
                case Add: return add(val1, val2);

                // Handle subtract
                case Subtract: return subtract(val1, val2);

                // Handle multiply
                case Multiply: return multiply(val1, val2);

                // Handle divide
                case Divide: return divide(val1, val2);

                // Handle Mod
                case Mod: return mod(val1, val2);

                // Handle compare
                case Equal:
                case NotEqual:
                case LessThan:
                case GreaterThan:
                case LessThanOrEqual:
                case GreaterThanOrEqual: return compareNumeric(val1, val2, op);
                case Or:
                case And: return compareLogical(val1, val2, op);

                // Handle unsupported: BitOr, BitXOr, BitAnd, InstanceOf, ShiftLeft, ShiftRight, ShiftRightUnsigned
                default: throw new RuntimeException("Operator not supported " + anExpr.getOp());
            }
        }

        // Handle ternary
        else if (opCount == 3 && op == JExprMath.Op.Conditional) {

            // Validate
            if (!isPrimitive(val1))
                throw new RuntimeException("Ternary conditional expr not bool: " + expr1);

            // Get resulting expression
            boolean result = boolValue(val1);
            JExpr resultExpr = result ? anExpr.getOperand(1) : anExpr.getOperand(2);

            // Evaluate resulting expression and return
            Object resultValue = evalExpr(anOR, resultExpr);
            return resultValue;
        }

        // Complain
        throw new RuntimeException("Invalid MathExpr " + anExpr.toString());
    }

    /**
     * Handle JExprMath Assign.
     */
    private Object evalAssignExpr(Object anOR, JExprMath anExpr) throws Exception
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
     * Handle JExprLambda.
     */
    private Object evalExprLambda(Object anOR, JExprLambda aLambdaExpr) throws Exception
    {
        final JExpr contentExpr = aLambdaExpr.getExpr();
        Object lambdaValue = null;

        // Get lambda class
        JavaClass lambdaClass = aLambdaExpr.getEvalClass();
        if (lambdaClass == null)
            return null;

        // Need to wrap Lambda in real expression
        Class<?> realClass = lambdaClass.getRealClass();

        // Handle DoubleUnaryOperator
        if (realClass == DoubleUnaryOperator.class) {
            DoubleUnaryOperator lambdaReal = d -> {
                setLocalVarValue("d", d);
                try {
                    Object value = evalExpr(anOR, contentExpr);
                    return SnapUtils.doubleValue(value);
                } catch (Exception e) {
                    return 0;
                }
            };
            lambdaValue = lambdaReal;
        }

        // Return
        return lambdaValue;
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
        // Get array
        Object array = getLocalVarValue(aName);

        // Make sure value is right type
        if (SnapUtils.isTeaVM) {
            Class<?> cls = array.getClass().getComponentType();
            if (cls.isPrimitive())
                aValue = castOrConvertValueToPrimitiveClass(aValue, cls);
        }

        // Set value
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
        if (anOR != null) {
            Class<?> realClass = anOR instanceof Class ? (Class<?>) anOR : anOR.getClass();
            JavaClass javaClass = _resolver.getJavaClassForClass(realClass);
            String innerClassName = javaClass != null ? javaClass.getName() + '$' + aName : null;
            JavaClass cls = javaClass.getInnerClassForName(innerClassName);
            if (cls != null)
                return cls;
        }

        // Look for root level class
        JavaClass rootLevelClass = _resolver.getJavaClassForName(aName);
        if (rootLevelClass != null)
            return rootLevelClass;

        // Return not found
        return null;
    }
}