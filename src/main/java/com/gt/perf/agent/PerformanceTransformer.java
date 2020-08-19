package com.gt.perf.agent;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class PerformanceTransformer implements ClassFileTransformer {
    private final String targetClassName;
    private final ClassLoader classLoader;

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
                    System.out.println("Method Name: " + method.getName());
                    method.addLocalVariable("startTime", CtClass.longType);
                    method.insertBefore("startTime = System.currentTimeMillis();");
                    method.addLocalVariable("endTime", CtClass.longType);
                    method.addLocalVariable("opTime", CtClass.longType);

                    String endBlock = "endTime = System.currentTimeMillis();" +
                            "opTime = (endTime - startTime)/1000;" +
                            "System.out.println(\"Optime for " +  method.getName() + " (In Seconds): \" + opTime);";
                    method.insertAfter(endBlock);
                }

                byteCode = ctClass.toBytecode();
                ctClass.detach();

            } catch (NotFoundException | CannotCompileException | IOException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Unable to find class: " + finalTargetClassName);
        }

        return byteCode;
    }
}
