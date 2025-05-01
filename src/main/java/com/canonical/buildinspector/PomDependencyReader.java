package com.canonical.buildinspector;

import org.apache.commons.text.StringSubstitutor;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Read dependencies of the POM file and return an array of ComponentIdentifiers
 */
public class PomDependencyReader {
    private final Logger logger = Logging.getLogger(PomDependencyReader.class);
    private final PomResolver pomResolver;
    private final DefaultModelBuilder builder;

    /**
     * Constructs POM dependency reader
     * @param handler - DependencyHandler to construct new dependencies
     * @param container - ConfigurationContainer to create detached configurations
     */
    public PomDependencyReader(DependencyHandler handler, ConfigurationContainer container) {
        DefaultModelBuilderFactory factory = new DefaultModelBuilderFactory();
        this.builder = factory.newInstance();
        this.builder.setModelValidator(new SilentModelValidator());
        this.pomResolver = new PomResolver(handler, container);
    }

    /**
     * Read pom file and return dependencies
     * @param pom - POM file
     * @return ComponentIdentifiers for dependencies
     */
    DependencyResolutionResult read(File pom, Set<String> scopes) {
        HashSet<ComponentIdentifier> toLookup = new HashSet<>();
        HashSet<ComponentIdentifier> bomLookup = new HashSet<>();
        System.out.println("Reading "+ pom);
        try {
            ModelBuildingRequest req = new DefaultModelBuildingRequest();
            req.setModelResolver(pomResolver);
            req.setPomFile(pom);
            req.setSystemProperties(System.getProperties());
            req.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
            ModelBuildingResult builtModel = builder.build(req);
            processModel(scopes, builtModel.getEffectiveModel(), toLookup, bomLookup);
        } catch (ModelBuildingException mbe) {
            logger.warn("Unable to process " + pom, mbe);
            throw new RuntimeException(mbe);
        }
        return new DependencyResolutionResult(toLookup, bomLookup);
    }

    private void processModel(Set<String> scopes, Model mavenModel, HashSet<ComponentIdentifier> toLookup, HashSet<ComponentIdentifier> bomLookup) {
        if (mavenModel != null) {
            StringSubstitutor replacer = createPropertyReplacer(mavenModel);
            if (mavenModel.getDependencies() != null) {
                for(org.apache.maven.model.Dependency mavenDep : mavenModel.getDependencies()) {
                    if (mavenDep.isOptional())
                        continue;
                    String scope = mavenDep.getScope();
                    if (scopes.contains(scope)) {
                        ModuleComponentIdentifier id =
                                createComponentIdentifier(replacer, mavenDep);
                        System.out.println("-- dependency: "+ id);
                        toLookup.add(id);
                    } else {
                        logger.debug("Dropped "+ mavenDep + " because scope "+ scope);
                    }
                }
            }
            if (mavenModel.getDependencyManagement() != null) {
                for (org.apache.maven.model.Dependency mavenDep : mavenModel.getDependencyManagement().getDependencies()) {
                    ModuleComponentIdentifier id =
                            createComponentIdentifier(replacer, mavenDep);
                    bomLookup.add(id);
                }
            }
        }
    }

    private static ModuleComponentIdentifier createComponentIdentifier(StringSubstitutor replacer, org.apache.maven.model.Dependency dep) {
        return DefaultModuleComponentIdentifier.newId(
                DefaultModuleIdentifier.newId(
                        replacer.replace(dep.getGroupId()),
                        replacer.replace(dep.getArtifactId())),
                replacer.replace(dep.getVersion()));
    }

    private static  StringSubstitutor createPropertyReplacer(Model mavenModel) {
        HashMap<String, String> replacements = new HashMap<>();

        for(String propertyName : mavenModel.getProperties().stringPropertyNames()) {
            replacements.put(propertyName, mavenModel.getProperties().getProperty(propertyName));
        }

        return new StringSubstitutor(replacements);
    }
}
