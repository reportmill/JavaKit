package javakit.runner;
import javakit.parse.JExprMath;
import snap.util.SnapUtils;

/**
 * Utility methods for JSExprEval.
 */
public class JSExprEvalUtils {

    /**
     * Add two values.
     */
    protected static Object add(Object aVal1, Object aVal2)
    {
        // Handle Number, Character
        if (isNumberOrChar(aVal1) && isNumberOrChar(aVal2)) {
            double val1 = doubleValue(aVal1);
            double val2 = doubleValue(aVal2);
            double result = val1 + val2;
            return value(result, aVal1, aVal2);
        }

        // Handle strings
        if (isString(aVal1) || isString(aVal2))
            return mirrorOf(toString(aVal1) + toString(aVal2));

        // Complain
        throw new RuntimeException("Can't add types " + aVal1 + " + " + aVal2);
    }

    /**
     * Subtract two values.
     */
    protected static Object subtract(Object aVal1, Object aVal2)
    {
        // Handle Number, Character
        if (isNumberOrChar(aVal1) && isNumberOrChar(aVal2)) {
            double val1 = doubleValue(aVal1);
            double val2 = doubleValue(aVal2);
            double result = val1 - val2;
            return value(result, aVal1, aVal2);
        }

        // Complain
        throw new RuntimeException("Can't subtract types " + aVal1 + " + " + aVal2);
    }

    /**
     * Multiply two values.
     */
    protected static Object multiply(Object aVal1, Object aVal2)
    {
        // Handle Number, Character
        if (isNumberOrChar(aVal1) && isNumberOrChar(aVal2)) {
            double val1 = doubleValue(aVal1);
            double val2 = doubleValue(aVal2);
            double result = val1 * val2;
            return value(result, aVal1, aVal2);
        }

        // Complain
        throw new RuntimeException("Can't multiply types " + aVal1 + " + " + aVal2);
    }

    /**
     * Divide two values.
     */
    protected static Object divide(Object aVal1, Object aVal2)
    {
        // Handle Number, Character
        if (isNumberOrChar(aVal1) && isNumberOrChar(aVal2)) {
            double val1 = doubleValue(aVal1);
            double val2 = doubleValue(aVal2);
            double result = val1 / val2;
            return value(result, aVal1, aVal2);
        }

        // Complain
        throw new RuntimeException("Can't divide types " + aVal1 + " + " + aVal2);
    }

    /**
     * Mod two values.
     */
    protected static Object mod(Object aVal1, Object aVal2)
    {
        // Handle Number, Character
        if (isNumberOrChar(aVal1) && isNumberOrChar(aVal2)) {
            long val1 = longValue(aVal1);
            long val2 = longValue(aVal2);
            double result = val1 % val2;
            return value(result, aVal1, aVal2);
        }

        // Complain
        throw new RuntimeException("Can't mod types " + aVal1 + " + " + aVal2);
    }

    /**
     * Compare two numeric values.
     */
    protected static Object compareEquals(Object aVal1, Object aVal2, JExprMath.Op anOp)
    {
        // Handle Number, Character
        if (isNumberOrChar(aVal1) && isNumberOrChar(aVal2)) {
            double v1 = doubleValue(aVal1);
            double v2 = doubleValue(aVal2);
            boolean val = compareNumeric(v1, v2, anOp);
            return mirrorOf(val);
        }

        // Handle anything
        if (anOp == JExprMath.Op.Equal)
            return aVal1 == aVal2;
        if (anOp == JExprMath.Op.NotEqual)
            return aVal1 != aVal2;

        // Complain
        throw new RuntimeException("Invalid equals op " + anOp);
    }

    /**
     * Compare two numeric values.
     */
    protected static Object compareNumeric(Object aVal1, Object aVal2, JExprMath.Op anOp)
    {
        // Handle Number, Character
        if (isNumberOrChar(aVal1) && isNumberOrChar(aVal2)) {
            double v1 = doubleValue(aVal1);
            double v2 = doubleValue(aVal2);
            boolean val = compareNumeric(v1, v2, anOp);
            return mirrorOf(val);
        }

        // Complain
        throw new RuntimeException("Can't numeric compare types " + aVal1 + " + " + aVal2);
    }

    /**
     * Compare two numeric values.
     */
    protected static boolean compareNumeric(double aVal1, double aVal2, JExprMath.Op anOp)
    {
        switch (anOp) {
            case Equal: return aVal1 == aVal2;
            case NotEqual: return aVal1 != aVal2;
            case LessThan: return aVal1 < aVal2;
            case GreaterThan: return aVal1 > aVal2;
            case LessThanOrEqual: return aVal1 <= aVal2;
            case GreaterThanOrEqual: return aVal1 >= aVal2;
            default: throw new RuntimeException("Not a compare op " + anOp);
        }
    }

    /**
     * Compare two boolean values.
     */
    protected static Object compareLogical(Object aVal1, Object aVal2, JExprMath.Op anOp)
    {
        // Handle boolean
        if (isBoolean(aVal1) && isBoolean(aVal2)) {
            boolean v1 = boolValue(aVal1);
            boolean v2 = boolValue(aVal2);
            boolean val = compareLogical(v1, v2, anOp);
            return mirrorOf(val);
        }

        // Handle unsupported value types
        throw new RuntimeException("Can't logical compare types " + aVal1 + " + " + aVal2);
    }

    /**
     * Compare two values.
     */
    protected static boolean compareLogical(boolean aVal1, boolean aVal2, JExprMath.Op anOp)
    {
        if (anOp == JExprMath.Op.And)
            return aVal1 && aVal2;
        if (anOp == JExprMath.Op.Or)
            return aVal1 || aVal2;

        // Handle unsupported value types
        throw new RuntimeException("Not a compare op " + anOp);
    }

    /**
     * Return value of appropriate type for given number and original two values.
     */
    protected static Object value(double aValue, Object aVal1, Object aVal2)
    {
        if (isDouble(aVal1) || isDouble(aVal2))
            return mirrorOf(aValue);
        if (isFloat(aVal1) || isFloat(aVal2))
            return mirrorOf((float) aValue);
        if (isLong(aVal1) || isLong(aVal2))
            return mirrorOf((long) aValue);
        if (isInt(aVal1) || isInt(aVal2))
            return mirrorOf((int) aValue);

        // Handle unsupported value types
        throw new RuntimeException("Can't discern value type for " + aVal1 + " and " + aVal2);
    }

    /**
     * Return whether object is boolean.
     */
    protected static boolean isBoolean(Object anObj)  { return anObj instanceof Boolean; }

    /**
     * Returns whether object is Number or Character.
     */
    protected static boolean isNumberOrChar(Object anObj)  { return anObj instanceof Number || anObj instanceof Character; }

    /**
     * Return whether object is int.
     */
    protected static boolean isInt(Object anObj)  { return anObj instanceof Integer; }

    /**
     * Return whether object is long.
     */
    protected static boolean isLong(Object anObj)  { return anObj instanceof Long; }

    /**
     * Return whether object is float.
     */
    protected static boolean isFloat(Object anObj)  { return anObj instanceof Float; }

    /**
     * Return whether object is double.
     */
    protected static boolean isDouble(Object anObj)  { return anObj instanceof Double; }

    /**
     * Return whether object is String.
     */
    protected static boolean isString(Object anObj)  { return anObj instanceof String; }

    /**
     * Returns whether object is array value.
     */
    protected static boolean isArray(Object anObj)  { return anObj != null && anObj.getClass().isArray(); }

    /**
     * Returns the boolean value.
     */
    protected static boolean boolValue(Object anObj)
    {
        return SnapUtils.boolValue(anObj);
    }

    /**
     * Returns the int value.
     */
    protected static int intValue(Object anObj)
    {
        return SnapUtils.intValue(anObj);
    }

    /**
     * Returns the long value.
     */
    protected static long longValue(Object anObj)
    {
        return SnapUtils.longValue(anObj);
    }

    /**
     * Returns the float value.
     */
    protected static float floatValue(Object anObj)
    {
        return (float) doubleValue(anObj);
    }

    /**
     * Returns the double value.
     */
    protected static double doubleValue(Object anObj)
    {
        // Handle Number
        if (anObj instanceof Number)
            return ((Number) anObj).doubleValue();

        // Handle Character
        if (anObj instanceof Character)
            return (char) (Character) anObj;

        // Handle other
        return 0;
    }

    /**
     * Returns a value cast or converted to given primitive class.
     */
    protected static Object castOrConvertValueToPrimitiveClass(Object aValue, Class<?> aClass)
    {
        if (aClass == double.class)
            return SnapUtils.doubleValue(aValue);
        if (aClass == float.class)
            return SnapUtils.floatValue(aValue);
        if (aClass == int.class)
            return SnapUtils.intValue(aValue);
        if (aClass == long.class)
            return SnapUtils.longValue(aValue);
        if (aClass == short.class)
            return (short) SnapUtils.intValue(aValue);
        if (aClass == byte.class)
            return (byte) SnapUtils.intValue(aValue);
        if (aClass == boolean.class)
            return SnapUtils.boolValue(aValue);
        if (aClass == char.class)
            return (char) SnapUtils.intValue(aValue);
        return aValue;
    }

    /**
     * Return the current this object.
     */
    protected static Object mirrorOf(Object anObj)  { return anObj; }

    /**
     * Return the current this object.
     */
    protected static String toString(Object anObj)  { return anObj != null ? anObj.toString() : null;  }
}
