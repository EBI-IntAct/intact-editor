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
import psidev.psi.mi.jami.model.Organism;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.jami.model.extension.IntactInteractor;
import uk.ac.ebi.intact.jami.model.extension.IntactOrganism;
import uk.ac.ebi.intact.jami.service.IntactQueryResultIterator;
import uk.ac.ebi.intact.jami.service.IntactService;
import uk.ac.ebi.intact.jami.service.OrganismService;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import javax.annotation.Resource;
import java.util.*;

/**
 */
@Service
public class OrganismSummaryService extends AbstractEditorService implements IntactService<OrganismSummary> {

    @Resource(name = "organismService")
    private OrganismService organismService;

    public OrganismSummaryService() {
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public long countAll() {
        return this.organismService.countAll();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<OrganismSummary> iterateAll() {
        return new IntactQueryResultIterator<OrganismSummary>(this);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<OrganismSummary> fetchIntactObjects(int first, int max) {
        List<Organism> pubs = this.organismService.fetchIntactObjects(first, max);
        return getOrganismSummaries(pubs);
    }

    private List<OrganismSummary> getOrganismSummaries(List<Organism> pubs) {
        List<OrganismSummary> summaryList = new ArrayList<OrganismSummary>(pubs.size());
        for (Organism pub : pubs){
            summaryList.add(createSummaryFrom((IntactOrganism)pub));
        }
        return summaryList;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public long countAll(String countQuery, Map<String, Object> parameters) {
        return this.organismService.countAll(countQuery, parameters);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<OrganismSummary> iterateAll(String countQuery, String query, Map<String, Object> parameters) {
        return new IntactQueryResultIterator<OrganismSummary>(this, countQuery, query, parameters);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<OrganismSummary> fetchIntactObjects(String query, Map<String, Object> parameters, int first, int max) {
        return getOrganismSummaries(organismService.fetchIntactObjects(query, parameters, first, max));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<OrganismSummary> fetchIntactObjects(String query, Map<String, Object> parameters) {
        return getOrganismSummaries(organismService.fetchIntactObjects(query, parameters));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<OrganismSummary> iterateAll(boolean loadLazyCollections) {
        return new IntactQueryResultIterator<OrganismSummary>(this, loadLazyCollections);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<OrganismSummary> fetchIntactObjects(int first, int max, boolean loadLazyCollections) {
        return getOrganismSummaries(organismService.fetchIntactObjects(first, max, loadLazyCollections));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public Iterator<OrganismSummary> iterateAll(String countQuery, String query, Map<String, Object> parameters, boolean loadLazyCollections) {
        return new IntactQueryResultIterator<OrganismSummary>(this, countQuery, query, parameters, loadLazyCollections);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<OrganismSummary> fetchIntactObjects(String query, Map<String, Object> parameters, int first, int max, boolean loadLazyCollections) {
        return getOrganismSummaries(organismService.fetchIntactObjects(query, parameters, first, max, loadLazyCollections));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public List<OrganismSummary> fetchIntactObjects(String query, Map<String, Object> parameters, boolean loadLazyCollections) {
        return getOrganismSummaries(organismService.fetchIntactObjects(query, parameters, loadLazyCollections));
    }

    @Override
    public void saveOrUpdate(OrganismSummary object) throws PersisterException, FinderException, SynchronizerException {
        throw new UnsupportedOperationException("The organism summary service is read only");
    }

    @Override
    public void saveOrUpdate(Collection<? extends OrganismSummary> objects) throws SynchronizerException, PersisterException, FinderException {
        throw new UnsupportedOperationException("The organism summary service is read only");
    }

    @Override
    public void delete(OrganismSummary object) throws PersisterException, FinderException, SynchronizerException {
        throw new UnsupportedOperationException("The organism summary service is read only");
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countParticipantsExpressIn( String biosourceAc ) {
        return getIntactDao().getParticipantEvidenceDao().countParticipantsByExpressedInOrganism(biosourceAc);
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countComplexesByOrganism( String biosourceAc ) {
        return getIntactDao().getComplexDao().countComplexesByOrganism(biosourceAc);
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countExperimentsByHostOrganism( String biosourceAc ) {
        return getIntactDao().getExperimentDao().countExperimentsByHostOrganism(biosourceAc);
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countInteractorsByOrganism( String biosourceAc ) {
        return getIntactDao().getInteractorDao(IntactInteractor.class).countInteractorsByOrganism(biosourceAc);
    }

    @Override
    public void delete(Collection<? extends OrganismSummary> objects) throws SynchronizerException, PersisterException, FinderException {
        throw new UnsupportedOperationException("The organism summary service is read only");
    }

    private OrganismSummary createSummaryFrom(IntactOrganism pub){
        OrganismSummary summary = new OrganismSummary();
        summary.setAc(pub.getAc());
        summary.setCommonName(pub.getCommonName());
        summary.setScientificName(pub.getScientificName());
        summary.setTaxId(pub.getTaxId());
        summary.setNumberParticipants(countParticipantsExpressIn(pub.getAc()));
        summary.setNumberComplexes(countComplexesByOrganism(pub.getAc()));
        summary.setNumberExperiments(countExperimentsByHostOrganism(pub.getAc()));
        summary.setNumberMolecules(countInteractorsByOrganism(pub.getAc()));
        return summary;
    }
}
