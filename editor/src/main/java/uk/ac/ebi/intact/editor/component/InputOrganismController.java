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
package uk.ac.ebi.intact.editor.component;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import uk.ac.ebi.intact.editor.controller.BaseController;
import uk.ac.ebi.intact.editor.services.curate.organism.BioSourceService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.extension.IntactOrganism;

import javax.annotation.Resource;
import javax.faces.event.ActionEvent;
import javax.faces.event.ComponentSystemEvent;
import java.util.List;

/**
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 */
@Component
@Scope("conversation.access")
public class InputOrganismController extends BaseController {

    private static final Log log = LogFactory.getLog(InputOrganismController.class);

    private String query;
    private IntactOrganism selected;
    private List<IntactOrganism> bioSources;
    private String dialogId;

    @Resource(name = "bioSourceService")
    private transient BioSourceService bioSourceService;

    public InputOrganismController() {
    }

    public void loadBioSources( ComponentSystemEvent evt) {
        if (log.isTraceEnabled()) log.trace("Load Biosources");

        if (query == null) {
            setBioSources(getBioSourceService().loadAllBioSources());
        }
    }

    public void loadOrganisms( ComponentSystemEvent evt) {
        if (log.isTraceEnabled()) log.trace("Load Organisms");

        if (query == null) {
            setBioSources(getBioSourceService().loadAllOrganisms());
        }
    }

    public void searchBioSources(ActionEvent evt) {
        String query = getQuery();

        if (log.isTraceEnabled()) log.trace("Searching with query: "+query);

        this.bioSources = getBioSourceService().searchBioSources(query);
    }

    public void searchOrganisms(ActionEvent evt) {
        String query = getQuery();

        if (log.isTraceEnabled()) log.trace("Searching with query: "+query);

        this.bioSources = getBioSourceService().searchOrganisms(query);
    }

    public void selectBioSource( IntactOrganism bioSource ) {
        setSelectedBioSource( bioSource );
    }

    public List<IntactOrganism> getBioSources() {
        return bioSources;
    }

    public void setBioSources( List<IntactOrganism> bioSources ) {
        this.bioSources = bioSources;
    }

    public IntactOrganism getSelectedBioSource() {
        return selected;
    }

    public void setSelectedBioSource( IntactOrganism selectedBioSource ) {
        this.selected = selectedBioSource;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getDialogId() {
        return dialogId;
    }

    public void setDialogId(String dialogId) {
        this.dialogId = dialogId;
    }

    public BioSourceService getBioSourceService() {
        if (this.bioSourceService == null){
            this.bioSourceService = ApplicationContextProvider.getBean("bioSourceService");
        }
        return bioSourceService;
    }
}