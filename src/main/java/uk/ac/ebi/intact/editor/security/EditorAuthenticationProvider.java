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
package uk.ac.ebi.intact.editor.security;

import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import uk.ac.ebi.intact.editor.controller.UserListener;
import uk.ac.ebi.intact.editor.controller.admin.UserManagerController;
import uk.ac.ebi.intact.editor.services.UserSessionService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.user.Role;
import uk.ac.ebi.intact.jami.model.user.User;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Map;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class EditorAuthenticationProvider implements AuthenticationProvider {

    private static final Log log = LogFactory.getLog( EditorAuthenticationProvider.class );

    @Autowired
    private UserManagerController userManagerController;
    @Resource(name = "userSessionService")
    private UserSessionService userSessionService;

    public Authentication authenticate( Authentication authentication ) throws AuthenticationException {

        log.debug( "======================= AUTHENTICATE ======================" );

        if ( log.isDebugEnabled() ) {
            log.debug( "Currently, there are " + userManagerController.getLoggedInUsers().size() + " users connected." );
            log.debug( "Authenticating user: " + authentication.getPrincipal() );
        }

        final User user = loadIntactUser(authentication);

        // get all the "user listener" beans and notify the login
        final Map<String,UserListener> userListeners = ApplicationContextProvider.getApplicationContext().getBeansOfType(UserListener.class);

        for (UserListener userListener : userListeners.values()) {
            userListener.userLoggedIn(user);
        }

        Collection<GrantedAuthority> authorities = Lists.newArrayList();
        log.info( user.getLogin() + " roles: " + user.getRoles() );
        for ( Role role : user.getRoles() ) {
            final String authorityName = "ROLE_" + role.getName();
            log.info( "Adding GrantedAuthority: '" + authorityName + "'" );
            authorities.add(new SimpleGrantedAuthority(authorityName));
        }

        return new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials(), authorities);
    }

    public User loadIntactUser(Authentication authentication) {
        final User user = userSessionService.loadUser( authentication.getPrincipal().toString() );

        if ( user == null || !user.getPassword().equals( authentication.getCredentials() ) ) {
            if ( log.isDebugEnabled() ) log.debug( "Bad credentials for user: " + authentication.getPrincipal() );
            throw new BadCredentialsException( "Unknown user or incorrect password." );
        }

        if ( user.isDisabled() ) {
            throw new DisabledException( "User " + user.getLogin() + " has been disabled, please contact the IntAct team." );
        }


        if ( log.isInfoEnabled() ) log.info( "Authentication successful for user: " + authentication.getPrincipal() );
        return user;
    }

    public boolean supports( Class authentication ) {
        return true;
    }
}