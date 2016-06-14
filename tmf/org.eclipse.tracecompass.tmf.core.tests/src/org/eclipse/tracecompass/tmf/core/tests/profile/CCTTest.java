package org.eclipse.tracecompass.tmf.core.tests.profile;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.tracecompass.analysis.os.linux.core.tests.Activator;
import org.eclipse.tracecompass.analysis.os.linux.core.tests.stubs.trace.TmfXmlKernelTraceStub;
import org.eclipse.tracecompass.internal.tmf.core.profile.Node;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.tests.profile.TestProfileTree.TestData;
import org.eclipse.tracecompass.tmf.core.tests.shared.TmfTestHelper;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author francisco from Geneviève Bastien code
 *
 */
public class CCTTest {
    private static final String KERNEL_FILE_TEST = "testfiles/KernelCCTAnalysis_testTrace.xml";
    private ITmfTrace fTrace;
    private CCTAnalysisModule fModule = null;

    private static void deleteSuppFiles(ITmfTrace trace) {
        /* Remove supplementary files */
        File suppDir = new File(TmfTraceManager.getSupplementaryFileDir(trace));
        for (File file : suppDir.listFiles()) {
            file.delete();
        }
    }

    /**
     * Setup the trace for the tests
     */
    @Before
    public void settings() {
        // take the trace:
        ITmfTrace trace = new TmfXmlKernelTraceStub();
        IPath filePath = Activator.getAbsoluteFilePath(KERNEL_FILE_TEST);
        IStatus status = trace.validate(null, filePath.toOSString());
        if (!status.isOK()) {
            fail(status.getException().getMessage());
        }
        try {
            trace.initTrace(null, filePath.toOSString(), TmfEvent.class);
        } catch (TmfTraceException e) {
            fail(e.getMessage());
        }
        deleteSuppFiles(trace);
        ((TmfTrace) trace).traceOpened(new TmfTraceOpenedSignal(this, trace, null));
        // original source:
        fModule = TmfTraceUtils.getAnalysisModuleOfClass(trace, CCTAnalysisModule.class, CCTAnalysisModule.ID);
        assertNotNull(fModule);
        fTrace = trace;
    }

    /**
     * Clean up
     */
    @After
    public void tearDown() {
        deleteSuppFiles(fTrace);
        fTrace.dispose();
    }

    /**
     * Test analysis from test
     */
    @Test
    public void testAnalysisExecution() {
        /* Make sure the analysis hasn't run yet */
        // assertNull(fModule.getStateSystem());

        /* Execute the analysis */
        // assertTrue(TmfTestHelper.executeAnalysis(fModule));
        // assertNotNull(fModule.getStateSystem());
        assertTrue(TmfTestHelper.executeAnalysis(fModule));

        /*
         * If we want to call it: if (module instance of
         * TmfAbstractAnalysisModule) { try { Class<?>[] argTypes = new Class[]
         * { IProgressMonitor.class }; Method method =
         * TmfAbstractAnalysisModule.class.getDeclaredMethod("executeAnalysis",
         * argTypes); method.setAccessible(true); Object obj =
         * method.invoke(module, new NullProgressMonitor()); return (Boolean)
         * obj; } catch (IllegalAccessException | IllegalArgumentException |
         * InvocationTargetException | NoSuchMethodException | SecurityException
         * e) { fail(e.toString()); }
         */

    }

    /**
     * Test execute Analysis by francis
     */

    /**
     * Abstract event request to fill a tree
     */
    @Ignore
    private static class requestTest extends TmfEventRequest { //

        private final Node<TestData> fNode;

        public <T> requestTest(Node<TestData> node) {
            super(ITmfEvent.class, TmfTimeRange.ETERNITY, 0, ITmfEventRequest.ALL_DATA, ExecutionType.BACKGROUND);

            fNode = node;
        }

        @Override
        public void handleData(final ITmfEvent event) {
            // Just for test, print on the console and add the children
            System.out.println(event.getName());
            Node<TestData> aux = Node.create(new TestData(0, event.getName()));

            fNode.addChild(aux);
        }

        @Ignore
        public void avoidAnnoyingErrorMessageWhenRunningTestsInAnt() {
            assertTrue(true); // do nothing;
        }
    }
}
