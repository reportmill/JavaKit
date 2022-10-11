package javakit.resolver;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javakit.reflect.Resolver;
import snap.util.*;
import snap.web.*;

/**
 * A class to return class file info for Project class paths.
 */
public class ClassPathInfo {

    // The shared list of class path sites
    private WebSite[]  _sites;

    // The list of all package files
    private List<WebFile>  _allPackageDirs;

    // The list of all class files
    private List<WebFile>  _allClassFiles;

    /**
     * Constructor.
     */
    public ClassPathInfo(Resolver aResolver)
    {
        // Handle TeaVM
        if (SnapUtils.isTeaVM) {
            WebSite site = WebURL.getURL("/").getSite();
            for (String cc : COMMON_CLASS_NAMES) {
                WebFile classFile = site.createFile(cc, false);
                classFile.save();
            }
            _sites = new WebSite[] { site };
            return;
        }

        // Add JRE jar file site
        WebURL jreURL = WebURL.getURL(List.class);
        WebSite jreSite = jreURL.getSite();
        List<WebSite> sites = new ArrayList<>();
        sites.add(jreSite);

        // Get Project ClassPaths (build dirs, jar files)
        String[] classPaths = aResolver.getClassPaths(); // Was ProjectSet JK

        // Add project class path sites (build dirs, jar files)
        for (String classPath : classPaths) {
            WebURL classPathURL = WebURL.getURL(classPath);
            WebSite classPathSite = classPathURL.getAsSite();
            sites.add(classPathSite);
        }

        // Set Sites
        _sites = sites.toArray(new WebSite[0]);
    }

    /**
     * Returns the class path sites.
     */
    public WebSite[] getSites()  { return _sites; }

    /**
     * Returns class names for prefix.
     */
    public List<String> getPackageClassNamesForMatcher(String aPkgName, Matcher aMatcher)
    {
        // Get packageDir
        WebFile pkgDir = getPackageDir(aPkgName);
        if (pkgDir == null) return Collections.emptyList();

        // Get all class files with prefix
        WebFile[] packageDirFiles = pkgDir.getFiles();
        Stream<WebFile> packageDirFilesStream = Stream.of(packageDirFiles);
        Stream<WebFile> packageDirFilesWithPrefixStream = packageDirFilesStream.filter(f -> isFilePrefixed(f, aMatcher));

        // Get class names for class files
        Stream<String> classNamesStream = packageDirFilesWithPrefixStream.map(f -> getClassNameForClassFile(f));
        List<String> classNames = classNamesStream.collect(Collectors.toList());

        // Return
        return classNames;
    }

    /**
     * Returns packages for prefix.
     */
    public List<String> getPackageChildrenNamesForMatcher(String aPkgName, Matcher aMatcher)
    {
        // Get dir for package name
        WebFile pkgDir = getPackageDir(aPkgName);
        if (pkgDir == null) return Collections.emptyList();

        // Get package dir files for package files
        List<WebFile> packageFiles = Arrays.asList(pkgDir.getFiles());
        List<WebFile> childPackageDirs = getChildPackageDirsForMatcher(packageFiles, aMatcher);

        // Get package names for package children dir files and return
        return getPackageNamesForPackageDirs(childPackageDirs);
    }

    /**
     * Returns all packages with prefix.
     */
    public List<String> getPackageNamesForMatcher(Matcher aMatcher)
    {
        List<WebFile> packageDirs = getAllPackageDirs();
        List<WebFile> childPackageDirs = getChildPackageDirsForMatcher(packageDirs, aMatcher);

        // Return list of package names for package dir files
        return getPackageNamesForPackageDirs(childPackageDirs);
    }

    /**
     * Returns class names for prefix.
     */
    public List<String> getClassNamesForPrefixMatcher(String aPrefix, Matcher prefixMatcher)
    {
        // If less than 3 letters, return common names for prefix
        if (aPrefix.length() <= 2)
            return getCommonClassNamesForMatcher(prefixMatcher);

        // Return all names
        return getAllClassNamesForMatcher(prefixMatcher);
    }

    /**
     * Returns all classes with prefix.
     */
    private List<String> getAllClassNamesForMatcher(Matcher aMatcher)
    {
        // Get all class files with prefix
        List<WebFile> allClassFiles = getAllClassFiles();
        Stream<WebFile> allClassFilesStream = allClassFiles.stream();
        Stream<WebFile> classFilesWithPrefixStream = allClassFilesStream.filter(f -> isFilePrefixed(f, aMatcher));

        // Get class names for class files and return
        Stream<String> classNamesStream = classFilesWithPrefixStream.map(f -> getClassNameForClassFile(f));
        List<String> classNames = classNamesStream.collect(Collectors.toList());
        return classNames;
    }

    /**
     * Returns all classes with prefix.
     */
    public List<String> getCommonClassNamesForMatcher(Matcher aMatcher)
    {
        String[] commonClassNames = COMMON_CLASS_NAMES;
        String[] commonClassNamesSimple = COMMON_CLASS_NAMES_SIMPLE;

        // Initialize COMMON_CLASS_NAMES_SIMPLE
        if (commonClassNamesSimple == null) {
            commonClassNamesSimple = COMMON_CLASS_NAMES_SIMPLE = new String[commonClassNames.length];
            for (int i = 0; i < commonClassNames.length; i++) {
                String str = commonClassNames[i];
                int ind = str.lastIndexOf('.');
                commonClassNamesSimple[i] = str.substring(ind + 1).toLowerCase();
            }
        }

        // Get commonClassNames where simple name has given prefix
        List<String> commonClassNamesForMatcher = new ArrayList<>();
        for (int i = 0, iMax = commonClassNames.length; i < iMax; i++)
            if (aMatcher.reset(commonClassNamesSimple[i]).lookingAt())
                commonClassNamesForMatcher.add(commonClassNames[i]);

        // Return
        return commonClassNamesForMatcher;
    }

    /**
     * Returns a package dir for a package name.
     */
    public WebFile getPackageDir(String aName)
    {
        String path = "/" + aName.replace('.', '/');
        WebSite[] sites = getSites();
        for (WebSite site : sites) {
            WebFile file = site.getFileForPath(path);
            if (file != null)
                return file;
        }
        return null;
    }

    /**
     * Returns a list of class files for a package dir and a prefix.
     */
    public List<WebFile> getChildPackageDirsForMatcher(List<WebFile> theFiles, Matcher aMatcher)
    {
        Stream<WebFile> filesStream = theFiles.stream();
        Stream<WebFile> packageDirsStream = filesStream.filter(f -> isDirWithPrefix(f, aMatcher));
        List<WebFile> packageDirs = packageDirsStream.collect(Collectors.toList());
        return packageDirs;
    }

    /**
     * Returns the list of all packages.
     */
    public List<WebFile> getAllPackageDirs()
    {
        if (_allPackageDirs == null) getAllClassFiles();
        return _allPackageDirs;
    }

    /**
     * Returns the list of all classes.
     */
    public List<WebFile> getAllClassFiles()
    {
        // If already set, just return
        if (_allClassFiles != null) return _allClassFiles;

        // Load AllClassFiles and AllPackageDirs
        List<WebFile> allClassFiles = new ArrayList<>();
        List<WebFile> allPackageDirs = new ArrayList<>();
        for (WebSite site : getSites()) {
            WebFile siteRootDir = site.getRootDir();
            getAll(siteRootDir, allClassFiles, allPackageDirs);
        }

        // Set, return
        _allPackageDirs = allPackageDirs;
        return _allClassFiles = allClassFiles;
    }

    /**
     * Gets all class files and package dirs in given directory.
     */
    private void getAll(WebFile aDir, List<WebFile> classFiles, List<WebFile> packageDirs)
    {
        // Get directory files
        WebFile[] dirFiles = aDir.getFiles();

        // Iterate over dir files and add to ClassFiles or PackageDirs
        for (WebFile file : dirFiles) {

            // Handle nested dir
            if (file.isDir()) {
                if (file.getName().indexOf('.') > 0) continue;
                if (packageDirs != null) packageDirs.add(file);
                getAll(file, classFiles, null); // Send null because we only want top level packages
            }

            // Handle plain file: Add to classFiles if interesting and .class
            else {
                String path = file.getPath();
                if (!path.endsWith(".class")) continue;
                if (!isInterestingPath(path)) continue;
                classFiles.add(file);
            }
        }
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        WebSite[] sites = getSites();
        String sitesString = Arrays.toString(sites);
        return getClass().getSimpleName() + ": " + sitesString;
    }

    /**
     * Returns whether given file has given prefix.
     */
    private static boolean isFilePrefixed(WebFile aFile, Matcher aPrefix)
    {
        String name = aFile.getName();
        int di = name.lastIndexOf('$');
        if (di > 0) name = name.substring(di + 1);
        return aPrefix.reset(name).lookingAt() && name.endsWith(".class");
    }

    /**
     * Returns whether given file is dir with given prefix (and no extension).
     */
    private static boolean isDirWithPrefix(WebFile aFile, Matcher aPrefix)
    {
        if (!aFile.isDir()) return false;
        String dirName = aFile.getName();
        return aPrefix.reset(dirName).lookingAt() && dirName.indexOf('.') < 0;
    }

    /**
     * Returns class name for class file.
     */
    private static String getClassNameForClassFile(WebFile aFile)
    {
        String filePath = aFile.getPath();
        String filePathNoExtension = filePath.substring(1, filePath.length() - 6);
        String className = filePathNoExtension.replace('/', '.');
        return className;
    }

    /**
     * Returns packages names for list of package dir files.
     */
    private List<String> getPackageNamesForPackageDirs(List<WebFile> packageDirs)
    {
        Stream<WebFile> packageDirsStream = packageDirs.stream();
        Stream<String> packageNamesStream = packageDirsStream.map(f -> getPackageNameForFile(f));
        return packageNamesStream.collect(Collectors.toList());
    }

    /**
     * Returns a package name for package dir file.
     */
    private static String getPackageNameForFile(WebFile aFile)
    {
        String filePath = aFile.getPath();
        String pkgName = filePath.substring(1).replace('/', '.');
        return pkgName;
    }

    /**
     * Adds an entry (override to ignore).
     */
    private static boolean isInterestingPath(String aPath)
    {
        if (aPath.startsWith("/sun")) return false;
        if (aPath.startsWith("/apple")) return false;
        if (aPath.startsWith("/com/sun")) return false;
        if (aPath.startsWith("/com/apple")) return false;
        if (aPath.startsWith("/com/oracle")) return false;
        if (aPath.startsWith("/javax/swing/plaf")) return false;
        if (aPath.startsWith("/org/omg")) return false;
        int dollar = aPath.endsWith(".class") ? aPath.lastIndexOf('$') : -1;
        if (dollar > 0 && Character.isDigit(aPath.charAt(dollar + 1))) return false;
        return true;
    }

    /**
     * Returns a list of common class names.
     */
    private static String COMMON_CLASS_NAMES_SIMPLE[], COMMON_CLASS_NAMES[] = {
            "java.lang.Boolean", "java.lang.Byte", "java.lang.Character", "java.lang.Class", "java.lang.Double",
            "java.lang.Enum", "java.lang.Float", "java.lang.Integer", "java.lang.Long", "java.lang.Math", "java.lang.Number",
            "java.lang.Object", "java.lang.String", "java.lang.StringBuffer", "java.lang.StringBuilder", "java.lang.System",
            "java.lang.Thread",
            "java.util.List", "java.util.Map", "java.util.Set", "java.util.ArrayList", "java.util.Arrays",
            "java.util.Collections", "java.util.Date", "java.util.HashMap", "java.util.HashSet", "java.util.Hashtable",
            "java.util.Map", "java.util.Random", "java.util.Scanner", "java.util.Stack", "java.util.Timer",
            "java.util.Vector",
            "java.io.File",
            "snap.gfx.Border", "snap.gfx.Color", "snap.gfx.Font",
            "snap.view.Button", "snap.view.Label", "snap.view.View", "snap.view.ViewOwner",
            "snap.viewx.ScanPane"
    };

}