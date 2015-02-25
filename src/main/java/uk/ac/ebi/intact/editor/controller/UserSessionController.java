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
package uk.ac.ebi.intact.editor.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.jami.model.Source;
import uk.ac.ebi.intact.editor.services.UserSessionService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.user.Role;
import uk.ac.ebi.intact.jami.model.user.User;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import javax.annotation.Resource;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope( "session" )
public class UserSessionController extends BaseController implements DisposableBean {

    private static final Log log = LogFactory.getLog( UserSessionController.class );

    private User currentUser;

    @Resource(name = "userSessionService")
    private transient UserSessionService userSessionService;

    public UserSessionController() {
    }

    public User getCurrentUser() {
        return getCurrentUser(false);
    }

    public User getCurrentUser(boolean refresh) {
        if (refresh && currentUser != null) {
            currentUser = getUserSessionService().loadUser(currentUser.getLogin());
        }

        return currentUser;
    }

    public void setCurrentUser( User currentUser ) {
        this.currentUser = currentUser;
    }

    public boolean hasRole(String role) {
        if (role == null) throw new NullPointerException("Role is null");

        for (Role userRole : currentUser.getRoles()) {
            if ("ADMIN".equals(userRole.getName()) || role.equals(userRole.getName())) {
                return true;
            }
        }

        return false;
    }

    public boolean isItMe(User user) {
        if (user == null) return false;

        return user.equals(currentUser);
    }

    public void notifyLastActivity() {

        if (currentUser != null) {
            try {
                getUserSessionService().notifyLastActivity(currentUser);
            } catch (SynchronizerException e) {
                addErrorMessage("Cannot notify last activity of user "+currentUser, e.getCause()+": "+e.getMessage());
            } catch (FinderException e) {
                addErrorMessage("Cannot notify last activity of user " + currentUser, e.getCause() + ": " + e.getMessage());
            } catch (PersisterException e) {
                addErrorMessage("Cannot notify last activity of user " + currentUser, e.getCause() + ": " + e.getMessage());
            } catch (Throwable e) {
                addErrorMessage("Cannot notify last activity of user " + currentUser, e.getCause() + ": " + e.getMessage());
            }
        }
    }

    public Source getUserInstitution() {

        return currentUser != null ? getUserSessionService().getUserInstitution(currentUser) : null;
    }

    @Override
    public void destroy() throws Exception {
        if (currentUser != null){
            log.info( "UserSessionController for user '" + currentUser.getLogin() + "' destroyed" );
        }
    }

    public UserSessionService getUserSessionService() {
        if (this.userSessionService == null){
            this.userSessionService = ApplicationContextProvider.getBean("userSessionService");
        }
        return userSessionService;
    }
}
