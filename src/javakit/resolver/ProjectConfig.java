package javakit.resolver;
import snap.props.PropObject;
import snap.util.ArrayUtils;
import snap.util.SnapUtils;
import snap.util.StringUtils;
import snap.web.WebFile;
import snap.web.WebSite;
import java.io.File;
import java.util.Arrays;

/**
 * A class to read/edit the .classpath file.
 */
public class ProjectConfig extends PropObject {

    // The project
    private Project  _proj;

    // The site
    private WebSite  _site;

    // The project source path
    protected String  _srcPath;

    // The project build path
    protected String  _buildPath;

    // The source paths
    protected String[]  _srcPaths = new String[0];

    // The library paths
    protected String[]  _libPaths = new String[0];

    // The project paths
    protected String[]  _projPaths = new String[0];

    // The config file
    private ProjectConfigFile  _configFile;

    // Constants for ClassPath properties
    public static final String BuildPath_Prop = "BuildPath";
    public static final String SrcPaths_Prop = "SrcPaths";
    public static final String JarPaths_Prop = "JarPaths";

    /**
     * Creates a new ClassPathFile for project.
     */
    public ProjectConfig(Project aProj)
    {
        _proj = aProj;
        _site = aProj.getSite();

        _configFile = new ProjectConfigFile(aProj, this);
    }

    /**
     * Returns the source path.
     */
    public String getSourcePath()  { return _srcPath; }

    /**
     * Returns the build path.
     */
    public String getBuildPath()  { return _buildPath; }

    /**
     * Sets the build path.
     */
    public void setBuildPath(String aPath)
    {
        // Update ivar
        if (SnapUtils.equals(aPath, getBuildPath())) return;

        String newPath = aPath != null ? getRelativePath(aPath) : null;
        firePropChange(BuildPath_Prop, _buildPath, _buildPath = newPath);
    }

    /**
     * Returns the source paths.
     */
    public String[] getSrcPaths()  { return _srcPaths; }

    /**
     * Adds a source path.
     */
    public void addSrcPath(String aPath)
    {
        // Add XML for path
        String path = getRelativePath(aPath);

        _srcPaths = ArrayUtils.add(_srcPaths, path);

        firePropChange(SrcPaths_Prop, null, path);
    }

    /**
     * Removes a source path.
     */
    public void removeSrcPath(String aPath)
    {
        // Update paths
        int index = ArrayUtils.indexOf(_srcPaths, aPath);
        if (index < 0) return;

        _srcPaths = ArrayUtils.remove(_srcPaths, index);

        // Fire property change
        firePropChange(SrcPaths_Prop, aPath, null);
    }

    /**
     * Returns the paths.
     */
    public String[] getLibPaths()  { return _libPaths; }

    /**
     * Adds a library path.
     */
    public void addLibPath(String aPath)
    {
        // Update paths
        String path = getRelativePath(aPath);

        _libPaths = ArrayUtils.add(_libPaths, path);

        firePropChange(JarPaths_Prop, null, path);
    }

    /**
     * Removes a library path.
     */
    public void removeLibPath(String aPath)
    {
        // Update paths
        int index = ArrayUtils.indexOf(_libPaths, aPath);
        if (index < 0) return;

        _libPaths = ArrayUtils.remove(_libPaths, index);

        firePropChange(JarPaths_Prop, aPath, null);
    }

    /**
     * Returns the project paths path.
     */
    public String[] getProjectPaths()  { return _projPaths; }

    /**
     * Returns the build path as absolute path.
     */
    public String getBuildPathAbsolute()
    {
        String buildPath = getBuildPath();
        String buildPathAbs = getAbsolutePath(buildPath);
        return addDirChar(buildPathAbs);
    }

    /**
     * Returns the library paths as absolute paths.
     */
    public String[] getLibPathsAbsolute()
    {
        String[] libPaths = getLibPaths();
        String[] absPaths = new String[libPaths.length];
        for (int i = 0; i < libPaths.length; i++) {
            String absPath = getAbsolutePath(libPaths[i]);
            absPaths[i] = addDirChar(absPath);
        }
        return absPaths;
    }

    /**
     * Returns an absolute path for given relative path with option to add .
     */
    private String getAbsolutePath(String aPath)
    {
        String path = aPath;
        if (!path.startsWith("/"))
            path = getProjRootDirPath() + path;
        return path;
    }

    /**
     * Adds a directory char to end of path if needed.
     */
    private String addDirChar(String aPath)
    {
        if (!StringUtils.endsWithIC(aPath, ".jar") && !StringUtils.endsWithIC(aPath, ".zip") && !aPath.endsWith("/"))
            aPath = aPath + '/';
        return aPath;
    }

    /**
     * Returns a relative path for given path.
     */
    private String getRelativePath(String aPath)
    {
        String path = aPath;
        if (File.separatorChar != '/') path = path.replace(File.separatorChar, '/');
        if (!aPath.startsWith("/")) return path;
        String root = getProjRootDirPath();
        if (path.startsWith(root)) path = path.substring(root.length());
        return path;
    }

    /**
     * Returns the project root path.
     */
    private String getProjRootDirPath()
    {
        String root = _site.getRootDir().getJavaFile().getAbsolutePath();
        if (File.separatorChar != '/') root = root.replace(File.separatorChar, '/');
        if (!root.endsWith("/")) root = root + '/';
        if (!root.startsWith("/")) root = '/' + root;
        return root;
    }

    /**
     * Reads the class path from .classpath file.
     */
    public void readFile()
    {
        _configFile.readFile();
    }

    /**
     * Returns whether given file is config file.
     */
    public boolean isConfigFile(WebFile aFile)
    {
        return aFile == _configFile.getFile();
    }

    /**
     * Standard toString implementation.
     */
    @Override
    public String toStringProps()
    {
        StringBuffer sb = new StringBuffer();
        StringUtils.appendProp(sb, BuildPath_Prop, getBuildPath());
        StringUtils.appendProp(sb, SrcPaths_Prop, Arrays.toString(getSrcPaths()));
        StringUtils.appendProp(sb, JarPaths_Prop, Arrays.toString(getLibPaths()));
        return sb.toString();
    }
}