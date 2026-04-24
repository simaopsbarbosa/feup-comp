package pt.up.fe.comp2026.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.MethodSymbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.type.JmmType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmArrayType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmClassType;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.Kind;
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
        JmmType varType = types.getExprType(assignmentExpr.getChild(0));
        if (!canAssign(assignmentExpr.getChild(0))) return null;
        this.validateAssignment(assignmentExpr, varType, table);
        return null;
    }


    private Void vistVarDecl(JmmNode varDeclExpr, SymbolTable table) {

        //if has assignment validate it
        if (!varDeclExpr.getChildren(JmmKind.EXPR).isEmpty()) {
            JmmType varType = types.convertType(varDeclExpr.getChild(0));
            this.validateAssignment(varDeclExpr, varType, table);
        }

        return null;
    }


    private void validateAssignment(JmmNode assignmentNode, JmmType varType, SymbolTable table) {
        JmmType exprType = types.getExprType(assignmentNode.getChildren(JmmKind.EXPR).getLast());

        // verifies if both types are classes
        if(varType instanceof JmmClassType varTypeClass && exprType instanceof JmmClassType exprTypeClass) {
            // var type has to be parent of expr
            if (isParentClass(varTypeClass.fullyQualifiedName(), exprTypeClass.fullyQualifiedName()))
                return;
        }

        // handle array level logic
        boolean isVarArray  = (varType instanceof JmmArrayType);
        boolean isExprArray = (exprType instanceof JmmArrayType);

        // mismatched array type
        if (isVarArray != isExprArray) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, assignmentNode,
                    "Array assignment with incorrect types. Variable is type " + varType + " Expr is " + exprType));
            return;
        }

        if (isVarArray && isExprArray) {
            JmmArrayType varArrayType = (JmmArrayType) varType;
            JmmArrayType exprArrayType = (JmmArrayType) exprType;

            // mismatched primitive type
            if (!varArrayType.itemType().equals(exprArrayType.itemType())) {
                addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, assignmentNode,
                        "Array Primitive types do not match. Variable is type " + varType + " Expr is " + exprType));
            }

            // mismatched dimensions
            if (varArrayType.dimension() != exprArrayType.dimension()) {
                addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, assignmentNode,
                        "Array dimension assignment does not match. Variable has " + varType + " dims and Expr has " + exprType +" dims"));
            }
            return;
        }

        // else they are primitive and need to be equal
        if (!varType.equals(exprType)) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, assignmentNode, "Assigment operation must have same type. Variable is " + varType + " and Expression is " + exprType));
        }
    }


    private boolean isParentClass(String supposedParent, String supposedChild) {
        if (supposedParent.endsWith(".Object")) return true;
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


    private boolean canAssign(JmmNode leftExpr) {
        Kind leftKind = leftExpr.getKind();
        if (leftKind.equals(JmmKind.VAR_REF_EXPR) || leftKind.equals(JmmKind.ARRAY_ACCESS) || leftKind.equals(JmmKind.FIELD_ACESS))
            return true;

        addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, leftExpr,
                "Variable cannot be assigned to. Variable is Node type " + leftExpr));
        return false;
    }



}
