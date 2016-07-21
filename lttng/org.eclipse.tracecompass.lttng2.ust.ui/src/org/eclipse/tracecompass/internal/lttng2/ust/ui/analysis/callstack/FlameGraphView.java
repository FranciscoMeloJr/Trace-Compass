/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Author:
 *     Sonia Farrah
 *******************************************************************************/
package org.eclipse.tracecompass.internal.lttng2.ust.ui.analysis.callstack;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.editors.ITmfTraceEditor;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphViewer;
import org.eclipse.ui.IEditorPart;

import com.google.common.collect.Lists;

/**
 * View to display the flameGraph View
 *
 * @author Sonia Farrah
 */
public class FlameGraphView extends TmfView {

    public static final String ID = "org.eclipse.tracecompass.internal.lttng2.ust.ui.analysis.callstack.stats.flamegraph"; //$NON-NLS-1$

    TimeGraphViewer fTimeGraphViewer;

    private FlameGraphContentProvider fTimeGraphContentProvider;

    private TimeGraphPresentationProvider fPresentationProvider;

    private ITmfTrace fTrace;

    /**
     * Constructor
     */
    public FlameGraphView() {
        super(ID);
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        fTimeGraphViewer = new TimeGraphViewer(parent, SWT.NONE);
        fTimeGraphContentProvider = new FlameGraphContentProvider();
        fPresentationProvider = new FlameGraphPresentationProvider();
        fTimeGraphViewer.setTimeGraphContentProvider(fTimeGraphContentProvider);
        fTimeGraphViewer.setTimeGraphProvider(fPresentationProvider);
        IEditorPart editor = getSite().getPage().getActiveEditor();
        if (editor instanceof ITmfTraceEditor) {
            ITmfTrace trace = ((ITmfTraceEditor) editor).getTrace();
            if (trace != null) {
                traceSelected(new TmfTraceSelectedSignal(this, trace));
            }
        }
    }

    /**
     * Signal updated
     *
     * @param sig
     *            signal
     */
    @TmfSignalHandler
    public void selectionUpdated(TmfSelectionRangeUpdatedSignal sig) {
        fTrace = TmfTraceManager.getInstance().getActiveTrace();
        IAnalysisModule flamegraphModule = fTrace.getAnalysisModule(CallStackAnalysis.ID);
        if (!(flamegraphModule instanceof CallStackAnalysis)) {
            return;
        }
        CallStackAnalysis callStackAnalysis = (CallStackAnalysis) flamegraphModule;
        callStackAnalysis.schedule();
        Job j = new Job("Flamegraph") { //$NON-NLS-1$

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                callStackAnalysis.waitForCompletion(monitor);
                Display.getDefault().asyncExec(() -> {
                    List<FlameGraphNode> temp = callStackAnalysis.getFirstNodes();
                    fTimeGraphViewer.setInput(temp);
                    System.out.println(temp.size());
                });
                return Status.OK_STATUS;
            }
        };
        j.schedule();

    }

    /**
     * Handler for the trace opened signal
     *
     * @param signal
     *            The incoming signal
     */
    @TmfSignalHandler
    public void TraceOpened(TmfTraceOpenedSignal signal) {
        fTrace = signal.getTrace();
        IAnalysisModule flamegraphModule = fTrace.getAnalysisModule(CallStackAnalysis.ID);
        if (!(flamegraphModule instanceof CallStackAnalysis)) {
            return;
        }
        CallStackAnalysis callStackAnalysis = (CallStackAnalysis) flamegraphModule;
        callStackAnalysis.schedule();
        Job j = new Job("Flamegraph") { //$NON-NLS-1$

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                callStackAnalysis.waitForCompletion(monitor);
                Display.getDefault().asyncExec(() -> {
                    List<FlameGraphNode> temp = callStackAnalysis.getFirstNodes();
                    fTimeGraphViewer.setInput(temp);
                    System.out.println(temp.size());
                });
                return Status.OK_STATUS;
            }
        };
        j.schedule();

    }

    /**
     * Handler for the trace selected signal
     *
     * @param signal
     *            The incoming signal
     */
    @TmfSignalHandler
    public void traceSelected(final TmfTraceSelectedSignal signal) {
        fTrace = signal.getTrace();
        IAnalysisModule flamegraphModule = fTrace.getAnalysisModule(CallStackAnalysis.ID);
        if (!(flamegraphModule instanceof CallStackAnalysis)) {
            return;
        }
        CallStackAnalysis callStackAnalysis = (CallStackAnalysis) flamegraphModule;
        callStackAnalysis.schedule();
        Job j = new Job("Flamegraph") { //$NON-NLS-1$

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                callStackAnalysis.waitForCompletion(monitor);
                Display.getDefault().asyncExec(() -> {
                    List<FlameGraphNode> temp = Lists.reverse(callStackAnalysis.getFirstNodes());
                    fTimeGraphViewer.setInput(temp);
                    System.out.println(temp.size());
                });
                return Status.OK_STATUS;
            }
        };
        j.schedule();

    }

    /**
     * Trace is closed: clear the data structures and the view
     *
     * @param signal
     *            the signal received
     */
    @TmfSignalHandler
    public void traceClosed(final TmfTraceClosedSignal signal) {
        if (signal.getTrace() == fTrace) {
            fTimeGraphViewer.setInput(null);
        }
    }

    @Override
    public void setFocus() {
        fTimeGraphViewer.setFocus();
    }

}
