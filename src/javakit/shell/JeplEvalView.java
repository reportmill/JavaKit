package javakit.shell;
import snap.geom.Insets;
import snap.gfx.Color;
import snap.text.TextDoc;
import snap.view.ColView;
import snap.view.TextArea;
import snap.view.View;

/**
 * A TextArea subclass to show code evaluation.
 */
class JeplEvalView extends ColView {

    // The JeplTextPane
    private JeplTextPane _jeplTextPane;

    /**
     * Constructor.
     */
    public JeplEvalView(JeplTextPane aJTP)
    {
        _jeplTextPane = aJTP;
        setSpacing(4);
        //setTextDoc(new TextDoc());
        //setDefaultStyle(getDefaultStyle().copyFor(aJTP.getCodeFont()));
        setFill(new Color(.98));
        //setTextFill(Color.GRAY); //setPrefWidth(200);
        //setEditable(false);

        // Set Padding to match TextArea
        Insets textPadding = aJTP.getTextArea().getPadding();
        setPadding(textPadding);
    }

    /**
     * Called to update when textView changes.
     */
    void updateLines()
    {
        removeChildren();

        // Get JavaShell and line values
        JeplDoc jeplDoc = _jeplTextPane._jeplDoc;
        View[] lineViews = jeplDoc.getReplViews();
        if (lineViews == null)
            return;

        // Iterate over LineVals and append string for each
        for (View view : lineViews) {

            // Handle null
            if (view == null) continue;
            addChild(view);
        }
    }
}
