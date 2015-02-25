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
package uk.ac.ebi.intact.editor.services.admin.report;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.apache.catalina.LifecycleEvent;
import org.joda.time.DateTime;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.jami.model.extension.IntactComplex;
import uk.ac.ebi.intact.jami.model.extension.IntactPublication;
import uk.ac.ebi.intact.jami.model.lifecycle.LifeCycleEvent;
import uk.ac.ebi.intact.jami.model.lifecycle.LifeCycleEventType;

import javax.faces.event.ActionEvent;
import javax.persistence.Query;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 */
@Service
public class AssignmentReportService extends AbstractEditorService {

    public AssignmentReportService() {
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED, readOnly = true)
    public List<AssignmentInfo> calculatePublicationReviewerAssignments(Date fromDate, Date toDate) {
        List<AssignmentInfo> assignmentInfos = new ArrayList<AssignmentInfo>();

        Query query = getIntactDao().getEntityManager().createQuery("select distinct p from IntactPublication p join p.lifecycleEvents as e where " +
                "e.cvEvent.shortName = :cvEvent and e.when >= :dateFrom and e.when <= :dateTo and e.note is null");
        query.setParameter("cvEvent", LifeCycleEventType.READY_FOR_CHECKING.shortLabel());
        query.setParameter("dateFrom", fromDate);
        query.setParameter("dateTo", new DateTime(toDate).plusDays(1).minusSeconds(1).toDate());

        List<IntactPublication> pubs = query.getResultList();

        Multiset<String> multiset = HashMultiset.create();

        for (IntactPublication pub : pubs){
            for (LifeCycleEvent event : pub.getLifecycleEvents()) {
                multiset.add(pub.getCurrentReviewer().getLogin());
            }
        }

        int total = multiset.size();

        for (String reviewer : multiset.elementSet()) {
            int count = multiset.count(reviewer);
            int percentage = count * 100 / total;
            assignmentInfos.add(new AssignmentInfo(reviewer, count, percentage));
        }

        return assignmentInfos;
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED, readOnly = true)
    public List<AssignmentInfo> calculateComplexReviewerAssignments(Date fromDate, Date toDate) {
        List<AssignmentInfo> assignmentInfos = new ArrayList<AssignmentInfo>();

        Query query = getIntactDao().getEntityManager().createQuery("select distinct c from IntactComplex c join c.lifecycleEvents as e where " +
                "e.cvEvent.shortName = :cvEvent and e.when >= :dateFrom and e.when <= :dateTo and e.note is null");
        query.setParameter("cvEvent", LifeCycleEventType.READY_FOR_CHECKING.shortLabel());
        query.setParameter("dateFrom", fromDate);
        query.setParameter("dateTo", new DateTime(toDate).plusDays(1).minusSeconds(1).toDate());

        List<IntactComplex> complexes = query.getResultList();

        Multiset<String> multiset = HashMultiset.create();

        for (IntactComplex pub : complexes){
            for (LifeCycleEvent event : pub.getLifecycleEvents()) {
                multiset.add(pub.getCurrentReviewer().getLogin());
            }
        }

        int total = multiset.size();

        for (String reviewer : multiset.elementSet()) {
            int count = multiset.count(reviewer);
            int percentage = count * 100 / total;
            assignmentInfos.add(new AssignmentInfo(reviewer, count, percentage));
        }

        return assignmentInfos;
    }

    public class AssignmentInfo implements Serializable{

        private String reviewer;
        private int count;
        private int percentage;

        private AssignmentInfo(String reviewer, int count, int percentage) {
            this.reviewer = reviewer;
            this.count = count;
            this.percentage = percentage;
        }

        public String getReviewer() {
            return reviewer;
        }

        public int getCount() {
            return count;
        }

        public int getPercentage() {
            return percentage;
        }
    }
}
