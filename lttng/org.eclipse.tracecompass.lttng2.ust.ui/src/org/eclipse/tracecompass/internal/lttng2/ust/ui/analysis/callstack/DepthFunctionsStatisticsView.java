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

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.internal.lttng2.ust.ui.analysis.callstack.CallStackStatistics.FunctionInfos;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.ui.symbols.SymbolProviderManager;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.AbstractTmfTreeViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeColumnDataProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeViewerEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeColumnData;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeViewerEntry;

//import com.google.common.base.Joiner;

/**
 * View to display the call stack's statistics. It shows the function's address,
 * its self time, the total time spent ,the total calls ,the callers and the
 * callees of this function.
 *
 * @author Sonia Farrah
 */
public class DepthFunctionsStatisticsView extends AbstractTmfTreeViewer {

    private static final String ID = CallStackAnalysis.ID + ".DepthFunc"; //$NON-NLS-1$

    // Timeout between to wait for in the updateElements method
    private CallStackAnalysis fModule = null;
    private String fSelectedThread = null;
    private CallStackStatistics fFunctionStatics = null;
    private ArrayList<FunctionInfos> fFunctionInfos = new ArrayList<>();

    private static final String[] COLUMN_NAMES = new String[] {
            Messages.SegmentStoreStaticsViewer_Depth,
            Messages.Function_address,
            Messages.SegmentStoreStaticsViewer_selfTime,
            Messages.SegmentStoreStaticsViewer_totalTime,
            Messages.SegmentStoreStaticsViewer_totalCalls,
            Messages.SegmentStoreStaticsViewer_Callers,
            Messages.SegmentStoreStaticsViewer_Callees,
    };

    /* A map that saves the mapping of a thread ID to its executable name */
    private final Map<String, String> fProcessNameMap = new HashMap<>();

    /** Provides label for the Call stack statistics viewer cells */
    protected static class CallStackLabelProvider extends TreeLabelProvider {
        @Override
        public String getColumnText(Object element, int columnIndex) {
            FunctionInfos obj = (FunctionInfos) element;
            if (columnIndex == 0) {
                return Integer.toString(obj.getDepth());
            } else if (columnIndex == 1) {
                ISymbolProvider x = SymbolProviderManager.getInstance().getSymbolProvider(TmfTraceManager.getInstance().getActiveTrace());
                String funcName = x.getSymbolText(obj.getAddress());
                if (funcName != null) {
                    return funcName;
                }
                return "0x" + Long.toHexString(obj.getAddress()); //$NON-NLS-1$
            } else if (columnIndex == 2) {
                return obj.getTotalSelfTime().toString();
            } else if (columnIndex == 3) {
                return obj.getTotalTime().toString();
            } else if (columnIndex == 4) {
                return Integer.toString(obj.getNbreCalls());
            } else if (columnIndex == 5) {
                return obj.getCallers().toString();
            } else if (columnIndex == 6) {
                return obj.getCallees().toString();
            }
            return element.toString();
        }

    }

    /**
     * Constructor
     *
     * @param parent
     *            The parent composite that holds this viewer
     */
    public DepthFunctionsStatisticsView(Composite parent) {
        super(parent, false);
        setLabelProvider(new CallStackLabelProvider());
    }

    @Override
    protected ITmfTreeColumnDataProvider getColumnDataProvider() {
        return new ITmfTreeColumnDataProvider() {

            @Override
            public List<TmfTreeColumnData> getColumnData() {
                List<TmfTreeColumnData> columns = new ArrayList<>();
                TmfTreeColumnData column = new TmfTreeColumnData(COLUMN_NAMES[0]);
                column.setComparator(new ViewerComparator() {
                    @Override
                    public int compare(Viewer viewer, Object e1, Object e2) {
                        FunctionInfos n1 = (FunctionInfos) e1;
                        FunctionInfos n2 = (FunctionInfos) e2;

                        return Integer.compare(n1.getDepth(), n2.getDepth());

                    }
                });
                columns.add(column);
                column = new TmfTreeColumnData(COLUMN_NAMES[1]);
                column.setComparator(new ViewerComparator() {
                    @Override
                    public int compare(Viewer viewer, Object e1, Object e2) {
                        FunctionInfos n1 = (FunctionInfos) e1;
                        FunctionInfos n2 = (FunctionInfos) e2;

                        return n1.getAddress().compareTo(n2.getAddress());

                    }
                });
                columns.add(column);
                column = new TmfTreeColumnData(COLUMN_NAMES[2]);
                column.setComparator(new ViewerComparator() {
                    @Override
                    public int compare(Viewer viewer, Object e1, Object e2) {
                        FunctionInfos n1 = (FunctionInfos) e1;
                        FunctionInfos n2 = (FunctionInfos) e2;

                        return n1.getTotalSelfTime().compareTo(n2.getTotalSelfTime());

                    }
                });
                columns.add(column);

                column = new TmfTreeColumnData(COLUMN_NAMES[3]);
                column.setComparator(new ViewerComparator() {
                    @Override
                    public int compare(Viewer viewer, Object e1, Object e2) {
                        FunctionInfos n1 = (FunctionInfos) e1;
                        FunctionInfos n2 = (FunctionInfos) e2;

                        return n1.getTotalTime().compareTo(n2.getTotalTime());

                    }
                });
                columns.add(column);
                column = new TmfTreeColumnData(COLUMN_NAMES[4]);
                columns.add(column);

                column = new TmfTreeColumnData(COLUMN_NAMES[5]);
                columns.add(column);

                column = new TmfTreeColumnData(COLUMN_NAMES[6]);
                columns.add(column);
                return columns;
            }

        };
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    @Override
    protected void contentChanged(ITmfTreeViewerEntry rootEntry) {
        String selectedThread = fSelectedThread;
        if (selectedThread != null) {
            /* Find the selected thread among the inputs */
            for (ITmfTreeViewerEntry entry : rootEntry.getChildren()) {
                if (entry instanceof FunctionInfos) {
                    if (selectedThread.equals(((FunctionInfos) entry).getAddress().toString())) {
                        List<ITmfTreeViewerEntry> list = Collections.singletonList(entry);
                        super.setSelection(list);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void initializeDataSource() {
        /* Should not be called while trace is still null */
        ITmfTrace trace = checkNotNull(getTrace());

        fModule = TmfTraceUtils.getAnalysisModuleOfClass(trace, CallStackAnalysis.class, CallStackAnalysis.ID);
        if (fModule == null) {
            return;
        }
        fModule.schedule();
        fModule.waitForCompletion();
        fProcessNameMap.clear();
    }

    @Override
    protected ITmfTreeViewerEntry updateElements(long start, long end, boolean isSelection) {
        if (isSelection || (start == end)) {
            return null;
        }
        if (getTrace() == null || fModule == null) {
            return null;
        }
        TmfTreeViewerEntry root = new TmfTreeViewerEntry(""); //$NON-NLS-1$
        List<ITmfTreeViewerEntry> entryList = root.getChildren();
        fFunctionStatics = fModule.getSegmentStatics();
        HashMap<Integer, HashMap<Long, FunctionInfos>> statistics = fFunctionStatics.getDepthstatistics();
        for (Entry<Integer, HashMap<Long, FunctionInfos>> Functions : statistics.entrySet()) {
            for (Entry<Long, FunctionInfos> functionEntry : Functions.getValue().entrySet()) {
                entryList.add(functionEntry.getValue());
            }
        }
        return root;
    }

    /**
     * Set the currently selected thread ID
     *
     * @param tid
     *            The selected thread ID
     */
    public void setSelectedThread(String tid) {
        fSelectedThread = tid;
    }

    /**
     * @return The call stack statistics
     */
    public CallStackStatistics getFunctionStatics() {
        return fFunctionStatics;
    }

    /**
     * @param functionStatics
     *            The functions statistics
     */
    public void setFunctionStatics(CallStackStatistics functionStatics) {
        fFunctionStatics = functionStatics;
    }

    /**
     * @return The statistics
     */
    public ArrayList<FunctionInfos> getFunctionInfos() {
        return fFunctionInfos;
    }

    /**
     * @param functionInfos
     *            The statistics
     */
    public void setFunctionInfos(ArrayList<FunctionInfos> functionInfos) {
        fFunctionInfos = functionInfos;
    }

    /**
     * @return ID
     */
    public static String getId() {
        return ID;
    }

}