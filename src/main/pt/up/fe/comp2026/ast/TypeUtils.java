package pt.up.fe.comp2026.ast;

import pt.up.fe.comp.jmm.analysis.table.MethodSymbol;
import pt.up.fe.comp.jmm.analysis.table.Signature;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmPrimitiveType;
import pt.up.fe.comp.jmm.analysis.table.type.JmmType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmArrayType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmClassType;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2026.symboltable.JmmSymbolTable;

import static pt.up.fe.comp2026.jmm.ast.JmmKind.*;


/**
 * Utility methods regarding types.
 */
public class TypeUtils {


    private final JmmSymbolTable table;

    public TypeUtils(SymbolTable table) {
        this.table = (JmmSymbolTable) table;
    }

    public static TypeUtils with(SymbolTable table) {
        return new TypeUtils(table);
    }

    public static JmmPrimitiveType intType() {
        return JmmPrimitiveType.INT;
    }


    public JmmType convertType(JmmNode typeNode) {
        assert (TYPE.check(typeNode));
        var name = typeNode.getChild(0).get("name");
        var dims=typeNode.getObjectAsList("dims");
        var type=JmmPrimitiveType.fromString(name);

        boolean isPrimitive = type.isPresent();
        boolean isArray = !dims.isEmpty();
        boolean isImported = false;
        var superName = table.getSuperFullyQualifiedName();

        if(!isPrimitive){

            //if is an imported class get qualifed name
            if (table.getImportNames().contains(name)) {
                name = table.getImportedFullyQualifiedName(name).get();
                isImported = true;
            }
            // if its the class itself
            else if (table.getClassName().equals(name)) {
                name = table.getFullyQualifiedName();
            }
            // if it is the extended object
            else if (superName != null && (superName.endsWith("."+name))) {
                name = table.getSuperFullyQualifiedName();
                isImported = true;
            }

            //TODO verify static context
            var typeclass = new JmmClassType(name,isImported,false);

            if(isArray) return new JmmArrayType(typeclass, dims.size());
            else return typeclass;
        }

        if(isArray) return new JmmArrayType(type.get(), dims.size());
        else return type.get();
    }


    /**
     * Gets the {@link JmmType} of an arbitrary expression.
     *
     * @param expr
     * @return
     */
    public JmmType getExprType(JmmNode expr) {
        return switch (expr.getKind()) {
            case INTEGER_LITERAL -> intType();
            case BOOL_LITERAL -> JmmPrimitiveType.BOOLEAN;
            case BINARY_EXPR, EQUALITY_EXPR, LOGICAL_EXPR -> getBinExprType(expr);
            case VAR_REF_EXPR -> getVarExprType(expr);
            case PARENTESES -> getParentesesExprType(expr);
            default ->
                    throw new UnsupportedOperationException("Can't compute type for expression kind '" + expr.getKind() + "'");
        };
    }



    public Signature getMethodDeclSignature(JmmNode methodDecl) {
        // Ensure given node is a MethodDecl
        METHOD_DECL.check(methodDecl);

        // Get name of the method
        var methodName = methodDecl.get("name");

        var params = methodDecl.getChildren(PARAM).stream()
                .map(param -> this.convertType(param.getChild(0)))
                .toList();

        // Create method signature with method name and types of parameters
        return new Signature(methodName, params);
    }


    private JmmType getBinExprType(JmmNode binaryExpr) {

        // Get operator
        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "-", "*", "/", "%" -> intType();
            case "<", ">", "<=", ">=", "==", "!=", "&&", "||" -> JmmPrimitiveType.BOOLEAN;
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }


    private JmmType getVarExprType(JmmNode varRefExpr) {
        var varRefName = varRefExpr.get("name");

        //verify if implicit field
        if (table.getField(varRefName).isPresent()) {
            return table.getField(varRefName).get().type();
        }

        //get method of variable
        var methodDecl = varRefExpr.getAncestor(METHOD_DECL);
        if (methodDecl.isEmpty()) return null;

        Signature methodSignature = this.getMethodDeclSignature(methodDecl.get());
        MethodSymbol method = table.getMethod(methodSignature).get();

        JmmType varType;

        //verify if a parameter
        if (method.getParameter(varRefName).isPresent()) {
            varType = method.getParameter(varRefName).get().type();
        }
        else varType = method.getLocalVariable(varRefName).get().type();

        return varType;
    }


    public JmmType getParentesesExprType(JmmNode parentesesExpr) {
        return getExprType(parentesesExpr.getChild(0));
    }

}
