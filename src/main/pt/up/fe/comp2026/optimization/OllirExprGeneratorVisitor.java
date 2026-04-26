package pt.up.fe.comp2026.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.type.JmmType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmArrayType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmClassType;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2026.ast.AccessType;
import pt.up.fe.comp2026.ast.TypeUtils;
import pt.up.fe.comp2026.jmm.ast.JmmAttributes;
import pt.up.fe.comp2026.jmm.ast.JmmKind;

import java.util.stream.Collectors;

import static pt.up.fe.comp2026.jmm.ast.JmmKind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends AJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

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
        addVisit(CLASS_INITIALIZATION, this::visitClassInit);
        addVisit(NEW_ARRAY,this::visitNewArray);
        addVisit(ARRAY_INIT,this::visitNewArray);
        addVisit(ARRAY_ACCESS, this::visitArrayAccess);
        //addVisit(EXPLICIT_METHOD_CALL,this::visitMethodCall);
        addVisit(CLASS_INITIALIZATION,this::visitClassInit);
        addVisit(THIS, this::visitThisExpr);
    }

    private OllirExprResult visitNewArray(JmmNode node,Void unused){
        StringBuilder comp=new StringBuilder();
        StringBuilder dims=new StringBuilder();
        JmmType type=types.getExprType(node);
        int size;
        String t=ollirTypes.toOllirType(type);

        if (node.getKind() == ARRAY_INIT) {
            var elements = node.getDescendants().stream().skip(2).filter(n -> n.getKind().equals(ARRAY_INITIALIZER))
                    .map(n -> n.getChildren(ARRAY_ELEMENT).size())
                    .toList();
            for(int i=1; i<=elements.size();i++){
                dims.append(i).append(".i32");
                if (i <elements.size()) {
                    dims.append(", ");
                }
            }

        } else {
            size = node.getObjectAsList("size").size();
            for(int i=1; i<=size;i++){
                JmmNode child=node.getChild(i);
                OllirExprResult res = visit(child);
                comp.append(res.getComputation());
                dims.append(res.getCode());
                if (i <size) {
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

        String elementOllirType = ollirTypes.toOllirType(types.getExprType(jmmNode));
        String code = arrayCode + '[' + indexCode + ']' + elementOllirType;
        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitThisExpr(JmmNode node,Void unused){
        String code;
        code = "this."+node.getAncestor(CLASS_DECL).get().get("name");
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

        var lhs = visit(node.getChild(0));
        var rhs = visit(node.getChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        JmmType resType = types.getExprType(node);
        String resOllirType = ollirTypes.toOllirType(resType);
        String code = ollirTypes.nextTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        JmmType type = types.getExprType(node);
        computation.append(op).append(ollirTypes.toOllirType(type)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        var id = ollirTypes.sanitizeId(node.get("name"));
        JmmType type = types.getExprType(node);
        String ollirType = ollirTypes.toOllirType(type);
        var code = new StringBuilder();

        // fields need pre computation
        if(table.getField(id).isPresent()){
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
        String temp = ollirTypes.nextTemp()+ "." + className;

        //assign object to temp
        computation.append(temp + SPACE + ASSIGN + "." + className + " new(%s).%s".formatted(className, className) + END_STMT);

        //call object init
        computation.append("invokespecial(" + temp + ", " +  "\"<init>\").V" + END_STMT);

        return new OllirExprResult(temp, computation.toString());
    }

}
