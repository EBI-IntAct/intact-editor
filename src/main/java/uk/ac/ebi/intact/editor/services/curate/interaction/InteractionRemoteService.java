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
package uk.ac.ebi.intact.editor.services.curate.interaction;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.ParticipantEvidence;
import psidev.psi.mi.jami.model.Publication;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.jami.model.extension.IntactInteractionEvidence;
import uk.ac.ebi.intact.jami.model.extension.IntactInteractor;
import uk.ac.ebi.intact.jami.model.extension.IntactPublication;

import java.util.Collection;
import java.util.Iterator;

/**
 * TODO comment this class header.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Service
public class InteractionRemoteService extends AbstractEditorService {

    public InteractionRemoteService() {
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public Collection<IntactInteractionEvidence> loadInteractions( String[]  proteins, String pubRef) {
        Collection<IntactInteractionEvidence> interactions = getIntactDao().getInteractionDao().getByInteractorsPrimaryId(proteins);

        Iterator<IntactInteractionEvidence> iterator = interactions.iterator();

        if (pubRef != null) {
            while (iterator.hasNext()) {
                IntactInteractionEvidence interaction = iterator.next();
                if (interaction.getExperiment() != null && interaction.getExperiment().getPublication() != null){
                    Publication pub = interaction.getExperiment().getPublication();
                    if (pub.getPubmedId() != null && !pub.getPubmedId().equals(pubRef)) {
                        iterator.remove();
                    }
                    else{
                        for (ParticipantEvidence participant : interaction.getParticipants()){
                            IntactInteractor interactor = (IntactInteractor)participant.getInteractor();
                            if (interactor.getAc() != null){
                                Hibernate.initialize(interactor.getDbXrefs());
                            }
                        }
                    }
                }
                else{
                    iterator.remove();
                }
            }
        }

        return interactions;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public IntactPublication loadPublication( String pubRef) {
        IntactPublication pub = getIntactDao().getPublicationDao().getByPubmedId(pubRef);

        if (pub != null){
            if (pub.getAc() != null){
                Hibernate.initialize(pub.getExperiments());
            }
            initialiseXrefs(pub.getDbXrefs());
        }
        return pub;
    }
}
