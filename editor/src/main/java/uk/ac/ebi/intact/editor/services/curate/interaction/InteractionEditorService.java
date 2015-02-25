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
package uk.ac.ebi.intact.editor.services.curate.interaction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.*;
import uk.ac.ebi.intact.editor.controller.curate.cloner.*;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
@Service
public class InteractionEditorService extends AbstractEditorService {

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countXrefs(IntactInteractionEvidence interaction) {
        return getIntactDao().getInteractionDao().countXrefsForInteraction(interaction.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countAnnotations(IntactInteractionEvidence interaction) {
        return getIntactDao().getInteractionDao().countAnnotationsForInteraction(interaction.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countParticipants(IntactInteractionEvidence interaction) {
        return getIntactDao().getInteractionDao().countParticipantsForInteraction(interaction.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countConfidences(IntactInteractionEvidence interaction) {
        return getIntactDao().getInteractionDao().countConfidencesForInteraction(interaction.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countParameters(IntactInteractionEvidence interaction) {
        return getIntactDao().getInteractionDao().countParametersForInteraction(interaction.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countVariableParameterValues(IntactInteractionEvidence interaction) {
        return getIntactDao().getInteractionDao().countVariableParameterValuesSetsForInteraction(interaction.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public IntactInteractionEvidence loadInteractionByAc(String ac) {
        IntactInteractionEvidence interaction = getIntactDao().getEntityManager().find(IntactInteractionEvidence.class, ac);

        if (interaction != null){
            // initialise experiment
            if (interaction.getExperiment() != null){
                initialiseExperiment((IntactExperiment)interaction.getExperiment());
            }
            // initialise annotations because needs caution
            initialiseAnnotations(interaction.getDbAnnotations());
            // initialise xrefs because of imex
            initialiseXrefs(interaction.getDbXrefs());
            // initialise confidences
            initialiseConfidence(interaction.getConfidences());
            // initialise parameters
            initialiseParameters(interaction.getParameters());
            // initialise variable parameters
            initialiseVariableParameters(interaction.getVariableParameterValues());
            // initialise participants
            Collection<ParticipantEvidence> dets = interaction.getParticipants();
            initialiseParticipants(interaction, dets);

            // load base types
            if (interaction.getInteractionType() != null) {
                initialiseCv(interaction.getInteractionType());
            }
        }

        return interaction;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public IntactExperiment loadExperimentByAcOrLabel(String ac) {
        IntactExperiment experiment = getIntactDao().getEntityManager().find(IntactExperiment.class, ac);

        if (experiment == null){
            experiment = getIntactDao().getExperimentDao().getByShortLabel(ac);
        }

        if (experiment != null){
            initialiseExperiment(experiment);
        }

        return experiment;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public IntactInteractionEvidence reloadFullyInitialisedInteraction(IntactInteractionEvidence interaction) {
        if (interaction == null){
            return null;
        }

        IntactInteractionEvidence reloaded = null;
        if (areInteractionCollectionsLazy(interaction)
                && interaction.getAc() != null
                && !getIntactDao().getEntityManager().contains(interaction)){
            reloaded = loadInteractionByAc(interaction.getAc());
        }

        // we need first to merge with reloaded complex
        if (reloaded != null){
            // detach reloaded now so not changes will be committed
            getIntactDao().getEntityManager().detach(reloaded);
            InteractionEvidenceCloner cloner = new InteractionEvidenceCloner();
            cloner.copyInitialisedProperties(interaction, reloaded);
            interaction = reloaded;
        }
        // initialise experiment
        if (interaction.getExperiment() != null && !isExperimentInitialised(interaction.getExperiment())){
            initialiseExperiment((IntactExperiment) interaction.getExperiment());
        }
        // initialise annotations because needs caution
        initialiseAnnotations(interaction.getDbAnnotations());
        // initialise xrefs because of imex
        initialiseXrefs(interaction.getDbXrefs());
        // initialise confidences
        initialiseConfidence(interaction.getConfidences());
        // initialise parameters
        initialiseParameters(interaction.getParameters());
        // initialise variable parameters
        initialiseVariableParameters(interaction.getVariableParameterValues());
        // initialise participants
        Collection<ParticipantEvidence> dets = interaction.getParticipants();
        initialiseParticipants(interaction, dets);

        // load base types
        if (interaction.getInteractionType() != null && !isCvInitialised(interaction.getInteractionType())) {
            CvTerm cv = initialiseCv(interaction.getInteractionType());
            if (cv != interaction.getInteractionType()){
                interaction.setInteractionType(cv);
            }
        }

        return interaction;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public Collection<Annotation> initialiseInteractionAnnotations(IntactInteractionEvidence releasable) {
        // reload complex without flushing changes
        IntactInteractionEvidence reloaded = releasable;
        // merge current user because detached
        if (releasable.getAc() != null && !getIntactDao().getEntityManager().contains(releasable)){
            reloaded = getIntactDao().getEntityManager().find(IntactInteractionEvidence.class, releasable.getAc());
            if (reloaded == null){
                reloaded = releasable;
            }
        }

        initialiseAnnotations(reloaded.getAnnotations());
        return reloaded.getAnnotations();
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public String computesShortLabel(IntactInteractionEvidence evidence) {
        return IntactUtils.generateAutomaticInteractionEvidenceShortlabelFor(evidence, IntactUtils.MAX_SHORT_LABEL_LEN);
    }

    private boolean areInteractionCollectionsLazy(IntactInteractionEvidence interactor) {
        return !interactor.areAnnotationsInitialized()
                || !interactor.areVariableParameterValuesInitialized()
                || !interactor.areParametersInitialized()
                || !interactor.areConfidencesInitialized()
                || !interactor.areXrefsInitialized()
                || !interactor.areParticipantsInitialized();
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public boolean isInteractionFullyLoaded(IntactInteractionEvidence interactionEvidence){
        if (interactionEvidence == null){
            return true;
        }
        if (!interactionEvidence.areAnnotationsInitialized()
                || !interactionEvidence.areVariableParameterValuesInitialized()
                || !interactionEvidence.areParametersInitialized()
                || !interactionEvidence.areConfidencesInitialized()
                || !interactionEvidence.areXrefsInitialized()
                || (interactionEvidence.getInteractionType() != null && !isCvInitialised(interactionEvidence.getInteractionType()))
                || !areParticipantsInitialised(interactionEvidence)
                || (interactionEvidence.getExperiment() != null && !isExperimentInitialised(interactionEvidence.getExperiment()))){
            return false;
        }
        return true;
    }

    private ParticipantEvidence initialiseParticipant(ParticipantEvidence det, uk.ac.ebi.intact.editor.controller.curate.cloner.InteractorCloner interactorCloner,
                                                      FeatureEvidenceCloner featureCloner, ParticipantEvidenceCloner participantCloner) {
        if (det instanceof IntactParticipantEvidence){
            if (!((IntactParticipantEvidence)det).areFeaturesInitialized()
                    && ((IntactParticipantEvidence)det).getAc() != null
                    && !getIntactDao().getEntityManager().contains(det)){
                IntactParticipantEvidence reloaded = getIntactDao().getEntityManager().find(IntactParticipantEvidence.class, ((IntactParticipantEvidence)det).getAc());
                if (reloaded != null){
                    Interactor interactor = reloaded.getInteractor();
                    if (!isInteractorInitialised(interactor)){
                        initialiseInteractor(interactor, interactorCloner);
                    }

                    if (det.getBiologicalRole() != null && !isCvInitialised(det.getBiologicalRole())){
                        initialiseCv(det.getBiologicalRole());
                    }

                    if (det.getExperimentalRole() != null && !isCvInitialised(det.getExperimentalRole())){
                        initialiseCv(det.getExperimentalRole());
                    }

                    initialiseFeatures(det, det.getFeatures(), featureCloner);
                    // detach relaoded object so no changes will be flushed
                    getIntactDao().getEntityManager().detach(reloaded);
                    participantCloner.copyInitialisedProperties((IntactParticipantEvidence) det, reloaded);
                    // will return reloaded object
                    det = reloaded;
                }
            }
        }

        Interactor interactor = det.getInteractor();
        if (!isInteractorInitialised(interactor)){
            interactor = initialiseInteractor(interactor, interactorCloner);
            det.setInteractor(interactor);
        }

        if (det.getBiologicalRole() != null && !isCvInitialised(det.getBiologicalRole())){
            CvTerm bioRole = initialiseCv(det.getBiologicalRole());
            if (bioRole != det.getBiologicalRole()){
                det.setBiologicalRole(bioRole);
            }
        }

        if (det.getExperimentalRole() != null && !isCvInitialised(det.getExperimentalRole())){
            CvTerm expRole = initialiseCv(det.getExperimentalRole());
            if (expRole != det.getExperimentalRole()){
                det.setExperimentalRole(expRole);
            }
        }

        initialiseFeatures(det, det.getFeatures(), featureCloner);

        return det;
    }

    private boolean areParticipantsInitialised(IntactInteractionEvidence interaction) {
        if (!interaction.areParticipantsInitialized()){
            return false;
        }

        for (ParticipantEvidence part : interaction.getParticipants()){
            if (!isParticipantInitialised(part)){
                return false;
            }
        }
        return true;
    }

    protected boolean isParticipantInitialised(ParticipantEvidence participant) {
        if (participant.getBiologicalRole() != null && !isCvInitialised(participant.getBiologicalRole())){
            return false;
        }
        else if (participant.getExperimentalRole() != null && !isCvInitialised(participant.getExperimentalRole())){
            return false;
        }
        else if (!isInteractorInitialised(participant.getInteractor())){
            return false;
        }
        else if (participant instanceof IntactParticipantEvidence
                && !((IntactParticipantEvidence) participant).areFeaturesInitialized()){
            return false;
        }
        else {
            for (FeatureEvidence f : participant.getFeatures()){
                if (!isFeatureInitialised(f)){
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void initialiseOtherInteractorProperties(IntactInteractor inter) {
        // initialise aliases
        initialiseAliases(inter.getAliases());
    }

    protected boolean areInteractorCollectionsLazy(IntactInteractor inter) {
        return !inter.areAliasesInitialized() || !inter.areXrefsInitialized()
                || !inter.areAnnotationsInitialized();
    }

    protected boolean isInteractorInitialised(Interactor interactor) {
        if(interactor instanceof IntactInteractor){
            IntactInteractor intactInteractor = (IntactInteractor)interactor;
            if (!intactInteractor.areXrefsInitialized()
                    || !intactInteractor.areAnnotationsInitialized()
                    || !intactInteractor.areAliasesInitialized()){
                return false;
            }
        }
        return true;
    }

    private void initialiseFeatures(ParticipantEvidence parent, Collection<FeatureEvidence> features, FeatureEvidenceCloner featureCloner) {
        List<FeatureEvidence> originalFeatures = new ArrayList<FeatureEvidence>(features);
        for (FeatureEvidence r : originalFeatures){
            if (!isFeatureInitialised(r)){
                FeatureEvidence reloaded = initialiseFeature(r, featureCloner);
                if (reloaded != r ){
                    features.remove(r);
                    parent.addFeature(reloaded);
                }
            }
        }
    }

    private void initialiseParticipants(InteractionEvidence parent, Collection<ParticipantEvidence> participants) {
        List<ParticipantEvidence> originalParticipants = new ArrayList<ParticipantEvidence>(participants);
        ParticipantEvidenceCloner participantCloner = new ParticipantEvidenceCloner();
        FeatureEvidenceCloner featureCloner = new FeatureEvidenceCloner();
        uk.ac.ebi.intact.editor.controller.curate.cloner.InteractorCloner interactorCloner = new uk.ac.ebi.intact.editor.controller.curate.cloner.InteractorCloner();
        for (ParticipantEvidence det : originalParticipants){
            ParticipantEvidence p = initialiseParticipant(det, interactorCloner, featureCloner, participantCloner);
            if (p != det){
                participants.remove(det);
                parent.addParticipant(p);
            }
        }
    }

    private FeatureEvidence initialiseFeature(FeatureEvidence det, FeatureEvidenceCloner featCloner) {
        if (det instanceof IntactFeatureEvidence){
            if (areFeatureCollectionsLazy((IntactFeatureEvidence) det)
                    && ((IntactFeatureEvidence)det).getAc() != null
                    && !getIntactDao().getEntityManager().contains(det)){
                IntactFeatureEvidence reloaded = getIntactDao().getEntityManager().find(IntactFeatureEvidence.class, ((IntactFeatureEvidence) det).getAc());
                if (reloaded != null){
                    // initialise properties freshly loaded from db
                    initialiseRanges(reloaded.getRanges());

                    for (Object linked : reloaded.getLinkedFeatures()){
                        ((Feature)linked).getLinkedFeatures().size();
                    }

                    // detach relaoded object so no changes will be flushed
                    getIntactDao().getEntityManager().detach(reloaded);
                    featCloner.copyInitialisedProperties((IntactFeatureEvidence)det, reloaded);
                    // will return reloaded object
                    det = reloaded;
                }
            }

            initialiseRanges(det.getRanges());

            for (Object linked : det.getLinkedFeatures()){
                ((Feature)linked).getLinkedFeatures().size();
            }
        }
        return det;
    }

    private boolean areFeatureCollectionsLazy(IntactFeatureEvidence det) {
        return !det.areLinkedFeaturesInitialized() || !det.areRangesInitialized();
    }

    private boolean isFeatureInitialised(FeatureEvidence feature) {
        if (feature.getType() != null && !isCvInitialised(feature.getType())){
            return false;
        }
        else if (feature.getRole() != null && !isCvInitialised(feature.getRole())){
            return false;
        }
        else if (feature instanceof IntactFeatureEvidence){
            IntactFeatureEvidence intactFeature = (IntactFeatureEvidence)feature;
            if (!intactFeature.areLinkedFeaturesInitialized()){
                return false;
            }
            else if (!intactFeature.areRangesInitialized()){
                return false;
            }
            else{
                for (Range r : intactFeature.getRanges()){
                    if (!isRangeInitialised(r)){
                        return false;
                    }
                }
            }
        }
        return true;
    }

    protected Range initialiseRange(Range range) {
        initialisePosition(range.getStart());
        initialisePosition(range.getEnd());
        return range;
    }

    protected boolean isRangeInitialised(Range range) {
        if (!isPositionInitialised(range.getStart()) || !isPositionInitialised(range.getEnd())){
            return false;
        }
        return true;
    }

    private boolean isExperimentInitialised(Experiment exp){
        if (exp instanceof IntactExperiment
                && ((IntactExperiment) exp).getParticipantIdentificationMethod() != null
                && !isCvInitialised(((IntactExperiment) exp).getParticipantIdentificationMethod())){
            return false;
        }
        return true;
    }

    private void initialiseExperiment(IntactExperiment experiment) {
        if (experiment.getParticipantIdentificationMethod() != null && !isCvInitialised(experiment.getParticipantIdentificationMethod())) {
            CvTerm cv = initialiseCv(experiment.getParticipantIdentificationMethod());
            if (cv != experiment.getParticipantIdentificationMethod()) {
                experiment.setParticipantIdentificationMethod(cv);
            }
        }
    }

    private void initialiseVariableParameters(Collection<VariableParameterValueSet> parameters) {
        Collection<VariableParameterValueSet> originalSet = new ArrayList<VariableParameterValueSet>(parameters);
        for (VariableParameterValueSet set : originalSet){
            if (set instanceof IntactVariableParameterValueSet && !isVariableParameterValueSetInitialised(set)){
                VariableParameterValueSet reloaded = initialiseVariableParameterValueSet(set);
                if (reloaded != set){
                    parameters.remove(set);
                    parameters.add(reloaded);
                }
            }
        }
    }

    private boolean isVariableParameterValueSetInitialised(VariableParameterValueSet set){
        if (set instanceof IntactVariableParameterValueSet && !((IntactVariableParameterValueSet) set).areVariableParameterValuesInitialized()){
            return false;
        }
        return true;
    }

    private VariableParameterValueSet initialiseVariableParameterValueSet(VariableParameterValueSet set){
        if (set instanceof IntactVariableParameterValueSet){
            if (!((IntactVariableParameterValueSet) set).areVariableParameterValuesInitialized()
                    && ((IntactVariableParameterValueSet)set).getId() != null && !getIntactDao().getEntityManager().contains(set)){
                IntactVariableParameterValueSet reloaded = getIntactDao().getEntityManager().find(IntactVariableParameterValueSet.class, ((IntactVariableParameterValueSet)set).getId());
                if (reloaded != null){
                    // initialise freshly loaded properties
                    set.size();
                    // detach object so no changes will be flushed
                    getIntactDao().getEntityManager().detach(reloaded);
                    // will return reloaded object
                    set = reloaded;
                }
            }
            set.size();
        }
        return set;
    }
}
