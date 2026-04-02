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
    public void arrayVoidNewSizeFail() {
        setDescription("Test that 'new void[size]' fails.");
        semantics("ArrayVoidNewSizeFail.jmm", true);
    }

    @Test
    public void arrayVoidInitEmptyFail() {
        setDescription("Test that 'new void[] {}' fails.");
        semantics("ArrayVoidInitEmptyFail.jmm", true);
    }

    @Test
    public void arrayVoidInitElementsFail() {
        setDescription("Test that 'new void[] {1, 2}' fails.");
        semantics("ArrayVoidInitElementsFail.jmm", true);
    }
}
