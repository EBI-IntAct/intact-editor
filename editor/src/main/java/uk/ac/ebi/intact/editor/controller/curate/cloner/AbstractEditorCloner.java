package uk.ac.ebi.intact.editor.controller.curate.cloner;

import uk.ac.ebi.intact.editor.controller.admin.UserManagerController;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.dao.IntactDao;
import uk.ac.ebi.intact.jami.model.IntactPrimaryObject;

import java.util.Date;

/**
 * General class for cloning objects in the editor
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>01/12/14</pre>
 */

public abstract class AbstractEditorCloner<I, T extends IntactPrimaryObject> implements EditorCloner<I, T>{

    protected void initAuditProperties(T auditable, IntactDao dao){
        // set current user
        UserManagerController userController = ApplicationContextProvider.getBean("userManagerController");
        auditable.setCreator(userController.getCurrentUser().getLogin());
        auditable.setUpdator(userController.getCurrentUser().getLogin());
        auditable.setCreated(new Date());
        auditable.setUpdated(auditable.getCreated());
        // set current user
        dao.getUserContext().
                setUser(((UserManagerController)ApplicationContextProvider.
                        getBean("userManagerController")).
                        getCurrentUser());
    }

    public abstract T clone(I object, IntactDao dao);
}
