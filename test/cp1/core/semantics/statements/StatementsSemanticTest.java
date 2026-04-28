package cp1.core.semantics.statements;

import org.junit.Test;
import pt.up.fe.comp.test.env.JmmTestEnv;

public class StatementsSemanticTest extends JmmTestEnv {

    private static final String BASE_PATH = "cp1/core/semantics/statements/semanticanalysis/";
    private static final String RESOURCES_LOCATION = "test";

    public StatementsSemanticTest() {
        super(BASE_PATH, RESOURCES_LOCATION);
    }

    @Test
    public void testIfWhileSemantics() {
        setDescription("Test that if and while conditions must be boolean");
        semantics("IfWhileFail.jmm", true);
        semantics("IfWhileOk.jmm", false);
    }

    @Test
    public void testDoWhileSemantics() {
        setDescription("Test that do-while conditions must be boolean");
        semantics("DoWhileFail.jmm", true);
        semantics("DoWhileOk.jmm", false);
    }

    @Test
    public void testForSemantics() {
        setDescription("Test that for loop conditions must be boolean");
        semantics("ForFail.jmm", true);
        semantics("ForOk.jmm", false);
    }

    @Test
    public void testReturnSemantics() {
        setDescription("Test that return expressions match the method's return type");
        semantics("ReturnFail.jmm", true);
        semantics("ReturnOk.jmm", false);
    }

    @Test
    public void testMethodDeclSemantics() {
        setDescription("Test method declaration rules (e.g. no duplicate local variables)");
        semantics("MethodDeclFail.jmm", true);
        semantics("MethodDeclOk.jmm", false);
    }
}
