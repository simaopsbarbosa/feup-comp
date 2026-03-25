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

    public CallsLogic(SymbolTable table) {
        super(table);
    }

    @Override
    protected void buildVisitor() {
        addVisit(JmmKind.EXPLICIT_METHOD_CALL, this::visitMethodCall);
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

    private Void visitMethodCall(JmmNode methodCall, SymbolTable table) {
        var receiver = methodCall.getChild(0);

        String methodName=methodCall.get("methodname");
        var argTypes = methodCall.getChildren().stream().skip(1)
                .map(arg -> super.types.getExprType(arg))
                .toList();
        Signature methodsignature=new Signature(methodName,argTypes);

        var type= methodCall.getChild(0);
        if (receiver.getKind() != JmmKind.THIS) {
            var receiverType = super.types.getExprType(type);
            if (receiverType instanceof JmmClassType classType && (classType.isImported() || super.table.isImplicitImport(classType.name()))) {
                var importTable = super.table.getImportedSymbolTable(classType.fullyQualifiedName());
                if(importTable.isPresent()){
                    if(!importTable.get().getMethod(methodsignature).isPresent()){
                        addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, methodCall, "Method is not present in import "));
                    }
                }

            }
        }
        else {
            var matchingMethods = getMethodsInHierarchy(table, methodsignature);

            if (!matchingMethods) { // if method does not exist locally or in super class
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(methodCall),
                        NodeUtils.getColumn(methodCall),
                        "Method '" + methodName + "' does not exist locally and no valid superclass was found.",
                        null
                ));
                return null;
            }
        }



        /*// assuming no method overloading for now
        var methodData = matchingMethods.get(0);

        var children = methodCall.getChildren();

        var providedArguments = children.subList(1, children.size());
        var expectedParameters = methodData.parameters();

        if (providedArguments.size() != expectedParameters.size()) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(methodCall),
                    NodeUtils.getColumn(methodCall),
                    "Invalid number of arguments for method " + methodName +
                            ". Expected " + expectedParameters.size() + ", got " + providedArguments.size(),
                    null
            ));
            return null;
        }*/
/*
        // TODO: compare the data types of providedArguments against expectedParameters
        for (int i = 0; i < providedArguments.size(); i++) {
            JmmNode argNode = providedArguments.get(i);
            pt.up.fe.comp.jmm.analysis.table.Symbol expectedParam = expectedParameters.get(i);

            var expectedType = expectedParam.type();
            var providedType = getTypes().getExprType(argNode);

            if (expectedType.getClass().getName() != providedType.getClass().getName() ||
                    expectedType.isArray() != providedType.isArray()) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(argNode),
                        NodeUtils.getColumn(argNode),
                        "Type mismatch for argument " + (i + 1) + " of method '" + methodName + "'. " +
                                "Expected " + expectedType + " but got " + providedType,
                        null
                ));
            }*/
        // }

        return null;
    }

}
