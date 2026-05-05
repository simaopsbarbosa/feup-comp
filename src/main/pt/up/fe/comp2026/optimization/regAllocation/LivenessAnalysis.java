package pt.up.fe.comp2026.optimization.regAllocation;

import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.inst.Instruction;

import java.util.ArrayList;
import java.util.List;

public class LivenessAnalysis {

    private final Method method;
    private List<InstructionLiveness> instructionTable;

    public LivenessAnalysis(Method method) {
        this.method = method;
        this.instructionTable = new ArrayList<>();
        this.computeInitialTable();
    }

    public void computeInitialTable() {
        List<Instruction> methodInstructions = this.method.getInstructions();
        for (Instruction instruction : methodInstructions) {
            InstructionLiveness liveness = new InstructionLiveness(instruction);
            instructionTable.add(liveness);
        }
    }




    public void printLivenessTable() {
        System.out.println("Liveness Analysis for method: " + method.getMethodName());

        String format = "%-4s | %-30s | %-15s | %-15s | %-20s | %-20s%n";
        System.out.printf(format, "Line", "Instruction", "In", "Out", "Use", "Def");
        System.out.println("-".repeat(115));

        int line = 0;
        for (InstructionLiveness liveness : instructionTable) {
            String instStr = liveness.getInstruction().toString().trim();
            if (instStr.length() > 30) {
                instStr = instStr.substring(0, 27) + "...";
            }

            System.out.printf(format,
                    line++,
                    instStr,
                    liveness.getIn().toString(),
                    liveness.getOut().toString(),
                    liveness.getUse().toString(),
                    liveness.getDef().toString()
            );
        }
        System.out.println();
    }
}
