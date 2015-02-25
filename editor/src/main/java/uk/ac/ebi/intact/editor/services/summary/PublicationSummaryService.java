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
package uk.ac.ebi.intact.editor.services.summary;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.Annotation;
import psidev.psi.mi.jami.model.Experiment;
import psidev.psi.mi.jami.model.Publication;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.jami.model.extension.IntactPublication;
import uk.ac.ebi.intact.jami.model.lifecycle.LifeCycleEvent;
import uk.ac.ebi.intact.jami.model.lifecycle.LifeCycleEventType;
import uk.ac.ebi.intact.jami.model.lifecycle.LifeCycleStatus;
import uk.ac.ebi.intact.jami.model.lifecycle.Releasable;
import uk.ac.ebi.intact.jami.service.IntactQueryResultIterator;
import uk.ac.ebi.intact.jami.service.IntactService;
import uk.ac.ebi.intact.jami.service.PublicationService;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.annotation.Resource;
import java.util.*;

/**
 */
@Service
public class PublicationSummaryService extends AbstractEditorService implements IntactService<PublicationSummary> {

    @Resource(name = "publicationService")
    private PublicationService publicationService;

    public PublicationSummaryService() {
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public long countAll() {
        return this.publicationService.countAll();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<PublicationSummary> iterateAll() {
        return new IntactQueryResultIterator<PublicationSummary>(this);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<PublicationSummary> fetchIntactObjects(int first, int max) {
        List<Publication> pubs = this.publicationService.fetchIntactObjects(first, max);
        return getPublicationSummaries(pubs);
    }

    private List<PublicationSummary> getPublicationSummaries(List<Publication> pubs) {
        List<PublicationSummary> summaryList = new ArrayList<PublicationSummary>(pubs.size());
        for (Publication pub : pubs){
            summaryList.add(createSummaryFrom((IntactPublication)pub));
        }
        return summaryList;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public long countAll(String countQuery, Map<String, Object> parameters) {
        return this.publicationService.countAll(countQuery, parameters);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<PublicationSummary> iterateAll(String countQuery, String query, Map<String, Object> parameters) {
        return new IntactQueryResultIterator<PublicationSummary>(this, countQuery, query, parameters);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<PublicationSummary> fetchIntactObjects(String query, Map<String, Object> parameters, int first, int max) {
        return getPublicationSummaries(publicationService.fetchIntactObjects(query, parameters, first, max));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<PublicationSummary> fetchIntactObjects(String query, Map<String, Object> parameters) {
        return getPublicationSummaries(publicationService.fetchIntactObjects(query, parameters));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<PublicationSummary> iterateAll(boolean loadLazyCollections) {
        return new IntactQueryResultIterator<PublicationSummary>(this, loadLazyCollections);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<PublicationSummary> fetchIntactObjects(int first, int max, boolean loadLazyCollections) {
        return getPublicationSummaries(publicationService.fetchIntactObjects(first, max, loadLazyCollections));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<PublicationSummary> iterateAll(String countQuery, String query, Map<String, Object> parameters, boolean loadLazyCollections) {
        return new IntactQueryResultIterator<PublicationSummary>(this, countQuery, query, parameters, loadLazyCollections);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<PublicationSummary> fetchIntactObjects(String query, Map<String, Object> parameters, int first, int max, boolean loadLazyCollections) {
        return getPublicationSummaries(publicationService.fetchIntactObjects(query, parameters, first, max, loadLazyCollections));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<PublicationSummary> fetchIntactObjects(String query, Map<String, Object> parameters, boolean loadLazyCollections) {
        return getPublicationSummaries(publicationService.fetchIntactObjects(query, parameters, loadLazyCollections));
    }

    @Override
    public void saveOrUpdate(PublicationSummary object) throws PersisterException, FinderException, SynchronizerException {
        throw new UnsupportedOperationException("The publication summary service is read only");
    }

    @Override
    public void saveOrUpdate(Collection<? extends PublicationSummary> objects) throws SynchronizerException, PersisterException, FinderException {
        throw new UnsupportedOperationException("The publication summary service is read only");
    }

    @Override
    public void delete(PublicationSummary object) throws PersisterException, FinderException, SynchronizerException {
        throw new UnsupportedOperationException("The publication summary service is read only");
    }

    @Override
    public void delete(Collection<? extends PublicationSummary> objects) throws SynchronizerException, PersisterException, FinderException {
        throw new UnsupportedOperationException("The publication summary service is read only");
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countInteractionsForPublication( IntactPublication publication ) {
        return getIntactDao().getPublicationDao().countInteractionsForPublication(publication.getAc());
    }

    private PublicationSummary createSummaryFrom(IntactPublication pub){
        PublicationSummary summary = new PublicationSummary();
        summary.setAc(pub.getAc());
        summary.setPubmedId(pub.getPubmedId());
        summary.setTitle(pub.getTitle());
        summary.setStatus(pub.getStatus().toString());
        summary.setOwner(pub.getCurrentOwner() != null ? pub.getCurrentOwner().getLogin() : "");
        summary.setReviewer(pub.getCurrentReviewer() != null ? pub.getCurrentReviewer().getLogin() : "");
        summary.setRowStyle(calculateStatusStyle(pub));
        summary.setExperimentCount(pub.getExperiments().size());
        summary.setInteractionCount(countInteractionsForPublication(pub));
        Annotation caution = AnnotationUtils.collectFirstAnnotationWithTopic(pub.getAnnotations(), Annotation.CAUTION_MI, Annotation.CAUTION);
        summary.setCaution(caution != null ? caution.getValue() : null);
        Annotation internal = AnnotationUtils.collectFirstAnnotationWithTopic(pub.getAnnotations(), null, "remark-internal");
        summary.setInternalRemark(internal != null ? internal.getValue() : null);
        if (!pub.getAuthors().isEmpty()){
            summary.setFirstAuthor(pub.getAuthors().get(0)+" et al.");
        }
        if (pub.getPublicationDate() != null){
            summary.setYear(IntactUtils.YEAR_FORMAT.format(pub.getPublicationDate()));
        }
        return summary;
    }

    private String calculateStatusStyle(IntactPublication publication) {
        if (isAccepted(publication)) {
            return "ia-accepted";
        }

        int timesRejected = 0;
        int timesReadyForChecking = 0;

        for (LifeCycleEvent evt : publication.getLifecycleEvents()) {
            if (LifeCycleEventType.REJECTED == evt.getEvent()) {
                timesRejected++;
            } else if (LifeCycleEventType.READY_FOR_CHECKING == evt.getEvent()) {
                timesReadyForChecking++;
            }
        }

        if (publication.getStatus() == LifeCycleStatus.CURATION_IN_PROGRESS && timesRejected > 0) {
            return "ia-rejected";
        } else if (publication.getStatus() == LifeCycleStatus.READY_FOR_CHECKING && timesReadyForChecking > 1) {
            return "ia-corrected";
        }

        return "";
    }

    private boolean isAccepted(IntactPublication pub) {
        if (isAcceptedOrBeyond(pub)) return true;

        return isAllExperimentsAccepted(pub.getExperiments());
    }

    private boolean isAcceptedOrBeyond(IntactPublication pub) {
        if (pub == null || pub.getStatus() == null) {
            return false;
        }

        return pub.getStatus() == LifeCycleStatus.ACCEPTED ||
                pub.getStatus() == LifeCycleStatus.ACCEPTED_ON_HOLD ||
                pub.getStatus() == LifeCycleStatus.READY_FOR_RELEASE ||
                pub.getStatus() == LifeCycleStatus.RELEASED;
    }

    private boolean isAllExperimentsAccepted(Collection<Experiment> experiments) {
        if (experiments.isEmpty()){
            return false;
        }
        for (Experiment exp : experiments){
            if (AnnotationUtils.collectFirstAnnotationWithTopic(exp.getAnnotations(), null, Releasable.ACCEPTED) == null){
                return false;
            }
        }
        return true;
    }
}
