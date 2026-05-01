package pt.up.fe.comp2026.optimization.ast;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import pt.up.fe.comp2026.jmm.ast.JmmAttributes;
import pt.up.fe.comp2026.jmm.ast.JmmKind;


public class ConstantFoldingVisitor extends AJmmVisitor<Void, Boolean> {

    @Override
    protected void buildVisitor() {
        addVisit(JmmKind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit(JmmKind.LOGICAL_EXPR, this::visitLogicalExpr);
        addVisit(JmmKind.EQUALITY_EXPR, this::visitEqualityExpr);
        addVisit(JmmKind.LOGICAL_NOT,this::visitNot);
        addVisit(JmmKind.PARENTESES,this::visitParenteses);
        setDefaultVisit(this::defaultVisit);
    }
    private boolean visitParenteses(JmmNode node,Void unused){
        boolean changed = false;
        JmmNode child=node.getChild(0);
        changed |= visit(child);
        node.replace(child);
        return true;
    }

    private boolean visitNot(JmmNode node,Void unused){
        boolean changed = false;
        JmmNode child=node.getChild(0);
        changed = visit(child);
        if(child.getKind()== JmmKind.BOOL_LITERAL){
            var value=child.get("bool").equals("true")?"false":"true";
            JmmNodeImpl boolLiteral=new JmmNodeImpl(JmmKind.BOOL_LITERAL);
            boolLiteral.put("bool",value);
            node.replace(boolLiteral);
        }
        if(child.getKind()==JmmKind.LOGICAL_NOT){
            JmmNode grandChild=child.getChild(0);
            node.replace(grandChild);
            return true;
        }

        return changed;
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
            newNode.put("value", newVal);
            jmmNode.replace(newNode);
            return true;
        }

        return changed;
    }

    private Boolean visitLogicalExpr(JmmNode jmmNode, Void unused) {
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
                case "<" -> {
                    newVal = String.valueOf(leftVal < rightVal);
                }
                case ">" -> {
                    newVal = String.valueOf(leftVal > rightVal);
                }
                case "<=" -> {
                    newVal = String.valueOf(leftVal <= rightVal);
                }
                case ">=" -> {
                    newVal = String.valueOf(leftVal >= rightVal);
                }
                default -> {
                    return changed;
                }
            }

            JmmNode newNode = new JmmNodeImpl(JmmKind.BOOL_LITERAL);
            newNode.put("bool", newVal);
            jmmNode.replace(newNode);
            return true;
        } else if (leftNode.getKind().equals(JmmKind.BOOL_LITERAL) && rightNode.getKind().equals(JmmKind.BOOL_LITERAL)) {
            String newVal;

            boolean leftVal = Boolean.parseBoolean(leftNode.get("bool"));
            boolean rightVal = Boolean.parseBoolean(rightNode.get("bool"));
            String op = jmmNode.get("op");

            switch (op) {
                case "&&" -> {
                    newVal = String.valueOf(leftVal && rightVal);
                }
                case "||" -> {
                    newVal = String.valueOf(leftVal || rightVal);
                }
                default -> {
                    return changed;
                }
            }

            JmmNode newNode = new JmmNodeImpl(JmmKind.BOOL_LITERAL);
            newNode.put("bool", newVal);
            jmmNode.replace(newNode);
            return true;
        }

        return changed;
    }

    private Boolean visitEqualityExpr(JmmNode jmmNode, Void unused) {
        boolean changed = false;

        for (var child : jmmNode.getChildren()) {
            changed |= visit(child);
        }

        JmmNode leftNode = jmmNode.getChild(0);
        JmmNode rightNode = jmmNode.getChild(1);

        boolean res;
        String op = jmmNode.get("op");
        String valueString;

        if (leftNode.getKind() == JmmKind.INTEGER_LITERAL && rightNode.getKind() == JmmKind.INTEGER_LITERAL) {
            valueString = "value";
        } else if (leftNode.getKind() == JmmKind.BOOL_LITERAL && rightNode.getKind() == JmmKind.BOOL_LITERAL) {
            valueString = "bool";
        } else {
            return changed;
        }

        switch (op) {
            case "==" -> {
                res = leftNode.get(valueString).equals(rightNode.get(valueString));
                changed = true;
            }
            case "!=" -> {
                res = !leftNode.get(valueString).equals(rightNode.get(valueString));
                changed = true;
            }
            default -> {
                return changed;
            }
        }

        if (changed) {
            JmmNode newNode = new JmmNodeImpl(JmmKind.BOOL_LITERAL);
            newNode.put("bool", String.valueOf(res));
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
