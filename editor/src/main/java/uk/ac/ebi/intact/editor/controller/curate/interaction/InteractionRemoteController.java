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
package uk.ac.ebi.intact.editor.controller.curate.interaction;

import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.jami.bridges.exception.BridgeFailedException;
import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.Experiment;
import uk.ac.ebi.intact.editor.controller.BaseController;
import uk.ac.ebi.intact.editor.controller.curate.CurateController;
import uk.ac.ebi.intact.editor.controller.curate.experiment.ExperimentController;
import uk.ac.ebi.intact.editor.controller.curate.publication.PublicationController;
import uk.ac.ebi.intact.editor.services.curate.cvobject.CvObjectService;
import uk.ac.ebi.intact.editor.services.curate.interaction.InteractionRemoteService;
import uk.ac.ebi.intact.editor.services.curate.interaction.ParticipantImportService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * TODO comment this class header.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope( "conversation.access" )
@ConversationName( "general" )
public class InteractionRemoteController extends BaseController {

    private String[] proteins;
    private String pubRef;

    private Collection<IntactInteractionEvidence> interactions;
    private IntactPublication publication;
    private IntactExperiment experiment;
    private List<SelectItem> experimentSelectItems;

    private IntactOrganism hostOrganism;
    private CvTerm cvInteraction;
    private CvTerm cvIdentification;

    @Resource(name = "interactionRemoteService")
    private transient InteractionRemoteService interactionRemoteService;

    @Autowired
    private InteractionController interactionController;

    @Autowired
    private CurateController curateController;

    @Autowired
    private PublicationController publicationController;

    @Autowired
    private ExperimentController experimentController;

    @Autowired
    private ParticipantImportService participantImportService;

    @Resource(name = "cvObjectService")
    private transient CvObjectService cvService;

    public InteractionRemoteController() {
    }

    public void loadData( ComponentSystemEvent event ) {
        interactions = getInteractionRemoteService().loadInteractions(proteins, pubRef);

        // redirect if one found
        if (interactions.size() == 1) {
            try {
                HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
                HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
                response.sendRedirect(request.getContextPath()+"/interaction/"+interactions.iterator().next().getAc());

                FacesContext.getCurrentInstance().responseComplete();
            } catch (IOException e) {
                handleException(e);
            }
        } else if (interactions.size() == 0) {
            publication = getInteractionRemoteService().loadPublication(pubRef);

            if (publication != null) {
                experimentSelectItems = new ArrayList<SelectItem>();
                experimentSelectItems.add(new SelectItem(null, "-- Select experiment --", null, false, true, true));

                for (Experiment exp : publication.getExperiments()) {
                    experimentSelectItems.add(new SelectItem(exp, interactionController.completeExperimentLabel((IntactExperiment)exp)));
                }
            }
        }
    }

    public String createNewInteraction() {
        if (experiment == null && (cvIdentification == null || cvInteraction == null || hostOrganism == null)) {
            addErrorMessage("Fields missing", "Select a value from all the drop down lists for the experiments");
            return null;
        }

        if (publication == null) {
            // create one
            publicationController.setIdentifier(pubRef);
            publicationController.newEmpty();
            publicationController.setUnsavedChanges(true);

            publication = publicationController.getPublication();
            publicationController.autocomplete(publication, pubRef);

            publicationController.doSave(false);
        } else {
            publicationController.setPublication(publication);
        }

        if (experiment == null) {
            experimentController.newExperiment(publication);
            experimentController.setUnsavedChanges(true);

            experiment = experimentController.getExperiment();
            experiment.setParticipantIdentificationMethod(cvIdentification);
            experiment.setHostOrganism(hostOrganism);
            experiment.setInteractionDetectionMethod(cvInteraction);

            experimentController.doSave(false);

        } else {
            experimentController.setExperiment(experiment);
        }

        interactionController.newInteraction(publication, experiment);

        List<ImportCandidate> candidates = new ArrayList<ImportCandidate>();

        // we get the first result that matches the primaryId. If none, get the first element
        for (String protein : proteins) {

            Set<ImportCandidate> proteinCandidates = null;
            boolean candidateFound = false;

            try {
                proteinCandidates = participantImportService.importParticipant(protein);

                for (ImportCandidate candidate : proteinCandidates) {
                    if (candidate.getPrimaryAcs().contains(protein)) {
                        candidates.add(candidate);
                        candidateFound = true;
                        break;
                    }
                }

                if (!candidateFound && !proteinCandidates.isEmpty()) {
                    candidates.add(proteinCandidates.iterator().next());
                }
            } catch (BridgeFailedException e) {
                addErrorMessage("Cannot load interactor " + protein, e.getCause() + ": " + e.getMessage());
            } catch (FinderException e) {
                addErrorMessage("Cannot load interactor " + protein, e.getCause() + ": " + e.getMessage());
            } catch (SynchronizerException e) {
                addErrorMessage("Cannot load interactor " + protein, e.getCause() + ": " + e.getMessage());
            } catch (PersisterException e) {
                addErrorMessage("Cannot load interactor " + protein, e.getCause() + ": " + e.getMessage());
            } catch (Throwable e) {
                addErrorMessage("Cannot load interactor " + protein, e.getCause() + ": " + e.getMessage());
            }
        }

        // import the proteins
        IntactInteractionEvidence interaction = interactionController.getInteraction();

        CvTerm unspecifiedExpRole = getCvService().getDefaultExperimentalRole();
        CvTerm unspecifiedBioRole = getCvService().getDefaultBiologicalRole();

        for (ImportCandidate candidate : candidates) {
            IntactInteractor interactor = candidate.getInteractor();

            IntactParticipantEvidence component = new IntactParticipantEvidence(interactor);
            component.setInteraction(interaction);
            component.setExperimentalRole(unspecifiedExpRole);
            component.setBiologicalRole(unspecifiedBioRole);
            interaction.addParticipant(component);
        }

        // update the status of the controller
        interactionController.refreshExperimentLists();
        interactionController.refreshParticipants();
        interactionController.updateShortLabel();
        interactionController.setUnsavedChanges(true);

        return curateController.edit(interaction);
    }

    public String[] getProteins() {
        return proteins;
    }

    public void setProteins(String[] proteins) {
        this.proteins = proteins;
    }

    public String getProteinsAsString() {
        return StringUtils.join(proteins, ", ");
    }

    public String getPubRef() {
        return pubRef;
    }

    public void setPubRef(String pubRef) {
        this.pubRef = pubRef;
    }

    public Collection<IntactInteractionEvidence> getInteractions() {
        return interactions;
    }

    public IntactPublication getPublication() {
        return publication;
    }

    public List<SelectItem> getExperimentSelectItems() {
        return experimentSelectItems;
    }

    public IntactExperiment getExperiment() {
        return experiment;
    }

    public IntactOrganism getHostOrganism() {
        return hostOrganism;
    }

    public void setHostOrganism(IntactOrganism hostOrganism) {
        this.hostOrganism = hostOrganism;
    }

    public CvTerm getCvInteraction() {
        return cvInteraction;
    }

    public void setCvInteraction(CvTerm cvInteraction) {
        this.cvInteraction = cvInteraction;
    }

    public CvTerm getCvIdentification() {
        return cvIdentification;
    }

    public void setCvIdentification(CvTerm cvIdentification) {
        this.cvIdentification = cvIdentification;
    }

    public InteractionRemoteService getInteractionRemoteService() {
        if (this.interactionRemoteService == null){
            this.interactionRemoteService = ApplicationContextProvider.getBean("interactionRemoteService");
        }
        return interactionRemoteService;
    }

    public CvObjectService getCvService() {
        if (this.cvService == null){
            this.cvService = ApplicationContextProvider.getBean("cvObjectService");
        }
        return cvService;
    }
}
