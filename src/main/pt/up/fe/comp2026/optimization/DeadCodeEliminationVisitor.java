package pt.up.fe.comp2026.optimization;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2026.jmm.ast.JmmKind;

public class DeadCodeEliminationVisitor extends AJmmVisitor<Void, Boolean> {

    @Override
    protected void buildVisitor() {
        addVisit(JmmKind.WHILE_STMT, this::visitWhileStmt);
        setDefaultVisit(this::defaultVisit);
    }

    private Boolean visitWhileStmt(JmmNode jmmNode, Void unused) {
        JmmNode condition = jmmNode.getChild(0);
        if (condition.getKind() == JmmKind.BOOL_LITERAL) {
            if (condition.get("bool").equals("false")) {
                jmmNode.delete();
                return true;
            }
        }
        return false;
    }


    private Boolean defaultVisit(JmmNode node, Void unused) {
        boolean changed = false;

        for (var child : node.getChildren()) {
            changed |= visit(child);
        }

        return changed;
    }
}
