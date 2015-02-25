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

import org.primefaces.model.LazyDataModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.Annotation;
import psidev.psi.mi.jami.model.InteractionEvidence;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.editor.util.LazyDataModelFactory;
import uk.ac.ebi.intact.jami.model.extension.IntactExperiment;
import uk.ac.ebi.intact.jami.model.extension.IntactInteractionEvidence;
import uk.ac.ebi.intact.jami.model.extension.IntactPublication;
import uk.ac.ebi.intact.jami.service.IntactQueryResultIterator;
import uk.ac.ebi.intact.jami.service.IntactService;
import uk.ac.ebi.intact.jami.service.InteractionEvidenceService;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import javax.annotation.Resource;
import java.util.*;

/**
 */
@Service
public class InteractionSummaryService extends AbstractEditorService implements IntactService<InteractionSummary> {

    @Resource(name = "interactionEvidenceService")
    private InteractionEvidenceService interactionEvidenceService;

    public InteractionSummaryService() {
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public long countAll() {
        return this.interactionEvidenceService.countAll();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<InteractionSummary> iterateAll() {
        return new IntactQueryResultIterator<InteractionSummary>(this);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<InteractionSummary> fetchIntactObjects(int first, int max) {
        List<InteractionEvidence> pubs = this.interactionEvidenceService.fetchIntactObjects(first, max);
        return getInteractionSummaries(pubs);
    }

    private List<InteractionSummary> getInteractionSummaries(List<InteractionEvidence> pubs) {
        List<InteractionSummary> summaryList = new ArrayList<InteractionSummary>(pubs.size());
        for (InteractionEvidence pub : pubs){
            summaryList.add(createSummaryFrom((IntactInteractionEvidence)pub));
        }
        return summaryList;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public long countAll(String countQuery, Map<String, Object> parameters) {
        return this.interactionEvidenceService.countAll(countQuery, parameters);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<InteractionSummary> iterateAll(String countQuery, String query, Map<String, Object> parameters) {
        return new IntactQueryResultIterator<InteractionSummary>(this, countQuery, query, parameters);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<InteractionSummary> fetchIntactObjects(String query, Map<String, Object> parameters, int first, int max) {
        return getInteractionSummaries(interactionEvidenceService.fetchIntactObjects(query, parameters, first, max));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<InteractionSummary> fetchIntactObjects(String query, Map<String, Object> parameters) {
        return getInteractionSummaries(interactionEvidenceService.fetchIntactObjects(query, parameters));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<InteractionSummary> iterateAll(boolean loadLazyCollections) {
        return new IntactQueryResultIterator<InteractionSummary>(this, loadLazyCollections);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<InteractionSummary> fetchIntactObjects(int first, int max, boolean loadLazyCollections) {
        return getInteractionSummaries(interactionEvidenceService.fetchIntactObjects(first, max, loadLazyCollections));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<InteractionSummary> iterateAll(String countQuery, String query, Map<String, Object> parameters, boolean loadLazyCollections) {
        return new IntactQueryResultIterator<InteractionSummary>(this, countQuery, query, parameters, loadLazyCollections);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<InteractionSummary> fetchIntactObjects(String query, Map<String, Object> parameters, int first, int max, boolean loadLazyCollections) {
        return getInteractionSummaries(interactionEvidenceService.fetchIntactObjects(query, parameters, first, max, loadLazyCollections));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<InteractionSummary> fetchIntactObjects(String query, Map<String, Object> parameters, boolean loadLazyCollections) {
        return getInteractionSummaries(interactionEvidenceService.fetchIntactObjects(query, parameters, loadLazyCollections));
    }

    @Override
    public void saveOrUpdate(InteractionSummary object) throws PersisterException, FinderException, SynchronizerException {
        throw new UnsupportedOperationException("The interaction summary service is read only");
    }

    @Override
    public void saveOrUpdate(Collection<? extends InteractionSummary> objects) throws SynchronizerException, PersisterException, FinderException {
        throw new UnsupportedOperationException("The interaction summary service is read only");
    }

    @Override
    public void delete(InteractionSummary object) throws PersisterException, FinderException, SynchronizerException {
        throw new UnsupportedOperationException("The interaction summary service is read only");
    }

    @Override
    public void delete(Collection<? extends InteractionSummary> objects) throws SynchronizerException, PersisterException, FinderException {
        throw new UnsupportedOperationException("The interaction summary service is read only");
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countParticipants(IntactInteractionEvidence interaction) {
        if (interaction.areParticipantsInitialized()){
            return interaction.getParticipants().size();
        }
        else{
            return getIntactDao().getInteractionDao().countParticipantsForInteraction(interaction.getAc());
        }
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    /**
     * WARNING: Needs to give service proxy (not this) so transactional is fired when called from LazyDataModel
     */
    public LazyDataModel<InteractionSummary> refreshDataModels(IntactPublication publication, InteractionSummaryService serviceProxy) {
        return LazyDataModelFactory.createLazyDataModel(serviceProxy, "interactionSummaryService",
                "select distinct i from IntactInteractionEvidence i join fetch i.dbExperiments as exp " +
                        "where exp.publication.ac = '" + publication.getAc() + "' order by exp.shortLabel asc",
                "select count(i) from IntactInteractionEvidence i join i.dbExperiments as exp " +
                        "where exp.publication.ac = '" + publication.getAc() + "'"
        );
    }

    public InteractionSummary createSummaryFrom(IntactInteractionEvidence pub){
        InteractionSummary summary = new InteractionSummary();
        summary.setAc(pub.getAc());
        summary.setInteractionType(pub.getInteractionType() != null ? pub.getInteractionType().getShortName() : "");
        summary.setNumberParticipants(countParticipants(pub));
        if (pub.getExperiment() instanceof IntactExperiment){
           IntactExperiment exp = (IntactExperiment)pub.getExperiment();
            summary.setExperimentLabel(exp.getShortLabel() != null ? exp.getShortLabel() : "");
        }
        else{
            summary.setExperimentLabel("");
        }
        summary.setInteraction(pub);
        summary.setShortName(pub.getShortName());
        Annotation caution = AnnotationUtils.collectFirstAnnotationWithTopic(pub.getAnnotations(), Annotation.CAUTION_MI, Annotation.CAUTION);
        summary.setCaution(caution != null ? caution.getValue() : null);
        Annotation internal = AnnotationUtils.collectFirstAnnotationWithTopic(pub.getAnnotations(), null, "remark-internal");
        summary.setInternalRemark(internal != null ? internal.getValue() : null);
        return summary;
    }
}
