package pt.up.fe.comp2026.optimization.regAllocation;

import org.specs.comp.ollir.ArrayOperand;
import org.specs.comp.ollir.Element;
import org.specs.comp.ollir.InstructionType;
import org.specs.comp.ollir.Operand;
import org.specs.comp.ollir.inst.*;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class LivenessHelper {


    public static void addElementToSet(Set<String> set, Element element) {

        if (element instanceof ArrayOperand arrOp) {
            set.add(arrOp.getName());
            AddElementsToSet(set, arrOp.getIndexOperands());
        }

        else if (element instanceof Operand op) {
            set.add(op.getName());
        }
    }

    public static void AddElementsToSet(Set<String> set, List<Element> elements) {
        for (Element element : elements) {
            addElementToSet(set, element);
        }
    }


    public static Set<String> computeDef(Instruction instruction) {
        InstructionType type = instruction.getInstType();
        Set<String> use = new HashSet<>();

        if (type != InstructionType.ASSIGN) return use;


        AssignInstruction assignInstruction = (AssignInstruction) instruction;
        Element dest = assignInstruction.getDest();

        // array access should not be in def set
        if (dest instanceof ArrayOperand) return use;

        addElementToSet(use, dest);

        return use;
    }



    public static Set<String> computeUse(Instruction instruction) {
        InstructionType type = instruction.getInstType();
        Set<String> use = new HashSet<>();

        switch (type) {

            case ASSIGN -> {
                AssignInstruction assign = (AssignInstruction) instruction;

                // when arr[i] = _; 'i' needs to be in use set
                if (assign.getDest() instanceof ArrayOperand) {
                    addElementToSet(use, assign.getDest());
                }

                Instruction rhs = assign.getRhs();
                use.addAll(computeUse(rhs));
            }

            case BINARYOPER -> {
                BinaryOpInstruction binary = (BinaryOpInstruction) instruction;
                AddElementsToSet(use, binary.getOperands());
            }

            case NOPER -> {
                SingleOpInstruction single = (SingleOpInstruction) instruction;
                Element element = single.getSingleOperand();
                addElementToSet(use, element);
            }

            case BRANCH -> {
                CondBranchInstruction branch = (CondBranchInstruction) instruction;
                AddElementsToSet(use, branch.getOperands());
            }

            //test this
            case UNARYOPER -> {
                UnaryOpInstruction unary = (UnaryOpInstruction) instruction;
                AddElementsToSet(use, unary.getOperands());
            }

            //TODO: not sure how to do
            case CALL -> {

            }

            // not sure if these are correct
            case GETFIELD -> {
                GetFieldInstruction field = (GetFieldInstruction) instruction;
                AddElementsToSet(use, field.getOperands());
            }

            case PUTFIELD ->  {
                PutFieldInstruction field = (PutFieldInstruction) instruction;
                AddElementsToSet(use, field.getOperands());
            }

            case RETURN -> {
                ReturnInstruction returnInst = (ReturnInstruction) instruction;
                Optional<Element> optElement = returnInst.getOperand();
                optElement.ifPresent(element -> addElementToSet(use, element));
            }


        }

        return use;
    }

}
