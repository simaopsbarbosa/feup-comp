package pt.up.fe.comp2026.optimization.ast;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2026.jmm.ast.JmmAttributes;
import pt.up.fe.comp2026.jmm.ast.JmmKind;

import java.util.List;

public class ForLoopVisitor extends AJmmVisitor<Void, Boolean> {

    @Override
    protected void buildVisitor() {
        addVisit(JmmKind.FOR_STMT, this::visitForStmt);
    }

    private boolean visitForStmt(JmmNode forStmt, Void unused) {
        JmmNode outerBlockNode = new JmmNodeImpl(JmmKind.BLOCK);
        boolean hasInit = !forStmt.getChildren(JmmKind.FOR_INIT).isEmpty();
        boolean hasCond = !forStmt.getChildren(JmmKind.FOR_COND).isEmpty();
        boolean hasUpdate = !forStmt.getChildren(JmmKind.FOR_UPDATE).isEmpty();

        //add initializations before while loop
        if (hasInit) {
            List<JmmNode> initNodes = forStmt.getChildren(JmmKind.FOR_INIT).getFirst().getChildren(JmmKind.ASSIGNMENT);

            initNodes.forEach(initNode -> {
                JmmNode assigntStmt = new JmmNodeImpl(JmmKind.ASSIGN_STMT);
                assigntStmt.add(initNode.getChild(0));
                assigntStmt.add(initNode.getChild(1));
                outerBlockNode.add(assigntStmt);
            });
        }

        JmmNode whileNode = new JmmNodeImpl(JmmKind.WHILE_STMT);

        //handle condition transform
        if (!hasCond) {
            JmmNode condNode = new JmmNodeImpl(JmmKind.BOOL_LITERAL);
            condNode.putObject(JmmAttributes.BOOL_LITERAL.BOOL, "true");
            whileNode.add(condNode);
        } else {
            JmmNode condNode = forStmt.getChildren(JmmKind.FOR_COND).getFirst().getChildren(JmmKind.EXPR).getFirst();
            whileNode.add(condNode);
        }

        JmmNode stmt = forStmt.getChildren(JmmKind.STMT).getFirst();

        if (hasUpdate) {
            JmmNode innerBlockNode = new JmmNodeImpl(JmmKind.BLOCK);

            // add stmt from for loop
            innerBlockNode.add(stmt);

            // add updates from for loop
            List<JmmNode> updateNodes = forStmt.getChildren(JmmKind.FOR_UPDATE).getFirst().getChildren(JmmKind.ASSIGNMENT);
            updateNodes.forEach(updateNode -> {
                JmmNode assignStmt = new JmmNodeImpl(JmmKind.ASSIGN_STMT);
                assignStmt.add(updateNode.getChild(0));
                assignStmt.add(updateNode.getChild(1));
                innerBlockNode.add(assignStmt);
            });

            whileNode.add(innerBlockNode);
        } else {
            whileNode.add(stmt);
        }

        //add while loop to block
        outerBlockNode.add(whileNode);
        forStmt.replace(outerBlockNode);

        return true;
    }


}
