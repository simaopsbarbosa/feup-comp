package pt.up.fe.comp2026.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Signature;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmClassType;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2026.analysis.AnalysisVisitorWithTable;
import pt.up.fe.comp2026.ast.NodeUtils;
import pt.up.fe.comp2026.jmm.ast.JmmKind;

import pt.up.fe.comp.jmm.analysis.table.MethodSymbol;
import pt.up.fe.comp2026.utils.ReportUtils;

import java.util.Collections;
import java.util.List;



public class CallsLogic extends AnalysisVisitorWithTable {

    private MethodSymbol currentMethod;

    public CallsLogic(SymbolTable table) {
        super(table);
    }

    @Override
    protected void buildVisitor() {
        addVisit(JmmKind.METHOD_DECL, this::visitMethodDecl);
        addVisit(JmmKind.EXPLICIT_METHOD_CALL, this::visitExplicitMethodCall);
        addVisit(JmmKind.IMPLICIT_METHOD_CALL,this::visitImplicitMethodCall);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = table.getMethods(method.get("name")).getFirst();
        return null;
    }

    private boolean getMethodsInHierarchy(SymbolTable currentTable, Signature methodSignature) {
        var localMethods = currentTable.getMethod(methodSignature).isPresent();
        if (localMethods) return true;

        String superName = currentTable.getSuperFullyQualifiedName();

        if (superName != null && !superName.isEmpty()) {
            var superTable = table.getImportedSymbolTable(superName);
            if (superTable.isPresent()) {
                return getMethodsInHierarchy(superTable.get(), methodSignature);
            }
        }
        return false;
    }

    private Void visitExplicitMethodCall(JmmNode methodCall, SymbolTable table) {
        var receiver = methodCall.getChild(0);

        String methodName=methodCall.get("methodname");
        var argTypes = methodCall.getChildren().stream().skip(1)
                .map(arg -> super.types.getExprType(arg))
                .toList();
        Signature methodsignature=new Signature(methodName,argTypes);

        if (receiver.getKind() == JmmKind.THIS) {
            var matchingMethods = getMethodsInHierarchy(table, methodsignature);

            if (!matchingMethods) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(methodCall),
                        NodeUtils.getColumn(methodCall),
                        "Method '" + methodName + "' does not exist locally and no valid superclass was found.",
                        null
                ));
            }
            return null;
        }

        // get the type of the receiver to check if its an object or a class name
        var receiverType = super.types.getExprType(receiver);
        if (receiverType instanceof JmmClassType classType) {
            boolean isImported = classType.isImported() || super.table.isImplicitImport(classType.name());

            if (isImported) {
                // check if the method belongs to an external class
                var importTable = super.table.getImportedSymbolTable(classType.fullyQualifiedName());
                if (importTable.isPresent()) {
                    var method = importTable.get().getMethod(methodsignature);
                    
                    // verify if the method actually exists in the imported class
                    if (method.isEmpty()) {
                        addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, methodCall, "Method '" + methodName + "' is not present in import " + classType.name()));
                    } 
                    // block instance methods called on a class name (static reference)
                    else if (classType.staticRef() && !method.get().isStatic()) {
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(methodCall),
                                NodeUtils.getColumn(methodCall),
                                "Non-static method '" + methodName + "' cannot be referenced from a static context.",
                                null));
                    }
                }
            } else {
                var matchingMethods = getMethodsInHierarchy(table, methodsignature);

                if (!matchingMethods) {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(methodCall),
                            NodeUtils.getColumn(methodCall),
                            "Method '" + methodName + "' does not exist locally and no valid superclass was found.",
                            null
                    ));
                }
                // check static calls for local class names used as targets
                else if (classType.staticRef()) {
                    var localMethod = table.getMethods(methodName).stream().findFirst();
                    if (localMethod.isPresent() && !localMethod.get().isStatic()) {
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(methodCall),
                                NodeUtils.getColumn(methodCall),
                                "Non-static method '" + methodName + "' cannot be referenced from a static context.",
                                null));
                    }
                }
            }
        }
        return null;
    }
    private Void visitImplicitMethodCall(JmmNode methodCall, SymbolTable table) {

        String methodName = methodCall.get("methodname");
        var argTypes = methodCall.getChildren().stream()
                .map(arg -> super.types.getExprType(arg))
                .toList();
        Signature methodsignature = new Signature(methodName, argTypes);

        var matchingMethods = getMethodsInHierarchy(table, methodsignature);

        if (!matchingMethods) { // if method does not exist locally or in super class
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(methodCall),
                    NodeUtils.getColumn(methodCall),
                    "Method '" + methodName + "' does not exist locally and no valid superclass was found.",
                    null
            ));
        }


        MethodSymbol calledMethod = table.getMethods(methodName).get(0);

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
