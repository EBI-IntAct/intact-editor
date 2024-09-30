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
import uk.ac.ebi.intact.editor.util.LazyDataModelFactory;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.lifecycle.LifeCycleStatus;
import uk.ac.ebi.intact.jami.model.user.Role;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope( "session" )
public class DashboardController extends BaseController {
    public static final List<String> DEFAULT_STATUS_SHOWN = List.of(
            LifeCycleStatus.NEW.shortLabel(),
            LifeCycleStatus.CURATION_IN_PROGRESS.shortLabel(),
            LifeCycleStatus.READY_FOR_CHECKING.shortLabel());
    public static final List<String> DEFAULT_COMPLEX_TYPES_SHOWN = List.of("curated");
    private LazyDataModel<PublicationSummary> allPublications;
    private LazyDataModel<PublicationSummary> ownedByUser;
    private LazyDataModel<PublicationSummary> reviewedByUser;
    private LazyDataModel<ComplexSummary> allComplexes;
    private LazyDataModel<ComplexSummary> complexesOwnedByUser;
    private LazyDataModel<ComplexSummary> complexesReviewedByUser;

    private boolean hideAcceptedAndReleased;
    private List<String> publicationStatusToShow;
    private List<String> complexStatusToShow;
    private List<String> complexTypesToShow;

    private boolean isPublicationTableEnabled = false;
    private boolean isComplexTableEnabled = false;

    @Resource(name = "dashboardQueryService")
    private transient DashboardQueryService queryService;

    @Autowired
    private UserSessionController userSessionController;

    public DashboardController() {
        hideAcceptedAndReleased = true;

        publicationStatusToShow = DEFAULT_STATUS_SHOWN;
        complexStatusToShow = DEFAULT_STATUS_SHOWN;
        complexTypesToShow = DEFAULT_COMPLEX_TYPES_SHOWN;
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
        this.refreshPublicationsTable();
        this.refreshComplexesTable();
    }

    public void refreshPublicationsTable() {
        final String userId = userSessionController.getCurrentUser().getLogin().toUpperCase();

        if (isPublicationTableEnabled){
            String additionalSql = getStatusToShowSql(publicationStatusToShow, DEFAULT_STATUS_SHOWN);
            allPublications = getQueryService().loadAllPublications(additionalSql);
            ownedByUser = getQueryService().loadPublicationsOwnedBy(userId, additionalSql);
            reviewedByUser = getQueryService().loadPublicationsReviewedBy(userId, additionalSql);
        }
    }

    public void refreshComplexesTable() {
        final String userId = userSessionController.getCurrentUser().getLogin().toUpperCase();

        if (isComplexTableEnabled){
            String additionalSql = getAdditionalSqlForComplexes();
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

    public String[] getPublicationStatusToShow() {
        return publicationStatusToShow.toArray(new String[0]);
    }

    public void setPublicationStatusToShow(String[] publicationStatusToShow) {
        this.publicationStatusToShow = Arrays.asList(publicationStatusToShow);
    }

    public String[] getComplexStatusToShow() {
        return complexStatusToShow.toArray(new String[0]);
    }

    public void setComplexStatusToShow(String[] complexStatusToShow) {
        this.complexStatusToShow = Arrays.asList(complexStatusToShow);
    }

    public String[] getComplexTypesToShow() {
        return complexTypesToShow.toArray(new String[0]);
    }

    public void setComplexTypesToShow(String[] complexTypesToShow) {
        this.complexTypesToShow = Arrays.asList(complexTypesToShow);
    }

    public void onComplexTypeChange() {
        if (this.complexTypesToShow.contains("curated")) {
            this.complexStatusToShow = DEFAULT_STATUS_SHOWN;
        } else if (this.complexTypesToShow.contains("predicted")) {
            this.complexStatusToShow = List.of(LifeCycleStatus.READY_FOR_RELEASE.shortLabel());
        }
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

    private String getStatusToShowSql(List<String> statusToShow, List<String> defaultStatus) {
        if (statusToShow.isEmpty()) {
            addWarningMessage("No statuses selected", "Using default status selection");
            statusToShow = defaultStatus;
        }

        StringBuilder statusToShowSql = new StringBuilder();

        for (int i=0; i<statusToShow.size(); i++) {
            if (i>0) {
                statusToShowSql.append(" or");
            }
            statusToShowSql.append(" p.cvStatus.shortName = '").append(statusToShow.get(i)).append("'");
        }

        return statusToShowSql.toString();
    }

    private String getComplexTypesToShowSql() {
        if (complexTypesToShow.isEmpty()) {
            addWarningMessage("No complex type selected", "Using default complex type selection");
            complexTypesToShow = DEFAULT_COMPLEX_TYPES_SHOWN;
        }

        StringBuilder complexesToShowSql = new StringBuilder();

        for (int i=0; i<complexTypesToShow.size(); i++) {
            if (i>0) {
                complexesToShowSql.append(" or");
            }
            if (complexTypesToShow.get(i).equals("predicted")) {
                complexesToShowSql.append(" p.predictedComplex is true");
            } else {
                complexesToShowSql.append(" p.predictedComplex is false or p.predictedComplex is null");
            }
        }

        return complexesToShowSql.toString();
    }

    private String getAdditionalSqlForComplexes() {
        String typeToShowSql = getComplexTypesToShowSql();
        List<String> defaultComplexStatus = DEFAULT_STATUS_SHOWN;
        if (!complexTypesToShow.contains("curated") && complexTypesToShow.contains("predicted")) {
            defaultComplexStatus = List.of(LifeCycleStatus.READY_FOR_RELEASE.shortLabel());
        }
        return "(" + typeToShowSql + ") and (" + getStatusToShowSql(complexStatusToShow, defaultComplexStatus) + ")";
    }
}
