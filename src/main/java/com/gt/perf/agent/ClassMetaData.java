package com.gt.perf.agent;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ClassMetaData {
    private Class<?> clazz;
    private ClassLoader classLoader;
}
