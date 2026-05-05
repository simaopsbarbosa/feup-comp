package pt.up.fe.comp2026.optimization.regAllocation;

import org.specs.comp.ollir.InstructionType;
import org.specs.comp.ollir.inst.Instruction;

import java.util.HashSet;
import java.util.Set;

public class InstructionLiveness {

    private final Instruction instruction;

    private Set<String> in;
    private Set<String> out;
    private final Set<String> use;
    private final Set<String> def;

    public InstructionLiveness(Instruction instruction) {
        this.instruction = instruction;
        this.in = new HashSet<>();
        this.out = new HashSet<>();
        this.use = computeUse(this.instruction);
        this.def = computeDef(this.instruction);
    }

    public static Set<String> computeUse(Instruction instruction) {
        return Set.of();
    }


    public static Set<String> computeDef(Instruction instruction) {
        return LivenessHelper.computeDef(instruction);
    }


    public void setIn(Set<String> in) {this.in = in;}

    public void setOut(Set<String> out) {this.out = out;}


    public Instruction getInstruction() {return instruction;}
    public Set<String> getIn() {return in;}
    public Set<String> getOut() {return out;}
    public Set<String> getUse() {return use;}
    public Set<String> getDef() {return def;}
}
