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
import psidev.psi.mi.jami.model.InteractorPool;
import psidev.psi.mi.jami.model.Xref;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import uk.ac.ebi.intact.editor.controller.curate.AnnotatedObjectController;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.jami.model.extension.IntactInteractor;
import uk.ac.ebi.intact.jami.service.IntactQueryResultIterator;
import uk.ac.ebi.intact.jami.service.IntactService;
import uk.ac.ebi.intact.jami.service.InteractorService;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import javax.annotation.Resource;
import java.util.*;

/**
 */
@Service
public class MoleculeSummaryService extends AbstractEditorService implements IntactService<MoleculeSummary> {

    @Resource(name = "interactorService")
    private InteractorService interactorService;

    public MoleculeSummaryService() {
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public long countAll() {
        return this.interactorService.countAll();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<MoleculeSummary> iterateAll() {
        return new IntactQueryResultIterator<MoleculeSummary>(this);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<MoleculeSummary> fetchIntactObjects(int first, int max) {
        List<Interactor> pubs = this.interactorService.fetchIntactObjects(first, max);
        return getMoleculeSummaries(pubs);
    }

    private List<MoleculeSummary> getMoleculeSummaries(List<Interactor> pubs) {
        List<MoleculeSummary> summaryList = new ArrayList<MoleculeSummary>(pubs.size());
        for (Interactor pub : pubs){
            summaryList.add(createSummaryFrom((IntactInteractor)pub, true));
        }
        return summaryList;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public long countAll(String countQuery, Map<String, Object> parameters) {
        return this.interactorService.countAll(countQuery, parameters);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<MoleculeSummary> iterateAll(String countQuery, String query, Map<String, Object> parameters) {
        return new IntactQueryResultIterator<MoleculeSummary>(this, countQuery, query, parameters);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<MoleculeSummary> fetchIntactObjects(String query, Map<String, Object> parameters, int first, int max) {
        return getMoleculeSummaries(interactorService.fetchIntactObjects(query, parameters, first, max));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<MoleculeSummary> fetchIntactObjects(String query, Map<String, Object> parameters) {
        return getMoleculeSummaries(interactorService.fetchIntactObjects(query, parameters));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<MoleculeSummary> iterateAll(boolean loadLazyCollections) {
        return new IntactQueryResultIterator<MoleculeSummary>(this, loadLazyCollections);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<MoleculeSummary> fetchIntactObjects(int first, int max, boolean loadLazyCollections) {
        return getMoleculeSummaries(interactorService.fetchIntactObjects(first, max, loadLazyCollections));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<MoleculeSummary> iterateAll(String countQuery, String query, Map<String, Object> parameters, boolean loadLazyCollections) {
        return new IntactQueryResultIterator<MoleculeSummary>(this, countQuery, query, parameters, loadLazyCollections);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<MoleculeSummary> fetchIntactObjects(String query, Map<String, Object> parameters, int first, int max, boolean loadLazyCollections) {
        return getMoleculeSummaries(interactorService.fetchIntactObjects(query, parameters, first, max, loadLazyCollections));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<MoleculeSummary> fetchIntactObjects(String query, Map<String, Object> parameters, boolean loadLazyCollections) {
        return getMoleculeSummaries(interactorService.fetchIntactObjects(query, parameters, loadLazyCollections));
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countInteractionsByMoleculeAc( IntactInteractor molecule ) {
        return getIntactDao().getInteractionDao().countInteractionsInvolvingInteractor(molecule.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countComplexesByMoleculeAc( IntactInteractor molecule ) {
        return getIntactDao().getComplexDao().countComplexesInvolvingInteractor(molecule.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countMoleculeSetsByMoleculeAc(IntactInteractor pub) {
        return getIntactDao().getInteractorPoolDao().countMoleculeSetsInvolvingInteractor(pub.getAc());
    }

    @Override
    public void saveOrUpdate(MoleculeSummary object) throws PersisterException, FinderException, SynchronizerException {
        throw new UnsupportedOperationException("The molecule summary service is read only");
    }

    @Override
    public void saveOrUpdate(Collection<? extends MoleculeSummary> objects) throws SynchronizerException, PersisterException, FinderException {
        throw new UnsupportedOperationException("The molecule summary service is read only");
    }

    @Override
    public void delete(MoleculeSummary object) throws PersisterException, FinderException, SynchronizerException {
        throw new UnsupportedOperationException("The molecule summary service is read only");
    }

    @Override
    public void delete(Collection<? extends MoleculeSummary> objects) throws SynchronizerException, PersisterException, FinderException {
        throw new UnsupportedOperationException("The molecule summary service is read only");
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

    private String getIdentityXref( IntactInteractor molecule ) {
        if (molecule instanceof InteractorPool){
            StringBuffer buffer = new StringBuffer();
            Iterator<Interactor> poolIterator = ((InteractorPool)molecule).iterator();
            if (!poolIterator.hasNext()){
                Xref xrefs = molecule.getPreferredIdentifier();


                if ( xrefs == null ) {
                    return "-";
                }

                return xrefs.getId();
            }
            else{
                while (poolIterator.hasNext()){
                    Interactor member = poolIterator.next();
                    buffer.append(getIdentityXref((IntactInteractor)member));
                    if (poolIterator.hasNext()){
                        buffer.append(", ");
                    }
                }
                return buffer.toString();
            }
        }
        else{
            Xref xrefs = molecule.getPreferredIdentifier();


            if ( xrefs == null ) {
                return "-";
            }

            return xrefs.getId();
        }
    }

    public MoleculeSummary createSummaryFrom(IntactInteractor pub, boolean countInteractions){
        MoleculeSummary summary = new MoleculeSummary();
        summary.setAc(pub.getAc());
        summary.setOrganism(pub.getOrganism() != null ? pub.getOrganism().getCommonName() : "");
        summary.setInteractorType(pub.getInteractorType() != null ? pub.getInteractorType().getShortName() : "");
        summary.setShortName(pub.getShortName());
        summary.setFullName(pub.getFullName());
        summary.setIdentityXref(getIdentityXref(pub));
        summary.setNoUniprotUpdate(isNoUniprotUpdate(pub));
        if (countInteractions){
            summary.setNumberComplexes(countComplexesByMoleculeAc(pub));
            summary.setNumberInteractions(countInteractionsByMoleculeAc(pub));
            summary.setNumberMoleculeSets(countMoleculeSetsByMoleculeAc(pub));
        }
        else{
            summary.setNumberInteractions(0);
            summary.setNumberComplexes(0);
            summary.setNumberMoleculeSets(0);
        }
        summary.setMolecule(pub);
        Annotation caution = AnnotationUtils.collectFirstAnnotationWithTopic(pub.getAnnotations(), Annotation.CAUTION_MI, Annotation.CAUTION);
        summary.setCaution(caution != null ? caution.getValue() : null);
        Annotation internal = AnnotationUtils.collectFirstAnnotationWithTopic(pub.getAnnotations(), null, "remark-internal");
        summary.setInternalRemark(internal != null ? internal.getValue() : null);
        return summary;
    }
}
