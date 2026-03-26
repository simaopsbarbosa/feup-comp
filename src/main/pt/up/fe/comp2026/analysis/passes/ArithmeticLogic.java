package pt.up.fe.comp2026.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Signature;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.type.JmmType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmPrimitiveType;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2026.analysis.AnalysisVisitor;
import pt.up.fe.comp2026.analysis.AnalysisVisitorWithTable;
import pt.up.fe.comp2026.ast.TypeUtils;
import pt.up.fe.comp2026.jmm.ast.JmmKind;
import pt.up.fe.comp2026.symboltable.JmmSymbolTable;
import pt.up.fe.comp2026.utils.ReportUtils;

public class ArithmeticLogic extends AnalysisVisitorWithTable {

    public ArithmeticLogic(SymbolTable table) {
        super(table);
    }

    @Override
    public void buildVisitor() {
        addVisit(JmmKind.BINARY_EXPR, this::visitBinExpr);
        addVisit(JmmKind.LOGICAL_EXPR, this::visitBinExpr);
        addVisit(JmmKind.EQUALITY_EXPR, this::visitBinExpr);
        addVisit(JmmKind.LOGICAL_NOT, this::visitNotExpr);
        addVisit(JmmKind.EQUALITY_EXPR, this::visitBinExpr);
        addVisit(JmmKind.LEFT_UNARY_EXPR, this::visitUnaryExpr);
        addVisit(JmmKind.RIGHT_UNARY_EXPR, this::visitUnaryExpr);
    }


    private Void visitBinExpr(JmmNode binaryExpr,SymbolTable table) {
        var leftType = super.types.getExprType(binaryExpr.getChild(0));
        var rightType = super.types.getExprType(binaryExpr.getChild(1));
        String op = binaryExpr.get("op");
        switch (op) {
            case "+", "-", "*", "/","<", ">", "<=", ">=", "%":
                if (leftType != JmmPrimitiveType.INT || rightType != JmmPrimitiveType.INT) {
                    addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, binaryExpr, "("+op+") " + "Arithmetic operands must be Integers. Left is " + leftType + " and Right is " + rightType));
                }
                break;

            case "&&", "||":
                if (leftType != JmmPrimitiveType.BOOLEAN || rightType != JmmPrimitiveType.BOOLEAN) {
                    addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, binaryExpr, "("+op+") " +"Logical operands must be Booleans. Left is " + leftType + " and Right is " + rightType));
                }
                break;

            case "==", "!=":
                if (leftType != rightType) {
                    addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, binaryExpr, "("+op+") " + "Equality operands must be equal. Left is " + leftType + " and Right is " + rightType));
                }
        }
        return null;
    }


    private Void visitNotExpr(JmmNode notExpr,SymbolTable table) {

        JmmType restType = super.types.getExprType(notExpr.getChild(0));

        if (restType != JmmPrimitiveType.BOOLEAN) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, notExpr, "Logical Not operand must be boolean. Rest of expression is " + restType));
        }

        return null;
    }

    private Void visitUnaryExpr(JmmNode unaryExpr, SymbolTable table) {
        JmmType exprType = super.types.getExprType(unaryExpr.getChild(0));
        String op = unaryExpr.get("op");

        if (exprType != JmmPrimitiveType.INT) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, unaryExpr, "("+op+") " + "Unary operator must have Interger operands. Operand of expression is " + exprType));
        }

        return null;
    }
}
