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
package uk.ac.ebi.intact.editor.controller.admin.report;

import org.joda.time.DateTime;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import uk.ac.ebi.intact.editor.controller.BaseController;
import uk.ac.ebi.intact.editor.services.admin.report.AssignmentReportService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;

import javax.annotation.Resource;
import javax.faces.event.ActionEvent;
import java.util.Date;
import java.util.List;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope("session")
public class AssignmentReportController extends BaseController {

    private List<AssignmentReportService.AssignmentInfo> publicationAssignmentInfos;
    private List<AssignmentReportService.AssignmentInfo> complexAssignmentInfos;
    private Date fromDate;
    private Date toDate;

    @Resource(name = "assignmentReportService")
    private transient AssignmentReportService assignmentReportService;

    public AssignmentReportController() {
        fromDate = new DateTime().minusMonths(1).toDate();
        toDate = new DateTime().toDate();
    }

    public void calculatePublicationAssigments(ActionEvent evt) {
        publicationAssignmentInfos = getAssignmentReportService().calculatePublicationReviewerAssignments(fromDate, toDate);
    }

    public void calculateComplexAssigments(ActionEvent evt) {
        complexAssignmentInfos = getAssignmentReportService().calculateComplexReviewerAssignments(fromDate, toDate);
    }

    public List<AssignmentReportService.AssignmentInfo> getPublicationAssignmentInfos() {
        return publicationAssignmentInfos;
    }

    public List<AssignmentReportService.AssignmentInfo> getComplexAssignmentInfos() {
        return complexAssignmentInfos;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public AssignmentReportService getAssignmentReportService() {
        if (this.assignmentReportService == null){
            this.assignmentReportService = ApplicationContextProvider.getBean("assignmentReportService");
        }
        return assignmentReportService;
    }
}
