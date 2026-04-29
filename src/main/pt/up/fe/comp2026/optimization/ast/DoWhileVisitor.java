package pt.up.fe.comp2026.optimization.ast;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2026.jmm.ast.JmmKind;

public class DoWhileVisitor extends AJmmVisitor<Void, Boolean> {

    @Override
    protected void buildVisitor() {
        addVisit(JmmKind.DO_WHILE_STMT, this::visitForStmt);
    }

    private boolean visitForStmt(JmmNode doWhile, Void unused) {

        JmmNode outerBlockNode = new JmmNodeImpl(JmmKind.BLOCK);
        JmmNode condition = doWhile.getChild(1);
        JmmNode stmt = doWhile.getChild(0);
        outerBlockNode.add(stmt);
        //create while stmt to replace the do while
        JmmNode whileNode = new JmmNodeImpl(JmmKind.WHILE_STMT);
        whileNode.add(condition);
        whileNode.add(stmt);
        outerBlockNode.add(whileNode);
        doWhile.replace(outerBlockNode);

        return true;
    }


}
