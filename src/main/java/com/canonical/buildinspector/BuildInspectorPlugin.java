package com.canonical.buildinspector;

import org.gradle.api.Plugin;
import org.gradle.api.invocation.Gradle;

public class BuildInspectorPlugin implements Plugin<Gradle> {

    @Override
    public void apply(Gradle target) {
        System.out.println("--- BuildInspector called ---");
    }
}