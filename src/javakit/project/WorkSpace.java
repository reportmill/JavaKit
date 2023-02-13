/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.project;
import javakit.resolver.Resolver;
import snap.util.ArrayUtils;
import snap.util.SnapUtils;
import snap.web.WebSite;
import java.io.Closeable;

/**
 * This class manages working with a set of one or more projects.
 */
public class WorkSpace {

    // The projects in the workspace
    private Project[]  _projects = new Project[0];

    // The ClassLoader for compiled class info
    protected ClassLoader  _classLoader;

    // The resolver
    protected Resolver  _resolver;

    /**
     * Constructor.
     */
    public WorkSpace()
    {
        super();
    }

    /**
     * Returns the projects that this workspace manages.
     */
    public Project[] getProjects()  { return _projects; }

    /**
     * Adds a project.
     */
    public void addProject(Project aProj)
    {
        _projects = ArrayUtils.addId(_projects, aProj);
    }

    /**
     * Returns the root project.
     */
    public Project getRootProject()  { return _projects[0]; }

    /**
     * Returns the project set.
     */
    public ProjectSet getProjectSet()
    {
        Project rootProj = getRootProject();
        return rootProj.getProjectSet();
    }

    /**
     * Returns the ClassLoader.
     */
    public ClassLoader getClassLoader()
    {
        // If already set, just return
        if (_classLoader != null) return _classLoader;

        // Create, set, return ClassLoader
        ClassLoader classLoader = createClassLoader();
        return _classLoader = classLoader;
    }

    /**
     * Creates the ClassLoader.
     */
    protected ClassLoader createClassLoader()
    {
        // Get System ClassLoader
        ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader(); //.getParent();

        // Get all project ClassPath URLs and add to class loader
        //String[] classPaths = getClassPaths();
        //URL[] urls = FilePathUtils.getURLs(classPaths);
        ClassLoader urlClassLoader = sysClassLoader; //new URLClassLoader(urls, sysClassLoader);

        // Return
        return urlClassLoader;
    }

    /**
     * Clears the class loader.
     */
    public void clearClassLoader()
    {
        // If ClassLoader closeable, close it
        if (_classLoader instanceof Closeable)
            try {  ((Closeable) _classLoader).close(); }
            catch (Exception e) { throw new RuntimeException(e); }

        // Clear
        _classLoader = null;
        _resolver = null;
    }

    /**
     * Returns the resolver.
     */
    public Resolver getResolver()
    {
        // If already set, just return
        if (_resolver != null) return _resolver;

        // Handle TeaVM special
        if (SnapUtils.isTeaVM) {
            return Resolver.newResolverForClassLoader(getClass().getClassLoader());
        }

        // Create Resolver
        ClassLoader classLoader = getClassLoader();
        Resolver resolver = Resolver.newResolverForClassLoader(classLoader);
        Project rootProj = getRootProject();
        String[] classPaths = rootProj.getClassPaths();
        resolver.setClassPaths(classPaths);

        // Set, return
        return _resolver = resolver;
    }

    /**
     * Returns a project for given site.
     */
    public Project getProjectForSite(WebSite aSite)
    {
        Project proj = Project.getProjectForSite(aSite);
        if (proj == null)
            proj = createProjectForSite(aSite);
        return proj;
    }

    /**
     * Creates a project for given site.
     */
    protected Project createProjectForSite(WebSite aSite)
    {
        return new Project(this, aSite);
    }
}
