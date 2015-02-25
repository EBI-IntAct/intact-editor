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
package uk.ac.ebi.intact.editor.controller.curate.interaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.Feature;
import psidev.psi.mi.jami.model.FeatureEvidence;
import psidev.psi.mi.jami.model.Organism;
import uk.ac.ebi.intact.editor.controller.curate.ChangesController;
import uk.ac.ebi.intact.jami.model.extension.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope("conversation.access")
@ConversationName("general")
public class ParticipantImportController extends AbstractParticipantImportController<IntactParticipantEvidence> {

    private static final Log log = LogFactory.getLog(ParticipantImportController.class);

    @Autowired
    private InteractionController interactionController;

    @Autowired
    private ChangesController changesController;

    private CvTerm cvExperimentalRole;
    private Organism expressedIn;
    private List<CvTerm> cvExperimentalPreparations;
    private List<CvTerm> cvIdentifications;

    @Override
    protected void initialiseOtherProperties() {
        if (cvExperimentalRole == null){
            cvExperimentalRole = getCvService().getDefaultExperimentalRole();
        }
    }

    @Override
    protected void doSave() {
        interactionController.doSave(false);
    }

    @Override
    protected void processAddedParticipant(IntactParticipantEvidence participant) {
        interactionController.addParticipant(participant);

        interactionController.setUnsavedChanges(true);
    }

    @Override
    protected IntactParticipantEvidence toParticipant(ImportCandidate candidate) {
        return toParticipant(candidate, interactionController.getInteraction());
    }

    protected IntactParticipantEvidence toParticipant(ImportCandidate candidate, IntactInteractionEvidence interaction) {
        IntactInteractor interactor = candidate.getInteractor();

        IntactParticipantEvidence component = new IntactParticipantEvidence(interactor);
        component.setInteraction(interaction);
        component.setExperimentalRole(cvExperimentalRole);
        component.setBiologicalRole(getCvBiologicalRole());
        component.setExpressedInOrganism(expressedIn);
        component.setStoichiometry(new IntactStoichiometry(getMinStoichiometry(), getMaxStoichiometry()));

        if (candidate.isChain() || candidate.isIsoform()) {
            Collection<String> parentAcs = new ArrayList<String>();

            if (interaction.getAc() != null) {
                parentAcs.add(interaction.getAc());

                addParentAcsTo(parentAcs, interaction);
            }

            changesController.markAsHiddenChange(interactor, interaction, parentAcs,
                    getParticipantImportService().getIntactDao().getSynchronizerContext().getInteractorSynchronizer(), "Interactor " + interactor.getShortName());
        }

        if (cvExperimentalPreparations != null) {
            for (CvTerm term : cvExperimentalPreparations){
                if (term != null){
                    component.getExperimentalPreparations().add(term);
                }
            }
        }
        if (cvIdentifications != null) {
            for (CvTerm term : cvIdentifications){
                if (term != null){
                    component.getIdentificationMethods().add(term);
                }
            }
        }

        // add cloned features
        for (Feature f : candidate.getClonedFeatures()){
            component.addFeature((FeatureEvidence)f);
        }

        return component;
    }

    /**
     * Add all the parent acs of this interaction
     *
     * @param parentAcs
     * @param inter
     */
    protected void addParentAcsTo(Collection<String> parentAcs, IntactInteractionEvidence inter) {
        if (inter.getAc() != null) {
            parentAcs.add(inter.getAc());
        }

        interactionController.addParentAcsTo(parentAcs, (IntactExperiment)inter.getExperiment());
    }

    public CvTerm getCvExperimentalRole() {
        return cvExperimentalRole;
    }

    public void setCvExperimentalRole(CvTerm cvExperimentalRole) {
        this.cvExperimentalRole = cvExperimentalRole;
    }

    public InteractionController getInteractionController() {
        return interactionController;
    }

    public Organism getExpressedIn() {
        return expressedIn;
    }

    public void setExpressedIn(Organism expressedIn) {
        this.expressedIn = expressedIn;
    }

    public List<CvTerm> getCvExperimentalPreparations() {
        return cvExperimentalPreparations;
    }

    public List<CvTerm> getCvIdentifications() {
        return cvIdentifications;
    }

    public void setCvExperimentalPreparations(List<CvTerm> cvExperimentalPreparations) {
        this.cvExperimentalPreparations = cvExperimentalPreparations;
    }

    public void setCvIdentifications(List<CvTerm> cvIdentifications) {
        this.cvIdentifications = cvIdentifications;
    }

    @Override
    protected void resetOtherProperties() {
        cvExperimentalRole = getCvService().getDefaultExperimentalRole();
        this.cvExperimentalPreparations = null;
        this.cvIdentifications = null;
        this.expressedIn = null;
    }
}
