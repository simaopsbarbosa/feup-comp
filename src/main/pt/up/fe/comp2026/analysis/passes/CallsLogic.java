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
        addVisit(JmmKind.EXPLICIT_METHOD_CALL, this::visitExplicitMethodCall);
        // addVisit(JmmKind.IMPLICIT_METHOD_CALL,this::visitImplicitMethodCall);
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
        return null;
    }

}
