package pt.up.fe.comp2026.optimization;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2026.jmm.ast.JmmKind;

public class BranchEliminationVisitor extends AJmmVisitor<Void, Boolean> {

    @Override
    protected void buildVisitor() {
        addVisit(JmmKind.ELSEIF_BLOCK_STMT, this::elseIfBlockStmtVisitor);
        setDefaultVisit(this::defaultVisit);
    }

    private Boolean elseIfBlockStmtVisitor(JmmNode jmmNode, Void unused) {
        JmmNode condition = jmmNode.getChild(0);
        JmmNode thenStmt = jmmNode.getChild(1);
        boolean hasElseStmt = jmmNode.getChildren().size() > 2;

        if (condition.getKind() == JmmKind.BOOL_LITERAL) {
            if (condition.get("bool").equals("false")) {
                if (hasElseStmt) {
                    JmmNode elseStmt = jmmNode.getChild(2);
                    jmmNode.replace(elseStmt);
                } else {
                    jmmNode.delete();
                }
                return true;
            } else if (condition.get("bool").equals("true")) {
                jmmNode.replace(thenStmt);
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
