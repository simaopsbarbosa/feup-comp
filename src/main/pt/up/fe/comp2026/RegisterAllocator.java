package pt.up.fe.comp2026;

import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.VarScope;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2026.optimization.regAllocation.InterferenceGraph;
import pt.up.fe.comp2026.optimization.regAllocation.InterferenceNode;
import pt.up.fe.comp2026.optimization.regAllocation.LivenessAnalysis;

public class RegisterAllocator {

    ClassUnit classUnit;
    int maxNumRegs;

    public RegisterAllocator(OllirResult ollirResult, int maxNumRegs) {
        this.classUnit = ollirResult.getOllirClass();
        this.classUnit.buildCFGs();
        this.classUnit.buildVarTables();

        this.maxNumRegs = maxNumRegs;
    }


    public void performClassAllocations() {
        for (Method method : classUnit.getMethods()) {
            performAllocation(method);
        }
    }


    private void performAllocation(Method method) {
        if (maxNumRegs > 0) {
            tryAllocationWithK(method, this.maxNumRegs);
            return;
        }

        // perform allocations with minimal amount of registers
        int numRegs = 0;
        boolean canAllocate = false;

        while (!canAllocate) {
            numRegs++;
            canAllocate = tryAllocationWithK(method, numRegs);
        }

        System.out.println("ALLOCATED WITH " + numRegs +  " REGISTERS\n\n\n");
    }


    private boolean tryAllocationWithK(Method method, int numRegs) {
        LivenessAnalysis livenessAnalysis = new LivenessAnalysis(method);
        livenessAnalysis.performAnalysis();

        livenessAnalysis.printLivenessTable();

        InterferenceGraph graph = new InterferenceGraph(livenessAnalysis.getInstructionTable());

        if (!graph.colorGraph(numRegs)) {
            return false;
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
        return true;
    }
}
