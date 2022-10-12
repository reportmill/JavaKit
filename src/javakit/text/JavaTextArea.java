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
import snap.geom.Rect;
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
    protected TextBoxToken[]  _selTokens = new TextBoxToken[0];

    // A PopupList to show code completion stuff
    protected JavaPopupList  _popup;

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
     * Override to create JavaText.
     */
    @Override
    protected TextAreaKeys createTextAreaKeys()  { return new JavaTextAreaKeys(this); }

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
        int selLineStart = selLine.getStartCharIndex();
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
            setSel(textLine.getStartCharIndex(), textLine.getEndCharIndex());
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

        // Reset SelTokens
        TextBoxToken[] selTokens = getTokensForNode(aNode);
        setSelTokens(selTokens);

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
     * Returns the array of selected tokens.
     */
    public TextBoxToken[] getSelTokens()  { return _selTokens; }

    /**
     * Sets the array of selected tokens.
     */
    private void setSelTokens(TextBoxToken[] theTokens)
    {
        // If new & old both empty, just return
        if (_selTokens.length == 0 && theTokens.length == 0) return;

        // Set + Repaint
        repaintTokensBounds(_selTokens);
        _selTokens = theTokens;
        repaintTokensBounds(_selTokens);
    }

    /**
     * Returns reference tokens for given node.
     */
    protected TextBoxToken[] getTokensForNode(JNode aNode)
    {
        // If not var name or type name, just return
        if (!(aNode instanceof JExprId || aNode instanceof JType))
            return new TextBoxToken[0];

        // Handle null
        JavaDecl nodeDecl = aNode != null ? aNode.getDecl() : null;
        if (nodeDecl == null)
            return new TextBoxToken[0];

        // Get other matching nodes
        List<JNode> matchingNodes = new ArrayList<>();
        NodeMatcher.getMatches(aNode.getFile(), nodeDecl, matchingNodes);
        if (matchingNodes.size() == 0)
            return new TextBoxToken[0];

        // Return TextBoxTokens
        return getTokensForNodes(matchingNodes);
    }

    /**
     * Returns a TextBoxToken array for given JNodes.
     */
    protected TextBoxToken[] getTokensForNodes(List<JNode> theNodes)
    {
        // Convert matching JNodes to TextBoxTokens
        List<TextBoxToken> tokensList = new ArrayList<>(theNodes.size());
        TextBox textBox = getTextBox();
        int textBoxLineStart = 0;
        TextDoc textDoc = getTextDoc();
        if (textDoc instanceof SubText) {
            TextDoc textDocReal = ((SubText) textDoc).getTextDoc();
            int startCharIndex = textDoc.getStartCharIndex();
            textBoxLineStart = textDocReal.getLineForCharIndex(startCharIndex).getIndex();
        }

        // Iterate over nodes and convert to TextBoxTokens
        for (JNode jnode : theNodes) {

            // Get line index (skip if negative - assume Repl import statement or something)
            int lineIndex = jnode.getLineIndex() - textBoxLineStart;
            if (lineIndex < 0)
                continue;;

            // Get line and token
            TextBoxLine textBoxLine = textBox.getLine(lineIndex);
            int startCharIndex = jnode.getLineCharIndex();
            TextBoxToken token = textBoxLine.getTokenAt(startCharIndex);

            // Add to tokens list
            if (token != null)
                tokensList.add(token);
            else System.out.println("JavaTextArea.getTokensForNode: Can't find token for matching node: " + jnode);
        }

        // Return
        return tokensList.toArray(new TextBoxToken[0]);
    }

    /**
     * Repaints token bounds.
     */
    private void repaintTokensBounds(TextBoxToken[] theTokens)
    {
        if (theTokens.length == 0) return;
        Rect tokensBounds = getBoundsForTokens(theTokens);
        repaint(tokensBounds);
    }

    /**
     * Returns the bounds rect for tokens.
     */
    private Rect getBoundsForTokens(TextBoxToken[] theTokens)
    {
        // Get first token and bounds
        TextBoxToken token0 = theTokens[0];
        double tokenX = Math.round(token0.getTextBoxX()) - 1;
        double tokenY = Math.round(token0.getTextBoxY()) - 1;
        double tokenMaxX = Math.ceil(token0.getTextBoxMaxX()) + 1;
        double tokenMaxY = Math.ceil(token0.getTextBoxMaxY()) + 1;

        // Iterate over remaining tokens and union bounds
        for (int i = 1; i < theTokens.length; i++) {
            TextBoxToken token = theTokens[i];
            tokenX = Math.min(tokenX, Math.round(token.getTextBoxX()) - 1);
            tokenY = Math.min(tokenY, Math.round(token.getTextBoxY()) - 1);
            tokenMaxX = Math.max(tokenMaxX, Math.ceil(token.getTextBoxMaxX()) + 1);
            tokenMaxY = Math.max(tokenMaxY, Math.ceil(token.getTextBoxMaxY()) + 1);
        }

        // Return bounds
        return new Rect(tokenX, tokenY, tokenMaxX - tokenX, tokenMaxY - tokenY);
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

            int issueStart = issue.getStart();
            int issueEnd = issue.getEnd();
            if (issueEnd < issueStart || issueEnd > length())
                continue;

            TextBoxLine textBoxLine = getLineForCharIndex(issueEnd);
            int lineStartCharIndex = textBoxLine.getStartCharIndex();
            if (issueStart < lineStartCharIndex)
                issueStart = lineStartCharIndex;
            TextBoxToken token = getTokenForCharIndex(issueStart);
            if (token != null) {
                int tend = token.getTextLine().getStartCharIndex() + token.getEndCharIndex();
                if (issueEnd < tend)
                    issueEnd = tend;
            }

            // If possible, make sure we underline at least one char
            if (issueStart == issueEnd && issueEnd < textBoxLine.getEndCharIndex()) issueEnd++;
            int yb = (int) Math.round(textBoxLine.getBaseline()) + 2;
            double x1 = textBoxLine.getXForChar(issueStart - lineStartCharIndex);
            double x2 = textBoxLine.getXForChar(issueEnd - lineStartCharIndex);
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
                TextBoxLine line = getLineForCharIndex(ind2);
                int s1 = ind2 - line.getStartCharIndex();
                int s2 = ind2 + 1 - line.getStartCharIndex();
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

        // Paint selected tokens highlight rects
        TextBoxToken[] selTokens = getSelTokens();
        if (selTokens.length > 0) {
            aPntr.setColor(new Color("#FFF3AA"));
            for (TextBoxToken token : selTokens) {
                double tokenX = Math.round(token.getTextBoxX()) - 1;
                double tokenY = Math.round(token.getTextBoxY()) - 1;
                double tokenW = Math.ceil(token.getTextBoxMaxX()) - tokenX + 1;
                double tokenH = Math.ceil(token.getTextBoxMaxY()) - tokenY + 1;
                aPntr.fillRect(tokenX, tokenY, tokenW, tokenH);
            }
        }

        // If HoverNode, underline
        if (_hoverNode != null) {
            TextBoxToken hoverToken = (TextBoxToken) _hoverNode.getStartToken();
            double tokenX = hoverToken.getTextBoxX();
            double tokenY = hoverToken.getTextBoxStringY() + 1;
            double tokenMaxX = tokenX + hoverToken.getWidth();
            aPntr.setColor(Color.BLACK);
            aPntr.drawLine(tokenX, tokenY, tokenMaxX, tokenY);
        }
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
            addChars(INDENT_STRING, null, line.getStartCharIndex());
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
            replaceChars("", null, line.getStartCharIndex(), line.getStartCharIndex() + 4, false);
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