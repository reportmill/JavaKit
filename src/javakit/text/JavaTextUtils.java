/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.text;
import javakit.reflect.JavaDecl;
import snap.gfx.Font;
import snap.gfx.Image;

/**
 * This class contains utility support for Java text.
 */
public class JavaTextUtils {

    // The recommended default font for code
    private static Font  _codeFont;

    /**
     * Returns a suitable code font.
     */
    public static Font getCodeFont()
    {
        if (_codeFont == null) {
            String[] names = { "Monaco", "Consolas", "Courier"};
            for (String name : names) {
                _codeFont = new Font(name, 12);
                if (_codeFont.getFamily().startsWith(name))
                    break;
            }
        }

        // Return
        return _codeFont;
    }

    // Images
    public static Image LVarImage = Image.get(JavaTextUtils.class, "LocalVariable.png");
    public static Image FieldImage = Image.get(JavaTextUtils.class, "PublicField.png");
    public static Image MethodImage = Image.get(JavaTextUtils.class, "PublicMethod.png");
    public static Image ClassImage = Image.get(JavaTextUtils.class, "PublicClass.png");
    public static Image PackageImage = Image.get(JavaTextUtils.class, "Package.png");
    public static Image CodeImage = Image.get(JavaTextUtils.class, "Code.png");

    /**
     * Returns an icon image for given JavaDecl.
     */
    public static Image getImageForJavaDecl(JavaDecl aDecl)
    {
        switch (aDecl.getType()) {
            case VarDecl: return LVarImage;
            case Field: return FieldImage;
            case Method: return MethodImage;
            case Class: return ClassImage;
            case Package: return PackageImage;
            default: return null;
        }
    }
}