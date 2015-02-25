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
package uk.ac.ebi.intact.editor.controller.dashboard;

import org.primefaces.model.LazyDataModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import uk.ac.ebi.intact.editor.controller.BaseController;
import uk.ac.ebi.intact.editor.controller.UserSessionController;
import uk.ac.ebi.intact.editor.services.dashboard.DashboardQueryService;
import uk.ac.ebi.intact.editor.services.summary.ComplexSummary;
import uk.ac.ebi.intact.editor.services.summary.PublicationSummary;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.user.Role;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope( "session" )
public class DashboardController extends BaseController {
    public static final String[] DEFAULT_STATUS_SHOWN = new String[]{"new", "curation in progress", "ready for checking"};
    private LazyDataModel<PublicationSummary> allPublications;
    private LazyDataModel<PublicationSummary> ownedByUser;
    private LazyDataModel<PublicationSummary> reviewedByUser;
    private LazyDataModel<ComplexSummary> allComplexes;
    private LazyDataModel<ComplexSummary> complexesOwnedByUser;
    private LazyDataModel<ComplexSummary> complexesReviewedByUser;

    private boolean hideAcceptedAndReleased;
    private String[] statusToShow;

    private boolean isPublicationTableEnabled = false;
    private boolean isComplexTableEnabled = false;

    @Resource(name = "dashboardQueryService")
    private transient DashboardQueryService queryService;

    @Autowired
    private UserSessionController userSessionController;

    public DashboardController() {
        hideAcceptedAndReleased = true;

        statusToShow = DEFAULT_STATUS_SHOWN;
    }

    public void loadData( ComponentSystemEvent event ) {
        if (!FacesContext.getCurrentInstance().isPostback()) {
            if (userSessionController.hasRole(Role.ROLE_CURATOR) || userSessionController.hasRole(Role.ROLE_REVIEWER) ){
                isPublicationTableEnabled = true;
            }
            else{
                isPublicationTableEnabled = false;
            }
            if (userSessionController.hasRole(Role.ROLE_COMPLEX_CURATOR) || userSessionController.hasRole(Role.ROLE_COMPLEX_REVIEWER) ){
                isComplexTableEnabled = true;
            }
            else{
                isComplexTableEnabled = false;
            }
            refreshAllTables();
        }
    }

    public void refreshAllTables() {
        final String userId = userSessionController.getCurrentUser().getLogin().toUpperCase();

        if (statusToShow.length == 0) {
            addWarningMessage("No statuses selected", "Using default status selection");
            statusToShow = DEFAULT_STATUS_SHOWN;
        }

        StringBuilder statusToShowSql = new StringBuilder();

        for (int i=0; i<statusToShow.length; i++) {
            if (i>0) {
                statusToShowSql.append(" or");
            }
            statusToShowSql.append(" p.cvStatus.shortName = '").append(statusToShow[i]).append("'");
        }

        String additionalSql = statusToShowSql.toString();

        if (isPublicationTableEnabled){

            allPublications = getQueryService().loadAllPublications(additionalSql);

            ownedByUser = getQueryService().loadPublicationsOwnedBy(userId, additionalSql);

            reviewedByUser = getQueryService().loadPublicationsReviewedBy(userId, additionalSql);
        }
        if (isComplexTableEnabled){
            allComplexes = getQueryService().loadAllComplexes(additionalSql);

            complexesOwnedByUser = getQueryService().loadComplexesOwnedBy(userId, additionalSql);

            complexesReviewedByUser = getQueryService().loadComplexesReviewedBy(userId, additionalSql);
        }
    }

    public LazyDataModel<PublicationSummary> getAllPublications() {
        return allPublications;
    }

    public LazyDataModel<PublicationSummary> getOwnedByUser() {
        return ownedByUser;
    }

    public LazyDataModel<PublicationSummary> getReviewedByUser() {
        return reviewedByUser;
    }

    public boolean isHideAcceptedAndReleased() {
        return hideAcceptedAndReleased;
    }

    public void setHideAcceptedAndReleased(boolean hideAcceptedAndReleased) {
        this.hideAcceptedAndReleased = hideAcceptedAndReleased;
    }

    public String[] getStatusToShow() {
        return statusToShow;
    }

    public void setStatusToShow(String[] statusToShow) {
        this.statusToShow = statusToShow;
    }

    public LazyDataModel<ComplexSummary> getAllComplexes() {
        return allComplexes;
    }

    public LazyDataModel<ComplexSummary> getComplexesOwnedByUser() {
        return complexesOwnedByUser;
    }

    public LazyDataModel<ComplexSummary> getComplexesReviewedByUser() {
        return complexesReviewedByUser;
    }

    public boolean isPublicationTableEnabled() {
        return isPublicationTableEnabled;
    }

    public boolean isComplexTableEnabled() {
        return isComplexTableEnabled;
    }

    public DashboardQueryService getQueryService() {
        if (this.queryService == null){
            this.queryService = ApplicationContextProvider.getBean("dashboardQueryService");
        }
        return queryService;
    }
}
