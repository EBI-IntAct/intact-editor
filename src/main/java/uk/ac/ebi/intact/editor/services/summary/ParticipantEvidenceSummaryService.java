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
import psidev.psi.mi.jami.model.Interactor;
import psidev.psi.mi.jami.model.ParticipantEvidence;
import psidev.psi.mi.jami.model.Xref;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import uk.ac.ebi.intact.editor.controller.curate.AnnotatedObjectController;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.jami.model.extension.IntactParticipantEvidence;
import uk.ac.ebi.intact.jami.service.IntactQueryResultIterator;
import uk.ac.ebi.intact.jami.service.IntactService;
import uk.ac.ebi.intact.jami.service.ParticipantEvidenceService;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import javax.annotation.Resource;
import java.util.*;

/**
 */
@Service
public class ParticipantEvidenceSummaryService extends AbstractEditorService implements IntactService<ParticipantSummary> {

    @Resource(name = "participantEvidenceService")
    private ParticipantEvidenceService participantEvidenceService;

    public ParticipantEvidenceSummaryService() {
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public long countAll() {
        return this.participantEvidenceService.countAll();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<ParticipantSummary> iterateAll() {
        return new IntactQueryResultIterator<ParticipantSummary>(this);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<ParticipantSummary> fetchIntactObjects(int first, int max) {
        List<ParticipantEvidence> pubs = this.participantEvidenceService.fetchIntactObjects(first, max);
        return getParticipantSummaries(pubs);
    }

    private List<ParticipantSummary> getParticipantSummaries(List<ParticipantEvidence> pubs) {
        List<ParticipantSummary> summaryList = new ArrayList<ParticipantSummary>(pubs.size());
        for (ParticipantEvidence pub : pubs){
            summaryList.add(createSummaryFrom((IntactParticipantEvidence)pub));
        }
        return summaryList;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public long countAll(String countQuery, Map<String, Object> parameters) {
        return this.participantEvidenceService.countAll(countQuery, parameters);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<ParticipantSummary> iterateAll(String countQuery, String query, Map<String, Object> parameters) {
        return new IntactQueryResultIterator<ParticipantSummary>(this, countQuery, query, parameters);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<ParticipantSummary> fetchIntactObjects(String query, Map<String, Object> parameters, int first, int max) {
        return getParticipantSummaries(participantEvidenceService.fetchIntactObjects(query, parameters, first, max));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<ParticipantSummary> fetchIntactObjects(String query, Map<String, Object> parameters) {
        return getParticipantSummaries(participantEvidenceService.fetchIntactObjects(query, parameters));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<ParticipantSummary> iterateAll(boolean loadLazyCollections) {
        return new IntactQueryResultIterator<ParticipantSummary>(this, loadLazyCollections);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<ParticipantSummary> fetchIntactObjects(int first, int max, boolean loadLazyCollections) {
        return getParticipantSummaries(participantEvidenceService.fetchIntactObjects(first, max, loadLazyCollections));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<ParticipantSummary> iterateAll(String countQuery, String query, Map<String, Object> parameters, boolean loadLazyCollections) {
        return new IntactQueryResultIterator<ParticipantSummary>(this, countQuery, query, parameters, loadLazyCollections);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<ParticipantSummary> fetchIntactObjects(String query, Map<String, Object> parameters, int first, int max, boolean loadLazyCollections) {
        return getParticipantSummaries(participantEvidenceService.fetchIntactObjects(query, parameters, first, max, loadLazyCollections));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<ParticipantSummary> fetchIntactObjects(String query, Map<String, Object> parameters, boolean loadLazyCollections) {
        return getParticipantSummaries(participantEvidenceService.fetchIntactObjects(query, parameters, loadLazyCollections));
    }

    @Override
    public void saveOrUpdate(ParticipantSummary object) throws PersisterException, FinderException, SynchronizerException {
        throw new UnsupportedOperationException("The participant evidence summary service is read only");
    }

    @Override
    public void saveOrUpdate(Collection<? extends ParticipantSummary> objects) throws SynchronizerException, PersisterException, FinderException {
        throw new UnsupportedOperationException("The participant evidence summary service is read only");
    }

    @Override
    public void delete(ParticipantSummary object) throws PersisterException, FinderException, SynchronizerException {
        throw new UnsupportedOperationException("The participant evidence summary service is read only");
    }

    @Override
    public void delete(Collection<? extends ParticipantSummary> objects) throws SynchronizerException, PersisterException, FinderException {
        throw new UnsupportedOperationException("The participant evidence summary service is read only");
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countFeaturesByParticipantAc( IntactParticipantEvidence comp ) {
        return getIntactDao().getParticipantEvidenceDao().countFeaturesForParticipant(comp.getAc());
    }

    private boolean isNoUniprotUpdate(Interactor interactor) {
        Collection<Annotation> annots = Collections.EMPTY_LIST;
        if (interactor == null) return false;
        else{
            annots = interactor.getAnnotations();
        }

        if (annots.isEmpty()){
            return false;
        }
        Annotation caution = AnnotationUtils.collectFirstAnnotationWithTopic(annots, null, AnnotatedObjectController.NON_UNIPROT);
        return caution != null ? true : false;
    }

    private String getIdentityXref( Interactor molecule ) {
        // TODO handle multiple identities (return xref and iterate to display them all)
        Xref xrefs = molecule.getPreferredIdentifier();


        if ( xrefs == null ) {
            return "-";
        }

        return xrefs.getId();
    }

    public ParticipantSummary createSummaryFrom(IntactParticipantEvidence pub){
        ParticipantSummary summary = new ParticipantSummary();
        summary.setAc(pub.getAc());
        summary.setInteractorShortName(pub.getInteractor().getShortName());
        summary.setNoUniprotUpdate(isNoUniprotUpdate(pub.getInteractor()));
        summary.setIdentityXref(getIdentityXref(pub.getInteractor()));
        summary.setBiologicalRole(pub.getBiologicalRole().getShortName());
        summary.setExperimentalRole(pub.getExperimentalRole().getShortName());
        summary.setExpressedInOrganism(pub.getExpressedInOrganism() != null ? pub.getExpressedInOrganism().getCommonName() : "");
        if (pub.getStoichiometry() != null){
            summary.setMinStoichiometry(pub.getStoichiometry().getMinValue());
            summary.setMaxStoichiometry(pub.getStoichiometry().getMaxValue());
        }
        else{
            summary.setMaxStoichiometry(0);
            summary.setMinStoichiometry(0);
        }
        summary.setFeaturesNumber(countFeaturesByParticipantAc(pub));

        Annotation caution = AnnotationUtils.collectFirstAnnotationWithTopic(pub.getAnnotations(), Annotation.CAUTION_MI, Annotation.CAUTION);
        summary.setCaution(caution != null ? caution.getValue() : null);
        Annotation internal = AnnotationUtils.collectFirstAnnotationWithTopic(pub.getAnnotations(), null, "remark-internal");
        summary.setInternalRemark(internal != null ? internal.getValue() : null);
        return summary;
    }
}
