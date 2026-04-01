package pt.up.fe.comp2026.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.MethodSymbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.type.JmmType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmClassType;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2026.analysis.AnalysisVisitorWithTable;
import pt.up.fe.comp2026.jmm.ast.JmmKind;
import pt.up.fe.comp2026.utils.ReportUtils;

import java.util.Optional;

public class AssignmentLogic extends AnalysisVisitorWithTable {

    public AssignmentLogic(SymbolTable table) {
        super(table);
    }


    @Override
    public void buildVisitor() {
        addVisit(JmmKind.ASSIGN_STMT, this::visitAssignment);
        addVisit(JmmKind.VAR_DECL, this::vistVarDecl);
        addVisit(JmmKind.ASSIGNMENT, this::visitAssignment);
    }


    private Void visitAssignment(JmmNode assignmentExpr, SymbolTable table) {
        this.validateAssignment(assignmentExpr, assignmentExpr.get("var"), table);
        return null;
    }

    private Void vistVarDecl(JmmNode varDeclExpr, SymbolTable table) {
        //if has assignment validate it
        if (!varDeclExpr.getChildren(JmmKind.EXPR).isEmpty()) {
            this.validateAssignment(varDeclExpr, varDeclExpr.get("name"), table);
        }

        return null;
    }


    private void validateAssignment(JmmNode assignmentNode, String varName, SymbolTable table) {
        Optional<MethodSymbol> curentMethod = Optional.empty();
        System.out.println(assignmentNode);
        JmmType varType = null;

        //verify if assignment is done in a method
        if (assignmentNode.getAncestor(JmmKind.METHOD_DECL).isPresent())
            curentMethod = table.getMethod(super.types.getMethodDeclSignature(assignmentNode.getAncestor(JmmKind.METHOD_DECL).get()));

        //if var in locals
        if (curentMethod.isPresent() && curentMethod.get().getLocalVariable(varName).isPresent())
            varType = curentMethod.get().getLocalVariable(varName).get().type();

        //if var in parameters
        if (curentMethod.isPresent() && curentMethod.get().getParameter(varName).isPresent())
            varType = curentMethod.get().getParameter(varName).get().type();

        //if var in class field
        if (table.getField(varName).isPresent())
            varType = table.getField(varName).get().type();

        //get type of assigned expression
        JmmType exprType = super.types.getExprType(assignmentNode.getChildren(JmmKind.EXPR).getFirst());
        System.out.println("this for sure works");
        // verifies if both types are classes
        if(varType instanceof JmmClassType varTypeClass && exprType instanceof JmmClassType exprTypeClass) {
            // var type has to be parent of expr
            if (isParentClass(varTypeClass.fullyQualifiedName(), exprTypeClass.fullyQualifiedName()))
                return;
        }

        // else they are primitive and need to be equal
        if (!varType.equals(exprType)) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, assignmentNode, "Assigment operation must have same type. Variable is " + varType + " and Expression is " + exprType));
        }
    }


    private boolean isParentClass(String supposedParent, String supposedChild) {
        if (supposedChild.equals("")) return false;
        if (supposedParent.equals(supposedChild)) return true;
        if (table.getFullyQualifiedName().equals(supposedChild)
                && table.getSuperFullyQualifiedName() != null
                && table.getSuperFullyQualifiedName().equals(supposedParent))
            return true;

        String newChild = "";

        if (super.table.getImportedSymbolTable(supposedChild).isPresent()) {
            SymbolTable childTable = super.table.getImportedSymbolTable(supposedChild).get();

            //verify if has class extension
            if (childTable.getSuperFullyQualifiedName() == null) return false;
            else newChild = childTable.getSuperFullyQualifiedName();
        }

        //recursive call on extended class
        return isParentClass(supposedParent, newChild);
    }



}
