package pt.up.fe.comp2026.optimization.regAllocation;

import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.VarScope;
import org.specs.comp.ollir.inst.Instruction;

import java.util.HashSet;
import java.util.Set;

public class InstructionLiveness {

    private final Instruction instruction;

    private Set<String> in;
    private Set<String> out;
    private final Set<String> use;
    private final Set<String> def;

    public InstructionLiveness(Instruction instruction, Method method) {
        this.instruction = instruction;
        this.in = new HashSet<>();
        this.out = new HashSet<>();
        this.use = computeUse(this.instruction);
        this.def = computeDef(this.instruction);

        ignoreParams(this.use, method);
        ignoreParams(this.def, method);
    }

    public static Set<String> computeUse(Instruction instruction) {
        return LivenessHelper.computeUse(instruction);
    }


    public static Set<String> computeDef(Instruction instruction) {
        return LivenessHelper.computeDef(instruction);
    }


    private void ignoreParams(Set<String> set, Method method) {
        set.removeIf(varName -> {
            var descriptor = method.getVarTable().get(varName);
            if (descriptor == null) return false;

            VarScope scope = descriptor.getScope();
            return scope == VarScope.PARAMETER;
        });
    }

    public void setIn(Set<String> in) {this.in = in;}

    public void setOut(Set<String> out) {this.out = out;}


    public Instruction getInstruction() {return instruction;}
    public Set<String> getIn() {return in;}
    public Set<String> getOut() {return out;}
    public Set<String> getUse() {return use;}
    public Set<String> getDef() {return def;}
}
