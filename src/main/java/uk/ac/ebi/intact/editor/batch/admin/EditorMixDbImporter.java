package uk.ac.ebi.intact.editor.batch.admin;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.Interaction;
import uk.ac.ebi.intact.dataexchange.dbimporter.writer.IntactInteractionMixDbImporter;
import uk.ac.ebi.intact.jami.model.user.User;

import java.util.List;

/**
 * Editor extension of db importer
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>27/01/15</pre>
 */

public class EditorMixDbImporter extends IntactInteractionMixDbImporter {

    private String userLogin;

    @Override
    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public void write(List<? extends Interaction> is) throws Exception {
        getInteractionEvidenceService().getIntactDao().getEntityManager().clear();
        getComplexService().getIntactDao().getEntityManager().clear();
        getModelledInteractionService().getIntactDao().getEntityManager().clear();
        User user = getInteractionEvidenceService().getIntactDao().getUserDao().getByLogin(userLogin);
        if (user != null){
            getInteractionEvidenceService().getIntactDao().getUserContext().setUser(user);
            getComplexService().getIntactDao().getUserContext().setUser(user);
            getModelledInteractionService().getIntactDao().getUserContext().setUser(user);
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
