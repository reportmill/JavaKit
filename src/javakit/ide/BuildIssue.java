/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.ide;

import snap.web.WebFile;

/**
 * This class represents a build error (or warning) for a file.
 */
public class BuildIssue implements Comparable<BuildIssue> {

    // The file that has the error
    WebFile _file;

    // The status
    Kind _kind;

    // The error text
    String _text;

    // The line and column index
    int _line, _column;

    // The character start and end
    int _start, _end;

    // Constants for kind of issue
    public enum Kind {Error, Warning, Note}

    /**
     * Creates a new error.
     */
    public BuildIssue init(WebFile aFile, Kind aKind, String theText, int aLine, int aColumn, int aStart, int anEnd)
    {
        _file = aFile;
        _kind = aKind;
        _text = theText;
        _line = aLine;
        _column = aColumn;
        _start = aStart;
        _end = anEnd;
        return this;
    }

    /**
     * Returns the file.
     */
    public WebFile getFile()
    {
        return _file;
    }

    /**
     * Returns the kind.
     */
    public Kind getKind()
    {
        return _kind;
    }

    /**
     * Returns whether issue is error.
     */
    public boolean isError()
    {
        return getKind() == Kind.Error;
    }

    /**
     * Returns the error text.
     */
    public String getText()
    {
        return _text;
    }

    /**
     * Returns the line index.
     */
    public int getLine()
    {
        return _line;
    }

    /**
     * Returns the column index.
     */
    public int getColumn()
    {
        return _column;
    }

    /**
     * Returns the start char index.
     */
    public int getStart()
    {
        return _start;
    }

    /**
     * Sets the start char index.
     */
    public void setStart(int aStart)
    {
        _start = aStart;
    }

    /**
     * Returns the end char index.
     */
    public int getEnd()
    {
        return _end;
    }

    /**
     * Returns the end char index.
     */
    public void setEnd(int anEnd)
    {
        _end = anEnd;
    }

    /**
     * Returns the line number (convenience).
     */
    public int getLineNumber()
    {
        return _line + 1;
    }

    /**
     * Standard compareTo implementation.
     */
    public int compareTo(BuildIssue aBI)
    {
        Kind k1 = getKind(), k2 = aBI.getKind();
        if (k1 != k2) return k1.ordinal() - k2.ordinal();
        int comp = getFile().compareTo(aBI.getFile());
        if (comp != 0) return comp;
        int l1 = getLine(), l2 = aBI.getLine();
        if (l1 != l2) return l2 - l1;
        int s1 = getStart(), s2 = aBI.getStart();
        if (s1 != s2) return s2 - s1;
        return getText().compareTo(aBI.getText());
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        return String.format("%s:%d: %s", getFile().getPath(), getLine() + 1, getText());
    }

}