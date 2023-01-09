/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.ide;
import javakit.parse.*;
import javakit.resolver.JavaDecl;
import javakit.parse.JavaTextDoc;
import snap.gfx.*;
import snap.props.PropChange;
import snap.props.Undoer;
import snap.text.TextDoc;
import snap.util.*;
import snap.view.*;
import snap.viewx.TextPane;

import java.util.ArrayList;
import java.util.List;

/**
 * A panel for editing Java files.
 */
public class JavaTextPane extends TextPane {

    // The JavaTextArea
    protected JavaTextArea  _textArea;

    // The RowHeader
    private LineHeaderView  _lineHeaderView;

    // The OverView
    private LineFooterView  _lineFooterView;

    /**
     * Constructor.
     */
    public JavaTextPane()
    {
        super();
    }

    /**
     * Returns the JavaTextArea.
     */
    public JavaTextArea getTextArea()
    {
        return (JavaTextArea) super.getTextArea();
    }

    /**
     * Creates the JavaTextArea.
     */
    protected JavaTextArea createTextArea()
    {
        return new JavaTextArea();
    }

    /**
     * Returns the code completion popup.
     */
    public JavaPopupList getPopup()
    {
        return getTextArea().getPopup();
    }

    /**
     * Initialize UI panel.
     */
    @Override
    protected void initUI()
    {
        // Do normal version
        super.initUI();

        // Get TextArea and start listening for events (KeyEvents, MouseReleased)
        _textArea = getTextArea();
        _textArea.setGrowWidth(true);
        enableEvents(_textArea, KeyPress, KeyRelease, KeyType, MousePress, MouseRelease);
        _textArea.addPropChangeListener(pc -> javaTextAreaDidPropChange(pc));

        // Start listening to TextArea doc
        TextDoc textDoc = _textArea.getTextDoc();
        textDoc.addPropChangeListener(pc -> textDocDidPropChange(pc));

        // Reset TextArea font
        double fontSize = Prefs.getDefaultPrefs().getDouble("JavaFontSize", 12);
        if (fontSize < 8) fontSize = 12;
        _textArea.setFont(new Font(_textArea.getDefaultFont().getName(), fontSize));

        // Create/configure LineNumView, LineFooterView
        _lineHeaderView = new LineHeaderView(this, getTextArea());
        _lineFooterView = new LineFooterView(this);

        // Create ScrollGroup for JavaTextArea and LineNumView
        ScrollGroup scrollGroup = new ScrollGroup();
        scrollGroup.setBorder(Color.GRAY9, 1);
        scrollGroup.setGrowWidth(true);
        scrollGroup.setContent(_textArea);
        scrollGroup.setLeftView(_lineHeaderView);
        scrollGroup.setMinWidth(200);

        // Replace TextPane center with scrollGroup
        BorderView borderView = getUI(BorderView.class);
        borderView.setCenter(scrollGroup);
        borderView.setRight(_lineFooterView);
    }

    /**
     * Reset UI.
     */
    protected void resetUI()
    {
        // Do normal version
        super.resetUI();

        // Reset FontSizeText
        JavaTextArea textArea = getTextArea();
        setViewValue("FontSizeText", textArea.getFont().getSize());

        // Update UndoButton, RedoButton
        Undoer undoer = textArea.getUndoer();
        boolean hasUndos = undoer.hasUndos();
        setViewEnabled("UndoButton", hasUndos);
        setViewEnabled("RedoButton", hasUndos);

        // Reset NodePathBox
        resetNodePathBox();
    }

    /**
     * Resets the NodePath box.
     */
    private void resetNodePathBox()
    {
        // Clear path box and add Lin/Col position label
        RowView nodePathBox = getView("BottomBox", RowView.class);
        while (nodePathBox.getChildCount() > 1)
            nodePathBox.removeChild(1);

        // Get Path node labels
        Label[] pathNodeLabels = getLabelsForSelNodePath();
        for (Label pathNodeLabel : pathNodeLabels) {
            pathNodeLabel.setOwner(this);
            enableEvents(pathNodeLabel, MouseRelease);
            nodePathBox.addChild(pathNodeLabel);
        }
    }

    /**
     * Respond to UI controls.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        // Do normal version
        super.respondUI(anEvent);

        // Handle TextArea key events
        if (anEvent.equals("TextArea")) {

            // Handle KeyPressed/KeyReleased to watch for CONTROL/COMMAND press/release
            if (anEvent.isKeyPress() || anEvent.isKeyRelease()) {
                int kc = anEvent.getKeyCode();
                if (kc == KeyCode.COMMAND || kc == KeyCode.CONTROL)
                    setTextAreaHoverEnabled(anEvent.isKeyPress());
            }

            // Handle KeyTyped: If PopupList not visible, ActivatePopupList
            else if (anEvent.isKeyType()) {
                if (getPopup().isShowing() || anEvent.isShortcutDown()) return;
                if (anEvent.isControlChar() || anEvent.isSpaceKey()) return;
                runLater(() -> getTextArea().activatePopupList());
            }

            // Handle PopupTrigger
            else if (anEvent.isPopupTrigger()) { //anEvent.consume();
                Menu cmenu = createContextMenu();
                cmenu.show(_textArea, anEvent.getX(), anEvent.getY());
            }

            // Handle MouseClick: If alt-down, open JavaDoc. If HoverNode, open declaration
            else if (anEvent.isMouseClick()) {

                // If alt is down and there is JavaDoc, open it
                if (anEvent.isAltDown() && getJavaDoc() != null)
                    getJavaDoc().openUrl();

                // If there is a hover node, open it (and clear Hover)
                else if (getTextArea().getHoverNode() != null) {
                    openDeclaration(getTextArea().getHoverNode());
                    setTextAreaHoverEnabled(false);
                }
            }

            // Handle MouseMoved
            else if (anEvent.isMouseMove()) {
                if (!anEvent.isShortcutDown()) {
                    setTextAreaHoverEnabled(false);
                    return;
                }
                int index = _textArea.getCharIndexForXY(anEvent.getX(), anEvent.getY());
                JNode node = _textArea.getJFile().getNodeAtCharIndex(index);
                _textArea.setHoverNode(node instanceof JExprId || node instanceof JType ? node : null);
            }
        }

        // Handle JavaDocButton
        else if (anEvent.equals("JavaDocButton")) {
            JavaDoc javaDoc = getJavaDoc();
            if (javaDoc != null)
                javaDoc.openUrl();
        }

        // Handle FontSizeText, IncreaseFontButton, DecreaseFontButton
        else if (anEvent.equals("FontSizeText") || anEvent.equals("IncreaseFontButton") || anEvent.equals("DecreaseFontButton"))
            Prefs.getDefaultPrefs().setValue("JavaFontSize", _textArea.getFont().getSize());

        // Handle OpenDeclarationMenuItem
        else if (anEvent.equals("OpenDeclarationMenuItem"))
            openDeclaration(_textArea.getSelNode());

        // Handle ShowReferencesMenuItem
        else if (anEvent.equals("ShowReferencesMenuItem"))
            showReferences(_textArea.getSelNode());

        // Handle ShowDeclarationsMenuItem
        else if (anEvent.equals("ShowDeclarationsMenuItem"))
            showDeclarations(_textArea.getSelNode());

        // Handle NodePathLabel
        else if (anEvent.equals("NodePathLabel")) {
            JavaTextArea javaTextArea = getTextArea();
            JNode clickedNode = (JNode) anEvent.getView().getProp("JNode");
            JNode deepNode = javaTextArea.getDeepNode();
            javaTextArea.setSel(clickedNode.getStartCharIndex(), clickedNode.getEndCharIndex());
            javaTextArea.setDeepNode(deepNode);
        }
    }

    /**
     * Save file.
     */
    public void saveChanges()
    {
        // Hide Popup
        getPopup().hide();

        // Do normal version
        super.saveChanges();

        // Force reparse
        //getTextArea().getTextDoc().clearJFile();
    }

    /**
     * Returns the JavaDoc for currently selected node.
     */
    public JavaDoc getJavaDoc()
    {
        JNode selNode = _textArea.getSelNode();
        return JavaDoc.getJavaDocForNode(selNode);
    }

    /**
     * Sets whether MouseMoved over JavaTextArea should set hover node.
     */
    protected void setTextAreaHoverEnabled(boolean isEnabled)
    {
        if (isEnabled) enableEvents(_textArea, MouseMove);
        else disableEvents(_textArea, MouseMove);
        _textArea.setHoverNode(null);
    }

    /**
     * Override to turn off TextAreaHoverEnabled.
     */
    public void showLineNumberPanel()
    {
        super.showLineNumberPanel();
        setTextAreaHoverEnabled(false);
    }

    /**
     * Creates the ContextMenu.
     */
    protected Menu createContextMenu()
    {
        Menu cm = new Menu(); //cm.setAutoHide(true); cm.setConsumeAutoHidingEvents(true);
        MenuItem mi1 = new MenuItem();
        mi1.setText("Open Declaration");
        mi1.setName("OpenDeclarationMenuItem");
        MenuItem mi2 = new MenuItem();
        mi2.setText("Show References");
        mi2.setName("ShowReferencesMenuItem");
        MenuItem mi3 = new MenuItem();
        mi3.setText("Show Declarations");
        mi3.setName("ShowDeclarationsMenuItem");
        cm.addItem(mi1);
        cm.addItem(mi2);
        cm.addItem(mi3);
        cm.setOwner(this);
        return cm;
    }

    /**
     * Sets the TextSelection.
     */
    public void setTextSel(int aStart, int anEnd)
    {
        _textArea.setSel(aStart, anEnd);
    }

    /**
     * Override to add trailing colon.
     */
    @Override
    public String getSelectionInfo()  { return super.getSelectionInfo() + ": "; }

    /**
     * Open declaration.
     */
    public void openDeclaration(JNode aNode)  { }

    /**
     * Open a super declaration.
     */
    public void openSuperDeclaration(JMemberDecl aMemberDecl)  { }

    /**
     * Show References.
     */
    public void showReferences(JNode aNode)  { }

    /**
     * Show declarations.
     */
    public void showDeclarations(JNode aNode)  { }

    /**
     * Returns the ProgramCounter line.
     */
    public int getProgramCounterLine()  { return -1; }

    /**
     * Called when JavaTextArea changes.
     */
    protected void javaTextAreaDidPropChange(PropChange aPC)
    {
        String propName = aPC.getPropName();
        if (propName == JavaTextArea.SelectedNode_Prop)
            resetLater();
    }

    /**
     * Called when TextDoc changes.
     */
    private void textDocDidPropChange(PropChange aPC)
    {
        String propName = aPC.getPropName();
        if (propName == TextDoc.Chars_Prop) {

            // Update TextModified
            boolean hasUndos = getTextArea().getUndoer().hasUndos();
            setTextModified(hasUndos);

            // If added/removed newline, reset
            CharSequence chars = (CharSequence) (aPC.getNewValue() != null ? aPC.getNewValue() : aPC.getOldValue());
            if (CharSequenceUtils.indexOfNewline(chars, 0) >= 0)
                _lineHeaderView.resetAll();
            _lineFooterView.resetAll();
        }
    }

    /**
     * Called when a build/break-point marker changes.
     */
    public void buildIssueOrBreakPointMarkerChanged()
    {
        _lineHeaderView.resetAll();
        _lineFooterView.resetAll();
        _textArea.repaint();
    }

    /**
     * Returns labels for
     */
    protected Label[] getLabelsForSelNodePath()
    {
        return getLabelsForSelNodePath(_textArea, JFile.class);
    }

    /**
     * Returns an array of labels for selected JNode hierarchy.
     */
    public static Label[] getLabelsForSelNodePath(JavaTextArea javaTextArea, Class<? extends JNode> excludeClass)
    {
        // Get SelNode and DeepNode
        JNode selNode = javaTextArea.getSelNode();
        JNode deepNode = javaTextArea._deepNode;
        Font font = Font.Arial11;
        List<Label> pathLabels = new ArrayList<>();

        // Iterate up from DeepPart and add parts
        for (JNode jnode = deepNode; jnode != null; jnode = jnode.getParent()) {

            // Create label for node
            Label label = new Label();
            label.setName("NodePathLabel");
            label.setText(jnode.getNodeString());
            label.setFont(font);
            label.setProp("JNode", jnode);
            if (jnode == selNode)
                label.setFill(Color.LIGHTGRAY);
            pathLabels.add(0, label);

            // If last part, break
            JNode parentNode = jnode.getParent();
            if (parentNode == null || excludeClass != null && excludeClass.isAssignableFrom(parentNode.getClass()))
                break;

            // Add separator
            Label separator = new Label();
            separator.setText(" \u2022 ");
            separator.setFont(font);
            pathLabels.add(0, separator);
        }

        // Add Eval Type Name of selected node to end
        JavaDecl evalType = selNode != null ? selNode.getEvalType() : null;
        if (evalType != null) {
            String str = " (" + evalType.getSimpleName() + ')';
            Label classLabel = new Label();
            classLabel.setName("ClassLabel");
            classLabel.setText(str);
            classLabel.setFont(font);
            classLabel.setToolTip(evalType.getName());
            pathLabels.add(classLabel);
        }

        // Return
        return pathLabels.toArray(new Label[0]);
    }

    /**
     * Standard main implementation.
     */
    public static void main(String[] args)
    {
        // Get test file
        JavaTextDoc javaTextDoc = JavaTextDoc.newFromSource("/tmp/Test.java");

        // Create JavaPane and show
        JavaTextPane javaPane = new JavaTextPane();
        JavaTextArea javaTextArea = javaPane.getTextArea();
        javaTextArea.setTextDoc(javaTextDoc);
        javaPane.getUI().setPrefHeight(800);
        javaPane.setWindowVisible(true);
    }
}