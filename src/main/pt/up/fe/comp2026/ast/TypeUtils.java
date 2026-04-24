package pt.up.fe.comp2026.ast;

import pt.up.fe.comp.jmm.analysis.table.MethodSymbol;
import pt.up.fe.comp.jmm.analysis.table.Signature;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmPrimitiveType;
import pt.up.fe.comp.jmm.analysis.table.type.JmmType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmArrayType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmClassType;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2026.jmm.ast.JmmAttributes;
import pt.up.fe.comp2026.jmm.ast.JmmKind;
import pt.up.fe.comp2026.symboltable.JmmSymbolTable;

import java.util.Collections;
import java.util.Optional;

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

        var size = 0;
        var dims = Collections.emptyList();
        dims = typeNode.getObjectAsList("dims");


        var type = JmmPrimitiveType.fromString(name);

        boolean isPrimitive = type.isPresent();
        boolean isArray = !dims.isEmpty();


        if (!isPrimitive) {
            var typeclass = resolveClass(name);
            if (isArray) return new JmmArrayType(typeclass, dims.size());
            else return typeclass;

        }

        if (isArray) {
            return new JmmArrayType(type.get(), dims.size());
        } else return type.get();
    }


    /**
     * Gets the {@link JmmType} of an arbitrary expression.
     *
     * @param expr
     * @return
     */
    public JmmType getExprType(JmmNode expr) {
        return switch (expr.getKind()) {
            case INTEGER_LITERAL, LEFT_UNARY_EXPR, RIGHT_UNARY_EXPR, LENGTH -> JmmPrimitiveType.INT;
            case BOOL_LITERAL, LOGICAL_NOT -> JmmPrimitiveType.BOOLEAN;
            case BINARY_EXPR, EQUALITY_EXPR, LOGICAL_EXPR -> getBinExprType(expr);
            case VAR_REF_EXPR -> getVarExprType(expr);
            case PARENTESES -> getParentesesExprType(expr);
            case CLASS_INITIALIZATION -> {
                var name = expr.get("name");
                yield resolveClass(name);
            }
            case EXPLICIT_METHOD_CALL -> getExplicitMethodCallType(expr);
            case THIS -> resolveClass(table.getClassName());
            case IMPLICIT_METHOD_CALL -> getImplicitMethodCallType(expr);
            case ARRAY_ACCESS -> getArrayAccessType(expr);
            case NEW_ARRAY, ARRAY_INIT -> getArrayType(expr);
            case PARAM -> convertType(expr.getChild(0));
            case ARRAY_ELEMENT, ARRAY_INITIALIZER -> getExprType(expr.getChild(0));
            case FIELD_ACESS -> getFieldAccessType(expr, table);
            default ->
                    throw new UnsupportedOperationException("Can't compute type for expression kind '" + expr.getKind() + "'");
        };
    }

    private JmmType getArrayType(JmmNode array) {
        int size;
        if (array.getKind() == ARRAY_INIT) {
            size = array.getObjectAsList("dims").size();
        } else {
            size = array.getObjectAsList("size").size();
        }

        var name = array.getChild(0).get("name");
        var type = JmmPrimitiveType.fromString(name);

        if (type.isPresent()) {
            return new JmmArrayType(type.get(), size);
        } else {
            var typeclass = resolveClass(name);
            return new JmmArrayType(typeclass, size);
        }

    }

    private JmmType getImplicitMethodCallType(JmmNode methodCall) {
        String methodName = methodCall.get("methodname");
        return getLocalMethodReturnType(methodName);
    }

    private JmmType getLocalMethodReturnType(String methodName) {
        var methods = table.getMethods(methodName);
        if (methods.isEmpty()) {
            // check superclass
            String superName = table.getSuperFullyQualifiedName();
            if (superName != null && !superName.isEmpty()) {
                var superTable = table.getImportedSymbolTable(superName);
                if (superTable.isPresent()) {
                    var superMethods = superTable.get().getMethods(methodName);
                    if (!superMethods.isEmpty()) {
                        return superMethods.get(0).returnType();
                    }
                }
            }
            return new JmmClassType("java.lang.Object", true, false);
        }
        return methods.get(0).returnType();
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
            case "+", "-", "*", "/", "%" -> JmmPrimitiveType.INT;
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
            return varType = method.getParameter(varRefName).get().type();
        }
        if (method.getLocalVariable(varRefName).isPresent()) {
            return method.getLocalVariable(varRefName).get().type();
        }

        if (table.getClassName().equals(varRefName) || table.getImportNames().contains(varRefName)) {
            return resolveClass(varRefName, true);
        }

        return null;
    }


    private JmmType resolveClass(String name) {
        return resolveClass(name, false);
    }

    private JmmType resolveClass(String name, Boolean isStatic) {
        boolean isImported = false;
        var superName = table.getSuperFullyQualifiedName();
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
        else if (superName != null && (superName.endsWith("." + name))) {
            name = table.getSuperFullyQualifiedName();
            isImported = true;
        } else if (table.isImplicitImport(name)) {
            name = "java.lang." + name;
            isImported = true;
        }

        var staticRef = isStatic;
        return new JmmClassType(name, isImported, staticRef);
    }


    public JmmType getParentesesExprType(JmmNode parentesesExpr) {
        return getExprType(parentesesExpr.getChild(0));
    }

    private JmmType getMethodReturnTypeInHierarchy(SymbolTable currentTable, Signature methodSignature) {
        var localMethods = currentTable.getMethod(methodSignature);
        if (localMethods.isPresent()) return localMethods.get().returnType();

        String superName = currentTable.getSuperFullyQualifiedName();

        if (superName != null && !superName.isEmpty()) {
            var superTable = table.getImportedSymbolTable(superName);
            if (superTable.isPresent()) {
                return getMethodReturnTypeInHierarchy(superTable.get(), methodSignature);
            }
        }
        return null;
    }

    private JmmType getExplicitMethodCallType(JmmNode explMethodCall) {
        String methodName = explMethodCall.get("methodname");

        var argTypes = explMethodCall.getChildren().stream().skip(1)
                .map(arg -> getExprType(arg))
                .toList();

        Signature methodSignature = new Signature(methodName, argTypes);

        //verifies if calls class method
        if (!table.getMethod(methodSignature).isEmpty())
            return table.getMethod(methodSignature).get().returnType();

        var calleeType = getExprType(explMethodCall.getChild(0));

        if (calleeType instanceof JmmClassType jmmClass && (true || jmmClass.isImported() || table.isImplicitImport(jmmClass.name()))) {;
            var importTable = table.getImportedSymbolTable(jmmClass.fullyQualifiedName());

            //checks if imported table contains method
            if (importTable.isPresent() && !importTable.get().getMethods().isEmpty()) {
                return importTable.get().getMethod(methodSignature).get().returnType();
            }
        }

        // verifies if in extends (recursive)
        if (table.getSuperFullyQualifiedName() != null) {
            var importTable = table.getImportedSymbolTable(table.getSuperFullyQualifiedName());
            if (importTable.isPresent() && importTable.get().getMethod(methodSignature).isPresent()) {
                return getMethodReturnTypeInHierarchy(table, methodSignature);
            }
        }



        //default to int
        return JmmPrimitiveType.INT;
    }

    private JmmType getArrayAccessType(JmmNode arrayAccess) {
        JmmType arrayType = getVarExprType(arrayAccess.getChild(0));
        int dimsAccessed = arrayAccess.getChildren(EXPR).size() - 1;   // remove expr for var

        System.out.println("-------------------------");
        System.out.println(dimsAccessed);

        if (arrayType instanceof JmmArrayType jmmArrayType) {
            JmmType itemType = jmmArrayType.itemType();
            int totalArrayDims = jmmArrayType.dimension();

            if (totalArrayDims > dimsAccessed) {
                return new JmmArrayType(itemType, totalArrayDims - dimsAccessed);
            } else if (totalArrayDims == dimsAccessed)
                return itemType;
        }

        return JmmPrimitiveType.INT;
    }

    private JmmType getFieldAccessType(JmmNode fieldAccess, JmmSymbolTable currentTable) {
        String varRefName = fieldAccess.get("name");

        if (currentTable.getField(varRefName).isPresent()) {
            return currentTable.getField(varRefName).get().type();
        }

        if (currentTable.getSuperFullyQualifiedName() != null) {
            JmmSymbolTable superTable = (JmmSymbolTable) currentTable.getImportedSymbolTable(currentTable.getSuperFullyQualifiedName()).get();
            return getFieldAccessType(fieldAccess, superTable);
        }
         return null;
    }



    public Optional<JmmType> getVarTypeFromName(JmmNode node, String varName) {
        Optional<JmmType> type = Optional.empty();
        Optional<MethodSymbol> curentMethod = Optional.empty();

        //verify if assignment is done in a method
        if (node.getAncestor(JmmKind.METHOD_DECL).isPresent())
            curentMethod = table.getMethod(getMethodDeclSignature(node.getAncestor(JmmKind.METHOD_DECL).get()));

        //if var in locals
        if (curentMethod.isPresent() && curentMethod.get().getLocalVariable(varName).isPresent())
            type = Optional.of(curentMethod.get().getLocalVariable(varName).get().type());

        //if var in parameters
        if (curentMethod.isPresent() && curentMethod.get().getParameter(varName).isPresent())
            type = Optional.of(curentMethod.get().getParameter(varName).get().type());

        //if var in class field
        if (table.getField(varName).isPresent())
            type = Optional.of(table.getField(varName).get().type());

        return type;
    }

    public int getExprValue(JmmNode expr) {
        return switch (expr.getKind()) {
            case INTEGER_LITERAL -> Integer.parseInt(expr.get("value"));
            case LEFT_UNARY_EXPR ->{
                int value=getExprValue(expr.getChild(0));
                String op=expr.get("op");
                switch( op){
                    case "--":
                        yield --value;
                    case "++":
                        yield ++value;
                    case "-":
                        yield  -value;
                    case  "+":
                        yield  +value;

                    default:
                        throw new RuntimeException("Unknown leftunary operator"+op);
                }
            }
            case RIGHT_UNARY_EXPR  ->{
                int value=getExprValue(expr.getChild(0));
                String op=expr.get("op");
                switch( op){
                    case "--":
                        yield value--;
                    case "++":
                        yield value++;

                    default:
                        throw new RuntimeException("Unknown rightunary operator"+op);
                }
            }
            case BINARY_EXPR ->{
                int lv=getExprValue(expr.getChild(0));
                int rv=getExprValue(expr.getChild(1));
                System.out.println();
                switch( expr.get("op")){
                    case "+":
                        yield lv+rv;
                    case "-":
                        yield lv-rv;
                    case "/":
                        yield lv/rv;
                    case  "*":
                        yield lv*rv;
                    case "%":
                        yield lv%rv;
                    default:
                        throw new RuntimeException("Unknown binary expression"+expr);
                }
            }
            case VAR_REF_EXPR -> {
                resolveScope(expr);
                yield 0;
            }
            default ->
                    throw new RuntimeException("Can't resolve"+expr);
        };
    }

    private int resolveScope(JmmNode var) {
        String name=var.get("name");
        var ancestor=var.getAncestor(METHOD_DECL);
        if(ancestor.isPresent()){
            Signature methodDeclSignature = getMethodDeclSignature(ancestor.get());
            MethodSymbol method = table.getMethod(methodDeclSignature).get();
            Optional<Symbol> name1 = method.getLocalVariable(name);
            if(name1.isPresent()){
                System.out.println("LOOK");
                name1.get().print();
                return  0;
            }
            else {
                name1= method.getParameter(name);
                if(name1.isPresent()){
                    return  0;
                }
            }

        }
        else {
            Optional<Symbol> name1= table.getField(name);
            if(name1.isPresent()){
                name1.get().print();
                return  0;
            }
        }

        return  0;
    }


}
