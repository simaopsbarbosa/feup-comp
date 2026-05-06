package pt.up.fe.comp2026.optimization.ast;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2026.jmm.ast.JmmAttributes;
import pt.up.fe.comp2026.jmm.ast.JmmKind;

import java.util.List;

public class UnaryOperators extends AJmmVisitor<Void, Boolean> {

    @Override
    protected void buildVisitor() {
        addVisit(JmmKind.LEFT_UNARY_EXPR, this::visitLeftUnary);
    }


    private boolean visitLeftUnary(JmmNode node, Void unused) {
        JmmNode outerBlockNode = new JmmNodeImpl(JmmKind.BINARY_EXPR);
        String op=node.get("op");
        switch (op){
            case "-"-> {
                JmmNode left = new JmmNodeImpl(JmmKind.INTEGER_LITERAL);
                JmmNode right = node.getChild(0);
                left.put("value", "0");
                outerBlockNode.put("op", op);
                outerBlockNode.add(left);
                outerBlockNode.add(right);
                node.replace(outerBlockNode);
            }
            case"+"->{
                JmmNode right = node.getChild(0);
                node.replace(right);
            }
        }

        return true;
    }


}
