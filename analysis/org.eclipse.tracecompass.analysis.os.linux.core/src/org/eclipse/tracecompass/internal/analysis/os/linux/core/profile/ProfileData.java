package org.eclipse.tracecompass.internal.analysis.os.linux.core.profile;

import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.IProfileData;

/**
 * @author frank
 * @since 2.0 This Class is for tests related with CCTAnalysis
 *
 */
public class ProfileData implements IProfileData {

    private String fLabel;
    int fWeight;
    int fX;
    long fstartTime;
    long fendTime;
    long fDuration = 0; // endTime - startTime

    // Constructor:
    public ProfileData(int weight, String label) {
        if (weight == 0) {
            fWeight = 0;
        } else {
            fWeight = weight;
        }
        fLabel = label;
        fDuration = 0;
    }

    // Constructor:
    public ProfileData(int weight, String label, Long start, Long end, int x) {
        if (weight == 0) {
            fWeight = 0;
        } else {
            fWeight = weight;
        }
        fLabel = label;
        if (start != null) {
            fstartTime = start;
        }
        if (end != null) {
            fendTime = end;
        }
        fDuration = 0;
        // test:
        fX = x;
    }

    // Constructor:
    public ProfileData(long weight, String label) {
        fWeight = (int) weight;
        fLabel = label;
        fDuration = 0;
    }

    // Add to the weight:
    public void addWeight(int value) {
        fWeight += value;
    }

    @Override
    public void merge(IProfileData other) {
        if (!(other instanceof ProfileData)) {
            throw new IllegalArgumentException("wrong type for merge operation");
        }
        ProfileData data = (ProfileData) other;
        if (fLabel.equals(data.getLabel())) {
            this.fDuration += data.getDuration();
            this.fWeight += data.getDuration();
        }

    }

    @Override
    public int minus(IProfileData other) {
        if (!(other instanceof ProfileData)) {
            throw new IllegalArgumentException("wrong type for minus operation");
        }
        ProfileData data = (ProfileData) other;
        if (fLabel.equals(data.getLabel())) {
            //gray:
            if(this.fDuration == data.getDuration()) {
                return 0;
            }
           //red:
            if(this.fDuration > data.getDuration()) {
                return 1;
            }
            //green:
            if(this.fDuration < data.getDuration()) {
                return -1;
            }
        //To see the colors:
            //this.fDuration -= data.getDuration();
            //this.fWeight -= data.getDuration();
        }
        //gray:
        return 0;
    }

    @Override
    public boolean equals(IProfileData other) {
        if (!(other instanceof ProfileData)) {
            throw new IllegalArgumentException("wrong type for minus operation");
        }
        ProfileData data = (ProfileData) other;
        if (fLabel.equals(data.getLabel())) {
            if (fWeight == data.getWeight()) {
                return true;
            }
        }
        return false;
    }

    // Change for StackCall
    public void setStartTime(long start) {
        fstartTime = start;
    }

    public void setEndTime(long end) {
        fendTime = end;
    }

    public long getStartTime() {
        return fstartTime;
    }

    public long getEndTime() {
        return fendTime;
    }

    @Override
    public String getLabel() {
        return fLabel;
    }

    @Override
    public int getWeight() {
        return fWeight;
    }

    @Override
    public String toString() {
        return new String(fDuration + " " + fLabel);
    }

    public void setMetric(int nextInt) {
        fX = nextInt;
    }

    public int getX() {
        return fX;
    }

    public void setDuration(long l) {
        fDuration = l;

    }

    public long getDuration() {
        return fDuration;

    }

    public void addDuration(long duration) {
        fDuration += duration;

    }
}
