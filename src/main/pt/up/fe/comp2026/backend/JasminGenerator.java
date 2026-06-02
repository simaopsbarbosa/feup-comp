package pt.up.fe.comp2026.backend;

import com.sun.tools.jconsole.JConsoleContext;
import org.specs.comp.ollir.*;
import org.specs.comp.ollir.inst.*;
import org.specs.comp.ollir.tree.TreeNode;
import org.specs.comp.ollir.type.ClassType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmClassType;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2026.optimization.OptUtils;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    private List<Report> reports;

    private String code;

    private Method currentMethod;

    boolean isInsideAssignment;

    private final JasminUtils types;
    private OptUtils utils;
    private final FunctionClassMap<TreeNode, String> generators;

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
        generators.put(NewInstruction.class, this::generateNewClass);
        generators.put(InvokeSpecialInstruction.class, this::generateInvokeSpecialInstruction);
        generators.put(PutFieldInstruction.class, this::putFieldInstruction);
        generators.put(GetFieldInstruction.class, this::getFieldInstruction);
        generators.put(SingleOpCondInstruction.class, this::generateSingleOpCondInstruction);
        generators.put(GotoInstruction.class, this::generateGotoInstruction);
        generators.put(OpCondInstruction.class, this::generateOpCondInstruction);
        generators.put(UnaryOpInstruction.class, this::generateUnaryOpInstruction);
        generators.put(InvokeVirtualInstruction.class,this::generateInvokeVirtualInstruction);
        generators.put(InvokeStaticInstruction.class,this::generateInvokeStaticInstruction);
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
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);
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
            
            //generate label
            for (var label : method.getLabels(inst)) {
                bodyCode.append(TAB).append(label).append(":").append(NL);
            }

            // stack gets complicated with assign to new object
            if (this.tryGenerateAssignNewObject(method, i, bodyCode)) {
                //skips next instruction (invokespecial)
                i += 1;
            }
            else {
                var instCode = StringLines.getLines(apply(inst)).stream()
                        .collect(Collectors.joining(NL + TAB, TAB, NL));
                bodyCode.append(instCode);
            }
        }

        // Add limits
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

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
                && assignInstruction.getRhs() instanceof NewInstruction newInstruction)
        {
            //check out of bounds
            if (instIdx + 1 >= method.getInstructions().size()) {
                return false;
            }

            Instruction nextInst = method.getInstr(instIdx + 1);

            if (nextInst instanceof InvokeSpecialInstruction invokeSpecialInstruction) {
                this.isInsideAssignment = true;
                //emit new
                code.append(this.generateNewClass(newInstruction));
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
        return types.getStore(reg) + NL;
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName());

        return types.getLoad(reg) + NL;
    }


    private String generateBinaryOp(BinaryOpInstruction binaryOp) {

        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(apply(binaryOp.getLeftOperand()));
        code.append(apply(binaryOp.getRightOperand()));


        // apply operation
        var binCode = switch (binaryOp.getOperation().getOpType()) {
            case ADD, MUL, SUB, DIV, REM -> this.generateArithmeticOps(binaryOp.getOperation());
            case LTH, LTE, GTH, GTE, EQ, NEQ -> this.generateComparisonOps(binaryOp.getOperation().getOpType());
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };


        code.append(binCode + NL);
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

    private String generateComparisonOps(OperationType operationType) {
        String ifId = types.getIfInstructionCode(operationType);
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


    private String generateNewClass(NewInstruction newInst) {
        var code = new StringBuilder();

        ClassType classType = (ClassType) newInst.getReturnType();
        String qualifiedName = types.getQualifiedNameFromClass(classType.getName());
        code.append("new ").append(qualifiedName).append(NL);

        // needs to dup for store
        if (isInsideAssignment) {
            code.append("dup" + NL);
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
    private String generateInvokeVirtualInstruction (InvokeVirtualInstruction invokeVirtualInst) {
        StringBuilder code = new StringBuilder();
        String initClassName = "java/lang/Object";
        Element caller = invokeVirtualInst.getCaller();
        code.append(apply(caller));
        if (caller.getType() instanceof ClassType classType) {
            initClassName = types.getQualifiedNameFromClass(classType.getName());
        }

        StringBuilder argtypes=new StringBuilder();
        // load arguments
        invokeVirtualInst.getArguments().forEach(element ->{argtypes.append(types.getTypeDescriptor(element.getType()));
            code.append(this.apply(element));});
        var methodnamenode = invokeVirtualInst.getMethodName().toString().split(":");
        var methodname = methodnamenode[methodnamenode.length-1].split("\\.")[0].trim();
        code.append("invokevirtual"+Space).append(initClassName).append("/"+methodname).append("(").append(argtypes).append(")").append(types.getTypeDescriptor(invokeVirtualInst.getReturnType())).append(NL);

        return code.toString();
    }
    private String generateInvokeStaticInstruction(InvokeStaticInstruction invokeStaticInst){
        StringBuilder code=new StringBuilder();
        String initClassName = "java/lang/Object";
        Element caller = invokeStaticInst.getCaller();
        if (caller.getType() instanceof ClassType classType) {
            initClassName = types.getQualifiedNameFromClass(classType.getName());
        }

        StringBuilder argtypes=new StringBuilder();
        // load arguments
        invokeStaticInst.getArguments().forEach(element ->{argtypes.append(types.getTypeDescriptor(element.getType()));
            code.append(this.apply(element));});
        var methodnamenode = invokeStaticInst.getMethodName().toString().split(":");
        var methodname = methodnamenode[methodnamenode.length-1].split("\\.")[0].trim();
        code.append("invokestatic"+Space).append(initClassName).append("/"+methodname).append("(").append(argtypes).append(")").append(types.getTypeDescriptor(invokeStaticInst.getReturnType())).append(NL);
        return code .toString();
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
}