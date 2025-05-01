package com.canonical.buildinspector;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.model.interpolation.DefaultModelVersionProcessor;
import org.apache.maven.model.validation.DefaultModelValidator;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.util.ArrayList;

/**
 * A model validator class that outputs warnings, but does not fail the model build.
 */
class SilentModelValidator extends DefaultModelValidator {

    private final Logger logger = Logging.getLogger(SilentModelValidator.class);

    private static class InternalModelProblemCollector implements ModelProblemCollector {

        private final ArrayList<ModelProblemCollectorRequest> requests = new ArrayList<>();

        @Override
        public void add(ModelProblemCollectorRequest modelProblemCollectorRequest) {
            requests.add(modelProblemCollectorRequest);
        }

        public ArrayList<ModelProblemCollectorRequest> getRequests() {
            return requests;
        }
    }

    public SilentModelValidator() {
        super(new DefaultModelVersionProcessor());
    }

    @Override
    public void validateRawModel(Model model, ModelBuildingRequest req, ModelProblemCollector collector) {
        InternalModelProblemCollector internalCollector = new InternalModelProblemCollector();
        super.validateRawModel(model, req, internalCollector);
        for (ModelProblemCollectorRequest problemReq : internalCollector.getRequests()) {
            logger.warn(problemReq.getMessage(), problemReq.getException());
        }
    }

    @Override
    public void validateEffectiveModel(Model model, ModelBuildingRequest req, ModelProblemCollector collector) {
        InternalModelProblemCollector internalCollector = new InternalModelProblemCollector();
        super.validateRawModel(model, req, internalCollector);
        for (ModelProblemCollectorRequest problemReq : internalCollector.getRequests()) {
            logger.warn(problemReq.getMessage(), problemReq.getException());
        }
    }
}
