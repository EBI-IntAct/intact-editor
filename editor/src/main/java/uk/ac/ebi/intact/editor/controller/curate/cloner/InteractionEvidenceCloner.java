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
import psidev.psi.mi.jami.utils.XrefUtils;
import uk.ac.ebi.intact.jami.dao.IntactDao;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import java.util.Collections;

/**
 * Editor specific cloning routine for complex participants.
 *
 * @author Prem Anand (prem@ebi.ac.uk)
 * @version $Id: InteractionIntactCloner.java 14783 2010-07-29 12:52:28Z brunoaranda $
 * @since 2.0.1-SNAPSHOT
 */
public class InteractionEvidenceCloner extends AbstractEditorCloner<InteractionEvidence, IntactInteractionEvidence> {

    private EditorCloner<ParticipantEvidence, IntactParticipantEvidence> participantCloner;

    public IntactInteractionEvidence clone(InteractionEvidence evidence, IntactDao dao) {
        IntactInteractionEvidence clone = new IntactInteractionEvidence();

        initAuditProperties(clone, dao);

        clone.setInteractionType(evidence.getInteractionType());
        clone.setExperiment(evidence.getExperiment());
        clone.setAvailability(evidence.getAvailability());
        clone.setInferred(evidence.isInferred());
        clone.setNegative(evidence.isNegative());

        for (Xref ref : evidence.getXrefs()){
            if (!(XrefUtils.isXrefFromDatabase(ref, Xref.IMEX_MI, Xref.IMEX)
                    && XrefUtils.doesXrefHaveQualifier(ref, Xref.PRIMARY_MI, Xref.PRIMARY))){
                clone.getXrefs().add(new InteractionXref(ref.getDatabase(), ref.getId(), ref.getVersion(), ref.getQualifier()));
            }

        }

        for (Annotation annotation : evidence.getAnnotations()){
            clone.getAnnotations().add(new InteractionAnnotation(annotation.getTopic(), annotation.getValue()));
        }

        for (ParticipantEvidence participant : evidence.getParticipants()){
            ParticipantEvidence r = getParticipantCloner().clone(participant, dao);
            clone.addParticipant(r);
        }

        for (Confidence confidence : evidence.getConfidences()){
            clone.getConfidences().add(new InteractionEvidenceConfidence(confidence.getType(), confidence.getValue()));
        }

        for (Parameter param : evidence.getParameters()){
            clone.getParameters().add(new InteractionEvidenceParameter(param.getType(), param.getValue(), param.getUnit(), param.getUncertainty()));
        }

        for (VariableParameterValueSet set : evidence.getVariableParameterValues()){
           VariableParameterValueSet setClone = new IntactVariableParameterValueSet(set);
            clone.getVariableParameterValues().add(setClone);
        }

        clone.setShortName(IntactUtils.generateAutomaticInteractionEvidenceShortlabelFor(clone, IntactUtils.MAX_SHORT_LABEL_LEN));
        // synchronize with db
        IntactUtils.synchronizeInteractionEvidenceShortName(clone, dao.getEntityManager(), Collections.EMPTY_SET);
        return clone;
    }

    @Override
    public void copyInitialisedProperties(IntactInteractionEvidence source, IntactInteractionEvidence target) {
        target.setShortName(source.getShortName());
        target.setInteractionType(source.getInteractionType());
        target.setExperiment(source.getExperiment());
        target.setAvailability(source.getAvailability());
        target.setInferred(source.isInferred());
        target.setNegative(source.isNegative());

        if (source.areXrefsInitialized()){
            target.getIdentifiers().clear();
            target.getIdentifiers().addAll(source.getIdentifiers());
            target.getXrefs().clear();
            target.getXrefs().addAll(source.getXrefs());
        }

        if (source.areAnnotationsInitialized()){
            target.getAnnotations().clear();
            target.getAnnotations().addAll(source.getAnnotations());
        }

        if (source.areParticipantsInitialized()){
            target.addAllParticipants(source.getParticipants());
        }

        if (source.areConfidencesInitialized()){
            target.getConfidences().clear();
            target.getConfidences().addAll(source.getConfidences());
        }

        if (source.areParametersInitialized()){
            target.getParameters().clear();
            target.getParameters().addAll(source.getParameters());
        }

        if (source.areVariableParameterValuesInitialized()){
            target.getVariableParameterValues().clear();
            target.getVariableParameterValues().addAll(source.getVariableParameterValues());
        }
    }

    public EditorCloner<ParticipantEvidence, IntactParticipantEvidence> getParticipantCloner() {
        if (this.participantCloner == null){
            this.participantCloner = new ParticipantEvidenceCloner();
        }
        return participantCloner;
    }

    protected void setParticipantCloner(EditorCloner<ParticipantEvidence, IntactParticipantEvidence> participantCloner) {
        this.participantCloner = participantCloner;
    }
}

