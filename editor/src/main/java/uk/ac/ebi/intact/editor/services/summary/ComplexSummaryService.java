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
import psidev.psi.mi.jami.model.Alias;
import psidev.psi.mi.jami.model.Annotation;
import psidev.psi.mi.jami.model.Complex;
import psidev.psi.mi.jami.utils.AliasUtils;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.jami.model.extension.IntactComplex;
import uk.ac.ebi.intact.jami.model.lifecycle.LifeCycleEvent;
import uk.ac.ebi.intact.jami.model.lifecycle.LifeCycleEventType;
import uk.ac.ebi.intact.jami.model.lifecycle.LifeCycleStatus;
import uk.ac.ebi.intact.jami.service.ComplexService;
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
public class ComplexSummaryService extends AbstractEditorService implements IntactService<ComplexSummary> {

    @Resource(name = "complexService")
    private ComplexService complexService;

    public ComplexSummaryService() {
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public long countAll() {
        return this.complexService.countAll();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<ComplexSummary> iterateAll() {
        return new IntactQueryResultIterator<ComplexSummary>(this);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<ComplexSummary> fetchIntactObjects(int first, int max) {
        List<Complex> pubs = this.complexService.fetchIntactObjects(first, max);
        return getComplexSummaries(pubs);
    }

    private List<ComplexSummary> getComplexSummaries(List<Complex> pubs) {
        List<ComplexSummary> summaryList = new ArrayList<ComplexSummary>(pubs.size());
        for (Complex pub : pubs){
            summaryList.add(createSummaryFrom((IntactComplex)pub));
        }
        return summaryList;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public long countAll(String countQuery, Map<String, Object> parameters) {
        return this.complexService.countAll(countQuery, parameters);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<ComplexSummary> iterateAll(String countQuery, String query, Map<String, Object> parameters) {
        return new IntactQueryResultIterator<ComplexSummary>(this, countQuery, query, parameters);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<ComplexSummary> fetchIntactObjects(String query, Map<String, Object> parameters, int first, int max) {
        return getComplexSummaries(complexService.fetchIntactObjects(query, parameters, first, max));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<ComplexSummary> fetchIntactObjects(String query, Map<String, Object> parameters) {
        return getComplexSummaries(complexService.fetchIntactObjects(query, parameters));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<ComplexSummary> iterateAll(boolean loadLazyCollections) {
        return new IntactQueryResultIterator<ComplexSummary>(this, loadLazyCollections);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<ComplexSummary> fetchIntactObjects(int first, int max, boolean loadLazyCollections) {
        return getComplexSummaries(complexService.fetchIntactObjects(first, max, loadLazyCollections));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<ComplexSummary> iterateAll(String countQuery, String query, Map<String, Object> parameters, boolean loadLazyCollections) {
        return new IntactQueryResultIterator<ComplexSummary>(this, countQuery, query, parameters, loadLazyCollections);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<ComplexSummary> fetchIntactObjects(String query, Map<String, Object> parameters, int first, int max, boolean loadLazyCollections) {
        return getComplexSummaries(complexService.fetchIntactObjects(query, parameters, first, max, loadLazyCollections));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<ComplexSummary> fetchIntactObjects(String query, Map<String, Object> parameters, boolean loadLazyCollections) {
        return getComplexSummaries(complexService.fetchIntactObjects(query, parameters, loadLazyCollections));
    }

    @Override
    public void saveOrUpdate(ComplexSummary object) throws PersisterException, FinderException, SynchronizerException {
        throw new UnsupportedOperationException("The complex summary service is read only");
    }

    @Override
    public void saveOrUpdate(Collection<? extends ComplexSummary> objects) throws SynchronizerException, PersisterException, FinderException {
        throw new UnsupportedOperationException("The complex summary service is read only");
    }

    @Override
    public void delete(ComplexSummary object) throws PersisterException, FinderException, SynchronizerException {
        throw new UnsupportedOperationException("The complex summary service is read only");
    }

    @Override
    public void delete(Collection<? extends ComplexSummary> objects) throws SynchronizerException, PersisterException, FinderException {
        throw new UnsupportedOperationException("The complex summary service is read only");
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countParticipants(IntactComplex interaction) {
        return getIntactDao().getComplexDao().countParticipantsForComplex(interaction.getAc());
    }

    private ComplexSummary createSummaryFrom(IntactComplex pub){
        ComplexSummary summary = new ComplexSummary();
        summary.setAc(pub.getAc());
        summary.setName(extractName(pub));
        summary.setComplexType(pub.getInteractorType() != null ? pub.getInteractorType().getShortName() : "");
        summary.setInteractionType(pub.getInteractionType() != null ? pub.getInteractionType().getShortName() : "");
        summary.setOrganism(pub.getOrganism() != null ? pub.getOrganism().getCommonName() : "");
        summary.setStatus(pub.getStatus().toString());
        summary.setOwner(pub.getCurrentOwner() != null ? pub.getCurrentOwner().getLogin() : "");
        summary.setReviewer(pub.getCurrentReviewer() != null ? pub.getCurrentReviewer().getLogin() : "");
        summary.setNumberParticipants(countParticipants(pub));
        summary.setRowStyle(calculateStatusStyle(pub));
        Annotation caution = AnnotationUtils.collectFirstAnnotationWithTopic(pub.getAnnotations(), Annotation.CAUTION_MI, Annotation.CAUTION);
        summary.setCaution(caution != null ? caution.getValue() : null);
        Annotation internal = AnnotationUtils.collectFirstAnnotationWithTopic(pub.getAnnotations(), null, "remark-internal");
        summary.setInternalRemark(internal != null ? internal.getValue() : null);
        return summary;
    }

    private String extractName(IntactComplex complex){
        String name = complex.getShortName();
        Collection<Alias> aliases = complex.getAliases();
        Alias recName = AliasUtils.collectFirstAliasWithType(aliases, Alias.COMPLEX_RECOMMENDED_NAME_MI, Alias.COMPLEX_RECOMMENDED_NAME);
        if (recName != null){
            name = recName.getName();
        }
        else{
            recName = AliasUtils.collectFirstAliasWithType(aliases, Alias.COMPLEX_SYSTEMATIC_NAME_MI, Alias.COMPLEX_SYSTEMATIC_NAME);
            if (recName != null){
                name = recName.getName();
            }
            else if (!aliases.isEmpty()){
                name = aliases.iterator().next().getName();
            }
        }
        return name;
    }

    private String calculateStatusStyle(IntactComplex complex) {
        if (isAccepted(complex)) {
            return "ia-accepted";
        }

        int timesRejected = 0;
        int timesReadyForChecking = 0;

        for (LifeCycleEvent evt : complex.getLifecycleEvents()) {
            if (LifeCycleEventType.REJECTED == evt.getEvent()) {
                timesRejected++;
            } else if (LifeCycleEventType.READY_FOR_CHECKING == evt.getEvent()) {
                timesReadyForChecking++;
            }
        }

        if (complex.getStatus() == LifeCycleStatus.CURATION_IN_PROGRESS && timesRejected > 0) {
            return "ia-rejected";
        } else if (complex.getStatus() == LifeCycleStatus.READY_FOR_CHECKING && timesReadyForChecking > 1) {
            return "ia-corrected";
        }

        return "";
    }

    public boolean isAccepted(IntactComplex pub) {
        return pub.getStatus() == LifeCycleStatus.ACCEPTED ||
                pub.getStatus() == LifeCycleStatus.ACCEPTED_ON_HOLD ||
                pub.getStatus() == LifeCycleStatus.READY_FOR_RELEASE ||
                pub.getStatus() == LifeCycleStatus.RELEASED;
    }
}
