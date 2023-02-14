/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.project;
import snap.util.SnapList;
import snap.web.WebFile;
import snap.web.WebSite;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * A class to manage project breakpoints.
 */
public class Breakpoints extends SnapList<Breakpoint> {

    // The Workspace
    private Workspace  _workspace;

    // An Empty array of BuildIssues
    public static final Breakpoint[] NO_BREAKPOINTS = new Breakpoint[0];

    /**
     * Constructor.
     */
    public Breakpoints(Workspace workspace)
    {
        super();
        _workspace = workspace;

        // Read from file and add to this list
        //List<Breakpoint> breakpoints = readFile();
        //for (Breakpoint bp : breakpoints)
        //    super.add(size(), bp);
    }

    /**
     * Adds a breakpoint.
     */
    public void add(int anIndex, Breakpoint aBP)
    {
        super.add(anIndex, aBP);
        writeFile();
    }

    /**
     * Removes a breakpoint.
     */
    public Breakpoint remove(int anIndex)
    {
        Breakpoint breakpoint = super.remove(anIndex);
        writeFile();
        return breakpoint;
    }

    /**
     * Adds a Breakpoint to file at line.
     */
    public void addBreakpointForFile(WebFile aFile, int aLine)
    {
        Breakpoint breakpoint = new Breakpoint(aFile, aLine);
        int index = Collections.binarySearch(this, breakpoint);
        if (index < 0) {
            index = -index - 1;
            add(index, breakpoint);
        }
    }

    /**
     * Returns the breakpoints for a given file.
     */
    public Breakpoint[] getBreakpointsForFile(WebFile aFile)
    {
        // If no BreakPoints, just return
        if (size() == 0) return NO_BREAKPOINTS;

        // Filter to get breakpoints for given file
        Breakpoint[] breakpoints = stream().filter(bp -> bp.getFile() == aFile).toArray(size -> new Breakpoint[size]);
        return breakpoints;
    }

    /**
     * Reads breakpoints from file.
     */
    protected List<Breakpoint> readFile()
    {
        // Get breakpoint file and text
        WebFile breakpointFile = getFile();
        if (!breakpointFile.getExists())
            return Collections.EMPTY_LIST;

        // Get vars
        String text = breakpointFile.getText();
        Scanner scanner = new Scanner(text);
        List<Breakpoint> breakpointsList = new ArrayList<>();

        // Iterate over text
        while (scanner.hasNext()) {

            // Get Breakpoint Type, Path, LineNum
            String type = scanner.next();
            String path = scanner.next();
            int lineNum = scanner.nextInt();

            // Get Breakpoint source file (just continue if file no longer found in Workspace)
            Project rootProj = _workspace.getRootProject();
            WebFile sourceFile = rootProj.getFile(path); // Should be checking whole Workspace
            if (sourceFile == null)
                continue;

            // Create/add new breakpoint
            Breakpoint breakpoint = new Breakpoint(sourceFile, lineNum);
            breakpointsList.add(breakpoint);
        }

        // Return
        return breakpointsList;
    }

    /**
     * Writes breakpoints to file.
     */
    public void writeFile()
    {
        // Create file text
        StringBuilder sb = new StringBuilder();
        for (Breakpoint breakpoint : this) {
            sb.append(breakpoint.getType()).append(' ');
            sb.append(breakpoint.getFilePath()).append(' ');
            sb.append(breakpoint.getLine()).append('\n');
        }

        // Get file, set text and save
        WebFile file = getFile();
        file.setText(sb.toString());
        try { file.save(); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * Returns the breakpoint file.
     */
    protected WebFile getFile()
    {
        Project rootProj = _workspace.getRootProject();
        WebSite projSite = rootProj.getSite();
        WebSite sandboxSite = projSite.getSandbox();

        // Get Sandbox breakpoints file
        WebFile breakpointsFile = sandboxSite.getFileForPath("/settings/breakpoints");
        if (breakpointsFile == null)
            breakpointsFile = sandboxSite.createFileForPath("/settings/breakpoints", false);

        // Return
        return breakpointsFile;
    }
}