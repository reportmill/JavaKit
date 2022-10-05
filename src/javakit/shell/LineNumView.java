package javakit.shell;
import snap.geom.HPos;
import snap.geom.Insets;
import snap.gfx.Color;
import snap.text.TextDoc;
import snap.text.TextLineStyle;
import snap.view.TextArea;

/**
 * A TextArea subclass to show line numbers.
 */
class LineNumView extends TextArea {

    // The textArea
    private TextArea _textArea;

    /**
     * Constructor.
     */
    public LineNumView(TextArea aTextArea)
    {
        _textArea = aTextArea;
        setTextDoc(new TextDoc());

        setDefaultStyle(getDefaultStyle().copyFor(Color.GRAY5));
        setFill(new Color(.98));
        setPrefWidth(25);
        setEditable(false);

        // Set LineStyle to match TextArea but right align
        TextLineStyle lineStyle = aTextArea.getTextDoc().getDefaultLineStyle();
        TextLineStyle lineStyleRight = lineStyle.copyFor(HPos.RIGHT);
        setDefaultLineStyle(lineStyleRight);

        // Set Padding to match TextArea
        Insets textPadding = aTextArea.getPadding();
        setPadding(textPadding);
    }

    /**
     * Called to update when TextArea changes.
     */
    void updateLines()
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 1, iMax = _textArea.getLineCount(); i <= iMax; i++)
            sb.append(i).append('\n');
        setText(sb.toString());
    }
}
