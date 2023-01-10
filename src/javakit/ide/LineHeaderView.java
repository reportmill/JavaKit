/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.ide;
import java.util.*;

import javakit.parse.JClassDecl;
import javakit.parse.JMemberDecl;
import javakit.resolver.JavaExecutable;
import snap.geom.*;
import snap.gfx.*;
import snap.text.*;
import snap.util.ArrayUtils;
import snap.view.*;

/**
 * A component to paint row markers.
 */
public class LineHeaderView extends View {

    // The JavaTextPane that contains this RowHeader
    private JavaTextPane  _textPane;

    // The TextArea
    private TextArea  _textArea;

    // The Java TextArea
    private JavaTextArea  _javaTextArea;

    // Whether to show line numbers
    private boolean  _showLineNumbers = true;

    // Whether to show line markers
    private boolean  _showLineMarkers = true;

    // The list of markers
    private Marker<?>[]  _markers;

    // The last mouse moved position
    private double  _mx, _my;

    // Constants
    private static Color BACKGROUND_FILL = new Color(.98);
    private static Color LINE_NUMBERS_COLOR = Color.GRAY5;

    // Constants for Markers width
    public static final int LINE_MARKERS_WIDTH = 12;

    // The marker images for Error, Warning, Breakpoint, Implements, Override
    static Image _errorImage = Image.get(JavaTextUtils.class, "ErrorMarker.png");
    static Image _warningImage = Image.get(JavaTextUtils.class, "WarningMarker.png");
    static Image _breakpointImage = Image.get(JavaTextUtils.class, "Breakpoint.png");
    static Image _implImage = Image.get(JavaTextUtils.class, "ImplementsMarker.png");
    static Image _overImage = Image.get(JavaTextUtils.class, "OverrideMarker.png");

    /**
     * Creates a new RowHeader.
     */
    public LineHeaderView(JavaTextPane aJTP, TextArea aTextArea)
    {
        // Set ivars
        _textPane = aJTP;
        _textArea = aTextArea;
        _javaTextArea = aTextArea instanceof JavaTextArea ? (JavaTextArea) aTextArea : null;

        // Config
        enableEvents(MouseMove, MouseRelease);
        setToolTipEnabled(true);
        setFill(BACKGROUND_FILL);

        // Set PrefWidth
        setPrefWidth(getSuggestedPrefWidth());
    }

    /**
     * Returns whether to show line numbers.
     */
    public boolean isShowLineNumbers()  { return _showLineNumbers; }

    /**
     * Sets whether to show line numbers.
     */
    public void setShowLineNumbers(boolean aValue)
    {
        if (aValue == isShowLineNumbers()) return;
        _showLineNumbers = aValue;

        // Adjust PrefWidth
        setPrefWidth(getSuggestedPrefWidth());
    }

    /**
     * Returns whether to show line markers.
     */
    public boolean isShowLineMarkers()  { return _showLineMarkers; }

    /**
     * Sets whether to show line markers.
     */
    public void setShowLineMarkers(boolean aValue)
    {
        if (aValue == isShowLineMarkers()) return;
        _showLineMarkers = aValue;

        // Adjust PrefWidth
        setPrefWidth(getSuggestedPrefWidth());
    }

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
        // If no JavaTextArea, just return
        if (_javaTextArea == null) return new Marker[0];

        // Create list
        List<Marker<?>> markers = new ArrayList<>();

        // Add markers for member Overrides/Implements
        JClassDecl cd = _javaTextArea.getJFile().getClassDecl();
        if (cd != null)
            getSuperMemberMarkers(cd, markers);

        // Add markers for BuildIssues
        BuildIssue[] buildIssues = _javaTextArea.getBuildIssues();
        for (BuildIssue issue : buildIssues)
            if (issue.getEnd() < _textArea.length())
                markers.add(new BuildIssueMarker(issue));

        // Add markers for breakpoints
        List<Breakpoint> breakpointsList = _javaTextArea.getBreakpoints();
        if (breakpointsList != null) {
            for (Breakpoint bp : breakpointsList) {
                if (bp.getLine() < _textArea.getLineCount())
                    markers.add(new BreakpointMarker(bp));
                else _javaTextArea.removeBreakpoint(bp);
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
            if (md.getSuperDecl() != null && md.getEndCharIndex() < _textArea.length())
                theMarkers.add(new SuperMemberMarker(md));
            if (md instanceof JClassDecl)
                getSuperMemberMarkers((JClassDecl) md, theMarkers);
        }
    }

    /**
     * Override to reset markers.
     */
    public void resetAll()
    {
        _markers = null;
        double prefW = getSuggestedPrefWidth();
        double prefH = _textArea.getPrefHeight();
        setPrefSize(prefW, prefH);
        repaint();
    }

    /**
     * Calculates pref width based on TextArea font size and ShowLineNumbers, ShowLineMarkers.
     */
    private double getSuggestedPrefWidth()
    {
        double prefW = 0;

        // Add width for line numbers
        if (_showLineNumbers) {
            Font font = _textArea.getFont();
            int lineCount = _textArea.getLineCount();
            double colCount = Math.ceil(Math.log10(lineCount) + .0001);
            double charWidth = Math.ceil(font.charAdvance('0'));
            double colsWidth = colCount * charWidth;
            double PADDING = 10;
            prefW += colsWidth + PADDING;
        }

        // Add Width for line markers
        if (_showLineMarkers) {
            prefW += LINE_MARKERS_WIDTH;
        }

        // Return
        return prefW;
    }

    /**
     * Handle events.
     */
    protected void processEvent(ViewEvent anEvent)
    {
        if (_javaTextArea == null) return;

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
                _javaTextArea.addBreakpoint(lineIndex);
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

        if (isShowLineNumbers())
            paintLineNumbers(aPntr);
    }

    /**
     * Paint line numbers.
     */
    protected void paintLineNumbers(Painter aPntr)
    {
        // Get/set Font and TextColor
        Font font = _textArea.getDefaultStyle().getFont();
        aPntr.setFont(font);
        aPntr.setColor(LINE_NUMBERS_COLOR);

        // Get current Painter.ClipBounds to restrict painted line numbers
        Rect clipRect = aPntr.getClipBounds();
        double clipY = Math.max(clipRect.y, 0);
        double clipMaxY = clipRect.getMaxY();

        // Get start line index for ClipY
        TextBoxLine startLine = _textArea.getTextBox().getLineForY(clipY);
        int startLineIndex = startLine.getIndex();
        int lineCount = _textArea.getLineCount();
        int PADDING = 6;
        double maxX = getWidth() - PADDING;

        // Iterate over lines and paint line number for each
        for (int i = startLineIndex; i < lineCount; i++) {

            // Get lineY (baseline)
            TextBoxLine textLine = _textArea.getLine(i);
            double lineY = textLine.getY() + textLine.getAscent();

            // Get String, Width and X
            String str = String.valueOf(i + 1);
            double strW = font.getStringAdvance(str);
            double strX = maxX - strW;
            aPntr.drawString(String.valueOf(i+1), strX, lineY);

            // If below clip, just return
            if (lineY > clipMaxY)
                return;
        }
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

        // The object that is being marked.
        protected T  _target;

        // The image
        protected Image  _image;

        /**
         * Creates a new marker for target.
         */
        public Marker(T aTarget)
        {
            _target = aTarget;
            setRect(-2, 0, LINE_MARKERS_WIDTH, LINE_MARKERS_WIDTH);
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

        // Ivars
        private JavaExecutable  _superDecl;
        private boolean  _interface;

        /**
         * Creates a new marker for target.
         */
        public SuperMemberMarker(JMemberDecl aTarget)
        {
            super(aTarget);
            _superDecl = aTarget.getSuperDecl();
            _interface = aTarget.isSuperDeclInterface();

            // Get/set Y from line
            int lineIndex = aTarget.getLineIndex();
            TextBoxLine textLine = _textArea.getLine(lineIndex);
            setY(Math.round(textLine.getY()));

            // Set image
            _image = isInterface() ? _implImage : _overImage;
        }

        /**
         * Returns whether is interface.
         */
        public boolean isInterface()  { return _interface;  }

        /**
         * Returns a tooltip.
         */
        public String getToolTip()
        {
            String className = _superDecl.getDeclaringClassName();
            return (isInterface() ? "Implements " : "Overrides ") + className + '.' + _target.getName();
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
        private boolean  _isError;

        /**
         * Creates a new marker for target.
         */
        public BuildIssueMarker(BuildIssue aTarget)
        {
            super(aTarget);
            _isError = aTarget.isError();

            // Get/set Y from line
            int charIndex = aTarget.getEnd();
            TextBoxLine textLine = _textArea.getLineForCharIndex(charIndex);
            setY(Math.round(textLine.getY()));

            // Set image
            _image = _isError ? _errorImage : _warningImage;
        }

        /**
         * Returns a tooltip.
         */
        public String getToolTip()  { return _target.getText(); }

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
        public String getToolTip()  { return _target.toString(); }

        /**
         * Handles MouseClick.
         */
        public void mouseClicked(ViewEvent anEvent)
        {
            if (anEvent.getClickCount() == 2)
                _javaTextArea.removeBreakpoint(_target);
            resetAll();
        }
    }
}