package uk.ac.ebi.intact.editor.services.dashboard;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.jami.model.user.User;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Service
@Scope( BeanDefinition.SCOPE_PROTOTYPE )
public class UserProfileService extends AbstractEditorService {

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public User updateProfile(User user) throws SynchronizerException, FinderException, PersisterException {
        attachDaoToTransactionManager();

        return synchronizeIntactObject(user, getIntactDao().getSynchronizerContext().getUserSynchronizer(), true);
    }
}
