package org.eclipse.tracecompass.internal.analysis.os.linux.core.profile;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.IProfileData;
import org.eclipse.tracecompass.segmentstore.core.ISegment;

/**
 * @author frank
 * @since 2.0 This Class is for tests related with CCTAnalysis
 *
 */
public class ProfileData implements IProfileData {

    private String fLabel;
    int fWeight;
    int fTestValue = -1;
    long fstartTime;
    long fendTime;
    long fDuration = 0; // endTime - startTime
    ArrayList<Long> eachRun;
    ArrayList<Integer> eachInfo; // cache, instruction and other informations

    // All the info:
    private HashMap<String, Double> info = new HashMap();

    // Constructor:
    public ProfileData(int weight, String label) {
        if (weight == 0) {
            fWeight = 0;
        } else {
            fWeight = weight;
        }
        fLabel = label;
        fDuration = 0;
        eachRun = new ArrayList<>();
        eachInfo = new ArrayList<>();
        info = new HashMap();
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
        fTestValue = x;
        eachRun = new ArrayList<>();
        eachInfo = new ArrayList<>();
    }

    // Constructor:
    public ProfileData(int weight, String label, Long start, Long end) {
        this(weight, label, start, end, -1);
    }

    // Constructor:
    public ProfileData(long weight, String label) {
        fWeight = (int) weight;
        fLabel = label;
        fDuration = 0;
        eachRun = new ArrayList<>();
        eachInfo = new ArrayList<>();
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
            // Test to merge and save the infor
            this.eachRun.add(data.getDuration());
            for (String key : data.getInfo().keySet()) {
                if (this.info.containsKey(key)) {
                    // merge with mean:
                    double newValue = (data.getInfo().get(key) + this.getInfo().get(key)) / 2;
                    this.info.put(key, newValue);
                } else {
                    // add
                    this.info.put(key, data.getInfo().get(key));
                }
            }
        }

    }

    @Override
    public int minus(IProfileData other, int threshold) {
        double mult = (100 + threshold) / 100;

        if (!(other instanceof ProfileData)) {
            throw new IllegalArgumentException("wrong type for minus operation");
        }
        ProfileData data = (ProfileData) other;
        if (fLabel.equals(data.getLabel())) {
            // gray:
            if (this.fDuration == data.getDuration()) {
                return 0;
            }
            // red:
            if (this.fDuration > (data.getDuration())) {
                if (this.fDuration > data.getDuration() * mult) {
                    return 1;
                }
            }
            // green:
            if (this.fDuration < data.getDuration()) {
                if (this.fDuration * mult < data.getDuration()) {
                    return -1;
                }
            }
            // To see the colors:
            // this.fDuration -= data.getDuration();
            // this.fWeight -= data.getDuration();
        }

        // gray:
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
        fTestValue = nextInt;
    }

    public int getX() {
        return fTestValue;
    }

    public void setDuration(long l) {
        fDuration = l;
        eachRun.add(l);
    }

    public long getDuration() {
        return fDuration;
    }

    public void addDuration(long duration) {
        fDuration += duration;
        eachRun.add(duration);
    }

    @Override
    public int minus(IProfileData other) {

        if (!(other instanceof ProfileData)) {
            throw new IllegalArgumentException("wrong type for minus operation");
        }
        ProfileData data = (ProfileData) other;
        if (fLabel.equals(data.getLabel())) {
            // gray:
            if (this.fDuration == data.getDuration()) {
                return 0;
            }
            // red:
            if (this.fDuration > (data.getDuration())) {
                return 1;

            }
            // green:
            if (this.fDuration < data.getDuration()) {
                return -1;
            }
            // To see the colors:
            // this.fDuration -= data.getDuration();
            // this.fWeight -= data.getDuration();
        }
        // gray:
        return 0;
    }

    public void addInfo(Integer intInfo) {
        eachInfo.add(intInfo);
    }

    // Hash:
    public void setInfo(String stringInfo, Double valueNew) {
        if (!info.containsKey(stringInfo)) {
            info.put(stringInfo, valueNew);
        } else {
            Double value = info.get(stringInfo);
            Double delta = valueNew - value;
            info.put(stringInfo, delta);
        }
    }

    //Return information from the hash:
    public HashMap<String, Double> getInfo() {
        return info;
    }

    //Add information to the hash:
    public Double getInfo(String stringInfo) {
        return info.get(stringInfo);
    }
}
