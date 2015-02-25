package uk.ac.ebi.intact.editor.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.Authentication;
import org.springframework.security.ui.logout.LogoutHandler;
import uk.ac.ebi.intact.editor.controller.UserListener;
import uk.ac.ebi.intact.editor.controller.admin.UserManagerController;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.user.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Custom behaviour at logout.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 2.0
 */
public class AppLogoutHandler implements LogoutHandler {

    private static final Log log = LogFactory.getLog( AppLogoutHandler.class );

    public AppLogoutHandler() {
        super();
    }

    @Override
    public void logout( HttpServletRequest request, HttpServletResponse response, Authentication authentication ) {
        if (authentication == null) {
            log.warn( "No authentication token" );
            return;
        }

        log.debug( "Logout [" + authentication.getPrincipal() + "]" );

        UserManagerController userManagerController = ApplicationContextProvider.getBean("userManagerController");

        User user = userManagerController.getUser(authentication.getPrincipal().toString());

        if ( user != null ) {
            log.debug( "Destroying session for user " + authentication.getPrincipal() + ": " );

            // get all the "user listener" beans and notify the logout
            final Map<String,UserListener> userListeners = ApplicationContextProvider.getApplicationContext().getBeansOfType(UserListener.class);

            for (UserListener userListener : userListeners.values()) {
                userListener.userLoggedOut(user);
            }
        } else {
            log.debug( "Destroying HTTP session - no User available:"+authentication.getPrincipal() );
        }
    }
}
