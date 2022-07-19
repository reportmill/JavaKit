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
    protected WebSite  _site;

    // ProjectConfig
    protected ProjectConfig  _projConfig;

    // ProjectFiles
    private ProjectFiles  _projFiles;

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
        }

        _projFiles = new ProjectFiles(this);
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
        _site.setProp(Project.class.getSimpleName(), this);
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
        return _projFiles.getSourceDir();
    }

    /**
     * Returns the build directory.
     */
    public WebFile getBuildDir()
    {
        return _projFiles.getBuildDir();
    }

    /**
     * Returns the source file for given path.
     */
    public WebFile getSourceFile(String aPath, boolean doCreate, boolean isDir)
    {
        // Get path
        String path = aPath;

        // If Path in BuildDir, strip BuildPath
        String buildPath = getBuildDir().getDirPath();
        if (buildPath.length() > 1 && path.startsWith(buildPath))
            path = path.substring(buildPath.length() - 1);

        // If Path not in SourceDir, add SourcePath
        String sourcePath = getSourceDir().getPath();
        if (sourcePath.length() > 1 && !path.startsWith(sourcePath))
            path = sourcePath + path;

        // Get file from site
        WebSite projSite = getSite();
        WebFile file = projSite.getFile(path);

        // If file still not found, maybe create
        if (file == null && doCreate)
            file = projSite.createFile(path, isDir);

        // Return
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
    public ProjectConfig getProjectConfig()
    {
        // If already set, just return
        if (_projConfig != null) return _projConfig;

        // Create ProjectConfig
        ProjectConfig projConfig = new ProjectConfig(this);

        // Read config from file
        projConfig.readFile();

        // Add PropChangeListener to save config file when changed
        projConfig.addPropChangeListener(pc -> projConfigDidPropChange(pc));

        // Set, return
        return _projConfig = projConfig;
    }

    /**
     * Returns the paths needed to compile/run project.
     */
    public String[] getClassPaths()
    {
        String bpath = getProjectConfig().getBuildPathAbsolute();
        String[] libPaths = getLibPaths();
        if (libPaths.length == 0) return new String[]{bpath};
        return ArrayUtils.add(libPaths, bpath, 0);
    }

    /**
     * Returns the paths needed to compile/run project.
     */
    public String[] getLibPaths()
    {
        return getProjectConfig().getLibPathsAbsolute();
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
     * Watches Project.ProjectConfig for JarPaths change to reset ClassPathInfo.
     */
    private void projConfigDidPropChange(PropChange anEvent)
    {
        if (anEvent.getPropertyName() == ProjectConfig.JarPaths_Prop)
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
        Project proj = (Project) aSite.getProp(Project.class.getSimpleName());
        if (proj == null && doCreate)
            proj = new Project(aSite);

        // Return
        return proj;
    }
}
