package extensions.semantics.arrays;

import org.junit.Test;

public class ArrayVoidSemanticAnalysisTest extends pt.up.fe.comp.test.env.JmmTestEnv {

    private static final String BASE_PATH = "extensions/semantics/arrays/semanticanalysis/";
    private static final String RESOURCES_LOCATION = "test";

    public ArrayVoidSemanticAnalysisTest() {
        super(BASE_PATH, RESOURCES_LOCATION);
    }

    @Test
    public void arrayVoidDeclarationFail() {
        setDescription("Test that void cannot be used as a base type for arrays in fields, parameters, and locals.");
        semantics("ArrayVoidDeclarationFail.jmm", true);
    }

    @Test
    public void arrayVoidInstantiationFail() {
        setDescription("Test that void cannot be used with the 'new' keyword or in array initialization.");
        semantics("ArrayVoidInstantiationFail.jmm", true);
    }
}
