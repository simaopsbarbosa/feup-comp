package pt.up.fe.comp2026.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.MethodSymbol;
import pt.up.fe.comp.jmm.analysis.table.Signature;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2026.analysis.AnalysisVisitorWithTable;
import pt.up.fe.comp2026.ast.NodeUtils;
import pt.up.fe.comp2026.jmm.ast.JmmKind;
import pt.up.fe.comp2026.utils.ReportUtils;

public class UndeclaredVariable extends AnalysisVisitorWithTable {

    private MethodSymbol currentMethod;

    public UndeclaredVariable(SymbolTable table) {
        super(table);
    }


    @Override
    public void buildVisitor() {
        addVisit(JmmKind.METHOD_DECL, this::visitMethodDecl);
        addVisit(JmmKind.VAR_REF_EXPR, this::visitVarRefExpr);
        addVisit(JmmKind.FIELD_ACESS, this::visitFieldAccess);

    }


    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        var argTypes = method.getChildren(JmmKind.PARAM).stream()
                .map(super.types::getExprType)
                .toList();
        Signature methodsignature = new Signature(method.get("name"), argTypes);
        if (table.getMethod(methodsignature).isEmpty()) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, method, "This method signature is something"));
            return null;
        }
        currentMethod = table.getMethod(methodsignature).get();
        return null;
    }

    private Void verifyVarExists(String attrName, JmmNode varRefExpr, SymbolTable table) {
        var varRefName = varRefExpr.get(attrName);

        if (currentMethod.getParameter(varRefName).isPresent())
            return null;

        if (currentMethod.getLocalVariable(varRefName).isPresent())
            return null;

        if (table.getField(varRefName).isPresent())
            return null;

        if (table.getImports().stream().anyMatch(s -> s.endsWith(varRefName)))
            return null;

        if (table.getClassName().equals(varRefName))
            return null;

        var message = String.format("Variable '%s' does not exist.", varRefName);
        addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(varRefExpr),
                NodeUtils.getColumn(varRefExpr), message, null)
        );

        return null;
    }


    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        return this.verifyVarExists("name", varRefExpr, table);
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        return this.verifyVarExists("var", assignStmt, table);
    }

    private Void visitArrayAssignment(JmmNode assignStmt, SymbolTable table) {
        return this.verifyVarExists("var", assignStmt, table);
    }

    private Void visitFieldAccess(JmmNode fieldAccessStmt, SymbolTable table) {
        var varRefName = fieldAccessStmt.get("name");
        var fstChild = fieldAccessStmt.getChild(0);

        //check if this.var access is declared
        if (JmmKind.check(fstChild, JmmKind.THIS) && !table.getField(fieldAccessStmt.get("name")).isPresent()) {
            var message = String.format("Field variable '%s' does not exist in this class.", varRefName);
            addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(fieldAccessStmt),
                    NodeUtils.getColumn(fieldAccessStmt), message, null)
            );
            return null;
        }

        return null;
    }

}



