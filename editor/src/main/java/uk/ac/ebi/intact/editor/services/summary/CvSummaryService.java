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
import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.jami.model.extension.IntactCvTerm;
import uk.ac.ebi.intact.jami.service.CvTermService;
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
public class CvSummaryService extends AbstractEditorService implements IntactService<CvSummary> {

    @Resource(name = "cvTermService")
    private CvTermService cvTermService;

    public CvSummaryService() {
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public long countAll() {
        return this.cvTermService.countAll();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<CvSummary> iterateAll() {
        return new IntactQueryResultIterator<CvSummary>(this);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<CvSummary> fetchIntactObjects(int first, int max) {
        List<CvTerm> pubs = this.cvTermService.fetchIntactObjects(first, max);
        return getCvSummaries(pubs);
    }

    private List<CvSummary> getCvSummaries(List<CvTerm> pubs) {
        List<CvSummary> summaryList = new ArrayList<CvSummary>(pubs.size());
        for (CvTerm pub : pubs){
            summaryList.add(createSummaryFrom((IntactCvTerm)pub));
        }
        return summaryList;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public long countAll(String countQuery, Map<String, Object> parameters) {
        return this.cvTermService.countAll(countQuery, parameters);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<CvSummary> iterateAll(String countQuery, String query, Map<String, Object> parameters) {
        return new IntactQueryResultIterator<CvSummary>(this, countQuery, query, parameters);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<CvSummary> fetchIntactObjects(String query, Map<String, Object> parameters, int first, int max) {
        return getCvSummaries(cvTermService.fetchIntactObjects(query, parameters, first, max));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<CvSummary> fetchIntactObjects(String query, Map<String, Object> parameters) {
        return getCvSummaries(cvTermService.fetchIntactObjects(query, parameters));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<CvSummary> iterateAll(boolean loadLazyCollections) {
        return new IntactQueryResultIterator<CvSummary>(this, loadLazyCollections);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<CvSummary> fetchIntactObjects(int first, int max, boolean loadLazyCollections) {
        return getCvSummaries(cvTermService.fetchIntactObjects(first, max, loadLazyCollections));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<CvSummary> iterateAll(String countQuery, String query, Map<String, Object> parameters, boolean loadLazyCollections) {
        return new IntactQueryResultIterator<CvSummary>(this, countQuery, query, parameters, loadLazyCollections);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<CvSummary> fetchIntactObjects(String query, Map<String, Object> parameters, int first, int max, boolean loadLazyCollections) {
        return getCvSummaries(cvTermService.fetchIntactObjects(query, parameters, first, max, loadLazyCollections));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<CvSummary> fetchIntactObjects(String query, Map<String, Object> parameters, boolean loadLazyCollections) {
        return getCvSummaries(cvTermService.fetchIntactObjects(query, parameters, loadLazyCollections));
    }

    @Override
    public void saveOrUpdate(CvSummary object) throws PersisterException, FinderException, SynchronizerException {
        throw new UnsupportedOperationException("The cv summary service is read only");
    }

    @Override
    public void saveOrUpdate(Collection<? extends CvSummary> objects) throws SynchronizerException, PersisterException, FinderException {
        throw new UnsupportedOperationException("The cv summary service is read only");
    }

    @Override
    public void delete(CvSummary object) throws PersisterException, FinderException, SynchronizerException {
        throw new UnsupportedOperationException("The cv summary service is read only");
    }

    @Override
    public void delete(Collection<? extends CvSummary> objects) throws SynchronizerException, PersisterException, FinderException {
        throw new UnsupportedOperationException("The cv summary service is read only");
    }

    private String getIdentifier( IntactCvTerm cv ) {
        String id = "-";
        if ( cv.getMIIdentifier() != null ) {
            id= cv.getMIIdentifier();
        }
        else if ( cv.getMODIdentifier() != null ) {
            id= cv.getMODIdentifier();
        }
        else if ( !cv.getIdentifiers().isEmpty() ) {
            id= cv.getIdentifiers().iterator().next().getId();
        }
        return id;
    }

    private String getType( IntactCvTerm cv ) {
        if (cv.getObjClass() == null){
            return "-";
        }
        int index = cv.getObjClass().lastIndexOf(".");
        if (index >= 0 && index < cv.getObjClass().length()){
            return cv.getObjClass().substring(index+1);
        }
        else{
            return "-";
        }
    }

    private CvSummary createSummaryFrom(IntactCvTerm pub){
        CvSummary summary = new CvSummary();
        summary.setAc(pub.getAc());
        summary.setShortName(pub.getShortName());
        summary.setType(getType(pub));
        summary.setIdentifier(getIdentifier(pub));

        Annotation caution = AnnotationUtils.collectFirstAnnotationWithTopic(pub.getAnnotations(), Annotation.CAUTION_MI, Annotation.CAUTION);
        summary.setCaution(caution != null ? caution.getValue() : null);
        Annotation internal = AnnotationUtils.collectFirstAnnotationWithTopic(pub.getAnnotations(), null, "remark-internal");
        summary.setInternalRemark(internal != null ? internal.getValue() : null);
        return summary;
    }
}
