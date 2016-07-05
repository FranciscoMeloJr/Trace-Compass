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
import java.util.Comparator;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.SegmentComparators;

import com.google.common.collect.Ordering;

/**
 * A call Stack event represented as an {@link ISegment}.
 *
 * @author Sonia Farrah
 * @since 2.0
 */
public class CalledFunction implements ISegment {

    private static final long serialVersionUID = -3257452887960883177L;
    private static final Comparator<ISegment> COMPARATOR;
    static {
        /*
         * checkNotNull() has to be called separately, or else it breaks the
         * type inference.
         */
        Comparator<ISegment> comp = Ordering.from(SegmentComparators.INTERVAL_START_COMPARATOR).compound(SegmentComparators.INTERVAL_END_COMPARATOR);
        COMPARATOR = checkNotNull(comp);
    }

    private final long fStart;
    private final long fEnd;
    private final long faddr;
    private long finalSelfTime;
    private int depth;
    private ArrayList<CalledFunction> children = new ArrayList<>();
    private CalledFunction parent = null;

    /**
     * Create a new segment.
     *
     * The end position should be equal to or greater than the start position.
     *
     * @param start
     *            Start position of the segment
     * @param end
     *            End position of the segment
     * @param address
     *            The address of the call stack event
     */
    public CalledFunction(long start, long end, long address) {
        if (end < start) {
            throw new IllegalArgumentException();
        }
        fStart = start;
        fEnd = end;
        faddr = address;
        setFinalSelfTime(fEnd - fStart);
    }

    @Override
    public long getStart() {
        return fStart;
    }

    @Override
    public long getEnd() {
        return fEnd;
    }

    @Override
    public int compareTo(@Nullable ISegment o) {
        if (o == null) {
            throw new IllegalArgumentException();
        }
        return COMPARATOR.compare(this, o);
    }

    @Override
    public String toString() {
        return new String('[' + String.valueOf(fStart) + ", " + String.valueOf(fEnd) + ']'); //$NON-NLS-1$
    }

    /**
     * The address of the call stack event.
     *
     * @return The address
     *
     */
    public long getAddr() {
        return faddr;
    }

    /**
     * The children of the segment
     *
     * @return The children
     *
     */
    public List<CalledFunction> getChildren() {
        return children;
    }

    /**
     * The children of the segment
     *
     * @param Children
     *
     */
    public void setChildren(ArrayList<CalledFunction> Children) {
        children = Children;
    }

    /**
     * The segment's parent
     *
     * @return The parent
     *
     */
    public CalledFunction getParent() {
        return parent;
    }

    /**
     * The segment's parent
     *
     * @param Parent
     *            The parent of the segment
     *
     */
    public void setParent(CalledFunction Parent) {
        parent = Parent;
    }

    /**
     * Add the child to the segment's children, and subtract the child's
     * duration to the duration of the segment so we can calculate its self
     * time.
     *
     * @param child
     *            The child to add to the segment's children
     */
    public void addChild(CalledFunction child) {
        child.setParent(this);
        children.add(child);
        substractChildDuration(child.fEnd - child.fStart);
    }

    /**
     * Subtract the child's duration to the duration of the segment.
     *
     * @param childDuration
     *            The child's duration
     *
     */
    private void substractChildDuration(long childDuration) {
        finalSelfTime -= childDuration;
    }

    /**
     * The segment's self Time
     *
     * @return finalSelfTime The self time
     */
    public long getFinalSelfTime() {
        return finalSelfTime;
    }

    /**
     * The segment's self time
     *
     * @param FinalSelfTime
     *            The self time
     */
    public void setFinalSelfTime(long FinalSelfTime) {
        finalSelfTime = FinalSelfTime;
    }

    /**
     * @return
     *      The depth of a function
     */
    public int getDepth() {
        return depth;
    }

    /**
     * @param Depth
     *      The depth of a function
     */
    public void setDepth(int Depth) {
        depth = Depth;
    }
}
