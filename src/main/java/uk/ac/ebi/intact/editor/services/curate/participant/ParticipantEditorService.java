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
package uk.ac.ebi.intact.editor.services.curate.participant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.*;
import uk.ac.ebi.intact.editor.controller.curate.cloner.EditorCloner;
import uk.ac.ebi.intact.editor.controller.curate.cloner.InteractorCloner;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.editor.services.curate.cvobject.CvObjectService;
import uk.ac.ebi.intact.jami.model.extension.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
@Service
public class ParticipantEditorService extends AbstractEditorService {

    @Resource(name = "cvObjectService")
    private CvObjectService cvObjectService;

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countAnnotations(AbstractIntactParticipant participant) {
        return getIntactDao().getParticipantDao(participant.getClass()).countAnnotationsForParticipant(participant.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countXrefs(AbstractIntactParticipant participant) {
        return getIntactDao().getParticipantDao(participant.getClass()).countXrefsForParticipant(participant.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countAliases(AbstractIntactParticipant participant) {
        return getIntactDao().getParticipantDao(participant.getClass()).countAliasesForParticipant(participant.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countFeatures(AbstractIntactParticipant participant) {
        return getIntactDao().getParticipantDao(participant.getClass()).countFeaturesForParticipant(participant.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countParameters(IntactParticipantEvidence participant) {
        return getIntactDao().getParticipantEvidenceDao().countParametersForParticipant(participant.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countConfidences(IntactParticipantEvidence participant) {
        return getIntactDao().getParticipantEvidenceDao().countConfidencesForParticipant(participant.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countIdentificationMethods(IntactParticipantEvidence participant) {
        return getIntactDao().getParticipantEvidenceDao().countIdentificationMethodsForParticipant(participant.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countExperimentalPreparations(IntactParticipantEvidence participant) {
        return getIntactDao().getParticipantEvidenceDao().countExperimentalPreparationsForParticipant(participant.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countCausalityStatements(AbstractIntactParticipant participant) {
        return getIntactDao().getParticipantDao(participant.getClass()).countCausalRelationshipsForParticipant(participant.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public <T extends AbstractIntactParticipant> T loadParticipantByAc(String ac, Class<T> participantClass, EditorCloner featureCloner) {
        T participant = getIntactDao().getEntityManager().find(participantClass, ac);

        if (participant != null){
            // initialise annotations because needs caution
            initialiseAnnotations(participant.getAnnotations());
            // initialise aliases
            initialiseAliases(participant.getAliases());
            // initialise xrefs
            initialiseXrefs(participant.getXrefs());

            // load base types
            if (participant.getBiologicalRole() != null){
                initialiseCv(participant.getBiologicalRole());
            }
            if (participant instanceof IntactParticipantEvidence){
                IntactParticipantEvidence evidence = (IntactParticipantEvidence)participant;
                if (evidence.getExperimentalRole() != null){
                    initialiseCv(evidence.getExperimentalRole());
                }
                // initialise experimental preparations
                initialiseCvs(evidence.getExperimentalPreparations());
                // initialise identification methods
                initialiseCvs(evidence.getDbIdentificationMethods());
                // initialise confidences
                initialiseConfidence(evidence.getConfidences());
                // initialise parameters
                initialiseParameters(evidence.getParameters());
            }

            // load participant interactor
            initialiseInteractor(participant.getInteractor(), new InteractorCloner());

            // load features
            initialiseFeatures(participant, participant.getFeatures(), featureCloner);
        }

        return participant;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public <T extends AbstractIntactParticipant> T reloadFullyInitialisedParticipant(T participant, EditorCloner participantCloner, EditorCloner featureCloner) {
        if (participant == null){
            return null;
        }

        T reloaded = null;
        if (areParticipantCollectionsLazy(participant)
                && participant.getAc() != null
                && !getIntactDao().getEntityManager().contains(participant)){
            reloaded = (T) loadParticipantByAc(participant.getAc(), participant.getClass(), featureCloner);
        }

        // we need first to merge with reloaded complex
        if (reloaded != null){
            // detach reloaded now so not changes will be committed
            getIntactDao().getEntityManager().detach(reloaded);
            participantCloner.copyInitialisedProperties(participant, reloaded);
            participant = reloaded;
        }
        // initialise annotations because needs caution
        initialiseAnnotations(participant.getAnnotations());
        // initialise aliases
        initialiseAliases(participant.getAliases());
        // initialise xrefs
        initialiseXrefs(participant.getXrefs());

        // load base types
        if (participant.getBiologicalRole() != null && !isCvInitialised(participant.getBiologicalRole())){
            CvTerm cv = initialiseCv(participant.getBiologicalRole());
            if (cv != participant.getBiologicalRole()){
                participant.setBiologicalRole(cv);
            }
        }
        if (participant instanceof IntactParticipantEvidence){
            IntactParticipantEvidence evidence = (IntactParticipantEvidence)participant;
            if (evidence.getExperimentalRole() != null && !isCvInitialised(evidence.getExperimentalRole())){
                CvTerm cv = initialiseCv(evidence.getExperimentalRole());
                if (cv != evidence.getExperimentalRole()){
                    evidence.setExperimentalRole(cv);
                }
            }
            // initialise experimental preparations
            initialiseCvs(evidence.getExperimentalPreparations());
            // initialise identification methods
            initialiseCvs(evidence.getDbIdentificationMethods());
            // initialise confidences
            initialiseConfidence(evidence.getConfidences());
            // initialise parameters
            initialiseParameters(evidence.getParameters());
        }

        // load participant interactor
        if (!isInteractorInitialised(participant.getInteractor())){
            Interactor reloadedInteractor = initialiseInteractor(participant.getInteractor(), new InteractorCloner());
            if (reloadedInteractor != participant.getInteractor()){
                participant.setInteractor(reloadedInteractor);
            }
        }

        // load features
        initialiseFeatures(participant, participant.getFeatures(), featureCloner);

        return participant;
    }

    private <T extends AbstractIntactParticipant> boolean areParticipantCollectionsLazy(T participant) {
        return !participant.areAnnotationsInitialized()
                || !participant.areAliasesInitialized()
                || !participant.areFeaturesInitialized()
                || !participant.areXrefsInitialized()
                || (participant instanceof IntactParticipantEvidence && !isParticipantEvidenceFullyInitialised((IntactParticipantEvidence)participant, false));
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public <T extends AbstractIntactParticipant> boolean isParticipantFullyLoaded(T participant){
        if (participant == null){
            return true;
        }
        if (!participant.areAnnotationsInitialized()
                || !participant.areAliasesInitialized()
                || !areFeaturesInitialised(participant)
                || (participant.getBiologicalRole() != null && !isCvInitialised(participant.getBiologicalRole()))
                || !participant.areXrefsInitialized()
                || !isInteractorInitialised(participant.getInteractor())
                || (participant instanceof IntactParticipantEvidence && !isParticipantEvidenceFullyInitialised((IntactParticipantEvidence)participant, true))){
            return false;
        }
        return true;
    }

    @Override
    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public boolean isInteractorInitialised(Interactor interactor) {
        return super.isInteractorInitialised(interactor);
    }

    @Override
    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public Interactor initialiseInteractor(Interactor inter, InteractorCloner interactorCloner) {
        return super.initialiseInteractor(inter, interactorCloner);
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public Collection<Annotation> initialiseParticipantAnnotations(AbstractIntactParticipant releasable) {
        // reload complex without flushing changes
        AbstractIntactParticipant reloaded = releasable;
        // merge current user because detached
        if (releasable.getAc() != null && !getIntactDao().getEntityManager().contains(releasable)){
            reloaded = getIntactDao().getEntityManager().find(releasable.getClass(), releasable.getAc());
            if (reloaded == null){
                reloaded = releasable;
            }
        }

        initialiseAnnotations(reloaded.getAnnotations());
        return reloaded.getAnnotations();
    }

    private boolean isParticipantEvidenceFullyInitialised(IntactParticipantEvidence participant, boolean checkExpRole) {
        return !participant.areIdentificationMethodsInitialized()
                        || !participant.areExperimentalPreparationsInitialized()
                        || !participant.areConfidencesInitialized()
                        || !participant.areParametersInitialized()
                        || (checkExpRole && participant.getExperimentalRole() != null && !isCvInitialised(participant.getExperimentalRole()));
    }

    private void initialiseCvs(Collection<CvTerm> detectionMethods) {
        Collection<CvTerm> dets = new ArrayList<CvTerm>(detectionMethods);
        for (CvTerm det : dets){
            if (!isCvInitialised(det)){
                CvTerm reloaded = initialiseCv(det);
                if (reloaded != det){
                    detectionMethods.remove(det);
                    detectionMethods.add(reloaded);
                }
            }
        }
    }

    private <T extends Participant, F extends Feature> void initialiseFeatures(T parent, Collection<F> features, EditorCloner featureCloner) {
        List<F> originalFeatures = new ArrayList<F>(features);
        for (F r : originalFeatures){
            if (!isFeatureInitialised(r)){
                F reloaded = initialiseFeature(r, featureCloner);
                if (reloaded != r ){
                    features.remove(r);
                    parent.addFeature(reloaded);
                }
            }
        }
    }

    private <T extends Feature> T initialiseFeature(T det, EditorCloner featCloner) {
        if (det instanceof AbstractIntactFeature){
            if (areFeatureCollectionsLazy((AbstractIntactFeature) det)
                    && ((AbstractIntactFeature)det).getAc() != null
                    && !getIntactDao().getEntityManager().contains(det)){
                AbstractIntactFeature reloaded = (AbstractIntactFeature)getIntactDao().getEntityManager().find(det.getClass(), ((AbstractIntactFeature) det).getAc());
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
                    det = (T)reloaded;
                }
            }

            initialiseRanges(det.getRanges());

            for (Object linked : det.getLinkedFeatures()){
                ((Feature)linked).getLinkedFeatures().size();
            }
        }
        return det;
    }

    private boolean areFeaturesInitialised(AbstractIntactParticipant participant) {
        if (!participant.areFeaturesInitialized()){
            return false;
        }

        for (Object part : participant.getFeatures()){
            if (!isFeatureInitialised((Feature) part)){
                return false;
            }
        }
        return true;
    }

    private boolean areFeatureCollectionsLazy(AbstractIntactFeature det) {
        return !det.areLinkedFeaturesInitialized() || !det.areRangesInitialized();
    }

    private <F extends Feature> boolean isFeatureInitialised(F feature) {
        if (feature.getType() != null && !isCvInitialised(feature.getType())){
            return false;
        }
        else if (feature.getRole() != null && !isCvInitialised(feature.getRole())){
            return false;
        }
        else if (feature instanceof AbstractIntactFeature){
            AbstractIntactFeature intactFeature = (AbstractIntactFeature)feature;
            if (!intactFeature.areLinkedFeaturesInitialized()){
                return false;
            }
            else if (!intactFeature.areRangesInitialized()){
                return false;
            }
            else{
                for (Object r : intactFeature.getRanges()){
                    if (!isRangeInitialised((Range)r)){
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
}
