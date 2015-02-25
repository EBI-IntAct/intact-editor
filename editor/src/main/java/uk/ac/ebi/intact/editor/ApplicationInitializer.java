package uk.ac.ebi.intact.editor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import uk.ac.ebi.intact.editor.services.admin.UserAdminService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;

/**
 * Application initializer.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 2.0
 */
@Component
public class ApplicationInitializer implements InitializingBean {

    private static final Log log = LogFactory.getLog( ApplicationInitializer.class );

    @Override
    public void afterPropertiesSet() throws Exception {

        UserAdminService userAdminService = ApplicationContextProvider.getBean("userAdminService");
        userAdminService.createDefaultRoles();
        userAdminService.createDefaultUsers();
    }
}
