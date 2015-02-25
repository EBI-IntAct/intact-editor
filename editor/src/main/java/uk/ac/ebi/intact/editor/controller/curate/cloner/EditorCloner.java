package uk.ac.ebi.intact.editor.controller.curate.cloner;

import uk.ac.ebi.intact.jami.dao.IntactDao;
import uk.ac.ebi.intact.jami.model.IntactPrimaryObject;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02/12/14</pre>
 */

public interface EditorCloner<I, T extends IntactPrimaryObject> {

    public T clone(I object, IntactDao dao);

    public void copyInitialisedProperties(T source, T target);
}
