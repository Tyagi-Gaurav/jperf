package com.gt.perf.agent;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class ClassRecorder {
    @NonNull
    private String className;

    private static Map<String, MethodRecorderMetaData> methodRecorderMetaDataMap = new HashMap<>();

    public static void addCallDuration(String method, long duration) {
        MethodRecorderMetaData methodRecorderMetaData =
                methodRecorderMetaDataMap.getOrDefault(method, new MethodRecorderMetaData());
        methodRecorderMetaData.addCallDuration(duration);
        methodRecorderMetaDataMap.put(method, methodRecorderMetaData);
    }

    public static void printReport() {
        for (Map.Entry<String, MethodRecorderMetaData> entry : methodRecorderMetaDataMap.entrySet()) {
            System.out.println(String.format("Method: %s, details: %s", entry.getKey(), entry.getValue()));
        }
    }

    private static class MethodRecorderMetaData {
        private long callCount = 0;
        private long callDurationTotal = 0;
        private long maxCallDuration = Long.MIN_VALUE;

        public void addCallDuration(long duration) {
            this.callCount++;
            //System.out.println("Add Duration Called: " + duration);
            if (maxCallDuration < duration) {
                maxCallDuration = duration;
            }

            callDurationTotal += maxCallDuration;
        }

        @Override
        public String toString() {
            return String.format("Call Count: %d, Total Call Duration (In Millis): %d, Avg Call Duration (In Millis): %f",
                    callCount, callDurationTotal, ((double)callDurationTotal/(double)callCount));
        }
    }
}
