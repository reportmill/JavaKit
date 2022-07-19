package javakit.resolver;
import snap.props.PropChange;
import snap.util.ArrayUtils;
import snap.web.WebFile;
import snap.web.WebSite;

/**
 * This is a Resolver subclass.
 */
public class Project extends Resolver {

    // The encapsulated data site
    private WebSite  _site;

    // ClassPath
    private ClassPath  _classPath;

    // ClassPathInfo
    private ClassPathInfo  _classPathInfo;

    /**
     * Creates a new Project for WebSite.
     */
    protected Project(WebSite aSite)
    {
        // Set site
        setSite(aSite);

        // If site doesn't exist, create root directory, src, bin and default .classpath file
        if (!aSite.getExists()) {
            aSite.getRootDir().save();
            aSite.createFile("/src", true).save();
            aSite.createFile("/bin", true).save();
            ClassPath.createFile(this);
        }
    }

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
        _site.setProp(Project.class.getName(), this);
    }

    /**
     * Returns a file for given path.
     */
    public WebFile getFile(String aPath)
    {
        return _site.getFile(aPath);
    }

    /**
     * Returns the source directory.
     */
    public WebFile getSourceDir()
    {
        return getClassPath().getSourceDir();
    }

    /**
     * Returns the build directory.
     */
    public WebFile getBuildDir()
    {
        return getClassPath().getBuildDir();
    }

    /**
     * Returns the source file for given path.
     */
    public WebFile getSourceFile(String aPath, boolean doCreate, boolean isDir)
    {
        // Look for file in source dir
        String path = aPath;
        String spath = getSourceDir().getPath();
        String bpath = getBuildDir().getDirPath();

        if (bpath.length() > 1 && path.startsWith(bpath)) path = path.substring(bpath.length() - 1);

        if (spath.length() > 1 && !path.startsWith(spath)) path = spath + path;

        WebFile file = getSite().getFile(path);

        // If file still not found, maybe create and return
        if (file == null && doCreate)
            file = getSite().createFile(path, isDir);
        return file;
    }

    /**
     * Returns the given path stripped of SourcePath or BuildPath if file is in either.
     */
    public String getSimplePath(String aPath)
    {
        // Get path (make sure it's a path) and get SourcePath/BuildPath
        String path = aPath.startsWith("/") ? aPath : "/" + aPath;
        String sp = getSourceDir().getPath(), bp = getBuildDir().getPath();

        // If in SourcePath (or is SourcePath) strip SourcePath prefix
        if (sp.length() > 1 && path.startsWith(sp)) {
            if (path.length() == sp.length()) path = "/";
            else if (path.charAt(sp.length()) == '/') path = path.substring(sp.length());
        }

        // If in BuildPath (or is BuildPath) strip BuildPath prefix
        else if (bp.length() > 1 && path.startsWith(bp)) {
            if (path.length() == sp.length()) path = "/";
            else if (path.charAt(bp.length()) == '/') path = path.substring(bp.length());
        }

        // Return path
        return path;
    }

    /**
     * Returns the class name for given class file.
     */
    public String getClassName(WebFile aFile)
    {
        String path = aFile.getPath();
        int i = path.lastIndexOf('.');
        if (i > 0) path = path.substring(0, i);
        return getSimplePath(path).substring(1).replace('/', '.');
    }

    /**
     * Returns the class that keeps track of class paths.
     */
    public ClassPath getClassPath()
    {
        // If already set, just return
        if (_classPath != null) return _classPath;

        // Create, add PropChangeListener
        ClassPath classPath = new ClassPath(this);
        classPath.addPropChangeListener(pc -> classPathDidPropChange(pc));

        // Set, return
        return _classPath = classPath;
    }

    /**
     * Returns the paths needed to compile/run project.
     */
    public String[] getClassPaths()
    {
        String bpath = getClassPath().getBuildPathAbsolute();
        String[] libPaths = getLibPaths();
        if (libPaths.length == 0) return new String[]{bpath};
        return ArrayUtils.add(libPaths, bpath, 0);
    }

    /**
     * Returns the paths needed to compile/run project.
     */
    public String[] getLibPaths()
    {
        return getClassPath().getLibPathsAbsolute();
    }

    /**
     * Returns ClassPathInfo.
     */
    public ClassPathInfo getClassPathInfo()
    {
        // If already set, just return
        if (_classPathInfo != null) return _classPathInfo;

        // Create, set, return
        ClassPathInfo classPathInfo = new ClassPathInfo(this);
        return _classPathInfo = classPathInfo;
    }

    /**
     * Watches Project.ClassPath for JarPaths change to reset ClassPathInfo.
     */
    private void classPathDidPropChange(PropChange anEvent)
    {
        if (anEvent.getPropertyName() == ClassPath.JarPaths_Prop)
            _classPathInfo = null;
    }

    /**
     * Returns the project for a given site.
     */
    public static Project get(WebFile aFile)
    {
        WebSite site = aFile.getSite();
        return get(site);
    }

    /**
     * Returns the project for a given site.
     */
    public static synchronized Project get(WebSite aSite)
    {
        return get(aSite, false);
    }

    /**
     * Returns the project for a given site.
     */
    public static synchronized Project get(WebSite aSite, boolean doCreate)
    {
        Project proj = (Project) aSite.getProp(Project.class.getName());
        if (proj == null && doCreate) proj = new Project(aSite);
        return proj;
    }
}
