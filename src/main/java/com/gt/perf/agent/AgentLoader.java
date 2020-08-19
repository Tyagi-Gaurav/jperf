package com.gt.perf.agent;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AgentLoader {
    public static void premain(String agentArgs, Instrumentation inst) {
        transformPackage("com.gt.main", inst);
    }

    private static void transformPackage(String packageName, Instrumentation inst) {
        String packagePath = packageName.replaceAll("\\.", "/");
        try {
            System.out.println("Looking for package: " + packagePath);
            System.out.println("Classpath: " + System.getProperty("java.class.path"));
            String[] jarFiles = System.getProperty("java.class.path").split(":");
            List<String> classArrayList = new ArrayList<>();
            for (String jarfile : jarFiles) {
                //while (resources.hasMoreElements()) {
                JarFile jarFile = new JarFile(jarfile);
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry x = entries.nextElement();
                    String name = x.getName();
                    if (!x.isDirectory() && name.endsWith(".class") && name.startsWith(packagePath)) {
                        name = name.replaceAll("/", "\\.").replaceAll(".class", "");
                        System.out.println(name);
                        transformClass(name, inst);
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void transformClass(
            String className, Instrumentation instrumentation) {
        Class<?> targetCls = null;
        ClassLoader targetClassLoader = null;
        // see if we can get the class using forName
        try {
            targetCls = Class.forName(className);
            targetClassLoader = targetCls.getClassLoader();
            transform(targetCls, targetClassLoader, instrumentation);
            return;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        // otherwise iterate all loaded classes and find what we want
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            if (clazz.getName().equals(className)) {
                targetCls = clazz;
                targetClassLoader = targetCls.getClassLoader();
                transform(targetCls, targetClassLoader, instrumentation);
                return;
            }
        }
        throw new RuntimeException(
                "Failed to find class [" + className + "]");
    }

    private static void transform(
            Class<?> clazz,
            ClassLoader classLoader,
            Instrumentation instrumentation) {
        PerformanceTransformer dt = new PerformanceTransformer(clazz.getName(), classLoader);
        instrumentation.addTransformer(dt, true);
        try {
            instrumentation.retransformClasses(clazz);
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Transform failed for: [" + clazz.getName() + "]", ex);
        }
    }
}
