/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.text;
import java.util.*;
import javakit.parse.*;
import javakit.reflect.JavaClass;
import javakit.reflect.JavaDecl;
import javakit.reflect.NodeMatcher;
import javakit.shell.JavaTextDoc;
import snap.gfx.*;
import snap.text.*;
import javakit.resolver.*;
import snap.props.PropChange;
import snap.view.*;
import snap.web.WebFile;

/**
 * A TextArea subclass for Java source editing.
 */
public class JavaTextArea extends TextArea {

    // Whether to draw line for print margin column
    private boolean  _showPrintMargin = true;

    // The selected JNode
    protected JNode  _selNode = new JFile();

    // The deepest child of SelNode recently selected
    protected JNode  _deepNode;

    // The node that the mouse is hovering over (if command down)
    protected JNode  _hoverNode;

    // The list of selected tokens
    protected List<TextBoxToken>  _tokens = new ArrayList<>();

    // A PopupList to show code completion stuff
    protected JavaPopupList  _popup;

    // A helper class for key processing
    private JavaTextAreaKeys  _keys = new JavaTextAreaKeys(this);

    // Constants for properties
    public static final String SelectedNode_Prop = "SelectedNode";

    // Constants
    protected static String INDENT_STRING = "    ";

    /**
     * Creates a new JavaTextArea.
     */
    public JavaTextArea()
    {
        setFill(Color.WHITE);
        setPadding(5, 5, 5,5);
        setEditable(true);
    }

    /**
     * Override to return text as JavaText.
     */
    public JavaTextBox getTextBox()  { return (JavaTextBox) super.getTextBox(); }

    /**
     * Override to create JavaText.
     */
    protected TextBox createTextBox()  { return new JavaTextBox(); }

    /**
     * Returns the code completion popup.
     */
    public JavaPopupList getPopup()
    {
        // If already set, just return
        if (_popup != null) return _popup;

        // Create, set, return
        JavaPopupList popupList = new JavaPopupList(this);
        return _popup = popupList;
    }

    /**
     * Activates the popup list (shows popup if multiple suggestions, does replace for one, does nothing for none).
     */
    public void activatePopupList()
    {
        // Get suggestions (just return if none)
        JavaDecl[] completions = getCompletionsAtCursor();
        if (completions == null || completions.length == 0)
            return;

        // Set completions
        JavaPopupList popup = getPopup();
        popup.setItems(completions);

        // Get location for text start
        TextSel textSel = getSel();
        TextBoxLine selLine = textSel.getStartLine();
        int selLineStart = selLine.getStart();
        JNode selNode = getSelNode();
        int selNodeStart = selNode.getStart() - getTextDoc().getStartCharIndex() - selLineStart;

        // Get location for popup and show
        double textX = selLine.getXForChar(selNodeStart);
        double textY = selLine.getMaxY() + 4;
        popup.show(this, textX, textY);
    }

    /**
     * Handle TextEditor PropertyChange to update Popup Suggestions when SelectedNode changes.
     */
    public void updatePopupList()
    {
        // Get popup (just return if not showing)
        JavaPopupList javaPopup = getPopup();
        if (!javaPopup.isShowing())
            return;

        // Get completions (just return if empty)
        JavaDecl[] completions = getCompletionsAtCursor();
        if (completions == null || completions.length == 0) {
            javaPopup.hide();
            return;
        }

        // Set completions
        javaPopup.setItems(completions);
    }

    /**
     * Returns completions for current text selection and selected node.
     */
    public JavaDecl[] getCompletionsAtCursor()
    {
        // If selection not empty, just return
        if (!isSelEmpty())
            return null;

        // If selection not at end of SelNode, just return
        int selStart = getSelStart();
        JNode selNode = getSelNode();
        int startCharIndex = getTextDoc().getStartCharIndex();
        int nodeEnd = selNode.getEnd() - startCharIndex;
        if (selStart != nodeEnd)
            return null;

        // Get completions and return
        JavaCompleter javaCompleter = new JavaCompleter();
        JavaDecl[] completions = javaCompleter.getCompletionsForNode(selNode);
        return completions;
    }

    /**
     * Returns whether to draw line for print margin column.
     */
    public boolean getShowPrintMargin()  { return _showPrintMargin; }

    /**
     * Sets whether to draw line for print margin column.
     */
    public void setShowPrintMargin(boolean aValue)  { _showPrintMargin = aValue; }

    /**
     * Selects a given line number.
     */
    public void selectLine(int anIndex)
    {
        TextBoxLine textLine = anIndex >= 0 && anIndex < getLineCount() ? getLine(anIndex) : null;
        if (textLine != null)
            setSel(textLine.getStart(), textLine.getEnd());
    }

    /**
     * Returns the default font.
     */
    public Font getDefaultFont()  { return JavaTextUtils.getCodeFont(); }

    /**
     * Returns the JFile (parsed representation of Java file).
     */
    public JFile getJFile()
    {
        //JavaTextBox textBox = getTextBox();
        TextDoc textDoc = getTextDoc();
        if (textDoc instanceof SubText)
            textDoc = ((SubText) textDoc).getTextDoc();

        // Get JavaTextDoc and forward
        JavaTextDoc javaTextDoc = (JavaTextDoc) textDoc;
        return javaTextDoc.getJFile();
    }

    /**
     * Returns the node at given start/end char indexes.
     */
    public JNode getNodeAtCharIndex(int startCharIndex, int endCharIndex)
    {
        // If TextDoc is SubText, adjust start/end
        TextDoc textDoc = getTextDoc();
        int subTextStart = textDoc.getStartCharIndex();
        if (subTextStart > 0) {
            startCharIndex += subTextStart;
            endCharIndex += subTextStart;
        }

        // Forward to JFile
        JFile jfile = getJFile();
        return jfile.getNodeAtCharIndex(startCharIndex, endCharIndex);
    }

    /**
     * Override to update selected node and tokens.
     */
    public void setSel(int aStart, int anEnd)
    {
        // Do normal version
        super.setSel(aStart, anEnd);

        // Get node for selection
        int selStart = getSelStart(), selEnd = getSelEnd();
        JNode node = getNodeAtCharIndex(selStart, selEnd);

        // Select node
        setSelNode(node);
    }

    /**
     * Returns the selected JNode.
     */
    public JNode getSelNode()  { return _selNode; }

    /**
     * Sets the selected JNode.
     */
    public void setSelNode(JNode aNode)
    {
        // If already set, just return
        if (aNode == getSelNode()) return;

        // Set value
        JNode oldSelNode = _selNode;
        _selNode = _deepNode = aNode;

        // Reset tokens
        setSelTokensForNode(aNode);

        // Reset PopupList
        updatePopupList();

        // Fire prop change
        firePropChange(SelectedNode_Prop, oldSelNode, _selNode);
    }

    /**
     * Returns the class name for the currently selected JNode.
     */
    public Class<?> getSelNodeClass()
    {
        JavaClass javaClass = _selNode != null ? _selNode.getEvalClass() : null;
        Class<?> realClass = javaClass != null ? javaClass.getRealClass() : null;
        return realClass;
    }

    /**
     * Returns the list of selected tokens.
     */
    public List<TextBoxToken> getSelTokens()  { return _tokens; }

    /**
     * Sets the list of selected tokens.
     */
    protected void setSelTokens(List<TextBoxToken> theTkns)
    {
        _tokens.clear();
        _tokens.addAll(theTkns);
    }

    /**
     * Sets the list of selected tokens (should be in background).
     */
    protected void setSelTokensForNode(JNode aNode)
    {
        // Not sure if this happens anymore
        if (aNode == null) {
            setSelTokens(Collections.EMPTY_LIST);
            return;
        }

        // This should go
        if (!(aNode.getStartToken() instanceof TextBoxToken)) {
            System.out.println("JavaTextArea.setSelTokensForNode: Not TextBoxToken");
            return;
        }

        // Create list for tokens
        List<TextBoxToken> tokens = new ArrayList<>();

        // If node is JType, select all of them
        JavaDecl decl = aNode.getDecl();
        if (decl != null) {
            List<JNode> others = new ArrayList<>();
            NodeMatcher.getMatches(aNode.getFile(), decl, others);
            for (JNode other : others) {
                TextBoxToken tt = (TextBoxToken) other.getStartToken();
                tokens.add(tt);
            }
        }

        // Set tokens
        setSelTokens(tokens);
    }

    /**
     * Returns the node under the mouse (if command is down).
     */
    public JNode getHoverNode()  { return _hoverNode; }

    /**
     * Sets the node under the mouse (if command is down).
     */
    public void setHoverNode(JNode aNode)
    {
        _hoverNode = aNode;
        repaint();
    }

    /**
     * Override to draw print margin.
     */
    protected void paintBack(Painter aPntr)
    {
        // Do normal version
        super.paintBack(aPntr);

        // Configure MarginLine
        if (getShowPrintMargin()) {
            double x = getPadding().getLeft() + getFont().charAdvance('X') * 120 + .5;
            aPntr.setColor(Color.LIGHTGRAY);
            aPntr.setStroke(Stroke.Stroke1);
            aPntr.drawLine(x, 0, x, getHeight());
        }

        // Underline build issues
        BuildIssue[] issues = getBuildIssues();
        for (BuildIssue issue : issues) {

            int istart = issue.getStart();
            int iend = issue.getEnd();
            if (iend < istart || iend > length())
                continue;

            TextBoxLine line = getLineAt(iend);
            int lstart = line.getStart();
            if (istart < lstart)
                istart = lstart;
            TextBoxToken token = getTokenAt(istart);
            if (token != null) {
                int tend = token.getLine().getStart() + token.getEnd();
                if (iend < tend)
                    iend = tend;
            }

            // If possible, make sure we underline at least one char
            if (istart == iend && iend < line.getEnd()) iend++;
            int yb = (int) Math.round(line.getBaseline()) + 2;
            double x1 = line.getXForChar(istart - lstart);
            double x2 = line.getXForChar(iend - lstart);
            aPntr.setPaint(issue.isError() ? Color.RED : new Color(244, 198, 60));
            aPntr.setStroke(Stroke.StrokeDash1);
            aPntr.drawLine(x1, yb, x2, yb);
            aPntr.setStroke(Stroke.Stroke1);
        }

        // Add box around balancing bracket
        if (getSel().getSize() < 2) {

            // Get chars before and after cursor char index
            int ind = getSelStart();
            int ind2 = -1;
            char c1 = ind > 0 ? charAt(ind - 1) : 0;
            char c2 = ind < length() ? charAt(ind) : 0;

            // If char at cursor is open/close, find close index
            if (c2 == '{' || c2 == '}') {    // || c2=='(' || c2==')'
                JNode jnode = getNodeAtCharIndex(ind, ind);
                ind2 = c2 == '}' ? jnode.getStart() : jnode.getEnd() - 1;
                ind2 -= getTextDoc().getStartCharIndex();
                if (ind2 + 1 > length()) {
                    System.err.println("JavaTextArea.paintBack: Invalid-A " + ind2);
                    ind2 = -1;
                }
            }

            // If char before cursor is open/close, find close index
            else if (c1 == '{' || c1 == '}') {  //  || c1=='(' || c1==')'
                JNode jnode = getNodeAtCharIndex(ind - 1, ind - 1);
                ind2 = c1 == '}' ? jnode.getStart() : jnode.getEnd() - 1;
                ind2 -= getTextDoc().getStartCharIndex();
                if (ind2 + 1 > length()) {
                    System.err.println("JavaTextArea.paintBack: Invalid-B" + ind2);
                    ind2 = -1;
                }
            }

            // If closing index found, draw rect
            if (ind2 >= 0) {
                TextBoxLine line = getLineAt(ind2);
                int s1 = ind2 - line.getStart();
                int s2 = ind2 + 1 - line.getStart();
                double x1 = line.getXForChar(s1);
                double x2 = line.getXForChar(s2);
                aPntr.setColor(Color.LIGHTGRAY);
                aPntr.drawRect(x1, line.getY(), x2 - x1, line.getHeight());
            }
        }

        // Paint program counter
        int progCounterLine = getProgramCounterLine();
        if (progCounterLine >= 0 && progCounterLine < getLineCount()) {
            TextBoxLine line = getLine(progCounterLine);
            aPntr.setPaint(new Color(199, 218, 175, 200));
            aPntr.fillRect(line.getX(), line.getY() + .5, line.getWidth(), line.getHeight());
        }

        // Paint boxes around selected tokens
        Color tcolor = new Color("#FFF3AA");
        for (TextBoxToken token : getSelTokens()) {
            double x = Math.round(token.getTextBoxX()) - 1, w = Math.ceil(token.getTextBoxMaxX()) - x + 1;
            double y = Math.round(token.getTextBoxY()) - 1, h = Math.ceil(token.getTextBoxMaxY()) - y + 1;
            aPntr.setColor(tcolor);
            aPntr.fillRect(x, y, w, h);
        }

        // If HoverNode, underline
        if (_hoverNode != null) {
            TextBoxToken ttoken = (TextBoxToken) _hoverNode.getStartToken();
            double x1 = ttoken.getTextBoxX();
            double y = ttoken.getTextBoxStringY() + 1;
            double x2 = x1 + ttoken.getWidth();
            aPntr.setColor(Color.BLACK);
            aPntr.drawLine(x1, y, x2, y);
        }
    }

    /**
     * Called when a key is pressed.
     */
    protected void keyPressed(ViewEvent anEvent)
    {
        _keys.keyPressed(anEvent);
    }

    /**
     * Called when a key is pressed.
     */
    protected void keyPressedSuper(ViewEvent anEvent)
    {
        super.keyPressed(anEvent);
    }

    /**
     * Called when a key is typed.
     */
    protected void keyTyped(ViewEvent anEvent)
    {
        _keys.keyTyped(anEvent);
    }

    /**
     * Called when a key is typed.
     */
    protected void keyTypedSuper(ViewEvent anEvent)
    {
        super.keyTyped(anEvent);
    }

    /**
     * Returns the indent of a given line.
     */
    public int getIndent(TextBoxLine aLine)
    {
        int i = 0;
        while (i < aLine.length() && aLine.charAt(i) == ' ') i++;
        return i;
    }

    /**
     * Indents the text.
     */
    public void indentLines()
    {
        TextSel textSel = getSel();
        int startLineIndex = textSel.getStartLine().getIndex();
        int endLineIndex = textSel.getEndLine().getIndex();
        for (int i = startLineIndex; i <= endLineIndex; i++) {
            TextBoxLine line = getLine(i);
            addChars(INDENT_STRING, null, line.getStart());
        }
    }

    /**
     * Indents the text.
     */
    public void outdentLines()
    {
        TextSel textSel = getSel();
        int startLineIndex = textSel.getStartLine().getIndex();
        int endLineIndex = textSel.getEndLine().getIndex();
        for (int i = startLineIndex; i <= endLineIndex; i++) {
            TextBoxLine line = getLine(i);
            if (line.length() < 4 || !line.subSequence(0, 4).toString().equals(INDENT_STRING)) continue;
            replaceChars("", null, line.getStart(), line.getStart() + 4, false);
        }
    }

    /**
     * Override to setTextModified.
     */
    protected void textDocDidPropChange(PropChange anEvent)
    {
        // Do normal version and update TextPane.TextModified (just return if not chars change)
        super.textDocDidPropChange(anEvent);
        if (anEvent.getPropertyName() != TextDoc.Chars_Prop)
            return;

        // Call didAddChars/didRemoveChars
        TextDocUtils.CharsChange charsChange = (TextDocUtils.CharsChange) anEvent;
        int charIndex = anEvent.getIndex();
        CharSequence addChars = charsChange.getNewValue();
        CharSequence removeChars = charsChange.getOldValue();
        if (addChars != null)
            didAddChars(addChars, charIndex);
        else didRemoveChars(removeChars, charIndex);
    }

    /**
     * Called when characters are added.
     */
    protected void didAddChars(CharSequence theChars, int charIndex)
    {
        // Iterate over BuildIssues and shift start/end for removed chars
        int charsLength = theChars.length();
        for (BuildIssue buildIssue : getBuildIssues()) {
            int buildIssueStart = buildIssue.getStart();
            if (charIndex <= buildIssueStart)
                buildIssue.setStart(buildIssueStart + charsLength);
            int buildIssueEnd = buildIssue.getEnd();
            if (charIndex < buildIssueEnd)
                buildIssue.setEnd(buildIssueEnd + charsLength);
        }

        // Iterate over Breakpoints and shift start/end for removed chars
//        int sline = getLineAt(aStart).getIndex(), eline = getLineAt(aStart+len).getIndex(), dline = eline - sline;
//        if(sline!=eline) for(Breakpoint bp : getBreakpoints().toArray(new Breakpoint[0])) {
//            int bline = bp.getLine();
//            if(sline<bline && eline<=bline) { bp.setLine(bline + dline);
//                getProjBreakpoints().writeFile(); }
//        }
    }

    /**
     * Called when characters are removed.
     */
    protected void didRemoveChars(CharSequence theChars, int charIndex)
    {
        // See if we need to shift BuildIssues
        int endOld = charIndex + theChars.length();
        for (BuildIssue buildIssue : getBuildIssues()) {
            int buildIssueStart = buildIssue.getStart();
            int buildIssueEnd = buildIssue.getEnd();
            int start = buildIssueStart;
            int end = buildIssueEnd;
            if (charIndex < buildIssueStart)
                start = buildIssueStart - (Math.min(buildIssueStart, endOld) - charIndex);
            if (charIndex < buildIssueEnd)
                end = buildIssueEnd - (Math.min(buildIssueEnd, endOld) - charIndex);
            buildIssue.setStart(start);
            buildIssue.setEnd(end);
        }

        // Get number of newlines removed (in chars)
        int nlc = 0;
        for (int i = 0, iMax = theChars.length(); i < iMax; i++) {
            char c = theChars.charAt(i);
            if (c == '\r') {
                nlc++;
                if (i + 1 < iMax && theChars.charAt(i + 1) == '\n') i++;
            } else if (c == '\n') nlc++;
        }

        // See if we need to remove Breakpoints
//        int sline = getLineAt(aStart).getIndex(), eline = sline + nlc, dline = eline - sline;
//        if(sline!=eline) for(Breakpoint bp : getBreakpoints().toArray(new Breakpoint[0])) {
//            int bline = bp.getLine();
//            if(sline<bline && eline<=bline) { bp.setLine(bline - dline);
//                getProjBreakpoints().writeFile(); }
//            else if(sline<bline && eline>bline)
//                getProjBreakpoints().remove(bp);
//        }
    }

    /**
     * Returns BuildIssues from ProjectFile.
     */
    public BuildIssue[] getBuildIssues()
    {
        return new BuildIssue[0]; // Was getRootProject().getBuildIssues().getIssues(getSourceFile()) JK
    }

    /**
     * Returns the project breakpoints.
     */
    private Breakpoints getProjBreakpoints()
    {
        return null; // Was getRootProject().getBreakPoints() JK
    }

    /**
     * Returns Breakpoints from ProjectFile.
     */
    public List<Breakpoint> getBreakpoints()
    {
        Breakpoints breakpoints = getProjBreakpoints(); if (breakpoints == null) return null;
        TextDoc textDoc = getTextDoc();
        WebFile file = textDoc.getSourceFile(); if (file == null) return null;
        return breakpoints.get(file);
    }

    /**
     * Adds breakpoint at line.
     */
    public void addBreakpoint(int aLine)
    {
        Breakpoints breakpoints = getProjBreakpoints(); if (breakpoints == null) return;
        TextDoc textDoc = getTextDoc();
        WebFile file = textDoc.getSourceFile();
        breakpoints.addBreakpoint(file, aLine);
    }

    /**
     * Remove breakpoint.
     */
    public void removeBreakpoint(Breakpoint aBP)
    {
        Breakpoints breakpoints = getProjBreakpoints(); if (breakpoints == null) return;
        breakpoints.remove(aBP);
    }

    /**
     * Returns the ProgramCounter line.
     */
    public int getProgramCounterLine()
    {
        JavaTextPane javaTextPane = getOwner(JavaTextPane.class); if (javaTextPane == null) return -1;
        return javaTextPane.getProgramCounterLine();
    }

    /**
     * Override to get string first.
     */
    @Override
    protected Object getClipboardContent(Clipboard clipboard)
    {
        // Try String first
        if (clipboard.hasString()) {
            String str = clipboard.getString();
            if (str != null && str.length() > 0)
                return str;
        }

        // Do normal version
        return super.getClipboardContent(clipboard);
    }

    /**
     * Override to remove extra indent from pasted strings.
     */
    @Override
    public void replaceCharsWithContent(Object theContent)
    {
        // If String, trim extra indent
        if (theContent instanceof String && getTextDoc() instanceof SubText)
            theContent = JavaTextUtils.removeExtraIndentFromString((String) theContent);

        // Do normal version
        super.replaceCharsWithContent(theContent);
    }
}