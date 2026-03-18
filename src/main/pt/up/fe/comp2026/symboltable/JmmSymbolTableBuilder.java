package pt.up.fe.comp2026.symboltable;

import pt.up.fe.comp.jmm.analysis.table.MethodSymbol;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Visibility;
import pt.up.fe.comp.jmm.analysis.table.reflection.Importer;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmPrimitiveType;
import pt.up.fe.comp.jmm.analysis.table.type.JmmType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmArrayType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmClassType;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp.jmm.utils.Attributes;
import pt.up.fe.comp2026.ast.NodeUtils;
import pt.up.fe.comp2026.ast.TypeUtils;
import pt.up.fe.comp2026.jmm.ast.JmmAttributes;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;

import static java.util.Collections.frequency;
import static pt.up.fe.comp2026.jmm.ast.JmmKind.*;

public class JmmSymbolTableBuilder {

    private final JmmNode root;
    private final Importer importer;
    public String className;
    private final List<Report> reports;
    private final List<String> imports;
    private final Map<String, String> declaredClasses;

    /**
     * Only build() can create new instances, this ensures that each instance is used only once,
     * and we do not have to worry about "cleaning state".
     */
    private JmmSymbolTableBuilder(JmmNode root) {
        this.root = root;
        reports = new ArrayList<>();
        imports = new ArrayList<>();
        declaredClasses = new HashMap<>();
        this.importer = Importer.fromThisClassPath();
    }

    private static Report newError(JmmNode node, String message) {
        return Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(node),
                NodeUtils.getColumn(node),
                message,
                null);
    }

    public static SymbolTableBuilderResult build(JmmNode root) {
        return new JmmSymbolTableBuilder(root).buildInternal();
    }

    private SymbolTableBuilderResult buildInternal() {


        var packageDecl = root.getChildren(PACKAGE_DECL).getFirst();
        var packagePathList = packageDecl.getObjectAsList("path", String.class);
        var packagePath = String.join(".", packagePathList);

        var classDecl = root.getObject("classNode", JmmNode.class);
        SpecsCheck.checkArgument(CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);

        this.className = classDecl.get("name");
        var fullyQualifiedName = packagePath + "." + className;

        //get imports
        var importDecls = root.getChildren(IMPORT_DECL);
        buildimports(importDecls);
        System.out.println("Imports: " + imports);

        //get super qualified name
        var superQualified = this.getSuperQualifiedName(classDecl);

        // Check if className is available
        if (declaredClasses.containsKey(className)) {
            reports.add(newError(root, "'" + className + "' is already defined in this compilation unit"));
        }
        declaredClasses.put(className, fullyQualifiedName);


        var methods = buildMethods(classDecl);
        var fields= buildFields(classDecl);

        var symbolTable = new JmmSymbolTable(imports, fullyQualifiedName, superQualified, fields, methods, importer);

        return new SymbolTableBuilderResult(symbolTable, reports);
    }
    private List<Symbol> buildFields(JmmNode classDecl){
        var seen=new HashSet<>();
        return classDecl.getChildren(FIELD_DECL).stream()
                .filter(decl ->{
                    var name=decl.getChild(0).get("name");
                    System.out.println(name+"sucks");
                    if(seen.contains(name)){
                        reports.add(newError(decl,"Same name Variables"));
                        return  false;
                    }
                    seen.add(name);
                    return true;
                })
                .map(decl -> new Symbol(TypeUtils.convertType(decl.getChild(0).getChild(0)), decl.getChild(0).get(JmmAttributes.VAR_DECL.NAME)))
                .toList();
    }

    private List<MethodSymbol> buildMethods(JmmNode classDecl) {
        var seen=new HashSet<>();
        return  classDecl.getChildren(METHOD_DECL).stream()
                .map(this::buildMethod)
                .filter(methodSymbol ->{
                    String signature=methodSymbol.signature().toString();
                    if(seen.contains(signature)){
                       reports.add(newError(classDecl,"Methods with same signature"));
                       return  false;
                    }
                    seen.add(signature);
                    return true;
                })
                .toList();



    }

    private MethodSymbol buildMethod(JmmNode method) {
        var methodName = method.get("name");

        System.out.println(methodName + " " + method.getChildren());

        var returnType = TypeUtils.convertType(method.getChild(0));
        var seenParams=new HashSet<>();
        var params = method.getChildren(PARAM).stream()
                .filter(param->{
                    String name=param.get(JmmAttributes.PARAM.NAME);
                    if(seenParams.contains(name)){
                        reports.add(newError(param,"Duplicate parameters"));
                        return  false;
                    }
                    seenParams.add(name);
                    return true;
                })
                .map(param -> new Symbol(TypeUtils.convertType(param.getChild(0)),param.get(JmmAttributes.VAR_DECL.NAME)))
                .toList();
        var seenLocals=new HashSet<>();
        var locals = method.getChildren(VAR_DECL).stream()
                .filter(decl ->{
                    String name=decl.get(JmmAttributes.VAR_DECL.NAME);
                    if(seenLocals.contains(name)||seenParams.contains(name)){
                        reports.add(newError(decl,"Same name Variables"));
                        return  false;
                    }
                    seenLocals.add(name);
                    return true;
                })
                .map(varDecl -> new Symbol(TypeUtils.convertType(varDecl.getChild(0)), varDecl.get(JmmAttributes.VAR_DECL.NAME)))
                .toList();

        var visibility =  Visibility.PUBLIC;
        var isStatic = method.getBoolean(JmmAttributes.METHOD_DECL.IS_STATIC, false);

        return new MethodSymbol(methodName, returnType, params, locals, isStatic, visibility);
    }


    private void buildimports(List<JmmNode> importDecls) {
        var nameToPath = new HashMap<String, String>();

        for (var importDecl : importDecls) {
            var path = importDecl.getObjectAsList("path", String.class);
            var real_path = String.join(".", path);
            var className = path.getLast();

            if (!nameToPath.containsKey(className)) {
                nameToPath.put(className, real_path);
                imports.add(real_path);
                continue;
            }

            if(!nameToPath.get(className).equals(real_path)) {
                reports.add(newError(importDecl, "'"+real_path+"."+className+"' is already defined"));
            }
        }
    }

    private String getSuperQualifiedName(JmmNode classDecl) {
        var superName = classDecl.getOptional("supername");
        String superQualified = null;

        if (superName.isPresent()) {
            for(var importStr : imports) {
                if (importStr.contains(superName.get())) superQualified = importStr;
            }
        }

        //should probably do in import Visitor

        //if (superName.isPresent() && JmmSymbolTable::isImplicitImport(superName.get())) {
        //    return superQualified;
        //}
//
//
        //if (superName.isPresent() && superQualified == null) {
        //    reports.add(newError(classDecl, "'" + superName.get() +  "' has no import declaration "));
        //}

        return superQualified;

    }


}
