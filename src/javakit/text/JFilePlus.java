package javakit.text;

import javakit.parse.JFile;
import javakit.parse.JNode;
import javakit.parse.JStmtBlock;
import snap.parse.*;

/**
 * A JFile node that can add/remove chars.
 */
public class JFilePlus extends JFile {

    // A JavaText
    JavaTextBox _jtbox;

    // A statement parser
    StmtParser _stmtParser;

    /**
     * Creates a JFilePlus from a JNode.
     */
    JFilePlus(JavaTextBox aJavaText, JFile aJFile)
    {
        _jtbox = aJavaText;
        init(aJFile);
        _stmtParser = new StmtParser();
    }

    /**
     * Updates this JFile for given range change.
     */
    void updateChars(int aStart, int endOld, int endNew)
    {
        // Get node and statement at index
        JNode jnode = getNodeAtCharIndex(aStart);
        int len = endNew - endOld;
        JStmtBlock stmt = jnode instanceof JStmtBlock ? (JStmtBlock) jnode : jnode.getParent(JStmtBlock.class);

        // If adding more than 50 chars, just re-parse. If removing, get outer statement enclosing range
        if (len > 50) stmt = null;
        else while (stmt != null && stmt.getEnd() < endOld)
            stmt = stmt.getParent(JStmtBlock.class);

        // Replace new statement
        replace(stmt);
    }

    /**
     * Replace statement.
     */
    void replace(JStmtBlock aStmt)
    {
        // If statement exists, re-parse and replace
        if (aStmt != null) { //System.out.println("Replacing: " + aStmt.getString().replace("\n", "  "));

            // Parse new JStmtBlock (create empty one if there wasn't enough in block to create it)
            _stmtParser.setInput(_jtbox.getRichText());
            _stmtParser.setCharIndex(aStmt.getStart());
            JStmtBlock stmt2 = null;
            try {
                stmt2 = _stmtParser.parseCustom(JStmtBlock.class);
            } catch (Exception e) {
            }
            if (stmt2 == null) {
                stmt2 = new JStmtBlock();
                stmt2.setStartToken(aStmt.getStartToken());
            }

            // Replace old statement with new statement
            JNode spar = aStmt.getParent();
            stmt2.setEndToken(aStmt.getEndToken());
            spar.setBlock(stmt2);
        }

        // Otherwise, do full parse
        //else _jtbox._jfile = null; //System.out.println("Full Parse");
    }

    /**
     * A Parser for JavaText modified statements.
     */
    public class StmtParser extends Parser {

        /**
         * Create new StmtParser.
         */
        StmtParser()
        {
            super(_jtbox._parser.getRule("Statement"));
        }

        /**
         * Returns tokenizer that gets tokens from text.
         */
        public Tokenizer getTokenizer()
        {
            return _jtbox._parser.getTokenizer();
        }

        /**
         * Override to ignore exception.
         */
        protected void parseFailed(ParseRule aRule, ParseHandler aHandler)
        {
        }
    }
}