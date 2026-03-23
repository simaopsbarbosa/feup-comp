package pt.up.fe.comp2026.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2026.analysis.AnalysisVisitorWithTable;
import pt.up.fe.comp2026.jmm.ast.JmmKind;
import pt.up.fe.comp2026.utils.ReportUtils;



public class ImportLogic extends AnalysisVisitorWithTable {

    public ImportLogic(SymbolTable table) {
        super(table);
    }

    @Override
    protected void buildVisitor() {
        addVisit(JmmKind.CLASS_INITIALIZATION, this::visitClass);
    }
    private Void visitClass(JmmNode classInit,SymbolTable table){
        var name=classInit.get("name");
        boolean implicit=super.table.isImplicitImport(name);
        boolean isExplicit=super.table.getImports().contains(name);
        boolean currentClass=super.table.getClassName().equals(name);
        if(!implicit && !isExplicit && !currentClass){
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, classInit, "Class is not defined or imported" + name));
        }
        return  null;
    }
}
