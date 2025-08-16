package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }
        String outputDir = args[0];
        defineAst(outputDir, "Expr", Arrays.asList(
            "Binary : Expr left, Token operator, Expr right",
            "Grouping : Expr expression",
            "Literal : Object value",
            "Unary : Token operator, Expr right",
            "Ternary : Expr cond, Token operator, Expr trueBranch, Expr falseBranch",
            "Variable : Token name",
            "Assign : Token name, Expr rhs",
            "Index : Token lbrack, Expr object, Expr idxExpr, Expr idxExpr2",
            "IndexPrefix : Token operator, Expr object, Expr idxExpr",
            "IndexPostfix : Token operator, Expr object, Expr idxExpr",
            "IndexAssign : Token lbrack, Expr object, Expr idxExpr, Expr rhs",
            "Prefix : Token operator, Token name",
            "Postfix : Token name, Token operator",
            "Call : Expr callee, Token paren, List<Expr> args",
            "Anonymous : List<Token> params, Stmt body",
            "Get : Expr object, Token name",
            "Set : Expr object, Token name, Expr rhs",
            "This: Token keyword",
            "Super: Token keyword, Token property",
            "ListExpr: List<Expr> exprs",
            "MapExpr: List<Map<Expr,Expr>> KeyValuePairs"
        ));
        defineAst(outputDir, "Stmt", Arrays.asList(
            "Expression : Expr expression",
            "Print : Expr expression",
            "Var : Token name, Expr initializer",
            "If : Expr condition, Stmt thenStmt, Stmt elseStmt",
            "While : Expr condition, Stmt body",
            "For : Stmt initialization, Expr condition, Stmt update, Stmt body",
            "Foreach : Expr.Variable iterator, Expr iterable, Stmt body",
            "Block : List<Stmt> statements",
            "Break : Token keyword",
            "Continue : Token keyword",
            "FunctionDef : Token name, List<Token> params, Stmt body, Boolean isStaticMethod, Boolean isGetterMethod",
            "Return : Token keyword, Expr value",
            "Class : Token name, List<Expr.Variable> superClasses, List<Stmt.FunctionDef> methods"
        ));
    }

    public static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path, "UTF-8");

        writer.println("package com.craftinginterpreters.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println("import java.util.Map;");
        writer.println();
        writer.println("abstract class " + baseName + " {");
        defineVisitor(writer, baseName, types);
        writer.println("\tabstract <R> R accept(" + baseName + "Visitor<R> visitor);");
        for (String type : types) {
            String className = type.split(":")[0].trim();
            String fields; 
            if (type.split(":").length > 1) {
                fields = type.split(":")[1].trim();
            } else {
                fields = "";
            }
            defineType(writer, baseName, className, fields);
        }
        writer.println("}");
        writer.close();
    }

    public static void defineType(PrintWriter writer, String baseName, String className, String fieldList) throws IOException {
        writer.println("\tstatic class " + className + " extends " + baseName + " {");

        // Constructor
        writer.println("\t\t" + className + "(" + fieldList + ") {");

        // Store params in fields
        String[] fields = new String[0];
        if (!fieldList.equals("")) {
            // Don't just split on commas - we need to handle generic types
            List<String> fieldsList = new ArrayList<>();
            int bracketDepth = 0;
            StringBuilder currentField = new StringBuilder();
            
            for (int i = 0; i < fieldList.length(); i++) {
                char c = fieldList.charAt(i);
                
                if (c == '<') bracketDepth++;
                else if (c == '>') bracketDepth--;
                
                // Only split on commas when not inside angle brackets
                if (c == ',' && bracketDepth == 0) {
                    fieldsList.add(currentField.toString().trim());
                    currentField = new StringBuilder();
                } else {
                    currentField.append(c);
                }
            }
            
            // Add the last field
            if (currentField.length() > 0) {
                fieldsList.add(currentField.toString().trim());
            }
            
            fields = fieldsList.toArray(new String[0]);
            
            for (String field : fields) {
                String trimmedField = field.trim();
                // Extract the parameter name (last word in the field)
                String name = trimmedField.substring(trimmedField.lastIndexOf(' ') + 1);
                writer.println("\t\t\tthis." + name + " = " + name + ";");
            }
        }

        writer.println("\t\t}");

        // Define accept()
        writer.println("\n\t\t@Override");
        writer.println("\t\tpublic <R> R accept(" + baseName + "Visitor<R> visitor) {");
        writer.println("\t\t\treturn visitor.visit" + className + baseName + "(this);");
        writer.println("\t\t}\n");

        // Define fields
        for (String field : fields) {
            writer.println("\t\tfinal " + field.trim() + ";");
        }
        
        writer.println("\t}");
    }

    public static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println("\tinterface " + baseName + "Visitor<R> {");
        String hack = null;
        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            // Hack for arg names that match Java keywords
            if (typeName.toLowerCase().equals("if") || 
                typeName.toLowerCase().equals("while") ||
                typeName.toLowerCase().equals("break") ||
                typeName.toLowerCase().equals("continue") ||
                typeName.toLowerCase().equals("return") ||
                typeName.toLowerCase().equals("for") ||
                typeName.toLowerCase().equals("this") ||
                typeName.toLowerCase().equals("super") ||
                typeName.toLowerCase().equals("class")) {
                hack = typeName.toLowerCase() + baseName;
            } else {
                hack = typeName.toLowerCase();
            }
            writer.println("\t\tR visit" + typeName + baseName + "(" 
                + baseName + "." + typeName + " " + hack + ");");
        }
        writer.println("\t}");
    }
}
