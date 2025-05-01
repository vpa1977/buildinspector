package com.canonical.buildinspector;

import org.apache.tools.ant.types.ModuleVersion;
import org.gradle.BuildListener;
import org.gradle.BuildResult;
import org.gradle.api.Plugin;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.result.ArtifactResolutionResult;
import org.gradle.api.artifacts.result.ArtifactResult;
import org.gradle.api.artifacts.result.ComponentArtifactsResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.execution.TaskExecutionGraph;
import org.gradle.api.execution.TaskExecutionGraphListener;
import org.gradle.api.execution.TaskExecutionListener;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.TaskState;
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier;
import org.gradle.maven.MavenModule;
import org.gradle.maven.MavenPomArtifact;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BuildInspectorPlugin implements Plugin<Gradle> {

    private final Logger logger = Logging.getLogger(PomDependencyReader.class);

    @Override
    public void apply(Gradle target) {

        System.out.println("--- BuildInspector called ---");
        target.getTaskGraph().whenReady(graph -> {
            graph.addTaskExecutionListener(new TaskExecutionListener() {
                @Override
                public void beforeExecute(Task task) {
                    System.out.println("Before execute "+ task.getGroup() + ":"+ task.getName());
                }

                @Override
                public void afterExecute(Task task, TaskState state) {
                    System.out.println("After execute "+ task.getGroup() + ":"+ task.getName() + " result "+ state);
                }
            });
        });

        target.beforeSettings( settings -> {

            try {
                dumpSettings("beforeSettings", settings);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        target.settingsEvaluated( settings -> {
            try {
                dumpSettings("settingsEvaluated", settings);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        target.addBuildListener(new BuildListener() {
            @Override
            public void settingsEvaluated(Settings settings) {
                System.out.println("--- Settings evaluated " + settings.getSettingsDir());
            }

            @Override
            public void projectsLoaded(Gradle gradle) {
                System.out.println("--- Projects are loaded ---");

            }

            @Override
            public void projectsEvaluated(Gradle gradle) {
                System.out.println("--- Projects evaluated ---");
            }

            @Override
            public void buildFinished(BuildResult result) {
                System.out.println("--- Build Complete ---");
            }
        });

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void copyConfiguration(PomDependencyReader pomDependencyReader,
                                   Configuration files,
                                   DependencyHandler handler) throws IOException {
        HashSet<ComponentIdentifier> resolved = new HashSet<>();
        HashSet<ComponentIdentifier> workQueue = new HashSet<>();
        // resolve and copy POM files
        handler.getComponents().all( details -> {
            ModuleVersionIdentifier result = details.getId();
            ModuleComponentIdentifier id = DefaultModuleComponentIdentifier.newId(DefaultModuleIdentifier.newId(result.getGroup(), result.getName()), result.getVersion());
            if (workQueue.contains(id) || resolved.contains(id))
                return;
            workQueue.add(id);
            System.out.println("--- component: "+ id);
            Set<String> scopes = new HashSet<>(Arrays.asList("compile", "import", "runtime"));

            while (!workQueue.isEmpty()) {
                ArtifactResolutionResult artifacts = handler
                        .createArtifactResolutionQuery()
                        .forComponents(workQueue)
                        .withArtifacts(MavenModule.class, new Class[]{MavenPomArtifact.class})
                        .execute();
                resolved.addAll(workQueue);
                workQueue.clear();
                for (ComponentArtifactsResult component : artifacts.getResolvedComponents()) {
                    if (component.getId() instanceof ModuleComponentIdentifier) {
                        for (ArtifactResult artifact : component.getArtifacts(MavenPomArtifact.class)) {
                            logger.warn("Found artifact " + artifact.getId());
                            // resolve maven dependencies to fetch poms
                            DependencyResolutionResult dependencies = pomDependencyReader.read(((ResolvedArtifactResult) artifact).getFile(), scopes);
                            // add unresolved modules to workQueue
                            dependencies.getDependencies().stream()
                                    .filter(x -> !resolved.contains(x))
                                    .forEach(workQueue::add);
                        }
                    }
                }
            }
        });
    }

    private void dumpSettings(String label, Settings settings) throws IOException {
        System.out.println("---  "+label + " " + settings.getSettingsDir());
        settings.getBuildscript().getRepositories().all(x -> {
            System.out.println("buildscript repository " + x.getName());
        });

        final PomDependencyReader buildScriptDependencyReader = new PomDependencyReader(settings.getBuildscript().getDependencies(),
                settings.getBuildscript().getConfigurations());
        System.out.println("---  investigate configurations");
        for (Configuration config : settings.getBuildscript().getConfigurations()) {
            System.out.println("---  configuration: "+ config.getName());
            copyConfiguration(buildScriptDependencyReader, config, settings.getBuildscript().getDependencies());
        }

        System.out.println("---");
    }
}