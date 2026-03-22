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
        addVisit(JmmKind.LOGICAL_NOT, this::visitNotExpr);
        addVisit(JmmKind.ELSEIF_BLOCK_STMT, this::visitIfStmt);
        addVisit(JmmKind.WHILE_STMT, this::visitWhileStmt);
        addVisit(JmmKind.RETURN_STMT, this::visitReturnStmt);
        addVisit(JmmKind.METHOD_DECL, this::visitMethodDecl);
    }


    private Void visitBinExpr(JmmNode binaryExpr, SymbolTable table) {
        TypeUtils type = new TypeUtils((JmmSymbolTable) table);

        var leftType = type.getExprType(binaryExpr.getChild(0));
        var rightType = type.getExprType(binaryExpr.getChild(1));
        String op = binaryExpr.get("op");
        var opType = type.getExprType(binaryExpr);

        switch (op) {
            case "+", "-", "*", "/":
                if (leftType != JmmPrimitiveType.INT || rightType != JmmPrimitiveType.INT) {
                    addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, binaryExpr, "Arithmetic operands must be Integers. Left is " + leftType + " and Right is " + rightType));
                }
                break;

            case "&&", "||":
                if (leftType != JmmPrimitiveType.BOOLEAN || rightType != JmmPrimitiveType.BOOLEAN) {
                    addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, binaryExpr, "Logical operands must be Booleans. Left is " + leftType + " and Right is " + rightType));
                }
                break;
            case "<", ">", "<=", ">=":
                if (leftType != JmmPrimitiveType.INT || rightType != JmmPrimitiveType.INT) {
                    addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, binaryExpr, "Comparison operands must be Integers. Left is" + leftType + " and Right is " + rightType));
                }
                break;

        }
        return null;
    }


    private Void visitNotExpr(JmmNode notExpr, SymbolTable table) {
        TypeUtils type = new TypeUtils((JmmSymbolTable) table);

        JmmType restType = type.getExprType(notExpr.getChild(0));

        if (restType != JmmPrimitiveType.BOOLEAN) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, notExpr, "Logical Not operand must be boolean. Rest of expression is " + restType));
        }

        return null;
    }

    private Void visitIfStmt(JmmNode ifStmt, SymbolTable table) {
        TypeUtils type = new TypeUtils((JmmSymbolTable) table);
        var condition = ifStmt.getChild(0);
        var conditionType = type.getExprType(condition);

        if (conditionType != JmmPrimitiveType.BOOLEAN) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, condition, "If condition must be boolean. Expression is " + conditionType));
        }
        return null;
    }

    private Void visitWhileStmt(JmmNode whileStmt, SymbolTable table) {
        TypeUtils type = new TypeUtils((JmmSymbolTable) table);
        var condition = whileStmt.getChild(0);
        var conditionType = type.getExprType(condition);

        if (conditionType != JmmPrimitiveType.BOOLEAN) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, condition, "While condition must be boolean. Expression is " + conditionType));
        }
        return null;
    }

    private Void visitReturnStmt(JmmNode returnStmt, SymbolTable table) {
        TypeUtils type = new TypeUtils((JmmSymbolTable) table);

        var methodNode = returnStmt.getAncestor(JmmKind.METHOD_DECL).get();
        JmmType expectedType = type.convertType(methodNode.getChild(0));

        if (returnStmt.getNumChildren() == 0) {
            if (!expectedType.toString().equalsIgnoreCase("void")) {
                addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, returnStmt, "Missing return value"));
            } else {
                 addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, returnStmt, "Return statement cannot exist in void method"));
            }
            return null;
        }

        var returnExpr = returnStmt.getChild(0);
        var actualType = type.getExprType(returnExpr);

        if (!actualType.equals(expectedType)) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, returnExpr, "Incompatible return type. Expected " + expectedType + " but got " + actualType));
        }
        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);
        JmmType returnType = typeUtils.convertType(method.getChild(0));

        if (!returnType.toString().equalsIgnoreCase("void")) {
            if (method.getDescendants(JmmKind.RETURN_STMT).isEmpty()) {
                addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, method, "Method " + method.get("name") + " must return a value of type " + returnType));
            }
        }
        return null;
    }
}
