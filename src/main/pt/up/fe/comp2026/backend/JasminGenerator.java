package pt.up.fe.comp2026.backend;

import com.sun.tools.jconsole.JConsoleContext;
import org.specs.comp.ollir.*;
import org.specs.comp.ollir.inst.*;
import org.specs.comp.ollir.tree.TreeNode;
import org.specs.comp.ollir.type.ArrayType;
import org.specs.comp.ollir.type.BuiltinKind;
import org.specs.comp.ollir.type.BuiltinType;
import org.specs.comp.ollir.type.ClassType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmClassType;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2026.optimization.OptUtils;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.specs.comp.ollir.InstructionType.NOPER;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";
    private static final String Space = " ";

    private final OllirResult ollirResult;
    private final JasminUtils types;
    private final FunctionClassMap<TreeNode, String> generators;
    boolean isInsideAssignment;
    private List<Report> reports;
    private String code;
    private Method currentMethod;
    private OptUtils utils;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;
        isInsideAssignment = false;

        types = new JasminUtils(ollirResult);
        // Initialize everytime we start a method
        utils = null;
        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(Field.class, this::generateField);
        generators.put(NewInstruction.class, this::generateNewObject);
        generators.put(InvokeSpecialInstruction.class, this::generateInvokeSpecialInstruction);
        generators.put(PutFieldInstruction.class, this::putFieldInstruction);
        generators.put(GetFieldInstruction.class, this::getFieldInstruction);
        generators.put(SingleOpCondInstruction.class, this::generateSingleOpCondInstruction);
        generators.put(GotoInstruction.class, this::generateGotoInstruction);
        generators.put(OpCondInstruction.class, this::generateOpCondInstruction);
        generators.put(UnaryOpInstruction.class, this::generateUnaryOpInstruction);
        generators.put(InvokeVirtualInstruction.class, this::generateInvokeVirtualInstruction);
        generators.put(InvokeStaticInstruction.class, this::generateInvokeStaticInstruction);
        generators.put(ArrayLengthInstruction.class, this::generateArrayLength);
    }


    private String apply(TreeNode node) {
        var code = new StringBuilder();

        // Print the corresponding OLLIR code as a comment
        //code.append("; ").append(node).append(NL);

        code.append(generators.apply(node));

        return code.toString();
    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = apply(ollirResult.getOllirClass());
        }

        return code;
    }


    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();

        // generate class name
        var nameWithPackage = ollirResult.getOllirClass().getClassFullyQualifiedName().replace('.', '/');
        code.append(".class public ").append(nameWithPackage).append(NL).append(NL);

        // TODO: When you support 'extends', this must be updated
        List<String> imports = ollirResult.getOllirClass().getImports();
        var fullSuperClass = types.getQualifiedNameFromClass(ollirResult.getOllirClass().getSuperClass());

        code.append(".super ").append(fullSuperClass).append(NL).append(NL);

        for (var field : ollirResult.getOllirClass().getFields()) {
            code.append(apply(field));
        }

        // generate code for methods
        for (var method : ollirResult.getOllirClass().getMethods()) {

            if (method.isConstructMethod()) {
                code.append(this.generateConstructor(method, fullSuperClass));
                continue;
            }

            code.append(apply(method));
        }

        return code.toString();
    }


    private String generateConstructor(Method method, String fullSuperClass) {
        StringBuilder code = new StringBuilder();
        currentMethod = method;

        code.append(";default constructor" + NL);
        code.append(".method public <init>()V" + NL);
        code.append(TAB).append(".limit stack ").append(calculateStackLimit(method)).append(NL);
        code.append(TAB).append(".limit locals ").append(calculateLimitLocals(method)).append(NL);
        code.append(TAB + "aload_0" + NL);

        for (Instruction inst : method.getInstructions()) {
            code.append(TAB + this.apply(inst));
        }

        code.append(TAB + "return" + NL);
        code.append(".end method" + NL);

        method.getInstructions();

        return code.toString();
    }


    private String generateMethod(Method method) {
        //System.out.println("STARTING METHOD " + method.getMethodName());
        // set method
        currentMethod = method;

        // Initialize utils, to have fresh labels
        utils = new OptUtils(null);

        var code = new StringBuilder();

        // TODO: Modifier is hard-coded
        var modifier = types.getModifier(method.getMethodAccessModifier());


        var staticMod = method.isStaticMethod() ? "static " : "";

        var methodName = method.getMethodName();

        var params = method.getParams().stream()
                .map(p -> types.getTypeDescriptor(p.getType()))
                .collect(Collectors.joining());

        var returnType = types.getTypeDescriptor(method.getReturnType());

        code.append("\n.method ").append(modifier)
                .append(staticMod)
                .append(methodName).append("(").append(params).append(")").append(returnType).append(NL);


        var bodyCode = new StringBuilder();
        for (int i = 0; i < method.getInstructions().toArray().length; i++) {
            Instruction inst = method.getInstr(i);
            bodyCode.append("; ").append(inst.toString()).append(NL);

            //generate label
            for (var label : method.getLabels(inst)) {
                bodyCode.append(TAB).append(label).append(":").append(NL);
            }

            // stack gets complicated with assign to new object
            if (this.tryGenerateAssignNewObject(method, i, bodyCode)) {
                //skips next instruction (invokespecial)
                i += 1;
            } else {
                var instCode = StringLines.getLines(apply(inst)).stream()
                        .collect(Collectors.joining(NL + TAB, TAB, NL));
                bodyCode.append(instCode);
            }
        }

        // Add limits
        code.append(TAB).append(".limit stack ").append(calculateStackLimit(method)).append(NL);
        code.append(TAB).append(".limit locals ").append(calculateLimitLocals(method)).append(NL);

        code.append(TAB).append(bodyCode);

        code.append(".end method\n");
        //System.out.println("METHOD:\n" + code);
        // unset method
        currentMethod = null;
        //System.out.println("ENDING METHOD " + method.getMethodName());
        return code.toString();
    }


    private boolean tryGenerateAssignNewObject(Method method, int instIdx, StringBuilder code) {
        Instruction inst = method.getInstr(instIdx);

        // temp0 := new ...
        if (inst instanceof AssignInstruction assignInstruction
                && assignInstruction.getRhs() instanceof NewInstruction newInstruction) {
            //check out of bounds
            if (instIdx + 1 >= method.getInstructions().size()) {
                return false;
            }

            Instruction nextInst = method.getInstr(instIdx + 1);

            if (nextInst instanceof InvokeSpecialInstruction invokeSpecialInstruction) {
                this.isInsideAssignment = true;
                //emit new
                code.append(this.generateNewObject(newInstruction));
                //emit invokespecial
                code.append(this.generateInvokeSpecialInstruction(invokeSpecialInstruction));
                //emit store
                code.append(this.generateStore(assignInstruction.getDest()));
                this.isInsideAssignment = false;

                return true;
            }
        }

        return false;
    }

    private String generateField(Field field) {
        var code = new StringBuilder();
        code.append(".field").append(Space).append(field.getFieldAccessModifier().toString().toLowerCase()).append(Space).append("'").append(field.getFieldName()).append("'").append(Space).append(types.getTypeDescriptor(field.getFieldType())).append(NL).append(NL);
        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        try {
            isInsideAssignment = true;

            var code = new StringBuilder();

            if (assign.getDest() instanceof ArrayOperand arrayOp) {
                var reg = currentMethod.getVarTable().get(arrayOp.getName());
                code.append(types.getLoad(reg)).append(NL); // pushes arrayref

                for (Element el : arrayOp.getIndexOperands()) {
                    code.append(this.apply(el));            // pushes index
                }

            }
            // generate code for loading what's on the right
            code.append(apply(assign.getRhs()));


            //generate store
            code.append(this.generateStore(assign.getDest()));


            return code.toString();
        } finally {
            isInsideAssignment = false;
        }
    }

    private String generateStore(Element lhs) {
        Operand operand = (Operand) lhs;
        Descriptor reg = currentMethod.getVarTable().get(operand.getName());
        StringBuilder code = new StringBuilder();

        // array generation
        if (lhs instanceof ArrayOperand arrayOp) {
            if (arrayOp.getType() instanceof ArrayType || arrayOp.getType() instanceof ClassType) {
                return code.append("aastore").append(NL).toString();
            } else {
                return code.append("iastore").append(NL).toString();
            }
        }
        return types.getStore(reg) + NL;
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        int val = Integer.parseInt(literal.getLiteral());

        if (val == -1) {
            return "iconst_m1" + NL;
        }
        if (val >= 0 && val <= 5) {
            return "iconst_" + val + NL;
        }
        if (val >= -128 && val <= 127) {
            return "bipush " + val + NL;
        }
        if (val >= -32768 && val <= 32767) {
            return "sipush " + val + NL;
        }

        return "ldc " + val + NL;
    }

    private String generateOperand(Operand operand) {
        StringBuilder code = new StringBuilder();

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName());
        code.append(types.getLoad(reg)).append(NL);
        if (operand instanceof ArrayOperand arrayOp) {
            for (Element el : arrayOp.getIndexOperands()) {
                code.append(this.apply(el));
            }
            if (arrayOp.getType() instanceof ArrayType || arrayOp.getType() instanceof ClassType) {
                code.append("aaload").append(NL);
            } else {
                code.append("iaload").append(NL);
            }
        }
        return code.toString();
    }


    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();
        Element lhs = binaryOp.getLeftOperand();
        Element rhs = binaryOp.getRightOperand();


        if (!types.isComparisonOp(binaryOp)) {
            code.append(apply(lhs));
            code.append(apply(rhs));
        } else {
            boolean isLeftZero = types.isElementZero(lhs);
            boolean isRightZero = types.isElementZero(rhs);

            if (isLeftZero && isRightZero) {
                code.append(apply(lhs));
            } else if (isLeftZero) {
                code.append(apply(rhs));
            } else if (isRightZero) {
                code.append(apply(lhs));
            } else {
                code.append(apply(lhs));
                code.append(apply(rhs));
            }
        }

        String binCode;
        Operation op = binaryOp.getOperation();
        if (types.isArithmeticOp(binaryOp)) {
            binCode = this.generateArithmeticOps(op);
        } else if (types.isComparisonOp(binaryOp)) {
            binCode = this.generateComparisonOps(op.getOpType(), lhs, rhs);
        } else {
            throw new NotImplementedException(binaryOp.getOperation().getOpType());
        }

        code.append(binCode).append(NL);
        return code.toString();
    }


    private String generateArithmeticOps(Operation operation) {
        String typePrefix = types.getTypePrefix(operation.getTypeInfo());
        OperationType opType = operation.getOpType();

        return typePrefix
                + switch (opType) {
            case ADD -> "add";
            case MUL -> "mul";
            case SUB -> "sub";
            case DIV -> "div";
            case REM -> "rem";
            default -> throw new IllegalStateException("Unexpected value: " + opType);
        };
    }

    private String generateComparisonOps(OperationType operationType, Element lhs, Element rhs) {
        String ifId = types.getIfInstructionCode(operationType, lhs, rhs);
        return this.generateBooleanFromIf(ifId);
    }


    private String generateBooleanFromIf(String ifInstruction) {
        StringBuilder code = new StringBuilder();
        String trueLabel = utils.nextLabel("jasmin_cmp_true");
        String endLabel = utils.nextLabel("jasmin_cmp_end");
        String loadID = "iconst_";
        String trueValue = loadID + "1";
        String elseValue = loadID + "0";

        code.append(ifInstruction).append(Space).append(trueLabel).append(NL);
        code.append(elseValue).append(NL);
        code.append("goto" + Space).append(endLabel).append(NL);
        code.append(trueLabel).append(":").append(NL);
        code.append(trueValue).append(NL);
        code.append(endLabel).append(":").append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        var returnType = returnInst.getReturnType();

        var typePrefix = types.getTypePrefix(returnType);

        // Load operand into the stack, if present
        returnInst.getOperand().ifPresent(op -> code.append(apply(op)));

        code.append(typePrefix).append("return").append(NL);

        return code.toString();
    }


    private String generateNewObject(NewInstruction newInst) {
        var code = new StringBuilder();
        if (newInst.getReturnType() instanceof ClassType) {
            ClassType classType = (ClassType) newInst.getReturnType();
            String qualifiedName = types.getQualifiedNameFromClass(classType.getName());
            code.append("new ").append(qualifiedName).append(NL);
            // needs to dup for store
            if (isInsideAssignment) {
                code.append("dup" + NL);
            }

        } else {
            String type;
            int dimensions = newInst.getArguments().size();
            if (newInst.getArguments().size() == 1) {
                String size;
                size = this.apply(newInst.getArguments().getLast());
                code.append(size).append(NL);
                type = types.getType(newInst.getCaller().getType());
                code.append("newarray ").append(type).append(NL);
            } else {
                String sizes = newInst.getArguments().stream().map(this::apply).collect(Collectors.joining()).toString();
                code.append(sizes);
                type = types.getTypeDescriptor(newInst.getCaller().getType());
                code.append("multianewarray ").append(type).append(Space).append(dimensions).append(NL);
            }


        }


        return code.toString();
    }


    private String generateInvokeSpecialInstruction(InvokeSpecialInstruction invokeSpecialInst) {
        StringBuilder code = new StringBuilder();
        String initClassName = "java/lang/Object";

        if (invokeSpecialInst.getSuperClass().isPresent()) {
            initClassName = invokeSpecialInst.getSuperClass().get();
        } else {
            Element caller = invokeSpecialInst.getCaller();
            if (caller.getType() instanceof ClassType classType) {
                initClassName = types.getQualifiedNameFromClass(classType.getName());
            }
        }

        // load arguments
        invokeSpecialInst.getArguments().forEach(element -> code.append(this.apply(element)));

        //parameter descriptor
        StringBuilder argDescriptors = new StringBuilder();
        invokeSpecialInst.getArguments().forEach(element -> argDescriptors.append(types.getTypeDescriptor(element.getType())));

        code.append("invokespecial" + Space).append(initClassName)
                .append("/<init>(" + argDescriptors.toString() + ")V").append(NL);

        return code.toString();
    }

    private String generateInvokeVirtualInstruction(InvokeVirtualInstruction invokeVirtualInst) {
        StringBuilder code = new StringBuilder();
        String initClassName = "java/lang/Object";
        Element caller = invokeVirtualInst.getCaller();
        code.append(apply(caller));
        if (caller.getType() instanceof ClassType classType) {
            initClassName = types.getQualifiedNameFromClass(classType.getName());
        }

        StringBuilder argtypes = new StringBuilder();
        // load arguments
        invokeVirtualInst.getArguments().forEach(element -> {
            argtypes.append(types.getTypeDescriptor(element.getType()));
            code.append(this.apply(element));
        });
        var methodnamenode = invokeVirtualInst.getMethodName().toString().split(":");
        var methodname = methodnamenode[methodnamenode.length - 1].split("\\.")[0].trim();
        code.append("invokevirtual" + Space).append(initClassName).append("/" + methodname).append("(").append(argtypes).append(")").append(types.getTypeDescriptor(invokeVirtualInst.getReturnType())).append(NL);


        if (!isInsideAssignment && invokeVirtualInst.getReturnType() instanceof BuiltinType builtinType
                && !builtinType.getKind().equals(BuiltinKind.VOID))
        {
            code.append("pop" + NL);
        }

        return code.toString();
    }

    private String generateInvokeStaticInstruction(InvokeStaticInstruction invokeStaticInst) {
        StringBuilder code = new StringBuilder();
        String initClassName = "java/lang/Object";
        Element caller = invokeStaticInst.getCaller();
        if (caller.getType() instanceof ClassType classType) {
            initClassName = types.getQualifiedNameFromClass(classType.getName());
        }

        StringBuilder argtypes = new StringBuilder();
        // load arguments
        invokeStaticInst.getArguments().forEach(element -> {
            argtypes.append(types.getTypeDescriptor(element.getType()));
            code.append(this.apply(element));
        });
        var methodnamenode = invokeStaticInst.getMethodName().toString().split(":");
        var methodname = methodnamenode[methodnamenode.length - 1].split("\\.")[0].trim();
        code.append("invokestatic" + Space).append(initClassName).append("/" + methodname).append("(").append(argtypes).append(")").append(types.getTypeDescriptor(invokeStaticInst.getReturnType())).append(NL);

        if (!isInsideAssignment && invokeStaticInst.getReturnType() instanceof BuiltinType builtinType
                && !builtinType.getKind().equals(BuiltinKind.VOID))
        {
            code.append("pop" + NL);
        }

        return code.toString();
    }


    private String putFieldInstruction(PutFieldInstruction putFieldInstruction) {
        var code = new StringBuilder();
        Operand fieldEL = putFieldInstruction.getField();
        Operand objectEl = putFieldInstruction.getObject();
        code.append(apply(objectEl));
        code.append(apply(putFieldInstruction.getValue()));
        code.append("putfield").append(Space).append(types.getQualifiedNameFromClass(objectEl.getName())).append("/").append(fieldEL.getName()).append(Space).append(types.getTypeDescriptor(fieldEL.getType())).append(NL);
        return code.toString();
    }

    private String getFieldInstruction(GetFieldInstruction getFieldInstruction) {
        var code = new StringBuilder();
        Operand fieldEL = getFieldInstruction.getField();
        Operand objectEl = getFieldInstruction.getObject();
        code.append(apply(objectEl));
        var p = fieldEL.getName();
        code.append("getfield").append(Space).append(types.getQualifiedNameFromClass(objectEl.getName())).append("/").append(fieldEL.getName()).append(Space).append(types.getTypeDescriptor(fieldEL.getType())).append(NL);
        return code.toString();
    }


    private String generateGotoInstruction(GotoInstruction gotoInst) {
        StringBuilder code = new StringBuilder();
        code.append("goto ").append(gotoInst.getLabel()).append(NL);
        return code.toString();
    }

    private String generateSingleOpCondInstruction(SingleOpCondInstruction singleCondInst) {
        StringBuilder code = new StringBuilder();

        String true_label = singleCondInst.getLabel();

        String cond = this.apply(singleCondInst.getCondition());
        code.append(cond);
        code.append("ifne" + Space).append(true_label).append(NL);

        return code.toString();
    }


    // called when if condition is not trivial
    private String generateOpCondInstruction(OpCondInstruction opCondInst) {
        StringBuilder code = new StringBuilder();

        String true_label = opCondInst.getLabel();
        OpInstruction cond = opCondInst.getCondition();

        // if (!a) -> puts the boolean value in stack
        if (cond instanceof UnaryOpInstruction unaryInst) {
            code.append(this.generateUnaryOpInstruction(unaryInst));
        }

        // go to true label if generated boolean above is != 0
        String ifInst = "ifne";
        code.append(ifInst).append(Space).append(true_label).append(NL);

        return code.toString();
    }


    private String generateUnaryOpInstruction(UnaryOpInstruction unaryOpInst) {
        StringBuilder code = new StringBuilder();
        code.append(this.apply(unaryOpInst.getOperand()));
        code.append(this.generateBooleanFromIf("ifeq"));
        return code.toString();
    }

    private int calculateLimitLocals(Method method) {
        int maxReg = method.isStaticMethod() ? -1 : 0;
        for (Descriptor descriptor : method.getVarTable().values()) {
            maxReg = Math.max(maxReg, descriptor.getVirtualReg());
        }
        return maxReg + 1;
    }

    private int calculateStackLimit(Method method) {
        int maxStack = 0;
        int currentStack = 0;
        List<Instruction> insts = method.getInstructions();
        for (int i = 0; i < insts.size(); i++) {
            Instruction inst = insts.get(i);
            int stackHeight = calculateInstStackLimit(inst);
            maxStack = Math.max(maxStack, stackHeight);
        }
        return maxStack;
    }

    private int calculateInstStackLimit(Instruction inst) {
        System.out.println(inst);
        if (inst instanceof AssignInstruction assign) {
            var cost = 0;
            if (assign.getDest() instanceof ArrayOperand) {
                cost += 2;
            }
            var t = calculateInstStackLimit(assign.getRhs());
            System.out.println("this is" + t);
            return cost + calculateInstStackLimit(assign.getRhs());
        } else if (inst instanceof SingleOpInstruction singleop) {
            return singleop.getSingleOperand() instanceof ArrayOperand ? 2 : 1;
        } else if (inst instanceof BinaryOpInstruction) {
            return 2;
        } else if (inst instanceof NewInstruction newInst) {
            if (newInst.getReturnType() instanceof ArrayType arrayType) {
                return arrayType.getNumDimensions();
            }
            return 1;
        } else if (inst instanceof InvokeSpecialInstruction invokeSpecial) {

            if (invokeSpecial.getMethodName().toString().contains("<init>")) {
                return 2 + invokeSpecial.getArguments().size();
            }
            return 1 + invokeSpecial.getArguments().size();
        } else if (inst instanceof InvokeVirtualInstruction invokeVirtual) {
            return 1 + invokeVirtual.getArguments().size();
        } else if (inst instanceof InvokeStaticInstruction invokeStatic) {
            return Math.max(1, invokeStatic.getArguments().size());
        } else if (inst instanceof PutFieldInstruction) {
            return 2;
        } else if (inst instanceof GetFieldInstruction) {
            return 1;
        } else if (inst instanceof ReturnInstruction ret) {
            return ret.getOperand().isPresent() ? 1 : 0;
        } else if (inst instanceof SingleOpCondInstruction) {
            return 1;
        } else if (inst instanceof OpCondInstruction opCond) {
            return calculateInstStackLimit(opCond.getCondition());
        } else if (inst instanceof UnaryOpInstruction) {
            return 1;
        }
        return 2;
    }

    private String generateArrayLength(ArrayLengthInstruction arrayLengthInst) {
        StringBuilder code = new StringBuilder();
        code.append(this.apply(arrayLengthInst.getCaller()));
        code.append("arraylength").append(NL);
        return code.toString();
    }
}