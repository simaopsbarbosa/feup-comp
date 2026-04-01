package pt.up.fe.comp2026.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.MethodSymbol;
import pt.up.fe.comp.jmm.analysis.table.Signature;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.type.JmmType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmArrayType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmClassType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmPrimitiveType;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2026.analysis.AnalysisVisitorWithTable;
import pt.up.fe.comp2026.jmm.ast.JmmKind;
import pt.up.fe.comp2026.utils.ReportUtils;

import java.util.List;

public class ArrayLogic extends AnalysisVisitorWithTable {

    public ArrayLogic(SymbolTable table) {
        super(table);
    }

    @Override
    public void buildVisitor() {
        addVisit(JmmKind.NEW_ARRAY, this::visitNewArray);
//        addVisit(JmmKind.ARRAY_INIT, this::visitArrayInit);
        addVisit(JmmKind.ARRAY_ACCESS, this::visitArrayAccess);
    }


    private void checkAccessInt(JmmNode parentNode, List<JmmNode> nodeList, int skip) {
        nodeList.stream()
                .skip(skip)
                .forEach(exprNode -> {
                    JmmType exprType = types.getExprType(exprNode);
                    if (!exprType.equals(JmmPrimitiveType.INT))
                        addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, parentNode,
                                "Array access size is not Integer. Expression of dimension size is type " + exprType));
                });
    }

    private Void visitNewArray(JmmNode newArray, SymbolTable table) {
        checkAccessInt(newArray, newArray.getChildren(JmmKind.EXPR), 0);
        return null;
    }

    private Void visitArrayAccess(JmmNode arrayAccess, SymbolTable table) {
        checkAccessInt(arrayAccess, arrayAccess.getChildren(JmmKind.EXPR), 1);

        JmmNode node = arrayAccess.getDescendants(JmmKind.VAR_REF_EXPR).getFirst();
        String varName = node.get("name");

        JmmType type = types.getVarTypeFromName(node, varName).get();

        // check if variable is an array
        if (!(type instanceof JmmArrayType)) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, arrayAccess,
                    "Variable is not subscriptable. Variable is type " + type));
        }

        return null;
    }


}
