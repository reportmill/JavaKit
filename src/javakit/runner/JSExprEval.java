/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.runner;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import javakit.parse.*;

import static javakit.runner.JSExprEvalUtils.*;

import javakit.resolver.*;
import snap.props.PropObject;
import snap.util.*;
import snap.view.EventListener;

/**
 * A class to evaluate expressions.
 */
public class JSExprEval {

    // The Statement Evaluator that created this instance
    private JSStmtEval  _stmtEval;

    // The current "this" object
    protected Object  _thisObj;

    // A map of local variables
    protected JSVarStack _varStack = new JSVarStack();

    // A Resolver
    protected Resolver _resolver;

    /**
     * Constructor.
     */
    public JSExprEval(JSStmtEval aStmtEval)
    {
        _stmtEval = aStmtEval;
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
            return evalIdExpr(anOR, (JExprId) anExpr);

        // Handle expression chain
        if (anExpr instanceof JExprChain)
            return evalExprChain(anOR, (JExprChain) anExpr);

        // Handle math expression
        if (anExpr instanceof JExprMath)
            return evalMathExpr(anOR, (JExprMath) anExpr);

        // Handle method call
        if (anExpr instanceof JExprMethodCall)
            return evalMethodCallExpr(anOR, (JExprMethodCall) anExpr);

        // Handle assign expression
        if (anExpr instanceof JExprAssign)
            return evalAssignExpr(anOR, (JExprAssign) anExpr);

        // Handle array dereference
        if (anExpr instanceof JExprArrayIndex)
            return evalArrayIndexExpr(anOR, (JExprArrayIndex) anExpr);

        // Handle alloc expression
        if (anExpr instanceof JExprAlloc)
            return evalAllocExpr(anOR, (JExprAlloc) anExpr);

        // Handle cast expression
        if (anExpr instanceof JExprCast)
            return evalCastExpr(anOR, (JExprCast) anExpr);

        // Handle paren expression
        if (anExpr instanceof JExprParen) {
            JExpr innerExpr = ((JExprParen) anExpr).getExpr();
            return evalExpr(anOR, innerExpr);
        }

        // Handle Instanceof expression
        if (anExpr instanceof JExprInstanceOf)
            throw new RuntimeException("JSExprEval.evalTypeExpr() not implemented");

        // Handle lambda expression
        if (anExpr instanceof JExprLambda)
            return evalLambdaExpr(anOR, (JExprLambda) anExpr);

        // Handle method ref expression
        if (anExpr instanceof JExprMethodRef)
            throw new RuntimeException("JSExprEval.evalMethodRef() not implemented");

        // Handle Type expression
        if (anExpr instanceof JExprType)
            return evalTypeExpr(anOR, (JExprType) anExpr);

        // Complain
        throw new RuntimeException("JSExprEval.evalExpr: Unsupported expression " + anExpr.getClass());
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
    private Object evalIdExpr(Object anOR, JExprId anId) throws Exception
    {
        // Get identifier name
        String name = anId.getName();

        // If LocalVar, get from stack
        JavaDecl idDecl = anId.getDecl();
        if (idDecl instanceof JavaLocalVar) {
            int indexInStackFrame = ((JavaLocalVar) idDecl).getIndexInStackFrame();
            if (indexInStackFrame >= 0) {
                Object value = _varStack.getStackValue(indexInStackFrame);
                return value;
            }
        }

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
        if (aName == null)
            return null;
        if (aName.equals("this"))
            return thisObject();
        if (aName.equals("class"))
            return anOR;

        // Handle array length
        if (aName.equals("length") && isArray(anOR))
            return Array.getLength(anOR);

        // Bogus for TeaVM
        if (anOR == System.class && SnapUtils.isTeaVM)
            return aName.equals("err") ? System.err : System.out;

        // Look for field
        if (anOR != null && !SnapUtils.isTeaVM) {
            Class<?> cls = anOR instanceof Class ? (Class<?>) anOR : anOR.getClass();
            Field field = ClassUtils.getFieldForName(cls, aName);
            if (field != null)
                return field.get(anOR);
        }

        // Check for class name
        Class<?> cls = getClassForName(anOR, aName);
        if (cls != null)
            return cls;

        // Complain
        return null;
        //throw new RuntimeException("Identifier not found: " + aName);
    }

    /**
     * Evaluate JExprMethodCall.
     */
    private Object evalMethodCallExpr(Object anOR, JExprMethodCall methodCallExpr) throws Exception
    {
        // Get arg values
        Object thisObj = thisObject();
        int argCount = methodCallExpr.getArgCount();
        Object[] argValues = new Object[argCount];
        for (int i = 0; i < argCount; i++) {
            JExpr argExpr = methodCallExpr.getArg(i);
            argValues[i] = evalExpr(thisObj, argExpr);
        }

        // Get method
        JavaMethod method = methodCallExpr.getDecl();
        if (method == null) {

            // Check for PropObject
            if (anOR instanceof PropObject)
                return evalMethodCallExprForPropObject((PropObject) anOR, methodCallExpr.getName(), argValues);

            // Alright, now we can give up
            throw new NoSuchMethodException("JSExprEval: Method not found for " + methodCallExpr.getName());
        }

        // If object null, throw NullPointerException
        if (anOR == null && !method.isStatic())
            throw new NullPointerException("JSExprEval: Can't call " + methodCallExpr.getName() + " on null");

        // Look for local MethodDecl
        JMethodDecl methodDecl = method.getMethodDecl();
        if (methodDecl != null)
            return evalMethodCallExprForMethodDecl(anOR, methodDecl, argValues);

        // Invoke method
        Object value = _resolver.invokeMethod(anOR, method, argValues);
        return value;
    }

    /**
     * Evaluate JExprMethodCall for local JMethodDecl.
     */
    private Object evalMethodCallExprForMethodDecl(Object anOR, JMethodDecl aMethodDecl, Object[] argValues) throws Exception
    {
        // Create stack frame
        _varStack.pushStackFrame();

        // Install params
        List<JVarDecl> params = aMethodDecl.getParameters();
        for (int i = 0, iMax = params.size(); i < iMax; i++) {
            JVarDecl varDecl = params.get(i);
            JExprId varId = varDecl.getId();
            setExprIdValue(varId, argValues[i]);
        }

        // Get method body and run
        JStmtBlock methodBody = aMethodDecl.getBlock();
        Object returnVal = _stmtEval.evalExecutable(anOR, methodBody);

        // Pop stack frame
        _varStack.popStackFrame();

        // Return
        return returnVal;
    }

    /**
     * Evaluate JExprMethodCall for PropObject.
     */
    private Object evalMethodCallExprForPropObject(PropObject propObject, String methName, Object[] argValues) throws Exception
    {
        if ((methName.startsWith("is") || methName.startsWith("get") || methName.startsWith("set"))) {

            // Get PropName
            String propName = methName.substring(methName.startsWith("is") ? 2 : 3);

            // Handle set
            if (methName.startsWith("set")) {
                if (argValues.length > 0) {
                    propObject.setPropValue(propName, argValues[0]);
                    return null;
                }
            }

            // Handle is/get
            return propObject.getPropValue(propName);
        }

        throw new NoSuchMethodException("JSExprEval: Method not found for " + methName);
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
    protected Object evalAllocExpr(Object anOR, JExprAlloc anExpr) throws Exception
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
     * Evaluate JExprCast.
     */
    protected Object evalCastExpr(Object anOR, JExprCast aCastExpr) throws Exception
    {
        // Get expression and evaluate
        JExpr expr = aCastExpr.getExpr();
        Object value = evalExpr(anOR, expr);

        // Get type - if not primative, just return
        JType type = aCastExpr.getType();
        JavaType typeClass = type != null ? type.getDecl() : null;
        if (typeClass == null) {
            System.out.println("JSExprEval: Couldn't get type for cast expression: " + aCastExpr);
            return value;
        }

        // If not primitve, just return value
        if (!typeClass.isPrimitive())
            return value;

        // If value is null, complain
        if (value == null)
            throw new RuntimeException("JSExprEval: Trying to cast null to " + typeClass.getClassName());

        // If valueClass is assignable to type class, just return value
        Class<?> valueClass = value.getClass();
        Class<?> castClass = typeClass.getEvalClass().getRealClass();
        if (castClass.isAssignableFrom(valueClass))
            return value;

        // Cast value and return
        Object castValue = JSExprEvalUtils.castOrConvertValueToPrimitiveClass(value, castClass);
        return castValue;
    }

    /**
     * Evaluate JExprChain.
     */
    private Object evalExprChain(Object anOR, JExprChain anExpr) throws Exception
    {
        Object val = anOR;

        // Iterate over chain
        for (int i = 0, iMax = anExpr.getExprCount(); i < iMax; i++) {
            JExpr expr = anExpr.getExpr(i);
            val = evalExpr(val, expr);
        }

        // Return
        return val;
    }

    /**
     * Evaluate JExprMath.
     */
    private Object evalMathExpr(Object anOR, JExprMath anExpr) throws Exception
    {
        // Get first value
        JExpr expr1 = anExpr.getOperand(0);
        Object val1 = evalExpr(anOR, expr1);

        // Handle Unary
        int opCount = anExpr.getOperandCount();
        if (opCount == 1)
            return evalMathExprUnary(anExpr, expr1, val1);

        // Handle Binary: Get second expression and value
        else if (opCount == 2) {
            JExpr expr2 = anExpr.getOperand(1);
            Object val2 = evalExpr(anOR, expr2);
            return evalMathExprBinary(anExpr, val1, val2);
        }

        // Handle ternary
        else if (opCount == 3) {

            // Validate
            if (!isBoolean(val1))
                throw new RuntimeException("Ternary conditional expr not bool: " + expr1);

            // Get resulting expression
            boolean result = boolValue(val1);
            JExpr resultExpr = result ? anExpr.getOperand(1) : anExpr.getOperand(2);

            // Evaluate resulting expression and return
            Object resultValue = evalExpr(anOR, resultExpr);
            return resultValue;
        }

        // Complain
        throw new RuntimeException("Invalid MathExpr " + anExpr);
    }

    /**
     * Evaluate JExprMath unary expression.
     */
    private Object evalMathExprUnary(JExprMath anExpr, JExpr assignToExpr, Object val1) throws Exception
    {
        JExprMath.Op op = anExpr.getOp();

        switch (op) {

            // Handle Not
            case Not:
                if (isBoolean(val1))
                    return !boolValue(val1);
                throw new RuntimeException("Logical Not MathExpr not boolean: " + anExpr);

                // Handle Negate
            case Negate: {
                if (isNumberOrChar(val1))
                    return -doubleValue(val1);
                throw new RuntimeException("Numeric Negate Expr not numeric: " + anExpr);
            }

            // Handle Increment
            case PreIncrement: {
                if (isNumberOrChar(val1)) {
                    Object val2 = add(val1, 1);
                    setAssignExprValue(assignToExpr, val2);
                    return val2;
                }
                throw new RuntimeException("Numeric PreIncrement Expr not numeric: " + anExpr);
            }

            // Handle Decrement
            case PreDecrement: {
                if (isNumberOrChar(val1)) {
                    Object val2 = add(val1, -1);
                    setAssignExprValue(assignToExpr, val2);
                    return val2;
                }
                throw new RuntimeException("Numeric PreDecrement Expr not numeric: " + anExpr);
            }

            // Handle Increment
            case PostIncrement: {
                if (isNumberOrChar(val1)) {
                    setAssignExprValue(assignToExpr, add(val1, 1));
                    return val1;
                }
                throw new RuntimeException("Numeric PostIncrement Expr not numeric: " + anExpr);
            }

            // Handle Decrement
            case PostDecrement: {
                if (isNumberOrChar(val1)) {
                    setAssignExprValue(assignToExpr, add(val1, -1));
                    return val1;
                }
                throw new RuntimeException("Numeric PostDecrement Expr not numeric: " + anExpr);
            }

            // Handle unknown (BitComp?)
            default: throw new RuntimeException("Operator not supported " + anExpr.getOp());
        }
    }

    /**
     * Evaluate JExprMath binary expression.
     */
    private Object evalMathExprBinary(JExprMath anExpr, Object val1, Object val2)
    {
        JExprMath.Op op = anExpr.getOp();

        // Handle binary op
        switch (op) {

            // Handle add, subtract, multiply, divide, mod
            case Add: return add(val1, val2);
            case Subtract: return subtract(val1, val2);
            case Multiply: return multiply(val1, val2);
            case Divide: return divide(val1, val2);
            case Mod: return mod(val1, val2);

            // Handle equal/not-equal
            case Equal:
            case NotEqual: return compareEquals(val1, val2, op);

            // Handle compare numeric
            case LessThan:
            case GreaterThan:
            case LessThanOrEqual:
            case GreaterThanOrEqual: return compareNumeric(val1, val2, op);

            // Handle compare logical
            case Or:
            case And: return compareLogical(val1, val2, op);

            // Handle unsupported: BitOr, BitXOr, BitAnd, InstanceOf, ShiftLeft, ShiftRight, ShiftRightUnsigned
            default: throw new RuntimeException("Operator not supported " + anExpr.getOp());
        }
    }

    /**
     * Handle JExprMath Assign.
     */
    private Object evalAssignExpr(Object anOR, JExprAssign anExpr) throws Exception
    {
        // Get value expression/value
        JExpr valExpr = anExpr.getValueExpr();
        Object value = evalExpr(anOR, valExpr);

        // Get name expression/name
        JExpr assignToExpr = anExpr.getIdExpr();

        // If op not simple, perform math
        JExprAssign.Op assignOp = anExpr.getOp();
        if (assignOp != JExprAssign.Op.Assign) {

            // Get AssignToExpr value
            Object assignToValue = evalExpr(anOR, assignToExpr);

            // Get value with assign op
            switch (assignOp) {
                case Add: value = add(assignToValue, value); break;
                case Subtract: value = subtract(assignToValue, value); break;
                case Multiply: value = multiply(assignToValue, value); break;
                case Divide: value = divide(assignToValue, value); break;
                case Mod: value = mod(assignToValue, value); break;
                default: throw new RuntimeException("JSExprEval.evalAssignExpr: Op not yet supported: " + assignOp);
            }
        }

        // Set value
        Object assignedValue = setAssignExprValue(assignToExpr, value);
        return assignedValue;
    }

    /**
     * Handle JExprLambda.
     */
    private Object evalLambdaExpr(Object anOR, JExprLambda aLambdaExpr)
    {
        // Get lambda class
        JavaClass lambdaClass = aLambdaExpr.getEvalClass();
        if (lambdaClass == null)
            throw new RuntimeException("JSExprEval.evalLambdaExpr: Can't determine lambda class for expr: " + aLambdaExpr);

        // Need to wrap Lambda in real expression
        Class<?> realClass = lambdaClass.getRealClass();

        // Get/return lambda of lambda class that wraps given expression
        Object wrappedLambda = getWrappedLambdaExpression(anOR, aLambdaExpr, realClass);
        return wrappedLambda;
    }

    /**
     * Handle JExprType.
     */
    private Object evalTypeExpr(Object anOR, JExprType typeExpr)
    {
        JavaClass evalClass = typeExpr.getEvalClass();
        Class<?> realClass = evalClass != null ? evalClass.getRealClass() : null;
        if (realClass == null)
            throw new RuntimeException("JSExprEval.evalTypeExpr: Can't find type for expr: " + typeExpr);
        return realClass;
    }

    /**
     * Sets an assignment value for given assignTo expression and value.
     */
    public Object setAssignExprValue(JExpr assignToExpr, Object aValue) throws Exception
    {
        // Handle ExprId
        if (assignToExpr instanceof JExprId)
            return setExprIdValue((JExprId) assignToExpr, aValue);

        // Handle array
        if (assignToExpr instanceof JExprArrayIndex)
            return setExprArrayIndexValue((JExprArrayIndex) assignToExpr, aValue);

        // I don't think this can happen
        throw new RuntimeException("JExprEval.setAssignExprValue: Unexpected assign to class: " + assignToExpr.getClass());
    }

    /**
     * Sets an assignment value for given identifier expression and value.
     */
    protected Object setExprIdValue(JExprId idExpr, Object aValue)
    {
        // Get name
        String name = idExpr.getName();

        // Convert type
        JavaClass assignClass = idExpr.getEvalClass();
        Class<?> realClass = assignClass.getRealClass();
        Object assignValue = castOrConvertValueToPrimitiveClass(aValue, realClass);

        if (!_varStack.setStackValueForNode(idExpr, assignValue))
            System.err.println("JSExprEval: Unknown id: " + idExpr);
        return assignValue;
    }

    /**
     * Sets an assignment value for given identifier expression and value.
     */
    protected Object setExprArrayIndexValue(JExprArrayIndex arrayIndexExpr, Object aValue) throws Exception
    {
        // Get name
        JExpr arrayExpr = arrayIndexExpr.getArrayExpr();

        // Get Index
        Object thisObj = thisObject();
        JExpr indexExpr = arrayIndexExpr.getIndexExpr();
        Object indexObj = evalExpr(thisObj, indexExpr); //if (!isPrimitive(indexObj)) return null;
        int index = intValue(indexObj);

        // Get array
        Object array = _varStack.getStackValueForNode(arrayExpr);

        // Make sure value is right type
        if (SnapUtils.isTeaVM) {
            Class<?> cls = array.getClass().getComponentType();
            if (cls.isPrimitive())
                aValue = castOrConvertValueToPrimitiveClass(aValue, cls);
        }

        // Set value and return
        Array.set(array, index, aValue);
        return aValue;
    }

    /**
     * Return the current this object.
     */
    public Object thisObject()  { return _thisObj; }

//    /**
//     * Returns whether name is a field of given object.
//     */
//    public boolean isField(Object anObj, String aName)
//    {
//        Class cls = anObj instanceof Class ? (Class) anObj : anObj.getClass();
//        Field field = ClassUtils.getFieldForName(cls, aName);
//        return field != null;
//    }
//
//    /**
//     * Returns the value of the field.
//     */
//    public Object getFieldValue(Object anObj, String aName)
//    {
//        Class cls = anObj instanceof Class ? (Class) anObj : anObj.getClass();
//        Field field = ClassUtils.getFieldForName(cls, aName);
//        try { return field.get(anObj); }
//        catch (Exception e) { return null; }
//        //ReferenceType refType = anOR.referenceType();
//        //Field field = refType.fieldByName(name);
//        //if(field!=null) return anOR.getValue(field);
//    }

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

    /**
     * Returns a wrapped lambda expression for given class.
     */
    private Object getWrappedLambdaExpression(Object anOR, JExprLambda lambdaExpr, Class<?> aClass)
    {
        // Get params and content expression
        List<JVarDecl> varDecls = lambdaExpr.getParams();
        JVarDecl param0 = varDecls.size() > 0 ? varDecls.get(0) : null;
        JVarDecl param1 = varDecls.size() > 1 ? varDecls.get(1) : null;
        JExpr contentExpr = lambdaExpr.getExpr();

        // Handle DoubleUnaryOperator
        if (aClass == DoubleUnaryOperator.class) {
            return (DoubleUnaryOperator) d -> {
                _varStack.pushStackFrame();
                _varStack.setStackValueForNode(param0, d);
                try {
                    Object value = evalExpr(anOR, contentExpr);
                    return Convert.doubleValue(value);
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
                finally {
                    _varStack.popStackFrame();
                }
            };
        }

        // Handle DoubleBinaryOperator
        if (aClass == DoubleBinaryOperator.class) {
            return (DoubleBinaryOperator) (x,y) -> {
                _varStack.pushStackFrame();
                _varStack.setStackValueForNode(param0, x);
                _varStack.setStackValueForNode(param1, y);
                try {
                    Object value = evalExpr(anOR, contentExpr);
                    return Convert.doubleValue(value);
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
                finally {
                    _varStack.popStackFrame();
                }
            };
        }

        // Handle EventListener
        if (aClass == EventListener.class) {
            return (EventListener) (e) -> {
                _varStack.pushStackFrame();
                _varStack.setStackValueForNode(param0, e);
                try { evalExpr(anOR, contentExpr); }
                catch (Exception e2) {
                    throw new RuntimeException(e2); }
                finally {
                    _varStack.popStackFrame();
                }
            };
        }

        // Complain
        throw new RuntimeException("JSExprEval.getWrappedLambdaExpr: Unknown lambda class: " + aClass.getName());
    }
}