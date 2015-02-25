/**
 * Copyright 2010 The European Bioinformatics Institute, and others.
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
package uk.ac.ebi.intact.editor.controller.curate.complex;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.jami.model.Feature;
import psidev.psi.mi.jami.model.ModelledFeature;
import uk.ac.ebi.intact.editor.controller.curate.ChangesController;
import uk.ac.ebi.intact.editor.controller.curate.interaction.AbstractParticipantImportController;
import uk.ac.ebi.intact.editor.controller.curate.interaction.ImportCandidate;
import uk.ac.ebi.intact.editor.services.curate.cvobject.CvObjectService;
import uk.ac.ebi.intact.jami.model.extension.IntactComplex;
import uk.ac.ebi.intact.jami.model.extension.IntactInteractor;
import uk.ac.ebi.intact.jami.model.extension.IntactModelledParticipant;
import uk.ac.ebi.intact.jami.model.extension.IntactStoichiometry;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope("conversation.access")
@ConversationName("general")
public class ModelledParticipantImportController extends AbstractParticipantImportController<IntactModelledParticipant> {

    private static final Log log = LogFactory.getLog(ModelledParticipantImportController.class);

    @Autowired
    private ChangesController changesController;

    @Autowired
    private ComplexController interactionController;

    @Override
    protected void initialiseOtherProperties() {
       // nothing to do
    }

    @Override
    protected void processAddedParticipant(IntactModelledParticipant participant) {
        interactionController.addParticipant(participant);

        interactionController.setUnsavedChanges(true);
    }

    @Override
    protected void doSave() {
        interactionController.doSave(false);
    }

    @Override
    protected IntactModelledParticipant toParticipant(ImportCandidate candidate) {
        return toParticipant(candidate, interactionController.getComplex());
    }

    protected IntactModelledParticipant toParticipant(ImportCandidate candidate, IntactComplex interaction) {
        IntactInteractor interactor = candidate.getInteractor();

        if (getCvBiologicalRole() == null) {
            CvObjectService cvObjectService = getCvService();
            if (getCvBiologicalRole() == null) {
                setCvBiologicalRole(cvObjectService.getDefaultBiologicalRole());
            }
        }

        IntactModelledParticipant component = new IntactModelledParticipant(interactor);
        component.setInteraction(interaction);
        component.setBiologicalRole(getCvBiologicalRole());
        component.setStoichiometry(new IntactStoichiometry(getMinStoichiometry(), getMaxStoichiometry()));

        if (candidate.isChain() || candidate.isIsoform()) {
            Collection<String> parentAcs = new ArrayList<String>();

            if (interaction.getAc() != null) {
                parentAcs.add(interaction.getAc());

                addParentAcsTo(parentAcs, interaction);
            }

            changesController.markAsHiddenChange(interactor, interaction, parentAcs,
                    getParticipantImportService().getIntactDao().getSynchronizerContext().getInteractorSynchronizer(), "Interactor "+interactor.getShortName());
        }

        // add cloned features
        for (Feature f : candidate.getClonedFeatures()){
            component.addFeature((ModelledFeature)f);
        }

        return component;
    }

    /**
     * Add all the parent acs of this interaction
     *
     * @param parentAcs
     * @param inter
     */
    protected void addParentAcsTo(Collection<String> parentAcs, IntactComplex inter) {
        if (inter.getAc() != null) {
            parentAcs.add(inter.getAc());
        }
    }

    public ComplexController getInteractionController() {
        return interactionController;
    }
}
