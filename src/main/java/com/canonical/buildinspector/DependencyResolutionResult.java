package com.canonical.buildinspector;

import org.gradle.api.artifacts.component.ComponentIdentifier;

import java.util.Set;

/**
 * Dependency lookup result for maven pom
 */
public class DependencyResolutionResult {
    private Set<ComponentIdentifier> dependencies;
    private Set<ComponentIdentifier> dependencyManagement;
    public DependencyResolutionResult(Set<ComponentIdentifier> dependencies, Set<ComponentIdentifier> dependencyManagement) {
        this.dependencies = dependencies;
        this.dependencyManagement = dependencyManagement;
    }

    public Set<ComponentIdentifier> getDependencies() { return this.dependencies; }
    public Set<ComponentIdentifier> getDependencyManagement() { return this.dependencyManagement; }
}
