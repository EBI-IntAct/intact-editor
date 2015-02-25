/**
 * Copyright 2012 The European Bioinformatics Institute, and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.intact.editor.services.curate.experiment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.FeatureEvidence;
import psidev.psi.mi.jami.model.InteractionEvidence;
import psidev.psi.mi.jami.model.ParticipantEvidence;
import uk.ac.ebi.intact.editor.controller.curate.cloner.InteractorCloner;
import uk.ac.ebi.intact.editor.controller.curate.experiment.ExperimentWrapper;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.jami.model.extension.*;

import javax.annotation.Resource;

/**
 * Service for experimentalDetailsController
 */
@Service
public class ExperimentDetailedViewService extends AbstractEditorService {

    @Resource(name = "experimentEditorService")
    private ExperimentEditorService experimentEditorService;

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED, readOnly = true)
    public ExperimentWrapper loadExperimentWrapperByAc( String ac ) {
        IntactExperiment experiment = experimentEditorService.loadExperimentByAc(ac);

        if (experiment != null) {
            return new ExperimentWrapper(experiment);
        } else {
            return null;
        }
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED, readOnly = true)
    public ExperimentWrapper loadExperimentWrapper( IntactExperiment experiment ) {
        if (!isExperimentFullyInitialised(experiment)
                && experiment.getAc() != null
                && !getIntactDao().getEntityManager().contains(experiment)){
            return loadExperimentWrapperByAc(experiment.getAc());
        }

        InteractorCloner cloner = new InteractorCloner();
        for (InteractionEvidence inter : experiment.getInteractionEvidences()){
            for (ParticipantEvidence part : inter.getParticipants()){
                initialiseParticipant(part, cloner);
            }
        }
        ExperimentWrapper experimentWrapper = new ExperimentWrapper(experiment);

        return experimentWrapper;
    }

    private void initialiseParticipant(ParticipantEvidence det, InteractorCloner cloner) {
        IntactInteractor interactor = (IntactInteractor)det.getInteractor();
        if (!isInteractorInitialised(interactor)){
            IntactInteractor interactorReloaded = (IntactInteractor)initialiseInteractor(interactor, cloner);
            if (interactorReloaded != interactor){
                det.setInteractor(interactor);
            }
        }
        initialiseXrefs(interactor.getDbXrefs());
        initialiseAnnotations(interactor.getDbAnnotations());
    }

    private boolean isExperimentFullyInitialised(IntactExperiment experiment){

        if (!experiment.areInteractionEvidencesInitialized() || !experiment.areAnnotationsInitialized()){
            return false;
        }
        else{
            for (InteractionEvidence inter : experiment.getInteractionEvidences()){
                if (inter instanceof IntactInteractionEvidence){
                    IntactInteractionEvidence ev = (IntactInteractionEvidence)inter;

                    if (!ev.areParticipantsInitialized()){
                        return false;
                    }
                    else if (!ev.areXrefsInitialized() || !ev.areParametersInitialized() || !ev.areAnnotationsInitialized()){
                        return false;
                    }
                    else{
                        for (ParticipantEvidence p : ev.getParticipants()){
                            if (p instanceof IntactParticipantEvidence){
                                IntactParticipantEvidence part = (IntactParticipantEvidence)p;
                                if (!part.areFeaturesInitialized()){
                                    return false;
                                }
                                else{
                                    for (FeatureEvidence f : part.getFeatures()){
                                        if (f instanceof IntactFeatureEvidence){
                                            IntactFeatureEvidence feat = (IntactFeatureEvidence)f;
                                            if (!feat.areLinkedFeaturesInitialized() || !feat.areRangesInitialized()){
                                                return false;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
}
