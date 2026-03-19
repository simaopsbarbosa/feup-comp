package pt.up.fe.comp2026.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.MethodSymbol;
import pt.up.fe.comp.jmm.analysis.table.Signature;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.type.JmmType;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.NodeAttribute;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2026.analysis.AnalysisVisitor;
import pt.up.fe.comp2026.ast.NodeUtils;
import pt.up.fe.comp2026.jmm.ast.JmmAttributes;
import pt.up.fe.comp2026.jmm.ast.JmmKind;
import pt.up.fe.comp2026.symboltable.JmmSymbolTableBuilder;

import java.util.List;
import java.util.Objects;

public class UndeclaredVariable extends AnalysisVisitor {

    private MethodSymbol currentMethod;


    @Override
    public void buildVisitor() {
        addVisit(JmmKind.METHOD_DECL, this::visitMethodDecl);
        addVisit(JmmKind.VAR_REF_EXPR, this::visitVarRefExpr);
        addVisit(JmmKind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(JmmKind.ARRAY_ASSIGNMENT, this::visitArrayAssignment);
    }


    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = table.getMethods(method.get("name")).getFirst();
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

}



