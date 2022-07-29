/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.text;

import javakit.parse.*;
import javakit.reflect.JavaDecl;
import javakit.resolver.Project;
import snap.gfx.*;
import snap.util.*;
import snap.view.*;
import snap.viewx.TextPane;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;

/**
 * A panel for editing Java files.
 */
public class JavaTextPane extends TextPane {

    // The JavaTextArea
    JavaTextArea _textArea;

    // The SplitView
    SplitView _splitView;

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
     * Returns the CodeBuilder.
     */
    public CodeBuilder getCodeBuilder()
    {
        return getTextArea().getCodeBuilder();
    }

    /**
     * Returns whether CodeBuilder is visible.
     */
    public boolean isCodeBuilderVisible()
    {
        return _splitView.getItemCount() > 1;
    }

    /**
     * Sets whether CodeBuilder is visible.
     */
    public void setCodeBuilderVisible(boolean aFlag)
    {
        // If already set, just return
        if (aFlag == isCodeBuilderVisible()) return;
        View codeBuildrPane = getCodeBuilder().getUI();

        // If showing CodeBuilder, add to SplitView (animated)
        if (aFlag) {
            _splitView.addItemWithAnim(codeBuildrPane, 260);
            getCodeBuilder().setCodeBlocks();
        }

        // If hiding CodeBuilder, remove from SplitView (animated)
        else if (_splitView.getItemCount() > 1)
            _splitView.removeItemWithAnim(codeBuildrPane);
    }

    /**
     * Initialize UI panel.
     */
    protected void initUI()
    {
        // Do normal version
        super.initUI();

        // Get TextArea and start listening for events (KeyEvents, MouseReleased, DragOver/Exit/Drop)
        _textArea = getTextArea();
        _textArea._textPane = this;
        _textArea.setGrowWidth(true);
        enableEvents(_textArea, KeyPress, KeyRelease, KeyType, MousePress, MouseRelease, DragOver, DragExit, DragDrop);

        // Reset TextArea font
        double fontSize = Prefs.get().getDouble("JavaFontSize", 12);
        if (fontSize < 8) fontSize = 12;
        _textArea.setFont(new Font(_textArea.getDefaultFont().getName(), fontSize));

        // Get TextArea.RowHeader and configure
        RowHeader rowHeader = createRowHeader();
        rowHeader.setTextArea(_textArea);
        _textArea._rowHeader = rowHeader;

        // Get ScrollView and add RowHeader
        ScrollView spane = getView("ScrollView", ScrollView.class);
        spane.setGrowWidth(true);
        RowView hbox = new RowView();
        hbox.setFillHeight(true);
        hbox.setChildren(rowHeader, _textArea);
        spane.setContent(hbox);

        // Get SplitView and add ScrollView and CodeBuilder
        _splitView = new SplitView();
        _splitView.addItem(spane);
        getUI(BorderView.class).setCenter(_splitView);

        // Get OverviewPane and set JavaTextArea
        OverviewPane overviewPane = createOverviewPane();
        overviewPane.setTextArea(_textArea);
        _textArea._overviewPane = overviewPane;
        getUI(BorderView.class).setRight(overviewPane);
    }

    /**
     * Reset UI.
     */
    protected void resetUI()
    {
        // Do normal version
        super.resetUI();

        // Reset FontSizeText
        setViewValue("FontSizeText", getTextArea().getFont().getSize());

        // Clear path box and add Lin/Col postion label
        RowView nodePathBox = getView("BottomBox", RowView.class);
        while (nodePathBox.getChildCount() > 1) nodePathBox.removeChild(1);
        Font font = Font.get("Arial", 11);

        // Iterate up from DeepPart and add parts
        JNode deepNode = getTextArea()._deepNode, selNode = getTextArea().getSelectedNode();
        for (JNode part = deepNode, spart = selNode; part != null; part = part.getParent()) {
            Label label = new Label();
            label.setText(part.getNodeString());
            label.setFont(font);
            label.setName("NodePathLabel");
            label.setProp("JNode", part);
            if (part == spart) label.setFill(Color.LIGHTGRAY);
            nodePathBox.addChild(label, 1);
            label.setOwner(this);
            enableEvents(label, MouseRelease);
            Label div = new Label();
            div.setText(" \u2022 ");
            div.setFont(font);
            if (part.getParent() == null) break;
            nodePathBox.addChild(div, 1);
        }

        // Add Eval Type Name of selected node to end
        JavaDecl etype = selNode != null ? selNode.getEvalType() : null;
        if (etype != null) {
            String str = " (" + etype.getSimpleName() + ')';
            Label label = new Label();
            label.setText(str);
            label.setFont(font);
            label.setToolTip(etype.getName());
            nodePathBox.addChild(label);
        }
    }

    /**
     * Get compile info.
     */
    public String getSelectionInfo()
    {
        return super.getSelectionInfo() + ": ";
    }

    /**
     * Respond to UI controls.
     */
    public void respondUI(ViewEvent anEvent)
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

                // If alt is down and there is a JavaDoc URL, open it
                if (anEvent.isAltDown() && getJavaDocURL() != null)
                    URLUtils.openURL(getJavaDocURL());

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
                int index = _textArea.getCharIndex(anEvent.getX(), anEvent.getY());
                JNode node = _textArea.getJFile().getNodeAtCharIndex(index);
                _textArea.setHoverNode(node instanceof JExprId || node instanceof JType ? node : null);
            }

            // Handle DragOver, DragExit, DragDrop
            else if (anEvent.isDragOver()) getCodeBuilder().dragOver(anEvent.getX(), anEvent.getY());
            else if (anEvent.isDragExit()) getCodeBuilder().dragExit();
            else if (anEvent.isDragDropEvent()) getCodeBuilder().drop(0, 0);
        }

        // Handle JavaDocButton
        else if (anEvent.equals("JavaDocButton")) URLUtils.openURL(getJavaDocURL());

            // Handle CodeBuilderButton
        else if (anEvent.equals("CodeBuilderButton")) setCodeBuilderVisible(!isCodeBuilderVisible());

            // Handle FontSizeText, IncreaseFontButton, DecreaseFontButton
        else if (anEvent.equals("FontSizeText") || anEvent.equals("IncreaseFontButton") || anEvent.equals("DecreaseFontButton"))
            Prefs.get().setValue("JavaFontSize", _textArea.getFont().getSize());

            // Handle OpenDeclarationMenuItem
        else if (anEvent.equals("OpenDeclarationMenuItem"))
            openDeclaration(_textArea.getSelectedNode());

            // Handle ShowReferencesMenuItem
        else if (anEvent.equals("ShowReferencesMenuItem"))
            showReferences(_textArea.getSelectedNode());

            // Handle ShowDeclarationsMenuItem
        else if (anEvent.equals("ShowDeclarationsMenuItem"))
            showDeclarations(_textArea.getSelectedNode());

            // Handle NodePathLabel
        else if (anEvent.equals("NodePathLabel")) {
            JNode part = (JNode) anEvent.getView().getProp("JNode"), dnode = _textArea._deepNode;
            _textArea.setSel(part.getStart(), part.getEnd());
            _textArea._deepNode = dnode;
        }
    }

    /**
     * Save file.
     */
    public void saveChanges()
    {
        getPopup().hide(); // Close popup
        super.saveChanges();
        getTextArea().getTextBox().reloadSymbols();
    }

    /**
     * Returns the string for the JavaDocButton. Called from binding set up in rib file.
     */
    public String getJavaDocText()
    {
        // Get class name for selected JNode
        Class cls = _textArea.getSelectedNodeClass();
        if (cls == null)
            return null;
        if (cls.isArray())
            cls = cls.getComponentType();

        // Iterate up through class parents until URL found or null
        while (cls != null) {
            String url = getJavaDocURL(cls);
            if (url != null) return cls.getSimpleName() + " Doc";
            Class scls = cls.getSuperclass();
            cls = scls != null && scls != Object.class ? scls : null;
        }
        return null;
    }

    /**
     * Returns the JavaDoc url for currently selected type.
     */
    public String getJavaDocURL()
    {
        // Get class name for selected JNode
        Class cls = _textArea.getSelectedNodeClass();
        if (cls == null) return null;
        if (cls.isArray()) cls = cls.getComponentType();

        // Iterate up through class parents until URL found or null
        while (cls != null) {
            String url = getJavaDocURL(cls);
            if (url != null) return url;
            Class scls = cls.getSuperclass();
            cls = scls != null && scls != Object.class ? scls : null;
        }
        return null;
    }

    /**
     * Returns the JavaDoc url for currently selected type.
     */
    public String getJavaDocURL(Class aClass)
    {
        // Get class name for selected JNode
        String cname = aClass.getName();

        // Handle reportmill class
        String url = null;
        if (cname.startsWith("snap."))
            url = "http://reportmill.com/snap1/javadoc/index.html?" + cname.replace('.', '/') + ".html";
        else if (cname.startsWith("com.reportmill."))
            url = "http://reportmill.com/rm14/javadoc/index.html?" + cname.replace('.', '/') + ".html";

            // Handle standard java classes
        else if (cname.startsWith("java.") || cname.startsWith("javax."))
            url = "http://docs.oracle.com/javase/8/docs/api/index.html?" + cname.replace('.', '/') + ".html";

            // Handle JavaFX classes
        else if (cname.startsWith("javafx."))
            url = "http://docs.oracle.com/javafx/2/api/index.html?" + cname.replace('.', '/') + ".html";

            // Handle Greenfoot classes
        else if (cname.startsWith("greenfoot."))
            url = "https://www.greenfoot.org/files/javadoc/index.html?" + cname.replace('.', '/') + ".html";

        // Return url
        return url;
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
     * Creates the RowHeader.
     */
    protected RowHeader createRowHeader()
    {
        return new RowHeader();
    }

    /**
     * Creates the OverviewPane.
     */
    protected OverviewPane createOverviewPane()
    {
        return new OverviewPane();
    }

    /**
     * Sets the TextSelection.
     */
    public void setTextSel(int aStart, int anEnd)
    {
        _textArea.setSel(aStart, anEnd);
    }

    /**
     * Open declaration.
     */
    public void openDeclaration(JNode aNode)
    {
    }

    /**
     * Open a super declaration.
     */
    public void openSuperDeclaration(JMemberDecl aMemberDecl)
    {
    }

    /**
     * Show References.
     */
    public void showReferences(JNode aNode)
    {
    }

    /**
     * Show declarations.
     */
    public void showDeclarations(JNode aNode)
    {
    }

    /**
     * Returns the ProgramCounter line.
     */
    public int getProgramCounterLine()
    {
        return -1;
    }

    /**
     * Standard main implementation.
     */
    public static void main(String[] args)
    {
        // Get Scratch Site
        WebSite scratchSite = WebURL.getURL("/tmp/ScratchProj").getAsSite();
        Project proj = new Project(scratchSite);

        // Get test file
        WebFile testFile = proj.getSourceFile("/Test.java", false, false);

        // Create JavaPane and show
        JavaTextPane javaPane = new JavaTextPane();
        javaPane.getTextArea().setSource(testFile);
        javaPane.getUI().setPrefHeight(800);
        javaPane.setWindowVisible(true);
    }
}