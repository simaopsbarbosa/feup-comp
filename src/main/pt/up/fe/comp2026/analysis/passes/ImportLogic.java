package pt.up.fe.comp2026.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Signature;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmClassType;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2026.analysis.AnalysisVisitorWithTable;
import pt.up.fe.comp2026.jmm.ast.JmmAttributes;
import pt.up.fe.comp2026.jmm.ast.JmmKind;
import pt.up.fe.comp2026.utils.ReportUtils;



public class ImportLogic extends AnalysisVisitorWithTable {

    public ImportLogic(SymbolTable table) {
        super(table);
    }

    @Override
    protected void buildVisitor() {
        addVisit(JmmKind.CLASS_INITIALIZATION, this::visitClass);
        addVisit(JmmKind.EXPLICIT_METHODCALL,this::visitMethod);
    }
    private Void visitClass(JmmNode classInit,SymbolTable table){
        var name=classInit.get("name");
        boolean implicit=super.table.isImplicitImport(name);
        boolean isExplicit=super.table.getImportNames().contains(name);
        boolean currentClass=super.table.getClassName().equals(name);
        if(!implicit && !isExplicit && !currentClass){
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, classInit, "Class is not defined or imported " + name));
        }
        return  null;
    }
    private Void visitMethod(JmmNode methodCall ,SymbolTable table){
        String methodName=methodCall.get("methodname");
        var argTypes = methodCall.getChildren().stream().skip(1)
                .map(arg -> super.types.getExprType(arg))
                .toList();
        Signature methodsignature=new Signature(methodName,argTypes);
        boolean exists=false;
        var type= methodCall.getChild(0);
        if(type.getKind()!= JmmKind.THIS) {
            var receiverType = super.types.getExprType(type);
            if (receiverType instanceof JmmClassType classType && (classType.isImported() || super.table.isImplicitImport(classType.name()))) {
                var importTable = super.table.getImportedSymbolTable(classType.fullyQualifiedName());
                if(importTable.isPresent()){
                    var all_methods=importTable.get().getMethods();
                    var methods=importTable.get().getMethods(methodName);
                    if(methods.isEmpty()){
                        addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, methodCall, "Method is not present in import "));
                    }
                    else exists=methods.stream().anyMatch(m->m.signature().equals(methodsignature));
                    if (!exists) {
                        addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, methodCall, "Method does not have the correct signature"));
                    }

                }


            }
        }
        else{
            if(super.table.getMethod(methodsignature).isEmpty()){
                addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, methodCall, "Method is not present in the class "));
            }
        }
        return null;
    }
}
