package pt.up.fe.comp2026.optimization;

import pt.up.fe.comp.jmm.analysis.table.MethodSymbol;
import pt.up.fe.comp.jmm.analysis.table.Signature;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.type.JmmType;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2026.ast.TypeUtils;
import pt.up.fe.comp2026.jmm.ast.JmmAttributes;

import java.util.List;

import static pt.up.fe.comp2026.jmm.ast.JmmKind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends AJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String DOUBLE_Quote = "\"";

    private final SymbolTable table;

    private final TypeUtils types;
    private final OptUtils ollirTypes;


    public OllirExprGeneratorVisitor(SymbolTable table, OptUtils ollirTypes) {
        this.table = table;
        this.types = new TypeUtils(table);
        this.ollirTypes = ollirTypes; // We need to pass ollirTypes, to ensure labels are unique
    }


    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(BOOL_LITERAL, this::visitBoolLiteral);
        addVisit(LOGICAL_EXPR, this::visitBinExpr);
        addVisit(EQUALITY_EXPR,this::visitBinExpr);
        addVisit(LOGICAL_NOT,this::visitNot);
        addVisit(CLASS_INITIALIZATION, this::visitClassInit);
        addVisit(NEW_ARRAY, this::visitNewArray);
        addVisit(ARRAY_INIT, this::visitNewArray);
        addVisit(ARRAY_ACCESS, this::visitArrayAccess);
        addVisit(LENGTH, this::visitLength);
        addVisit(EXPLICIT_METHOD_CALL, this::visitMethodCall);
        addVisit(CLASS_INITIALIZATION, this::visitClassInit);
        addVisit(THIS, this::visitThisExpr);
        addVisit(PARENTESES, this::visitParenteses);
    }
    private OllirExprResult visitNot(JmmNode node, Void unused){

        StringBuilder computation=new StringBuilder();
        OllirExprResult res=visit(node.getChild(0));
        computation.append(res.getComputation());
        var resollirtype=ollirTypes.toOllirType(types.getExprType(node));
        String code=ollirTypes.nextTemp()+resollirtype;
        computation.append(code);
        computation.append(SPACE).append(ASSIGN).append(".bool").append(SPACE).append("!.bool").append(SPACE).append(res.getCode()).append(END_STMT);
        return new OllirExprResult(code,computation);
    }
    private OllirExprResult visitParenteses(JmmNode node, Void unused) {
        return this.visit(node.getChild(0));
    }

    private OllirExprResult visitMethodCall(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        JmmNode calle = node.getChild(0);
        OllirExprResult calleResult = this.visit(calle);
        computation.append(calleResult.getComputation());
        String ollirType = ollirTypes.toOllirType(types.getExprType(node));
        String methodname = node.get("methodname");
        JmmNode parent = node.getParent();
        List<OllirExprResult> res = node.getChildren().stream().skip(1).map(this::visit).toList();

        var argTypes = node.getChildren().stream().skip(1)
                .map(arg -> types.getExprType(arg))
                .toList();

        Signature methodSignature = new Signature(methodname, argTypes);
        MethodSymbol methodSymbol = table.getMethod(methodSignature).get();
        if (methodSymbol.isStatic()) {
            code.append("invokestatic").append(calle.get("name"));
        } else {
            code.append("invokevirtual(").append(calleResult.getCode());
        }
        code.append(SPACE).append(",").append(DOUBLE_Quote).append(methodname).append(DOUBLE_Quote);
        for (int i = 0; i < res.size(); i++) {
            OllirExprResult r = res.get(i);
            computation.append(r.getComputation());
            code.append(",").append(SPACE).append(r.getCode());
        }
        code.append(")").append(ollirType);
        if (parent.getKind() == EXPRESSION) {
            code.append(END_STMT);
            return new OllirExprResult(code.toString(), computation);
        }
        String tempVar = ollirTypes.nextTemp() + ollirType;
        computation.append(tempVar).append(SPACE).append(":=").append(ollirType)
                .append(SPACE).append(code).append(END_STMT);
        return new OllirExprResult(tempVar, computation);
    }

    private OllirExprResult visitNewArray(JmmNode node, Void unused) {
        StringBuilder comp = new StringBuilder();
        StringBuilder dims = new StringBuilder();
        JmmType type = types.getExprType(node);
        int size;
        String t = ollirTypes.toOllirType(type);

        if (node.getKind() == ARRAY_INIT) {
            var elements = node.getDescendants().stream().skip(2).filter(n -> n.getKind().equals(ARRAY_INITIALIZER))
                    .map(n -> n.getChildren(ARRAY_ELEMENT).size())
                    .toList();
            for (int i = 1; i <= elements.size(); i++) {
                dims.append(i).append(".i32");
                if (i < elements.size()) {
                    dims.append(", ");
                }
            }

        } else {
            size = node.getObjectAsList("size").size();
            for (int i = 1; i <= size; i++) {
                JmmNode child = node.getChild(i);
                OllirExprResult res = visit(child);
                comp.append(res.getComputation());
                dims.append(res.getCode());
                if (i < size) {
                    dims.append(", ");
                }
            }
        }
        String code = "new(array, " + dims + ")" + t;
        return new OllirExprResult(code, comp);

    }

    private OllirExprResult visitArrayAccess(JmmNode jmmNode, Void unused) {
        var arrayExpr = visit(jmmNode.getChild(0));
        var indexExpr = visit(jmmNode.getChild(1));

        StringBuilder computation = new StringBuilder();
        computation.append(arrayExpr.getComputation());
        computation.append(indexExpr.getComputation());

        String arrayCode = arrayExpr.getCode();
        String indexCode = indexExpr.getCode();

        if (indexCode.contains("[")) {
            String temp = ollirTypes.nextTemp() + ".i32";
            computation.append(temp).append(" :=.i32 ").append(indexCode).append(END_STMT);
            indexCode = temp;
        }

        if (arrayCode.contains("[")) {
            JmmType arrayType = types.getExprType(jmmNode.getChild(0));
            String arrayOllirType = ollirTypes.toOllirType(arrayType);
            String temp = ollirTypes.nextTemp() + arrayOllirType;
            computation.append(temp).append(" :=").append(arrayOllirType).append(" ").append(arrayCode).append(END_STMT);
            arrayCode = temp;
        }

        var parent = jmmNode.getParent();
        boolean isAssignmentLHS = parent != null && parent.getKind() == ASSIGN_STMT && parent.getChild(0) == jmmNode;

        String elementOllirType = ollirTypes.toOllirType(types.getExprType(jmmNode));
        String code = arrayCode + '[' + indexCode + ']' + elementOllirType;

        if (isAssignmentLHS) {
            return new OllirExprResult(code, computation);
        } else {
            String temp = ollirTypes.nextTemp() + elementOllirType;
            computation.append(temp).append(SPACE).append(ASSIGN).append(elementOllirType).append(SPACE)
                    .append(code).append(END_STMT);
            return new OllirExprResult(temp, computation);
        }
    }

    private OllirExprResult visitLength(JmmNode node, Void unused) {
        var arrayExpr = visit(node.getChild(0));
        StringBuilder computation = new StringBuilder();
        computation.append(arrayExpr.getComputation());

        String arrayCode = arrayExpr.getCode();
        if (arrayCode.contains("[")) {
            JmmType arrayType = types.getExprType(node.getChild(0));
            String arrayOllirType = ollirTypes.toOllirType(arrayType);
            String temp = ollirTypes.nextTemp() + arrayOllirType;
            computation.append(temp).append(" :=").append(arrayOllirType).append(" ").append(arrayCode).append(END_STMT);
            arrayCode = temp;
        }

        String temp = ollirTypes.nextTemp() + ".i32";
        computation.append(temp).append(" :=.i32 ").append("arraylength(").append(arrayCode).append(").i32").append(END_STMT);

        return new OllirExprResult(temp, computation);
    }

    private OllirExprResult visitThisExpr(JmmNode node, Void unused) {
        String code;
        code = "this." + node.getAncestor(CLASS_DECL).get().get("name");
        System.out.println(code);
        return new OllirExprResult(code);
    }

    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = TypeUtils.intType();
        String ollirIntType = ollirTypes.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {
        var op = node.get("op");
        StringBuilder computation= new StringBuilder();
        String code;
        JmmType type = types.getExprType(node.getChild(0));
        switch (op){
            case "&&" ->{
                JmmType resType = types.getExprType(node);
                String resOllirType = ollirTypes.toOllirType(resType);
                var lhs = visit(node.getChild(0));
                computation.append(lhs.getComputation());
                String false_label= ollirTypes.nextLabel("f");
                computation.append("if(!.bool ").append(lhs.getCode()).append(")").append(SPACE).append("goto").append(SPACE).append(false_label).append(END_STMT);
                code = ollirTypes.nextTemp() + resOllirType;
                String end_label=ollirTypes.nextLabel("s");
                var rhs = visit(node.getChild(1));
                computation.append(rhs.getComputation());
                computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType)
                        .append(SPACE).append(rhs.getCode()).append(END_STMT).append("goto").append(SPACE).append(end_label).append(END_STMT);
                computation.append(false_label).append(":\n");
                computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType)
                        .append(SPACE).append("0.bool").append(END_STMT).append(end_label).append(":\n");
            }
            case "||"->{
                JmmType resType = types.getExprType(node);
                String resOllirType = ollirTypes.toOllirType(resType);
                var lhs = visit(node.getChild(0));
                computation.append(lhs.getComputation());
                String true_label= ollirTypes.nextLabel("t");
                String end_label=ollirTypes.nextLabel("s");
                computation.append("if(").append(lhs.getCode()).append(")").append(SPACE).append("goto").append(SPACE).append(true_label).append(END_STMT);
                code = ollirTypes.nextTemp() + resOllirType;
                var rhs = visit(node.getChild(1));
                computation.append(rhs.getComputation());
                computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType)
                        .append(SPACE).append(rhs.getCode()).append(END_STMT).append("goto").append(SPACE).append(end_label).append(END_STMT);
                computation.append(true_label).append(":\n");
                computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType)
                        .append(SPACE).append("1.bool").append(END_STMT).append(end_label).append(":\n");

            }
            default -> {
                var lhs = visit(node.getChild(0));
                var rhs = visit(node.getChild(1));
                // code to compute the children
                computation.append(lhs.getComputation());
                computation.append(rhs.getComputation());

                // code to compute self
                JmmType resType = types.getExprType(node);
                String resOllirType = ollirTypes.toOllirType(resType);
                code = ollirTypes.nextTemp() + resOllirType;

                computation.append(code).append(SPACE)
                        .append(ASSIGN).append(resOllirType).append(SPACE)
                        .append(lhs.getCode()).append(SPACE);

                computation.append(op).append(ollirTypes.toOllirType(type)).append(SPACE)
                        .append(rhs.getCode()).append(END_STMT);

            }
        }


        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        var id = ollirTypes.sanitizeId(node.get("name"));
        JmmType type = types.getExprType(node);
        String ollirType = ollirTypes.toOllirType(type);
        var code = new StringBuilder();

        // fields need pre computation
        if (table.getField(id).isPresent()) {
            String temp = ollirTypes.nextTemp() + ollirType;
            String computation = temp + ASSIGN + ollirType + " getfield(this," + id + ollirType + ")" + ollirType + END_STMT;
            return new OllirExprResult(temp, computation);
        }

        code.append(id).append(ollirType);
        return new OllirExprResult(code.toString());

    }

    private OllirExprResult visitBoolLiteral(JmmNode node, Void unused) {
        if (node.get("bool").equals("true")) {
            return new OllirExprResult("1.bool");
        }

        return new OllirExprResult("0.bool");
    }


    // temp = new(myClass).myClass
    // invoke(temp, init);
    private OllirExprResult visitClassInit(JmmNode classInit, Void unused) {
        StringBuilder computation = new StringBuilder();
        String className = classInit.get(JmmAttributes.CLASS_INITIALIZATION.NAME);
        String temp = ollirTypes.nextTemp() + "." + className;

        //assign object to temp
        computation.append(temp + SPACE + ASSIGN + "." + className + " new(%s).%s".formatted(className, className) + END_STMT);

        //call object init
        computation.append("invokespecial(" + temp + ", " + "\"<init>\").V" + END_STMT);

        return new OllirExprResult(temp, computation.toString());
    }

}
