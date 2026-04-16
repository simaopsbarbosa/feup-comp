package pt.up.fe.comp2026.optimization;

import pt.up.fe.comp.jmm.analysis.table.MethodSymbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.type.JmmType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmArrayType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmPrimitiveType;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.utils.Attributes;
import pt.up.fe.comp2026.ast.AccessType;
import pt.up.fe.comp2026.ast.NodeUtils;
import pt.up.fe.comp2026.ast.TypeUtils;
import pt.up.fe.comp2026.jmm.ast.JmmAttributes;
import pt.up.fe.comp2026.jmm.ast.JmmKind;
import pt.up.fe.specs.util.utilities.StringLines;
import pt.up.fe.specs.util.utilities.Table;

import java.util.stream.Collectors;

import static pt.up.fe.comp.jmm.analysis.table.type.impls.JmmPrimitiveType.VOID;
import static pt.up.fe.comp2026.jmm.ast.JmmKind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";


    private final SymbolTable table;

    private final TypeUtils types;
    private final OptUtils ollirTypes;


    private final OllirExprGeneratorVisitor exprVisitor;

    private MethodSymbol currentMethod;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        this.types = new TypeUtils(table);
        this.ollirTypes = new OptUtils(types);
        exprVisitor = new OllirExprGeneratorVisitor(table, ollirTypes);
        currentMethod = null;
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(PACKAGE_DECL, this::visitPackageDecl);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(PARAM, this::visitParam);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        //addVisit(ASSIGNMENT, this::visitAssignStmt);
        addVisit(ELSEIF_BLOCK_STMT, this::visitIfStmt);
        addVisit(BLOCK, this::visitBlockStmt);
        addVisit(FIELD_DECL,this::FieldDecl);
        addVisit(WHILE_STMT, this::visitWhileStmt);
        setDefaultVisit(this::defaultVisit);
    }


    private String FieldDecl(JmmNode node, Void unused) {
        var visibility= node.getOptional("visibility").isPresent()? node.getOptional("visibility").get(): "public";
        return ".field "+visibility+" "+node.getChild(0).get("name") + ollirTypes.toOllirType(node.getChild(0).getObject("typeNode", JmmNode.class)) + ";";
    }


    private String visitParam(JmmNode varDecl, Void unused) {
        return varDecl.get("name") + ollirTypes.toOllirType(varDecl.getObject("typeNode", JmmNode.class));
    }



    private String visitPackageDecl(JmmNode packageDecl, Void unused) {
        return "package " + String.join(".", packageDecl.getObjectAsList("path", String.class)) + ";\n";
    }

    private String visitWhileStmt(JmmNode whileStmt, Void unused) {
        StringBuilder code = new StringBuilder();
        String loopBeginLabel = ollirTypes.nextLabel("loop_begin");
        String loopLabel = ollirTypes.nextLabel("loop_body");
        String exitLabel = ollirTypes.nextLabel("loop_exit");

        code.append(loopBeginLabel + ":" + NL);

        //handle condition
        JmmNode condNode = whileStmt.getChildren(EXPR).get(0);
        String cond = exprVisitor.visit(condNode).getCode();
        code.append("   if(" + cond + ") goto " + loopLabel + END_STMT);

        code.append("   " + "goto " + exitLabel + END_STMT);

        //handles loop stmts:
        code.append("   " + loopLabel + ":" + NL);
        code.append(visit(whileStmt.getChildren(STMT).getFirst()));
        code.append("   " + "goto " + loopBeginLabel + END_STMT);

        code.append("   " + exitLabel + ":" + NL);

        return code.toString();
    }



    //TODO: Spaces are weird
    private String visitIfStmt(JmmNode ifStmt, Void unused) {
        StringBuilder code = new StringBuilder();
        String trueLabel = ollirTypes.nextLabel("if_true");
        String exitLabel = ollirTypes.nextLabel("if_exit");

        JmmNode condNode = ifStmt.getChildren(EXPR).get(0);
        String cond = exprVisitor.visit(condNode).getCode();
        code.append("if(" + cond + ") goto " + trueLabel + END_STMT);

        //handle else stmt
        if (ifStmt.getChildren(STMT).size() > 1) {
            JmmNode elseStmt = ifStmt.getChildren(STMT).getLast();
            String elseCodes = visit(elseStmt);
            code.append(elseCodes);
        }
        code.append("   " + "goto " + exitLabel + END_STMT);

        //handles true stmts:
        code.append("   " + trueLabel + ":" + NL);
        JmmNode trueStmt = ifStmt.getChildren(STMT).getFirst();
        String trueCodes = visit(trueStmt);
        code.append(trueCodes);

        code.append("   " + exitLabel + ":" + NL);

        return code.toString();
    }


    private String visitBlockStmt(JmmNode blockStmt, Void unused) {
        //visit nested stmts
        return blockStmt.getChildren(STMT).stream().map(this::visit).collect(Collectors.joining("\n   ", "   ", ""));
    }



    private String visitAssignStmt(JmmNode assignStmtNode, Void unused) {
        // TODO: Several hard-coded things, should be rewritten
        JmmNode node = assignStmtNode.getChild(0);

        System.out.println(node);
        var rhs = exprVisitor.visit(node.getChild(1));

        // code to compute self
        // statement has type of lhs

        var varName = node.getDescendants(VAR_REF_EXPR).getFirst().get("name");
        JmmType thisType = TypeUtils.with(table).getExprType(node.getChild(0));
        String typeString = ollirTypes.toOllirType(thisType);
        var varCode = ollirTypes.sanitizeId(varName) + typeString;


        var code = new StringBuilder();

        // code to compute the children
        code.append(rhs.getComputation());

        code.append(varCode);
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(rhs.getCode());

        code.append(END_STMT);
        return code.toString();
    }

    private String visitReturn(JmmNode node, Void unused) {
        JmmType retType = node.getChildren().isEmpty() ? VOID : TypeUtils.with(table).getExprType(node.getChild(0));
        OllirExprResult expr=null;
        StringBuilder code = new StringBuilder();
        if (node.getChildren().isEmpty()) {
            code.append("ret");
            code.append(ollirTypes.toOllirType(retType));
            code.append(SPACE);
            code.append(END_STMT);
            return code.toString();
        }
        expr = exprVisitor.visit(node.getChild(0));
        code.append(expr.getComputation());
        code.append("ret");
        code.append(ollirTypes.toOllirType(retType));
        code.append(SPACE);
        code.append(expr.getCode());
        code.append(END_STMT);
        return code.toString();
    }

    private String visitMethodDecl(JmmNode node, Void unused) {

        currentMethod = table.getMethod(TypeUtils.with(table).getMethodDeclSignature(node)).orElseThrow();

        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");

        if (isPublic) {
            code.append("public ");
        }

        if (node.getObject("isStatic", Boolean.class)) {
            code.append("static ");
        }

        // name
        var name = ollirTypes.sanitizeId(node.get("name"));
        code.append(name);

        // params
        var paramsCode = node.getChildren(PARAM).stream().map(this::visit).collect(Collectors.joining(", ", " (", ")"));;
        code.append(paramsCode);

        // type
        var retType = ollirTypes.toOllirType(currentMethod.returnType());//table.getReturnType(node.get("name")));
        code.append(retType);
        code.append(L_BRACKET);

        var stmts = node.getChildren(STMT);
        if (!stmts.isEmpty()) {
            // rest of its children stmts
            var stmtsCode = stmts.stream().map(this::visit).collect(Collectors.joining("\n   ", "   ", ""));
            code.append(stmtsCode);
        }

        code.append(R_BRACKET);
        code.append(NL);

        currentMethod = null;

        return code.toString();
    }

    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(NL);
        code.append(table.getClassName());

        code.append(L_BRACKET);
        code.append(NL);
        code.append(NL);
        for(var child:node.getChildren(FIELD_DECL)){
            var result=visit(child);
            code.append(result);
        }
        code.append(NL);

        code.append(buildConstructor());
        code.append(NL);

        for (var child : node.getChildren(METHOD_DECL)) {
            var result = visit(child);
            code.append(result);
        }

        code.append(R_BRACKET);

        return code.toString();
    }

    private String buildConstructor() {
        return """
                .construct %s().V {
                    invokespecial(this, "<init>").V;
                }
                """.formatted(table.getClassName());
    }

    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        node.getChildren().stream().map(this::visit).forEach(code::append);

        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {
        System.out.println("default visited " + node + "----------------------------------------------");
        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
