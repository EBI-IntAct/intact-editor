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
package uk.ac.ebi.intact.editor.services.curate.experiment;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import psidev.psi.mi.jami.utils.CvTermUtils;
import uk.ac.ebi.intact.editor.controller.curate.cloner.ExperimentCloner;
import uk.ac.ebi.intact.editor.controller.curate.cloner.InteractionEvidenceCloner;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.model.lifecycle.Releasable;

import java.util.ArrayList;
import java.util.Collection;

/**
 */
@Service
public class ExperimentEditorService extends AbstractEditorService {

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countAnnotations(IntactExperiment experiment) {
        return getIntactDao().getExperimentDao().countAnnotationsForExperiment(experiment.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countXrefs(IntactExperiment experiment) {
        return getIntactDao().getExperimentDao().countXrefsForExperiment(experiment.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countInteractions(IntactExperiment experiment) {
        return getIntactDao().getExperimentDao().countInteractionsForExperiment(experiment.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countVariableParameters(IntactExperiment experiment) {
        return getIntactDao().getExperimentDao().countVariableParametersForExperiment(experiment.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public IntactExperiment loadExperimentByAc(String ac) {

        return loadExperimentByAc(ac, false);
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public IntactExperiment loadExperimentByAc(String ac, boolean loadInteractions) {
        IntactExperiment experiment = getIntactDao().getEntityManager().find(IntactExperiment.class, ac);

        if (experiment != null){
            if (CvTermUtils.isCvTerm(experiment.getInteractionDetectionMethod(), Experiment.INFERRED_BY_CURATOR_MI,
                    Experiment.INFERRED_BY_CURATOR)){
                return null;
            }

            // initialise annotations because needs caution
            initialiseAnnotations(experiment.getAnnotations());
            // initialise xrefs
            initialiseXrefs(experiment.getXrefs());
            // initialise variable parameters
            initialiseVariableParameters(experiment.getVariableParameters());

            // initialise publication annotations and xrefs
            if (experiment.getPublication() != null){
                initialiseXrefs(((IntactPublication)experiment.getPublication()).getDbXrefs());
                initialiseAnnotations(((IntactPublication) experiment.getPublication()).getDbAnnotations());
            }

            initialiseCv(experiment.getInteractionDetectionMethod());
            initialiseCv(experiment.getParticipantIdentificationMethod());

            if (loadInteractions){
                initialiseEvidences(experiment, experiment.getInteractionEvidences());
            }
        }

        return experiment;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countAcceptedExperiments(String pubAc) {
        IntactPublication reloaded = getIntactDao().getEntityManager().find(IntactPublication.class, pubAc);

        Collection<Experiment> experiments = reloaded.getExperiments();
        int expAccepted = 0;

        for (Experiment exp : experiments) {
            if (AnnotationUtils.collectFirstAnnotationWithTopic(exp.getAnnotations(), null, Releasable.ACCEPTED) != null) {
                expAccepted++;
            }
        }

        return expAccepted;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countRejectedExperiments(String pubAc) {
        IntactPublication reloaded = getIntactDao().getEntityManager().find(IntactPublication.class, pubAc);

        Collection<Experiment> experiments = reloaded.getExperiments();
        int rejected = 0;

        for (Experiment exp : experiments) {
            if (AnnotationUtils.collectFirstAnnotationWithTopic(exp.getAnnotations(), null, Releasable.TO_BE_REVIEWED) != null) {
                rejected++;
            }
        }

        return rejected;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public Collection<Annotation> initialiseExperimentAnnotations(IntactExperiment releasable) {
        // reload complex without flushing changes
        IntactExperiment reloaded = releasable;
        // merge current user because detached
        if (releasable.getAc() != null && !getIntactDao().getEntityManager().contains(releasable)){
            reloaded = getIntactDao().getEntityManager().find(IntactExperiment.class, releasable.getAc());
            if (reloaded == null){
                reloaded = releasable;
            }
        }

        initialiseAnnotations(reloaded.getAnnotations());
        return reloaded.getAnnotations();
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public IntactExperiment reloadFullyInitialisedExperiment(IntactExperiment exp) {
        if (exp == null){
            return null;
        }

        IntactExperiment reloaded = null;
        if (areExperimentCollectionsLazy(exp)
                && exp.getAc() != null
                && !getIntactDao().getEntityManager().contains(exp)){
            reloaded = loadExperimentByAc(exp.getAc(), exp.areInteractionEvidencesInitialized());
        }

        // we need first to merge with reloaded complex
        if (reloaded != null){
            // detach reloaded now so not changes will be committed
            getIntactDao().getEntityManager().detach(reloaded);
            ExperimentCloner cloner = new ExperimentCloner(exp.areInteractionEvidencesInitialized());
            cloner.copyInitialisedProperties(exp, reloaded);
            exp = reloaded;
        }

        // initialise annotations because needs caution
        initialiseAnnotations(exp.getAnnotations());
        // initialise xrefs
        initialiseXrefs(exp.getXrefs());
        // initialise variable parameters
        initialiseVariableParameters(exp.getVariableParameters());

        // initialise publication annotations and xrefs
        if (exp.getPublication() != null && !isPublicationInitialised(exp.getPublication())){
            Publication pub = initialisePublication(exp.getPublication());
            if (pub != exp.getPublication()){
                exp.setPublication(pub);
            }
        }

        // initialise evidences if not done
        if (exp.areInteractionEvidencesInitialized()){
             initialiseEvidences(exp, exp.getInteractionEvidences());
        }

        if (!isCvInitialised(exp.getInteractionDetectionMethod())){
            CvTerm cv = initialiseCv(exp.getInteractionDetectionMethod());
            if (cv != exp.getInteractionDetectionMethod()){
                exp.setInteractionDetectionMethod(cv);
            }
        }
        if (exp.getParticipantIdentificationMethod() != null && !isCvInitialised(exp.getParticipantIdentificationMethod())){
            CvTerm cv2 = initialiseCv(exp.getParticipantIdentificationMethod());
            if (cv2 != exp.getParticipantIdentificationMethod()){
                exp.setParticipantIdentificationMethod(cv2);
            }
        }

        return exp;
    }


    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public IntactPublication loadPublicationByAcOrPubmedId(String id) {

        IntactPublication pub = getIntactDao().getEntityManager().find(IntactPublication.class, id);
        if (pub == null){
            pub = getIntactDao().getPublicationDao().getByPubmedId(id);
        }

        // initialise pub
        if (pub != null){
            initialiseXrefs(pub.getDbXrefs());
            initialiseAnnotations(pub.getDbAnnotations());
        }

        return pub;
    }

    private void initialiseEvidences(IntactExperiment parent, Collection<InteractionEvidence> evidences) {
        Collection<InteractionEvidence> originalInteractions = new ArrayList<InteractionEvidence>(evidences);
        InteractionEvidenceCloner cloner = new InteractionEvidenceCloner();
        for (InteractionEvidence ev : originalInteractions){
            if (ev instanceof IntactInteractionEvidence && !isInteractionEvidenceInitialised((IntactInteractionEvidence)ev)){
                InteractionEvidence reloaded = initialiseInteraction(ev, cloner);
                if (reloaded != ev){
                    evidences.remove(ev);
                    parent.addInteractionEvidence(reloaded);
                }
            }
        }
    }

    private InteractionEvidence initialiseInteraction(InteractionEvidence inter, InteractionEvidenceCloner interactionCloner) {
        if (inter instanceof IntactInteractionEvidence){
            if (((IntactInteractionEvidence) inter).areAnnotationsInitialized()
                    && ((IntactInteractionEvidence)inter).getAc() != null && !getIntactDao().getEntityManager().contains(inter)){
                IntactInteractionEvidence reloaded = getIntactDao().getEntityManager().find(IntactInteractionEvidence.class, ((IntactInteractionEvidence)inter).getAc());
                if (reloaded != null){
                    // initialise freshly loaded properties
                    initialiseAnnotations(reloaded.getDbAnnotations());
                    // detach object so no changes will be flushed
                    getIntactDao().getEntityManager().detach(reloaded);
                    interactionCloner.copyInitialisedProperties((IntactInteractionEvidence) inter, reloaded);
                    // will return reloaded object
                    inter = reloaded;
                }
            }
            inter.getExperiment();
            initialiseAnnotations(((IntactInteractor) inter).getDbAnnotations());
        }
        return inter;
    }

    private void initialiseVariableParameters(Collection<VariableParameter> parameters) {
        for (VariableParameter param : parameters){
            if (param.getUnit() != null && !isCvInitialised(param.getUnit())){
                CvTerm unit = initialiseCv(param.getUnit());
                if (unit != param.getUnit()){
                    param.setUnit(unit);
                }
            }

            if (((IntactVariableParameter)param).getId() != null){
                Hibernate.initialize(param.getVariableValues());
            }
        }
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public boolean isExperimentFullyLoaded(IntactExperiment experiment){
        if (experiment == null){
            return true;
        }
        if (areExperimentCollectionsLazy(experiment)
                || !isCvInitialised(experiment.getInteractionDetectionMethod())
                || !areInteractionInitialised(experiment)
                || (experiment.getParticipantIdentificationMethod() != null && !isCvInitialised(experiment.getParticipantIdentificationMethod()))
                || (experiment.getPublication() != null && !isPublicationInitialised(experiment.getPublication()))){
            return false;
        }
        return true;
    }

    private boolean areExperimentCollectionsLazy(IntactExperiment experiment) {
        return !experiment.areAnnotationsInitialized()
                || !experiment.areXrefsInitialized()
                || !experiment.areVariableParametersInitialized();
    }

    private boolean areInteractionInitialised(IntactExperiment experiment) {
        if (experiment.areInteractionEvidencesInitialized()){
            for (InteractionEvidence ev : experiment.getInteractionEvidences()){
                if (!isInteractionEvidenceInitialised((IntactInteractionEvidence) ev)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isInteractionEvidenceInitialised(IntactInteractionEvidence ev) {
        if (!ev.areAnnotationsInitialized()){
            return false;
        }
        return true;
    }

    private boolean isPublicationInitialised(Publication pub){
        if (pub instanceof IntactPublication){
            return !arePublicationCollectionsLazy((IntactPublication)pub);
        }
        return true;
    }

    private Publication initialisePublication(Publication det) {
        if (det instanceof IntactPublication){
            if (arePublicationCollectionsLazy((IntactPublication) det)
                    && ((IntactPublication)det).getAc() != null
                    && !getIntactDao().getEntityManager().contains(det)){
                IntactPublication reloaded = getIntactDao().getEntityManager().find(IntactPublication.class, ((IntactPublication) det).getAc());
                if (reloaded != null){
                    // initialise properties freshly loaded from db
                    initialiseXrefs(reloaded.getDbXrefs());
                    initialiseAnnotations(reloaded.getDbAnnotations());

                    // detach relaoded object so no changes will be flushed
                    getIntactDao().getEntityManager().detach(reloaded);
                    uk.ac.ebi.intact.editor.controller.curate.cloner.PublicationCloner cloner = new uk.ac.ebi.intact.editor.controller.curate.cloner.PublicationCloner();
                    cloner.copyInitialisedProperties((IntactPublication)det, reloaded);
                    // will return reloaded object
                    det = reloaded;
                }

                // initialise properties freshly loaded from db
                initialiseXrefs(((IntactPublication) det).getDbXrefs());
                initialiseAnnotations(((IntactPublication) det).getDbAnnotations());
            }
        }
        return det;
    }

    private boolean arePublicationCollectionsLazy(IntactPublication det) {
        return !((IntactPublication) det).areAnnotationsInitialized() || !((IntactPublication) det).areXrefsInitialized();
    }
}
