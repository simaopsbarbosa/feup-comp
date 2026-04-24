package pt.up.fe.comp2026.optimization;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.Map;

import pt.up.fe.comp2026.jmm.ast.JmmKind;

public class ConstantPropagationVisitor extends AJmmVisitor<Map<String, JmmNode>, Boolean> {

    @Override
    protected void buildVisitor() {
        addVisit(JmmKind.ASSIGN_STMT, this::visitAssignment);
        setDefaultVisit(this::defaultVisit);
    }

    private Boolean visitAssignment(JmmNode jmmNode, Map<String, JmmNode> stringJmmNodeMap) {
        JmmNode leftSide = jmmNode.getChild(0);
        String variableName = leftSide.get("name");
        JmmNode rightSide = jmmNode.getChildren().getLast();

        System.out.println("Variable Name: " + variableName);
        System.out.println("Value Node: " + rightSide);

        if (rightSide.getKind() == JmmKind.INTEGER_LITERAL || rightSide.getKind() == JmmKind.BOOL_LITERAL) {
            // the right side is constant (x = 10 or y = True)
            // add it to the map
            System.out.println("Right side is constant!");
            stringJmmNodeMap.put(leftSide.toString(), rightSide);
            return false; // temporary
        }

        return false;
    }

    private Boolean defaultVisit(JmmNode node, Map<String, JmmNode> stringJmmNodeMap) {
        boolean changed = false;

        for (var child : node.getChildren()) {
            changed |= visit(child, stringJmmNodeMap);
        }

        return changed;
    }
}
