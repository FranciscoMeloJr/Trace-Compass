package org.eclipse.tracecompass.internal.tmf.core.profile;

public interface IProfileData {

    public IProfileData minus(IProfileData other);
    public void merge(IProfileData other);
}