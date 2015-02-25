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
import psidev.psi.mi.jami.utils.AnnotationUtils;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.jami.model.extension.IntactExperiment;
import uk.ac.ebi.intact.jami.model.extension.IntactPublication;
import uk.ac.ebi.intact.jami.model.lifecycle.Releasable;
import uk.ac.ebi.intact.jami.service.ExperimentService;
import uk.ac.ebi.intact.jami.service.IntactQueryResultIterator;
import uk.ac.ebi.intact.jami.service.IntactService;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import javax.annotation.Resource;
import java.util.*;

/**
 */
@Service
public class ExperimentSummaryService extends AbstractEditorService implements IntactService<ExperimentSummary> {

    @Resource(name = "experimentService")
    private ExperimentService experimentService;

    public ExperimentSummaryService() {
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public long countAll() {
        return this.experimentService.countAll();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<ExperimentSummary> iterateAll() {
        return new IntactQueryResultIterator<ExperimentSummary>(this);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<ExperimentSummary> fetchIntactObjects(int first, int max) {
        List<Experiment> pubs = this.experimentService.fetchIntactObjects(first, max);
        return getExperimentSummaries(pubs);
    }

    private List<ExperimentSummary> getExperimentSummaries(List<Experiment> pubs) {
        List<ExperimentSummary> summaryList = new ArrayList<ExperimentSummary>(pubs.size());
        for (Experiment pub : pubs){
            summaryList.add(createSummaryFrom((IntactExperiment)pub));
        }
        return summaryList;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public long countAll(String countQuery, Map<String, Object> parameters) {
        return this.experimentService.countAll(countQuery, parameters);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<ExperimentSummary> iterateAll(String countQuery, String query, Map<String, Object> parameters) {
        return new IntactQueryResultIterator<ExperimentSummary>(this, countQuery, query, parameters);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<ExperimentSummary> fetchIntactObjects(String query, Map<String, Object> parameters, int first, int max) {
        return getExperimentSummaries(experimentService.fetchIntactObjects(query, parameters, first, max));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<ExperimentSummary> fetchIntactObjects(String query, Map<String, Object> parameters) {
        return getExperimentSummaries(experimentService.fetchIntactObjects(query, parameters));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<ExperimentSummary> iterateAll(boolean loadLazyCollections) {
        return new IntactQueryResultIterator<ExperimentSummary>(this, loadLazyCollections);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<ExperimentSummary> fetchIntactObjects(int first, int max, boolean loadLazyCollections) {
        return getExperimentSummaries(experimentService.fetchIntactObjects(first, max, loadLazyCollections));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<ExperimentSummary> iterateAll(String countQuery, String query, Map<String, Object> parameters, boolean loadLazyCollections) {
        return new IntactQueryResultIterator<ExperimentSummary>(this, countQuery, query, parameters, loadLazyCollections);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<ExperimentSummary> fetchIntactObjects(String query, Map<String, Object> parameters, int first, int max, boolean loadLazyCollections) {
        return getExperimentSummaries(experimentService.fetchIntactObjects(query, parameters, first, max, loadLazyCollections));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<ExperimentSummary> fetchIntactObjects(String query, Map<String, Object> parameters, boolean loadLazyCollections) {
        return getExperimentSummaries(experimentService.fetchIntactObjects(query, parameters, loadLazyCollections));
    }

    @Override
    public void saveOrUpdate(ExperimentSummary object) throws PersisterException, FinderException, SynchronizerException {
        throw new UnsupportedOperationException("The experiment summary service is read only");
    }

    @Override
    public void saveOrUpdate(Collection<? extends ExperimentSummary> objects) throws SynchronizerException, PersisterException, FinderException {
        throw new UnsupportedOperationException("The experiment summary service is read only");
    }

    @Override
    public void delete(ExperimentSummary object) throws PersisterException, FinderException, SynchronizerException {
        throw new UnsupportedOperationException("The experiment summary service is read only");
    }

    @Override
    public void delete(Collection<? extends ExperimentSummary> objects) throws SynchronizerException, PersisterException, FinderException {
        throw new UnsupportedOperationException("The experiment summary service is read only");
    }

    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public int countInteractionsByExperiment( IntactExperiment experiment ) {

        if (experiment.areInteractionEvidencesInitialized()){
            return experiment.getInteractionEvidences().size();
        }
        else{
            return getIntactDao().getExperimentDao().countInteractionsForExperiment(experiment.getAc());
        }
    }

    public ExperimentSummary createSummaryFrom(IntactExperiment pub){
        ExperimentSummary summary = new ExperimentSummary();
        summary.setAc(pub.getAc());
        summary.setRowStyleClass(isAccepted(pub) ? "ia-accepted" : isToBeReviewed(pub) ? "ia-to-be-reviewed" : "ia-not-accepted");
        summary.setShortLabel(pub.getShortLabel());
        if (pub.getPublication() != null){
            summary.setPublicationAc(((IntactPublication)pub.getPublication()).getAc());
            summary.setPubmedId(pub.getPublication().getPubmedId());
        }
        summary.setInteractionDetectionMethod(pub.getInteractionDetectionMethod().getShortName());
        summary.setParticipantIdentificationMethod(pub.getParticipantIdentificationMethod() != null ? pub.getParticipantIdentificationMethod().getShortName() : "");
        summary.setHostOrganism(pub.getHostOrganism() != null ? pub.getHostOrganism().getCommonName() : "");
        summary.setInteractionNumber(countInteractionsByExperiment(pub));
        Annotation caution = AnnotationUtils.collectFirstAnnotationWithTopic(pub.getAnnotations(), Annotation.CAUTION_MI, Annotation.CAUTION);
        summary.setCaution(caution != null ? caution.getValue() : null);
        Annotation internal = AnnotationUtils.collectFirstAnnotationWithTopic(pub.getAnnotations(), null, "remark-internal");
        summary.setInternalRemark(internal != null ? internal.getValue() : null);
        summary.setExperiment(pub);
        return summary;
    }

    public boolean isAccepted(IntactExperiment exp) {
        if (exp.areAnnotationsInitialized()){
            return AnnotationUtils.collectFirstAnnotationWithTopic(exp.getAnnotations(), null, Releasable.ACCEPTED) != null;
        }
        else{
            return AnnotationUtils.collectAllAnnotationsHavingTopic(exp.getAnnotations(), null, Releasable.ACCEPTED)!=null;
        }
    }

    public boolean isToBeReviewed(IntactExperiment exp) {
        if (exp.areAnnotationsInitialized()){
            return AnnotationUtils.collectFirstAnnotationWithTopic(exp.getAnnotations(), null, Releasable.TO_BE_REVIEWED) != null;
        }
        else{
            return AnnotationUtils.collectAllAnnotationsHavingTopic(exp.getAnnotations(), null, Releasable.TO_BE_REVIEWED)!=null;
        }
    }
}
