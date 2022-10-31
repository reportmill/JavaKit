/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.runner;
import snap.util.SnapUtils;
import java.lang.reflect.Array;
import java.util.*;
import static javakit.runner.JSExprEvalUtils.castOrConvertValueToPrimitiveClass;

/**
 * A class to manage variables in a running interpreter session.
 */
public class JSVarStack {

    // A map of local variables
    private Map<String,Object> _locals = new HashMap<>();

    // The stack of frames
    private Map<String,Object>[]  _frames = new Map[100];

    // The number of frames
    private int  _frameCount;

    /**
     * Constructor.
     */
    public JSVarStack()
    {
        _frames[_frameCount++] = _locals;
    }

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
        // Look at current stack frame
        Object value = _locals.get(aName);
        if (value != null)
            return value;

        // Look at previous frames
        for (int i = _frameCount - 2; i >= 0; i--) {
            value = _frames[i].get(aName);
            if (value != null)
                return value;
        }

        // Return not found
        return null;

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
     * Push a stack frame.
     */
    public void pushStackFrame()
    {
        pushStackFrame(new HashMap<>(_locals));
    }

    /**
     * Push a stack frame.
     */
    public void pushStackFrame(Map<String,Object> aFrame)
    {
        _locals = aFrame;
        _frames[_frameCount++] = _locals;
    }

    /**
     * Pops a stack frame.
     */
    public void popStackFrame()
    {
        _locals = _frames[--_frameCount - 1];
    }

    /**
     * Creates a new frame.
     */
    public Map<String,Object> newFrame()  { return new HashMap<>(); }
}
