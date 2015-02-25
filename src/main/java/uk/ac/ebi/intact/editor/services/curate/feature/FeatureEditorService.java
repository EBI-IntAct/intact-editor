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
package uk.ac.ebi.intact.editor.services.curate.feature;

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
public class FeatureEditorService extends AbstractEditorService {

    @Resource(name = "cvObjectService")
    private CvObjectService cvObjectService;

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countAnnotations(AbstractIntactFeature feature) {
        return getIntactDao().getFeatureDao(feature.getClass()).countAnnotationsForFeature(feature.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countXrefs(AbstractIntactFeature feature) {
        return getIntactDao().getFeatureDao(feature.getClass()).countXrefsForFeature(feature.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countAliases(AbstractIntactFeature feature) {
        return getIntactDao().getFeatureDao(feature.getClass()).countAliasesForFeature(feature.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countRanges(AbstractIntactFeature feature) {
        return getIntactDao().getFeatureDao(feature.getClass()).countRangesForFeature(feature.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countParameters(IntactFeatureEvidence feature) {
        return getIntactDao().getFeatureEvidenceDao().countParametersForFeature(feature.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countDetectionMethods(IntactFeatureEvidence feature) {
        int first = feature.getFeatureIdentification() != null ? 1 : 0;
        return first + getIntactDao().getFeatureEvidenceDao().countDetectionMethodsForFeature(feature.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public <T extends AbstractIntactFeature> T loadFeatureByAc(String ac, Class<T> featureClass) {
        T feature = getIntactDao().getEntityManager().find(featureClass, ac);

        if (feature != null){
            // initialise annotations because needs caution
            initialiseAnnotations(feature.getAnnotations());
            // initialise xrefs
            initialiseXrefs(feature.getDbXrefs());
            // initialise aliases
            initialiseAliases(feature.getAliases());
            // initialise parameters and detection methods
            if (feature instanceof IntactFeatureEvidence){
               initialiseCv(((IntactFeatureEvidence) feature).getFeatureIdentification());
               initialiseParameters(((IntactFeatureEvidence) feature).getParameters());
               initialiseDetectionMethods(((IntactFeatureEvidence) feature).getDbDetectionMethods());
            }

            // load base types
            if (feature.getType() != null){
                initialiseCv(feature.getType());
            }
            if (feature.getRole() != null){
                initialiseCv(feature.getRole());
            }

            // load participant interactor
            if (feature.getParticipant() != null){
                initialiseParticipant(feature.getParticipant());
            }

            // load feature ranges
            initialiseRanges(feature.getRanges());
        }

        return feature;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public <T extends AbstractIntactFeature> T reloadFullyInitialisedFeature(T feature, EditorCloner cloner) {
        if (feature == null){
            return null;
        }
        T reloaded = null;
        if (areFeatureCollectionsLazy(feature)
                && feature.getAc() != null
                && !getIntactDao().getEntityManager().contains(feature)){
            reloaded = (T) loadFeatureByAc(feature.getAc(), feature.getClass());
        }

        // we need first to merge with reloaded complex
        if (reloaded != null){
            // detach reloaded now so not changes will be committed
            getIntactDao().getEntityManager().detach(reloaded);
            cloner.copyInitialisedProperties(feature, reloaded);
            feature = reloaded;
        }

        // initialise annotations because needs caution
        initialiseAnnotations(feature.getAnnotations());
        // initialise xrefs
        initialiseXrefs(feature.getDbXrefs());
        // initialise aliases
        initialiseAliases(feature.getAliases());
        // initialise parameters and detection methods
        if (feature instanceof IntactFeatureEvidence){
            IntactFeatureEvidence featureEv = (IntactFeatureEvidence)feature;
            if (featureEv.getFeatureIdentification() != null && !isCvInitialised(featureEv.getFeatureIdentification())){
                CvTerm reloadedCv = initialiseCv(featureEv.getFeatureIdentification());
                if (reloadedCv != featureEv.getFeatureIdentification()){
                    featureEv.setFeatureIdentification(reloadedCv);
                }
            }
            initialiseParameters(((IntactFeatureEvidence) feature).getParameters());
            initialiseDetectionMethods(((IntactFeatureEvidence) feature).getDbDetectionMethods());
        }

        // load base types
        if (feature.getType() != null && !isCvInitialised(feature.getType())){
            CvTerm reloadedCv = initialiseCv(feature.getType());
            if (reloadedCv != feature.getType()){
                feature.setType(reloadedCv);
            }
        }
        if (feature.getRole() != null && !isCvInitialised(feature.getRole())){
            CvTerm reloadedCv = initialiseCv(feature.getRole());
            if (reloadedCv != feature.getRole()){
                feature.setRole(reloadedCv);
            }
        }

        // load participant interactor
        if (feature.getParticipant() != null && !isParticipantInitialised(feature.getParticipant())){
            initialiseParticipant(feature.getParticipant());
        }

        // load feature ranges
        initialiseRanges(feature.getRanges());

        return feature;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public boolean isFeatureFullyLoaded(AbstractIntactFeature feature){
        if (feature == null){
            return true;
        }
        if (areFeatureCollectionsLazy(feature)
                || (((IntactFeatureEvidence) feature).getFeatureIdentification() != null && !isCvInitialised(((IntactFeatureEvidence) feature).getFeatureIdentification()))
                || (feature.getType() != null && !isCvInitialised(feature.getType()))
                || (feature.getRole() != null && !isCvInitialised(feature.getRole()))
                || (feature.getParticipant() != null && !isParticipantInitialised(feature.getParticipant()))
                || !feature.areAliasesInitialized()){
            return false;
        }
        return true;
    }

    private boolean areFeatureCollectionsLazy(AbstractIntactFeature feature) {
        return !feature.areAnnotationsInitialized()
                || !feature.areXrefsInitialized()
                || (feature instanceof IntactFeatureEvidence
                && (!((IntactFeatureEvidence) feature).areDetectionMethodsInitialized()
                || !((IntactFeatureEvidence) feature).areParametersInitialized()));
    }

    private void initialiseParticipant(Entity participant) {

        if (!isInteractorInitialised(participant.getInteractor())){
            Interactor reloaded = initialiseInteractor(participant.getInteractor(), new InteractorCloner());
            if (reloaded != participant.getInteractor()){
                participant.setInteractor(reloaded);
            }
        }
    }

    private boolean isParticipantInitialised(Entity participant){
         if (!isInteractorInitialised(participant.getInteractor())){
             return false;
         }
        return true;
    }

    @Override
    protected void initialiseOtherInteractorProperties(IntactInteractor inter) {
        // initialise participants
        if (inter instanceof IntactComplex){
            List<ModelledParticipant> originalParticipants = new ArrayList<ModelledParticipant>(((IntactComplex) inter).getParticipants());
            for (ModelledParticipant p : originalParticipants){
                if (!isParticipantInitialised(p)){
                    initialiseParticipant(p);
                }
            }
        }
    }

    protected boolean areInteractorCollectionsLazy(IntactInteractor inter) {
        return !inter.areXrefsInitialized()
                || !inter.areAnnotationsInitialized()
                || (inter instanceof IntactComplex && !((IntactComplex) inter).areParticipantsInitialized());
    }

    protected boolean isInteractorInitialised(Interactor interactor) {
        if(interactor instanceof IntactInteractor){
            IntactInteractor intactInteractor = (IntactInteractor)interactor;
            if (!intactInteractor.areXrefsInitialized()
                    || !intactInteractor.areAnnotationsInitialized()){
                return false;
            }

            if (intactInteractor instanceof IntactComplex
                    && !((IntactComplex) intactInteractor).areParticipantsInitialized()){
                return false;
            }
        }

        if (interactor instanceof Complex){
            for (ModelledParticipant p : ((Complex) interactor).getParticipants()){
                if (!isInteractorInitialised(p.getInteractor())){
                    return false;
                }
            }
        }
        return true;
    }

    private void initialiseDetectionMethods(Collection<CvTerm> detectionMethods) {
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

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public Collection<Annotation> initialiseFeatureAnnotations(AbstractIntactFeature releasable) {
        // reload complex without flushing changes
        AbstractIntactFeature reloaded = releasable;
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
}
