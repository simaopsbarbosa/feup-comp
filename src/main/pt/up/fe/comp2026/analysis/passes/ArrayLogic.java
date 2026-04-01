package pt.up.fe.comp2026.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.type.JmmType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmPrimitiveType;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2026.analysis.AnalysisVisitorWithTable;
import pt.up.fe.comp2026.jmm.ast.JmmKind;
import pt.up.fe.comp2026.utils.ReportUtils;

public class ArrayLogic extends AnalysisVisitorWithTable {
    public ArrayLogic(SymbolTable table) {
        super(table);
    }

    @Override
    public void buildVisitor() {
        addVisit(JmmKind.NEW_ARRAY, this::visitNewArray);
        // addVisit(JmmKind.ARRAY_INIT, this::visitArrayInit);
//        addVisit(JmmKind.ARRAY_INIT, this::visitArrayInit);
//        addVisit(JmmKind.ARRAY_INIT, this::visitArrayInit);
    }

    private Void visitNewArray(JmmNode newArray, SymbolTable table) {
        // verifies if all experssions for dimensions are type INT
        newArray.getChildren(JmmKind.EXPR).stream().forEach(exprNode -> {
            JmmType exprType = types.getExprType(exprNode);
            if (!exprType.equals(JmmPrimitiveType.INT)) {
                addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, newArray,
                        "New Array dimension size is not Integer. Expression of dimension size is type " +  exprType));
            }
        });

        return null;
    }


}
