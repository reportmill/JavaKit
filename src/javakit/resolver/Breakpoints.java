package javakit.resolver;

import snap.util.SnapList;
import snap.web.WebFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * A class to manage project breakpoints.
 */
public class Breakpoints extends SnapList<Breakpoint> {

    // The project
    Project _proj;

    /**
     * Creates new Breakpoints for Project.
     */
    public Breakpoints(Project aProj)
    {
        _proj = aProj;
        List<Breakpoint> bpoints = readFile();
        for (Breakpoint bp : bpoints) super.add(size(), bp);
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
        Breakpoint bp = super.remove(anIndex);
        writeFile();
        return bp;
    }

    /**
     * Adds a Breakpoint to file at line.
     */
    public void addBreakpoint(WebFile aFile, int aLine)
    {
        Breakpoint bp = new Breakpoint(aFile, aLine);
        int index = Collections.binarySearch(this, bp);
        if (index < 0) index = -index - 1;
        else return;
        add(index, bp);
    }

    /**
     * Returns the breakpoints for a given file.
     */
    public List<Breakpoint> get(WebFile aFile)
    {
        List bps = Collections.EMPTY_LIST;
        for (Breakpoint bp : this) {
            if (bp.getFile() == aFile) {
                if (bps == Collections.EMPTY_LIST) bps = new ArrayList();
                bps.add(bp);
            }
        }
        return bps;
    }

    /**
     * Reads breakpoints from file.
     */
    protected List<Breakpoint> readFile()
    {
        // Get breakpoint file and text
        WebFile file = getFile();
        if (!file.getExists()) return Collections.EMPTY_LIST;
        String text = file.getText();

        // Scan over text
        List<Breakpoint> list = new ArrayList();
        Scanner scanner = new Scanner(text);
        while (scanner.hasNext()) {
            String type = scanner.next();
            String path = scanner.next();
            int line = scanner.nextInt();
            WebFile f = _proj.getFile(path); // Was ProjectSet JK
            if (f != null)
                list.add(new Breakpoint(f, line));
        }
        scanner.close();

        // Return list
        return list;
    }

    /**
     * Writes breakpoints to file.
     */
    public void writeFile()
    {
        StringBuffer sb = new StringBuffer();
        for (Breakpoint bp : this) {
            sb.append(bp.getType()).append(' ');
            sb.append(bp.getFilePath()).append(' ');
            sb.append(bp.getLine()).append('\n');
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
        WebFile file = _proj.getSite().getSandbox().getFile("/settings/breakpoints");
        if (file == null) file = _proj.getSite().getSandbox().createFile("/settings/breakpoints", false);
        return file;
    }

}