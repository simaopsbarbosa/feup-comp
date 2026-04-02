package pt.up.fe.comp2026.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.type.JmmType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmPrimitiveType;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2026.analysis.AnalysisVisitorWithTable;
import pt.up.fe.comp2026.jmm.ast.JmmKind;
import pt.up.fe.comp2026.utils.ReportUtils;

import javax.lang.model.type.PrimitiveType;

public class ArrayLogic extends AnalysisVisitorWithTable {
    public ArrayLogic(SymbolTable table) {
        super(table);
    }

    @Override
    public void buildVisitor() {
        addVisit(JmmKind.NEW_ARRAY, this::visitNewArray);
        addVisit(JmmKind.ARRAY_INIT, this::visitArrayInit);
//        addVisit(JmmKind.ARRAY_INIT, this::visitArrayInit);
//        addVisit(JmmKind.ARRAY_INIT, this::visitArrayInit);
    }

    private Void visitNewArray(JmmNode newArray, SymbolTable table) {
        // verifies if all experssions for dimensions are type INT
        newArray.getChildren(JmmKind.EXPR).stream().forEach(exprNode -> {
            JmmType exprType = types.getExprType(exprNode);
            if (!exprType.equals(JmmPrimitiveType.INT)) {
                addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, newArray,
                        "New Array dimension size is not Integer. Expression of dimension size is type " +  exprType));
            }
        });

        return null;
    }
    private Void visitArrayInit(JmmNode arrayInit,SymbolTable table){
        var typeNode= arrayInit.getChild(0);
        var type=super.types.getExprType(arrayInit).asArray().itemType();
        var arrayelment=arrayInit.getChild(1).getChildren(JmmKind.ARRAY_ELEMENT);
        if(arrayelment.isEmpty()) {
           return  null;
        }
        var children= arrayelment.stream().allMatch(e -> super.types.getExprType(e).equals(type));

        if(!children){
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, arrayInit,
                    "Type mismatch array expects "+ type + " but at leats one child is of wrong type"   ));
        }

        return  null;
    }

}
