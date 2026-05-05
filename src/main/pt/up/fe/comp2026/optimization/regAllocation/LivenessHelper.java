package pt.up.fe.comp2026.optimization.regAllocation;

import org.specs.comp.ollir.ArrayOperand;
import org.specs.comp.ollir.Element;
import org.specs.comp.ollir.InstructionType;
import org.specs.comp.ollir.Operand;
import org.specs.comp.ollir.inst.AssignInstruction;
import org.specs.comp.ollir.inst.Instruction;

import java.util.Set;

public class LivenessHelper {

    public static Set<String> computeDef(Instruction instruction) {
        InstructionType type = instruction.getInstType();

        if (type != InstructionType.ASSIGN) return Set.of();

        AssignInstruction assignInstruction = (AssignInstruction) instruction;
        Element dest = assignInstruction.getDest();

        if (dest instanceof ArrayOperand) return Set.of();
        if (dest instanceof Operand op) return Set.of(op.getName());
        return Set.of();
    }
}
