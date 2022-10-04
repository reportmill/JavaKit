package javakit.shell;
import javakit.text.JavaTextArea;
import snap.view.Clipboard;

/**
 * This JavaTextArea subclass is slightly modified for working with code snippets.
 */
class JavaTextAreaX extends JavaTextArea {

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
        if (theContent instanceof String)
            theContent = removeExtraIndentFromString((String) theContent);

        // Do normal version
        super.replaceCharsWithContent(theContent);
    }

    /**
     * Removes superfluous indent from a string.
     */
    private static String removeExtraIndentFromString(String str)
    {
        // If starts with newline, strip it
        if (str.startsWith("\n"))
            str = str.substring(1);

        // Get string as lines
        String[] lines = str.split("\n");
        int minIndent = 99;

        // Get minimum indent for given lines
        for (String line : lines) {
            if (line.trim().length() == 0)
                continue;
            int indent = 0;
            for (int i = 0; i < line.length(); i++) {
                if (line.charAt(i) == ' ')
                    indent++;
                else break;
            }
            minIndent = Math.min(minIndent, indent);
        }

        // If there is superfluous indent, remove from lines and reset string
        if (minIndent > 0) {

            // Get indent string
            String indentStr = " ";
            for (int i = 1; i < minIndent; i++) indentStr += ' ';

            // Remove indent string from lines
            for (int i = 0; i < lines.length; i++)
                lines[i] = lines[i].replaceFirst(indentStr, "");

            // Rebuild string
            str = String.join("\n", lines);
        }

        // Return
        return str;
    }
}
