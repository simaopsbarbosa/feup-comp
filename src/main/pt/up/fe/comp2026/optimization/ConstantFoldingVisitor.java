package pt.up.fe.comp2026.optimization;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2026.jmm.ast.JmmKind;

public class ConstantFoldingVisitor extends AJmmVisitor<Void, Boolean> {

    @Override
    protected void buildVisitor() {
        addVisit(JmmKind.BINARY_EXPR, this::visitBinaryExpr);
        setDefaultVisit(this::defaultVisit);
    }

    private Boolean visitBinaryExpr(JmmNode jmmNode, Void unused) {
        boolean changed = false;

        for (var child : jmmNode.getChildren()) {
            changed |= visit(child);
        }

        JmmNode leftNode = jmmNode.getChild(0);
        JmmNode rightNode = jmmNode.getChild(1);

        if (leftNode.getKind().equals(JmmKind.INTEGER_LITERAL) && rightNode.getKind().equals(JmmKind.INTEGER_LITERAL)) {
            String newVal;

            int leftVal = Integer.parseInt(leftNode.get("value"));
            int rightVal = Integer.parseInt(rightNode.get("value"));
            String op = jmmNode.get("op");

            switch (op) {
                case "+" -> newVal = String.valueOf(leftVal + rightVal);
                case "-" -> newVal = String.valueOf(leftVal - rightVal);
                case "*" -> newVal = String.valueOf(leftVal * rightVal);
                case "%" -> newVal = String.valueOf(leftVal % rightVal);
                case "/" -> newVal = String.valueOf(leftVal / rightVal);
                default -> {
                    return changed;
                }
            }

            JmmNode newNode = new JmmNodeImpl(JmmKind.INTEGER_LITERAL);
            newNode.put("value",newVal);
            jmmNode.replace(newNode);
            return true;
        }

        return changed;
    }

    private Boolean defaultVisit(JmmNode node, Void unused) {
        boolean changed = false;

        for (var child : node.getChildren()) {
            changed |= visit(child);
        }

        return changed;
    }
}
