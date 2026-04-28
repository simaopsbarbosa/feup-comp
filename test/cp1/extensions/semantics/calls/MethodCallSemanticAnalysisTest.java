package cp1.extensions.semantics.calls;

import org.junit.Test;
import pt.up.fe.comp.test.env.JmmTestEnv;

/**
 * Test semantic analysis for method calls.
 */
public class MethodCallSemanticAnalysisTest extends JmmTestEnv {
    private static final String BASE_PATH = "cp1/extensions/semantics/calls/semanticanalysis/";
    private static final String RESOURCES_LOCATION = "test";

    public MethodCallSemanticAnalysisTest() {
        super(BASE_PATH, RESOURCES_LOCATION);
    }

    @Test
    public void nonStaticOnClass() {
        setDescription("Attempt to call a locally defined non-static method using the local Class ID");
        semantics("NonStaticOnClassFail.jmm", true);
    }

    @Test
    public void staticOnClass() {
        setDescription("Call a locally defined static method using the local Class ID");
        semantics("StaticOnClassOk.jmm", false);
    }

    @Test
    public void staticOnInstance() {
        setDescription("Instantiate the local class and call a locally defined static method on the resulting object");
        semantics("StaticOnInstanceOk.jmm", false);
    }

    @Test
    public void importedNonStaticOnClass() {
        setDescription("Import an external class and attempt to call a non-static method directly on the imported Class ID");
        semantics("ImportedNonStaticOnClassFail.jmm", true);
    }

    @Test
    public void shadowedClassId() {
        setDescription("Declare a local variable with the exact same name as an imported class and call a non-static method on it");
        semantics("ShadowedClassIdOk.jmm", false);
    }

    @Test
    public void implicitStaticCall() {
        setDescription("Call a local static method from inside main without using a target");
        semantics("ImplicitStaticCallOk.jmm", false);
    }

    @Test
    public void implicitNonStaticInStaticContext() {
        setDescription("Attempt to call a local non-static method from inside main without using an instance target");
        semantics("ImplicitNonStaticInStaticContextFail.jmm", true);
    }

    @Test
    public void staticMethodWithArguments() {
        setDescription("Call a static method over a Class ID passing multiple arguments");
        semantics("StaticMethodWithArgumentsOk.jmm", false);
    }
}
