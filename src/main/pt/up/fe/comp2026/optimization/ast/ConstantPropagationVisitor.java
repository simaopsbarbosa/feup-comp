package pt.up.fe.comp2026.optimization.ast;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
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
        addVisit(JmmKind.WHILE_STMT, this::visitWhileStmt);
        addVisit(JmmKind.LEFT_UNARY_EXPR, this::visitUnaryExpr);
        addVisit(JmmKind.RIGHT_UNARY_EXPR, this::visitUnaryExpr);
        setDefaultVisit(this::defaultVisit);
    }

    private Boolean visitMethodDecl(JmmNode methodDecl, Map<String, JmmNode> constants) {
        boolean changed = false;
        Map<String, JmmNode> methodConstants = new HashMap<>(constants);
        for (var child : methodDecl.getChildren()) {
            changed |= visit(child, methodConstants);
        }
        return changed;
    }

    private Boolean visitWhileStmt(JmmNode whileStmt, Map<String, JmmNode> constants) {
        boolean changed = visit(whileStmt.getChild(0), constants);
        JmmNode condition = whileStmt.getChild(0);
        if (condition.getKind().equals(JmmKind.BOOL_LITERAL.toString()) && condition.get("bool").equals("false")) {
            return changed;
        }
        Map<String, JmmNode> bodyConstants = new HashMap<>(constants);
        changed |= visit(whileStmt.getChild(1), bodyConstants);
        removeAssignedVariables(whileStmt.getChild(1), constants);
        return changed;
    }

    private Boolean visitIfStmt(JmmNode jmmNode, Map<String, JmmNode> constants) {
        boolean changed = visit(jmmNode.getChild(0), constants);
        JmmNode condition = jmmNode.getChild(0);
        if (JmmKind.BOOL_LITERAL.check(condition)) {
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
        }
        removeAssignedVariables(jmmNode.getChild(1), constants);
        if (jmmNode.getChildren().size() > 2) {
            removeAssignedVariables(jmmNode.getChild(2), constants);
        }
        return changed;
    }

    private void removeAssignedVariables(JmmNode jmmNode, Map<String, JmmNode> constants) {
        if ((JmmKind.ASSIGN_STMT.check(jmmNode) || JmmKind.ASSIGNMENT.check(jmmNode)) &&
                JmmKind.VAR_REF_EXPR.check(jmmNode.getChild(0))) {
            constants.remove(jmmNode.getChild(0).get("name"));
        }
        for (var child : jmmNode.getChildren()) {
            removeAssignedVariables(child, constants);
        }
    }

    private Boolean visitAssignment(JmmNode jmmNode, Map<String, JmmNode> constants) {
        JmmNode leftSide = jmmNode.getChild(0);
        if (!JmmKind.VAR_REF_EXPR.check(leftSide)) return false;
        String variableName = leftSide.get("name");
        JmmNode rightSide = jmmNode.getChild(jmmNode.getChildren().size() - 1);
        boolean changed = visit(rightSide, constants);
        JmmNode newRightSide = jmmNode.getChild(jmmNode.getChildren().size() - 1);
        changed |= handleAssignment(variableName, newRightSide, constants);
        return changed;
    }

    private Boolean visitVarDecl(JmmNode jmmNode, Map<String, JmmNode> constants) {
        if (jmmNode.getChildren().size() == 2) {
            String variableName = jmmNode.get("name");
            JmmNode rightSide = jmmNode.getChild(1);
            boolean changed = visit(rightSide, constants);
            JmmNode newRightSide = jmmNode.getChild(1);
            changed |= handleAssignment(variableName, newRightSide, constants);
            return changed;
        }
        return false;
    }

    private Boolean visitUnaryExpr(JmmNode jmmNode, Map<String, JmmNode> constants) {
        JmmNode child = jmmNode.getChild(0);
        if (!JmmKind.VAR_REF_EXPR.check(child)) return visit(child, constants);
        String variableName = child.get("name");
        if (!constants.containsKey(variableName)) return false;
        JmmNode constantNode = constants.get(variableName);
        if (!JmmKind.INTEGER_LITERAL.check(constantNode)) {
            constants.remove(variableName);
            return false;
        }
        int value = Integer.parseInt(constantNode.get("value"));
        String op = jmmNode.get("op");
        int newValue = switch (op) {
            case "++" -> value + 1;
            case "--" -> value - 1;
            default -> {
                constants.remove(variableName);
                yield value;
            }
        };
        JmmNode newConstantNode = new JmmNodeImpl(JmmKind.INTEGER_LITERAL);
        newConstantNode.put("value", String.valueOf(newValue));
        constants.put(variableName, newConstantNode);
        return false;
    }

    private Boolean handleAssignment(String variableName, JmmNode rightSide, Map<String, JmmNode> constants) {
        if (JmmKind.INTEGER_LITERAL.check(rightSide) || JmmKind.BOOL_LITERAL.check(rightSide)) {
            constants.put(variableName, rightSide);
            return false;
        }
        constants.remove(variableName);
        return false;
    }

    private Boolean visitVarRefExpr(JmmNode jmmNode, Map<String, JmmNode> constants) {
        String variableName = jmmNode.get("name");
        if (constants.containsKey(variableName)) {
            JmmNode parent = jmmNode.getParent();
            boolean isLeftSide = (JmmKind.ASSIGN_STMT.check(parent) || JmmKind.ASSIGNMENT.check(parent) || JmmKind.VAR_DECL.check(parent)) &&
                    parent.getChild(0) == jmmNode;
            if (!isLeftSide) {
                jmmNode.replace(constants.get(variableName).copy());
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
