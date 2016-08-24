package org.eclipse.tracecompass.internal.analysis.os.linux.core.profile;

/*
 * This interface is related with an interface for profiling data
 */
public interface IProfileData {

    public int minus(IProfileData other);
    public int minus(IProfileData other, int th);
    public void merge(IProfileData other);
    public boolean equals(IProfileData other);
    public String getLabel();
    public int getWeight();
    @Override
    public String toString();
}
