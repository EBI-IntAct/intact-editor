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
package uk.ac.ebi.intact.editor.controller.curate.organism;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.editor.controller.JpaAwareController;
import uk.ac.ebi.intact.jami.dao.OrganismDao;
import uk.ac.ebi.intact.jami.model.extension.IntactOrganism;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.model.SelectItem;
import java.util.*;

/**
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 */
@Service
@Lazy
public class EditorOrganismService extends JpaAwareController {

    private static final Log log = LogFactory.getLog( EditorOrganismService.class );

    private List<SelectItem> organismSelectItems;

    private boolean isInitialised = false;

    private Map<String, IntactOrganism> acOrganismMap;

    public EditorOrganismService() {
        acOrganismMap = new HashMap<String, IntactOrganism>();
    }

    public void clearAll(){
        this.organismSelectItems = null;
        isInitialised = false;
    }

    private void loadOrganisms() {
        organismSelectItems = new ArrayList<SelectItem>();
        organismSelectItems.add(new SelectItem( null, "select organism", "select organism", false, false, true ));

        OrganismDao organismDao = getIntactDao().getOrganismDao();

        Collection<IntactOrganism> loadedOrganisms = organismDao.getAllOrganisms(false, false);
        for (IntactOrganism organism : loadedOrganisms){
            acOrganismMap.put(organism.getAc(), organism);
            organismSelectItems.add(createSelectItem(organism));
        }

    }

    private SelectItem createSelectItem( IntactOrganism organism ) {
        return new SelectItem( organism, organism.getCommonName(), organism.getScientificName());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public void loadDataIfNotDone( ComponentSystemEvent event ) {
        if (!FacesContext.getCurrentInstance().isPostback()) {
            if (!isInitialised){
                loadOrganisms();
                isInitialised = true;
            }
        }
    }

    public List<SelectItem> getOrganismSelectItems() {
        return organismSelectItems;
    }

    public IntactOrganism findCvByAc(String ac){
        return acOrganismMap.get(ac);
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public void refresh( ActionEvent evt ) {
        if ( log.isDebugEnabled() ) log.debug( "Loading BioSources" );

        loadOrganisms();
    }
}