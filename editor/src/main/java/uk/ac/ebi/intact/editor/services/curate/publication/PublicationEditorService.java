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
package uk.ac.ebi.intact.editor.services.curate.publication;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.*;
import uk.ac.ebi.intact.editor.controller.curate.cloner.ComplexCloner;
import uk.ac.ebi.intact.editor.controller.curate.cloner.ExperimentCloner;
import uk.ac.ebi.intact.editor.controller.curate.cloner.PublicationCloner;
import uk.ac.ebi.intact.editor.controller.curate.publication.PublicationController;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.jami.model.IntactPrimaryObject;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.model.lifecycle.*;

import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Collection;

/**
 */
@Service
public class PublicationEditorService extends AbstractEditorService {

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countAnnotations(IntactPublication publication) {
        return getIntactDao().getPublicationDao().countAnnotationsForPublication(publication.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countXrefs(IntactPublication publication) {
        return getIntactDao().getPublicationDao().countXrefsForPublication(publication.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countInteractions(IntactPublication publication) {
        return getIntactDao().getPublicationDao().countInteractionsForPublication(publication.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countExperiments(IntactPublication publication) {
        return getIntactDao().getPublicationDao().countExperimentsForPublication(publication.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public IntactPublication loadPublicationByAc(String ac) {
        IntactPublication publication = getIntactDao().getEntityManager().find(IntactPublication.class, ac);

        if (publication != null){
            if (publication.getPubmedId() != null && (
                    "14681455".equals(publication.getPubmedId()) ||
                            "unassigned638".equals(publication.getPubmedId()) ||
                            "24288376".equals(publication.getPubmedId()) ||
                            "24214965".equals(publication.getPubmedId())
            )){
                return null;
            }
            // initialise annotations because needs caution
            initialiseAnnotations(publication.getDbAnnotations());
            // initialise xrefs because needs pubmed
            initialiseXrefs(publication.getDbXrefs());
            // initialise status
            if (publication.getCvStatus() != null){
                initialiseCv(publication.getCvStatus());
            }
            // initialise experiments
            initialiseEvidences(publication, publication.getExperiments());
            // initialise lifecycle events
            initialiseEvents(publication.getLifecycleEvents());
        }

        return publication;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public IntactPublication reloadFullyInitialisedPublication(IntactPublication publication) {
        if (publication == null){
            return null;
        }

        IntactPublication reloaded = null;
        if (arePublicationCollectionsLazy(publication)
                && publication.getAc() != null
                && !getIntactDao().getEntityManager().contains(publication)){
            reloaded = loadPublicationByAc(publication.getAc());
        }

        // we need first to merge with reloaded complex
        if (reloaded != null){
            // detach reloaded now so not changes will be committed
            getIntactDao().getEntityManager().detach(reloaded);
            PublicationCloner cloner = new PublicationCloner();
            cloner.copyInitialisedProperties((IntactPublication)publication, reloaded);
            publication = reloaded;
        }
        if (publication.getPubmedId() != null && (
                "14681455".equals(publication.getPubmedId()) ||
                        "unassigned638".equals(publication.getPubmedId()) ||
                        "24288376".equals(publication.getPubmedId()) ||
                        "24214965".equals(publication.getPubmedId())
        )){
            return null;
        }
        // initialise annotations because needs caution
        initialiseAnnotations(publication.getDbAnnotations());
        // initialise xrefs because needs pubmed
        initialiseXrefs(publication.getDbXrefs());
        // initialise status
        if (publication.getCvStatus() != null && !isCvInitialised(publication.getCvStatus())){
            CvTerm cv = initialiseCv(publication.getCvStatus());
            if (cv != publication.getCvStatus()){
                publication.setCvStatus(cv);
            }
        }
        // initialise experiments
        initialiseEvidences(publication, publication.getExperiments());
        // initialise lifecycle events
        initialiseEvents(publication.getLifecycleEvents());

        return publication;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public boolean isPublicationFullyLoaded(IntactPublication publication){
        if (publication == null){
            return true;
        }
        if (!publication.areAnnotationsInitialized()
                || !publication.areLifeCycleEventsInitialized()
                || !publication.areAnnotationsInitialized()
                || !publication.areXrefsInitialized()
                || (publication.getCvStatus() != null && !isCvInitialised(publication.getCvStatus()))
                || !areExperimentsInitialised(publication)){
            return false;
        }
        return true;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public Collection<Annotation> initialisePublicationAnnotations(IntactPublication releasable) {
        // reload complex without flushing changes
        IntactPublication reloaded = releasable;
        // merge current user because detached
        if (releasable.getAc() != null && !getIntactDao().getEntityManager().contains(releasable)){
            reloaded = getIntactDao().getEntityManager().find(IntactPublication.class, releasable.getAc());
            if (reloaded == null){
                reloaded = releasable;
            }
        }

        initialiseAnnotations(reloaded.getAnnotations());
        return reloaded.getAnnotations();
    }

    private boolean arePublicationCollectionsLazy(IntactPublication interactor) {
        return !interactor.areAnnotationsInitialized()
                || !interactor.areLifeCycleEventsInitialized()
                || !interactor.areExperimentsInitialized()
                || !interactor.areAnnotationsInitialized()
                || !interactor.areXrefsInitialized();
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public IntactPublication loadPublicationByAcOrPubmedId(String id) {

        IntactPublication pub = getIntactDao().getEntityManager().find(IntactPublication.class, id);
        if (pub == null){
            pub = getIntactDao().getPublicationDao().getByPubmedId(id);
        }

        if (pub != null){
            if (pub.getPubmedId() != null && (
                    "14681455".equals(pub.getPubmedId()) ||
                            "unassigned638".equals(pub.getPubmedId()) ||
                            "24288376".equals(pub.getPubmedId()) ||
                            "24214965".equals(pub.getPubmedId())
            )){
                return null;
            }
            // initialise annotations because needs caution
            initialiseAnnotations(pub.getDbAnnotations());
            // initialise xrefs because needs pubmed
            initialiseXrefs(pub.getDbXrefs());
            // initialise status
            if (pub.getCvStatus() != null){
                initialiseCv(pub.getCvStatus());
            }
            // initialise experiments
            initialiseEvidences(pub, pub.getExperiments());
            // initialise lifecycle events
            initialiseEvents(pub.getLifecycleEvents());
        }

        return pub;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public boolean doesDatasetAlreadyExist(String datasetName) {
        String sql = "select distinct a.value from PublicationAnnotation a where a.topic.shortName = :dataset and lower(a.value) like :name";
        Query query = getIntactDao().getEntityManager().createQuery(sql);
        query.setParameter("dataset", PublicationController.DATASET);
        query.setParameter("name", datasetName.toLowerCase() + " -%");

        if (!query.getResultList().isEmpty()) {
            return true;
        }

        return false;
    }

    private boolean isExperimentInitialised(Experiment exp){
        if (exp instanceof IntactExperiment){
            IntactExperiment intactExp = (IntactExperiment)exp;
            return intactExp.areAnnotationsInitialized() && intactExp.areXrefsInitialized();
        }
        return true;
    }

    private boolean areExperimentCollectionsLazy(IntactExperiment det) {
        return !det.areAnnotationsInitialized() || !det.areXrefsInitialized();
    }

    private boolean areExperimentsInitialised(IntactPublication pub) {
        if (!pub.areExperimentsInitialized()){
            return false;
        }

        for (Experiment part : pub.getExperiments()){
            if (!isExperimentInitialised(part)){
                return false;
            }
        }
        return true;
    }

    private Experiment initialiseExperiment(Experiment det){
        if (det instanceof IntactExperiment){
            if (areExperimentCollectionsLazy((IntactExperiment) det)
                    && ((IntactExperiment)det).getAc() != null
                    && !getIntactDao().getEntityManager().contains(det)){
                IntactExperiment reloaded = getIntactDao().getEntityManager().find(IntactExperiment.class, ((IntactExperiment) det).getAc());
                if (reloaded != null){
                    // initialise properties freshly loaded from db
                    initialiseAnnotations(reloaded.getAnnotations());
                    initialiseXrefs(reloaded.getXrefs());

                    // detach relaoded object so no changes will be flushed
                    getIntactDao().getEntityManager().detach(reloaded);
                    ExperimentCloner cloner = new ExperimentCloner(false);
                    cloner.copyInitialisedProperties((IntactExperiment)det, reloaded);
                    // will return reloaded object
                    det = reloaded;
                }
            }

            initialiseAnnotations(det.getAnnotations());
            initialiseXrefs(det.getXrefs());
        }
        return det;
    }

    private void initialiseEvidences(Publication parent, Collection<Experiment> evidences) {
        Collection<Experiment> originalExperiments = new ArrayList<Experiment>(evidences);
        for (Experiment exp : originalExperiments){
            if (!isExperimentInitialised(exp)){
                Experiment reloaded = initialiseExperiment(exp);
                if (reloaded != exp){
                    evidences.remove(exp);
                    parent.addExperiment(reloaded);
                }
            }
        }
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
