/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.project;
import javakit.parse.*;
import snap.util.TaskMonitor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This JavaFileBuilder implementation simply resolves the node tree to find errors.
 */
public class JavaFileBuilderSimple extends JavaFileBuilder {

    // The errors
    private NodeError[]  _errors;

    /**
     * Constructor for given Project.
     */
    public JavaFileBuilderSimple(Project aProject)
    {
        super(aProject);
    }

    /**
     * Compiles files.
     */
    @Override
    public boolean buildFiles(TaskMonitor aTaskMonitor)
    {
        return true;
    }

    /**
     * Compile.
     */
    public void compile(JFile aJFile)
    {
        // Get errors
        List<NodeError> errorsList = new ArrayList<>();
        findNodeErrors(aJFile, errorsList);
        _errors = errorsList.toArray(NodeError.NO_ERRORS);
    }

    /**
     * Returns the errors.
     */
    public NodeError[] getErrors()  { return _errors; }

    /**
     * Recurse into nodes
     */
    private static void findNodeErrors(JNode aNode, List<NodeError> theErrors)
    {
        NodeError[] errors = aNode.getErrors();
        if (errors.length > 0)
            Collections.addAll(theErrors, errors);

        if (aNode instanceof JStmtExpr)
            return;

        List<JNode> children = aNode.getChildren();
        for (JNode child : children)
            findNodeErrors(child, theErrors);
    }
}
