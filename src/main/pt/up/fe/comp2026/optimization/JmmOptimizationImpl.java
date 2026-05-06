package pt.up.fe.comp2026.optimization;

import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.OllirErrorException;
import org.specs.comp.ollir.VarScope;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2026.ConfigOptions;
import pt.up.fe.comp2026.jmm.ast.JmmKind;
import pt.up.fe.comp2026.optimization.ast.*;
import pt.up.fe.comp2026.optimization.regAllocation.InterferenceGraph;
import pt.up.fe.comp2026.optimization.regAllocation.InterferenceNode;
import pt.up.fe.comp2026.optimization.regAllocation.LivenessAnalysis;
import pt.up.fe.comp2026.utils.ReportUtils;

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
        ArrayInitVisitor arrayInitVisitor=new ArrayInitVisitor();
        UnaryOperators unaryOperators=new UnaryOperators();
        //replace for loops
        rootNode.getDescendants(JmmKind.FOR_STMT).forEach(forLoopTransformation::visit);
        //replace do while
        rootNode.getDescendants(JmmKind.DO_WHILE_STMT).forEach(doWhileVisitor::visit);
        //replace arrayinit
        rootNode.getDescendants(JmmKind.ARRAY_INIT).forEach(arrayInitVisitor::visit);
        //replace leftunary
        rootNode.getDescendants(JmmKind.LEFT_UNARY_EXPR).forEach(unaryOperators::visit);
        //replace rightunary
        rootNode.getDescendants(JmmKind.RIGHT_UNARY_EXPR).forEach(unaryOperators::visit);
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
        System.out.println("transfoorm"+semanticsResult.getRootNode().toTree());
        return semanticsResult;
    }


    @Override
    public OllirResult transformOllir(OllirResult ollirResult) {

        if (ollirResult.config().getOrDefault("debug", "false").equals("true")) {
            System.out.println("OLLIR CODE:");
            System.out.println(ollirResult.getOllirCode());
        }

        int num_regs = ConfigOptions.getRegisterAllocation(ollirResult.config());
        //if (num_regs == -1) return ollirResult;

        // TODO: IF NUM REGS IS LOWER THAN 0 SHOULD ITERATE ON NUM REGS POSSIBLE UNTIL IT IS DONE
        if (num_regs <= 0) num_regs = 8;

        var classUnit = ollirResult.getOllirClass();
        classUnit.buildCFGs();
        classUnit.buildVarTables();

        for(var method : classUnit.getMethods()) {
            LivenessAnalysis livenessAnalysis = new LivenessAnalysis(method);
            livenessAnalysis.performAnalysis();
            livenessAnalysis.printLivenessTable();

            InterferenceGraph graph = new InterferenceGraph(livenessAnalysis.getInstructionTable());

            if (!graph.colorGraph(num_regs)) {
                // add error
            }

            var varTable = method.getVarTable();

            System.out.println("HERE ARE THE VAR NAMES TO USE");
            System.out.println(varTable.keySet());


            int numParamReserved = method.getParams().size();

            for (String var : varTable.keySet()) {
                Descriptor descriptor = varTable.get(var);

                // skip parameters
                if (descriptor.getScope() == VarScope.PARAMETER) {
                    continue;
                }

                InterferenceNode node = graph.matchNodeFromVar(var);

                // skip this keyword and other stuff i do not know
                if (node == null) {
                    System.out.println("VAR NOT IN GRAPH TABLE");
                    continue;
                }

                method.getVarTable().get(var).setVirtualReg(node.getColor() + numParamReserved);
            }

            graph.printGraph();
        }


        //TODO: Do your OLLIR-based optimizations here

        return ollirResult;
    }

}
