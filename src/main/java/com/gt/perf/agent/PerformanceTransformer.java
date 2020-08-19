package com.gt.perf.agent;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Map;

public class PerformanceTransformer implements ClassFileTransformer {
    private final Map<String, ClassMetaData> classMetaData;

    public PerformanceTransformer(Map<String, ClassMetaData> classMetaData) {
        this.classMetaData = classMetaData;
    }

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {
        byte[] byteCode = classfileBuffer;
        String finalTargetClassName = className.replaceAll("/", "\\.");
        //System.out.println("Looking for class Name: " + finalTargetClassName);
        if (!classMetaData.containsKey(finalTargetClassName)) {
            return byteCode;
        }
        System.out.println("Found Class Name in Map: " + finalTargetClassName);

        if (loader.equals(classMetaData.get(finalTargetClassName).getClassLoader())) {
            try {
                System.out.println("Found class: " + finalTargetClassName);
                ClassPool classPool = ClassPool.getDefault();
                CtClass ctClass = classPool.get(finalTargetClassName);
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
            System.err.println("Loader Actual: " + loader);
            System.err.println("Loader Expected: " + classMetaData.get(finalTargetClassName));
        }

        return byteCode;
    }
}
