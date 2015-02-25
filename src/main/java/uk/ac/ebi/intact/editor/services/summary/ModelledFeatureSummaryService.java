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
import psidev.psi.mi.jami.model.ModelledFeature;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.jami.model.extension.IntactInteractor;
import uk.ac.ebi.intact.jami.model.extension.IntactModelledFeature;
import uk.ac.ebi.intact.jami.service.IntactQueryResultIterator;
import uk.ac.ebi.intact.jami.service.IntactService;
import uk.ac.ebi.intact.jami.service.ModelledFeatureService;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import javax.annotation.Resource;
import java.util.*;

/**
 */
@Service
public class ModelledFeatureSummaryService extends AbstractEditorService implements IntactService<FeatureSummary> {

    @Resource(name = "modelledFeatureService")
    private ModelledFeatureService modelledFeatureService;

    public ModelledFeatureSummaryService() {
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public long countAll() {
        return this.modelledFeatureService.countAll();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<FeatureSummary> iterateAll() {
        return new IntactQueryResultIterator<FeatureSummary>(this);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<FeatureSummary> fetchIntactObjects(int first, int max) {
        List<ModelledFeature> pubs = this.modelledFeatureService.fetchIntactObjects(first, max);
        return getFeatureSummaries(pubs);
    }

    private List<FeatureSummary> getFeatureSummaries(List<ModelledFeature> pubs) {
        List<FeatureSummary> summaryList = new ArrayList<FeatureSummary>(pubs.size());
        for (ModelledFeature pub : pubs){
            summaryList.add(createSummaryFrom((IntactModelledFeature)pub));
        }
        return summaryList;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public long countAll(String countQuery, Map<String, Object> parameters) {
        return this.modelledFeatureService.countAll(countQuery, parameters);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<FeatureSummary> iterateAll(String countQuery, String query, Map<String, Object> parameters) {
        return new IntactQueryResultIterator<FeatureSummary>(this, countQuery, query, parameters);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<FeatureSummary> fetchIntactObjects(String query, Map<String, Object> parameters, int first, int max) {
        return getFeatureSummaries(modelledFeatureService.fetchIntactObjects(query, parameters, first, max));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<FeatureSummary> fetchIntactObjects(String query, Map<String, Object> parameters) {
        return getFeatureSummaries(modelledFeatureService.fetchIntactObjects(query, parameters));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<FeatureSummary> iterateAll(boolean loadLazyCollections) {
        return new IntactQueryResultIterator<FeatureSummary>(this, loadLazyCollections);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<FeatureSummary> fetchIntactObjects(int first, int max, boolean loadLazyCollections) {
        return getFeatureSummaries(modelledFeatureService.fetchIntactObjects(first, max, loadLazyCollections));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<FeatureSummary> iterateAll(String countQuery, String query, Map<String, Object> parameters, boolean loadLazyCollections) {
        return new IntactQueryResultIterator<FeatureSummary>(this, countQuery, query, parameters, loadLazyCollections);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<FeatureSummary> fetchIntactObjects(String query, Map<String, Object> parameters, int first, int max, boolean loadLazyCollections) {
        return getFeatureSummaries(modelledFeatureService.fetchIntactObjects(query, parameters, first, max, loadLazyCollections));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<FeatureSummary> fetchIntactObjects(String query, Map<String, Object> parameters, boolean loadLazyCollections) {
        return getFeatureSummaries(modelledFeatureService.fetchIntactObjects(query, parameters, loadLazyCollections));
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countInteractionsByMoleculeAc( IntactInteractor molecule ) {
        return getIntactDao().getInteractionDao().countInteractionsInvolvingInteractor(molecule.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countComplexesByMoleculeAc( IntactInteractor molecule ) {
        return getIntactDao().getComplexDao().countComplexesInvolvingInteractor(molecule.getAc());
    }

    @Override
    public void saveOrUpdate(FeatureSummary object) throws PersisterException, FinderException, SynchronizerException {
        throw new UnsupportedOperationException("The modelled feature summary service is read only");
    }

    @Override
    public void saveOrUpdate(Collection<? extends FeatureSummary> objects) throws SynchronizerException, PersisterException, FinderException {
        throw new UnsupportedOperationException("The modelled feature summary service is read only");
    }

    @Override
    public void delete(FeatureSummary object) throws PersisterException, FinderException, SynchronizerException {
        throw new UnsupportedOperationException("The modelled feature summary service is read only");
    }

    @Override
    public void delete(Collection<? extends FeatureSummary> objects) throws SynchronizerException, PersisterException, FinderException {
        throw new UnsupportedOperationException("The modelled feature summary service is read only");
    }

    public FeatureSummary createSummaryFrom(IntactModelledFeature pub){
        FeatureSummary summary = new FeatureSummary();
        summary.setAc(pub.getAc());
        summary.setShortName(pub.getShortName());

        Annotation caution = AnnotationUtils.collectFirstAnnotationWithTopic(pub.getAnnotations(), Annotation.CAUTION_MI, Annotation.CAUTION);
        summary.setCaution(caution != null ? caution.getValue() : null);
        Annotation internal = AnnotationUtils.collectFirstAnnotationWithTopic(pub.getAnnotations(), null, "remark-internal");
        summary.setInternalRemark(internal != null ? internal.getValue() : null);
        return summary;
    }
}
