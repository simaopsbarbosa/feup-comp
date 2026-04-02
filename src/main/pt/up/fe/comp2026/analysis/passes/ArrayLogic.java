package pt.up.fe.comp2026.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.type.JmmType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmArrayType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmPrimitiveType;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2026.analysis.AnalysisVisitorWithTable;
import pt.up.fe.comp2026.jmm.ast.JmmKind;
import pt.up.fe.comp2026.utils.ReportUtils;

import java.util.List;

import static pt.up.fe.comp2026.jmm.ast.JmmKind.EXPR;

public class ArrayLogic extends AnalysisVisitorWithTable {

    public ArrayLogic(SymbolTable table) {
        super(table);
    }

    @Override
    public void buildVisitor() {
        addVisit(JmmKind.NEW_ARRAY, this::visitNewArray);
        addVisit(JmmKind.ARRAY_ACCESS, this::visitArrayAccess);
        addVisit(JmmKind.LENGTH, this::visitArrayLength);
        addVisit(JmmKind.ARRAY_INIT, this::visitArrayInit);
        addVisit(JmmKind.TYPE, this::visitType);
    }


    private void checkAccessInt(JmmNode parentNode, List<JmmNode> nodeList, int skip) {
        nodeList.stream()
                .skip(skip)
                .forEach(exprNode -> {
                    JmmType exprType = types.getExprType(exprNode);
                    if (!exprType.equals(JmmPrimitiveType.INT))
                        addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, parentNode,
                                "Array access size is not Integer. Expression of dimension size is type " + exprType));
                });

    }

    private Void visitType(JmmNode typeNode, SymbolTable table) {
        String baseTypeName = typeNode.getChild(0).get("name");
        List<Object> dims = typeNode.getObjectAsList("dims");

        if ("void".equals(baseTypeName) && !dims.isEmpty()) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, typeNode,
                    "Variables, fields, or parameters cannot be declared as void arrays."));
        }

        return null;
    }

    private void checkVoidType(JmmNode arrayNode) {
        String baseTypeName = arrayNode.getChild(0).get("name");

        if ("void".equals(baseTypeName)) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, arrayNode,
                    "An array's base type cannot be void."));
        }
    }

    private Void visitNewArray(JmmNode newArray, SymbolTable table) {
        checkAccessInt(newArray, newArray.getChildren(JmmKind.EXPR), 0);
        checkVoidType(newArray);
        return null;
    }

    private Void visitArrayAccess(JmmNode arrayAccess, SymbolTable table) {
        checkAccessInt(arrayAccess, arrayAccess.getChildren(JmmKind.EXPR), 1);

        JmmNode node = arrayAccess.getDescendants(JmmKind.VAR_REF_EXPR).getFirst();
        String varName = node.get("name");

        JmmType type = types.getVarTypeFromName(node, varName).get();

        // check if variable is an array
        if (!(type instanceof JmmArrayType arrayType)) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, arrayAccess,
                    "Variable is not subscriptable. Variable is type " + type));
        }
        else {
            int totalDims = arrayType.dimension();
            int accessedDims = arrayAccess.getChildren(EXPR).size() - 1;

            //check correct amount of dims accessed
            if (accessedDims > totalDims) {
                addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, arrayAccess,
                        "Accessed non-existent array dimension. Array has " + totalDims + " dimensions and Access has " + accessedDims + " dimensions"));
            }
        }

        return null;
    }
    private Void visitArrayInit(JmmNode arrayInit,SymbolTable table){
        checkVoidType(arrayInit);
        var type=super.types.getExprType(arrayInit).asArray().itemType();
        var arrayElement=arrayInit.getChild(1).getChildren(JmmKind.ARRAY_ELEMENT);
        if(arrayElement.isEmpty()) {
           return  null;
        }
        var children= arrayElement.stream().allMatch(e -> super.types.getExprType(e).equals(type));

        if(!children){
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, arrayInit,
                    "Type mismatch array expects "+ type + " but at leats one child is of wrong type"   ));
        }

        return  null;
    }



    private Void visitArrayLength(JmmNode arrayLengthExpr, SymbolTable table) {
        JmmNode varNode = arrayLengthExpr.getDescendants(JmmKind.VAR_REF_EXPR).getFirst();
        String varName = varNode.get("name");

        JmmType varType = types.getVarTypeFromName(varNode, varName).get();

        if (!(varType instanceof JmmArrayType)) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, arrayLengthExpr,
                    "Cant get length of non array type. Variable is type " + varType));
        }
        return null;
    }


}
