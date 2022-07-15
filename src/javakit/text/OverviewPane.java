/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.text;

import java.util.*;

import snap.geom.*;
import snap.gfx.*;
import snap.text.*;
import javakit.resolver.BuildIssue;
import snap.view.*;

/**
 * A component to show locations of Errors, warnings, selected symbols, etc.
 */
public class OverviewPane extends View {

    // The JavaTextArea
    JavaTextArea _textArea;

    // The list of markers
    List<Marker> _markers;

    // The last mouse point
    double _mx, _my;

    // Colors
    static final Color _error = new Color(236, 175, 205), _errorBorder = new Color(248, 50, 147);
    static final Color _warning = new Color(252, 240, 203), _warningBorder = new Color(246, 209, 95);

    /**
     * Creates a new OverviewPane.
     */
    public OverviewPane()
    {
        enableEvents(MouseMove, MouseRelease);
        setToolTipEnabled(true);
        setPrefWidth(14);
    }

    /**
     * Returns the JavaTextArea.
     */
    public JavaTextArea getTextArea()
    {
        return _textArea;
    }

    /**
     * Sets the JavaTextArea.
     */
    public void setTextArea(JavaTextArea aJTA)
    {
        _textArea = aJTA;
    }

    /**
     * Sets the JavaTextArea selection.
     */
    public void setTextSel(int aStart, int anEnd)
    {
        _textArea._textPane.setTextSel(aStart, anEnd);
    }

    /**
     * Returns the list of markers.
     */
    public List<Marker> getMarkers()
    {
        return _markers != null ? _markers : (_markers = createMarkers());
    }

    /**
     * Returns the list of markers.
     */
    protected List<Marker> createMarkers()
    {
        // Create list
        List<Marker> markers = new ArrayList();

        // Add markers for TextArea.JavaSource.BuildIssues
        BuildIssue buildIssues[] = _textArea.getBuildIssues();
        for (BuildIssue issue : buildIssues)
            markers.add(new BuildIssueMarker(issue));

        // Add markers for TextArea.SelectedTokens
        List<TextBoxToken> tokens = _textArea.getSelectedTokens();
        for (TextBoxToken token : tokens)
            markers.add(new TokenMarker(token));

        // Return markers
        return markers;
    }

    /**
     * Override to reset marker nodes.
     */
    protected void resetAll()
    {
        _markers = null;
        repaint();
    }

    /**
     * Called on mouse click to select marker line.
     */
    protected void processEvent(ViewEvent anEvent)
    {
        // Handle MosueClicked
        if (anEvent.isMouseClick()) {
            for (Marker m : getMarkers()) {
                if (m.contains(anEvent.getX(), anEvent.getY())) {
                    setTextSel(m.getSelStart(), m.getSelEnd());
                    return;
                }
            }
            TextBoxLine line = _textArea.getTextBox().getLineForY(anEvent.getY() / getHeight() * _textArea.getHeight());
            setTextSel(line.getStart(), line.getEnd());
        }

        // Handle MouseMoved
        if (anEvent.isMouseMove()) {
            _mx = anEvent.getX();
            _my = anEvent.getY();
            for (Marker m : getMarkers())
                if (m.contains(_mx, _my)) {
                    setCursor(Cursor.HAND);
                    return;
                }
            setCursor(Cursor.DEFAULT);
        }
    }

    /**
     * Paint markers.
     */
    protected void paintFront(Painter aPntr)
    {
        double th = _textArea.getHeight(), h = Math.min(getHeight(), th);
        aPntr.setStroke(Stroke.Stroke1);
        for (Marker m : getMarkers()) {
            m.setY(m._y / th * h);
            aPntr.setPaint(m.getColor());
            aPntr.fill(m);
            aPntr.setPaint(m.getStrokeColor());
            aPntr.draw(m);
        }
    }

    /**
     * Override to return tool tip text.
     */
    public String getToolTip(ViewEvent anEvent)
    {
        for (Marker marker : getMarkers())
            if (marker.contains(_mx, _my))
                return marker.getToolTip();
        TextBoxLine line = _textArea.getTextBox().getLineForY(_my / getHeight() * _textArea.getHeight());
        return "Line: " + (line.getIndex() + 1);
    }

    /**
     * The class that describes a overview marker.
     */
    public abstract static class Marker<T> extends Rect {

        /**
         * The object that is being marked.
         */
        T _target;
        double _y;

        /**
         * Creates a new marker for target.
         */
        public Marker(T aTarget)
        {
            _target = aTarget;
            setRect(2, 0, 9, 5);
        }

        /**
         * Returns the color.
         */
        public abstract Color getColor();

        /**
         * Returns the stroke color.
         */
        public abstract Color getStrokeColor();

        /**
         * Returns the selection start.
         */
        public abstract int getSelStart();

        /**
         * Returns the selection start.
         */
        public abstract int getSelEnd();

        /**
         * Returns a tooltip.
         */
        public abstract String getToolTip();
    }

    /**
     * The class that describes a overview marker.
     */
    public class BuildIssueMarker extends Marker<BuildIssue> {

        /**
         * Creates a new marker for target.
         */
        public BuildIssueMarker(BuildIssue anIssue)
        {
            super(anIssue);
            TextBoxLine line = _textArea.getLineAt(Math.min(anIssue.getEnd(), _textArea.length()));
            _y = line.getY() + line.getHeight() / 2;
        }

        /**
         * Returns the color.
         */
        public Color getColor()
        {
            return _target.isError() ? _error : _warning;
        }

        /**
         * Returns the stroke color.
         */
        public Color getStrokeColor()
        {
            return _target.isError() ? _errorBorder : _warningBorder;
        }

        /**
         * Returns the selection start.
         */
        public int getSelStart()
        {
            return _target.getStart();
        }

        /**
         * Returns the selection start.
         */
        public int getSelEnd()
        {
            return _target.getEnd();
        }

        /**
         * Returns a tooltip.
         */
        public String getToolTip()
        {
            return _target.getText();
        }
    }

    /**
     * The class that describes a overview marker.
     */
    public class TokenMarker extends Marker<TextBoxToken> {

        /**
         * Creates a new TokenMarker.
         */
        public TokenMarker(TextBoxToken aToken)
        {
            super(aToken);
            TextBoxLine line = aToken.getLine();
            _y = line.getY() + line.getHeight() / 2;
        }

        /**
         * Returns the color.
         */
        public Color getColor()
        {
            return _warning;
        }

        /**
         * Returns the stroke color.
         */
        public Color getStrokeColor()
        {
            return _warningBorder;
        }

        /**
         * Returns the selection start.
         */
        public int getSelStart()
        {
            return _target.getLine().getStart() + _target.getStart();
        }

        /**
         * Returns the selection start.
         */
        public int getSelEnd()
        {
            return _target.getLine().getStart() + _target.getEnd();
        }

        /**
         * Returns a tooltip.
         */
        public String getToolTip()
        {
            return "Occurrence of \'" + _target.getString() + "\'";
        }
    }
}