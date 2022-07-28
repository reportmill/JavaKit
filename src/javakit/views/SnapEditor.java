package javakit.views;

import java.util.List;

import javakit.parse.*;
import snap.text.TextBoxLine;
import javakit.text.JavaTextArea;
import snap.view.*;

/**
 * The Pane that actually holds SnapPart pieces.
 */
public class SnapEditor extends StackView {

    // The JavaTextArea
    JavaTextArea _jtextArea;

    // The scripts pane
    JFileView _filePart;

    // The selected part
    JNodeView _selPart;

    // The mouse node and X/Y during mouse drag
    View _mnode;
    JNodeView _mpart;
    double _mx, _my;

    /**
     * Creates a new SnapCodeArea.
     */
    public SnapEditor(JavaTextArea aJTA)
    {
        // Set JavaTextArea
        _jtextArea = aJTA;

        // Create FilePart and add
        _filePart = new JFileView();
        _filePart._editor = this;
        _filePart.setGrowWidth(true);
        _filePart.setGrowHeight(true);
        addChild(_filePart);

        // Configure mouse handling
        enableEvents(MousePress, MouseDrag, MouseRelease);
        rebuildUI();
    }

    /**
     * Returns the SnapEditorPane.
     */
    public SnapEditorPane getEditorPane()
    {
        return getOwner(SnapEditorPane.class);
    }

    /**
     * Returns the JavaTextArea.
     */
    public JavaTextArea getJavaTextArea()
    {
        return _jtextArea;
    }

    /**
     * Returns the selected part.
     */
    public JNodeView getSelectedPart()
    {
        return _selPart;
    }

    /**
     * Sets the selected parts.
     */
    public void setSelectedPart(JNodeView aPart)
    {
        if (_selPart != null) _selPart.setSelected(false);
        _selPart = aPart != null ? aPart : _filePart;
        _selPart.setSelected(true);

        // Update JavaTextArea selection
        JNode jnode = _selPart.getJNode();
        int ss = jnode.getStart(), se = jnode.getEnd();
        getJavaTextArea().setSel(ss, se);

        // Forward to editor
        SnapEditorPane ep = getEditorPane();
        if (ep != null) ep.updateSelectedPart(_selPart);
    }

    /**
     * Returns the FilePart.
     */
    public JFileView getFilePart()
    {
        return _filePart;
    }

    /**
     * Returns the JFile JNode.
     */
    public JFile getJFile()
    {
        return getJavaTextArea().getJFile();
    }

    /**
     * Returns the selected part's class.
     */
    public Class getSelectedPartClass()
    {
        // Get class for SnapPart.JNode
        JNodeView spart = getSelectedPart();
        if (spart == null) spart = getFilePart();
        JNode jnode = spart.getJNode();
        Class cls = null;
        for (JNode jn = jnode; jn != null && cls == null; jn = jn.getParent())
            cls = jn.getEvalTypeRealClass();
        return cls;
    }

    /**
     * Returns the selected part's class or the enclosing class, if void.class.
     */
    public Class getSelectedPartEnclClass()
    {
        // Get class for SnapPart.JNode
        JNodeView spart = getSelectedPart();
        if (spart == null) spart = getFilePart();
        JNode jnode = spart.getJNode();
        Class cls = null;
        for (JNode jn = jnode; jn != null && (cls == null || cls.isPrimitive()); jn = jn.getParent())
            cls = jn.getEvalTypeRealClass();
        return cls;
    }

    /**
     * Rebuilds the pieces.
     */
    protected void rebuildUI()
    {
        JFile jfile = getJFile();
        getFilePart().setJNode(jfile);
        setSelectedPartFromTextArea();
    }

    /**
     * Sets the selected part from TextArea selection.
     */
    void setSelectedPartFromTextArea()
    {
        int index = getJavaTextArea().getSelStart();
        JNodeView spart = getSnapPartAt(getFilePart(), index);
        setSelectedPart(spart);
    }

    /**
     * Returns the snap part at given index.
     */
    public JNodeView getSnapPartAt(JNodeView aPart, int anIndex)
    {
        // Check children
        List<JNodeView> children = aPart.getJNodeViews();
        for (JNodeView child : children) {
            JNodeView part = getSnapPartAt(child, anIndex);
            if (part != null)
                return part;
        }

        // Check part
        JNode jnode = aPart.getJNode();
        return jnode.getStart() <= anIndex && anIndex <= jnode.getEnd() ? aPart : null;
    }

    /**
     * Replaces a string.
     */
    protected void replaceText(String aString, int aStart, int anEnd)
    {
        JavaTextArea tview = getJavaTextArea();
        tview.undoerSaveChanges();
        tview.replaceChars(aString, null, aStart, anEnd, true);
        rebuildUI();
    }

    /**
     * Sets text selection.
     */
    protected void setTextSelection(int aStart, int anEnd)
    {
        JavaTextArea tview = getJavaTextArea();
        tview.setSel(aStart, anEnd);
    }

    /**
     * Insets a node.
     */
    public void insertNode(JNode aBaseNode, JNode aNewNode, int aPos)
    {
        if (aBaseNode instanceof JFile) {
            System.out.println("Can't add to file");
            return;
        }

        if (aBaseNode instanceof JStmtExpr && aNewNode instanceof JStmtExpr &&
                aBaseNode.getEvalTypeRealClass() == getSelectedPartClass() && aBaseNode.getEvalTypeRealClass() != void.class) {
            int index = aBaseNode.getEnd();
            String nodeStr = aNewNode.getString(), str = '.' + nodeStr;
            replaceText(str, index - 1, index);
            setTextSelection(index, index + nodeStr.length());
        } else {
            int index = aPos < 0 ? getBeforeNode(aBaseNode) : aPos > 0 ? getAfterNode(aBaseNode) : getInNode(aBaseNode);
            String indent = getIndent(aBaseNode, aPos);
            String nodeStr = aNewNode.getString().trim().replace("\n", "\n" + indent);
            String str = indent + nodeStr + '\n';
            replaceText(str, index, index);
            setTextSelection(index + indent.length(), index + indent.length() + nodeStr.trim().length());
        }
    }

    /**
     * Replaces a JNode with string.
     */
    public void replaceJNode(JNode aNode, String aString)
    {
        replaceText(aString, aNode.getStart(), aNode.getEnd());
    }

    /**
     * Removes a node.
     */
    public void removeNode(JNode aNode)
    {
        int start = getBeforeNode(aNode), end = getAfterNode(aNode);
        replaceText(null, start, end);
    }

    /**
     * Returns after node.
     */
    public int getBeforeNode(JNode aNode)
    {
        int index = aNode.getStart();
        JExpr pexpr = aNode instanceof JExpr ? ((JExpr) aNode).getParentExpr() : null;
        if (pexpr != null) return pexpr.getEnd();
        TextBoxLine tline = getJavaTextArea().getLineAt(index);
        return tline.getStart();
    }

    /**
     * Returns after node.
     */
    public int getAfterNode(JNode aNode)
    {
        int index = aNode.getEnd();
        JExprChain cexpr = aNode.getParent() instanceof JExprChain ? (JExprChain) aNode.getParent() : null;
        if (cexpr != null) return cexpr.getExpr(cexpr.getExprCount() - 1).getEnd();
        TextBoxLine tline = getJavaTextArea().getLineAt(index);
        return tline.getEnd();
    }

    /**
     * Returns in the node.
     */
    public int getInNode(JNode aNode)
    {
        JavaTextArea tview = getJavaTextArea();
        int index = aNode.getStart();
        while (index < tview.length() && tview.charAt(index) != '{') index++;
        TextBoxLine tline = tview.getLineAt(index);
        return tline.getEnd();
    }

    /**
     * Returns the indent.
     */
    String getIndent(JNode aNode, int aPos)
    {
        int index = aNode.getStart();
        TextBoxLine tline = getJavaTextArea().getLineAt(index);
        int c = 0;
        while (c < tline.length() && Character.isWhitespace(tline.charAt(c))) c++;
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < c; i++) sb.append(' ');
        if (aPos == 0) sb.append("    ");
        return sb.toString();
    }

    /**
     * Process events.
     */
    protected void processEvent(ViewEvent anEvent)
    {
        // Handle MousePressed
        if (anEvent.isMousePress()) {
            _mx = anEvent.getX();
            _my = anEvent.getY();
            _mnode = ViewUtils.getDeepestChildAt(this, _mx, _my);
            _mpart = JNodeView.getJNodeView(_mnode);
            if (_mpart == null) _mnode = null;
            else _mnode = _mpart;
            if (_mpart == _filePart) {
                setSelectedPart(null);
                _mpart = null;
            }
            setSelectedPart(_mpart);
        }

        // Handle MouseDragged
        else if (anEvent.isMouseDrag()) {
            if (_mpart == null) return;
            double mx = anEvent.getX(), my = anEvent.getY();
            _mnode.setTransX(_mnode.getTransX() + mx - _mx);
            _mx = mx;
            _mnode.setTransY(_mnode.getTransY() + my - _my);
            _my = my;
        }

        // Handle MouseReleased
        else if (anEvent.isMouseRelease()) {
            if (_mpart == null) return;
            if (_mnode.getTransX() > 150 && _mpart.getJNodeViewParent() != null) removeNode(_mpart.getJNode());
            _mnode.setTransX(0);
            _mnode.setTransY(0);
            _mnode = null;
        }
    }
}