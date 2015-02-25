/**
 * Copyright 2011 The European Bioinformatics Institute, and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.intact.editor.services.curate.organism;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.bridges.exception.BridgeFailedException;
import psidev.psi.mi.jami.bridges.uniprot.taxonomy.UniprotTaxonomyFetcher;
import psidev.psi.mi.jami.model.Alias;
import psidev.psi.mi.jami.model.Organism;
import uk.ac.ebi.intact.jami.dao.IntactDao;
import uk.ac.ebi.intact.jami.model.extension.IntactOrganism;
import uk.ac.ebi.intact.jami.model.extension.OrganismAlias;

import javax.annotation.Resource;

/**
 * Checks if the biosource exist in IntAct first.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Service
public class EditorBioSourceService extends UniprotTaxonomyFetcher {

    @Resource(name = "intactDao")
    private IntactDao intactDao;

    public EditorBioSourceService() {
        super();
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public IntactOrganism getBiosourceByTaxid(int taxid) throws BridgeFailedException {
        IntactOrganism biosource = intactDao.getOrganismDao().getByTaxidOnly(taxid);

        if (biosource == null) {
            Organism org = super.fetchByTaxID(taxid);
            biosource = new IntactOrganism(taxid);
            biosource.setCommonName(org.getCommonName());
            biosource.setScientificName(org.getScientificName());

            // copy collections
            for (Alias alias : org.getAliases()){
                biosource.getAliases().add(new OrganismAlias(alias.getType(), alias.getName()));
            }
        }

        return biosource;
    }
}
