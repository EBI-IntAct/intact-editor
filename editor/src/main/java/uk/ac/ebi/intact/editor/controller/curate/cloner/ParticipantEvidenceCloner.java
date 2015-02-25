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
import uk.ac.ebi.intact.jami.dao.IntactDao;
import uk.ac.ebi.intact.jami.model.extension.*;

/**
 * Editor specific cloning routine for participant evidences.
 *
 * @author Prem Anand (prem@ebi.ac.uk)
 * @version $Id: InteractionIntactCloner.java 14783 2010-07-29 12:52:28Z brunoaranda $
 * @since 2.0.1-SNAPSHOT
 */
public class ParticipantEvidenceCloner extends AbstractEditorCloner<ParticipantEvidence, IntactParticipantEvidence> {

    private EditorCloner<FeatureEvidence, IntactFeatureEvidence> featureCloner;

    public IntactParticipantEvidence clone(ParticipantEvidence participant, IntactDao dao) {
        IntactParticipantEvidence clone = new IntactParticipantEvidence(participant.getInteractor());
        // set current user
        initAuditProperties(clone, dao);

        clone.setBiologicalRole(participant.getBiologicalRole());
        clone.setExperimentalRole(participant.getExperimentalRole());
        clone.setExpressedInOrganism(participant.getExpressedInOrganism());
        clone.setInteraction(participant.getInteraction());
        clone.setStoichiometry(new IntactStoichiometry(participant.getStoichiometry().getMinValue(), participant.getStoichiometry().getMaxValue()));
        clone.getExperimentalPreparations().addAll(participant.getExperimentalPreparations());
        clone.getIdentificationMethods().addAll(participant.getIdentificationMethods());

        for (Object obj : participant.getAliases()){
            Alias alias = (Alias)obj;
            clone.getAliases().add(new ParticipantEvidenceAlias(alias.getType(), alias.getName()));
        }

        for (Object obj : participant.getXrefs()){
            Xref ref = (Xref)obj;
            clone.getXrefs().add(new ParticipantEvidenceXref(ref.getDatabase(), ref.getId(), ref.getVersion(), ref.getQualifier()));
        }

        for (Object obj : participant.getAnnotations()){
            Annotation annotation = (Annotation)obj;
            clone.getAnnotations().add(new ParticipantEvidenceAnnotation(annotation.getTopic(), annotation.getValue()));
        }

        for (Object obj : participant.getParameters()){
            Parameter param = (Parameter)obj;
            clone.getParameters().add(new ParticipantEvidenceParameter(param.getType(), param.getValue(), param.getUnit(), param.getUncertainty()));
        }

        for (Object obj : participant.getConfidences()){
            Confidence conf = (Confidence)obj;
            clone.getConfidences().add(new ParticipantEvidenceConfidence(conf.getType(), conf.getValue()));
        }

        for (FeatureEvidence feature : participant.getFeatures()){
            FeatureEvidence r = getFeatureCloner().clone(feature, dao);
            clone.addFeature(r);
        }

        // don't need to add it to the feature component because it is already done by the cloner
        return clone;
    }

    @Override
    public void copyInitialisedProperties(IntactParticipantEvidence source, IntactParticipantEvidence target) {
        target.setBiologicalRole(source.getBiologicalRole());
        target.setStoichiometry(source.getStoichiometry());
        target.setExperimentalRole(source.getExperimentalRole());
        target.setExpressedInOrganism(source.getExpressedInOrganism());
        target.setInteraction(source.getInteraction());
        if (source.areAliasesInitialized()){
            target.getAliases().clear();
            target.getAliases().addAll(source.getAliases());
        }

        if (source.areXrefsInitialized()){
            target.getXrefs().clear();
            target.getXrefs().addAll(source.getXrefs());
        }

        if (source.areAnnotationsInitialized()){
            target.getAnnotations().clear();
            target.getAnnotations().addAll(source.getAnnotations());
        }

        if (source.areFeaturesInitialized()){
            target.getFeatures().clear();
            target.addAllFeatures(source.getFeatures());
        }

        if (source.areConfidencesInitialized()){
            target.getConfidences().clear();
            target.getConfidences().addAll(source.getConfidences());
        }

        if (source.areParametersInitialized()){
            target.getParameters().clear();
            target.getParameters().addAll(source.getParameters());
        }

        if (source.areExperimentalPreparationsInitialized()){
            target.getExperimentalPreparations().clear();
            target.getExperimentalPreparations().addAll(source.getExperimentalPreparations());
        }

        if (source.areIdentificationMethodsInitialized()){
            target.getIdentificationMethods().clear();
            target.getIdentificationMethods().addAll(source.getIdentificationMethods());
        }
    }

    public EditorCloner<FeatureEvidence, IntactFeatureEvidence> getFeatureCloner() {
        if (this.featureCloner == null){
            this.featureCloner = new FeatureEvidenceCloner();
        }
        return featureCloner;
    }

    protected void setFeatureCloner(EditorCloner<FeatureEvidence, IntactFeatureEvidence> featureCloner) {
        this.featureCloner = featureCloner;
    }
}

