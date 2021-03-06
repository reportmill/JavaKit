/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;

import javakit.reflect.JavaDecl;
import javakit.reflect.JavaClass;
import javakit.reflect.JavaType;
import javakit.resolver.*;
import snap.web.WebFile;

/**
 * The top level Java part describing a Java file.
 */
public class JFile extends JNode {

    // The source file for this JFile
    protected WebFile  _sourceFile;

    // The resolver for source file
    protected Resolver  _resolver;

    // The package declaration
    protected JPackageDecl  _packageDecl;

    // The list of imports
    protected List<JImportDecl>  _importDecls = new ArrayList();

    // The list of class declarations
    List<JClassDecl> _classDecls = new ArrayList();

    // The parse exception, if one was hit
    Exception _exception;

    // A set to hold unused imports
    Set<JImportDecl> _unusedImports;

    /**
     * Returns the WebFile for this JFile.
     */
    public WebFile getSourceFile()
    {
        return _sourceFile;
    }

    /**
     * Sets the WebFile for this JFile.
     */
    public void setSourceFile(WebFile aFile)
    {
        _sourceFile = aFile;
        _resolver = Project.get(aFile);
    }

    /**
     * Returns the Resolver.
     */
    public Resolver getResolver()
    {
        // If already set, just return
        if (_resolver != null) return _resolver;

        // Get, set, return
        Resolver resolver = Resolver.getCurrent();
        return _resolver = resolver;
    }

    /**
     * Returns the package declaration.
     */
    public JPackageDecl getPackageDecl()
    {
        return _packageDecl;
    }

    /**
     * Sets the package declaration.
     */
    public void setPackageDecl(JPackageDecl aPD)
    {
        replaceChild(_packageDecl, _packageDecl = aPD);
    }

    /**
     * Returns the package name.
     */
    public String getPackageName()
    {
        return _packageDecl != null ? _packageDecl.getName() : null;
    }

    /**
     * Returns the import statements.
     */
    public List<JImportDecl> getImportDecls()
    {
        return _importDecls;
    }

    /**
     * Adds an import declaration.
     */
    public void addImportDecl(JImportDecl anID)
    {
        _importDecls.add(anID);
        addChild(anID, -1);
    }

    /**
     * Returns the JClassDecl for the file.
     */
    public JClassDecl getClassDecl()
    {
        return _classDecls.size() > 0 ? _classDecls.get(0) : null;
    }

    /**
     * Returns the JClassDecls for the file.
     */
    public List<JClassDecl> getClassDecls()
    {
        return _classDecls;
    }

    /**
     * Adds a JClassDecls for the file.
     */
    public void addClassDecl(JClassDecl aCD)
    {
        _classDecls.add(aCD);
        addChild(aCD, -1);
    }

    /**
     * Override to return this file node.
     */
    public JFile getFile()
    {
        return this;
    }

    /**
     * Override to get name from ClassDecl.
     */
    protected String getNameImpl()
    {
        JavaDecl jd = getDecl();
        return jd != null ? jd.getName() : null;
    }

    /**
     * Returns the type class of this file.
     */
    protected JavaDecl getDeclImpl()
    {
        JClassDecl cd = getClassDecl();
        return cd != null ? cd.getDecl() : null;
    }

    /**
     * Override to check for package name, import class name, static import class member.
     */
    protected JavaDecl getDeclImpl(JNode aNode)
    {
        // Get node info
        String name = aNode.getName();

        // If it's in JPackageDecl, it's a Package
        if (isKnownPackageName(name))
            return getJavaDecl(name);

        // See if it's a known class name using imports
        String cname = getImportClassName(name);
        JavaDecl cd = cname != null ? getJavaDecl(cname) : null;
        if (cd != null)
            return cd;

        // See if it's a known static import class member
        JavaDecl field = getImportClassMember(name, null);
        if (field != null)
            return field;

        // Do normal version
        return super.getDeclImpl(aNode);
    }

    /**
     * Returns a JavaDecl for a Class, Field, Method, Constructor or class name string.
     */
    public JavaDecl getJavaDecl(Object anObj)
    {
        Resolver resolver = getResolver();
        if (resolver == null) return null;
        return resolver.getJavaDecl(anObj);
    }

    /**
     * Returns a JavaDecl for a Class, Field, Method, Constructor or class name string.
     */
    @Override
    public JavaClass getJavaClass(Class<?> aClass)
    {
        Resolver resolver = getResolver();
        if (resolver == null) return null;
        return (JavaClass) resolver.getTypeDecl(aClass);
    }

    /**
     * Returns an import that can be used to resolve the given name.
     */
    public JImportDecl getImport(String aName)
    {
        // Handle fully specified name
        if (isKnownClassName(aName)) return null;

        // Iterate over imports to see if any can resolve name
        JImportDecl match = null;
        for (int i = _importDecls.size() - 1; i >= 0; i--) {
            JImportDecl imp = _importDecls.get(i);

            // Get import name (just continue if null)
            String iname = imp.getName();
            if (iname == null) continue;

            // If import is static, see if it matches given name
            if (imp.isStatic() && iname.endsWith(aName)) {
                if (iname.length() == aName.length() || iname.charAt(iname.length() - aName.length() - 1) == '.') {
                    match = imp;
                    break;
                }
            }

            // If import is inclusive ("import xxx.*") and ImportName.aName is known class, return class name
            else if (imp.isInclusive() && match == null) {
                String cname = iname + '.' + aName;
                if (isKnownClassName(cname) || imp.isClassName() && isKnownClassName(iname + '$' + aName))
                    match = imp;
            }

            // Otherwise, see if import refers explicitly to class name
            else if (iname.endsWith(aName)) {
                if (iname.length() == aName.length() || iname.charAt(iname.length() - aName.length() - 1) == '.') {
                    match = imp;
                    break;
                }
            }
        }

        // Remove match from UnusedImports and return
        if (match != null) {
            if (match.isInclusive()) match.addFoundClassName(aName);
            match._used = true;
        }
        return match;
    }

    /**
     * Returns a Class name for given name referenced in file.
     */
    public String getImportClassName(String aName)
    {
        // Handle fully specified name
        if (isKnownClassName(aName))
            return aName;

        // If name has parts, handle them separately
        if (aName.indexOf('.') > 0) {
            String names[] = aName.split("\\.");
            String cname = getImportClassName(names[0]);
            if (cname == null) return null;
            JavaClass cdecl = (JavaClass) getJavaDecl(cname);
            for (int i = 1; cdecl != null && i < names.length; i++)
                cdecl = cdecl.getClassDecl(names[i]);
            return cdecl != null ? cdecl.getName() : null;
        }

        // Try "java.lang" + name
        JavaDecl jld = getJavaDecl("java.lang." + aName);
        if (jld != null && jld.isClass())
            return jld.getName();

        // If file declares package, see if it's in package
        String pname = getPackageName();
        if (pname != null && pname.length() > 0) {
            String cname = pname + '.' + aName;
            if (isKnownClassName(cname))
                return cname;
        }

        // Get import for name
        JImportDecl imp = getImport(aName);
        if (imp != null)
            return imp.getImportClassName(aName);

        // Return null since class not found
        return null;
    }

    /**
     * Returns a Class name for given name referenced in file.
     */
    public JavaDecl getImportClassMember(String aName, JavaType[] theParams)
    {
        JImportDecl imp = getStaticImport(aName, theParams);
        if (imp != null)
            return imp.getImportMember(aName, theParams);
        return null;
    }

    /**
     * Returns an import that can be used to resolve the given name.
     */
    private JImportDecl getStaticImport(String aName, JavaType[] theParams)
    {
        // Iterate over imports to see if any can resolve name
        for (int i = _importDecls.size() - 1; i >= 0; i--) {
            JImportDecl imp = _importDecls.get(i);

            // If import is static ("import static xxx.*") and name/params is known field/method, return member
            if (imp.isStatic()) {
                JavaDecl mbr = imp.getImportMember(aName, theParams);
                if (mbr != null) {
                    if (imp.isInclusive()) imp.addFoundClassName(aName);
                    imp._used = true;
                    return imp;
                }
            }
        }

        // Return null since import not found
        return null;
    }

    /**
     * Returns unused imports for file.
     */
    public Set<JImportDecl> getUnusedImports()
    {
        if (_unusedImports != null) return _unusedImports;
        resolveClassNames(this);
        Set<JImportDecl> uimps = new HashSet();
        for (JImportDecl imp : getImportDecls()) if (!imp._used) uimps.add(imp);
        return _unusedImports = uimps;
    }

/** Print expanded imports. */
/*private void printExpandedExports()
{
    // If no expansions, just return
    boolean hasExp = false; for(JImportDecl i : getImportDecls()) if(i.isInclusive()) hasExp = true;
    if(!hasExp) return;
    
    // Print expansions
    System.out.println("Expanded imports in file " + getClassName() + ":");
    for(JImportDecl imp : getImportDecls()) {
        if(imp.isInclusive() && !imp.isStatic() && imp.getFoundClassNames().size()>0) {
            System.out.print("    " + imp.getString().trim().replace(';',':') + ' ');
            List <String> names = new ArrayList(imp.getFoundClassNames());
            String last = names.size()>0? names.get(names.size()-1):null;
            for(String n : names) {
                System.out.print(n); if(n!=last) System.out.print(", "); else System.out.println(); }
        }
    }
}*/

    /**
     * Forces all nodes to resolve class names.
     */
    private void resolveClassNames(JNode aNode)
    {
        // Handle JType
        if (aNode instanceof JType || aNode instanceof JExprId)
            aNode.getDecl();

        // Recurse for children
        for (JNode child : aNode.getChildren())
            resolveClassNames(child);
    }

    /**
     * Returns the exception if one was hit.
     */
    public Exception getException()
    {
        return _exception;
    }

    /**
     * Sets the exception.
     */
    public void setException(Exception anException)
    {
        _exception = anException;
    }

    /**
     * Init from another JFile.
     */
    protected void init(JFile aJFile)
    {
        _name = aJFile._name;
        _startToken = aJFile._startToken;
        _endToken = aJFile._endToken;
        _children = aJFile._children;
        for (JNode c : _children) c._parent = this;

        _sourceFile = aJFile._sourceFile;
        _resolver = aJFile._resolver;
        _packageDecl = aJFile._packageDecl;
        _importDecls = aJFile._importDecls;
        _classDecls = aJFile._classDecls;
        _exception = aJFile._exception;
    }

}