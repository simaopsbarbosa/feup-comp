package pt.up.fe.comp2026.optimization;

import org.apache.commons.lang3.StringUtils;
import pt.up.fe.comp.jmm.analysis.table.MethodSymbol;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        addVisit(ELSEIF_BLOCK_STMT, this::visitIfStmt);
        addVisit(BLOCK, this::visitBlockStmt);
        addVisit(FIELD_DECL,this::FieldDecl);
        addVisit(WHILE_STMT, this::visitWhileStmt);
        addVisit(VAR_DECL, this::simpleVarDecl);
        setDefaultVisit(this::defaultVisit);
    }

    private String FieldDecl(JmmNode node, Void unused) {
        var visibility= node.getOptional("visibility").isPresent()? node.getOptional("visibility").get(): "public";
        return ".field "+visibility+" "+node.getChild(0).get("name") + ollirTypes.toOllirType(node.getChild(0).getObject("typeNode", JmmNode.class)) + ";";
    }


    private String visitParam(JmmNode varDecl, Void unused) {
        return ollirTypes.sanitizeId(varDecl.get("name")) + ollirTypes.toOllirType(varDecl.getObject("typeNode", JmmNode.class));
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


    private String visitAssignment(JmmNode node, String varName, JmmType thisType, JmmNode AssigneeExpr) {
        var rhs = exprVisitor.visit(AssigneeExpr);

        var varCode = "";
        var code = new StringBuilder();

        String typeString = ollirTypes.toOllirType(thisType);
        if (table.getField(varName).isPresent()) {
            varCode=ollirTypes.sanitizeId(varName) + typeString;
            code.append(rhs.getComputation());
            code.append("putfield(this,");
            code.append(varCode).append(",").append(rhs.getCode()).append(")").append(".V").append(END_STMT);
        } else {
            varCode = ollirTypes.sanitizeId(varName) + typeString;
            String computation = rhs.getComputation();
            String tempCode = rhs.getCode();

            // visitBinExpr always adds a temp, so try to replace it with real var name (not sure if the correct way)
            if (!computation.isEmpty() && computation.contains(tempCode + " :=")) {
                computation = computation.replace(tempCode + " :=", varCode + " :=");
                code.append(computation);
            } else {
                code.append(computation);
                code.append(varCode).append(SPACE)
                        .append(ASSIGN).append(typeString).append(SPACE)
                        .append(tempCode).append(END_STMT);
            }
        }

        return code.toString();
    }


    private String visitAssignStmt(JmmNode node, Void unused) {

        String varName = node.getDescendants(VAR_REF_EXPR).getFirst().get("name");
        JmmType thisType = TypeUtils.with(table).getExprType(node.getChild(0));
        JmmNode assigneeExpr = node.getChild(1);

        return visitAssignment(node, varName, thisType, assigneeExpr);
    }




    private String simpleVarDecl(JmmNode varDecl, Void unused) {
        if (!varDecl.getChildren(EXPR).isEmpty()) {
            String varName = varDecl.get(JmmAttributes.VAR_DECL.NAME);
            JmmType thisType = TypeUtils.with(table).convertType(varDecl.getChild(0));
            JmmNode assigneeExpr = varDecl.getChild(1);
            return visitAssignment(varDecl, varName, thisType, assigneeExpr);
        }
        return "";
    }


    private String visitReturn(JmmNode node, Void unused) {
        JmmType retType = node.getChildren().isEmpty() ? VOID : TypeUtils.with(table).getExprType(node.getChild(0));
        OllirExprResult expr=null;
        StringBuilder code = new StringBuilder();
        if (node.getChildren().isEmpty()) {
            code.append("ret" + ollirTypes.toOllirType(retType) + SPACE + END_STMT);
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

        String visibility = "";
        Optional<String> nodeVisibility = node.getOptional(JmmAttributes.METHOD_DECL.VISIBILITY);
        if (nodeVisibility.isPresent()) {
            visibility = nodeVisibility.get();
        }
        code.append(visibility + " ");

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
        var retType = ollirTypes.toOllirType(currentMethod.returnType());
        code.append(retType);
        code.append(L_BRACKET);

        //NEEDS TO CHECK VARDECL WHOM HAVE ASSIGNMENTS
        var varDecls = node.getChildren(VAR_DECL);
        if (!varDecls.isEmpty()) {
            var varDeclCode = varDecls.stream().map(this::visit).collect(Collectors.joining("\n   ", "   ", ""));
            code.append(varDeclCode);
        }

        var stmts = node.getChildren(STMT);
        if (!stmts.isEmpty()) {
            // rest of its children stmts
            var stmtsCode = stmts.stream().map(this::visit).collect(Collectors.joining("\n   ", "   ", ""));
            code.append(stmtsCode);
        }
        var expr=node.getChildren().stream().flatMap(n->n.getChildren().stream()).filter(n -> n.getKind()==EXPLICIT_METHOD_CALL ||
                n.getKind()==IMPLICIT_METHOD_CALL).toList();
        if(!expr.isEmpty()){
            for (var n : expr) {
                OllirExprResult res = exprVisitor.visit(n);
                code.append(res.getComputation());
                code.append(res.getCode());
            }
        }

        code.append(R_BRACKET);
        code.append(NL);

        currentMethod = null;

        return code.toString();
    }

    // a parte do extends só tentei diferentes variações até passar os testes, então não sei se tá correto
    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(NL);

        if (table.getSuper() != null) {
            code.append(table.getClassName() + " extends " + table.getSuper());
        } else code.append(table.getClassName());

        code.append(L_BRACKET);
        code.append(NL);
        code.append(NL);
        for(var child:node.getChildren(FIELD_DECL)){
            var result=visit(child);
            code.append(result);
        }
        code.append(NL);

        code.append(buildConstructor(node));
        code.append(NL);

        for (var child : node.getChildren(METHOD_DECL)) {
            var result = visit(child);
            code.append(result);
        }

        code.append(R_BRACKET);

        return code.toString();
    }

    //TODO: use node to check if fields have initialization
    private String buildConstructor(JmmNode classNode) {
        StringBuilder code = new StringBuilder();
        String extendName = "java.lang.Object";
        if (table.getSuperFullyQualifiedName() != null) {
            extendName = table.getSuper();
        }

        // get constructor params
        List<Symbol> fields = table.getFields();
        List<String> constructorTypes = new ArrayList<>();
        fields.forEach(symbol -> {constructorTypes.add(ollirTypes.nextTemp() + ollirTypes.toOllirType(symbol.type()));});
        String constructorCode = StringUtils.join(constructorTypes, ", ");


        code.append(".construct \"<init>\"(" + constructorCode + ").V" + L_BRACKET);
        code.append("invokespecial(this.\"%s\", \"<init>\").V;\n".formatted(extendName));

        for (int i = 0; i < fields.size(); i++) {
            var symbolField = fields.get(i);
            String typeSuffix = ollirTypes.toOllirType(symbolField.type());
            String fieldIdentifier = symbolField.name() + typeSuffix;
            String tmpVar = "tmp" + i + typeSuffix;
            code.append("putfield(this, %s, %s).V;\n".formatted(fieldIdentifier, tmpVar));
        }
        code.append(R_BRACKET);

        return code.toString();
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
