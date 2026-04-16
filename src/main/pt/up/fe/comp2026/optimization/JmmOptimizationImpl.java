package pt.up.fe.comp2026.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2026.jmm.ast.JmmAttributes;
import pt.up.fe.comp2026.jmm.ast.JmmKind;


import java.util.Collections;
import java.util.List;

public class JmmOptimizationImpl implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {

        // Create visitor that will generate the OLLIR code
        var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());

        // Visit the AST and obtain OLLIR code
        var ollirCode = visitor.visit(semanticsResult.getRootNode());

//        System.out.println("\nOLLIR:\n\n" + ollirCode);

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    @Override
    public JmmSemanticsResult transformAst(JmmSemanticsResult semanticsResult) {
        JmmNode rootNode = semanticsResult.getRootNode();

        //replace for loops
        rootNode.getDescendants(JmmKind.FOR_STMT).stream().forEach(forNode -> {
            forNode.replace(transformForLoop(forNode));
        } );

        //TODO: Do your AST-based optimizations here
        return semanticsResult;
    }


    public JmmNode transformForLoop(JmmNode forStmt) {
        JmmNode outerBlockNode = new JmmNodeImpl(JmmKind.BLOCK);
        boolean hasInit = !forStmt.getChildren(JmmKind.FOR_INIT).isEmpty();
        boolean hasCond = !forStmt.getChildren(JmmKind.FOR_COND).isEmpty();
        boolean hasUpdate = !forStmt.getChildren(JmmKind.FOR_UPDATE).isEmpty();

        //add initializations before while loop
        if (hasInit) {
            List<JmmNode> initNodes = forStmt.getChildren(JmmKind.FOR_INIT).getFirst().getChildren(JmmKind.ASSIGNMENT);
            initNodes.forEach(initNode -> outerBlockNode.add(initNode));
        }

        JmmNode whileNode = new JmmNodeImpl(JmmKind.WHILE_STMT);

        //handle condition transform
        if (!hasCond) {
            JmmNode condNode = new JmmNodeImpl(JmmKind.BOOL_LITERAL);
            condNode.putObject(JmmAttributes.BOOL_LITERAL.BOOL, "true");
            whileNode.add(condNode);
        } else {
            JmmNode condNode = forStmt.getChildren(JmmKind.FOR_COND).getFirst().getChildren(JmmKind.EXPR).getFirst();
            whileNode.add(condNode);
        }

        JmmNode stmt = forStmt.getChildren(JmmKind.STMT).getFirst();

        if (hasUpdate) {
            JmmNode innerBlockNode = new JmmNodeImpl(JmmKind.BLOCK);

            // add stmt from for loop
            innerBlockNode.add(stmt);

            // add updates from for loop
            List<JmmNode> updateNodes = forStmt.getChildren(JmmKind.FOR_UPDATE).getFirst().getChildren(JmmKind.ASSIGNMENT);
            updateNodes.forEach(updateNode -> innerBlockNode.add(updateNode));

            whileNode.add(innerBlockNode);
        } else {
            whileNode.add(stmt);
        }

        //add while loop to block
        outerBlockNode.add(whileNode);

        return outerBlockNode;
    }



    @Override
    public OllirResult transformOllir(OllirResult ollirResult) {

        if (ollirResult.config().getOrDefault("debug", "false").equals("true")) {
            System.out.println("OLLIR CODE:");
            System.out.println(ollirResult.getOllirCode());
        }

        //TODO: Do your OLLIR-based optimizations here

        return ollirResult;
    }

}
