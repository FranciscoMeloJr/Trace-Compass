package org.eclipse.tracecompass.internal.lttng2.ust.ui;

public interface IProfileData {

    public IProfileData minus(IProfileData other);
    public void merge(IProfileData other);
    public boolean equals(IProfileData other);
    public String getLabel();
    public int getWeight();
    @Override
    public String toString();
}