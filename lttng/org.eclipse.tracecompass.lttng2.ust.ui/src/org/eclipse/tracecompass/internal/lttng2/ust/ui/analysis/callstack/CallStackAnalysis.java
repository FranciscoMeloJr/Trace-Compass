/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.tracecompass.internal.lttng2.ust.ui.analysis.callstack;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.IAnalysisProgressListener;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.internal.analysis.timing.core.store.ArrayListStore;
import org.eclipse.tracecompass.lttng2.ust.ui.analysis.callstack.LttngUstCallStackAnalysis;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.views.callstack.AbstractCallStackAnalysis;

/**
 * Call-stack analysis used to create a segment for each stack call event.
 *
 * @author Sonia Farrah
 * @since 2.0
 */
public class CallStackAnalysis extends TmfAbstractAnalysisModule implements ISegmentStoreProvider {

    /**
     * ID
     */
    public static final String ID = "org.eclipse.tracecompass.internal.lttng2.ust.ui.analysis.callstack.stats"; //$NON-NLS-1$

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private final ISegmentStore<@NonNull ISegment> fStore = new ArrayListStore<>();
    private final ListenerList fListeners = new ListenerList(ListenerList.IDENTITY);
    private final CallStackStatistics segmentStatistics = new CallStackStatistics();
    //The functions called in the first level
    private List<FlameGraphNode> threadNodes = new ArrayList<>();
    private List<Integer> actualQuarks = null;

    @Override

    protected boolean executeAnalysis(@Nullable IProgressMonitor monitor) {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return false;
        }
        IAnalysisModule module = trace.getAnalysisModule(LttngUstCallStackAnalysis.ID);
        if (!(module instanceof LttngUstCallStackAnalysis)) {
            return false;
        }
        LttngUstCallStackAnalysis analysisModule = (LttngUstCallStackAnalysis) module;
        analysisModule.waitForCompletion();
        ITmfStateSystem ss = analysisModule.getStateSystem();
        if (ss == null) {
            return false;
        }
        if ( monitor == null) {
            return false;
        }
        List<Integer> threadQuarks = Collections.emptyList();
        List<Integer> processQuarks = ss.getQuarks(((AbstractCallStackAnalysis) module).getProcessesPattern());
        for (int processQuark : processQuarks) {
            threadQuarks = ss.getQuarks(processQuark, ((AbstractCallStackAnalysis) module).getThreadsPattern());
            FlameGraphNode firstNode = null;
            for (int threadQuark : threadQuarks) {
                //Create the root node representing the thread
                FlameGraphNode init=new FlameGraphNode(0L,0L,0);
                String threadName=ss.getAttributeName(threadQuark);
                init.setName(threadName);
                try {
                    long curTime = ss.getStartTime();
                    long limit = ss.getCurrentEndTime();
                    while (curTime < limit) {
                        if (monitor.isCanceled()) {
                            return false;
                        }
                        int callStackQuark = ss.getQuarkRelative(threadQuark, ((AbstractCallStackAnalysis) module).getCallStackPath()); // $NON-NLS-1$
                        actualQuarks = ss.getSubAttributes(callStackQuark, false);
                        int quarkParent = actualQuarks.get(0);
                        ITmfStateInterval interval = ss.querySingleState(curTime, quarkParent);
                        ITmfStateValue stateValue = interval.getStateValue();

                        if (!stateValue.isNull()) {
                            long intervalStart = interval.getStartTime();
                            long intervalEnd = interval.getEndTime();
                            if (monitor.isCanceled()) {
                                return false;
                            }
                            // Create the segment for the first call event.
                            CalledFunction segment = new CalledFunction(intervalStart, intervalEnd, stateValue.unboxLong());
                            segment.setDepth(0);
                            firstNode = new FlameGraphNode(stateValue.unboxLong(), intervalEnd - intervalStart, segment.getDepth());
                            firstNode.setMaxDepth(actualQuarks.size());
                            findChildren(segment, 0, ss, actualQuarks.size() - 1, firstNode);
                            // Calculate the statistics for the first segment
                            segmentStatistics.calculateStatistics(segment);
                            init.addChild(firstNode);
                        }
                        curTime = interval.getEndTime() + 1;
                    }
                    while (!ss.waitUntilBuilt(500)) {

                    }
                    monitor.worked(1);
                    monitor.done();

                } catch (AttributeNotFoundException | StateSystemDisposedException |

                        TimeRangeException e) {
                    return false;
                }
                threadNodes.add(init);
            }
        }
        // sendUpdate(fStore);
        return true;
    }

    /**
     * Find the events called by a Stack call event,and add the segments of
     * those events as children of the segment of the caller
     *
     * @param node
     *            The segment of the stack call event(the parent) callStackQuark
     *            The quark of the segment parent ss The actual state system
     *            maxQuark The last quark in the state system
     * @since 2.0
     */
    private void findChildren(CalledFunction node, int callStackQuark, ITmfStateSystem ss, int maxQuark, FlameGraphNode flameGraphNode) {
        fStore.add(node);
        long curTime = node.getStart();
        long limit = node.getEnd();
        ITmfStateInterval interval = null;
        while (curTime < limit) {
            try {
                if (callStackQuark + 1 <= maxQuark) {
                    interval = ss.querySingleState(curTime, actualQuarks.get(callStackQuark + 1));
                } else {
                    return;
                }
            } catch (StateSystemDisposedException e) {
                e.printStackTrace();
            }
            if (interval != null) {
                ITmfStateValue stateValue = interval.getStateValue();
                if (!stateValue.isNull()) {
                    long intervalStart = interval.getStartTime();
                    long intervalEnd = interval.getEndTime();
                    CalledFunction segment = new CalledFunction(intervalStart, intervalEnd + 1, stateValue.unboxLong());
                    segment.setDepth(node.getDepth() + 1);
                    FlameGraphNode childNode = new FlameGraphNode(stateValue.unboxLong(), segment.getLength(), segment.getDepth());
                    // Search for the children with the next quark.
                    findChildren(segment, callStackQuark + 1, ss, maxQuark, childNode);
                    flameGraphNode.addChild(childNode);
                    node.addChild(segment);
                    // Calculate the statistics for a segment.
                    segmentStatistics.calculateStatistics(segment);
                    curTime = interval.getEndTime() + 1;
                } else {
                    curTime = interval.getEndTime() + 1;
                }
            }
        }
    }

    @Override
    protected Iterable<IAnalysisModule> getDependentAnalyses() {
        Set<IAnalysisModule> modules = new HashSet<>();
        /* Depends on the LTTng Kernel analysis modules */
        for (ITmfTrace trace : TmfTraceManager.getTraceSet(getTrace())) {
            trace = checkNotNull(trace);
            for (LttngUstCallStackAnalysis module : TmfTraceUtils.getAnalysisModulesOfClass(trace, LttngUstCallStackAnalysis.class)) {
                modules.add(module);
            }
        }
        return modules;
    }

    @Override
    public void addListener(@NonNull IAnalysisProgressListener listener) {
        fListeners.add(listener);
    }

    @Override
    public void removeListener(@NonNull IAnalysisProgressListener listener) {
        fListeners.remove(listener);
    }

    @Override
    public @NonNull Iterable<@NonNull ISegmentAspect> getSegmentAspects() {
        return Collections.emptyList();
    }

    @Override
    protected void canceling() {
    }

    @Override
    public @Nullable ISegmentStore<@NonNull ISegment> getSegmentStore() {
        return fStore;
    }

    /**
     * @param store
     *            The segment store
     */
    protected void sendUpdate(final ISegmentStore<@NonNull ISegment> store) {
        for (IAnalysisProgressListener listener : getListeners()) {
            listener.onComplete(this, store);
        }
    }

    /**
     * @return The listeners
     */
    protected Iterable<IAnalysisProgressListener> getListeners() {
        List<IAnalysisProgressListener> listeners = new ArrayList<>();
        for (Object listener : fListeners.getListeners()) {
            if (listener != null) {
                listeners.add((IAnalysisProgressListener) listener);
            }
        }
        return listeners;
    }

    /**
     * @return The segment store statistics
     */
    public CallStackStatistics getSegmentStatics() {
        return segmentStatistics;
    }

    /**
     * @return The nodes representing threads
     */
    public List<FlameGraphNode> getFirstNodes() {
        return threadNodes;
    }

    /**
     * @param ThreadNodes
     *      The nodes representing threads
     */
    public void setFirstNodes(List<FlameGraphNode> ThreadNodes) {
        threadNodes = ThreadNodes;
    }
}
