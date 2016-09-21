/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.timing.core.callgraph;

/**
 * This class represents one thread. It's used as a root node for the aggregated
 * tree created in the CallGraphAnalysis.
 *
 * @author Sonia Farrah
 */
public class ThreadNode extends AggregatedCalledFunction {

    private final long fId;
    private int fColor;

    /**
     * @param calledFunction
     *            the called function
     * @param maxDepth
     *            The maximum depth
     * @param id
     *            The thread id
     */
    public ThreadNode(AbstractCalledFunction calledFunction, int maxDepth, long id) {
        super(calledFunction, maxDepth);
        fId = id;
        fColor = 0;
    }

    /**
     * The thread id
     *
     * @return The thread id
     */
    public long getId() {
        return fId;
    }

    //Mod:
    @Override
    public String toString(){
        return Long.toString(fId) + getSymbol();
    }

    //This function does the differential:
    public void diff(ThreadNode o){

        if(getDuration() <= o.getDuration()){
            fColor = -1;
        }
        else{
            fColor = 1;
        }

    }

    //return the color of the comparison:
    public long getColor() {
        return fColor;
    }
}
