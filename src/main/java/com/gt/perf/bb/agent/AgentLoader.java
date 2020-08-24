package com.gt.perf.bb.agent;

import com.gt.perf.agent.ClassRecorder;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.Optional;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class AgentLoader {
    public static void premain(String agentArgs, Instrumentation inst) {
        String packagesToScan = Optional.ofNullable(System.getProperty("packages")).orElse("com");
        System.out.println("Packages to Scan: " + packagesToScan);

        String[] split = packagesToScan.split(",");
        ElementMatcher.Junction<NamedElement> matchingPackages = ElementMatchers.nameStartsWith(split[0]);

        for (int i = 1; i < split.length; i++) {
            System.out.println("Adding " + split[i]);
            matchingPackages = matchingPackages.and(ElementMatchers.nameStartsWith(split[i]));
        }

        new AgentBuilder.Default()
                .with(new AgentBuilder.InitializationStrategy.SelfInjection.Eager())
                .with(AgentBuilder.Listener.StreamWriting.toSystemError())
                .type(not(
                        nameContains("ClassRecorder")
                                .or(nameStartsWith("java")))
                        .and(matchingPackages)
                )
                .transform((builder, typeDescription, classLoader, module) -> builder
                        .method(not(isConstructor()
                                .and(isStatic())))
                        .intercept(MethodDelegation.to(TimingInterceptor.class))
                ).installOn(inst);

        Runtime.getRuntime().addShutdownHook(new Thread(ClassRecorder::printReport));
    }
}
