package javakit.resolver;
import javakit.parse.*;
import javakit.reflect.JavaDecl;
import javakit.reflect.JavaType;

import java.lang.reflect.*;

/**
 * Utility methods for JavaParse package.
 */
public class ResolverUtils {

    /**
     * Returns an id string for given Java part.
     */
    public static String getId(Object anObj)
    {
        // Handle Class: <Name>
        if (anObj instanceof Class) {
            Class cls = (Class) anObj;
            if (cls.isArray()) return getId(cls.getComponentType()) + "[]";
            return cls.getName();
        }

        // Create StringBuffer
        StringBuffer sb = new StringBuffer();

        // Handle Field: DeclClassName.<Name>
        if (anObj instanceof Field) {
            Field field = (Field) anObj;
            sb.append(field.getDeclaringClass()).append('.').append(field.getName());
        }

        // Handle Method: DeclClassName.Name(<ParamType>,...)
        else if (anObj instanceof Method) {
            Method meth = (Method) anObj;
            Class[] paramTypes = meth.getParameterTypes();
            sb.append(getId(meth.getDeclaringClass())).append('.').append(meth.getName()).append('(');
            if (paramTypes.length > 0)
                sb.append(getId(paramTypes));
            sb.append(')');
        }

        // Handle Constructor: DeclClassName(<ParamType>,...)
        else if (anObj instanceof Constructor) {
            Constructor constr = (Constructor) anObj;
            Class[] paramTypes = constr.getParameterTypes();
            sb.append(getId(constr.getDeclaringClass())).append('(');
            if (paramTypes.length > 0)
                sb.append(getId(paramTypes));
            sb.append(')');
        }

        // Handle ParameterizedType: RawType<TypeArg,...>
        else if (anObj instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) anObj;
            Type[] typeArgs = parameterizedType.getActualTypeArguments();
            sb.append(getId(parameterizedType.getRawType())).append('<');
            if (typeArgs.length > 0) sb.append(getId(typeArgs));
            sb.append('>');
        }

        // Handle TypeVariable: DeclType.Name
        else if (anObj instanceof TypeVariable) {
            TypeVariable typeVariable = (TypeVariable) anObj;
            sb.append(getId(typeVariable.getGenericDeclaration())).append('.').append(typeVariable.getName());
        }

        // Handle GenericArrayType: CompType[]
        else if (anObj instanceof GenericArrayType) {
            GenericArrayType gat = (GenericArrayType) anObj;
            sb.append(getId(gat.getGenericComponentType())).append("[]");
        }

        // Handle WildcardType
        else if (anObj instanceof WildcardType) {
            WildcardType wc = (WildcardType) anObj;
            if (wc.getLowerBounds().length > 0)
                sb.append(getId(wc.getLowerBounds()[0]));
            else sb.append(getId(wc.getUpperBounds()[0]));
        }

        // Handle JVarDecl
        else if (anObj instanceof JVarDecl) {
            JVarDecl varDecl = (JVarDecl) anObj;
            JType varDeclType = varDecl.getType();
            JavaType varType = varDeclType != null ? varDeclType.getDecl() : null;
            if (varType != null)
                sb.append(varType.getId()).append(' ');
            sb.append(varDecl.getName());
        }

        // Handle String (package name)
        else if (anObj instanceof String)
            sb.append(anObj);

        // Handle array of types
        else if (anObj instanceof Object[]) {
            Object[] types = (Object[]) anObj;
            for (int i = 0, iMax = types.length, last = iMax - 1; i < iMax; i++) {
                Object type = types[i];
                sb.append(getId(type));
                if (i != last) sb.append(',');
            }
        }

        // Complain about anything else
        else throw new RuntimeException("ResolverUtils.getId: Unsupported type: " + anObj);

        // Return string
        return sb.toString();
    }

    /**
     * Returns an id string for given Java part.
     */
    public static String getParamTypeId(JavaDecl aDecl, JavaDecl[] theTypeDecls)
    {
        StringBuilder sb = new StringBuilder(aDecl.getId());
        sb.append('<').append(theTypeDecls[0].getId());
        for (int i = 1; i < theTypeDecls.length; i++) sb.append(',').append(theTypeDecls[i].getId());
        sb.append('>');
        return sb.toString();
    }

    /**
     * Returns the class name, converting primitive arrays to 'int[]' instead of '[I'.
     */
    public static String getTypeName(Type aType)
    {
        // Handle Class
        if (aType instanceof Class)
            return getId(aType);

        // Handle GenericArrayType
        if (aType instanceof GenericArrayType) {
            GenericArrayType gat = (GenericArrayType) aType;
            return getTypeName(gat.getGenericComponentType()) + "[]";
        }

        // Handle ParameterizedType (e.g., Class <T>, List <T>, Map <K,V>)
        if (aType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) aType;
            Type base = pt.getRawType(), types[] = pt.getActualTypeArguments();
            StringBuffer sb = new StringBuffer(getTypeName(base)).append('<');
            for (int i = 0, iMax = types.length, last = iMax - 1; i < iMax; i++) {
                Type type = types[i];
                sb.append(getTypeName(type));
                if (i != last) sb.append(',');
            }
            return sb.append('>').toString();
        }

        // Handle TypeVariable
        if (aType instanceof TypeVariable) {
            TypeVariable tv = (TypeVariable) aType;
            //Type typ = tv.getBounds()[0]; return getTypeName(typ);
            return tv.getName();
        }

        // Handle WildcardType
        if (aType instanceof WildcardType) {
            WildcardType wc = (WildcardType) aType;
            if (wc.getLowerBounds().length > 0)
                return getTypeName(wc.getLowerBounds()[0]);
            return getTypeName(wc.getUpperBounds()[0]);
        }

        // Complain about anything else
        throw new RuntimeException("JavaKitUtils.getTypeName: Can't get name from type: " + aType);
    }

    /**
     * Returns the class name, converting primitive arrays to 'int[]' instead of '[I'.
     */
    public static Class getClassForType(Type aType)
    {
        // Handle Class
        if (aType instanceof Class)
            return (Class) aType;

        // Handle GenericArrayType
        if (aType instanceof GenericArrayType) {
            GenericArrayType gat = (GenericArrayType) aType;
            Class cls = getClassForType(gat.getGenericComponentType());
            return Array.newInstance(cls, 0).getClass();
        }

        // Handle ParameterizedType (e.g., Class <T>, List <T>, Map <K,V>)
        if (aType instanceof ParameterizedType)
            return getClassForType(((ParameterizedType) aType).getRawType());

        // Handle TypeVariable
        if (aType instanceof TypeVariable)
            return getClassForType(((TypeVariable) aType).getBounds()[0]);

        // Handle WildcardType
        if (aType instanceof WildcardType) {
            WildcardType wc = (WildcardType) aType;
            if (wc.getLowerBounds().length > 0)
                return getClassForType(wc.getLowerBounds()[0]);
            return getClassForType(wc.getUpperBounds()[0]);
        }

        // Complain about anything else
        throw new RuntimeException("JavaKitUtils.getClass: Can't get class from type: " + aType);
    }

    /**
     * Returns whether a JavaDecl is expected.
     */
    public static boolean isDeclExpected(JNode aNode)
    {
        if(aNode instanceof JExprLiteral) return !((JExprLiteral) aNode).isNull();
        try { return aNode.getClass().getDeclaredMethod("getDeclImpl")!=null; }
        catch(Exception e) { return false; }
    }
}