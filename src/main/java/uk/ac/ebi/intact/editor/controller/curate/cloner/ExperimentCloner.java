/**
 * Copyright 2010 The European Bioinformatics Institute, and others.
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
package uk.ac.ebi.intact.editor.controller.curate.cloner;

import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import psidev.psi.mi.jami.utils.XrefUtils;
import uk.ac.ebi.intact.jami.dao.IntactDao;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.model.lifecycle.Releasable;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import java.util.Collections;

/**
 * Editor specific cloning routine for experiment.
 *
 * @author Prem Anand (prem@ebi.ac.uk)
 * @version $Id: InteractionIntactCloner.java 14783 2010-07-29 12:52:28Z brunoaranda $
 * @since 2.0.1-SNAPSHOT
 */
public class ExperimentCloner extends AbstractEditorCloner<Experiment, IntactExperiment> {

    private EditorCloner<InteractionEvidence, IntactInteractionEvidence> evidenceCloner;
    private boolean cloneInteractions = false;

    public ExperimentCloner(boolean cloneInteractions) {
        this.cloneInteractions = cloneInteractions;
    }

    public IntactExperiment clone(Experiment experiment, IntactDao dao) {
        IntactExperiment clone = new IntactExperiment(experiment.getPublication());

        initAuditProperties(clone, dao);

        clone.setShortLabel(IntactUtils.generateAutomaticExperimentShortlabelFor(clone, IntactUtils.MAX_SHORT_LABEL_LEN));
        // synchronize with db
        IntactUtils.synchronizeExperimentShortLabel(clone, dao.getEntityManager(), Collections.EMPTY_SET);
        clone.setHostOrganism(experiment.getHostOrganism());
        clone.setInteractionDetectionMethod(experiment.getInteractionDetectionMethod());
        clone.setPublication(experiment.getPublication());
        if (experiment instanceof IntactExperiment){
            clone.setParticipantIdentificationMethod(((IntactExperiment) experiment).getParticipantIdentificationMethod());
        }

        clone.getConfidences().addAll(experiment.getConfidences());

        for (Xref ref : experiment.getXrefs()){
            if (!(XrefUtils.doesXrefHaveQualifier(ref, Xref.IMEX_PRIMARY_MI, Xref.IMEX_PRIMARY)
                    && XrefUtils.isXrefFromDatabase(ref, Xref.IMEX_MI, Xref.IMEX))){
                AbstractIntactXref intactRef = new ExperimentXref(ref.getDatabase(), ref.getId(), ref.getVersion(), ref.getQualifier());
                if (ref instanceof AbstractIntactXref){
                    intactRef.setSecondaryId(((AbstractIntactXref) ref).getSecondaryId());
                }
                clone.getXrefs().add(intactRef);
            }
        }

        for (Annotation annotation : experiment.getAnnotations()){
            if (!AnnotationUtils.doesAnnotationHaveTopic(annotation, null, Releasable.CORRECTION_COMMENT)
                    && !AnnotationUtils.doesAnnotationHaveTopic(annotation, null, Releasable.ON_HOLD)
                    && !AnnotationUtils.doesAnnotationHaveTopic(annotation, null, Releasable.TO_BE_REVIEWED)
                    && !AnnotationUtils.doesAnnotationHaveTopic(annotation, null, Releasable.ACCEPTED) ){
                clone.getAnnotations().add(new ExperimentAnnotation(annotation.getTopic(), annotation.getValue()));
            }
        }

        for (VariableParameter param : experiment.getVariableParameters()){

            VariableParameter paramClone = new IntactVariableParameter(param.getDescription(), clone, param.getUnit());
            clone.getVariableParameters().add(paramClone);
            for (VariableParameterValue value : param.getVariableValues()){
                VariableParameterValue cloneValue = new IntactVariableParameterValue(value.getValue(), paramClone, value.getOrder());
                paramClone.getVariableValues().add(cloneValue);
            }
        }

        if (cloneInteractions){
            for (InteractionEvidence evidence : experiment.getInteractionEvidences()){
                clone.addInteractionEvidence(getInteractionEvidenceCloner().clone(evidence, dao));
            }
        }

        return clone;
    }

    @Override
    public void copyInitialisedProperties(IntactExperiment source, IntactExperiment target) {
        target.setShortLabel(source.getShortLabel());
        target.setHostOrganism(source.getHostOrganism());
        target.setInteractionDetectionMethod(source.getInteractionDetectionMethod());
        target.setPublication(source.getPublication());
        target.setParticipantIdentificationMethod(source.getParticipantIdentificationMethod());

        if (source.areXrefsInitialized()){
            target.getXrefs().clear();
            target.getXrefs().addAll(source.getXrefs());
        }

        if (source.areAnnotationsInitialized()){
            target.getAnnotations().clear();
            target.getAnnotations().addAll(source.getAnnotations());
        }

        if (source.areVariableParametersInitialized()){
            target.getVariableParameters().clear();
            target.addAllVariableParameters(source.getVariableParameters());
        }

        if (source.areInteractionEvidencesInitialized()){
            target.getInteractionEvidences().clear();
            target.addAllInteractionEvidences(source.getInteractionEvidences());
        }
    }

    public EditorCloner<InteractionEvidence, IntactInteractionEvidence> getInteractionEvidenceCloner(){
        if (this.evidenceCloner == null){
            this.evidenceCloner = new InteractionEvidenceCloner();
        }
        return this.evidenceCloner;
    }

    protected void setEvidenceCloner(EditorCloner<InteractionEvidence, IntactInteractionEvidence> evidenceCloner) {
        this.evidenceCloner = evidenceCloner;
    }
}

