/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.text;

import java.util.List;

import javakit.parse.JFile;
import javakit.parse.JImportDecl;
import javakit.parse.JNode;
import javakit.parse.JavaDecl;
import snap.gfx.*;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.text.*;
import snap.view.*;

/**
 * A PopupList for an JavaTextPane.
 */
public class JavaPopupList extends PopupList<JavaDecl> {

    // The JavaTextArea
    JavaTextArea _textArea;

    // The current selection start
    int _selStart;

    /**
     * Creates a new java popup for given JavaTextArea.
     */
    public JavaPopupList(JavaTextArea aJavaTextArea)
    {
        ListArea<JavaDecl> listArea = getListArea();
        listArea.setCellConfigure(lc -> configureCell(lc));

        _textArea = aJavaTextArea;
        setPrefWidth(500);
        setPrefRowCount(15);
    }

    /**
     * Returns the JavaTextArea.
     */
    public JavaTextArea getTextArea()
    {
        return _textArea;
    }

    /**
     * Applies the current suggestion.
     */
    public void applySuggestion()
    {
        applySuggestion(getSelItem());
    }

    /**
     * Applies a suggestion.
     */
    public void applySuggestion(JavaDecl aDecl)
    {
        // Add suggestion text
        JavaTextArea textArea = getTextArea();
        JNode selectedNode = textArea.getSelectedNode();
        String completion = aDecl.getReplaceString();
        int selStart = selectedNode.getStart();
        textArea.replaceChars(completion, null, selStart, textArea.getSelEnd(), false);
        int argStart = completion.indexOf('('), argEnd = argStart > 0 ? completion.indexOf(')', argStart) : -1;
        if (argEnd > argStart + 1) textArea.setSel(selStart + argStart + 1, selStart + argEnd);

        // Add import for suggestion Class, if not present
        addImport(aDecl, selectedNode.getFile());

        // Hide PopupList
        hide();
    }

    /**
     * Inserts the import statement for suggestion into text, if missing.
     */
    protected void addImport(JavaDecl aDecl, JFile aFile)
    {
        // Handle ClassName suggestion
        if (aDecl.isClass() || aDecl.isConstructor()) {
            String cname = aDecl.getClassName(), csname = aDecl.getSimpleName();
            String cname2 = aFile.getImportClassName(csname);
            if (cname2 == null || !cname2.equals(cname)) {
                String cpath = cname.replace('$', '.'), istring = "import " + cpath + ";\n";
                List<JImportDecl> imports = aFile.getImportDecls();
                int is = aFile.getPackageDecl() != null ? aFile.getPackageDecl().getLineIndex() + 1 : 0;
                for (JImportDecl i : imports) {
                    if (cpath.compareTo(i.getName()) < 0) break;
                    else is = i.getLineIndex() + 1;
                }
                TextBoxLine line = getTextArea().getLine(is);
                getTextArea().replaceChars(istring, null, line.getStart(), line.getStart(), false);
            }
        }
    }

    /**
     * Override to register for textArea property change.
     */
    public void show(View aView, double aX, double aY)
    {
        super.show(aView, aX, aY);
        _textArea.addPropChangeListener(_textAreaLsnr);
        _selStart = _textArea.getSelStart();
    }

    /**
     * Override to unregister property change.
     */
    public void hide()
    {
        super.hide();
        _textArea.removePropChangeListener(_textAreaLsnr);
    }

    // PropChangeListner for TextArea prop changes
    PropChangeListener _textAreaLsnr = pce -> textAreaPropChange(pce);

    /**
     * Catch TextArea Selection changes that should cause Popup to close.
     */
    public void textAreaPropChange(PropChange anEvent)
    {
        // If not showing, unregister (in case we were PopupList was dismissed without hide)
        if (!isShowing()) {
            _textArea.removePropChangeListener(_textAreaLsnr);
            return;
        }

        // If Selection change, update or hide
        if (anEvent.getPropertyName().equals("Selection")) {
            int start = _textArea.getSelStart(), end = _textArea.getSelEnd();
            if (start != end || !(start == _selStart + 1 || start == _selStart - 1))
                hide();
            _selStart = start;
        }
    }

    /**
     * Override to select first item and resize.
     */
    public void setItems(List<JavaDecl> theItems)
    {
        super.setItems(theItems);
        if (theItems != null && theItems.size() > 0)
            setSelIndex(0);
    }

    /**
     * Configure cells for this PopupList.
     */
    protected void configureCell(ListCell<JavaDecl> aCell)
    {
        JavaDecl item = aCell.getItem();
        if (item == null) return;
        aCell.setText(item.getSuggestionString());
        aCell.setImage(getSuggestionImage(item));
    }

    /**
     * Override to apply suggestion.
     */
    protected void fireActionEvent(ViewEvent anEvent)
    {
        applySuggestion();
    }

    /**
     * Returns an icon for suggestion.
     */
    public static Image getSuggestionImage(JavaDecl aDecl)
    {
        switch (aDecl.getType()) {
            case VarDecl:
                return JavaTextBox.LVarImage;
            case Field:
                return JavaTextBox.FieldImage;
            case Method:
                return JavaTextBox.MethodImage;
            case Class:
                return JavaTextBox.ClassImage;
            case Package:
                return JavaTextBox.PackageImage;
            default:
                return null;
        }
    }
}