package javakit.project;
import snap.props.PropObject;
import snap.props.PropSet;
import snap.util.ArrayUtils;
import snap.util.Convert;
import java.util.Arrays;
import java.util.Objects;

/**
 * This class manages project properties.
 */
public class ProjectConfig extends PropObject {

    // The project
    private Project  _proj;

    // The project source path
    public String  _srcPath;

    // The project build path
    public String  _buildPath;

    // The library paths
    public String[]  _libPaths;

    // The project paths
    public String[]  _projPaths;

    // Constants for ClassPath properties
    public static final String SourcePath_Prop = "SourcePaths";
    public static final String BuildPath_Prop = "BuildPath";
    public static final String LibPaths_Prop = "LibPaths";
    public static final String ProjectPaths_Prop = "ProjectPaths";

    // Constants for defaults
    private static final String DEFAULT_SOURCE_PATH = "src";
    private static final String DEFAULT_BUILD_PATH = "bin";
    private static final String[] DEFAULT_LIB_PATHS = new String[0];
    private static final String[] DEFAULT_PROJECT_PATHS = new String[0];

    /**
     * Creates a new ClassPathFile for project.
     */
    public ProjectConfig(Project aProj)
    {
        _proj = aProj;

        // Set defaults
        _srcPath = DEFAULT_SOURCE_PATH;
        _buildPath = DEFAULT_BUILD_PATH;
        _libPaths = DEFAULT_LIB_PATHS;
        _projPaths = DEFAULT_PROJECT_PATHS;
    }

    /**
     * Returns the source path.
     */
    public String getSourcePath()  { return _srcPath; }

    /**
     * Sets the source path.
     */
    public void setSourcePath(String aPath)
    {
        // If already set, just return
        if (Objects.equals(aPath, _srcPath)) return;

        // Set, firePropChange
        String newPath = aPath != null ? ProjectUtils.getRelativePath(_proj, aPath) : null;
        firePropChange(SourcePath_Prop, _srcPath, _srcPath = newPath);
    }

    /**
     * Returns the build path.
     */
    public String getBuildPath()  { return _buildPath; }

    /**
     * Sets the build path.
     */
    public void setBuildPath(String aPath)
    {
        // If already set, just return
        if (Objects.equals(aPath, _buildPath)) return;

        // Set, firePropChange
        String newPath = aPath != null ? ProjectUtils.getRelativePath(_proj, aPath) : null;
        firePropChange(BuildPath_Prop, _buildPath, _buildPath = newPath);
    }

    /**
     * Returns the library paths.
     */
    public String[] getLibPaths()  { return _libPaths; }

    /**
     * Sets the library paths.
     */
    public void setLibPaths(String[] libPaths)
    {
        // If already set, just return
        if (libPaths == _libPaths) return;

        // Convert to relative
        String[] relativeLibPaths = ArrayUtils.map(libPaths, path -> ProjectUtils.getRelativePath(_proj, path), String.class);

        // If already set, just return
        if (Arrays.equals(relativeLibPaths, _libPaths)) return;

        // Set, fire prop change
        firePropChange(LibPaths_Prop, _libPaths, _libPaths = relativeLibPaths);
    }

    /**
     * Adds a library path.
     */
    public void addLibPath(String aPath)
    {
        // Get relative path if inside project
        String libPath = ProjectUtils.getRelativePath(_proj, aPath);

        // If already set, just return
        if (ArrayUtils.contains(_libPaths, libPath))
            return;

        // Add path
        _libPaths = ArrayUtils.add(_libPaths, libPath);

        // Fire prop change
        int index = _libPaths.length - 1;
        firePropChange(LibPaths_Prop, null, libPath, index);
    }

    /**
     * Removes a library path.
     */
    public void removeLibPath(String aPath)
    {
        // Update paths
        int index = ArrayUtils.indexOf(_libPaths, aPath);
        if (index < 0)
            return;

        // Remove path
        _libPaths = ArrayUtils.remove(_libPaths, index);

        // Fire prop change
        firePropChange(LibPaths_Prop, aPath, null, index);
    }

    /**
     * Adds a class path for jar containing given class.
     */
    public void addLibPathForClass(Class<?> aClass)
    {
        String classPath = ProjectUtils.getClassPathForClass(aClass);
        if (classPath != null)
            addLibPath(classPath);
        else System.out.println("ProjectConfig.addLibPathForClass: Couldn't find path for class: " + aClass.getName());
    }

    /**
     * Returns the project paths.
     */
    public String[] getProjectPaths()  { return _projPaths; }

    /**
     * Sets the project paths.
     */
    public void setProjectPaths(String[] projectPaths)
    {
        // If already set, just return
        if (projectPaths == _projPaths) return;

        // Convert to relative
        String[] relativeProjPaths = ArrayUtils.map(projectPaths, path -> ProjectUtils.getRelativePath(_proj, path), String.class);

        // If already set, just return
        if (Arrays.equals(relativeProjPaths, _projPaths)) return;

        // Set, fire prop change
        firePropChange(ProjectPaths_Prop, _projPaths, _projPaths = relativeProjPaths);
    }

    /**
     * Adds a project path.
     */
    public void addProjectPath(String aPath)
    {
        // Add XML for path
        String path = ProjectUtils.getRelativePath(_proj, aPath);

        // Add to array
        _projPaths = ArrayUtils.add(_projPaths, path);

        // Fire property change
        int index = _projPaths.length - 1;
        firePropChange(ProjectPaths_Prop, null, path, index);
    }

    /**
     * Removes a project path.
     */
    public void removeProjectPath(String aPath)
    {
        // Update paths
        int index = ArrayUtils.indexOf(_projPaths, aPath);
        if (index < 0)
            return;

        // Remove from array
        _projPaths = ArrayUtils.remove(_projPaths, index);

        // Fire property change
        firePropChange(ProjectPaths_Prop, aPath, null, index);
    }

    /**
     * Returns the build path as absolute path.
     */
    public String getBuildPathAbsolute()
    {
        String buildPath = getBuildPath();
        return ProjectUtils.getAbsolutePath(_proj, buildPath, true);
    }

    /**
     * Returns the library paths as absolute paths.
     */
    public String[] getLibPathsAbsolute()
    {
        String[] libPaths = getLibPaths();
        String[] absPaths = ArrayUtils.map(libPaths, path -> ProjectUtils.getAbsolutePath(_proj, path, true), String.class);
        return absPaths;
    }

    /**
     * Initialize properties for this class.
     */
    @Override
    protected void initProps(PropSet aPropSet)
    {
        // Do normal version
        super.initProps(aPropSet);

        // SourcePath, BuildPath, LibPaths, ProjectPaths
        aPropSet.addPropNamed(SourcePath_Prop, String.class, DEFAULT_SOURCE_PATH);
        aPropSet.addPropNamed(BuildPath_Prop, String.class, DEFAULT_BUILD_PATH);
        aPropSet.addPropNamed(LibPaths_Prop, String[].class, DEFAULT_LIB_PATHS);
        aPropSet.addPropNamed(ProjectPaths_Prop, String[].class, DEFAULT_PROJECT_PATHS);
    }

    /**
     * Returns the prop value for given key.
     */
    @Override
    public Object getPropValue(String aPropName)
    {
        // Handle properties
        switch (aPropName) {

            // SourcePath, BuildPath, LibPaths, ProjectPaths
            case SourcePath_Prop: return getSourcePath();
            case BuildPath_Prop: return getBuildPath();
            case LibPaths_Prop: return getLibPaths();
            case ProjectPaths_Prop: return getProjectPaths();

            // Handle super class properties (or unknown)
            default: System.err.println("ProjectConfig.getPropValue: Unknown prop: " + aPropName); return null;
        }
    }

    /**
     * Sets the prop value for given key.
     */
    @Override
    public void setPropValue(String aPropName, Object aValue)
    {
        // Handle properties
        switch (aPropName) {

            // SourcePath, BuildPath, LibPaths, ProjectPaths
            case SourcePath_Prop: setSourcePath(Convert.stringValue(aValue)); break;
            case BuildPath_Prop: setBuildPath(Convert.stringValue(aValue)); break;
            case LibPaths_Prop: setLibPaths((String[]) aValue); break;
            case ProjectPaths_Prop: setProjectPaths((String[]) aValue); break;

            // Handle super class properties (or unknown)
            default: System.err.println("ProjectConfig.setPropValue: Unknown prop: " + aPropName);
        }
    }
}