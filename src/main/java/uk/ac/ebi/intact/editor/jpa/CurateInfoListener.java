package uk.ac.ebi.intact.editor.jpa;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.event.spi.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.ac.ebi.intact.editor.controller.curate.ChangesController;
import uk.ac.ebi.intact.editor.controller.curate.CuratorContextController;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.IntactPrimaryObject;

import java.util.Collections;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class CurateInfoListener implements PostUpdateEventListener, PostInsertEventListener, PostDeleteEventListener {

    private static final Log log = LogFactory.getLog( CurateInfoListener.class );

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        if (!isHttpSessionAvailable()) return;

        final Object entity = event.getEntity();

        if (entity instanceof IntactPrimaryObject) {
            IntactPrimaryObject io = (IntactPrimaryObject) entity;

            if (io.getAc() == null || !getChangesController().isDeletedAc(io.getAc())) {
                getCuratorContextController()
                    .addInfoMessage( getCuratorContextController().intactObjectSimpleName(io) +" updated", "- "+io.getAc() );

                getChangesController().removeFromUnsaved(io, Collections.EMPTY_LIST);
            }

            if (log.isDebugEnabled()) log.debug("Updated: "+io.getAc());
        }
    }



    @Override
    public void onPostDelete(PostDeleteEvent event) {
        if (!isHttpSessionAvailable()) return;

         final Object entity = event.getEntity();

        if (entity instanceof IntactPrimaryObject) {

            IntactPrimaryObject io = (IntactPrimaryObject) entity;
            getChangesController()
                .addInfoMessage( getCuratorContextController().intactObjectSimpleName(io) +" deleted", "- "+io.getAc() );

            getChangesController().removeFromUnsaved(io, Collections.EMPTY_LIST);

            if (log.isDebugEnabled()) log.debug("Deleted: "+io.getAc());
        }
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        if (!isHttpSessionAvailable()) return;

        final Object entity = event.getEntity();

        if (entity instanceof IntactPrimaryObject) {
            IntactPrimaryObject io = (IntactPrimaryObject) entity;
            getChangesController()
                .addInfoMessage( getCuratorContextController().intactObjectSimpleName(io) +" created", "- "+io.getAc() );

            getChangesController().removeFromUnsaved(io, Collections.EMPTY_LIST);

            if (log.isDebugEnabled()) log.debug("Created: "+io.getAc());
        }
    }

    public ChangesController getChangesController() {
        return (ChangesController) ApplicationContextProvider.getBean("changesController");
    }


    public CuratorContextController getCuratorContextController() {
        return (CuratorContextController) ApplicationContextProvider.getBean("curatorContextController");
    }

    public boolean isHttpSessionAvailable() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return (attr != null);
    }
}
