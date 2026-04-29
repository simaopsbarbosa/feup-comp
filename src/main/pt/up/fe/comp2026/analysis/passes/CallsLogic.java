package pt.up.fe.comp2026.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.MethodSymbol;
import pt.up.fe.comp.jmm.analysis.table.Signature;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2026.analysis.AnalysisVisitorWithTable;
import pt.up.fe.comp2026.ast.NodeUtils;
import pt.up.fe.comp2026.jmm.ast.JmmKind;


public class CallsLogic extends AnalysisVisitorWithTable {

    private MethodSymbol currentMethod;

    public CallsLogic(SymbolTable table) {
        super(table);
    }

    @Override
    protected void buildVisitor() {
        addVisit(JmmKind.METHOD_DECL, this::visitMethodDecl);
        addVisit(JmmKind.EXPLICIT_METHOD_CALL, this::visitExplicitMethodCall);
        addVisit(JmmKind.IMPLICIT_METHOD_CALL, this::visitImplicitMethodCall);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = table.getMethods(method.get("name")).getFirst();
        return null;
    }

    private boolean isMethodInHierarchy(SymbolTable currentTable, Signature methodSignature) {
        var localMethods = currentTable.getMethod(methodSignature).isPresent();
        if (localMethods) return true;

        String superName = currentTable.getSuperFullyQualifiedName();

        if (superName != null && !superName.isEmpty()) {
            var superTable = table.getImportedSymbolTable(superName);
            if (superTable.isPresent()) {
                return isMethodInHierarchy(superTable.get(), methodSignature);
            }
        }
        return false;
    }

    private MethodSymbol getMethodInHierarchy(SymbolTable currentTable, Signature methodSignature) {
        var localMethods = currentTable.getMethod(methodSignature);
        if (localMethods.isPresent()) return localMethods.get();

        String superName = currentTable.getSuperFullyQualifiedName();

        if (superName != null && !superName.isEmpty()) {
            var superTable = table.getImportedSymbolTable(superName);
            if (superTable.isPresent()) {
                return getMethodInHierarchy(superTable.get(), methodSignature);
            }
        }
        return null;
    }

    private Void visitExplicitMethodCall(JmmNode methodCall, SymbolTable table) {
        MethodSymbol symbol = types.getExplicitMethodCallSymbol(methodCall);

        if (symbol == null) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(methodCall),
                    NodeUtils.getColumn(methodCall),
                    "Method '" + methodCall.get("methodname") + "' does not exist locally and in imports and no valid superclass was found.",
                    null
            ));
        } else if (types.getExprType(methodCall.getChild(0)).asClass().staticRef() && !symbol.isStatic()) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(methodCall),
                    NodeUtils.getColumn(methodCall),
                    "Method '" + methodCall.get("methodname") + "' cannot be called in a static manner.",
                    null
            ));
        }
        return null;
    }

    private Void visitImplicitMethodCall(JmmNode methodCall, SymbolTable table) {

        String methodName = methodCall.get("methodname");
        var argTypes = methodCall.getChildren().stream()
                .map(super.types::getExprType)
                .toList();
        Signature methodsignature = new Signature(methodName, argTypes);

        var matchingMethods = isMethodInHierarchy(table, methodsignature);

        if (!matchingMethods) { // if method does not exist locally or in super class
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(methodCall),
                    NodeUtils.getColumn(methodCall),
                    "Method '" + methodName + "' does not exist locally and no valid superclass was found.",
                    null
            ));
        }


        MethodSymbol calledMethod = getMethodInHierarchy(table, methodsignature);

        // cant call non-static method from static method
        if (currentMethod.isStatic() && !calledMethod.isStatic()) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(methodCall),
                    NodeUtils.getColumn(methodCall),
                    "Can't call non-static method '" + methodName + "' from a static context.",
                    null
            ));
        }

        return null;
    }
}
