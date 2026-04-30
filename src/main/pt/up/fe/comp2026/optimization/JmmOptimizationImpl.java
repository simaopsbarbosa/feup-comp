package pt.up.fe.comp2026.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2026.ConfigOptions;
import pt.up.fe.comp2026.jmm.ast.JmmKind;
import pt.up.fe.comp2026.optimization.ast.*;

import java.util.Collections;
import java.util.HashMap;

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
        ForLoopVisitor forLoopTransformation = new ForLoopVisitor();
        DoWhileVisitor doWhileVisitor=new DoWhileVisitor();
        UnaryExprVisitor unaryExprVisitor=new UnaryExprVisitor();
        //replace for loops
        rootNode.getDescendants(JmmKind.FOR_STMT).forEach(forLoopTransformation::visit);
        //replace do while
        rootNode.getDescendants(JmmKind.DO_WHILE_STMT).forEach(doWhileVisitor::visit);
        //replace unary expressions
        rootNode.getDescendants(JmmKind.RIGHT_UNARY_EXPR).forEach(unaryExprVisitor::visit);
        rootNode.getDescendants(JmmKind.LEFT_UNARY_EXPR).forEach(unaryExprVisitor::visit);
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
