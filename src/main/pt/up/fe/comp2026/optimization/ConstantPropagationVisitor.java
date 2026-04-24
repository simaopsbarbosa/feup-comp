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

    private Boolean visitAssignment(JmmNode jmmNode, Map<String, JmmNode> constants) {
        JmmNode leftSide = jmmNode.getChild(0);
        JmmNode rightSide = jmmNode.getChild(jmmNode.getChildren().size() - 1);

        return handleAssignment(leftSide, rightSide, constants);
    }

    private Boolean handleAssignment(JmmNode leftSide, JmmNode rightSide, Map<String, JmmNode> constants) {
        String variableName = leftSide.get("name");

        if (rightSide.getKind() == JmmKind.INTEGER_LITERAL || rightSide.getKind() == JmmKind.BOOL_LITERAL) {
            // the right side is constant (x = 10 or y = True)
            // add it to the map
            JmmNode previousValue = constants.put(variableName, rightSide);
            System.out.println("Constant variable " + variableName + " found with value " + rightSide);
            return previousValue == null || !previousValue.equals(rightSide);
        }

        return constants.remove(variableName) != null;
    }

    private Boolean defaultVisit(JmmNode node, Map<String, JmmNode> constants) {
        boolean changed = false;

        for (var child : node.getChildren()) {
            changed |= visit(child, constants);
        }

        return changed;
    }
}
