package pt.up.fe.comp2026.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2026.analysis.AnalysisVisitorWithTable;
import pt.up.fe.comp2026.jmm.ast.JmmKind;

public class ArrayLogic extends AnalysisVisitorWithTable {
    public ArrayLogic(SymbolTable table) {
        super(table);
    }

    @Override
    public void buildVisitor() {
        addVisit(JmmKind.NEW_ARRAY, this::visitNewArray);
    }

    private Void visitNewArray(JmmNode jmmNode, SymbolTable table) {
        return null;
    }
}
