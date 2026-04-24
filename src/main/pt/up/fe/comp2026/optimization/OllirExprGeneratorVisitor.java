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
        addVisit(CLASS_INITIALIZATION,this::visitClassInit);
        addVisit(THIS, this::visitThisExpr);
    }

    private OllirExprResult visitNewArray(JmmNode node,Void unused){
        //checked documentation have zero ideas on how to make this for multiarrays
        StringBuilder code=new StringBuilder();
        StringBuilder method=new StringBuilder();
        StringBuilder dims=new StringBuilder();
        JmmType type=types.getExprType(node);
        int size;
        String t=ollirTypes.toOllirType(type);

        if (node.getKind() == ARRAY_INIT) {
            size = node.getObjectAsList("dims").size();
            dims.append(size).append(".i32");
        } else {
            size = node.getObjectAsList("size").size();
            for(int i=1; i<=size;i++){
                JmmNode child=node.getChild(i);
                String value = ollirTypes.toOllirExpr(child, method);
                if(i!=size){
                    dims.append(value).append(',');
                }
                else {
                    dims.append(value);
                }

            }
        }
        System.out.println(method);
        code.append(" new(array,").append(dims).append(")").append(t);
        System.out.println(this + code.toString());
        return new OllirExprResult(code.toString());
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
        if(table.getField(id).isPresent()){
            code.append("getfield(").append("this,").append(id).append(ollirType).append(")").append(ollirType);
        }
        else {
            code.append(id).append(ollirType);
        }

        return new OllirExprResult(code.toString());

    }

    private OllirExprResult visitBoolLiteral(JmmNode node, Void unused) {
        if (node.get("bool").equals("true")) {
            return new OllirExprResult("1.bool");
        }

        return new OllirExprResult("0.bool");
    }


//new(myClass).myClass
    private OllirExprResult visitClassInit(JmmNode classInit, Void unused) {
        StringBuilder code = new StringBuilder();
        String className = classInit.get(JmmAttributes.CLASS_INITIALIZATION.NAME);
        code.append("new(%s).%s".formatted(className, className));
        return new OllirExprResult(code.toString());
    }


}
