package pt.up.fe.comp2026.optimization;

import pt.up.fe.comp.jmm.analysis.table.MethodSymbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.type.JmmType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmArrayType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmClassType;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2026.ast.TypeUtils;

import java.util.Arrays;
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
        addVisit(EQUALITY_EXPR, this::visitBinExpr);
        addVisit(LOGICAL_NOT, this::visitNot);
        addVisit(NEW_ARRAY, this::visitNewArray);
        addVisit(ARRAY_ACCESS, this::visitArrayAccess);
        addVisit(LENGTH, this::visitLength);
        addVisit(EXPLICIT_METHOD_CALL, this::visitMethodCall);
        addVisit(IMPLICIT_METHOD_CALL, this::visitMethodCall);
        addVisit(CLASS_INITIALIZATION, this::visitClassInit);
        addVisit(THIS, this::visitThisExpr);
        addVisit(PARENTESES, this::visitParenteses);
        addVisit(RIGHT_UNARY_EXPR, this::visitRightUnary);
        addVisit(LEFT_UNARY_EXPR, this::visitLeftUnary);
    }
    private boolean isfield(JmmNode node){
        var id="";
        var name="";
        if(node.getKind()!=VAR_REF_EXPR){
            id= ollirTypes.sanitizeId(node.getChild(0).get("name"));
            name=node.getChild(0).get("name");
        }
        else{id=ollirTypes.sanitizeId(node.get("name")); name = node.get("name");}
        boolean isparam=false, islocal=false;
        if (node.getAncestor(METHOD_DECL).isPresent()) {

            JmmNode method = node.getAncestor(METHOD_DECL).get();
            String finalName = name;
            isparam = method.getChildren(PARAM).stream().anyMatch(param -> param.get("name").equals(finalName));
            islocal = method.getChildren(VAR_DECL).stream().anyMatch(param -> param.get("name").equals(finalName));
        }
        if (table.getField(id).isPresent() && !islocal && !isparam) {

            return true;
        }
        return false;
    }

    private OllirExprResult visitLeftUnary(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        String op = node.get("op").equals("++") ? "+" : "-";
        String type = ollirTypes.toOllirType(types.getExprType(node.getChild(0)));
        JmmNode child = node.getChild(0);
        var ls = visit(child);
        computation.append(ls.getComputation());
        computation.append(ls.getCode())
                .append(SPACE).append(ASSIGN).append(type)
                .append(SPACE).append(ls.getCode())
                .append(SPACE).append(op)
                .append(type).append(SPACE)
                .append("1.i32").append(END_STMT);
        if(child.getKind()==ARRAY_ACCESS){
            String cleanComp = ls.getComputation().trim();
            if (cleanComp.endsWith(";")) {
                cleanComp = cleanComp.substring(0, cleanComp.length() - 1);
            }
            String[] parts = cleanComp.split(" ");
            String arrayTarget = parts[parts.length - 1];

            computation.append(arrayTarget)
                    .append(SPACE).append(ASSIGN).append(type)
                    .append(SPACE).append(ls.getCode())
                    .append(END_STMT);
        }
        if(this.isfield(child)){
            var id =child.getKind()!=VAR_REF_EXPR?ollirTypes.sanitizeId(child.getChild(0).get("name")):ollirTypes.sanitizeId(child.get("name"));
            var olltype = ollirTypes.toOllirType(types.getExprType(child));
            computation.append("putfield(this,").append(id+olltype).append(",").append(ls.getCode()).append(")").append(".V").append(END_STMT);
        }
        return new OllirExprResult(ls.getCode(), computation);
    }

    private OllirExprResult visitRightUnary(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        String op = node.get("op").equals("++") ? "+" : "-";
        String type = ollirTypes.toOllirType(types.getExprType(node.getChild(0)));
        JmmNode child = node.getChild(0);
        var ls = visit(child);
        String temp = ollirTypes.nextTemp() + type;
        computation.append(ls.getComputation());
        computation.append(temp).append(SPACE).append(ASSIGN).append(type).append(SPACE).append(ls.getCode()).append(END_STMT);
        computation.append(ls.getCode()).append(SPACE).append(ASSIGN).append(type).append(SPACE).append(ls.getCode()).append(SPACE).append(op).append(type).append(SPACE).append("1.i32").append(END_STMT);
        if(child.getKind()==ARRAY_ACCESS){
            String cleanComp = ls.getComputation().trim();
            if (cleanComp.endsWith(";")) {
                cleanComp = cleanComp.substring(0, cleanComp.length() - 1);
            }
            String[] parts = cleanComp.split(" ");
            String arrayTarget = parts[parts.length - 1];

            computation.append(arrayTarget)
                    .append(SPACE).append(ASSIGN).append(type)
                    .append(SPACE).append(ls.getCode())
                    .append(END_STMT);
        }
        if(this.isfield(child)){
            var id =child.getKind()!=VAR_REF_EXPR?ollirTypes.sanitizeId(child.getChild(0).get("name")):ollirTypes.sanitizeId(child.get("name"));
            var olltype = ollirTypes.toOllirType(types.getExprType(child));
            computation.append("putfield(this,").append(id+olltype).append(",").append(ls.getCode()).append(")").append(".V").append(END_STMT);
        }
        return new OllirExprResult(temp, computation);

    }

    private OllirExprResult visitNot(JmmNode node, Void unused) {

        StringBuilder computation = new StringBuilder();
        OllirExprResult res = visit(node.getChild(0));
        computation.append(res.getComputation());
        var resollirtype = ollirTypes.toOllirType(types.getExprType(node));
        String code = ollirTypes.nextTemp() + resollirtype;
        computation.append(code);
        computation.append(SPACE).append(ASSIGN).append(".bool").append(SPACE).append("!.bool").append(SPACE).append(res.getCode()).append(END_STMT);
        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitParenteses(JmmNode node, Void unused) {
        return this.visit(node.getChild(0));
    }

    private OllirExprResult visitMethodCall(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        String ollirType = ollirTypes.toOllirType(types.getExprType(node));
        String methodname = node.get("methodname");
        JmmNode parent = node.getParent();
        int skip = node.getKind() == IMPLICIT_METHOD_CALL ? 0 : 1;
        List<OllirExprResult> res = node.getChildren().stream().skip(skip).map(this::visit).toList();
        MethodSymbol method = types.getExplicitMethodCallSymbol(node);
        List<String> parameters = method.parameters().stream().map(s -> ollirTypes.toOllirType(s.type())).toList();
        if (node.getKind() == EXPLICIT_METHOD_CALL) {
            JmmNode calle = node.getChild(0);
            JmmClassType classtype = types.getExprType(calle).asClass();
            OllirExprResult calleResult = this.visit(calle);
            computation.append(calleResult.getComputation());
            if (method.isStatic()) {
                code.append("invokestatic(").append(classtype.name());
            } else {
                code.append("invokevirtual(").append(calleResult.getCode());
            }
        } else {
            if (method.isStatic()) {
                code.append("invokestatic(");
            } else {
                code.append("invokevirtual(");
            }
            code.append("this.").append(table.getClassName());
        }

        code.append(SPACE).append(",").append(DOUBLE_Quote).append(methodname).append(DOUBLE_Quote);
        for (int i = 0; i < res.size(); i++) {
            String actualT = parameters.get(i);
            StringBuilder adtComp = new StringBuilder();
            OllirExprResult r = res.get(i);
            List<String> code2 = Arrays.stream(r.getCode().split("\\.")).toList();
            String passedT = code2.getLast();
            if (!passedT.equals(actualT)) {
                String tempVar = ollirTypes.nextTemp() + actualT;
                adtComp.append(tempVar).append(SPACE).append(ASSIGN).append(actualT).append(SPACE).append(code2.getFirst()).append(".").append(code2.getLast()).append(END_STMT);
                System.out.println("this is the actual Type:" + passedT + "expected type" + actualT);
                code.append(",").append(SPACE).append(tempVar);
            } else {
                code.append(",").append(SPACE).append(r.getCode());
            }
            computation.append(r.getComputation());
            computation.append(adtComp);

        }
        code.append(")").append(ollirType);
        if (parent.getKind() == EXPRESSION) {
            code.append(END_STMT);
            return new OllirExprResult(code.toString(), computation);
        }
        String tempVar = ollirTypes.nextTemp() + ollirType;
        computation.append(tempVar).append(SPACE).append(":=").append(ollirType).append(SPACE).append(code).append(END_STMT);
        return new OllirExprResult(tempVar, computation);
    }

    private OllirExprResult visitNewArray(JmmNode node, Void unused) {
        StringBuilder comp = new StringBuilder();
        StringBuilder dims = new StringBuilder();
        JmmType type = types.getExprType(node);
        String t = ollirTypes.toOllirType(type);
        node.getChildren().stream().skip(1).forEach(child -> {
            OllirExprResult res = visit(child);
            comp.append(res.getComputation());
            dims.append(res.getCode());
            if (child != node.getChildren().getLast()) {
                dims.append(", ");
            }
        });


        // passed new array inst to computation
        String temp = ollirTypes.nextTemp() + t;
        String newInst = "new(array, " + dims + ")" + t;
        comp.append(temp + SPACE + ASSIGN + t + SPACE + newInst + END_STMT);
        return new OllirExprResult(temp, comp);
    }

    private OllirExprResult visitArrayAccess(JmmNode jmmNode, Void unused) {
        var arrayExpr = visit(jmmNode.getChild(0));
        var indexExpr = jmmNode.getChildren().stream().skip(1).map(this::visit).toList();
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        String currentRef = arrayExpr.getCode();
        computation.append(arrayExpr.getComputation());
        JmmArrayType arrayType = types.getExprType(jmmNode.getChild(0)).asArray();
        var parent = jmmNode.getParent();
        boolean isAssignmentLHS = parent != null && parent.getKind() == ASSIGN_STMT && parent.getChild(0) == jmmNode;
        String temp;
        for (int i = 0; i < indexExpr.size(); i++) {
            OllirExprResult element = indexExpr.get(i);
            int dims = arrayType.dimension() - (i + 1);
            String type = dims == 0 ? ollirTypes.toOllirType(arrayType.itemType()) : ollirTypes.toOllirType(new JmmArrayType(arrayType.itemType(), dims));


            computation.append(element.getComputation());
            String current = currentRef + "[" + element.getCode() + "]" + type;
            boolean isLast = (i == indexExpr.size() - 1);
            if (isLast) {
                if (isAssignmentLHS) {
                    return new OllirExprResult(current, computation);
                } else {
                    temp = ollirTypes.nextTemp() + type;
                    computation.append(temp).append(ASSIGN).append(type).append(SPACE).append(current).append(END_STMT);
                    code.append(temp);
                    return new OllirExprResult(code.toString(), computation);
                }
            }
            temp = ollirTypes.nextTemp() + type;
            computation.append(temp).append(ASSIGN).append(type).append(SPACE).append(current).append(END_STMT);
            currentRef = temp;
        }
        return new OllirExprResult(code.toString(), computation);
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
        StringBuilder computation = new StringBuilder();
        String code;
        JmmType type = types.getExprType(node.getChild(0));
        switch (op) {
            case "&&" -> {
                JmmType resType = types.getExprType(node);
                String resOllirType = ollirTypes.toOllirType(resType);
                var lhs = visit(node.getChild(0));
                computation.append(lhs.getComputation());
                String false_label = ollirTypes.nextLabel("f");
                computation.append("if(!.bool ").append(lhs.getCode()).append(")").append(SPACE).append("goto").append(SPACE).append(false_label).append(END_STMT);
                code = ollirTypes.nextTemp() + resOllirType;
                String end_label = ollirTypes.nextLabel("s");
                var rhs = visit(node.getChild(1));
                computation.append(rhs.getComputation());
                computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE).append(rhs.getCode()).append(END_STMT).append("goto").append(SPACE).append(end_label).append(END_STMT);
                computation.append(false_label).append(":\n");
                computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE).append("0.bool").append(END_STMT).append(end_label).append(":\n");
            }
            case "||" -> {
                JmmType resType = types.getExprType(node);
                String resOllirType = ollirTypes.toOllirType(resType);
                var lhs = visit(node.getChild(0));
                computation.append(lhs.getComputation());
                String true_label = ollirTypes.nextLabel("t");
                String end_label = ollirTypes.nextLabel("s");
                computation.append("if(").append(lhs.getCode()).append(")").append(SPACE).append("goto").append(SPACE).append(true_label).append(END_STMT);
                code = ollirTypes.nextTemp() + resOllirType;
                var rhs = visit(node.getChild(1));
                computation.append(rhs.getComputation());
                computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE).append(rhs.getCode()).append(END_STMT).append("goto").append(SPACE).append(end_label).append(END_STMT);
                computation.append(true_label).append(":\n");
                computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE).append("1.bool").append(END_STMT).append(end_label).append(":\n");

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

                computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE).append(lhs.getCode()).append(SPACE);

                computation.append(op).append(ollirTypes.toOllirType(type)).append(SPACE).append(rhs.getCode()).append(END_STMT);

            }
        }


        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        var id = ollirTypes.sanitizeId(node.get("name"));
        JmmType type = types.getExprType(node);
        String ollirType = ollirTypes.toOllirType(type);
        var code = new StringBuilder();
        boolean islocal = false, isparam = false;
        if (node.getAncestor(METHOD_DECL).isPresent()) {
            var name = node.get("name");
            JmmNode method = node.getAncestor(METHOD_DECL).get();
            isparam = method.getChildren(PARAM).stream().anyMatch(param -> param.get("name").equals(name));
            islocal = method.getChildren(VAR_DECL).stream().anyMatch(param -> param.get("name").equals(name));
        }
        // fields need pre computation
        if (table.getField(id).isPresent() && !islocal && !isparam) {
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
        StringBuilder var = new StringBuilder();
        String className = classInit.get("name");
        String temp = ollirTypes.nextTemp() + "." + className;

        computation.append(temp).append(ASSIGN).append(".").append(className).append(" new(").append(className).append(").").append(className).append(END_STMT);
        List<JmmNode> children = classInit.getChildren();
        List<OllirExprResult> res = children.stream().map(this::visit).toList();
        for (int i = 0; i < res.size(); i++) {
            OllirExprResult child = res.get(i);
            computation.append(child.getComputation());
            var.append(",").append(SPACE);
            var.append(child.getCode());
        }

        computation.append("invokespecial(").append(temp).append(", ").append("\"<init>\"").append(var).append(")").append(".V").append(END_STMT);


        return new OllirExprResult(temp, computation);
    }

}
