/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.ide;
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

    // The project
    private Project  _proj;

    // An Empty array of BuildIssues
    public static final Breakpoint[] NO_BREAKPOINTS = new Breakpoint[0];

    /**
     * Creates new Breakpoints for Project.
     */
    public Breakpoints(Project aProj)
    {
        _proj = aProj;
        List<Breakpoint> breakpoints = readFile();
        for (Breakpoint bp : breakpoints)
            super.add(size(), bp);
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
        WebFile file = getFile();
        if (!file.getExists())
            return Collections.EMPTY_LIST;

        // Get vars
        String text = file.getText();
        Scanner scanner = new Scanner(text);
        List<Breakpoint> breakpointsList = new ArrayList<>();

        // Iterate over text
        while (scanner.hasNext()) {
            String type = scanner.next();
            String path = scanner.next();
            int lineNum = scanner.nextInt();
            WebFile breakpointFile = _proj.getFile(path); // Was ProjectSet JK
            if (breakpointFile != null)
                breakpointsList.add(new Breakpoint(breakpointFile, lineNum));
        }
        scanner.close();

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
        try {
            file.save();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the breakpoint file.
     */
    protected WebFile getFile()
    {
        WebSite projSite = _proj.getSite();
        WebSite sandboxSite = projSite.getSandbox();
        WebFile file = sandboxSite.getFileForPath("/settings/breakpoints");
        if (file == null)
            file = sandboxSite.createFile("/settings/breakpoints", false);
        return file;
    }
}