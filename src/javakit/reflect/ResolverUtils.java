package javakit.reflect;
import javakit.parse.*;
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
        if (anObj instanceof Class)
            return getIdForClass((Class<?>) anObj);

        // Handle java.lang.reflect.Member (Field, Method, Constructor)
        if (anObj instanceof Member)
            return getIdForMember((Member) anObj);

        // Handle java.lang.reflect.Type (ParameterizedType, TypeVariable, GenericArrayType, WildcardType)
        else if (anObj instanceof Type)
            return getIdForType((Type) anObj);

        // Handle JVarDecl
        else if (anObj instanceof JVarDecl)
            return getIdForJVarDecl((JVarDecl) anObj);

        // Handle String (package name)
        else if (anObj instanceof String)
            return (String) anObj;

        // Handle array of types
        else if (anObj instanceof Object[])
            return getIdForTypeArray((Object[]) anObj);

        // Complain about anything else
        else throw new RuntimeException("ResolverUtils.getId: Unsupported type: " + anObj);
    }

    /**
     * Returns an Id for a Java.lang.Class.
     */
    private static String getIdForClass(Class<?> aClass)
    {
        if (aClass.isArray())
            return getIdForClass(aClass.getComponentType()) + "[]";
        return aClass.getName();
    }

    /**
     * Returns an Id for a Java.lang.reflect.Member.
     */
    private static String getIdForMember(Member aMember)
    {
        // Get id for Member.DeclaringClass
        Class<?> declaringClass = aMember.getDeclaringClass();
        String classId = getId(declaringClass);

        // Start StringBuffer
        StringBuffer sb = new StringBuffer(classId);

        // Handle Field: DeclClassName.<Name>
        if (aMember instanceof Field)
            sb.append('.').append(aMember.getName());

        // Handle Method: DeclClassName.Name(<ParamType>,...)
        else if (aMember instanceof Method) {
            Method meth = (Method) aMember;
            sb.append('.').append(meth.getName()).append('(');
            Class<?>[] paramTypes = meth.getParameterTypes();
            if (paramTypes.length > 0) {
                String paramTypesId = getIdForTypeArray(paramTypes);
                sb.append(paramTypesId);
            }
            sb.append(')');
        }

        // Handle Constructor: DeclClassName(<ParamType>,...)
        else if (aMember instanceof Constructor) {
            Constructor<?> constr = (Constructor<?>) aMember;
            Class<?>[] paramTypes = constr.getParameterTypes();
            sb.append('(');
            if (paramTypes.length > 0) {
                String paramTypesId = getIdForTypeArray(paramTypes);
                sb.append(paramTypesId);
            }
            sb.append(')');
        }

        // Return
        return sb.toString();
    }

    /**
     * Returns an Id for Java.lang.reflect.Type.
     */
    public static String getIdForType(Type aType)
    {
        // Handle GenericArrayType: CompType[]
        if (aType instanceof GenericArrayType)
            return getIdForGenericArrayType((GenericArrayType) aType);

        // Handle ParameterizedType: RawType<TypeArg,...>
        if (aType instanceof ParameterizedType)
            return getIdForParameterizedType((ParameterizedType) aType);

        // Handle TypeVariable: DeclType.Name
        if (aType instanceof TypeVariable) {
            TypeVariable typeVar = (TypeVariable) aType;
            GenericDeclaration genericDecl = typeVar.getGenericDeclaration();
            String genericDeclId = getId(genericDecl);
            String typeVarName = typeVar.getName();
            return new StringBuffer(genericDeclId).append('.').append(typeVarName).toString();
        }

        // Handle WildcardType: Need to fix for
        WildcardType wc = (WildcardType) aType;
        Type[] bounds = wc.getLowerBounds().length > 0 ? wc.getLowerBounds() : wc.getUpperBounds();
        Type bound = bounds[0];
        String boundStr = getIdForType(bound);
        return boundStr;
    }

    /**
     * Returns an Id for Java.lang.reflect.ParameterizedType.
     */
    public static String getIdForGenericArrayType(GenericArrayType genericArrayType)
    {
        Type compType = genericArrayType.getGenericComponentType();
        String compTypeStr = getIdForType(compType);
        return compTypeStr + "[]";
    }

    /**
     * Returns an Id for Java.lang.reflect.ParameterizedType.
     */
    public static String getIdForParameterizedType(ParameterizedType parameterizedType)
    {
        // Append RawType
        Type rawType = parameterizedType.getRawType();
        String rawTypeId = getId(rawType);
        StringBuffer sb = new StringBuffer(rawTypeId);

        // Append TypeArgs
        Type[] typeArgs = parameterizedType.getActualTypeArguments();
        sb.append('<');
        if (typeArgs.length > 0) {
            String typeArgsId = getIdForTypeArray(typeArgs);
            sb.append(typeArgsId);
        }

        // Return
        return sb.append('>').toString();
    }

    /**
     * Returns an id string for given Java part.
     */
    public static String getIdForParameterizedTypeParts(JavaDecl aDecl, JavaDecl[] theTypeDecls)
    {
        StringBuilder sb = new StringBuilder(aDecl.getId());
        sb.append('<').append(theTypeDecls[0].getId());
        for (int i = 1; i < theTypeDecls.length; i++)
            sb.append(',').append(theTypeDecls[i].getId());
        sb.append('>');
        return sb.toString();
    }

    /**
     * Returns an Id string for a Type array.
     */
    private static String getIdForTypeArray(Object[] typeArray)
    {
        StringBuffer sb = new StringBuffer();

        for (int i = 0, iMax = typeArray.length, last = iMax - 1; i < iMax; i++) {
            Object type = typeArray[i];
            String typeStr = getId(type);
            sb.append(typeStr);
            if (i != last)
                sb.append(',');
        }

        // Return
        return sb.toString();
    }

    /**
     * Returns an Id for JVarDecl.
     */
    private static String getIdForJVarDecl(JVarDecl varDecl)
    {
        StringBuffer sb = new StringBuffer();
        JType varDeclType = varDecl.getType();
        JavaType varType = varDeclType != null ? varDeclType.getDecl() : null;
        if (varType != null)
            sb.append(varType.getId()).append(' ');
        sb.append(varDecl.getName());
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
            Type base = pt.getRawType();
            Type[] types = pt.getActualTypeArguments();
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
    public static Class<?> getClassForType(Type aType)
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
            Type[] boundsTypes = wc.getLowerBounds().length > 0 ? wc.getLowerBounds() : wc.getUpperBounds();
            Type boundsType = boundsTypes.length > 0 ? boundsTypes[0] : Object.class;
            return getClassForType(boundsType);
        }

        // Complain about anything else
        throw new RuntimeException("JavaKitUtils.getClass: Can't get class from type: " + aType);
    }
}