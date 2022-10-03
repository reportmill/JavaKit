package javakit.shell;
import snap.geom.HPos;
import snap.gfx.Color;
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
        setDefaultLineStyle(TextLineStyle.DEFAULT.copyFor(HPos.RIGHT));
        setFill(new Color("#f7f7f7"));
        setTextFill(new Color(.6f));
        setPrefWidth(25);
        setPadding(2, 4, 2, 2);
        setEditable(false);
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
