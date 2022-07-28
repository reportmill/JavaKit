/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.text;

import java.lang.reflect.Method;
import java.util.*;

import javakit.parse.JNode;
import javakit.parse.JStmtBlock;
import snap.geom.*;
import snap.text.*;
import snap.view.*;

/**
 * A class to manage a Java inspector.
 */
public class CodeBuilder extends ViewOwner {

    // The JavaTextArea this inspector works for.
    JavaTextArea _textArea;

    // The selected node
    JNode _node;

    // The suggestion list
    ListView<CodeBlock> _suggestionsList;

    // The dragging CodeBlock
    CodeBlock _dragCodeBlock;

    // The current drag point
    Point _dragPoint;

    // The current node at drag point
    JNode _dragNode, _dragBlock;

    // The drag text
    TextBox _dragText;

    /**
     * Creates a new JavaInspector.
     */
    public CodeBuilder(JavaTextArea aJavaTextArea)
    {
        _textArea = aJavaTextArea;
    }

    /**
     * Returns the JavaTextArea associated with text pane.
     */
    public JavaTextArea getTextArea()
    {
        return _textArea;
    }

    /**
     * Whether inspector is visible.
     */
    public boolean isVisible()
    {
        return isUISet() && getUI().isVisible();
    }

    /**
     * Sets CodeBlocks for current TextArea.SelectedNode.
     */
    public void setCodeBlocks()
    {
        // Get SelectedNode (or first node parent with class) and its class
        _node = getTextArea().getSelectedNode();
        while (_node != null && _node.getEvalTypeRealClass() == null) _node = _node.getParent();

        // Get suggested CodeBlocks for class and set in Suggestions list
        Object items[] = getCodeBlocks(_node);
        setViewItems(_suggestionsList, items);
        resetLater();
    }

    /**
     * Returns suggestions for class.
     */
    private CodeBlock[] getCodeBlocks(JNode aNode)
    {
        List list = new ArrayList();
        Class cls = _node != null ? _node.getEvalTypeRealClass() : null;
        Method methods[] = cls != null ? cls.getMethods() : new Method[0];
        for (Method method : methods) {
            if (method.getDeclaringClass() == Object.class) continue;
            list.add(new CodeBlock().init(aNode, method));
        }
        return (CodeBlock[]) list.toArray(new CodeBlock[list.size()]);
    }

    /**
     * Create UI for CodeBuilder.
     */
    protected View createUI()
    {
        Label label = new Label();
        label.setName("ClassText");
        label.setPrefHeight(24);
        label.setPadding(5, 5, 5, 5);
        ListView slist = new ListView();
        slist.setName("SuggestionsList");
        slist.setGrowHeight(true);
        slist.setRowHeight(22);
        ScrollView spane = new ScrollView(slist);
        spane.setGrowHeight(true);
        ColView vbox = new ColView();
        vbox.setChildren(label, spane);
        vbox.setFillWidth(true);
        vbox.setPrefWidth(260);
        return vbox;
    }

    /**
     * Initializes UI panel.
     */
    public void initUI()
    {
        // Get suggestion list
        _suggestionsList = getView("SuggestionsList", ListView.class);
        enableEvents(_suggestionsList, ViewEvent.Type.DragGesture, ViewEvent.Type.DragSourceEnd);
        _suggestionsList.setCellConfigure(this::configureSuggestionsList);
    }

    /**
     * Reset UI.
     */
    public void resetUI()
    {
        setViewValue("ClassText", _node != null ? _node.getEvalTypeRealClass().getSimpleName() + " Methods" : "No Selection");
    }

    /**
     * Responds to UI.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Handle SuggestionsList
        if (anEvent.is("SuggestionsList")) {

            // Handle DragGesture
            if (anEvent.isDragGesture()) {

                // Set DragSuggestion and DragString
                _dragCodeBlock = (CodeBlock) anEvent.getSelItem();
                String dragString = _dragCodeBlock.getString();

                // Get event dboard and start drag
                Clipboard cboard = anEvent.getClipboard();
                cboard.addData(dragString);
                //cboard.setDragImageFromString(dragString, getTextArea().getFont().deriveFont(10f));
                //cboard.setDragImagePoint(0, dboard.getDragImage().getHeight()/2);
                cboard.startDrag();
            }

            // Handle DragSourceEnd
            if (anEvent.isDragSourceEnd()) _dragCodeBlock = null;
        }
    }

    /**
     * Called to configure ListCell.
     */
    protected void configureSuggestionsList(ListCell<CodeBlock> aCell)
    {
        CodeBlock cb = aCell.getItem();
        if (cb == null) return;
        aCell.setText(cb.getString());
        aCell.setImage(JavaTextBox.CodeImage);
        aCell.getGraphic().setPadding(4, 4, 4, 4);
    }

    /**
     * Called when drag is over TextArea.
     */
    public void dragOver(double anX, double aY)
    {
        // Bail?
        if (_dragCodeBlock == null) return;

        // Set DragPoint and register TextArea to repaint
        _dragPoint = new Point(anX, aY);
        getTextArea().repaint();

        // Set DragNode
        int index = getTextArea().getCharIndex(anX, aY);
        _dragNode = getTextArea().getJFile().getNodeAtCharIndex(index);

        // Get DragBlock
        _dragBlock = _dragNode;
        while (_dragBlock != null && !(_dragBlock instanceof JStmtBlock)) _dragBlock = _dragBlock.getParent();
        if (_dragBlock == null) {
            clearDrag();
            return;
        }

        // Make sure Y is below DragBlock first line
        TextBoxToken dragBlockToken = (TextBoxToken) _dragBlock.getStartToken();
        TextBoxLine dragBlockLine = dragBlockToken.getLine();
        if (aY < dragBlockLine.getY() + dragBlockLine.getLineAdvance())
            _dragPoint = new Point(anX, dragBlockLine.getY() + dragBlockLine.getLineAdvance() + 1);

        // Get DragBlock.String with indent
        TextBoxToken dragToken = (TextBoxToken) _dragNode.getStartToken();
        TextBoxLine line = dragToken.getLine();
        String indent = getIndentString(line.getIndex());
        String dragString = indent + _dragCodeBlock.getString();

        // If DragText needs to be reset, create and reset
        if (_dragText == null || !_dragText.getString().equals(dragString)) {
            JavaTextArea textArea = getTextArea();
            JavaTextBox text = textArea.getTextBox();
            _dragText = textArea.createTextBox();
            _dragText.setX(text.getX());
            _dragText.setString(dragString);
        }
    }

    /**
     * Called when drag exits TextArea.
     */
    public void dragExit()
    {
        clearDrag();
    }

    /**
     * Drop suggestion at point.
     */
    public void drop(double anX, double aY)
    {
        JavaTextArea textArea = getTextArea();
        TextBoxLine line = textArea.getTextBox().getLineForY(_dragPoint.getY());
        CharSequence indent = getIndentString(line.getIndex());
        String string = _dragCodeBlock.getReplaceString(), fullString = indent + string + "\n";
        int selStart = line.getStart();
        textArea.replaceChars(fullString, null, selStart, selStart, false);
        textArea.setSel(selStart + indent.length(), selStart + indent.length() + string.length());
        //int argStart = string.indexOf('('), argEnd = argStart>0? string.indexOf(')', argStart) : -1;
        //if(argEnd>argStart+1) textArea.setSelection(selStart + argStart + 1, selStart + argEnd);
        textArea.requestFocus();
        clearDrag();
    }

    /**
     * Clears the drag information.
     */
    private void clearDrag()
    {
        _dragText = null;
        _dragNode = _dragBlock = null;
    }

    /**
     * Returns the Drag point.
     */
    public Point getDragPoint()
    {
        return _dragPoint;
    }

    /**
     * Returns the DragCodeBlock.
     */
    public CodeBlock getDragCodeBlock()
    {
        return _dragCodeBlock;
    }

    /**
     * Returns the DragNode.
     */
    public JNode getDragNode()
    {
        return _dragNode;
    }

    /**
     * Returns the DragText.
     */
    public TextBox getDragText()
    {
        return _dragText;
    }

    /**
     * Override to provide hook for CodeBuilder to paint.
     */
    /*protected boolean paintTextSelection(JavaTextArea aTextArea, Graphics2D aGraphics)
    {
        // If DragBlock is null, return false
        if(_dragBlock==null || _dragText==null) return false;

        // Get SelectionPath for DragBlock and paint
        TextToken startToken = aTextArea.getText().getTokenAt(_dragBlock.getStart());
        TextToken endToken = aTextArea.getText().getTokenAt(_dragBlock.getEnd());
        double x = Math.min(startToken.getX(), endToken.getX()) - 2, w = aTextArea.getWidth() - x - 10;
        double y = startToken.getY() - 3, h = endToken.getMaxY() - y + _dragText.getPrefHeight() + 2;
        Rectangle2D rect = new Rectangle2D(x, y, w, h);
        aGraphics.setColor(_dbFill); aGraphics.fill(rect);
        aGraphics.setColor(_dbStroke); aGraphics.setStroke(new BasicStroke(2)); aGraphics.draw(rect);
        return true;
    }*/

    // DragBlock paint colors
    //private static Color _dbFill = new Color(255, 235, 235), _dbStroke = new Color(160,160,255);

    /**
     * Paints a TextLine.
     */
    /*protected boolean paintLine(JavaTextArea aTextArea, Graphics2D aGraphics, TextLine aLine, double anX, double aY)
    {
        // Get DragText and DragPoint (return false if no DragText or not to DragPoint yet)
        Text dragText = getDragText(); if(dragText==null) return false;
        Point2D dragPoint = getDragPoint(); if(aLine.getMaxY()<=dragPoint.getY()) return false;
        double y = aY;

        // If Line straddles DragPoint, paint DragText.Line
        if(aLine.getMaxY() - aLine.getLineAdvance()<=dragPoint.getY()) { // Draw DragText if Line straddles DragPoint
            TextLine dragLine = dragText.getLine(0); TextToken dragLineToken0 = dragLine.getToken(0);
            double x = dragLineToken0.getX(), w = dragLine.getMaxX() - x;
            aGraphics.setColor(Color.decode("#FFF280"));
            aGraphics.fillRect((int)x, (int)aLine.getY()-2, (int)w, (int)aLine.getHeight()+2);
            //aTextArea.paintLineImpl(aGraphics, dragLine, 0, y);
        }
        y += aLine.getLineAdvance();

        // Do normal version
        aTextArea.paintLineImpl(aGraphics, aLine, anX, y);
        return true;
    }*/

    /**
     * Returns the number of indent spaces for line at given index.
     */
    public int getIndent(int anIndex)
    {
        if (anIndex == 0) return 0;
        TextBoxLine line = getTextArea().getTextBox().getLine(anIndex - 1);
        int c = 0;
        while (c < line.length() && Character.isWhitespace(line.charAt(c))) c++;
        if (!line.getString().trim().endsWith(";")) c += 4;
        return c;
    }

    /**
     * Returns the indent string for line at given index.
     */
    public String getIndentString(int anIndex)
    {
        int c = getIndent(anIndex);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < c; i++) sb.append(' ');
        return sb.toString();
    }
}