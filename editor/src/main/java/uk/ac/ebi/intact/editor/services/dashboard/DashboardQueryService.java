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
package uk.ac.ebi.intact.editor.services.dashboard;

import org.primefaces.model.LazyDataModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.editor.services.summary.ComplexSummary;
import uk.ac.ebi.intact.editor.services.summary.ComplexSummaryService;
import uk.ac.ebi.intact.editor.services.summary.PublicationSummary;
import uk.ac.ebi.intact.editor.services.summary.PublicationSummaryService;
import uk.ac.ebi.intact.editor.util.LazyDataModelFactory;

import javax.annotation.Resource;

/**
 */
@Service
public class DashboardQueryService extends AbstractEditorService {

    @Resource(name = "publicationSummaryService")
    private PublicationSummaryService publicationSummaryService;

    @Resource(name = "complexSummaryService")
    private ComplexSummaryService complexSummaryService;

    public DashboardQueryService() {
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<PublicationSummary> loadAllPublications(String additionalSql){
         return  LazyDataModelFactory.createLazyDataModel(publicationSummaryService, "publicationSummaryService",
                 "select distinct p from IntactPublication p left join fetch p.dbXrefs as x where " +
                         "(p.shortLabel <> '14681455' and p.shortLabel <> 'unassigned638' " +
                         "and p.shortLabel <> '24288376' and p.shortLabel <> '24214965') and ( " + additionalSql+" )",
                 "select count(distinct p.ac) from IntactPublication p where "+
                         "(p.shortLabel <> '14681455' and p.shortLabel <> 'unassigned638' " +
                         "and p.shortLabel <> '24288376' and p.shortLabel <> '24214965') and ( "+ additionalSql+" )", "p", "updated, p.ac", false);
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<PublicationSummary> loadPublicationsOwnedBy(String userLogin, String additionalSql){
        return LazyDataModelFactory.createLazyDataModel(publicationSummaryService, "publicationSummaryService",
                "select distinct p from IntactPublication p left join fetch p.dbXrefs as x where " +
                        "(p.shortLabel <> '14681455' and p.shortLabel <> 'unassigned638' " +
                        "and p.shortLabel <> '24288376' and p.shortLabel <> '24214965') and upper(p.currentOwner.login) = '" + userLogin + "'" +
                        " and (" + additionalSql + ")",
                "select count(distinct p.ac) from IntactPublication p where " +
                        "(p.shortLabel <> '14681455' and p.shortLabel <> 'unassigned638' " +
                        "and p.shortLabel <> '24288376' and p.shortLabel <> '24214965') and upper(p.currentOwner.login) = '" + userLogin + "'" +
                        " and (" + additionalSql + ")", "p", "updated, p.ac", false
        );
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<PublicationSummary> loadPublicationsReviewedBy(String userLogin, String additionalSql){
        return LazyDataModelFactory.createLazyDataModel(publicationSummaryService, "publicationSummaryService",
                "select distinct p from IntactPublication p left join fetch p.dbXrefs as x where " +
                        "(p.shortLabel <> '14681455' and p.shortLabel <> 'unassigned638' " +
                        "and p.shortLabel <> '24288376' and p.shortLabel <> '24214965') and upper(p.currentReviewer.login) = '" + userLogin + "'" +
                        " and (" + additionalSql + ")",
                "select count(distinct p.ac) from IntactPublication p where " +
                        "(p.shortLabel <> '14681455' and p.shortLabel <> 'unassigned638' " +
                        "and p.shortLabel <> '24288376' and p.shortLabel <> '24214965') and upper(p.currentReviewer.login) = '" + userLogin + "'" +
                        " and (" + additionalSql + ")", "p", "updated, p.ac", false
        );
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<ComplexSummary> loadAllComplexes(String additionalSql){
        return LazyDataModelFactory.createLazyDataModel(complexSummaryService, "complexSummaryService",
                "select p from IntactComplex p where " + additionalSql,
                "select count(distinct p.ac) from IntactComplex p where " + additionalSql, "p", "updated, p.ac", false);
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<ComplexSummary> loadComplexesOwnedBy(String userLogin, String additionalSql){
        return LazyDataModelFactory.createLazyDataModel(complexSummaryService, "complexSummaryService",
                "select p from IntactComplex p where upper(p.currentOwner.login) = '" + userLogin + "'" +
                        " and (" + additionalSql + ")",
                "select count(distinct p.ac) from IntactComplex p where upper(p.currentOwner.login) = '" + userLogin + "'" +
                        " and (" + additionalSql + ")", "p", "updated, p.ac", false
        );
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<ComplexSummary> loadComplexesReviewedBy(String userLogin, String additionalSql){
        return LazyDataModelFactory.createLazyDataModel(complexSummaryService, "complexSummaryService",
                "select p from IntactComplex p where upper(p.currentReviewer.login) = '" + userLogin + "'" +
                        " and (" + additionalSql + ")",
                "select count(distinct p.ac) from IntactComplex p where upper(p.currentReviewer.login) = '" + userLogin + "'" +
                        " and (" + additionalSql + ")", "p", "updated, p.ac", false
        );
    }
}
