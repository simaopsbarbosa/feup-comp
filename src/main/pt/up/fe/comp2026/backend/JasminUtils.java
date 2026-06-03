package pt.up.fe.comp2026.backend;

import org.specs.comp.ollir.AccessModifier;
import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.OperationType;
import org.specs.comp.ollir.type.ArrayType;
import org.specs.comp.ollir.type.BuiltinType;
import org.specs.comp.ollir.type.ClassType;
import org.specs.comp.ollir.type.Type;
import pt.up.fe.comp.jmm.analysis.table.reflection.Importer;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmArrayType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmClassType;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.HashMap;
import java.util.Map;

public class JasminUtils {

    private final OllirResult ollirResult;

    private final Map<String, String> fullClassnames;

    private final Importer importer;

    public JasminUtils(OllirResult ollirResult) {
        this.ollirResult = ollirResult;
        this.importer = Importer.fromThisClassPath();
        // Build imports table
        fullClassnames = new HashMap<>();

        // Predefined classnames
        fullClassnames.put("this", ollirResult.getOllirClass().getClassFullyQualifiedName().replace('.', '/'));
        // This will be get caught as STRING OLLIR element type.
        // And classes cannot be named String, since it is an OLLIR reserved keyword
        //imports.put("String", "java/lang/String");

        for (var fullImport : ollirResult.getOllirClass().getImports()) {
            var splitted = fullImport.split("\\.");

            // Last element will be the key
            var key = splitted[splitted.length - 1];
            fullClassnames.put(key, fullImport.replace('.', '/'));
        }
    }


    public String getTypePrefix(Type type) {
        System.out.println("[TODO] JasminUtils.getTypePrefix(): Assumes it is always int, needs to be expanded");
        String ret = "i";
        if (type instanceof BuiltinType builtinType) {
            switch (builtinType.getKind()) {
                case VOID -> ret = "";
                case INT32,BOOLEAN -> ret="i";
            }
        }
        if(type instanceof  ClassType || type instanceof ArrayType){
            ret="a";
        }
        return ret;
    }

    public String getTypeDescriptor(Type type) {

        if (type instanceof BuiltinType builtinType) {
            return switch (builtinType.getKind()) {
                case INT32 -> "I";
                case BOOLEAN -> "Z";
                case VOID -> "V";
                default ->
                        throw new RuntimeException("Not implemented for element type '" + builtinType.getKind() + "'");
            };
        }
        if (type instanceof ClassType classType) {
            String qualifiedName = this.getQualifiedNameFromClass(classType.getName());
            return "L" +  qualifiedName + ";";
        }
        if(type instanceof ArrayType arrayType){
            return new StringBuilder().repeat("[",arrayType.getNumDimensions())+this.getTypeDescriptor(arrayType.getElementType());
        }

        throw new RuntimeException("Not implemented for element type '" + type + "'");
    }


    public String getModifier(AccessModifier accessModifier) {
        String modifierName = accessModifier.name().toLowerCase();
        //for some reason breaks with default
        if (modifierName.equals("default")) {
            return "";
        }
        return modifierName + " ";
    }

    public String getLoad(Descriptor reg) {
        var prefix = getTypePrefix(reg.getVarType());
        var value = reg.getVirtualReg();

        return prefix + "load " + value;
    }

    public String getStore(Descriptor reg) {
        var prefix = getTypePrefix(reg.getVarType());
        var value = reg.getVirtualReg();

        return prefix + "store " + value;
    }


    public String getQualifiedNameFromClass(String className) {
        String l;
        if(className.equals(ollirResult.getOllirClass().getClassName())){
            l="this";
        }
        else{
           l=className;
        }
        String qualifiedName = fullClassnames.get(l);
        //verify implicit with importer later
        if (importer.isImplicitImport(className)) {
            var implicitImport = importer.tryImplicitImport(className);
            if (implicitImport.isPresent()) {
                String dotted = implicitImport.get().getFullyQualifiedName();
                qualifiedName = dotted.replace('.', '/');
            }
        }

        //default to object
        if (qualifiedName == null) qualifiedName = "java/lang/Object";

        return qualifiedName;
    }


    public String getIfInstructionCode(OperationType opType) {
        String id = "";

        switch (opType) {
            case LTH -> id = "lt";
            case GTH -> id = "gt";
            case EQ -> id = "eq";
            case LTE -> id = "le";
            case GTE -> id = "ge";
            case NEQ -> id = "ne";

            case LOGICAL_AND -> id = "";
            case LOGICAL_OR -> id = "";
        }

        return "if_icmp" + id;
    }
    public  String getType(Type type){

        if(type instanceof ArrayType arrayType){
            Type eleType=arrayType.getElementType();
            if (eleType instanceof BuiltinType builtinType) {
                return switch (builtinType.getKind()) {
                    case INT32 -> "int";
                    case BOOLEAN -> "boolean";
                    default ->
                            throw new RuntimeException("Not implemented for element type '" + builtinType.getKind() + "'");
                };
            }
            if(eleType instanceof ClassType classType) {
                return this.getQualifiedNameFromClass(classType.getName());
            }
        }
        return "";
    }


}
