package pt.up.fe.comp2026.optimization;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2026.jmm.ast.JmmKind;

import java.util.ArrayList;
import java.util.List;

public class DeadCodeEliminationVisitor extends AJmmVisitor<Void, Boolean> {

    @Override
    protected void buildVisitor() {
        addVisit(JmmKind.WHILE_STMT, this::visitWhileStmt);
        addVisit(JmmKind.METHOD_DECL, this::visitBlock);
        addVisit(JmmKind.BLOCK, this::visitBlock);
        setDefaultVisit(this::defaultVisit);
    }

    private Boolean visitWhileStmt(JmmNode jmmNode, Void unused) {
        boolean changed = false;

        for (var child : new ArrayList<>(jmmNode.getChildren())) {
            changed |= visit(child);
        }

        JmmNode condition = jmmNode.getChild(0);
        if (condition.getKind() == JmmKind.BOOL_LITERAL) {
            if (condition.get("bool").equals("false")) {
                jmmNode.delete();
                return true;
            }
        }
        return changed;
    }

    private Boolean visitBlock(JmmNode node, Void unused) {
        boolean changed = false;

        for (var child : new ArrayList<>(node.getChildren())) {
            changed |= visit(child);
        }

        boolean foundReturn = false;

        List<JmmNode> children = new ArrayList<>(node.getChildren());

        for (JmmNode child : children) {
            if (!JmmKind.STMT.check(child)) {
                continue;
            }

            if (foundReturn) {
                child.delete();
                changed = true;
                continue;
            }

            if (alwaysReturns(child)) {
                foundReturn = true;
            }
        }


        return changed;
    }

    private boolean alwaysReturns(JmmNode node) {
        if (JmmKind.RETURN_STMT.check(node)) {
            return true;
        }

        if (!JmmKind.BLOCK.check(node)) {
            return false;
        }

        for (var child : node.getChildren()) {
            if (!JmmKind.STMT.check(child)) {
                continue;
            }

            if (alwaysReturns(child)) {
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
