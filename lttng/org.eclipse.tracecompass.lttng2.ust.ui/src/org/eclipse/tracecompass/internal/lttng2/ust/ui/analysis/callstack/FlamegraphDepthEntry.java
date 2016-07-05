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

import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * An entry, or row, in the FlameGraph view
 *
 * @author esonfar
 */
public class FlamegraphDepthEntry extends TimeGraphEntry {
    private String fFunctionName;
    private int depth;

    /**
     * @param name
     *            name of an entry
     * @param startTime
     *            Start time of an entry
     * @param endTime
     *            The end time of an entry
     * @param Depth
     *            The Depth of an entry
     */
    public FlamegraphDepthEntry(String name, long startTime, long endTime, int Depth) {
        super(name, startTime, endTime);
        setDepth(Depth);
    }

    /**
     * @return The function 's name
     */
    public String getFunctionName() {
        return fFunctionName;
    }

    /**
     * @return The depth of a function
     */
    public int getDepth() {
        return depth;
    }

    /**
     * @param Depth
     *            The depth of a function
     */
    public void setDepth(int Depth) {
        depth = Depth;
    }
}
