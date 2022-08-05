package javakit.shell;
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
        // Handle strings
        if (isString(aVal1) || isString(aVal2))
            return mirrorOf(toString(aVal1) + toString(aVal2));

        // Handle primitives
        if (isPrimitive(aVal1) && isPrimitive(aVal2)) {
            double result = doubleValue(aVal1) + doubleValue(aVal2);
            return value(result, aVal1, aVal2);
        }

        // Complain
        throw new RuntimeException("Can't add types " + aVal1 + " + " + aVal2);
    }

    /**
     * Subtract two values.
     */
    protected static Object subtract(Object aVal1, Object aVal2)
    {
        // Handle primitives
        if (isPrimitive(aVal1) && isPrimitive(aVal2)) {
            double result = doubleValue(aVal1) - doubleValue(aVal2);
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
        // Handle primitives
        if (isPrimitive(aVal1) && isPrimitive(aVal2)) {
            double result = doubleValue(aVal1) * doubleValue(aVal2);
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
        // Handle primitives
        if (isPrimitive(aVal1) && isPrimitive(aVal2)) {
            double result = doubleValue(aVal1) / doubleValue(aVal2);
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
        // Handle primitives
        if (isPrimitive(aVal1) && isPrimitive(aVal2)) {
            double result = longValue(aVal1) % longValue(aVal2);
            return value(result, aVal1, aVal2);
        }

        // Complain
        throw new RuntimeException("Can't mod types " + aVal1 + " + " + aVal2);
    }

    /**
     * Compare two numeric values.
     */
    protected static Object compareNumeric(Object aVal1, Object aVal2, JExprMath.Op anOp)
    {
        // Handle primitives
        if (isPrimitive(aVal1) && isPrimitive(aVal2)) {
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
        if (isPrimitive(aVal1) && isPrimitive(aVal2)) {
            boolean v1 = boolValue(aVal1), v2 = boolValue(aVal2);
            boolean val = compareLogical(v1, v2, anOp);
            return mirrorOf(val);
        }
        throw new RuntimeException("Can't logical compare types " + aVal1 + " + " + aVal2);
    }

    /**
     * Compare two values.
     */
    protected static boolean compareLogical(boolean aVal1, boolean aVal2, JExprMath.Op anOp)
    {
        if (anOp == JExprMath.Op.And) return aVal1 && aVal2;
        if (anOp == JExprMath.Op.Or) return aVal1 && aVal2;
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
        throw new RuntimeException("Can't discern value type for " + aVal1 + " and " + aVal2);
    }

    /**
     * Return whether object is primitive.
     */
    protected static boolean isPrimitive(Object anObj)
    {
        return isInt(anObj) || isLong(anObj) || isFloat(anObj) || isDouble(anObj);
    }

    /**
     * Return whether object is boolean.
     */
    protected static boolean isBoolean(Object anObj)  { return anObj instanceof Boolean; }

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
        return SnapUtils.floatValue(anObj);
    }

    /**
     * Returns the double value.
     */
    protected static double doubleValue(Object anObj)
    {
        return SnapUtils.doubleValue(anObj);
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
