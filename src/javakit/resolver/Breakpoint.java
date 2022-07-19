package javakit.resolver;
import snap.util.SnapUtils;
import snap.web.WebFile;

import java.util.List;

/**
 * Represents a debugger break point for a project file.
 */
public class Breakpoint implements Comparable<Breakpoint> {

    // The type of breakpoint
    Type _type;

    // The file path
    WebFile _file;

    // The class name
    String _className;

    // Whether class name is wild carded (if Class breakpoint)
    boolean _isWild;

    // The line number
    int _line;

    // The field name (for Watchpoints)
    String _fieldId;

    // The method name (for MethodBreakpoint)
    String _methodName;

    // The method args (for MethodBreakpoint)
    List<String> _methodArgs;

    // Whether to notify caught, uncaught (for Exception)
    boolean _notifyCaught, _notifyUncaught;

    // Whether breakpoint is enabled
    boolean _enabled = true;

    // Constants for type
    public enum Type {LineBreakpoint, MethodBreakpoint, Exception, AccessWatchpoint, ModificationWatchpoint}

    ;

    /**
     * Creates a Breakpoint for project, FilePath and Line.
     */
    protected Breakpoint()
    {
    }

    /**
     * Creates a Breakpoint for File and Line.
     */
    public Breakpoint(WebFile aFile, int aLine)
    {
        _type = Type.LineBreakpoint;
        setFile(aFile);
        setLine(aLine);
        Project proj = Project.get(aFile);
        _className = proj.getClassNameForFile(aFile);
    }

    /**
     * Creates a Breakpoint for class name and Line.
     */
    public Breakpoint(String aClsName, int line)
    {
        _type = Type.LineBreakpoint;
        _className = aClsName;
        _line = line;
        if (_className != null && _className.startsWith("*")) {
            _isWild = true;
            _className = _className.substring(1);
        }
    }

    /**
     * Creates a Breakpoint for class name, method name and args.
     * For example: initMethod("snap.app.App", "main", Collections.singletonList("java.lang.String[]"));
     */
    public Breakpoint(String aClsName, String aMethod, List theArgs)
    {
        _type = Type.MethodBreakpoint;
        _className = aClsName;
        _methodName = aMethod;
        _methodArgs = theArgs;
    }

    /**
     * Initializes a ExceptionIntercept.
     */
    public static Breakpoint getExceptionBreak(String aClsName, boolean notifyCaught, boolean notifyUncaught)
    {
        Breakpoint bp = new Breakpoint();
        bp._type = Type.Exception;
        bp._className = aClsName;
        bp._notifyCaught = notifyCaught;
        bp._notifyUncaught = notifyUncaught;
        return bp;
    }

    /**
     * Initializes a Watchpoint.
     */
    public static Breakpoint getAccessWatchpoint(String aClsName, String fieldId, boolean isAccess)
    {
        Breakpoint bp = new Breakpoint();
        bp._type = isAccess ? Type.AccessWatchpoint : Type.ModificationWatchpoint;
        bp._className = aClsName;
        bp._fieldId = fieldId; //if(!isJavaIdentifier(fieldId)) throw new MalformedMemberNameException(fieldId);
        return bp;
    }

    /**
     * Returns the name.
     */
    public String getName()
    {
        if (_file != null) return _file.getPath(); // + '@' + _lineNumber;
        return _isWild ? "*" + _className : _className;
    }

    /**
     * Returns the type.
     */
    public Type getType()
    {
        return _type;
    }

    /**
     * Returns the file.
     */
    public WebFile getFile()
    {
        return _file;
    }

    /**
     * Sets the file.
     */
    protected void setFile(WebFile aFile)
    {
        _file = aFile;
    }

    /**
     * Returns the file path.
     */
    public String getFilePath()
    {
        return getFile().getPath();
    }

    /**
     * Returns the line index.
     */
    public int getLine()
    {
        return _line;
    }

    /**
     * Sets the line number.
     */
    public void setLine(int aLine)
    {
        _line = aLine;
    }

    /**
     * Returns the line number.
     */
    public int getLineNum()
    {
        return _line + 1;
    }

    /**
     * Returns the class name.
     */
    public String getClassName()
    {
        return _className;
    }

    /**
     * Returns the field name (if field break point).
     */
    public String getFieldName()
    {
        return _fieldId;
    }

    /**
     * Returns the method name (if method break point).
     */
    public String getMethodName()
    {
        return _methodName;
    }

    /**
     * Returns the method args (if method break point).
     */
    public List<String> getMethodArgs()
    {
        return _methodArgs;
    }

    /**
     * Returns whether to notify caught.
     */
    public boolean isNotifyCaught()
    {
        return _notifyCaught;
    }

    /**
     * Returns whether to notify caught.
     */
    public boolean isNotifyUncaught()
    {
        return _notifyUncaught;
    }

    /**
     * Returns whether breakpoint is enabled.
     */
    public boolean isEnabled()
    {
        return _enabled;
    }

    /**
     * Sets whether breakpoint is enabled.
     */
    public void setEnabled(boolean aValue)
    {
        _enabled = aValue;
    }

    /**
     * Returns a descriptor string.
     */
    public String getDescriptor()
    {
        return getFile().getPath() + " [Line: " + getLineNum() + "]";
    }

    /**
     * Standard compare implementation.
     */
    public int compareTo(Breakpoint aBP)
    {
        if (_file != aBP._file) return _file.compareTo(aBP._file);
        return _line < aBP._line ? -1 : _line == aBP._line ? 0 : 1;
    }

    /**
     * Return hash code.
     */
    public int hashCode()
    {
        // Get base
        int base = _file != null ? _file.hashCode() : _className.hashCode();

        // Handle Type LineNumber
        if (getType() == Type.LineBreakpoint)
            return base + _line;

        // Handle Type Method
        if (getType() == Type.MethodBreakpoint)
            return base + (_methodName != null ? _methodName.hashCode() : 0) +
                    (_methodArgs != null ? _methodArgs.hashCode() : 0);

        // Handle Type AccessWatchpoint and ModificationWatchpoint
        if (getType() == Type.AccessWatchpoint || getType() == Type.ModificationWatchpoint)
            return base + _fieldId.hashCode() + getClass().hashCode();

        // Everything else
        return base;
    }

    /**
     * Standard equals implementation.
     */
    public boolean equals(Object anObj)
    {
        // Check identity and get other
        if (anObj == this) return true;
        if (!(anObj instanceof Breakpoint)) return false;
        Breakpoint other = (Breakpoint) anObj;

        // Check Type, SourceName, ClassName
        if (other._type != _type) return false;
        if (!SnapUtils.equals(other._file, _file)) return false;
        if (!SnapUtils.equals(other._className, _className)) return false;

        // Handle Type LineNumber
        if (getType() == Type.LineBreakpoint)
            return other._line == _line;

        // Handle Type Method
        if (getType() == Type.MethodBreakpoint)
            return other._methodName.equals(_methodName) && other._methodArgs.equals(_methodArgs);

        // Handle Type AccessWatchpoint and ModificationWatchpoint
        if (getType() == Type.AccessWatchpoint || getType() == Type.ModificationWatchpoint)
            return other._fieldId.equals(_fieldId);

        // Return true
        return true;
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        // Handle Type LineNumber
        if (getType() == Type.LineBreakpoint)
            return String.format("breakpoint %s:%d", getName(), getLineNum());

        // Handle Type Method
        if (getType() == Type.MethodBreakpoint) {
            StringBuffer sb = new StringBuffer("breakpoint ");
            sb.append(getName()).append('.').append(_methodName);
            if (_methodArgs != null) {
                boolean first = true;
                sb.append('(');
                for (String arg : _methodArgs) {
                    if (!first) sb.append(',');
                    first = false;
                    sb.append(arg);
                }
                sb.append(")");
            }
            return sb.toString();
        }

        // Handle Exception
        if (getType() == Type.Exception)
            return "Exception catch " + getName();

        // Handle Watchpoint
        if (getType() == Type.AccessWatchpoint || getType() == Type.ModificationWatchpoint)
            return "Watchpoint: " + _fieldId;

        // Unknown
        return "Unknown Event request type " + getType();
    }

}