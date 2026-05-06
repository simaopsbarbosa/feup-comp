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
            InstructionLiveness liveness = new InstructionLiveness(instruction, this.method);
            instructionTable.add(liveness);
        }
    }

    public void performAnalysis() {
        boolean hasChanges = true;

        while (hasChanges) {
            hasChanges = false;

            for (InstructionLiveness liveness : this.instructionTable) {

                int prevInSize = liveness.getIn().size();
                int prevOutSize = liveness.getOut().size();

                // compute new In set
                liveness.getIn().addAll(liveness.getOut());
                liveness.getIn().removeAll(liveness.getDef());
                liveness.getIn().addAll(liveness.getUse());

                //compute new Out set
                Instruction instruction = liveness.getInstruction();
                for (Instruction nextInst : instruction.getSuccessorsAsInst()) {
                    InstructionLiveness nextLiveness = LivenessHelper.getLivenessInstFromTable(this.instructionTable, nextInst);

                    if (nextLiveness != null) {
                        liveness.getOut().addAll(nextLiveness.getIn());
                    }
                }

                int newInSize = liveness.getIn().size();
                int newOutSize = liveness.getOut().size();

                if (newInSize != prevInSize || newOutSize != prevOutSize)
                    hasChanges = true;
            }
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


    public List<InstructionLiveness> getInstructionTable() {return instructionTable;}
}
