package com.gt.perf.bb.agent;

import com.gt.perf.agent.ClassRecorder;
import net.bytebuddy.asm.Advice;

public class Interceptor {
    //static Map<String, ClassRecorder> classRecorderMap = new HashMap<>();

    @Advice.OnMethodEnter
    static long enter(@Advice.Origin String method) {
        long start = System.currentTimeMillis();
        return start;
    }

    @Advice.OnMethodExit
    static void exit(@Advice.Origin String method, @Advice.Enter long start) {
        long end = System.currentTimeMillis();
        //System.out.println(method + " took " + (end - start) + " milliseconds ");
        String name = "Test";
        ClassRecorder.addCallDuration(method, end - start);
        //classRecorder.addCallDuration(method, end - start);
    }
}
