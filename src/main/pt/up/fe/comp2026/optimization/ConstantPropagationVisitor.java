package pt.up.fe.comp2026.optimization;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2026.jmm.ast.JmmKind;

import java.util.HashMap;
import java.util.Map;

public class ConstantPropagationVisitor extends AJmmVisitor<Map<String, JmmNode>, Boolean> {

    @Override
    protected void buildVisitor() {
        addVisit(JmmKind.METHOD_DECL, this::visitMethodDecl);
        addVisit(JmmKind.ASSIGN_STMT, this::visitAssignment);
        addVisit(JmmKind.ASSIGNMENT, this::visitAssignment);
        addVisit(JmmKind.VAR_DECL, this::visitVarDecl);
        addVisit(JmmKind.VAR_REF_EXPR, this::visitVarRefExpr);
        addVisit(JmmKind.ELSEIF_BLOCK_STMT, this::visitIfStmt);
        setDefaultVisit(this::defaultVisit);
    }

    // reset the map of constants at the start of every method
    private Boolean visitMethodDecl(JmmNode methodDecl, Map<String, JmmNode> constants) {
        boolean changed = false;

        Map<String, JmmNode> methodConstants = new HashMap<>(constants);

        for (var child : methodDecl.getChildren()) {
            changed |= visit(child, methodConstants);
        }

        return changed;
    }

    private Boolean visitIfStmt(JmmNode jmmNode, Map<String, JmmNode> constants) {
        boolean changed = visit(jmmNode.getChild(0), constants);

        JmmNode condition = jmmNode.getChild(0);
        if (condition.getKind() == JmmKind.BOOL_LITERAL) {
            if (condition.get("bool").equals("true")) {
                changed |= visit(jmmNode.getChild(1), constants);
            } else if (jmmNode.getChildren().size() > 2) {
                changed |= visit(jmmNode.getChild(2), constants);
            }
            return changed;
        }

        Map<String, JmmNode> thenConstants = new HashMap<>(constants);
        Map<String, JmmNode> elseConstants = new HashMap<>(constants);

        changed |= visit(jmmNode.getChild(1), thenConstants);

        if (jmmNode.getChildren().size() > 2) {
            changed |= visit(jmmNode.getChild(2), elseConstants);
        } else {
            elseConstants = new HashMap<>(constants);
        }

        removeAssignedVariables(jmmNode.getChild(1), constants);
        if (jmmNode.getChildren().size() > 2) {
            removeAssignedVariables(jmmNode.getChild(2), constants);
        }

        return changed;
    }

    private void removeAssignedVariables(JmmNode jmmNode, Map<String, JmmNode> constants) {
        if ((jmmNode.getKind() == JmmKind.ASSIGN_STMT ||
                jmmNode.getKind() == JmmKind.ASSIGNMENT) &&
                jmmNode.getChild(0).getKind() == JmmKind.VAR_REF_EXPR) {
            constants.remove(jmmNode.getChild(0).get("name"));
        }

        for (var child : jmmNode.getChildren()) {
            removeAssignedVariables(child, constants);
        }
    }

    private Boolean visitAssignment(JmmNode jmmNode, Map<String, JmmNode> constants) {
        JmmNode leftSide = jmmNode.getChild(0);

        if (leftSide.getKind() != JmmKind.VAR_REF_EXPR) {
            return false;
        }

        String variableName = leftSide.get("name");
        JmmNode rightSide = jmmNode.getChild(jmmNode.getChildren().size() - 1);

        boolean changed = handleAssignment(variableName, rightSide, constants);

        changed |= visit(rightSide, constants);

        return changed;
    }

    private Boolean visitVarDecl(JmmNode jmmNode, Map<String, JmmNode> constants) {
        if (jmmNode.getChildren().size() == 2) {
            String variableName = jmmNode.get("name");
            JmmNode rightSide = jmmNode.getChild(1);
            return handleAssignment(variableName, rightSide, constants);
        }
        return false;
    }

    private Boolean handleAssignment(String variableName, JmmNode rightSide, Map<String, JmmNode> constants) {
        if (rightSide.getKind() == JmmKind.INTEGER_LITERAL || rightSide.getKind() == JmmKind.BOOL_LITERAL) {
            // the right side is constant (x = 10 or y = True)
            // add it to the map
            JmmNode previousValue = constants.put(variableName, rightSide);
            return false;
        }
        constants.remove(variableName);
        return false;
    }

    private Boolean visitVarRefExpr(JmmNode jmmNode, Map<String, JmmNode> constants) {
        String variableName = jmmNode.get("name");

        if (constants.containsKey(variableName)) {
            JmmNode parent = jmmNode.getParent();

            boolean isLeftSide = (parent.getKind().equals(JmmKind.ASSIGN_STMT) ||
                    parent.getKind().equals(JmmKind.ASSIGNMENT)) ||
                    parent.getKind().equals(JmmKind.VAR_DECL) &&
                    parent.getChild(0) == jmmNode;

            if (!isLeftSide) {
                JmmNode constantNode = constants.get(variableName);
                JmmNode newNode = constantNode.copy();

                jmmNode.replace(newNode);
                return true;
            }
        }
        return false;
    }

    private Boolean defaultVisit(JmmNode node, Map<String, JmmNode> constants) {
        boolean changed = false;

        for (var child : node.getChildren()) {
            changed |= visit(child, constants);
        }

        return changed;
    }
}
