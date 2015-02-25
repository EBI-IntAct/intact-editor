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
package uk.ac.ebi.intact.editor.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.Source;
import uk.ac.ebi.intact.editor.controller.misc.AbstractUserController;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.context.IntactConfiguration;
import uk.ac.ebi.intact.jami.model.extension.IntactSource;
import uk.ac.ebi.intact.jami.model.user.Preference;
import uk.ac.ebi.intact.jami.model.user.User;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

/**
 * User session service
 */
@Service
public class UserSessionService extends AbstractEditorService {

    private static final Log log = LogFactory.getLog( UserSessionService.class );

    public UserSessionService() {
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public User loadUser(String login) {
        User user = getIntactDao().getUserDao().getByLogin(login);
        if (user != null && user.getAc() != null) {
            Hibernate.initialize(user.getRoles());
            Hibernate.initialize(user.getPreferences());
        }

        return user;
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public void notifyLastActivity(User user) throws SynchronizerException, FinderException, PersisterException {

        DateTime dateTime = new DateTime();
        String dateTimeStr = dateTime.toString("dd/MM/yyyy HH:mm");

        if (user != null) {
            Preference pref = user.getPreference("last.activity");

            if (pref == null) {
                pref = new Preference("last.activity");
                user.getPreferences().add(pref);
            }

            pref.setValue(dateTimeStr);

            updateIntactObject(user, getIntactDao().getUserDao());
        }
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED, readOnly = true)
    public Source getUserInstitution(User user) {
        if (user == null){
            return null;
        }
        Preference instiPref = user.getPreference(AbstractUserController.INSTITUTION_AC);
        IntactConfiguration jamiConfiguration = ApplicationContextProvider.getBean("intactJamiConfiguration");

        if (instiPref == null) {
            return jamiConfiguration.getDefaultInstitution();
        }

        Source institution = getIntactDao().getSourceDao().getByAc(instiPref.getValue());

        if (institution == null) {
            return jamiConfiguration.getDefaultInstitution();
        }
        else if (((IntactSource)institution).getAc() != null){
            Hibernate.initialize(((IntactSource)institution).getDbXrefs());
        }

        return institution;
    }
}
