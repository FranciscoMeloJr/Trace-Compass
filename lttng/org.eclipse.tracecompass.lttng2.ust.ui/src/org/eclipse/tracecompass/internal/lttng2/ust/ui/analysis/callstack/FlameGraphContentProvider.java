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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.ITimeGraphContentProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

import com.google.common.collect.Lists;

/**
 * Content provider for the flame graph view
 *
 * @author Sonia Farrah
 *
 */
public class FlameGraphContentProvider implements ITimeGraphContentProvider {

    private FlameGraphNode fModel;
    private static List<FlamegraphDepthEntry> FlameGraphEntries = new ArrayList<>();

    /**
     * @param firstNode
     *            The first node of the aggregation tree
     * @param maxDepth
     *            The depth of the tree
     * @param trace
     *            The active trace
     * @param childrenEntries
     *            The children list (event list) of an entry parent
     * @return The list of children
     */
    public List<FlamegraphDepthEntry> setData(FlameGraphNode firstNode, int maxDepth, ITmfTrace trace, List<FlamegraphDepthEntry> childrenEntries) {
        // Build the entry list
        List<FlamegraphDepthEntry> childrenEntries2 = childrenEntries;
        for (int i = 0; i < maxDepth; i++) {
            FlamegraphDepthEntry entry = new FlamegraphDepthEntry(String.valueOf(i), 0, trace.getEndTime().toNanos() - trace.getStartTime().toNanos(), i);
            if (i >= childrenEntries2.size()) {
                childrenEntries2.add(entry);
            }
        }
        final int modulo = FlameGraphPresentationProvider.NUM_COLORS / 2;
        int value = String.valueOf(firstNode.getAddress()).hashCode() % modulo + modulo;
        FlamegraphDepthEntry entry = NonNullUtils.checkNotNull(childrenEntries2.get(0));
        entry.addEvent(new FlamegraphEvent(entry, firstNode.getTimestamp(), firstNode.getDuration(), value, firstNode.getAddress(), 1, 1));
        // Build the event list for each entry(depth)
        addEvent(firstNode, childrenEntries2);
        return childrenEntries2;
    }

    /**
     * Build the events list for each entry(depth)
     *
     * @param FlameGraphNode
     *            The node of the aggregation tree
     */
    private void addEvent(FlameGraphNode node, List<FlamegraphDepthEntry> childrenEntries) {

        final int modulo = FlameGraphPresentationProvider.NUM_COLORS / 2;
        Long childrenDuration = node.getTimestamp();
        // Sort the callees by duration
        Comparator<FlameGraphNode> byFlameGraphDuration = (e2, e1) -> Long.compare(
                e1.getDuration(), e2.getDuration());
        Object[] children = node.getChildren().values().stream().sorted(byFlameGraphDuration).toArray();
        for (Object child : children) {
            FlameGraphNode function = (FlameGraphNode) child;
            FlamegraphDepthEntry entry = NonNullUtils.checkNotNull(childrenEntries.get(function.getDepth()));
            int value = String.valueOf(node.getAddress()).hashCode() % modulo + modulo;
            function.setTimeStamp(childrenDuration);
            entry.addEvent(new FlamegraphEvent(entry, childrenDuration, function.getDuration(), value, function.getAddress(), (double) (function.getDuration()) / node.getDuration(), function.getNbreCalls()));
            childrenDuration += function.getDuration();
        }
        // Build the event list for the next depth
        for (Object child : children) {
            FlameGraphNode function = (FlameGraphNode) child;
            addEvent(function, childrenEntries);
        }

    }

    @Override
    public boolean hasChildren(Object element) {
        return fModel.getChildren().isEmpty();
    }

    @Override
    public ITimeGraphEntry[] getElements(Object inputElement) {
        // get the traces
        FlameGraphEntries.clear();
        // Get the root of each thread
        List<FlameGraphNode> threadNodes = (List<FlameGraphNode>) inputElement;
        ITmfTrace activeTrace = TmfTraceManager.getInstance().getActiveTrace();
        if (activeTrace == null) {
            return new ITimeGraphEntry[0];
        }
        for (FlameGraphNode threadNode : threadNodes) {
            buildChildrenEntries(threadNode, activeTrace);
        }
        return FlameGraphEntries.toArray(new ITimeGraphEntry[FlameGraphEntries.size()]);
    }

    /**
     * Build the events list for one thread
     *
     * @param FlameGraphNode
     *            The node of the aggregation tree
     */
    private void buildChildrenEntries(FlameGraphNode threadNode, ITmfTrace activeTrace) {
        FlamegraphDepthEntry parentEntry = new FlamegraphDepthEntry("", 0, activeTrace.getEndTime().toNanos() - activeTrace.getStartTime().toNanos(), FlameGraphEntries.size()); //$NON-NLS-1$
        List<FlamegraphDepthEntry> childrenEntries = new ArrayList<>();
        Long timeStamp = 0L;
        //Sort children by duration
        Comparator<FlameGraphNode> byFlameGraphDuration = (e2, e1) -> Long.compare(
                e1.getDuration(), e2.getDuration());
        Object[] children = threadNode.getChildren().values().stream().sorted(byFlameGraphDuration).toArray();
        for (Object Function : children) {
            @NonNull
            FlameGraphNode functionEntry = (FlameGraphNode) Function;
            functionEntry.setTimeStamp(timeStamp);
            childrenEntries = setData(functionEntry, functionEntry.getMaxDepth(), activeTrace, childrenEntries);
            timeStamp += functionEntry.getDuration();
        }
        childrenEntries = Lists.reverse(childrenEntries);
        for (FlamegraphDepthEntry child : childrenEntries) {
            parentEntry.addChild(child);
        }
        //parentEntry.addEvent(new FlamegraphEvent(parentEntry, 0, timeStamp,200, timeStamp, 1, 1));
        parentEntry.setName(threadNode.getName());
        FlameGraphEntries.add(parentEntry);
    }

    @Override
    public ITimeGraphEntry[] getChildren(Object parentElement) {
        // get the entries of a trace
        if (parentElement instanceof FlameGraphNode) {
            return FlameGraphEntries.toArray(new TimeGraphEntry[FlameGraphEntries.size()]);
        }
        return null;
    }

    @Override
    public ITimeGraphEntry getParent(Object element) {
        return null;
    }

    @Override
    public void dispose() {

    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

    }

}
