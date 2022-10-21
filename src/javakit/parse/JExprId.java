package javakit.parse;
import javakit.resolver.*;

/**
 * A JExpr subclass for identifiers.
 */
public class JExprId extends JExpr {

    /**
     * Creates a new identifier.
     */
    public JExprId()
    {
    }

    /**
     * Creates a new identifier for given value.
     */
    public JExprId(String aName)
    {
        setName(aName);
    }

    /**
     * Override to resolve id name from parents.
     */
    protected JavaDecl getDeclImpl()
    {
        return getDeclForChildNode(this);
    }

    /**
     * Returns whether this is variable identifier.
     */
    public boolean isVarId()
    {
        JavaDecl decl = getDecl();
        return decl instanceof JavaLocalVar;
    }

    /**
     * Returns whether this is Class identifier.
     */
    public boolean isClassId()
    {
        JavaDecl decl = getDecl();
        return decl instanceof JavaClass;
    }

    /**
     * Returns whether this is Enum identifier.
     */
    public boolean isEnumId()
    {
        JavaDecl jd = getDecl();
        JavaType evalType = jd != null ? jd.getEvalType() : null;
        return evalType != null && evalType.isEnum();
    }

    /**
     * Returns whether this is Enum identifier.
     */
    public boolean isEnumConstId()
    {
        JavaDecl decl = getDecl();
        return decl instanceof JavaField && decl.getEvalType().isEnum();
    }

    /**
     * Returns whether this is ClassField identifier.
     */
    public boolean isFieldId()
    {
        JavaDecl decl = getDecl();
        return decl instanceof JavaField;
    }

    /**
     * Returns whether this id is a reference to a field.
     */
    public boolean isFieldRef()
    {
        return isFieldId() && !isVarDeclId();
    }

    /**
     * Returns whether this identifier is a method name.
     */
    public boolean isMethodId()
    {
        JavaDecl decl = getDecl();
        return decl instanceof JavaMethod;
    }

    /**
     * Returns the method call if parent is method call.
     */
    public boolean isMethodCall()
    {
        return isMethodId() && getMethodCall() != null;
    }

    /**
     * Returns the method call if parent is method call.
     */
    public JExprMethodCall getMethodCall()
    {
        JNode p = getParent();
        return p instanceof JExprMethodCall ? (JExprMethodCall) p : null;
    }

    /**
     * Returns whether this id is a JFieldDecl id (actually inside the JFieldDecl).
     */
    public boolean isFieldDeclId()
    {
        return isFieldId() && getFieldDecl() != null;
    }

    /**
     * Returns the method declaration if parent is method declaration.
     */
    public JFieldDecl getFieldDecl()
    {
        JNode vd = getVarDecl();
        JNode vdp = vd != null ? vd.getParent() : null;
        return vdp instanceof JFieldDecl ? (JFieldDecl) vdp : null;
    }

    /**
     * Returns whether this id is a JMethodDecl id (actually inside the JMethodDecl).
     */
    public boolean isMethodDeclId()
    {
        JMethodDecl md = getMethodDecl();
        return md != null && md.getId() == this;
    }

    /**
     * Returns the method declaration if parent is method declaration.
     */
    public JMethodDecl getMethodDecl()
    {
        JNode p = getParent();
        return p instanceof JMethodDecl ? (JMethodDecl) p : null;
    }

    /**
     * Returns whether this id is a JVarDecl id (actually inside the JVarDecl).
     */
    public boolean isVarDeclId()
    {
        JVarDecl vd = getVarDecl();
        return vd != null && vd.getId() == this;
    }

    /**
     * Returns the variable declaration if parent is variable declaration.
     */
    public JVarDecl getVarDecl()
    {
        JNode p = getParent();
        return p instanceof JVarDecl ? (JVarDecl) p : null;
    }

    /**
     * Returns whether this is package identifier.
     */
    public boolean isPackageName()
    {
        JavaDecl decl = getDecl();
        return decl instanceof JavaPackage;
    }

    /**
     * Returns the full package name for this package identifier.
     */
    public String getPackageName()
    {
        JavaDecl decl = getDecl();
        JavaPackage pkg = decl instanceof JavaPackage ? (JavaPackage) decl : null;
        return pkg != null ? pkg.getName() : null;
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()
    {
        JavaDecl decl = getDecl();
        if (decl == null) return "UnknownId";
        switch (decl.getType()) {
            case Class: return "ClassId";
            case Constructor: return "ConstrId";

            case Field: {
                JavaField field = (JavaField) decl;
                JavaClass fieldClass = field.getDeclaringClass();
                if (fieldClass != null && fieldClass.isEnum())
                    return "EnumId";
                return "FieldId";
            }

            case Method: return "MethodId";
            case Package: return "PackageId";
            case TypeVar: return "TypeVarId";
            case VarDecl: return "VariableId";
            default: return "UnknownId";
        }
    }
}