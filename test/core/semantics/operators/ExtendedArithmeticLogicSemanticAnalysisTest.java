package core.semantics.operators;


import org.junit.Test;

public class ExtendedArithmeticLogicSemanticAnalysisTest extends pt.up.fe.comp.test.env.JmmTestEnv {

    private static final String BASE_PATH = "core/semantics/operators/semanticanalysis/";
    private static final String RESOURCES_LOCATION = "test-public";

    public ExtendedArithmeticLogicSemanticAnalysisTest() {
        super(BASE_PATH, RESOURCES_LOCATION);
    }


    @Test
    public void unaryArithmeticOperators() {
        setDescription("Test that all arithmetic operators are only applied over integers");
        semantics("UnaryArithmeticOperatorsFail.jmm", true);
        semantics("UnaryArithmeticOperatorsOk.jmm", false);
    }

}
