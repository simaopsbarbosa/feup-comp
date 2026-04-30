package pt.up.fe.comp2026.optimization.ast;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2026.jmm.ast.JmmAttributes;
import pt.up.fe.comp2026.jmm.ast.JmmKind;

import java.util.List;

public class UnaryExprVisitor extends AJmmVisitor<Void, Boolean> {

    @Override
    protected void buildVisitor() {
        addVisit(JmmKind.LEFT_UNARY_EXPR, this::visitUnaryExpr);
        addVisit(JmmKind.RIGHT_UNARY_EXPR, this::visitUnaryExpr);
        //addVisit(JmmKind.LEFT_UNARY_EXPR, this::visitSignLeftUnaryExpr);
    }

    private boolean visitUnaryExpr(JmmNode unaryStmt, Void unused) {
        JmmNode varRefExpr = unaryStmt.getChild(0);
        String op = unaryStmt.get("op");
        JmmNode newAssignStmt = new JmmNodeImpl(JmmKind.ASSIGN_STMT);
        newAssignStmt.add(varRefExpr);

        JmmNode newBinaryExpr = new JmmNodeImpl(JmmKind.BINARY_EXPR);

        switch (op) {
            case "++" -> {
                newBinaryExpr.put("op", "+");
            }
            case "--" -> {
                newBinaryExpr.put("op", "-");
            }
            default -> {
                return false;
            }
        }

        newBinaryExpr.add(varRefExpr);

        JmmNode newIntegerLiteral = new JmmNodeImpl(JmmKind.INTEGER_LITERAL);
        newIntegerLiteral.put("value", "1");
        newBinaryExpr.add(newIntegerLiteral);
        newAssignStmt.add(newBinaryExpr);

        unaryStmt.getParent().replace(newAssignStmt);
        return true; // :D
    }

    // might not need this
//    private boolean visitSignLeftUnaryExpr(JmmNode unaryStmt, Void unused) {
//        String op = unaryStmt.get("op");
//        JmmNode rightVar = unaryStmt.getChild(0);
//
//        // replace with leftVar = rightVar * -1;
//        if (op.equals("-")) {
//            JmmNode newBinaryExpr = new JmmNodeImpl(JmmKind.BINARY_EXPR);
//            newBinaryExpr.put("op", "*");
//            newBinaryExpr.add(rightVar);
//            JmmNode newIntegerLiteral = new JmmNodeImpl(JmmKind.INTEGER_LITERAL);
//            newIntegerLiteral.put("value", "-1");
//            newBinaryExpr.add(newIntegerLiteral);
//
//            unaryStmt.replace(newBinaryExpr);
//        } else if (op.equals("+")) {
//            unaryStmt.replace(rightVar);
//        }
//
//
//        return true;
//    }
}
