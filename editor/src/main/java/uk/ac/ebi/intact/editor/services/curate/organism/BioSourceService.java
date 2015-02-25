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
package uk.ac.ebi.intact.editor.services.curate.organism;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.*;
import uk.ac.ebi.intact.editor.controller.curate.cloner.InteractorCloner;
import uk.ac.ebi.intact.editor.controller.curate.cloner.OrganismCloner;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.extension.*;

import javax.faces.event.ActionEvent;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.model.SelectItem;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.*;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Service
@Lazy
public class BioSourceService extends AbstractEditorService {

    private static final Log log = LogFactory.getLog( BioSourceService.class );

    private Map<String, IntactOrganism> acOrganismMap;
    private Map<Integer, IntactOrganism> taxidOrganismMap;
    private List<SelectItem> bioSourceSelectItems;
    private List<SelectItem> organismSelectItems;

    private boolean isInitialised = false;


    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countAliases(IntactOrganism organis) {
        return getIntactDao().getOrganismDao().countAliasesForOrganism(organis.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public IntactOrganism loadOrganismByAc(String ac) {
        IntactOrganism organism = getIntactDao().getEntityManager().find(IntactOrganism.class, ac);

        if (organism != null){
            // initialise aliases because first tab
            initialiseAliases(organism.getAliases());

            if (organism.getCellType() != null){
                initialiseCv(organism.getCellType());
            }
            if (organism.getTissue() != null){
                initialiseCv(organism.getTissue());
            }
        }

        return organism;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public IntactOrganism reloadFullyInitialisedOrganism(IntactOrganism organism) {
        if (organism == null){
            return null;
        }

        IntactOrganism reloaded = null;
        if (!organism.areAliasesInitialized()
                && organism.getAc() != null
                && !getIntactDao().getEntityManager().contains(organism)){
            reloaded = loadOrganismByAc(organism.getAc());
        }

        // we need first to merge with reloaded complex
        if (reloaded != null){
            // detach reloaded now so not changes will be committed
            getIntactDao().getEntityManager().detach(reloaded);
            OrganismCloner cloner = new OrganismCloner();
            cloner.copyInitialisedProperties(organism, reloaded);
            organism = reloaded;
        }

        // initialise aliases because first tab
        initialiseAliases(organism.getAliases());

        if (organism.getCellType() != null && !isCvInitialised(organism.getCellType())){
            CvTerm cv = initialiseCv(organism.getCellType() );
            if (cv != organism.getCellType()  ){
                organism.setCellType(cv);
            }
        }
        if (organism.getTissue() != null && !isCvInitialised(organism.getTissue())){
            CvTerm cv = initialiseCv(organism.getTissue() );
            if (cv != organism.getTissue()  ){
                organism.setTissue(cv);
            }
        }

        return organism;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public boolean isOrganismFullyLoaded(IntactOrganism organism){
        if (organism == null){
            return true;
        }
        if (!organism.areAliasesInitialized()
                || (organism.getCellType() != null && !isCvInitialised(organism.getCellType()))
                || (organism.getTissue() != null && !isCvInitialised(organism.getTissue()))){
            return false;
        }
        return true;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public void loadData( ) {
        if ( log.isDebugEnabled() ) log.debug( "Loading BioSources" );
        synchronized (this) {
            if (isInitialised) {
                this.acOrganismMap.clear();
                this.taxidOrganismMap.clear();
                this.bioSourceSelectItems = null;
                this.organismSelectItems = null;
                isInitialised = false;
            }
            List<IntactOrganism> allBioSources = getIntactDao().getOrganismDao().getAllSorted(0, Integer.MAX_VALUE, "commonName", true);

            bioSourceSelectItems = new ArrayList<SelectItem>(allBioSources.size());
            organismSelectItems = new ArrayList<SelectItem>(allBioSources.size());

            acOrganismMap = new HashMap<String, IntactOrganism>();
            taxidOrganismMap = new HashMap<Integer, IntactOrganism>();

            bioSourceSelectItems.add(new SelectItem(null, "-- Select BioSource --", "-- Select BioSource --", false, false, true));
            organismSelectItems.add(new SelectItem(null, "-- Select Organism --", "-- Select Organism --", false, false, true));
            for (IntactOrganism bioSource : allBioSources) {

                SelectItem item = new SelectItem(bioSource, bioSource.getCommonName(), bioSource.getScientificName());
                bioSourceSelectItems.add(item);
                if (bioSource.getCellType() == null && bioSource.getTissue() == null) {
                    organismSelectItems.add(item);
                }

                if (bioSource.getCellType() != null) {
                    initialiseCv(bioSource.getCellType());
                }
                if (bioSource.getTissue() != null) {
                    initialiseCv(bioSource.getTissue());
                }

                acOrganismMap.put(bioSource.getAc(), bioSource);
                taxidOrganismMap.put(bioSource.getTaxId(), bioSource);
            }
            isInitialised = true;
        }
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public List<IntactOrganism> loadAllBioSources( ) {
        if ( log.isDebugEnabled() ) log.debug( "Loading BioSources" );

        return getIntactDao().getOrganismDao().getAllSorted(0, Integer.MAX_VALUE, "commonName", true);
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public List<IntactOrganism> loadAllOrganisms( ) {
        if ( log.isDebugEnabled() ) log.debug( "Loading BioSources" );

        return new ArrayList<IntactOrganism>(getIntactDao().getOrganismDao().getAllOrganisms(false, false));
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public List<IntactOrganism> searchBioSources(String query) {

        if (log.isTraceEnabled()) log.trace("Searching with query: "+query);

        if (query == null) {
            return Collections.EMPTY_LIST;
        }

        Query jpaQuery = getIntactDao().getEntityManager()
                .createQuery("select b from IntactOrganism b " +
                        "where lower(b.commonName) like lower(:commonName) or " +
                        "lower(b.scientificName) like lower(:scientificName) or " +
                        "b.dbTaxid = :taxId");
        jpaQuery.setParameter("commonName", query+"%");
        jpaQuery.setParameter("scientificName", query+"%");
        jpaQuery.setParameter("taxId", query);


        return jpaQuery.getResultList();
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public List<IntactOrganism> searchOrganisms(String query) {
        if (log.isTraceEnabled()) log.trace("Searching with query: "+query);

        if (query == null) {
            return Collections.EMPTY_LIST;
        }

        Query jpaQuery = getIntactDao().getEntityManager()
                .createQuery("select b from IntactOrganism b " +
                        "where (lower(b.commonName) like lower(:commonName) or " +
                        "lower(b.scientificName) like lower(:scientificName) or " +
                        "b.dbTaxid = :taxId) and b.cellType is null and b.tissue is null");
        jpaQuery.setParameter("commonName", query+"%");
        jpaQuery.setParameter("scientificName", query+"%");
        jpaQuery.setParameter("taxId", query);


        return jpaQuery.getResultList();
    }

    public IntactOrganism findBioSourceByAc( String ac ) {
        return acOrganismMap.get(ac);
    }

    public List<SelectItem> getBioSourceSelectItems() {
        return bioSourceSelectItems;
    }

    public IntactOrganism findBiosourceByTaxid(Integer taxid) {
        return taxidOrganismMap.get(taxid);
    }

    public Map<String, IntactOrganism> getAcOrganismMap() {
        return acOrganismMap;
    }

    public Map<Integer, IntactOrganism> getTaxidOrganismMap() {
        return taxidOrganismMap;
    }

    public List<SelectItem> getOrganismSelectItems() {
        return organismSelectItems;
    }

    public boolean isInitialised() {
        return isInitialised;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public void loadDataIfNecessary( ComponentSystemEvent event ) {
        if (!isInitialised()) {

            loadData();
        }
    }
}
