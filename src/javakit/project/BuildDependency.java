package javakit.project;
import snap.web.WebFile;
import snap.web.WebSite;

/**
 * This class represents a build dependency (e.g. JarFile, Project, Maven Package).
 */
public abstract class BuildDependency {

    // The project
    private Project _project;

    // An identifier string
    private String _id;

    // Constants for type
    public enum Type { JarFile, Project, Maven };

    /**
     * Constructor.
     */
    private BuildDependency(Project aProject, String idStr)
    {
        super();
        _project = aProject;
        _id = idStr;
    }

    /**
     * Returns the type.
     */
    public abstract Type getType();

    /**
     * Returns the identifier string.
     */
    public String getId()  { return _id; }

    /**
     * Returns a dependency for given path.
     */
    public BuildDependency getDependencyForPath(Project aProject, String aPath)
    {
        // Get WebFile for path
        WebSite projSite = aProject.getSite();
        String relativePath = ProjectUtils.getRelativePath(aProject, aPath);
        WebFile snapFile = projSite.getFileForPath(relativePath);
        if (snapFile == null)
            return null;

        // If Jar, return JarFileDependency
        String snapFileType = snapFile.getType();
        if (snapFileType.equals("jar"))
            return new JarFileDependency(aProject, relativePath);

        // If Project dir, return Project
        if (snapFile.isDir()) {
            String projectName = snapFile.getSimpleName();
            return new ProjectDependency(aProject, projectName);
        }

        // Return not found
        return null;
    }

    /**
     * This class represents a JarFile dependency.
     */
    public static class JarFileDependency extends BuildDependency {

        // The Path to the JarFile
        private String _jarFilePath;

        /**
         * Constructor.
         */
        public JarFileDependency(Project aProject, String jarFilePath)
        {
            super(aProject, jarFilePath);
            _jarFilePath = jarFilePath;
        }

        /**
         * Returns the type.
         */
        public Type getType()  { return Type.JarFile; }

        /**
         * Returns the path to the JarFile.
         */
        public String getJarFilePath()  { return _jarFilePath; }
    }

    /**
     * This class represents a Project dependency.
     */
    public static class ProjectDependency extends BuildDependency {

        // The Project name
        private String _projectName;

        /**
         * Constructor.
         */
        public ProjectDependency(Project aProject, String projectName)
        {
            super(aProject, projectName);
            _projectName = projectName;
        }

        /**
         * Returns the type.
         */
        public Type getType()  { return Type.JarFile; }

        /**
         * Returns the Project name.
         */
        public String getProjectName()  { return _projectName; }
    }
}
