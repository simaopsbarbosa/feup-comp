package pt.up.fe.comp2026.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.MethodSymbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.type.JmmType;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2026.analysis.AnalysisVisitorWithTable;
import pt.up.fe.comp2026.jmm.ast.JmmKind;
import pt.up.fe.comp2026.utils.ReportUtils;

public class Assigment extends AnalysisVisitorWithTable {

    public Assigment(SymbolTable table) {
        super(table);
    }


    @Override
    public void buildVisitor() {
        addVisit(JmmKind.ASSIGN_STMT, this::visitAssignment);
        addVisit(JmmKind.VAR_DECL, this::vistVarDecl);
    }


    private Void visitAssignment(JmmNode assignmentExpr, SymbolTable table) {
        MethodSymbol curentMethod = table.getMethod(super.types.getMethodDeclSignature(assignmentExpr.getAncestor(JmmKind.METHOD_DECL).get())).get();
        String varName = assignmentExpr.get("var");
        JmmNode varDeclNode = null;
        JmmType varType = null;

        if (curentMethod.getLocalVariable(varName).isPresent())
            varType = curentMethod.getLocalVariable(varName).get().type();

        if (curentMethod.getParameter(varName).isPresent())
            varType = curentMethod.getParameter(varName).get().type();

        if (table.getField(varName).isPresent())
            varType = table.getField(varName).get().type();


        JmmType exprType = super.types.getExprType(assignmentExpr.getChildren(JmmKind.EXPR).getFirst());

        if (!varType.equals(exprType)) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, assignmentExpr, "Assigment operation must have same type. Variable is " + varType + " and Expression is " + exprType));
        }

        // perguntar se é necessário se o field é de uma classe super  !!!!!!!!!!!!!!


        return null;
    }


    private Void vistVarDecl(JmmNode varDeclExpr, SymbolTable table) {
        JmmType varType = super.types.convertType(varDeclExpr.getChild(0));

        if (!varDeclExpr.getChildren(JmmKind.EXPR).isEmpty()) {
            JmmType exprType = super.types.getExprType(varDeclExpr.getChildren(JmmKind.EXPR).getFirst());

            if (!varType.equals(exprType)) {
                addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, varDeclExpr, "Assigment operation must have same type. Variable is " + varType + " and Expression is " + exprType));
            }
        }

        return null;
    }
}
