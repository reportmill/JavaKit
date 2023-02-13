/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.project;
import javakit.resolver.Resolver;
import snap.props.PropChange;
import snap.util.TaskMonitor;
import snap.web.WebFile;
import snap.web.WebSite;

/**
 * This is a Resolver subclass.
 */
public class Project {

    // The Pod that owns this project
    private Pod  _pod;

    // The encapsulated data site
    protected WebSite  _site;

    // ProjectConfig
    protected ProjectConfig  _projConfig;

    // ProjectFiles
    protected ProjectFiles  _projFiles;

    // The ProjectBuilder
    protected ProjectBuilder  _projBuilder;

    // The set of projects this project depends on
    private ProjectSet  _projSet;

    // A list of build issues
    private BuildIssues  _buildIssues;

    // The list of Breakpoints
    private Breakpoints  _breakpoints;

    /**
     * Creates a new Project for WebSite.
     */
    public Project(Pod aPod, WebSite aSite)
    {
        _pod = aPod;
        aPod.addProject(this);

        // Set site
        setSite(aSite);

        // If site doesn't exist, create root directory, src, bin and default .classpath file
        if (!aSite.getExists()) {
            aSite.getRootDir().save();
            aSite.createFileForPath("/src", true).save();
            aSite.createFileForPath("/bin", true).save();
        }

        // Create/set ProjectFiles, ProjectBuilder
        _projFiles = new ProjectFiles(this);
        _projBuilder = new ProjectBuilder(this);

        // Create ProjectSet
        _projSet = new ProjectSet(this);
        _projSet.getProjects();
    }

    /**
     * Returns the Pod that manages this project.
     */
    public Pod getPod()  { return _pod; }

    /**
     * Returns root project if part of hierarchy.
     */
    public Project getRootProject()  { return this; }

    /**
     * Returns the project name.
     */
    public String getName()
    {
        return _site.getName();
    }

    /**
     * Returns the encapsulated WebSite.
     */
    public WebSite getSite()  { return _site; }

    /**
     * Sets the encapsulated WebSite.
     */
    protected void setSite(WebSite aSite)
    {
        _site = aSite;
        _site.setProp(Project.class.getSimpleName(), this);
    }

    /**
     * Returns the ProjectConfig that manages project properties.
     */
    public ProjectConfig getProjectConfig()
    {
        // If already set, just return
        if (_projConfig != null) return _projConfig;

        // Create ProjectConfig
        ProjectConfig projConfig = createProjectConfig();

        // Add PropChangeListener to clear ClassPathInfo when changed
        projConfig.addPropChangeListener(pc -> projConfigDidPropChange(pc));

        // Set, return
        return _projConfig = projConfig;
    }

    /**
     * Returns the ProjectConfig that manages project properties.
     */
    protected ProjectConfig createProjectConfig()
    {
        return new ProjectConfig(this);
    }

    /**
     * Returns the paths needed to compile/run project.
     */
    public String[] getClassPaths()  { return _projConfig.getClassPaths(); }

    /**
     * Returns the set of projects this project depends on.
     */
    public ProjectSet getProjectSet()  { return _projSet; }

    /**
     * Returns an array of projects that this project depends on.
     */
    public Project[] getProjects()  { return _projSet.getProjects(); }

    /**
     * Returns the ProjectBuilder.
     */
    public ProjectBuilder getProjectBuilder()  { return _projBuilder; }

    /**
     * Builds the project.
     */
    public boolean buildProject(TaskMonitor aTM)
    {
        return _projBuilder.buildProject(aTM);
    }

    /**
     * Interrupts build.
     */
    public void interruptBuild()  { _projBuilder.interruptBuild(); }

    /**
     * Removes all build files from project.
     */
    public void cleanProject()  { _projBuilder.cleanProject(); }

    /**
     * Adds a build file.
     */
    public void addBuildFilesAll()  { _projBuilder.addBuildFilesAll(); }

    /**
     * Returns the resolver.
     */
    public Resolver getResolver()
    {
        Pod pod = getPod();
        return pod.getResolver();
    }

    /**
     * Returns whether file is config file.
     */
    protected boolean isConfigFile(WebFile aFile)  { return false; }

    /**
     * Returns the ProjectFiles that manages project files.
     */
    public ProjectFiles getProjectFiles()  { return _projFiles; }

    /**
     * Returns the source directory.
     */
    public WebFile getSourceDir()  { return _projFiles.getSourceDir(); }

    /**
     * Returns the build directory.
     */
    public WebFile getBuildDir()  { return _projFiles.getBuildDir(); }

    /**
     * Returns a file for given path.
     */
    public WebFile getFile(String aPath)
    {
        return _site.getFileForPath(aPath);
    }

    /**
     * Returns the source file for given path.
     */
    public WebFile getSourceFile(String aPath, boolean doCreate, boolean isDir)
    {
        return _projFiles.getSourceFile(aPath, doCreate, isDir);
    }

    /**
     * Returns the class name for given class file.
     */
    public String getClassNameForFile(WebFile aFile)
    {
        return _projFiles.getClassNameForFile(aFile);
    }

    /**
     * The breakpoint list property.
     */
    public BuildIssues getBuildIssues()
    {
        if (_buildIssues != null) return _buildIssues;
        return _buildIssues = new BuildIssues(this);
    }

    /**
     * Returns the breakpoints.
     */
    public Breakpoints getBreakpoints()
    {
        if (_breakpoints != null) return _breakpoints;
        return _breakpoints = new Breakpoints(this);
    }

    /**
     * Watches Project.ProjectConfig for JarPaths change to reset ClassPathInfo.
     */
    private void projConfigDidPropChange(PropChange anEvent)
    {
        String propName = anEvent.getPropName();
        if (propName == ProjectConfig.JarPaths_Prop) {
            Pod pod = getPod();
            pod.clearClassLoader();
        }
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        return "Project: " + getSite();
    }

    /**
     * Returns the project for a given site.
     */
    public static Project getProjectForFile(WebFile aFile)
    {
        WebSite site = aFile.getSite();
        return getProjectForSite(site);
    }

    /**
     * Returns the project for a given site.
     */
    public static synchronized Project getProjectForSite(WebSite aSite)
    {
        Project proj = (Project) aSite.getProp(Project.class.getSimpleName());
        return proj;
    }
}
