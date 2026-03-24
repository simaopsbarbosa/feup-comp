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

        addVisit(JmmKind.ELSEIF_BLOCK_STMT, this::visitIfStmt);
        addVisit(JmmKind.WHILE_STMT, this::visitWhileStmt);
        addVisit(JmmKind.RETURN_STMT, this::visitReturnStmt);
        addVisit(JmmKind.METHOD_DECL, this::visitMethodDecl);
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


    private Void visitIfStmt(JmmNode ifStmt, SymbolTable table) {
        var condition = ifStmt.getChild(0);
        var conditionType = super.types.getExprType(condition);

        if (conditionType != JmmPrimitiveType.BOOLEAN) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, condition, "If condition must be boolean. Expression is " + conditionType));
        }
        return null;
    }

    private Void visitWhileStmt(JmmNode whileStmt, SymbolTable table) {

        var condition = whileStmt.getChild(0);
        var conditionType = super.types.getExprType(condition);

        if (conditionType != JmmPrimitiveType.BOOLEAN) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, condition, "While condition must be boolean. Expression is " + conditionType));
        }
        return null;
    }


    private Void visitReturnStmt(JmmNode returnStmt, SymbolTable table) {

        var methodNode = returnStmt.getAncestor(JmmKind.METHOD_DECL).get();
        JmmType expectedType = super.types.convertType(methodNode.getChild(0));

        if (returnStmt.getNumChildren() == 0) {
            if (!expectedType.toString().equalsIgnoreCase("void")) {
                addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, returnStmt, "Missing return value"));
            } else {
                 addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, returnStmt, "Return statement cannot exist in void method"));
            }
            return null;
        }

        var returnExpr = returnStmt.getChild(0);
        var actualType = super.types.getExprType(returnExpr);

        if (!actualType.equals(expectedType)) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, returnExpr, "Incompatible return type. Expected " + expectedType + " but got " + actualType));
        }
        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        JmmType returnType = super.types.convertType(method.getChild(0));

        if (!returnType.toString().equalsIgnoreCase("void")) {
            if (method.getDescendants(JmmKind.RETURN_STMT).isEmpty()) {
                addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, method, "Method " + method.get("name") + " must return a value of type " + returnType));
            }
        }
        return null;
    }
}
