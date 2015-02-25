package uk.ac.ebi.intact.editor.controller.curate.cloner;

import psidev.psi.mi.jami.model.Alias;
import psidev.psi.mi.jami.model.Organism;
import uk.ac.ebi.intact.jami.dao.IntactDao;
import uk.ac.ebi.intact.jami.model.extension.IntactOrganism;
import uk.ac.ebi.intact.jami.model.extension.OrganismAlias;

/**
 * Cloner for organism
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>05/12/14</pre>
 */

public class OrganismCloner extends AbstractEditorCloner<Organism, IntactOrganism>{
    @Override
    public IntactOrganism clone(Organism object, IntactDao dao) {
        IntactOrganism clone = new IntactOrganism(object.getTaxId());

        initAuditProperties(clone, dao);

        clone.setCommonName(object.getCommonName());
        clone.setScientificName(object.getScientificName());
        clone.setCellType(object.getCellType());
        clone.setTissue(object.getTissue());
        clone.setCompartment(object.getCompartment());

        // copy collections
        for (Alias alias : object.getAliases()){
            clone.getAliases().add(new OrganismAlias(alias.getType(), alias.getName()));
        }

        return clone;
    }

    @Override
    public void copyInitialisedProperties(IntactOrganism source, IntactOrganism target) {
        target.setCommonName(source.getCommonName());
        target.setScientificName(source.getScientificName());
        target.setTaxId(source.getTaxId());
        target.setCellType(source.getCellType());
        target.setTissue(source.getTissue());

        if (source.areAliasesInitialized()){
            target.getAliases().clear();
            target.getAliases().addAll(source.getAliases());
        }
    }
}
