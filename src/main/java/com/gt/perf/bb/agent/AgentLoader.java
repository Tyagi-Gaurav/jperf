package com.gt.perf.bb.agent;

import com.gt.perf.agent.ClassRecorder;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;

public class AgentLoader {
    public static void premain(String agentArgs, Instrumentation inst) {
        new AgentBuilder.Default()
                .with(new AgentBuilder.InitializationStrategy.SelfInjection.Eager())
                //.with(AgentBuilder.Listener.StreamWriting.toSystemError())
                .type((ElementMatchers.not(ElementMatchers.nameContains("ClassRecorder")))
                        .and(ElementMatchers.not(ElementMatchers.nameStartsWith("java"))))
                .transform((builder, typeDescription, classLoader, module) -> builder
                        .method(ElementMatchers.not(ElementMatchers.isConstructor()
                                .and(ElementMatchers.isStatic())))
                        .intercept(Advice.to(Interceptor.class))
                ).installOn(inst);

        Runtime.getRuntime().addShutdownHook(new Thread(ClassRecorder::printReport));
    }
}
