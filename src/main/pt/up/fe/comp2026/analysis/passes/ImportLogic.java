package pt.up.fe.comp2026.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Signature;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2026.analysis.AnalysisVisitorWithTable;
import pt.up.fe.comp2026.jmm.ast.JmmKind;
import pt.up.fe.comp2026.utils.ReportUtils;


public class ImportLogic extends AnalysisVisitorWithTable {

    public ImportLogic(SymbolTable table) {
        super(table);
    }

    @Override
    protected void buildVisitor() {
        addVisit(JmmKind.CLASS_INITIALIZATION, this::visitClass);

    }

    private Void visitClass(JmmNode classInit, SymbolTable table) {
        var name = classInit.get("name");
        boolean implicit = super.table.isImplicitImport(name);
        boolean isExplicit = super.table.getImportNames().contains(name);
        boolean currentClass = super.table.getClassName().equals(name);
        if (!implicit && !isExplicit && !currentClass) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, classInit, "Class is not defined or imported " + name));
        }
        if (implicit || isExplicit) {
            String qualified = "";
            if (isExplicit) {
                qualified = super.table.getImportedFullyQualifiedName(name).get();
            }
            var argTypes = classInit.getChildren().stream()
                    .map(super.types::getExprType)
                    .toList();
            Signature methodsignature = new Signature("<init>", argTypes);
            var importedTable = implicit ? super.table.getImplicitImport(name).get() : super.table.getImportedSymbolTable(qualified).get();
            if (super.table.getImportedSymbolTable(qualified).isPresent()) {
                var classConstructor = importedTable.getMethod(methodsignature);
                if (classConstructor.isEmpty()) {
                    addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, classInit, "Class constructor is wrong"));
                }
            }
        } else {
            var argTypes = classInit.getChildren().stream()
                    .map(super.types::getExprType)
                    .toList();
            Signature methodsignature = new Signature("<init>", argTypes);

            var classConstructor = super.table.getMethod(methodsignature);
            System.out.println(classConstructor);
            if (classConstructor.isEmpty()) {
                if (!methodsignature.parameters().isEmpty()) {
                    addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, classInit, "Class constructor is wrong"));
                }
            }
        }
        return null;
    }
}
