/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.text;
import java.util.*;
import javakit.parse.JClassDecl;
import javakit.parse.JMemberDecl;
import javakit.reflect.JavaExecutable;
import snap.geom.*;
import snap.gfx.*;
import snap.text.*;
import javakit.resolver.*;
import snap.util.ArrayUtils;
import snap.view.*;

/**
 * A component to paint row markers.
 */
public class RowHeader extends View {

    // The JavaTextPane that contains this RowHeader
    private JavaTextPane  _textPane;

    // The JavaTextArea
    private JavaTextArea  _textArea;

    // The list of markers
    private Marker<?>[] _markers;

    // The last mouse moved position
    private double  _mx, _my;

    // Width of this component
    public static final int WIDTH = 12;

    // The marker images for Error, Warning, Breakpoint, Implements, Override
    static Image _errorImage = Image.get(JavaTextBox.class, "ErrorMarker.png");
    static Image _warningImage = Image.get(JavaTextBox.class, "WarningMarker.png");
    static Image _breakpointImage = Image.get(JavaTextBox.class, "Breakpoint.png");
    static Image _implImage = Image.get(JavaTextBox.class, "ImplementsMarker.png");
    static Image _overImage = Image.get(JavaTextBox.class, "pkg.images/OverrideMarker.png");

    /**
     * Creates a new RowHeader.
     */
    public RowHeader(JavaTextPane aJTP)
    {
        // Set ivars
        _textPane = aJTP;
        _textArea = aJTP.getTextArea();

        // Config
        enableEvents(MouseMove, MouseRelease);
        setToolTipEnabled(true);
        setFill(new Color(233, 233, 233));
        setPrefWidth(WIDTH);
    }

    /**
     * Returns the JavaTextArea.
     */
    public JavaTextPane getTextPane()
    {
        return _textPane;
    }

    /**
     * Returns the JavaTextArea.
     */
    public JavaTextArea getTextArea()  { return _textArea; }

    /**
     * Sets the JavaTextArea selection.
     */
    public void setTextSelection(int aStart, int anEnd)
    {
        _textArea.setSel(aStart, anEnd);
    }

    /**
     * Returns the markers.
     */
    public Marker<?>[] getMarkers()
    {
        // If already set, just return
        if (_markers != null) return _markers;

        // Get, set, return
        Marker<?>[] markers = createMarkers();
        return _markers = markers;
    }

    /**
     * Returns the list of markers.
     */
    protected Marker<?>[] createMarkers()
    {
        // Create list
        List<Marker<?>> markers = new ArrayList<>();

        // Add markers for member Overrides/Implements
        JClassDecl cd = _textArea.getJFile().getClassDecl();
        if (cd != null)
            getSuperMemberMarkers(cd, markers);

        // Add markers for BuildIssues
        BuildIssue[] buildIssues = _textArea.getBuildIssues();
        for (BuildIssue issue : buildIssues)
            if (issue.getEnd() < _textArea.length())
                markers.add(new BuildIssueMarker(issue));

        // Add markers for breakpoints
        List<Breakpoint> breakpointsList = _textArea.getBreakpoints();
        if (breakpointsList != null) {
            for (Breakpoint bp : breakpointsList) {
                if (bp.getLine() < _textArea.getLineCount())
                    markers.add(new BreakpointMarker(bp));
                else _textArea.removeBreakpoint(bp);
            }
        }

        // Return markers
        return markers.toArray(new Marker[0]);
    }

    /**
     * Loads a list of SuperMemberMarkers for a class declaration (recursing for inner classes).
     */
    private void getSuperMemberMarkers(JClassDecl aCD, List<Marker<?>> theMarkers)
    {
        for (JMemberDecl md : aCD.getMemberDecls()) {
            if (md.getSuperDecl() != null && md.getEnd() < _textArea.length())
                theMarkers.add(new SuperMemberMarker(md));
            if (md instanceof JClassDecl)
                getSuperMemberMarkers((JClassDecl) md, theMarkers);
        }
    }

    /**
     * Override to reset markers.
     */
    protected void resetAll()
    {
        _markers = null;
        repaint();
    }

    /**
     * Handle events.
     */
    protected void processEvent(ViewEvent anEvent)
    {
        // Handle MouseClick
        if (anEvent.isMouseClick()) {

            // Get reversed markers (so click effects top marker)
            Marker<?>[] markers = getMarkers().clone();
            ArrayUtils.reverse(markers);
            double x = anEvent.getX(), y = anEvent.getY();

            // Handle double click
            if (anEvent.getClickCount() == 2) {
                for (Marker<?> marker : markers) {
                    if (marker.contains(x, y) && marker instanceof BreakpointMarker) {
                        marker.mouseClicked(anEvent);
                        return;
                    }
                }
                TextBoxLine line = _textArea.getTextBox().getLineForY(anEvent.getY());
                int lineIndex = line.getIndex();
                _textArea.addBreakpoint(lineIndex);
                resetAll();
                return;
            }

            // Handle normal click
            for (Marker<?> marker : markers) {
                if (marker.contains(x, y)) {
                    marker.mouseClicked(anEvent);
                    return;
                }
            }
        }

        // Handle MouseMoved
        else if (anEvent.isMouseMove()) {
            _mx = anEvent.getX();
            _my = anEvent.getY();
            for (Marker<?> marker : getMarkers()) {
                if (marker.contains(_mx, _my)) {
                    setCursor(Cursor.HAND);
                    return;
                }
            }
            setCursor(Cursor.DEFAULT);
        }
    }

    /**
     * Paint markers.
     */
    protected void paintFront(Painter aPntr)
    {
        aPntr.setStroke(Stroke.Stroke1);
        for (Marker<?> m : getMarkers())
            aPntr.drawImage(m._image, m.x, m.y);
    }

    /**
     * Override to return tool tip text.
     */
    public String getToolTip(ViewEvent anEvent)
    {
        for (Marker<?> marker : getMarkers())
            if (marker.contains(_mx, _my))
                return marker.getToolTip();
        return null;
    }

    /**
     * The class that describes a overview marker.
     */
    public abstract static class Marker<T> extends Rect {

        /**
         * The object that is being marked.
         */
        T _target;

        // The image
        Image _image;

        /**
         * Creates a new marker for target.
         */
        public Marker(T aTarget)
        {
            _target = aTarget;
            setRect(-2, 0, WIDTH, WIDTH);
        }

        /**
         * Returns a tooltip.
         */
        public abstract String getToolTip();

        /**
         * Handles MouseClick.
         */
        public abstract void mouseClicked(ViewEvent anEvent);
    }

    /**
     * A Marker for super members.
     */
    public class SuperMemberMarker extends Marker<JMemberDecl> {

        JavaExecutable _superDecl;
        boolean _interface;

        /**
         * Creates a new marker for target.
         */
        public SuperMemberMarker(JMemberDecl aTarget)
        {
            super(aTarget);
            _superDecl = aTarget.getSuperDecl();
            _interface = aTarget.isSuperDeclInterface();
            TextBoxLine line = _textArea.getLine(aTarget.getLineIndex());
            setY(Math.round(line.getY()));
            _image = isInterface() ? _implImage : _overImage;
        }

        /**
         * Returns whether is interface.
         */
        public boolean isInterface()
        {
            return _interface;
        }

        /**
         * Returns a tooltip.
         */
        public String getToolTip()
        {
            String cname = _superDecl.getDeclaringClassName();
            return (isInterface() ? "Implements " : "Overrides ") + cname + '.' + _target.getName();
        }

        /**
         * Handles MouseClick.
         */
        public void mouseClicked(ViewEvent anEvent)
        {
            _textPane.openSuperDeclaration(_target);
        }
    }

    /**
     * A Marker subclass for BuildIssues.
     */
    public class BuildIssueMarker extends Marker<BuildIssue> {

        // Whether issue is error
        boolean _isError;

        /**
         * Creates a new marker for target.
         */
        public BuildIssueMarker(BuildIssue aTarget)
        {
            super(aTarget);
            _isError = aTarget.isError();
            TextBoxLine line = _textArea.getLineAt(aTarget.getEnd());
            setY(Math.round(line.getY()));
            _image = _isError ? _errorImage : _warningImage;
        }

        /**
         * Returns a tooltip.
         */
        public String getToolTip()
        {
            return _target.getText();
        }

        /**
         * Handles MouseClick.
         */
        public void mouseClicked(ViewEvent anEvent)
        {
            setTextSelection(_target.getStart(), _target.getEnd());
        }
    }

    /**
     * A Marker subclass for Breakpoints.
     */
    public class BreakpointMarker extends Marker<Breakpoint> {

        /**
         * Creates a BreakpointMarker.
         */
        public BreakpointMarker(Breakpoint aBP)
        {
            super(aBP);
            TextBoxLine line = _textArea.getLine(aBP.getLine());
            setY(Math.round(line.getY()));
            _image = _breakpointImage;
        }

        /**
         * Returns a tooltip.
         */
        public String getToolTip()
        {
            return _target.toString();
        }

        /**
         * Handles MouseClick.
         */
        public void mouseClicked(ViewEvent anEvent)
        {
            if (anEvent.getClickCount() == 2)
                _textArea.removeBreakpoint(_target);
            resetAll();
        }
    }
}