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

import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;

/**
 * Time Event implementation specific to the FlameGraph View (it represents a
 * function in a certain depth)
 *
 * @author esonfar
 *
 */
public class FlamegraphEvent extends TimeEvent {

    private Long address;
    private int nbreCalls;
    private Double percentage;

    /**
     * @param source
     *            The Entry
     * @param beginTime
     *            The event's begin time
     * @param totalTime
     *            The event's total time
     * @param value
     *            The event's value
     * @param Address
     *            The event's address
     * @param Percentage
     *            The event's percentage
     * @param NbreCalls
     *            The event's number of calls
     */
    public FlamegraphEvent(Object source, long beginTime, long totalTime, int value, Long Address, double Percentage, int NbreCalls) {
        super((ITimeGraphEntry) source, beginTime, totalTime, value);
        address = Address;
        nbreCalls = NbreCalls;
        percentage = Percentage;
    }

    /**
     * @return The event's address
     */
    public Long getAddress() {
        return address;
    }

    /**
     * @param Address
     *            The event's address
     */
    public void setAdress(Long Address) {
        address = Address;
    }

    /**
     * @return The event's number of a calls
     */
    public int getNbreCalls() {
        return nbreCalls;
    }

    /**
     * @param NbreCalls
     *            The number of calls of the event
     */
    public void setNbreCalls(int NbreCalls) {
        nbreCalls = NbreCalls;
    }

    /**
     * @return The percentage of the event
     */
    public Double getPercentage() {
        return percentage;
    }

    /**
     * @param Percentage
     *            The event's percentage
     */
    public void setPercentage(Double Percentage) {
        percentage = Percentage;
    }
}
