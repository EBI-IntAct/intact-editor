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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import psidev.psi.mi.jami.bridges.exception.BridgeFailedException;
import psidev.psi.mi.jami.model.CvTerm;
import uk.ac.ebi.intact.editor.config.EditorConfig;
import uk.ac.ebi.intact.editor.controller.BaseController;
import uk.ac.ebi.intact.editor.controller.curate.ChangesController;
import uk.ac.ebi.intact.editor.services.curate.cvobject.CvObjectService;
import uk.ac.ebi.intact.editor.services.curate.interaction.ParticipantImportService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.extension.AbstractIntactParticipant;
import uk.ac.ebi.intact.jami.model.extension.IntactInteractorPool;

import javax.annotation.Resource;
import javax.faces.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public abstract class AbstractParticipantImportController<T extends AbstractIntactParticipant> extends BaseController {

    private static final Log log = LogFactory.getLog(AbstractParticipantImportController.class);

    @Resource(name = "participantImportService")
    private transient ParticipantImportService participantImportService;

    @Resource(name = "cvObjectService")
    private transient CvObjectService cvService;

    @Autowired
    private ChangesController changesController;

    private List<ImportCandidate> importCandidates;
    private List<String> queriesNoResults;
    private String[] participantsToImport = new String[0];

    private CvTerm cvBiologicalRole;
    private int minStoichiometry;
    private int maxStoichiometry;

    @Resource(name = "editorConfig")
    private transient EditorConfig editorConfig;

    private boolean isLoadAsMoleculeSet=false;

    private final static String FEATURE_CHAIN = "PRO_";

    public void importParticipants(ActionEvent evt) {
        getParticipantImportService().getIntactDao().getUserContext().setUser(getCurrentUser());

        importCandidates = new ArrayList<ImportCandidate>();
        queriesNoResults = new ArrayList<String>();
        this.minStoichiometry = getEditorConfig().getDefaultStoichiometry();
        this.maxStoichiometry = getEditorConfig().getDefaultStoichiometry();

        CvObjectService cvObjectService = getCvService();

        cvBiologicalRole = cvObjectService.getDefaultBiologicalRole();

        initialiseOtherProperties();

        if (participantsToImport == null) {
            addErrorMessage("No participants to import", "Please add at least one identifier in the box");
            return;
        }

        for (String participantToImport : participantsToImport) {
            participantToImport = participantToImport.trim();

            if (participantToImport.isEmpty()) {
                continue;
            }

            // only import if the query has more than 4 chars (to avoid massive queries) {

            if (participantToImport.length() < 4) {
                queriesNoResults.add(participantToImport + " (short query - less than 4 chars.)");
            } else if (participantToImport.contains("*")) {
                queriesNoResults.add(participantToImport + " (wildcards not allowed)");
            } else {
                Set<ImportCandidate> candidates = null;
                try {
                    candidates = getParticipantImportService().importParticipant(participantToImport);
                    if (candidates.isEmpty()) {
                        queriesNoResults.add(participantToImport);
                    } else {
                        importCandidates.addAll(candidates);
                    }
                } catch (BridgeFailedException e) {
                    addErrorMessage("Cannot load interactor " + participantToImport, e.getCause() + ": " + e.getMessage());
                    queriesNoResults.add(participantToImport);
                } catch (Throwable e) {
                    addErrorMessage("Cannot load interactor " + participantToImport, e.getCause() + ": " + e.getMessage());
                    queriesNoResults.add(participantToImport);
                }
            }
        }
    }

    protected abstract void initialiseOtherProperties();

    public void importSelected(ActionEvent evt) {
        getParticipantImportService().getIntactDao().getUserContext().setUser(getCurrentUser());

        CvObjectService cvObjectService = getCvService();

        if (cvBiologicalRole == null){
            cvBiologicalRole = cvObjectService.getDefaultBiologicalRole();
        }

        initialiseOtherProperties();

        if (!isLoadAsMoleculeSet){
            for (ImportCandidate candidate : importCandidates) {
                if (candidate.isSelected()) {
                    T participant = toParticipant(candidate);
                    processAddedParticipant(participant);
                }
            }
        }
        else if (!importCandidates.isEmpty()){
            String query = StringUtils.join(participantsToImport, "_");
            IntactInteractorPool newPool = new IntactInteractorPool(query);
            ImportCandidate firstSelected = new ImportCandidate(query, newPool);
            for (ImportCandidate candidate : importCandidates) {
                if (candidate.isSelected()) {
                    if (candidate.isChain() || candidate.isIsoform()) {

                        changesController.markAsHiddenChange(candidate.getInteractor(), null, Collections.EMPTY_LIST,
                                getParticipantImportService().getIntactDao().getSynchronizerContext().getInteractorSynchronizer(), "Interactor " + candidate.getInteractor().getShortName());
                    }
                    newPool.add(candidate.getInteractor());
                }
            }

            if (firstSelected != null){
                firstSelected.setInteractor(newPool);
                T participant = toParticipant(firstSelected);
                processAddedParticipant(participant);
            }
        }

        doSave();

        participantsToImport = new String[0];
        // reset load molecule set
        this.isLoadAsMoleculeSet = false;
        cvBiologicalRole = cvObjectService.getDefaultBiologicalRole();
        resetOtherProperties();
    }

    protected void resetOtherProperties(){

    }

    protected abstract void doSave();

    protected abstract void processAddedParticipant(T participant);

    protected abstract T toParticipant(ImportCandidate candidate);

    public String[] getParticipantsToImport() {
        return participantsToImport;
    }

    public void setParticipantsToImport(String[] participantsToImport) {
        this.participantsToImport = participantsToImport;
    }

    public List<ImportCandidate> getImportCandidates() {
        return importCandidates;
    }

    public void setImportCandidates(List<ImportCandidate> importCandidates) {
        this.importCandidates = importCandidates;
    }

    public CvTerm getCvBiologicalRole() {
        return cvBiologicalRole;
    }

    public void setCvBiologicalRole(CvTerm cvBiologicalRole) {
        this.cvBiologicalRole = cvBiologicalRole;
    }

    public List<String> getQueriesNoResults() {
        return queriesNoResults;
    }

    public void setQueriesNoResults(List<String> queriesNoResults) {
        this.queriesNoResults = queriesNoResults;
    }

    public ParticipantImportService getParticipantImportService() {
        if (this.participantImportService == null){
            this.participantImportService = ApplicationContextProvider.getBean("participantImportService");
        }
        return participantImportService;
    }

    public int getMinStoichiometry() {
        return minStoichiometry;
    }

    public void setMinStoichiometry(int stoichiometry) {
        this.minStoichiometry = stoichiometry;
        this.maxStoichiometry = Math.max(minStoichiometry, this.maxStoichiometry);
    }

    public int getMaxStoichiometry() {
        return maxStoichiometry;
    }

    public void setMaxStoichiometry(int stoichiometry) {
        this.maxStoichiometry = stoichiometry;
        this.minStoichiometry = Math.min(this.minStoichiometry, maxStoichiometry);
    }

    public EditorConfig getEditorConfig() {
        if (this.editorConfig == null){
            this.editorConfig = ApplicationContextProvider.getBean("editorConfig");
        }
        return editorConfig;
    }

    public CvObjectService getCvService() {
        if (this.cvService == null){
            this.cvService = ApplicationContextProvider.getBean("cvObjectService");
        }
        return cvService;
    }

    public boolean isLoadAsMoleculeSet() {
        return isLoadAsMoleculeSet;
    }

    public void setLoadAsMoleculeSet(boolean isLoadAsMoleculeSet) {
        this.isLoadAsMoleculeSet = isLoadAsMoleculeSet;
    }
}
