package pt.up.fe.comp2026.optimization.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2026.jmm.ast.JmmKind;

import java.util.ArrayList;
import java.util.List;

public class DeadCodeEliminationVisitor extends AJmmVisitor<Void, Boolean> {

    private final SymbolTable symbolTable;
    protected String currentMethod;

    public DeadCodeEliminationVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    @Override
    protected void buildVisitor() {
        addVisit(JmmKind.METHOD_DECL, this::visitMethodDecl);
        addVisit(JmmKind.WHILE_STMT, this::visitWhileStmt);
        addVisit(JmmKind.BLOCK, this::visitBlock);
        setDefaultVisit(this::defaultVisit);
    }

    private Boolean visitMethodDecl(JmmNode node, Void unused) {
        this.currentMethod = node.get("name");
        return defaultVisit(node, unused);
    }

    private Boolean visitWhileStmt(JmmNode jmmNode, Void unused) {
        boolean changed = defaultVisit(jmmNode, unused);
        JmmNode condition = jmmNode.getChild(0);
        if (JmmKind.BOOL_LITERAL.check(condition) && condition.get("bool").equals("false")) {
            jmmNode.delete();
            return true;
        }
        return changed;
    }

    private Boolean visitBlock(JmmNode node, Void unused) {
        boolean changed = false;
        List<JmmNode> children = new ArrayList<>(node.getChildren());
        boolean foundReturn = false;
        for (JmmNode child : children) {
            if (foundReturn) {
                child.delete();
                changed = true;
                continue;
            }
            changed |= visit(child);
            if (alwaysReturns(child)) foundReturn = true;
        }
        return changed;
    }

    private boolean alwaysReturns(JmmNode node) {
        if (JmmKind.RETURN_STMT.check(node)) return true;
        if (JmmKind.BLOCK.check(node)) {
            for (var child : node.getChildren()) if (alwaysReturns(child)) return true;
        }
        if (JmmKind.ELSEIF_BLOCK_STMT.check(node)) {
            if (node.getChildren().size() < 3) return false;
            return alwaysReturns(node.getChild(1)) && alwaysReturns(node.getChild(2));
        }
        return false;
    }

    private Boolean defaultVisit(JmmNode node, Void unused) {
        boolean changed = false;
        for (var child : new ArrayList<>(node.getChildren())) {
            changed |= visit(child);
        }
        return changed;
    }
}
