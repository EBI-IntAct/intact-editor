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
package uk.ac.ebi.intact.editor.services.reviewer;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.jami.model.user.Preference;
import uk.ac.ebi.intact.jami.model.user.Role;
import uk.ac.ebi.intact.jami.model.user.User;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import java.util.Collection;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Service
@Scope( BeanDefinition.SCOPE_PROTOTYPE )
public class ReviewerAvailabilityService extends AbstractEditorService {

    public ReviewerAvailabilityService() {
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public Collection<User> loadAllReviewers() {
        Collection<User> reviewers = getIntactDao().getUserDao().getByRole(Role.ROLE_REVIEWER);
        for (User user : reviewers){
            if (user.getAc() != null){
                Hibernate.initialize(user.getPreferences());
                Hibernate.initialize(user.getRoles());
            }
        }
        return reviewers;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public Collection<User> loadAllComplexReviewers() {
        Collection<User> reviewers = getIntactDao().getUserDao().getByRole(Role.ROLE_COMPLEX_REVIEWER);
        for (User user : reviewers){
            if (user.getAc() != null){
                Hibernate.initialize(user.getPreferences());
                Hibernate.initialize(user.getRoles());
            }
        }
        return reviewers;
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public void saveUsers(Collection<User> reviewers) throws SynchronizerException, FinderException, PersisterException {
        for (User reviewer : reviewers) {

            Preference pref = reviewer.getPreference(Preference.KEY_REVIEWER_AVAILABILITY);

            if (pref != null) {
                updateIntactObject(reviewer, getIntactDao().getUserDao());
            }

        }
    }
}
