/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.lttng2.ust.ui.analysis.callstack;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.internal.lttng2.ust.ui.analysis.callstack.CallStackStatistics.FunctionInfos;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;

/**
 * View for the call stack statistics.
 *
 * @author Sonia Farrah
 */
public class CallStackStatisticsDepthView extends TmfView  {

    /**
     * Constructor
     */
    public CallStackStatisticsDepthView() {
        super("");
    }

    /** The view's ID */
    public static final String ID = CallStackAnalysis.ID+".latency"; //$NON-NLS-1$

    private @Nullable DepthFunctionsStatisticsView fTreeViewer = null;
    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);

        /* Initialize the viewers with the currently selected trace */
        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        if (trace != null) {
            TmfTraceSelectedSignal signal = new TmfTraceSelectedSignal(this, trace);
            if (fTreeViewer != null) {
                fTreeViewer.traceSelected(signal);
            }
        }
        final DepthFunctionsStatisticsView viewer = new DepthFunctionsStatisticsView(parent);

        /* Add selection listener to tree viewer */
        viewer.addSelectionChangeListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                ISelection selection = event.getSelection();
                if (selection instanceof IStructuredSelection) {
                    Object structSelection = ((IStructuredSelection) selection).getFirstElement();
                    if (structSelection instanceof FunctionInfos) {
                        FunctionInfos entry = (FunctionInfos) structSelection;
                        if (fTreeViewer != null) {
                            fTreeViewer.setSelectedThread(entry.getAddress().toString());
                        }
                    }
                }
            }
        });

        viewer.getControl().addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                super.controlResized(e);
            }
        });

        fTreeViewer = viewer;
    }
    @Override
    public void setFocus() {
        // TODO Auto-generated method stub
    }
}