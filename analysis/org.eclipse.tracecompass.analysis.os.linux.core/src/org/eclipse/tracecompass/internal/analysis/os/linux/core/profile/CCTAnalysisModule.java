package org.eclipse.tracecompass.internal.analysis.os.linux.core.profile;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * @author frank
 *
 */
public class CCTAnalysisModule extends TmfAbstractAnalysisModule {
    /**
     * Analysis ID, it should match that in the plugin.xml file
     */
    public static final @NonNull String ID = "org.eclipse.tracecompass.analysis.os.linux.core.profile.cctanalysis.module"; //$NON-NLS-1$

    /**
     * Default constructor
     */
    public CCTAnalysisModule() {
        super();
    }

    @Override
    protected boolean executeAnalysis(IProgressMonitor monitor) throws TmfAnalysisException {
        ITmfTrace trace = checkNotNull(getTrace());

        //Node<TestData> root = Node.create(new TestData(0, "root"));
        RequestTest request = new RequestTest(); // with the active
                                                      // trace
        trace.sendRequest(request); // the method handleData is called for
                                         // each event
        try {
            request.waitForCompletion();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Abstract event request to fill a tree
     */

    private static class RequestTest extends TmfEventRequest {

        public RequestTest() {
            super(ITmfEvent.class, TmfTimeRange.ETERNITY, 0, ITmfEventRequest.ALL_DATA, ExecutionType.BACKGROUND);
        }

        @Override
        public void handleData(final ITmfEvent event) {
            // Just for test, print on the console and add the children
            System.out.println(event.toString());
        }

        /*
         * @Override public void handleData(final ITmfEvent event) { // Just for
         * test, print on the console and add the children
         * System.out.println(event.getName()); Node<TestData> aux =
         * Node.create(new TestData(0, event.getName()));
         *
         * fNode.addChild(aux);
         *
         * final String eventName = event.getType().getName(); if
         * (eventName.startsWith(layout.eventSyscallEntryPrefix()) ||
         * eventName.startsWith(layout.eventCompatSyscallEntryPrefix())) {
         *
         * }else if (eventName.startsWith(layout.eventSyscallExitPrefix())) {
         *
         * long endTime = event.getTimestamp().getValue(); } }
         *  >>write (f, g, h)
         *
         *
         */
    }

    @Override
    public boolean canExecute(ITmfTrace trace) {
        System.out.println("CCTAnalysisModule.canExecute()");
        return true;
    }

    @Override
    protected void canceling() {
    }
}
