package javakit.resolver;
import java.util.*;
import java.util.regex.Matcher;
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

    // The array of all package files
    private WebFile[]  _allPackageDirs;

    // The array of all class names
    private ClassRecord[]  _allClasses;

    // The array of common class names
    private static ClassRecord[]  _commonClasses;

    // A map of package names to class records
    private Map<String,ClassRecord[]>  _packageClasses = new HashMap<>();

    /**
     * Constructor.
     */
    public ClassPathInfo(Resolver aResolver)
    {
        // Handle TeaVM
        if (SnapUtils.isTeaVM) {
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
    public String[] getPackageClassNamesForMatcher(String aPkgName, Matcher aMatcher)
    {
        ClassRecord[] pkgClasses = getPackageClasses(aPkgName);
        String[] classNames = getClassNamesForClassRecsAndMatcher(pkgClasses, aMatcher);
        return classNames;
    }

    /**
     * Returns packages for prefix.
     */
    public String[] getPackageChildrenNamesForMatcher(String aPkgName, Matcher aMatcher)
    {
        // Get dir for package name
        WebFile pkgDir = getPackageDir(aPkgName);
        if (pkgDir == null)
            return new String[0];

        // Get package dir files for package files
        WebFile[] packageFiles = pkgDir.getFiles();
        WebFile[] childPackageDirs = getChildPackageDirsForMatcher(packageFiles, aMatcher);

        // Get package names for package children dir files and return
        return getPackageNamesForPackageDirs(childPackageDirs);
    }

    /**
     * Returns all packages with prefix.
     */
    public String[] getPackageNamesForMatcher(Matcher aMatcher)
    {
        WebFile[] packageDirs = getAllPackageDirs();
        WebFile[] childPackageDirs = getChildPackageDirsForMatcher(packageDirs, aMatcher);

        // Return list of package names for package dir files
        return getPackageNamesForPackageDirs(childPackageDirs);
    }

    /**
     * Returns class names for prefix.
     */
    public String[] getClassNamesForPrefixMatcher(String aPrefix, Matcher prefixMatcher)
    {
        // If less than 3 letters, return common names for prefix
        ClassRecord[] classRecs = getAllClasses();
        if (aPrefix.length() <= 2)
            classRecs = getCommonClasses();

        // Return all names
        return getClassNamesForClassRecsAndMatcher(classRecs, prefixMatcher);
    }

    /**
     * Returns all classes with prefix.
     */
    private String[] getClassNamesForClassRecsAndMatcher(ClassRecord[] theClassRecs, Matcher aMatcher)
    {
        // Simple case
        if (theClassRecs.length == 0) return new String[0];

        // Get all class files with prefix
        Stream<ClassRecord> classesStream = Stream.of(theClassRecs);
        Stream<ClassRecord> classesWithPrefixStream = classesStream.filter(crec -> isClassRecPrefixed(crec, aMatcher));

        // Get class names for class files and return
        Stream<String> classNamesStream = classesWithPrefixStream.map(crec -> crec.fullName);
        String[] classNames = classNamesStream.toArray(size -> new String[size]);
        return classNames;
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
     * Returns classes for package.
     */
    public ClassRecord[] getPackageClasses(String aPkgName)
    {
        // Get from cache, return if found
        ClassRecord[] classes = _packageClasses.get(aPkgName);
        if (classes != null)
            return classes;

        // Get, set, return
        classes = getClassesForPackageImpl(aPkgName);
        _packageClasses.put(aPkgName, classes);
        return classes;
    }

    /**
     * Returns classes for package.
     */
    private ClassRecord[] getClassesForPackageImpl(String aPkgName)
    {
        // Get packageDir
        WebFile pkgDir = getPackageDir(aPkgName);
        if (pkgDir == null)
            return new ClassRecord[0];

        // Get all class files with prefix
        WebFile[] packageFiles = pkgDir.getFiles();
        Stream<WebFile> packageFilesStream = Stream.of(packageFiles);
        Stream<WebFile> classFilesStream = packageFilesStream.filter(f -> isClassFileAndSearchCandidate(f));

        // Get class records for class files
        Stream<ClassRecord> classRecsStream = classFilesStream.map(f -> new ClassRecord(getClassNameForClassFile(f)));
        ClassRecord[] classRecords = classRecsStream.toArray(size -> new ClassRecord[size]);
        return classRecords;
    }

    /**
     * Returns a list of class files for a package dir and a prefix.
     */
    public WebFile[] getChildPackageDirsForMatcher(WebFile[] theFiles, Matcher aMatcher)
    {
        Stream<WebFile> filesStream = Stream.of(theFiles);
        Stream<WebFile> packageDirsStream = filesStream.filter(f -> isDirWithPrefix(f, aMatcher));
        WebFile[] packageDirs = packageDirsStream.toArray(size -> new WebFile[size]);
        return packageDirs;
    }

    /**
     * Returns the array of all packages.
     */
    public WebFile[] getAllPackageDirs()
    {
        if (_allPackageDirs != null) return _allPackageDirs;
        getAllClasses();
        return _allPackageDirs;
    }

    /**
     * Returns the array of all class names.
     */
    public ClassRecord[] getAllClasses()
    {
        // If already set, just return
        if (_allClasses != null) return _allClasses;

        // Load AllClassFiles and AllPackageDirs
        List<ClassRecord> allClasses = new ArrayList<>();
        List<WebFile> allPackageDirs = new ArrayList<>();
        for (WebSite site : getSites()) {
            WebFile siteRootDir = site.getRootDir();
            getAll(siteRootDir, allClasses, allPackageDirs);
        }

        // Set
        _allPackageDirs = allPackageDirs.toArray(new WebFile[0]);
        _allClasses = allClasses.toArray(new ClassRecord[0]);

        // Return
        return _allClasses;
    }

    /**
     * Gets all class files and package dirs in given directory.
     */
    private void getAll(WebFile aDir, List<ClassRecord> classNames, List<WebFile> packageDirs)
    {
        // Get directory files
        WebFile[] dirFiles = aDir.getFiles();

        // Iterate over dir files and add to ClassFiles or PackageDirs
        for (WebFile file : dirFiles) {

            // Handle nested dir
            if (file.isDir()) {
                if (file.getName().indexOf('.') > 0) continue;
                if (packageDirs != null) packageDirs.add(file);
                getAll(file, classNames, null); // Send null because we only want top level packages
            }

            // Handle plain file: Add to classFiles if interesting and .class
            else {
                String path = file.getPath();
                if (!path.endsWith(".class")) continue;
                if (!isInterestingPath(path)) continue;
                String className = getClassNameForClassFile(file);
                ClassRecord classRecord = new ClassRecord(className);
                classNames.add(classRecord);
            }
        }
    }

    /**
     * Returns common classes.
     */
    private static ClassRecord[] getCommonClasses()
    {
        if (_commonClasses != null) return _commonClasses;
        Stream<String> commonClassesStream = Stream.of(COMMON_CLASS_NAMES);
        Stream<ClassRecord> classRecsStream = commonClassesStream.map(cname -> new ClassRecord(cname));
        ClassRecord[] classRecs = classRecsStream.toArray(size -> new ClassRecord[size]);
        return _commonClasses = classRecs;
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
     * Returns a simple class name for class name.
     */
    private static String getSimpleClassName(String aClassName)
    {
        // Get index of last '$' or '.'
        int index = aClassName.lastIndexOf('$');
        if (index < 0)
            index = aClassName.lastIndexOf('.');

        // Return ClassName stripped of package and/or parent-class
        return aClassName.substring(index + 1);
    }

    /**
     * Returns whether given file has given prefix.
     */
    private static boolean isFilePrefixed(WebFile aFile, Matcher aPrefix)
    {
        String name = aFile.getName();
        int di = name.lastIndexOf('$');
        if (di > 0)
            name = name.substring(di + 1);

        return aPrefix.reset(name).lookingAt() && name.endsWith(".class");
    }

    /**
     * Returns whether given file has given prefix.
     */
    private static boolean isClassRecPrefixed(ClassRecord aClassRec, Matcher aPrefix)
    {
        String name = aClassRec.simpleName;
        return aPrefix.reset(name).lookingAt();
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
    private String[] getPackageNamesForPackageDirs(WebFile[] packageDirs)
    {
        Stream<WebFile> packageDirsStream = Stream.of(packageDirs);
        Stream<String> packageNamesStream = packageDirsStream.map(f -> getPackageNameForFile(f));
        return packageNamesStream.toArray(size -> new String[size]);
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
     * Returns whether given file is a class that should be used for searching.
     */
    private static boolean isClassFileAndSearchCandidate(WebFile aFile)
    {
        String path = aFile.getPath();
        if (!path.endsWith(".class"))
            return false;
        if (!isInterestingPath(path))
            return false;
        return true;
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
        if (aPath.startsWith("/java/awt/dnd")) return false;
        if (aPath.startsWith("/javax/swing/plaf")) return false;
        if (aPath.startsWith("/javax/xml")) return false;
        if (aPath.startsWith("/org/omg")) return false;
        if (aPath.startsWith("/org/w3c")) return false;

        // If anonymous inner class, return false
        int dollar = aPath.lastIndexOf('$');
        if (dollar > 0 && Character.isDigit(aPath.charAt(dollar + 1)))
            return false;

        // Return true
        return true;
    }

    /**
     * Adds to the CommonClassNames.
     */
    public static void addCommonClassNames(String[] moreNames)
    {
        COMMON_CLASS_NAMES = ArrayUtils.addAll(COMMON_CLASS_NAMES, moreNames);
    }

    /**
     * An array of common class names.
     */
    private static String[] COMMON_CLASS_NAMES = {

            // Java.lang
            "java.lang.Boolean", "java.lang.Byte", "java.lang.Character", "java.lang.Class", "java.lang.Double",
            "java.lang.Enum", "java.lang.Float", "java.lang.Integer", "java.lang.Long", "java.lang.Math", "java.lang.Number",
            "java.lang.Object", "java.lang.String", "java.lang.StringBuffer", "java.lang.StringBuilder", "java.lang.System",
            "java.lang.Thread",

            // Java.util
            "java.util.List", "java.util.Map", "java.util.Set", "java.util.ArrayList", "java.util.Arrays",
            "java.util.Collections", "java.util.Date", "java.util.HashMap", "java.util.HashSet", "java.util.Hashtable",
            "java.util.Map", "java.util.Random", "java.util.Scanner", "java.util.Stack", "java.util.Timer",
            "java.util.Vector",

            // Java.io
            "java.io.File",

            // Snap.gfx
            "snap.gfx.Border", "snap.gfx.Color", "snap.gfx.Font",

            // Snap.view
            "snap.view.Button", "snap.view.Label", "snap.view.View", "snap.view.ViewOwner"
    };

    /**
     * A class to hold class name/path info.
     */
    private static class ClassRecord {

        // Class full name
        public final String fullName;

        // Class simple name
        public final String simpleName;

        /** Constructor. */
        public ClassRecord(String aClassName)
        {
            fullName = aClassName;
            simpleName = getSimpleClassName(aClassName);
        }
    }
}