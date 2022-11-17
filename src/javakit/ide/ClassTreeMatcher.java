/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.ide;
import java.util.stream.Stream;
import javakit.resolver.ClassTree;
import javakit.resolver.ClassTree.*;
import javakit.resolver.ClassTreeWeb;

/**
 * This class handles searches on a ClassTree.
 */
public class ClassTreeMatcher {

    // The ClassTree
    private ClassTree _classTree;

    /**
     * Constructor.
     */
    public ClassTreeMatcher(ClassTree aClassTree)
    {
        _classTree = aClassTree;
    }

    /**
     * Returns all packages with prefix.
     */
    public String[] getPackageNamesForMatcher(DeclMatcher aMatcher)
    {
        PackageNode rootPackage = _classTree.getRootPackage();
        PackageNode[] rootPackages = rootPackage.packages;
        PackageNode[] childPackages = getChildPackagesForMatcher(rootPackages, aMatcher);

        // Return list of package names for package dir files
        return getPackageNamesForPackages(childPackages);
    }

    /**
     * Returns packages for prefix.
     */
    public String[] getPackageChildrenNamesForMatcher(String aPkgName, DeclMatcher aMatcher)
    {
        // Get dir for package name
        PackageNode packageNode = _classTree.getPackageForName(aPkgName);
        if (packageNode == null)
            return new String[0];

        // Get package dir files for package files
        PackageNode[] childPackages = packageNode.packages;
        PackageNode[] childPackageDirs = getChildPackagesForMatcher(childPackages, aMatcher);

        // Get package names for package children dir files and return
        return getPackageNamesForPackages(childPackageDirs);
    }

    /**
     * Returns class names for prefix.
     */
    public String[] getPackageClassNamesForMatcher(String aPkgName, DeclMatcher aMatcher)
    {
        PackageNode packageNode = _classTree.getPackageForName(aPkgName);
        ClassNode[] pkgClasses = packageNode.classes;
        String[] classNames = getClassNamesForClassesAndMatcher(pkgClasses, aMatcher);
        return classNames;
    }

    /**
     * Returns class names for prefix matcher.
     */
    public String[] getClassNamesForPrefixMatcher(String aPrefix, DeclMatcher prefixMatcher)
    {
        // If less than 3 letters, return common names for prefix
        ClassNode[] classes = _classTree.getAllClasses();
        if (aPrefix.length() <= 2)
            classes = ClassTreeWeb.getShared().getAllClasses();

        // Return all names
        return getClassNamesForClassesAndMatcher(classes, prefixMatcher);
    }

    /**
     * Returns a list of class files for a package dir and a prefix.
     */
    private PackageNode[] getChildPackagesForMatcher(PackageNode[] thePackages, DeclMatcher declMatcher)
    {
        Stream<PackageNode> packagesStream = Stream.of(thePackages);
        Stream<PackageNode> matchingPackagesStream = packagesStream.filter(pkg -> declMatcher.matchesString(pkg.simpleName));
        PackageNode[] matchingPackages = matchingPackagesStream.toArray(size -> new PackageNode[size]);
        return matchingPackages;
    }

    /**
     * Returns all classes with prefix.
     */
    private String[] getClassNamesForClassesAndMatcher(ClassNode[] theClasses, DeclMatcher declMatcher)
    {
        // Simple case
        if (theClasses.length == 0) return new String[0];

        // Get all class files with prefix
        Stream<ClassNode> classesStream = Stream.of(theClasses);
        Stream<ClassNode> matchingClassesStream = classesStream.filter(cls -> declMatcher.matchesString(cls.simpleName));

        // Get class names for class files and return
        Stream<String> classNamesStream = matchingClassesStream.map(cls -> cls.fullName);
        String[] classNames = classNamesStream.toArray(size -> new String[size]);
        return classNames;
    }

    /**
     * Returns packages names for list of package dir files.
     */
    private String[] getPackageNamesForPackages(PackageNode[] packages)
    {
        Stream<PackageNode> packagesStream = Stream.of(packages);
        Stream<String> packageNamesStream = packagesStream.map(f -> f.fullName);
        return packageNamesStream.toArray(size -> new String[size]);
    }
}
