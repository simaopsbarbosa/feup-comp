package pt.up.fe.comp2026.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.type.JmmType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmPrimitiveType;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2026.analysis.AnalysisVisitorWithTable;
import pt.up.fe.comp2026.jmm.ast.JmmKind;
import pt.up.fe.comp2026.utils.ReportUtils;

public class StatementLogic extends AnalysisVisitorWithTable {
    public StatementLogic(SymbolTable table) {
        super(table);
    }

    @Override
    public void buildVisitor() {
        addVisit(JmmKind.ELSEIF_BLOCK_STMT, this::visitIfStmt);
        addVisit(JmmKind.WHILE_STMT, this::visitWhileStmt);
        addVisit(JmmKind.DO_WHILE_STMT, this::visitDoWhileStmt);
        addVisit(JmmKind.FOR_STMT, this::visitForStmt);
        addVisit(JmmKind.RETURN_STMT, this::visitReturnStmt);
        addVisit(JmmKind.METHOD_DECL, this::visitMethodDecl);
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

    private Void visitDoWhileStmt(JmmNode jmmNode, SymbolTable table) {
        var condition = jmmNode.getChild(1);
        var conditionType = super.types.getExprType(condition);

        if (conditionType != JmmPrimitiveType.BOOLEAN) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, condition, "Do While condition must be boolean. Expression is " + conditionType));
        }

        return null;
    }

    private Void visitForStmt(JmmNode jmmNode, SymbolTable table) {
        if (jmmNode.getChildren(JmmKind.FOR_COND).isEmpty()) return null;

        //get conditional expressions
        var children = jmmNode.getChildren(JmmKind.FOR_COND).getFirst().getChildren(JmmKind.EXPR);

        children.stream()
                .forEach(child -> { // Use forEach instead of map
                    var conditionType = super.types.getExprType(child);
                    if (conditionType != JmmPrimitiveType.BOOLEAN) {
                        addReport(ReportUtils.buildErrorReport(
                                Stage.SEMANTIC, child,
                                "For loop condition must evaluate to a boolean. Found: " + conditionType
                        ));
                    }
                });
        return null;
    }

    private Void visitReturnStmt(JmmNode returnStmt, SymbolTable table) {

        var methodNode = returnStmt.getAncestor(JmmKind.METHOD_DECL).get();
        JmmType expectedType = super.types.convertType(methodNode.getChild(0));

        if (returnStmt.getNumChildren() == 0) {
            if (!expectedType.toString().equalsIgnoreCase("void")) {
                addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, returnStmt, "Missing return value"));
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
