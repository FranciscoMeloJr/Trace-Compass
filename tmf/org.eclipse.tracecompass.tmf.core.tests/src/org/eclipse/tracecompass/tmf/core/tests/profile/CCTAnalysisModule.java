package org.eclipse.tracecompass.tmf.core.tests.profile;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.IAnalysisProgressListener;
import org.eclipse.tracecompass.internal.tmf.core.profile.Node;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest.ExecutionType;
import org.eclipse.tracecompass.tmf.core.tests.profile.TestProfileTree.TestData;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

/**
 * @author frank
 *
 */
public abstract class CCTAnalysisModule extends TmfAbstractAnalysisModule {
    /**
     * Analysis ID, it should match that in the plugin.xml file
     */
    public static final @NonNull String ID = "org.eclipse.tracecompass.tmf.core.tests.profile.cctanalysis.module"; //$NON-NLS-1$

    @Override
    protected boolean executeAnalysis(IProgressMonitor monitor) throws TmfAnalysisException {
        ITmfTrace trace = checkNotNull(getTrace());

        requestTest request = new requestTest(trace); // with the active
                                                              // trace
        getTrace().sendRequest(request); // the method handleData is called for
                                         // each event
        request.waitForCompletion(); // waits for completion
        return true;
    }

    /**
     * Abstract event request to fill a tree
     */
    private static class requestTest extends TmfEventRequest { //

        private final Node<TestData> fNode;

        public <T> requestTest(Node<TestData> node) { //public <T extends IProfileData> requestTest(Node<T extends IProfileData> node)
            super(ITmfEvent.class, TmfTimeRange.ETERNITY, 0, ITmfEventRequest.ALL_DATA, ExecutionType.BACKGROUND);

            fNode = node;
        }

        @Override
        public void handleData(final ITmfEvent event) {
            //Just for test, print on the console and add the children
            System.out.println(event.getName());
            Node<TestData> aux = Node.create(new TestData(0, event.getName()));

            fNode.addChild(aux);
        }

    }
}
