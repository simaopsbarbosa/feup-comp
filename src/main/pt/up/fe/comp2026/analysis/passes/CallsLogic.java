package pt.up.fe.comp2026.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.MethodSymbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2026.analysis.AnalysisVisitorWithTable;
import pt.up.fe.comp2026.ast.NodeUtils;
import pt.up.fe.comp2026.jmm.ast.JmmKind;

public class CallsLogic extends AnalysisVisitorWithTable {

    public CallsLogic(SymbolTable table) {
        super(table);
    }

    @Override
    protected void buildVisitor() {
        addVisit(JmmKind.EXPLICIT_METHOD_CALL, this::visitMethodCall);
    }

    private Void visitMethodCall(JmmNode methodCall, SymbolTable table) {
        String methodName = methodCall.get("methodname");

        var matchingMethods = table.getMethods().stream()
                .filter(m -> m.name().equals(methodName))
                .toList();

        if (matchingMethods.isEmpty()) { // if method does not exist locally
            String superClass = table.getSuper();
            if (superClass != null && !superClass.isEmpty()) {
                if (table.getImports().contains(superClass)) { // is superclass imported?
                    return null;
                }
            }
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(methodCall),
                    NodeUtils.getColumn(methodCall),
                    "Method '" + methodName + "' does not exist locally and no valid superclass was found.",
                    null
            ));
            return null;
        }

        // assuming no method overloading for now
        var methodData = matchingMethods.get(0);

        var providedArguments = methodCall.getChildren();
        var expectedParameters = methodData.parameters();

        if (providedArguments.size() != expectedParameters.size()) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(methodCall),
                    NodeUtils.getColumn(methodCall),
                    "Invalid number of arguments for method " + methodName +
                            ". Expected " + expectedParameters.size() + ", got " + providedArguments.size(),
                    null
            ));
        }

        // TODO: compare the data types of providedArguments against expectedParameters
        for (int i = 0; i < providedArguments.size(); i++) {
            JmmNode argNode = providedArguments.get(i);
            pt.up.fe.comp.jmm.analysis.table.Symbol expectedParam = expectedParameters.get(i);

            var expectedType = expectedParam.type();
            var providedType = getTypes().getExprType(argNode);

            if (expectedType.getClass().getName() != providedType.getClass().getName() ||
                    expectedType.isArray() != providedType.isArray()) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(argNode),
                        NodeUtils.getColumn(argNode),
                        "Type mismatch for argument " + (i + 1) + " of method '" + methodName + "'. " +
                                "Expected " + expectedType + " but got " + providedType,
                        null
                ));
            }
        }

        return null;
    }

}
