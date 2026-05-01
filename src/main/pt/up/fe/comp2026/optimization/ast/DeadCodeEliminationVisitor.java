package pt.up.fe.comp2026.optimization.ast;

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
        List<JmmNode> children = new ArrayList<>(node.getChildren());
        if(children.isEmpty()){
            node.delete();
            return true;
        }
        for (var child :children) {
            changed |= visit(child);
        }

        boolean foundReturn = false;
        for (JmmNode child : children) {
            if (foundReturn) {
                child.delete();
                changed = true;
                continue;
            }
            changed|=visit(child);
            if (alwaysReturns(child)) {
                foundReturn = true;
            }
        }


        return changed;
    }

    private boolean alwaysReturns(JmmNode node) {
        List<JmmNode> children=node.getChildren();
        if (JmmKind.RETURN_STMT.check(node)) {
            return true;
        }

        if (JmmKind.BLOCK.check(node)) {
            for (var child :children) {
                if (alwaysReturns(child)) {
                    return true;
                }
            }
        }
        if(JmmKind.ELSEIF_BLOCK_STMT.check(node)){
            if(children.size()<3){
                return false;
            }
            JmmNode thenBranch = node.getChild(1);
            JmmNode elseBranch = node.getChild(2);
            return alwaysReturns(thenBranch) && alwaysReturns(elseBranch);
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
