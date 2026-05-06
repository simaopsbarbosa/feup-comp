package pt.up.fe.comp2026.optimization.ast;

import pt.up.fe.comp.jmm.analysis.table.MethodSymbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2026.jmm.ast.JmmKind;

import java.util.*;

public class DeadCodeEliminationVisitor extends AJmmVisitor<Void, Boolean> {

    private final SymbolTable symbolTable;
    private String currentMethod;

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
        boolean changed = false;

        for (var child : new ArrayList<>(node.getChildren())) {
            changed |= visit(child);
        }

        changed |= removeUnusedAssignments(node);

        return changed;
    }

    private Boolean visitWhileStmt(JmmNode jmmNode, Void unused) {
        boolean changed = false;

        for (var child : new ArrayList<>(jmmNode.getChildren())) {
            changed |= visit(child);
        }

        JmmNode condition = jmmNode.getChild(0);
        if (JmmKind.BOOL_LITERAL.check(condition)) {
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
        if (children.isEmpty()) {
            node.delete();
            return true;
        }

        boolean foundReturn = false;
        for (JmmNode child : children) {
            if (foundReturn) {
                child.delete();
                changed = true;
                continue;
            }
            changed |= visit(child);
            if (alwaysReturns(child)) {
                foundReturn = true;
            }
        }

        return changed;
    }

    private boolean alwaysReturns(JmmNode node) {
        if (JmmKind.RETURN_STMT.check(node)) return true;
        if (JmmKind.BLOCK.check(node)) {
            for (var child : node.getChildren()) {
                if (alwaysReturns(child)) return true;
            }
        }
        if (JmmKind.ELSEIF_BLOCK_STMT.check(node)) {
            if (node.getChildren().size() < 3) return false;
            return alwaysReturns(node.getChild(1)) && alwaysReturns(node.getChild(2));
        }
        return false;
    }

    private boolean removeUnusedAssignments(JmmNode methodNode) {
        boolean changed = false;
        Set<JmmNode> neededNodes = new HashSet<>();
        Set<String> usefulVars = new HashSet<>();

        var methods = symbolTable.getMethods(currentMethod);
        if (methods.isEmpty()) return false;
        MethodSymbol method = methods.get(0);
        method.parameters().forEach(p -> usefulVars.add(p.name()));

        for (JmmNode node : methodNode.getDescendants()) {
            if (isInherentlyNeeded(node)) {
                markNeeded(node, neededNodes, usefulVars);
            }
        }

        boolean added = true;
        while (added) {
            added = false;
            for (JmmNode node : methodNode.getDescendants(JmmKind.ASSIGN_STMT)) {
                if (neededNodes.contains(node)) continue;
                String name = getLhsVariableName(node.getChild(0));
                if (name != null && usefulVars.contains(name)) {
                    if (markNeeded(node, neededNodes, usefulVars)) added = true;
                }
            }
            for (JmmNode node : methodNode.getDescendants()) {
                if (neededNodes.contains(node)) continue;
                if (JmmKind.WHILE_STMT.check(node) || JmmKind.ELSEIF_BLOCK_STMT.check(node)) {
                    if (hasNeededDescendant(node, neededNodes)) {
                        if (markNeeded(node, neededNodes, usefulVars)) added = true;
                    }
                }
            }
        }

        for (JmmNode node : new ArrayList<>(methodNode.getDescendants())) {
            if (JmmKind.ASSIGN_STMT.check(node) && !neededNodes.contains(node)) {
                JmmNode rhs = node.getChild(1);
                if (hasSideEffects(rhs)) {
                    JmmNode exprStmt = new JmmNodeImpl(JmmKind.EXPRESSION);
                    node.replace(exprStmt);
                    exprStmt.add(rhs);
                } else {
                    node.delete();
                }
                changed = true;
            } else if ((JmmKind.WHILE_STMT.check(node) || JmmKind.ELSEIF_BLOCK_STMT.check(node)) && !neededNodes.contains(node)) {
                if (hasSideEffects(node.getChild(0))) {
                    JmmNode exprStmt = new JmmNodeImpl(JmmKind.EXPRESSION);
                    node.replace(exprStmt);
                    exprStmt.add(node.getChild(0));
                } else {
                    node.delete();
                }
                changed = true;
            }
        }
        return changed;
    }

    private boolean isInherentlyNeeded(JmmNode node) {
        if (JmmKind.RETURN_STMT.check(node)) return true;
        if (JmmKind.EXPRESSION.check(node) && hasSideEffects(node.getChild(0))) return true;
        if (JmmKind.ASSIGN_STMT.check(node) && isField(node.getChild(0))) return true;
        return false;
    }

    private String getLhsVariableName(JmmNode lhs) {
        if (JmmKind.VAR_REF_EXPR.check(lhs)) return lhs.get("name");
        if (JmmKind.ARRAY_ACCESS.check(lhs)) {
            JmmNode arrayRef = lhs.getChild(0);
            if (JmmKind.VAR_REF_EXPR.check(arrayRef)) return arrayRef.get("name");
        }
        return null;
    }

    private boolean isField(JmmNode node) {
        if (JmmKind.VAR_REF_EXPR.check(node)) {
            return symbolTable.getField(node.get("name")).isPresent();
        }
        return false;
    }

    private boolean hasSideEffects(JmmNode node) {
        if (JmmKind.EXPLICIT_METHOD_CALL.check(node) || JmmKind.IMPLICIT_METHOD_CALL.check(node) ||
            JmmKind.CLASS_INITIALIZATION.check(node) || JmmKind.NEW_ARRAY.check(node) || JmmKind.ARRAY_INIT.check(node)) {
            return true;
        }
        if (JmmKind.ASSIGN_STMT.check(node) && isField(node.getChild(0))) return true;
        for (JmmNode child : node.getChildren()) {
            if (hasSideEffects(child)) return true;
        }
        return false;
    }

    private boolean markNeeded(JmmNode node, Set<JmmNode> neededNodes, Set<String> usefulVars) {
        if (!neededNodes.add(node)) return false;
        if (JmmKind.ASSIGN_STMT.check(node)) {
            JmmNode lhs = node.getChild(0);
            if (JmmKind.ARRAY_ACCESS.check(lhs)) markVariablesAsUseful(lhs.getChild(1), usefulVars);
            markVariablesAsUseful(node.getChild(1), usefulVars);
        } else {
            markVariablesAsUseful(node, usefulVars);
        }
        return true;
    }

    private void markVariablesAsUseful(JmmNode node, Set<String> usefulVars) {
        if (JmmKind.VAR_REF_EXPR.check(node)) {
            usefulVars.add(node.get("name"));
        }
        for (JmmNode ref : node.getDescendants(JmmKind.VAR_REF_EXPR)) {
            usefulVars.add(ref.get("name"));
        }
    }

    private boolean hasNeededDescendant(JmmNode node, Set<JmmNode> neededNodes) {
        for (JmmNode child : node.getChildren()) {
            if (neededNodes.contains(child) || hasNeededDescendant(child, neededNodes)) return true;
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
