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
package uk.ac.ebi.intact.editor.services.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.jami.model.IntactPrimaryObject;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Service
public class HqlQueryService extends AbstractEditorService {
    public static final int MAX_RESULTS = 200;

    public HqlQueryService() {
        super();
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public List<? extends IntactPrimaryObject> runQuery(int maxResults, String hqlQuery) {
        maxResults = Math.min(maxResults, MAX_RESULTS);

        hqlQuery = cleanQuery(hqlQuery);

        EntityManager em = getIntactDao().getEntityManager();
        Query query = em.createQuery(hqlQuery);
        query.setMaxResults(maxResults);

        return query.getResultList();
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public <T> List<T[]> runNativeQuery(int maxResults, String sqlQuery) {
        maxResults = Math.min(maxResults, MAX_RESULTS);

        EntityManager em = getIntactDao().getEntityManager();
        Query query = em.createNativeQuery(sqlQuery);
        query.setMaxResults(maxResults);

        return query.getResultList();
    }

    private String cleanQuery(String hqlQuery) {
        return hqlQuery.replaceAll(";", "");
    }
}
