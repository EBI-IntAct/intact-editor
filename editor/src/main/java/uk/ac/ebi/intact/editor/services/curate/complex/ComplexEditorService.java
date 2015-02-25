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
package uk.ac.ebi.intact.editor.services.curate.complex;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.*;
import uk.ac.ebi.intact.editor.controller.curate.cloner.ComplexCloner;
import uk.ac.ebi.intact.editor.controller.curate.cloner.ModelledFeatureCloner;
import uk.ac.ebi.intact.editor.controller.curate.cloner.ModelledParticipantCloner;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.model.lifecycle.AbstractLifeCycleEvent;
import uk.ac.ebi.intact.jami.model.lifecycle.LifeCycleEvent;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
@Service
public class ComplexEditorService extends AbstractEditorService {

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countXrefs(IntactComplex interaction) {
        return getIntactDao().getComplexDao().countXrefsForInteractor(interaction.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countAnnotations(IntactComplex interaction) {
        return getIntactDao().getComplexDao().countAnnotationsForInteractor(interaction.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countParticipants(IntactComplex interaction) {
        return getIntactDao().getComplexDao().countParticipantsForComplex(interaction.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countConfidences(IntactComplex interaction) {
        return getIntactDao().getComplexDao().countConfidencesForComplex(interaction.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countParameters(IntactComplex interaction) {
        return getIntactDao().getComplexDao().countParametersForComplex(interaction.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public IntactComplex loadComplexByAc(String ac) {
        IntactComplex interaction = getIntactDao().getEntityManager().find(IntactComplex.class, ac);

        if (interaction != null){
            // iniTransactionSynchtialise annotations because needs caution
            initialiseAnnotations(interaction.getDbAnnotations());
            // initialise aliases
            initialiseAliases(interaction.getAliases());
            // initialise lifecycle events
            initialiseEvents(interaction.getLifecycleEvents());
            // initialise participants
            Collection<ModelledParticipant> dets = interaction.getParticipants();
            initialiseParticipants(interaction, dets);

            // load base types
            if (interaction.getInteractionType() != null) {
                initialiseCv(interaction.getInteractionType());
            }
            if (interaction.getInteractorType() != null) {
                initialiseCv(interaction.getInteractorType());
            }
            if (interaction.getEvidenceType() != null) {
                initialiseCv(interaction.getEvidenceType());
            }
            if (interaction.getCvStatus() != null){
                initialiseCv(interaction.getCvStatus());
            }

            // initialise xrefs
            initialiseXrefs(interaction.getDbXrefs());
            // initialise confidences
            initialiseConfidence(interaction.getModelledConfidences());
            // initialiseParameters
            initialiseParameters(interaction.getModelledParameters());
        }

        return interaction;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public IntactComplex cloneInteractionEvidence(IntactInteractionEvidence ao, ComplexCloner cloner) throws SynchronizerException,
            FinderException,PersisterException {

        IntactInteractionEvidence reloaded = ao;
        // reload fully initialised object if not done yet
        if (reloaded.getAc() != null){
            reloaded = getIntactDao().getEntityManager().find(IntactInteractionEvidence.class, ao.getAc());
        }

        IntactComplex clone = null;
        try {
            clone = cloner.cloneFromEvidence(reloaded, getIntactDao());
            getIntactDao().getEntityManager().detach(reloaded);
            return clone;
        } catch (SynchronizerException e) {
            // clear cache
            getIntactDao().getSynchronizerContext().clearCache();
            getIntactDao().getEntityManager().detach(reloaded);
            throw e;
        } catch (FinderException e) {
            // clear cache
            getIntactDao().getSynchronizerContext().clearCache();
            getIntactDao().getEntityManager().detach(reloaded);
            throw e;
        } catch (PersisterException e) {
            // clear cache
            getIntactDao().getSynchronizerContext().clearCache();
            getIntactDao().getEntityManager().detach(reloaded);
            throw e;
        }
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public IntactComplex reloadFullyInitialisedComplex(IntactComplex interactor) {
        if (interactor == null){
            return null;
        }

        IntactComplex reloaded = null;
        if (areComplexCollectionsLazy(interactor)
                && interactor.getAc() != null
                && !getIntactDao().getEntityManager().contains(interactor)){
            reloaded = loadComplexByAc(interactor.getAc());
        }

        // we need first to merge with reloaded complex
        if (reloaded != null){
            // detach reloaded now so not changes will be committed
            getIntactDao().getEntityManager().detach(reloaded);
            ComplexCloner cloner = new ComplexCloner();
            cloner.copyInitialisedProperties(interactor, reloaded);
            interactor = reloaded;
        }

        // initialise properties now
        // initialise annotations because needs caution
        initialiseAnnotations(interactor.getDbAnnotations());
        // initialise aliases
        initialiseAliases(interactor.getAliases());
        // initialise lifecycle events
        initialiseEvents(interactor.getLifecycleEvents());

        // initialise participants
        Collection<ModelledParticipant> dets = interactor.getParticipants();
        initialiseParticipants(reloaded, dets);

        // load base types
        if (interactor.getInteractionType() != null && !isCvInitialised(interactor.getInteractionType())) {
            CvTerm cv = initialiseCv(interactor.getInteractionType());
            if (cv != interactor.getInteractionType()){
                interactor.setInteractionType(cv);
            }
        }
        if (interactor.getInteractorType() != null && !isCvInitialised(interactor.getInteractorType())) {
            CvTerm cv = initialiseCv(interactor.getInteractorType());
            if (cv != interactor.getInteractorType()){
                interactor.setInteractorType(cv);
            }
        }
        if (interactor.getEvidenceType() != null && !isCvInitialised(interactor.getEvidenceType())) {
            CvTerm cv = initialiseCv(interactor.getEvidenceType());
            if (cv != interactor.getEvidenceType()){
                interactor.setEvidenceType(cv);
            }
        }

        // initialise status
        if (interactor.getCvStatus() != null && !isCvInitialised(interactor.getCvStatus())){
            CvTerm cv = initialiseCv(interactor.getCvStatus());
            if (cv != interactor.getCvStatus()){
                interactor.setCvStatus(cv);
            }
        }

        // initialise xrefs
        initialiseXrefs(interactor.getDbXrefs());
        // initialise confidences
        initialiseConfidence(interactor.getModelledConfidences());
        // initialise parameters
        initialiseParameters(interactor.getModelledParameters());

        return interactor;
    }

    private boolean areComplexCollectionsLazy(IntactComplex interactor) {
        return !interactor.areAnnotationsInitialized()
                || !interactor.areLifeCycleEventsInitialized()
                || !interactor.areParametersInitialized()
                || !interactor.areConfidencesInitialized()
                || !interactor.areXrefsInitialized()
                || !interactor.areParticipantsInitialized()
                || !interactor.areAliasesInitialized();
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public boolean isComplexFullyLoaded(IntactComplex complex){
        if (complex == null){
            return true;
        }
        if (!complex.areAnnotationsInitialized()
                || !complex.areLifeCycleEventsInitialized()
                || !complex.areParametersInitialized()
                || !complex.areConfidencesInitialized()
                || !complex.areXrefsInitialized()
                || !isCvInitialised(complex.getInteractionType())
                || !areParticipantsInitialised(complex)
                || !isCvInitialised(complex.getInteractorType())
                || !isCvInitialised(complex.getEvidenceType())
                || !complex.areAliasesInitialized()){
            return false;
        }
        return true;
    }

    private ModelledParticipant initialiseParticipant(ModelledParticipant det, uk.ac.ebi.intact.editor.controller.curate.cloner.InteractorCloner interactorCloner,
                                                      ModelledFeatureCloner featureCloner, ModelledParticipantCloner participantCloner) {
        if (det instanceof IntactModelledParticipant){
            if (!((IntactModelledParticipant)det).areFeaturesInitialized()
                    && ((IntactModelledParticipant)det).getAc() != null
                    && !getIntactDao().getEntityManager().contains(det)){
                IntactModelledParticipant reloaded = getIntactDao().getEntityManager().find(IntactModelledParticipant.class, ((IntactModelledParticipant)det).getAc());
                if (reloaded != null){
                    Interactor interactor = reloaded.getInteractor();
                    if (!isInteractorInitialised(interactor)){
                        initialiseInteractor(interactor, interactorCloner);
                    }

                    if (det.getBiologicalRole() != null && !isCvInitialised(det.getBiologicalRole())){
                        initialiseCv(det.getBiologicalRole());
                    }

                    initialiseFeatures(det, det.getFeatures(), featureCloner);
                    // detach relaoded object so no changes will be flushed
                    getIntactDao().getEntityManager().detach(reloaded);
                    participantCloner.copyInitialisedProperties((IntactModelledParticipant) det, reloaded);
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

        initialiseFeatures(det, det.getFeatures(), featureCloner);

        return det;
    }

    private boolean areParticipantsInitialised(IntactComplex interaction) {
        if (!interaction.areParticipantsInitialized()){
            return false;
        }

        for (ModelledParticipant part : interaction.getParticipants()){
            if (!isParticipantInitialised(part)){
                return false;
            }
        }
        return true;
    }

    protected boolean isParticipantInitialised(ModelledParticipant participant) {
        if (participant.getBiologicalRole() != null && !isCvInitialised(participant.getBiologicalRole())){
            return false;
        }
        else if (!isInteractorInitialised(participant.getInteractor())){
            return false;
        }
        else if (participant instanceof IntactModelledParticipant
                && !((IntactModelledParticipant) participant).areFeaturesInitialized()){
            return false;
        }
        else {
            for (ModelledFeature f : participant.getFeatures()){
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

    private ModelledFeature initialiseFeature(ModelledFeature det, ModelledFeatureCloner featCloner) {
        if (det instanceof IntactModelledFeature){
            if (areFeatureCollectionsLazy((IntactModelledFeature) det)
                    && ((IntactModelledFeature)det).getAc() != null
                    && !getIntactDao().getEntityManager().contains(det)){
                IntactModelledFeature reloaded = getIntactDao().getEntityManager().find(IntactModelledFeature.class, ((IntactModelledFeature) det).getAc());
                if (reloaded != null){
                    // initialise properties freshly loaded from db
                    initialiseRanges(reloaded.getRanges());

                    for (Object linked : reloaded.getLinkedFeatures()){
                        ((Feature)linked).getLinkedFeatures().size();
                    }

                    // detach relaoded object so no changes will be flushed
                    getIntactDao().getEntityManager().detach(reloaded);
                    featCloner.copyInitialisedProperties((IntactModelledFeature)det, reloaded);
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

    private boolean areFeatureCollectionsLazy(IntactModelledFeature det) {
        return !det.areLinkedFeaturesInitialized() || !det.areRangesInitialized();
    }

    private void initialiseFeatures(ModelledParticipant parent, Collection<ModelledFeature> features, ModelledFeatureCloner featureCloner) {
        List<ModelledFeature> originalFeatures = new ArrayList<ModelledFeature>(features);
        for (ModelledFeature r : originalFeatures){
            if (!isFeatureInitialised(r)){
                ModelledFeature reloaded = initialiseFeature(r, featureCloner);
                if (reloaded != r ){
                    features.remove(r);
                    parent.addFeature(reloaded);
                }
            }
        }
    }

    private void initialiseParticipants(Complex parent, Collection<ModelledParticipant> participants) {
        List<ModelledParticipant> originalParticipants = new ArrayList<ModelledParticipant>(participants);
        ModelledParticipantCloner participantCloner = new ModelledParticipantCloner();
        ModelledFeatureCloner featureCloner = new ModelledFeatureCloner();
        uk.ac.ebi.intact.editor.controller.curate.cloner.InteractorCloner interactorCloner = new uk.ac.ebi.intact.editor.controller.curate.cloner.InteractorCloner();
        for (ModelledParticipant det : originalParticipants){
            ModelledParticipant p = initialiseParticipant(det, interactorCloner, featureCloner, participantCloner);
            if (p != det){
                participants.remove(det);
                parent.addParticipant(p);
            }
        }
    }

    private boolean isFeatureInitialised(ModelledFeature feature) {
        if (feature.getType() != null && !isCvInitialised(feature.getType())){
            return false;
        }
        else if (feature.getRole() != null && !isCvInitialised(feature.getRole())){
            return false;
        }
        else if (feature instanceof IntactModelledFeature){
            IntactModelledFeature intactFeature = (IntactModelledFeature)feature;
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

    private void initialiseEvents(Collection<LifeCycleEvent> evidences) {
        for (LifeCycleEvent evt : evidences){
            if (evt instanceof AbstractLifeCycleEvent
                    && !isCvInitialised(((AbstractLifeCycleEvent) evt).getCvEvent())){
                CvTerm cvEvent = initialiseCv(((AbstractLifeCycleEvent)evt).getCvEvent());
                if (cvEvent != ((AbstractLifeCycleEvent)evt).getCvEvent()){
                    ((AbstractLifeCycleEvent)evt).setCvEvent(cvEvent);
                }
            }
        }
    }
}
