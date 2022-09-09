/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.text;
import java.util.List;
import javakit.parse.JFile;
import javakit.parse.JImportDecl;
import javakit.parse.JNode;
import javakit.reflect.JavaClass;
import javakit.reflect.JavaConstructor;
import javakit.reflect.JavaDecl;
import snap.geom.Insets;
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
    private JavaTextArea  _textArea;

    // The current selection start
    private int  _selStart;

    // PropChangeListner for TextArea prop changes
    private PropChangeListener  _textAreaLsnr = pce -> textAreaPropChange(pce);

    // Constants
    private static Color BACKGROUND_COLOR = Color.get("#FC");
    private static Color CELL_TEXT_FILL = Color.get("#28");

    /**
     * Creates a new java popup for given JavaTextArea.
     */
    public JavaPopupList(JavaTextArea aJavaTextArea)
    {
        ListArea<JavaDecl> listArea = getListArea();
        listArea.setCellConfigure(lc -> configureCell(lc));
        listArea.setCellPadding(new Insets(0, 2, 2, 2));
        listArea.setRowHeight(18);
        listArea.setFill(BACKGROUND_COLOR);
        listArea.setAltPaint(BACKGROUND_COLOR);

        _textArea = aJavaTextArea;
        setPrefWidth(500);
        setPrefRowCount(12);

        // Set font
        TextDoc textDoc = aJavaTextArea.getTextDoc();
        Font font = textDoc.getDefaultStyle().getFont();
        listArea.setFont(font);
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
        // Get completeString
        String completion = aDecl.getReplaceString();

        // Get start/stop char index for completion (adjust for SubText if needed)
        JavaTextArea textArea = getTextArea();
        TextDoc textDoc = textArea.getTextDoc();
        JNode selNode = textArea.getSelNode();
        int selStart = selNode.getStart() - textDoc.getStartCharIndex();
        int selEnd = textArea.getSelEnd();

        // Replace selection with completeString
        textArea.replaceChars(completion, null, selStart, selEnd, false);

        // If complete string has args, select inside
        int argStart = completion.indexOf('(');
        if (argStart > 0) {
            int argEnd = completion.indexOf(')', argStart);
            if (argEnd > argStart + 1)
                textArea.setSel(selStart + argStart + 1, selStart + argEnd);
        }

        // Add import for suggestion Class, if not present
        JFile jfile = selNode.getFile();
        addImport(aDecl, jfile);

        // Hide PopupList
        hide();
    }

    /**
     * Inserts the import statement for suggestion into text, if missing.
     */
    protected void addImport(JavaDecl aDecl, JFile aFile)
    {
        // Handle ClassName suggestion
        if (aDecl instanceof JavaClass || (aDecl instanceof JavaConstructor)) {

            // Get
            String className = aDecl instanceof JavaClass ? ((JavaClass) aDecl).getClassName() :
                    ((JavaConstructor) aDecl).getDeclaringClassName();
            String simpleName = aDecl.getSimpleName();
            String importClassName = aFile.getImportClassName(simpleName);

            if (importClassName == null || !importClassName.equals(className)) {

                String classPath = className.replace('$', '.');
                String importStr = "import " + classPath + ";\n";
                List<JImportDecl> imports = aFile.getImportDecls();
                int is = aFile.getPackageDecl() != null ? aFile.getPackageDecl().getLineIndex() + 1 : 0;

                for (JImportDecl imp : imports) {
                    if (classPath.compareTo(imp.getName()) < 0)
                        break;
                    is = imp.getLineIndex() + 1;
                }

                // Add import
                TextArea textArea = getTextArea();
                TextBoxLine line = textArea.getLine(is);
                textArea.addChars(importStr, null, line.getStart(), false);
            }
        }
    }

    /**
     * Override to register for textArea property change.
     */
    public void show(View aView, double aX, double aY)
    {
        // Shift X by image width
        aX -= 24;

        // Do normal version
        super.show(aView, aX, aY);

        // Start listening to TextArea
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

    /**
     * Catch TextArea Selection changes that should cause Popup to close.
     */
    public void textAreaPropChange(PropChange aPC)
    {
        // If not showing, unregister (in case we were PopupList was dismissed without hide)
        if (!isShowing()) {
            _textArea.removePropChangeListener(_textAreaLsnr);
            return;
        }

        // If Selection change, update or hide
        String propName = aPC.getPropName();
        if (propName == TextArea.Selection_Prop) {
            int start = _textArea.getSelStart();
            int end = _textArea.getSelEnd();
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
        // Get cell item
        JavaDecl item = aCell.getItem();
        if (item == null) return;

        // Get/set cell text
        String cellText = item.getSuggestionString();
        aCell.setText(cellText);
        aCell.setTextFill(CELL_TEXT_FILL);

        // Get/set cell image
        Image cellImage = JavaTextUtils.getImageForJavaDecl(item);
        aCell.setImage(cellImage);
    }

    /**
     * Override to apply suggestion.
     */
    protected void fireActionEvent(ViewEvent anEvent)
    {
        applySuggestion();
    }
}