package uk.ac.ebi.intact.editor.batch.admin;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.dataexchange.dbimporter.writer.IntactDbImporter;
import uk.ac.ebi.intact.jami.model.user.User;

import java.util.List;

/**
 * Editor extension of db importer
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>27/01/15</pre>
 */

public class EditorDbImporter<I> extends IntactDbImporter<I> {

    private String userLogin;

    @Override
    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public void write(List<? extends I> is) throws Exception {
        getIntactService().getIntactDao().getEntityManager().clear();
        User user = getIntactService().getIntactDao().getUserDao().getByLogin(userLogin);
        if (user != null){
            getIntactService().getIntactDao().getUserContext().setUser(user);
        }
        super.write(is);
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }
}
