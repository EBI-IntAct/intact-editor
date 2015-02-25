/**
 * Copyright 2011 The European Bioinformatics Institute, and others.
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
package uk.ac.ebi.intact.editor.controller.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.primefaces.model.DualListModel;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import uk.ac.ebi.intact.editor.controller.BaseController;
import uk.ac.ebi.intact.editor.services.admin.InstitutionAdminService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.extension.IntactSource;
import uk.ac.ebi.intact.jami.model.user.User;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.model.DataModel;
import javax.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.List;

/** *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope( "conversation.access" )
@ConversationName( "general" )
public class InstitutionAdminController extends BaseController {
    private IntactSource[] selectedInstitutions;
    private IntactSource mergeDestinationInstitution;

    private DualListModel<uk.ac.ebi.intact.jami.model.user.User> usersDualListModel;
    private DataModel<IntactSource> institutionsDataModel;

    @Resource(name = "institutionAdminService")
    private transient InstitutionAdminService institutionAdminService;

    private static final Log log = LogFactory.getLog(InstitutionAdminController.class);

    public InstitutionAdminController() {

    }

    public void load(ComponentSystemEvent event) {
        if (!FacesContext.getCurrentInstance().isPostback()) {
            reloadDataModels();
        }
    }

    private void reloadDataModels() {
        List<User> usersSelected = new ArrayList<User>();

        institutionsDataModel = getInstitutionAdminService().getAllInstitutions();

        usersDualListModel = getInstitutionAdminService().createUserDualListModel(usersSelected);

        selectedInstitutions = null;
    }

    public void mergeSelected(ActionEvent evt) {
        if (mergeDestinationInstitution == null) {
            addErrorMessage("Destination institution not selected", "Select one in the drop down list");
            return;
        }

        int updated = 0;
        try {
            getInstitutionAdminService().getIntactDao().getUserContext().setUser(getCurrentUser());
            updated = getInstitutionAdminService().mergeSelected(mergeDestinationInstitution, selectedInstitutions);
            addInfoMessage(selectedInstitutions.length + " Institutions merged", updated + " annotated objects updated");
        } catch (SynchronizerException e) {
            addErrorMessage("Cannot merge institutions ", e.getCause() + ": " + e.getMessage());
            log.error("Cannot merge institutions", e);
        } catch (FinderException e) {
            addErrorMessage("Cannot merge institutions ", e.getCause() + ": " + e.getMessage());
            log.error("Cannot merge institutions", e);
        } catch (PersisterException e) {
            addErrorMessage("Cannot merge institutions ", e.getCause() + ": " + e.getMessage());
            log.error("Cannot merge institutions", e);
        } catch (Throwable e) {
            addErrorMessage("Cannot merge institutions ", e.getCause() + ": " + e.getMessage());
            log.error("Cannot merge institutions", e);
        }

        reloadDataModels();
    }

    public void deleteSelected(ActionEvent evt) {

        try {
            getInstitutionAdminService().getIntactDao().getUserContext().setUser(getCurrentUser());
            getInstitutionAdminService().deleteSelected(selectedInstitutions);
        } catch (SynchronizerException e) {
            addErrorMessage("Cannot delete institutions ", e.getCause() + ": " + e.getMessage());
            log.error("Cannot delete institutions", e);
        } catch (FinderException e) {
            addErrorMessage("Cannot delete institutions ", e.getCause() + ": " + e.getMessage());
            log.error("Cannot delete institutions", e);
        } catch (PersisterException e) {
            addErrorMessage("Cannot delete institutions ", e.getCause() + ": " + e.getMessage());
            log.error("Cannot delete institutions", e);
        } catch (Throwable e) {
            addErrorMessage("Cannot delete institutions ", e.getCause() + ": " + e.getMessage());
            log.error("Cannot delete institutions", e);
        }

        reloadDataModels();
    }

    public void fixReleasableOwners(ActionEvent evt) {
        if (usersDualListModel.getTarget().isEmpty()) {
            addErrorMessage("No users selected", "Add some users to fix using the picklist");
            return;
        }

        getInstitutionAdminService().getIntactDao().getUserContext().setUser(getCurrentUser());
        int updatedCount = 0;
        try {
            updatedCount = getInstitutionAdminService().fixReleasableOwners(usersDualListModel.getTarget());
            addInfoMessage("Users object ownership fixed", "Updated annotated objects: "+updatedCount);
        } catch (Throwable e) {
            addErrorMessage("Problem updating user annotated objects", e.getCause()+": "+e.getMessage());
            log.error("Problem updating user annotated objects", e);
        }

        reloadDataModels();
    }

    public IntactSource[] getSelectedInstitutions() {
        return selectedInstitutions;
    }

    public void setSelectedInstitutions(IntactSource[] selectedInstitutions) {
        this.selectedInstitutions = selectedInstitutions;
    }

    public IntactSource getMergeDestinationInstitution() {
        return mergeDestinationInstitution;
    }

    public void setMergeDestinationInstitution(IntactSource mergeDestinationInstitution) {
        this.mergeDestinationInstitution = mergeDestinationInstitution;
    }

    public DualListModel<uk.ac.ebi.intact.jami.model.user.User> getUsersDualListModel() {
        return usersDualListModel;
    }

    public void setUsersDualListModel(DualListModel<uk.ac.ebi.intact.jami.model.user.User> usersDualListModel) {
        this.usersDualListModel = usersDualListModel;
    }

    public DataModel<IntactSource> getInstitutionsDataModel() {
        return institutionsDataModel;
    }

    public List<SelectItem> getInstitutionItems() {

        return getInstitutionAdminService().getInstitutionItems();
    }

    public InstitutionAdminService getInstitutionAdminService() {
        if (this.institutionAdminService == null){
            this.institutionAdminService = ApplicationContextProvider.getBean("institutionAdminService");
        }
        return institutionAdminService;
    }
}
