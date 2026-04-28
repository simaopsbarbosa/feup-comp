package pt.up.fe.comp2026.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2026.ConfigOptions;
import pt.up.fe.comp2026.jmm.ast.JmmAttributes;
import pt.up.fe.comp2026.jmm.ast.JmmKind;


import java.util.Collections;
import java.util.HashMap;
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

        //System.out.println(semanticsResult);


        boolean hasChanges = true;
        int passes = 0;

        if (ConfigOptions.getOptimize(semanticsResult.config())) {
            while (hasChanges) {
                ConstantPropagationVisitor propagator = new ConstantPropagationVisitor();
                hasChanges = propagator.visit(rootNode, new HashMap<>());

                ConstantFoldingVisitor folder = new ConstantFoldingVisitor();
                hasChanges |= folder.visit(rootNode);

                BranchEliminationVisitor branchEliminator = new BranchEliminationVisitor();
                hasChanges |= branchEliminator.visit(rootNode);

                DeadCodeEliminationVisitor deadCodeEliminator = new DeadCodeEliminationVisitor();
                hasChanges |= deadCodeEliminator.visit(rootNode);

                passes++;
            }
        }

        System.out.println(passes + " passes completed");
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

            initNodes.forEach(initNode -> {
                JmmNode assigntStmt = new JmmNodeImpl(JmmKind.ASSIGN_STMT);
                assigntStmt.add(initNode.getChild(0));
                assigntStmt.add(initNode.getChild(1));
                outerBlockNode.add(assigntStmt);
            });
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
                updateNodes.forEach(updateNode -> {
                    JmmNode assignStmt = new JmmNodeImpl(JmmKind.ASSIGN_STMT);
                    assignStmt.add(updateNode.getChild(0));
                    assignStmt.add(updateNode.getChild(1));
                    innerBlockNode.add(assignStmt);
                });

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
