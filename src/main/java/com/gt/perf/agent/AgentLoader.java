package com.gt.perf.agent;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

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

            List<String> classesToTransform = new ArrayList<>();

            for (String jarfile : jarFiles) {
                JarFile jarFile = new JarFile(jarfile);
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry x = entries.nextElement();
                    String name = x.getName();
                    if (!x.isDirectory() && name.endsWith(".class") && name.startsWith(packagePath)) {
                        name = name.replaceAll("/", "\\.").replaceAll(".class", "");
                        classesToTransform.add(name);
                    }
                }
            }

            Map<String, ClassMetaData> classMetaData = transformClass(classesToTransform, inst);
            transform(classMetaData, inst);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static Map<String, ClassMetaData> transformClass(
            List<String> classz, Instrumentation instrumentation) {
        Class<?> targetCls = null;
        ClassLoader targetClassLoader = null;
        Map<String, ClassMetaData> classesToTransform = new HashMap<>();
        // see if we can get the class using forName
        for(String className : classz) {
            try {
                targetCls = Class.forName(className);
                targetClassLoader = targetCls.getClassLoader();
                System.out.println("Adding to map: " + targetCls.getName());
                classesToTransform.put(targetCls.getName(), new ClassMetaData(targetCls, targetClassLoader));
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException(
                        "Failed to find class [" + className + "]");
            }
        }

        return classesToTransform;
    }

    private static void transform(
            Map<String, ClassMetaData> classMetaData,
            Instrumentation instrumentation) {
        PerformanceTransformer dt = new PerformanceTransformer(classMetaData);
        instrumentation.addTransformer(dt, true);
        try {
            List<Class<?>> collect = classMetaData.values().stream()
                    .map(ClassMetaData::getClazz)
                    .collect(Collectors.toList());
            instrumentation.retransformClasses(collect.toArray(new Class[0]));
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(
                    "Transform failed.", ex);
        }
    }
}
