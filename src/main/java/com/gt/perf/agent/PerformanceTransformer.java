package com.gt.perf.agent;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Arrays;

public class PerformanceTransformer implements ClassFileTransformer {
    private final String targetClassName;
    private final ClassLoader classLoader;
    //private final Logger

    public PerformanceTransformer(String targetClassName, ClassLoader classLoader) {
        this.targetClassName = targetClassName;
        this.classLoader = classLoader;
    }

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {
        byte[] byteCode = classfileBuffer;
        //System.out.println("Called to transform class: " + className);
        String finalTargetClassName = this.targetClassName.replaceAll("\\.", "/");
        if (!className.equals(finalTargetClassName)) {
            return byteCode;
        }

        if (loader.equals(classLoader)) {
            try {
                System.out.println("Found class: " + finalTargetClassName);
                ClassPool classPool = ClassPool.getDefault();
                CtClass ctClass = classPool.get(this.targetClassName);
                CtMethod[] declaredMethods = ctClass.getDeclaredMethods();
                for (CtMethod method : declaredMethods) {
                    if (method.getName().equals("doSomething")) {
                        System.out.println("Method Name: " + method.getName());
                        method.addLocalVariable("startTime", CtClass.longType);
                        method.insertBefore("startTime = System.currentTimeMillis();");
                        method.addLocalVariable("endTime", CtClass.longType);
                        method.addLocalVariable("opTime", CtClass.longType);

                        String endBlock = "endTime = System.currentTimeMillis();" +
                                "opTime = (endTime - startTime)/1000;" +
                                "System.out.println(\"Optime (In Seconds): \" + opTime);";
                        method.insertAfter(endBlock);

                        byteCode = ctClass.toBytecode();
                        ctClass.detach();
                        System.out.println("Changed byte code for class");
                    }
                }
            } catch (NotFoundException | CannotCompileException | IOException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("Unable to find class: " + finalTargetClassName);
        }

        return byteCode;
    }
}
