package pt.up.fe.comp2026.optimization.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2026.jmm.ast.JmmKind;

import java.util.ArrayList;

public class DeadCodeEliminationVisitor extends AJmmVisitor<Void, Boolean> {

    private final SymbolTable symbolTable;
    private String currentMethod;

    public DeadCodeEliminationVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    @Override
    protected void buildVisitor() {
        setDefaultVisit(this::defaultVisit);
    }

    private Boolean defaultVisit(JmmNode node, Void unused) {
        boolean changed = false;
        for (var child : new ArrayList<>(node.getChildren())) {
            changed |= visit(child);
        }
        return changed;
    }
}
