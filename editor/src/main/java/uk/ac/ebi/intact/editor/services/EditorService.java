package uk.ac.ebi.intact.editor.services;

import uk.ac.ebi.intact.jami.dao.IntactDao;
import uk.ac.ebi.intact.jami.interceptor.IntactTransactionSynchronization;

/**
 * Editor service interface
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>27/11/14</pre>
 */

public interface EditorService {

    public IntactDao getIntactDao();

    public IntactTransactionSynchronization getAfterCommitExecutor();
}
