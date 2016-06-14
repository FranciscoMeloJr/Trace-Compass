package org.eclipse.tracecompass.internal.analysis.os.linux.core.profile;

public interface IProfileData {

    public IProfileData minus(IProfileData other);
    public void merge(IProfileData other);
    public boolean equals(IProfileData other);
    public String getLabel();
    public int getWeight();
}
