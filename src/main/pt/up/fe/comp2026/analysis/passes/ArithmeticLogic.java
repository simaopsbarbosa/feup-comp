package pt.up.fe.comp2026.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.type.JmmType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmPrimitiveType;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2026.analysis.AnalysisVisitor;
import pt.up.fe.comp2026.ast.TypeUtils;
import pt.up.fe.comp2026.jmm.ast.JmmKind;
import pt.up.fe.comp2026.symboltable.JmmSymbolTable;
import pt.up.fe.comp2026.utils.ReportUtils;

public class ArithmeticLogic extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(JmmKind.BINARY_EXPR, this::visitBinExpr);
        addVisit(JmmKind.LOGICAL_EXPR, this::visitBinExpr);
        addVisit(JmmKind.EQUALITY_EXPR, this::visitBinExpr);
        addVisit(JmmKind.LOGICAL_EXPR, this::visitBinExpr);

        addVisit(JmmKind.LOGICAL_NOT, this::visitNotExpr);
    }


    private Void visitBinExpr(JmmNode binaryExpr, SymbolTable table) {
        TypeUtils type = new TypeUtils((JmmSymbolTable) table);

        var leftType = type.getExprType(binaryExpr.getChild(0));
        var rightType = type.getExprType(binaryExpr.getChild(1));
        String op = binaryExpr.get("op");
        var opType = type.getExprType(binaryExpr);

        switch(op) {
            case "+", "-", "*", "/":
                if (leftType != JmmPrimitiveType.INT || rightType != JmmPrimitiveType.INT) {
                    addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, binaryExpr,
                            "Arithmetic operands must be Integers. Left is " + leftType + " and Right is " + rightType));
                }
                break;

            case "&&", "||":
                if (leftType != JmmPrimitiveType.BOOLEAN || rightType != JmmPrimitiveType.BOOLEAN) {
                    addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, binaryExpr,
                           "Logical operands must be Booleans. Left is " + leftType + " and Right is " + rightType));
                }
                break;
            case "<", ">", "<=", ">=":
                if (leftType != JmmPrimitiveType.INT || rightType != JmmPrimitiveType.INT) {
                    addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, binaryExpr,
                            "Comparison operands must be Integers. Left is"  + leftType + " and Right is " + rightType));
                }
                break;

        }
        return null;
    }


    private Void visitNotExpr(JmmNode notExpr, SymbolTable table) {
        TypeUtils type = new TypeUtils((JmmSymbolTable) table);

        JmmType restType = type.getExprType(notExpr.getChild(0));

        if (restType != JmmPrimitiveType.BOOLEAN) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, notExpr,
                    "Logical Not operand must be boolean. Rest of expression is " + restType));
        }

        return null;
    }

}