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
package uk.ac.ebi.intact.editor.controller.curate.publication;

import edu.ucla.mbi.imex.central.ws.v20.IcentralFault;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.joda.time.DateTime;
import org.primefaces.context.RequestContext;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.model.LazyDataModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.jami.bridges.europubmedcentral.EuroPubmedCentralFetcher;
import psidev.psi.mi.jami.bridges.exception.BridgeFailedException;
import psidev.psi.mi.jami.bridges.imex.ImexCentralClient;
import psidev.psi.mi.jami.enricher.exception.EnricherException;
import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import psidev.psi.mi.jami.utils.XrefUtils;
import uk.ac.ebi.intact.dataexchange.imex.idassigner.ImexCentralManager;
import uk.ac.ebi.intact.dataexchange.imex.idassigner.actions.PublicationImexUpdaterException;
import uk.ac.ebi.intact.editor.controller.UserSessionController;
import uk.ac.ebi.intact.editor.controller.curate.AnnotatedObjectController;
import uk.ac.ebi.intact.editor.controller.curate.UnsavedChange;
import uk.ac.ebi.intact.editor.controller.curate.cloner.EditorCloner;
import uk.ac.ebi.intact.editor.services.curate.publication.DatasetPopulator;
import uk.ac.ebi.intact.editor.services.curate.publication.PublicationEditorService;
import uk.ac.ebi.intact.editor.services.summary.ExperimentSummary;
import uk.ac.ebi.intact.editor.services.summary.ExperimentSummaryService;
import uk.ac.ebi.intact.editor.services.summary.InteractionSummary;
import uk.ac.ebi.intact.editor.services.summary.InteractionSummaryService;
import uk.ac.ebi.intact.editor.util.LazyDataModelFactory;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.lifecycle.IllegalTransitionException;
import uk.ac.ebi.intact.jami.lifecycle.LifeCycleManager;
import uk.ac.ebi.intact.jami.model.IntactPrimaryObject;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.model.lifecycle.*;
import uk.ac.ebi.intact.jami.model.user.Preference;
import uk.ac.ebi.intact.jami.sequence.SequenceManager;
import uk.ac.ebi.intact.jami.service.PublicationService;
import uk.ac.ebi.intact.jami.synchronizer.IntactDbSynchronizer;
import uk.ac.ebi.intact.jami.utils.IntactUtils;
import uk.ac.ebi.intact.jami.utils.ReleasableUtils;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope("conversation.access")
@ConversationName("general")
public class PublicationController extends AnnotatedObjectController {

    private static final Log log = LogFactory.getLog(PublicationController.class);

    public static final String SUBMITTED = "MI:0878";
    public static final String CURATION_REQUEST = "MI:0873";
    private static final String CURATION_DEPTH = "MI:0955";
    public static final String DATASET = "dataset";
    public static final String DATASET_MI_REF = "MI:0875";

    private IntactPublication publication;
    private String ac;

    private String identifier;
    private String identifierToOpen;
    private String identifierToImport;

    private boolean assignToMe = true;

    private String datasetToAdd;
    private String[] datasetsToRemove;

    private String reasonForReadyForChecking;
    private String reasonForOnHoldFromDialog;

    private boolean isCitexploreActive;

    private boolean isLifeCycleDisabled;

    private LazyDataModel<InteractionSummary> interactionDataModel;

    @Autowired
    private UserSessionController userSessionController;

    @Resource(name = "publicationService")
    private transient PublicationService publicationService;

    @Resource(name = "publicationEditorService")
    private transient PublicationEditorService publicationEditorService;

    @Resource(name = "experimentSummaryService")
    private transient ExperimentSummaryService experimentSummaryService;

    @Resource(name = "interactionSummaryService")
    private transient InteractionSummaryService interactionSummaryService;

    @Resource(name = "imexCentralManager")
    private transient ImexCentralManager imexCentralManager;

    @Resource(name = "datasetPopulator")
    private transient DatasetPopulator datasetPopulator;

    @Resource(name = "jamiLifeCycleManager")
    private transient LifeCycleManager lifecycleManager;

    private String curationDepth;

    private String newDatasetDescriptionToCreate;
    private String newDatasetNameToCreate;

    private boolean isExperimentTabDisabled = false;
    private boolean isInteractionTabDisabled = true;

    private String journal;
    private String contactEmail;
    private Short year;
    private String authors;
    private String onHold;
    private String accepted;
    private String toBeReviewed = null;
    private String imexId=null;

    private List<ExperimentSummary> experiments = Collections.EMPTY_LIST;

    private  List<SelectItem> datasetsSelectItems;

    public PublicationController() {
        experiments = new ArrayList<ExperimentSummary>();
    }

    @Override
    protected void loadCautionMessages() {
        if (this.publication != null){

            Annotation caution = AnnotationUtils.collectFirstAnnotationWithTopic(this.publication.getAnnotations(), Annotation.CAUTION_MI, Annotation.CAUTION);
            setCautionMessage(caution != null ? caution.getValue() : null);
            Annotation internal = AnnotationUtils.collectFirstAnnotationWithTopic(this.publication.getAnnotations(), null, "remark-internal");
            setInternalRemark(internal != null ? internal.getValue() : null);
            this.toBeReviewed = this.publication.getToBeReviewedComment();
            this.accepted = this.publication.getAcceptedComment();
            this.onHold = this.publication.getOnHoldComment();
            this.imexId = this.publication.getImexId();
            this.journal = this.publication.getJournal();
            this.identifier = this.publication.getPubmedId();
            this.curationDepth = this.publication.getCurationDepth().toString();
            this.authors = !this.publication.getAuthors().isEmpty() ? StringUtils.join(this.publication.getAuthors(), ", ") : null;
            Annotation contactEmail = AnnotationUtils.collectFirstAnnotationWithTopic(this.publication.getAnnotations(), Annotation.CONTACT_EMAIL_MI,
                    Annotation.CONTACT_EMAIL);
            this.contactEmail = contactEmail != null ? contactEmail.getValue() : null;
            if (publication.getPublicationDate() == null){
                this.year = null;
            }
            else{
                Calendar cal = Calendar.getInstance();
                cal.setTime(publication.getPublicationDate());
                int year = cal.get(Calendar.YEAR);
                this.year = (short)year;
            }
        }
    }

    @Override
    public IntactPrimaryObject getAnnotatedObject() {
        return getPublication();
    }

    @Override
    public void setAnnotatedObject(IntactPrimaryObject annotatedObject) {
        setPublication((IntactPublication)annotatedObject);
    }

    public void loadData(ComponentSystemEvent event) {
        if (!FacesContext.getCurrentInstance().isPostback()) {
            loadByAc();

            refreshTabs();
        }

        generalLoadChecks();
    }

    @Override
    protected void generalLoadChecks() {
        super.generalLoadChecks();
        generalPublicationLoadChecks();

        if (!getDatasetPopulator().isInitialised()){
            getDatasetPopulator().loadData();
        }
    }

    private void loadByAc() {
        if (ac != null) {
            if (publication == null || !ac.equals(publication.getAc())) {
                setPublication(getPublicationEditorService().loadPublicationByAcOrPubmedId(ac));
            }

        } else if (publication != null) {
            ac = publication.getAc();
        }

        if (publication == null) {
            addErrorMessage("No Publication with this AC", ac);
            return;
        }
    }

    public void refreshDataModels() {
        interactionDataModel = getInteractionSummaryService().refreshDataModels(this.publication, getInteractionSummaryService());
    }

    private void loadFormFields() {
        // reset previous dataset actions in the form
        this.datasetsToRemove = null;
        this.datasetToAdd = null;
        this.datasetsSelectItems = new ArrayList<SelectItem>();

        Collection<Annotation> datasets = AnnotationUtils.collectAllAnnotationsHavingTopic(this.publication.getAnnotations(), DATASET_MI_REF, DATASET);
        for (Annotation annot : datasets){
            if (annot.getValue() != null){
                SelectItem item = getDatasetPopulator().createSelectItem(annot.getValue());
                if (item != null){
                    this.datasetsSelectItems.add(item);
                }
            }
        }

        // load curationDepth
        this.curationDepth = this.publication.getCurationDepth().toString();
    }

    public boolean isCitexploreOnline() {
        if (isCitexploreActive) {
            return true;
        }
        if (log.isDebugEnabled()) log.debug("Checking Europe Pubmed Central status");
        try {
            URL url = new URL("http://www.ebi.ac.uk/webservices/citexplore/v3.0.1/service?wsdl");
            final URLConnection urlConnection = url.openConnection();
            urlConnection.setConnectTimeout(1000);
            urlConnection.setReadTimeout(1000);
            urlConnection.connect();
        } catch (Exception e) {
            log.debug("\tEurope Pubmed Central is not reachable");

            isCitexploreActive = false;
            return false;
        }

        isCitexploreActive = true;
        return true;
    }

    public String newAutocomplete() {
        identifier = identifierToImport;

        if (identifier == null) {
            addErrorMessage("Cannot auto-complete", "ID is empty");
            RequestContext requestContext = RequestContext.getCurrentInstance();
            requestContext.execute("newPublicationDlg.hide()");
            return null;
        }

        // check if already exists
        IntactPublication existingPublication = getPublicationEditorService().loadPublicationByAcOrPubmedId(identifier);

        if (existingPublication != null) {
            setPublication(existingPublication);
            addWarningMessage("Publication already exists", "Loaded from the database");
            return "/curate/publication?faces-redirect=true&includeViewParams=true";
        } else {

            // check if it already exists in IMEx central
            try {

                if (getImexCentralManager().isPublicationAlreadyRegisteredInImexCentral(identifier, Xref.PUBMED)) {
                    RequestContext requestContext = RequestContext.getCurrentInstance();
                    requestContext.execute("newPublicationDlg.hide()");
                    requestContext.execute("imexCentralActionDlg.show()");
                    return null;
                } else {
                    createNewPublication(null);

                    return "/curate/publication?faces-redirect=true";
                }
            }
            // cannot check IMEx central, add warning and create publication
            catch (BridgeFailedException e) {
                log.error(e.getMessage(), e);
                addWarningMessage("Impossible to check with IMExcentral if " + identifier + " is already curated", e.getMessage());
                createNewPublication(null);

                return "/curate/publication?faces-redirect=true";
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                addWarningMessage("Impossible to check with IMExcentral if " + identifier + " is already curated", e.getMessage());
                createNewPublication(null);

                return "/curate/publication?faces-redirect=true";
            }
        }
    }

    public void createNewPublication(ActionEvent evt) {

        if (identifier == null) {
            addErrorMessage("Cannot create publication", "ID is empty");
            return;
        }

        newEmpty();
        autocomplete(publication, identifier);

        identifier = null;
        identifierToImport = null;
    }

    public void createNewEmptyPublication(ActionEvent evt) {

        if (identifier == null) {
            addErrorMessage("Cannot create publication", "ID is empty");
            return;
        }

        newEmpty();

        identifier = null;
        identifierToImport = null;
    }

    public void doFormAutocomplete(ActionEvent evt) {
        if (publication.getPubmedId() != null) {
            autocomplete(publication, publication.getPubmedId());
        }
    }

    public void autocomplete(IntactPublication publication, String id) {
        EuroPubmedCentralFetcher citexploreClient = null;

        try {
            citexploreClient = new EuroPubmedCentralFetcher();
        } catch (Exception e) {
            addErrorMessage("Cannot auto-complete", "Citexplore service is down at the moment");
            return;
        }

        try {
            final Publication citation = citexploreClient.fetchByIdentifier(id, Xref.PUBMED);

            // new publication. No autocompletion available and this publication can be created under unassigned
            if (citation == null && publication.getAc() == null) {
                addErrorMessage("No citation was found, the auto completion has been aborted", "PMID: " + id);
                setPublication(null);
                return;
            } else if (citation == null && publication.getAc() != null) {
                addErrorMessage("This pubmed id does not exist and the autocompletion has been aborted", "PMID: " + id);
                return;
            }

            setPrimaryReference(id);

            publication.setTitle(citation.getTitle());
            publication.setJournal(citation.getJournal());
            publication.setPublicationDate(citation.getPublicationDate());
            publication.getAuthors().addAll(citation.getAuthors());

            setUnsavedChanges(true);

            addInfoMessage("Auto-complete successful", "Fetched details for: " + id);

        } catch (Throwable e) {
            addErrorMessage("Problem auto-completing publication", e.getMessage());
            e.printStackTrace();
        }
    }

    public String newEmptyUnassigned() {
        SequenceManager sequenceManager = (SequenceManager) getSpringContext().getBean("jamiSequenceManager");
        try {
            sequenceManager.createSequenceIfNotExists("unassigned_seq");
            String nextIntegerAsString = String.valueOf(sequenceManager.getNextValueForSequence(IntactUtils.UNASSIGNED_SEQ));
            identifier = "unassigned" + nextIntegerAsString;
        } catch (Exception e) {
            handleException(e);
        }

        // check if already exists, so we skip this unassigned
        IntactPublication existingPublication = getPublicationEditorService().loadPublicationByAcOrPubmedId(identifier);

        if (existingPublication != null) {
            setPublication(existingPublication);
            addWarningMessage("Publication already exists", "Loaded from the database");
            return "/curate/publication?faces-redirect=true&includeViewParams=true";
        } else {
            // check if it already exists in IMEx central
            try {

                if (getImexCentralManager().isPublicationAlreadyRegisteredInImexCentral(identifier, Xref.PUBMED)) {
                    RequestContext requestContext = RequestContext.getCurrentInstance();
                    requestContext.execute("newPublicationDlg.hide()");
                    requestContext.execute("imexCentralUnassignedActionDlg.show()");
                    return null;
                } else {
                    newEmpty();

                    identifier = null;
                    identifierToImport = null;

                    return "/curate/publication?faces-redirect=true";
                }
            }
            // cannot check IMEx central, add warning and create publication
            catch (BridgeFailedException e) {
                addWarningMessage("Impossible to check with IMExcentral if " + identifier + " is already curated", e.getMessage());
                newEmpty();

                identifier = null;
                identifierToImport = null;

                return "/curate/publication?faces-redirect=true";
            } catch (Exception e) {
                addWarningMessage("Impossible to check with IMExcentral if " + identifier + " is already curated", e.getMessage());
                newEmpty();

                identifier = null;
                identifierToImport = null;

                return "/curate/publication?faces-redirect=true";
            }
        }
    }

    public void newEmpty() {

        IntactPublication publication = new IntactPublication(identifier);
        publication.setSource(userSessionController.getUserInstitution());

        setPublication(publication);

        // add the primary reference xref
        setPrimaryReference(identifier);

        interactionDataModel = LazyDataModelFactory.createEmptyDataModel();

        String defaultCurationDepth;
        final Preference curDepthPref = getCurrentUser().getPreference("curation.depth");

        if (curDepthPref != null) {
            defaultCurationDepth = curDepthPref.getValue();
        } else {
            defaultCurationDepth = getEditorConfig().getDefaultCurationDepth();
        }

        setCurationDepth(defaultCurationDepth);
        if (curationDepth != null && curationDepth.equals("IMEx")){
            publication.setCurationDepth(CurationDepth.IMEx);
        }
        else if (curationDepth != null && curationDepth.equals("MIMIx")){
            publication.setCurationDepth(CurationDepth.MIMIx);
        }
        else if (curationDepth != null && curationDepth.equals("rapid curation")){
            publication.setCurationDepth(CurationDepth.rapid_curation);
        }

        try{
            getLifecycleManager().getStartStatus().create(publication, "Created in Editor", userSessionController.getCurrentUser());

            if (assignToMe) {
                getLifecycleManager().getNewStatus().claimOwnership(publication, userSessionController.getCurrentUser());
                getLifecycleManager().getAssignedStatus().startCuration(publication, userSessionController.getCurrentUser());
            }

            setPublication(publication);
            setUnsavedChanges(true);
        }
        catch (IllegalTransitionException e){
            addErrorMessage("Cannot create publication: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
        }
    }

    public void openByPmid(ActionEvent evt) {
        identifier = identifierToOpen;

        if (identifier == null || identifier.trim().length() == 0) {
            addErrorMessage("PMID is empty", "No PMID was supplied");
        } else {
            IntactPublication publicationToOpen = getPublicationEditorService().loadPublicationByAcOrPubmedId(identifier);

            if (publicationToOpen == null) {
                addErrorMessage("PMID not found", "There is no publication with PMID '" + identifier + "'");
            } else {
                setPublication(publicationToOpen);
            }

            identifierToOpen = null;
        }

    }

    public boolean isNewPublication() {
        return publication.getStatus() == LifeCycleStatus.NEW;
    }

    public boolean isAssigned() {
        return publication.getStatus() == LifeCycleStatus.ASSIGNED;
    }

    public boolean isCurationInProgress() {
        return publication.getStatus() == LifeCycleStatus.CURATION_IN_PROGRESS;
    }

    public boolean isReadyForChecking() {
        return publication.getStatus() == LifeCycleStatus.READY_FOR_CHECKING;
    }

    public boolean isReadyForRelease() {
        return publication.getStatus() == LifeCycleStatus.READY_FOR_RELEASE;
    }

    public boolean isAcceptedOnHold() {
        return publication.getStatus() == LifeCycleStatus.ACCEPTED_ON_HOLD;
    }

    public boolean isReleased() {
        return publication.getStatus() == LifeCycleStatus.RELEASED;
    }

    public void claimOwnership(ActionEvent evt) {
        try{
            getEditorService().claimOwnership(publication, getCurrentUser(), isAssigned());

            // automatically set as curation in progress if no one was assigned before
            if (isAssigned()) {
                addInfoMessage("Curation started", "Curation is now in progress");

                // try to register/update record in IMEx central if they don't have IMEx. IMEx records are updated automatically with a cronjob
                if (publication.getAc() != null && getImexId()==null){
                    try {
                        getImexCentralManager().registerAndUpdatePublication(publication.getAc());
                    } catch (EnricherException e) {
                        addWarningMessage("Impossible to register/update status of " + identifier + " in IMEx central", e.getMessage());
                    }
                }
            }

            addInfoMessage("Claimed publication ownership", "You are now the owner of this publication");
        }
        catch (IllegalTransitionException e){
            addErrorMessage("Cannot claim ownership of publication: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
        }
    }

    public void markAsAssignedToMe(ActionEvent evt) {
        try{
            getEditorService().markAsAssignedToMe(publication, getCurrentUser());

            addInfoMessage("Ownership claimed", "The publication has been assigned to you");

            addInfoMessage("Curation started", "Curation is now in progress");

            // try to register/update record in IMEx central if they don't have IMEx. IMEx records are updated automatically with a cronjob
            if (publication.getAc() != null && getImexId()==null){
                try {
                    getImexCentralManager().registerAndUpdatePublication(publication.getAc());
                } catch (EnricherException e) {
                    addWarningMessage("Impossible to register/update status of " + identifier + " in IMEx central", e.getMessage());
                }
            }
        }
        catch (IllegalTransitionException e){
            addErrorMessage("Cannot assign publication: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
        }
    }

    public void markAsCurationInProgress(ActionEvent evt) {
        if (!userSessionController.isItMe(publication.getCurrentOwner())) {
            addErrorMessage("Cannot mark as curation in progress", "You are not the owner of this publication");
            return;
        }
        try{
            getEditorService().markAsCurationInProgress(publication, getCurrentUser());

            addInfoMessage("Curation started", "Curation is now in progress");

            // try to register/update record in IMEx central if they don't have IMEx. IMEx records are updated automatically with a cronjob
            if (publication.getAc() != null && getImexId()==null){
                try {
                    getImexCentralManager().registerAndUpdatePublication(publication.getAc());
                } catch (EnricherException e) {
                    addWarningMessage("Impossible to register/update status of " + identifier + " in IMEx central", e.getMessage());
                }
            }
        }
        catch (IllegalTransitionException e){
            addErrorMessage("Cannot mark as curation in progress: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
        }
    }

    public void markAsReadyForChecking(ActionEvent evt) {
        if (!userSessionController.isItMe(publication.getCurrentOwner())) {
            addErrorMessage("Cannot mark as Ready for checking", "You are not the owner of this publication");
            return;
        }

        if (isBeenRejectedBefore()) {
            List<String> correctionComments = new ArrayList<String>();

            for (Experiment exp : publication.getExperiments()) {
                Annotation correctionCommentAnnot = AnnotationUtils.collectFirstAnnotationWithTopic(exp.getAnnotations(), null, Releasable.CORRECTION_COMMENT);

                if (correctionCommentAnnot != null) {
                    correctionComments.add(correctionCommentAnnot.getValue());
                }

                reasonForReadyForChecking = StringUtils.join(correctionComments, "; ");
            }

        }

        try{
            getEditorService().markAsReadyForChecking(publication, getCurrentUser(), reasonForReadyForChecking);

            reasonForReadyForChecking = null;

            addInfoMessage("Publication ready for checking", "Assigned to reviewer: " + publication.getCurrentReviewer().getLogin());
            // try to register/update record in IMEx central. IMEx records are updated automatically with a cron job so if it has an IMEx id we do nothing
            if (publication.getAc() != null && getImexId() == null){
                try {
                    getImexCentralManager().registerAndUpdatePublication(publication.getAc());
                } catch (EnricherException e) {
                    addWarningMessage("Impossible to register/update status of " + identifier + " in IMEx central", e.getMessage());
                }
            }
        }
        catch (IllegalTransitionException e){
            addErrorMessage("Cannot mark as ready for checking: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
        }
    }

    public void revertReadyForChecking(ActionEvent evt) {
        try{
            getEditorService().revertReadyForChecking(this.publication, getCurrentUser());
            // try to register/update record in IMEx central. IMEx records are updated automatically with a cron job so if it has an IMEx id we do nothing
            if (publication.getAc() != null && getImexId() == null){
                try {
                    getImexCentralManager().registerAndUpdatePublication(publication.getAc());
                } catch (EnricherException e) {
                    addWarningMessage("Impossible to register/update status of " + identifier + " in IMEx central", e.getMessage());
                }
            }
        }
        catch (IllegalTransitionException e){
            addErrorMessage("Cannot revert ready for checking: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
        }
    }

    public void revertAccepted(ActionEvent evt) {
        try{
            getEditorService().revertAccepted(this.publication, getCurrentUser(), isReadyForRelease());

            // try to register/update record in IMEx central. IMEx records are updated automatically with a cron job so if it has an IMEx id we do nothing
            if (publication.getAc() != null && getImexId() == null){
                try {
                    getImexCentralManager().registerAndUpdatePublication(publication.getAc());
                } catch (EnricherException e) {
                    addWarningMessage("Impossible to register/update status of " + identifier + " in IMEx central", e.getMessage());
                }
            }
        }
        catch (IllegalTransitionException e){
            addErrorMessage("Cannot revert accepted: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
        }
    }

    public void putOnHold(ActionEvent evt) {
        try{
            getEditorService().putOnHold(publication, getCurrentUser(), reasonForOnHoldFromDialog, isReadyForChecking(), isReleased());

            if (isReadyForRelease()) {
                addInfoMessage("On-hold added to publication", "Publication won't be released until the 'on hold' is removed");
            } else if (isReleased()) {
                addInfoMessage("On-hold added to released publication", "Data will be publicly visible until the next release");
            }

            reasonForOnHoldFromDialog = null;
            // try to register/update record in IMEx central. IMEx records are updated automatically with a cron job so if it has an IMEx id we do nothing
            if (publication.getAc() != null && getImexId() == null){
                try {
                    getImexCentralManager().registerAndUpdatePublication(publication.getAc());
                } catch (EnricherException e) {
                    addWarningMessage("Impossible to register/update status of " + identifier + " in IMEx central", e.getMessage());
                }
            }
        }
        catch (IllegalTransitionException e){
            addErrorMessage("Cannot put on hold: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
        }
    }

    public void readyForReleaseFromOnHold(ActionEvent evt) {
        setOnHold(null);

        try{
            getEditorService().readyForReleaseFromOnHold(publication, getCurrentUser());
            // try to register/update record in IMEx central. IMEx records are updated automatically with a cron job so if it has an IMEx id we do nothing
            if (publication.getAc() != null && getImexId() == null){
                try {
                    getImexCentralManager().registerAndUpdatePublication(publication.getAc());
                } catch (EnricherException e) {
                    addWarningMessage("Impossible to register/update status of " + identifier + " in IMEx central", e.getMessage());
                }
            }
        }
        catch (IllegalTransitionException e){
            addErrorMessage("Cannot mark as ready for release: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
        }
    }

    public boolean isAllExperimentsAccepted() {
        final Collection<Experiment> experiments = publication.getExperiments();
        return isAllExperimentsAccepted(experiments);
    }

    public boolean isAllExperimentsAccepted(Collection<Experiment> experiments) {
        if (experiments.isEmpty()){
            return false;
        }
        for (Experiment exp : experiments){
            if (AnnotationUtils.collectFirstAnnotationWithTopic(exp.getAnnotations(), null, Releasable.ACCEPTED) == null){
                return false;
            }
        }
        return true;
    }

    public boolean hasExperimentToBeReviewed(Collection<Experiment> experiments) {
        if (experiments.isEmpty()){
            return false;
        }
        for (Experiment exp : experiments){
            if (AnnotationUtils.collectFirstAnnotationWithTopic(exp.getAnnotations(), null, Releasable.TO_BE_REVIEWED) != null){
                return true;
            }
        }
        return false;
    }

    public boolean isBackToCurationButtonRendered() {
        return isButtonRendered(LifeCycleEventType.READY_FOR_CHECKING);
    }

    public boolean isBackToCheckingButtonRendered() {
        boolean render = isButtonRendered(LifeCycleEventType.READY_FOR_RELEASE);

        if (!render) {
            render = isButtonRendered(LifeCycleEventType.ACCEPTED);
        }

        return render;
    }

    private boolean isButtonRendered(LifeCycleEventType eventType) {
        LifeCycleEvent event = ReleasableUtils.getLastEventOfType(publication, eventType);

        if (event == null) {
            return false;
        }

        DateTime eventTime = new DateTime(event.getWhen());

        return new DateTime().isBefore(eventTime.plusMinutes(getEditorConfig().getRevertDecisionTime()));
    }

    @Override
    public void doSave(boolean refreshCurrentView) {
        boolean registerPublication = (publication != null && publication.getAc() == null);
        super.doSave(refreshCurrentView);
        // try to register/update record in IMEx central. IMEx records are updated automatically with a cron job so if it has an IMEx id we do nothing
        if (registerPublication && getImexId() == null){
            try{
                getImexCentralManager().registerAndUpdatePublication(publication.getAc());
            }
            // cannot check IMEx central, add warning and create publication
            catch (EnricherException e) {
                addWarningMessage("Impossible to register " + identifier + " in IMEx central", e.getMessage());
            }
        }
    }

    @Override
    protected void initialiseDefaultProperties(IntactPrimaryObject annotatedObject) {
        IntactPublication publication = (IntactPublication)annotatedObject;
        if (!getPublicationEditorService().isPublicationFullyLoaded(publication)){
            this.publication = getPublicationEditorService().reloadFullyInitialisedPublication(publication);
        }

        refreshDataModels();
        loadFormFields();
        refreshExperiments();

        setDescription("Publication: "+(this.publication.getPubmedId() != null ? this.publication.getPubmedId() : this.publication.getShortLabel()));
    }

    @Override
    public String doDelete() {
        if (publication.getAc() != null){
            try{
                getImexCentralManager().discardPublication(publication.getAc());
            }
            // cannot check IMEx central, add warning and create publication
            catch (BridgeFailedException e) {
                addWarningMessage("Impossible to discard " + identifier + " in IMEx central. Please do it manually.", e.getMessage());
            }
        }

        return super.doDelete();
    }

    @Override
    protected void addNewXref(AbstractIntactXref newRef) {
        if (XrefUtils.isXrefAnIdentifier(newRef) || XrefUtils.doesXrefHaveQualifier(newRef, null, "intact-secondary")
                || XrefUtils.doesXrefHaveQualifier(newRef, Xref.PRIMARY_MI, Xref.PRIMARY)){
            this.publication.getIdentifiers().add(newRef);
            this.identifier = this.publication.getPubmedId();
        }
        else{
            this.publication.getXrefs().add(newRef);
            this.imexId = this.publication.getImexId();
        }
    }

    @Override
    protected PublicationXref newXref(CvTerm db, String id, String secondaryId, String version, CvTerm qualifier) {
        PublicationXref ref = new PublicationXref(db, id, version, qualifier);
        ref.setSecondaryId(secondaryId);
        return ref;
    }

    @Override
    public PublicationXref newXref(String db, String dbMI, String id, String secondaryId, String qualifier, String qualifierMI) {
        return new PublicationXref(getCvService().findCvObject(IntactUtils.DATABASE_OBJCLASS, dbMI != null ? dbMI : db),
                id, secondaryId, getCvService().findCvObject(IntactUtils.QUALIFIER_OBJCLASS, qualifierMI != null ? qualifierMI : qualifier));
    }

    public void addDataset(ActionEvent evt) {
        if (datasetToAdd != null) {
            addAnnotation(DATASET, DATASET_MI_REF, datasetToAdd, publication.getAnnotations());

            Collection<Experiment> experiments = publication.getExperiments();

            if (!experiments.isEmpty()) {
                Collection<String> parentAcs = new ArrayList<String>();
                if (publication.getAc() != null) {
                    parentAcs.add(publication.getAc());
                }
                for (Experiment experiment : experiments) {
                    experiment.getAnnotations().add(new ExperimentAnnotation(IntactUtils.createMITopic(DATASET, DATASET_MI_REF), datasetToAdd));
                    getChangesController().markAsUnsaved((IntactExperiment)experiment, getEditorService().getIntactDao().getSynchronizerContext().getExperimentSynchronizer(),
                            "Experiment: "+((IntactExperiment)experiment).getShortLabel(), parentAcs);
                }
            }

            // register new dataset in list of existing datasets
            SelectItem item = getDatasetPopulator().createSelectItem(datasetToAdd);
            if (item != null){
                this.datasetsSelectItems.add(item);
            }

            // reset the dataset to add as it has already been added
            datasetToAdd = null;
            setUnsavedChanges(true);
        }
    }

    public void createAndAddNewDataset(ActionEvent evt) {
        if (newDatasetDescriptionToCreate == null) {
            addErrorMessage("A short sentence describing the dataset is required", "dataset description required");
        }
        if (newDatasetNameToCreate == null) {
            addErrorMessage("A short dataset name is required", "dataset name required");
        }
        if (newDatasetDescriptionToCreate != null && newDatasetNameToCreate != null) {

            String newDataset = newDatasetNameToCreate + " - " + newDatasetDescriptionToCreate;

            if (getPublicationEditorService().doesDatasetAlreadyExist(newDatasetNameToCreate)) {
                addErrorMessage("A dataset with this name already exists. Cannot create two datasets with same name", "dataset name already exists");
            } else {

                addAnnotation(DATASET, DATASET_MI_REF, newDataset, publication.getAnnotations());

                Collection<Experiment> experiments = publication.getExperiments();

                if (!experiments.isEmpty()) {
                    Collection<String> parentAcs = new ArrayList<String>();
                    if (publication.getAc() != null) {
                        parentAcs.add(publication.getAc());
                    }
                    for (Experiment experiment : experiments) {
                        experiment.getAnnotations().add(new ExperimentAnnotation(IntactUtils.createMITopic(DATASET, DATASET_MI_REF), newDataset));
                        getChangesController().markAsUnsaved((IntactExperiment)experiment, getEditorService().getIntactDao().getSynchronizerContext().getExperimentSynchronizer(),
                                "Experiment: "+((IntactExperiment)experiment).getShortLabel(), parentAcs);
                    }
                }

                // register new dataset in list of existing datasets
                SelectItem item = getDatasetPopulator().createSelectItem(newDataset);
                if (item != null){
                    this.datasetsSelectItems.add(item);
                }

                // reset the dataset to add as it has already been added
                newDatasetDescriptionToCreate = null;
                newDatasetNameToCreate = null;

                getDatasetPopulator().loadData();

                // save publication and refresh datasetPopulator
                doSave();
            }
        }
    }

    public void removeDatasets(ActionEvent evt) {
        if (datasetsToRemove != null) {
            for (String datasetToRemove : datasetsToRemove) {

                removeAnnotation(DATASET, DATASET_MI_REF, datasetToRemove, publication.getAnnotations());

                Collection<Experiment> experiments = publication.getExperiments();

                if (!experiments.isEmpty()) {
                    Collection<String> parentAcs = new ArrayList<String>();
                    if (publication.getAc() != null) {
                        parentAcs.add(publication.getAc());
                    }
                    for (Experiment experiment : experiments) {
                        experiment.getAnnotations().remove(new ExperimentAnnotation(IntactUtils.createMITopic(DATASET, DATASET_MI_REF), datasetToRemove));
                        getChangesController().markAsUnsaved((IntactExperiment) experiment, getEditorService().getIntactDao().getSynchronizerContext().getExperimentSynchronizer(),
                                "Experiment: " + ((IntactExperiment) experiment).getShortLabel(), parentAcs);
                    }
                }
            }
            setUnsavedChanges(true);
            getDatasetPopulator().loadData();
        }
    }

    public boolean isUnassigned() {
        return publication.getPubmedId() != null && publication.getPubmedId().startsWith("unassigned");
    }

    public String getAc() {
        return ac;
    }

    @Override
    public int getXrefsSize() {
        if (publication == null){
            return 0;
        }
        else {
            return publication.getDbXrefs().size();
        }
    }

    @Override
    public int getAliasesSize() {
        return 0;
    }

    @Override
    public int getAnnotationsSize() {
        if (publication == null){
            return 0;
        }
        else {
            return publication.getAnnotations().size();
        }
    }

    public int getInteractionsSize() {
        if (publication == null){
            return 0;
        }
        else {
            return interactionDataModel.getRowCount();
        }
    }

    public void setAc(String ac) {
        this.ac = ac;
    }

    public IntactPublication getPublication() {
        return publication;
    }

    @Override
    public void refreshTabsAndFocusXref() {
        refreshTabs();
    }

    @Override
    public void onTabChanged(TabChangeEvent e) {

        // the xref tab is active
        super.onTabChanged(e);

        // all the tabs selectOneMenu are disabled, we can process the tabs specific to experiment
        if (isAliasDisabled() && isXrefDisabled() && isAnnotationTopicDisabled()){
            if (e.getTab().getId().equals("experimentTab")){
                isExperimentTabDisabled = false;
                isInteractionTabDisabled = true;
                isLifeCycleDisabled = true;
            }
            else if (e.getTab().getId().equals("interactionTab")){
                isExperimentTabDisabled = true;
                isInteractionTabDisabled = false;
                isLifeCycleDisabled = true;
            }
            else if (e.getTab().getId().equals("lifeCycleTab")){
                isExperimentTabDisabled = true;
                isInteractionTabDisabled = true;
                isLifeCycleDisabled = false;
            }
            else {
                isExperimentTabDisabled = true;
                isInteractionTabDisabled = true;
                isLifeCycleDisabled = true;
            }
        }
        else {
            isExperimentTabDisabled = true;
            isInteractionTabDisabled = true;
            isLifeCycleDisabled = true;
        }
    }

    public void setPublication(IntactPublication publication) {
        this.publication = publication;

        if (publication != null) {
            this.ac = publication.getAc();

            initialiseDefaultProperties(this.publication);
        }
        else{
            this.ac = null;
        }
    }

    public String getJournal() {
        return journal;
    }

    public void setJournal(String journal) {
        this.journal = journal;
    }

    public void onJournalChanged(ValueChangeEvent evt) {
        setUnsavedChanges(true);
        String newValue = (String) evt.getNewValue();
        this.publication.setJournal(newValue);
        this.journal = newValue;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public Short getYear() {
        return year;
    }

    public void setYear(Short year) {
        this.year = year;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setPrimaryReference(String id) {
        publication.setPubmedId(id);
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public void onAuthorsChanged(ValueChangeEvent evt) {
        String newValue = (String) evt.getNewValue();
        if (newValue != null && newValue.length() > 0){
            publication.getAuthors().clear();
            if (newValue.contains(", ")){
                publication.getAuthors().addAll(Arrays.asList(newValue.split(", ")));
            }
            else{
                publication.getAuthors().add(newValue);
            }
            this.authors = newValue;
            setUnsavedChanges(true);
        }
        else{
            this.authors = null;
            this.publication.getAuthors().clear();
            setUnsavedChanges(true);
        }
    }

    public String getOnHold() {
        return this.onHold;
    }

    public void setOnHold(String reason) {
        this.onHold = reason;
    }

    public void onHoldChanged(ValueChangeEvent evt) {
        setUnsavedChanges(true);
        String newValue = (String) evt.getNewValue();

        this.publication.onHold(newValue);
        this.onHold = newValue;

        setExperimentAnnotation(Releasable.ON_HOLD, null, newValue);
    }

    public void setExperimentAnnotation(String topic, String topicMI, String text) {

        Collection<Experiment> experiments = publication.getExperiments();

        if (!experiments.isEmpty()) {
            Collection<String> parentAcs = new ArrayList<String>();
            if (publication.getAc() != null) {
                parentAcs.add(publication.getAc());
            }
            for (Experiment experiment : experiments) {
                experiment.getAnnotations().add(new ExperimentAnnotation(IntactUtils.createMITopic(topic, topicMI), text));
                getChangesController().markAsUnsaved((IntactExperiment) experiment, getEditorService().getIntactDao().getSynchronizerContext().getExperimentSynchronizer(),
                        "Experiment: " + ((IntactExperiment) experiment).getShortLabel(), parentAcs);
            }
        }
    }

    public void curationDepthChanged() {
        setUnsavedChanges(true);

        setCurationDepthAnnot(curationDepth);
        setExperimentAnnotation(Annotation.CURATION_DEPTH, Annotation.CURATION_DEPTH_MI, curationDepth);
    }

    public void contactEmailChanged(ValueChangeEvent evt) {
        setUnsavedChanges(true);
        String newValue = (String) evt.getNewValue();

        if (newValue != null && newValue.length() > 0){
            updateAnnotation(Annotation.CONTACT_EMAIL, Annotation.CONTACT_EMAIL_MI, newValue, publication.getAnnotations());
            setExperimentAnnotation(Annotation.CONTACT_EMAIL, Annotation.CONTACT_EMAIL_MI, newValue);
        }
        else{
            removeAnnotation(Annotation.CONTACT_EMAIL, Annotation.CONTACT_EMAIL_MI, publication.getAnnotations());
        }
        this.contactEmail = newValue;
    }

    public void publicationYearChanged(ValueChangeEvent evt) {
        setUnsavedChanges(true);
        Short newValue = (Short)evt.getNewValue();

        if (newValue != null && newValue > 0){
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy");
            try{
                this.publication.setPublicationDate(formatter.parse(Short.toString(newValue)));
                this.year = newValue;
                setExperimentAnnotation(Annotation.PUBLICATION_YEAR, Annotation.PUBLICATION_YEAR_MI, Short.toString(newValue));
            }
            catch (ParseException e){
                this.year = null;
                addErrorMessage("The publication year is not a valid year "+newValue, e.getCause()+": "+e.getMessage());
            }
        }
        else{
            this.publication.setPublicationDate(null);
            this.year = null;
        }
    }

    public void publicationTitleChanged(ValueChangeEvent evt) {
        setUnsavedChanges(true);
    }

    public void publicationIdentifierChanged(ValueChangeEvent evt) {
        setUnsavedChanges(true);
        String newValue = (String)evt.getNewValue();
        if (newValue != null && newValue.length() > 0){
            setPrimaryReference(newValue);

            Collection<Experiment> experiments = publication.getExperiments();

            if (!experiments.isEmpty()) {
                Collection<String> parentAcs = new ArrayList<String>();
                if (publication.getAc() != null) {
                    parentAcs.add(publication.getAc());
                }
                for (Experiment experiment : experiments) {
                    experiment.getXrefs().add(new ExperimentXref(IntactUtils.createMIDatabase(Xref.PUBMED, Xref.PUBMED_MI), newValue,
                            IntactUtils.createMIQualifier(Xref.PRIMARY, Xref.PRIMARY_MI)));
                    getChangesController().markAsUnsaved((IntactExperiment) experiment, getEditorService().getIntactDao().getSynchronizerContext().getExperimentSynchronizer(),
                            "Experiment: " + ((IntactExperiment) experiment).getShortLabel(), parentAcs);
                }
            }
            this.identifier = newValue;
        }
        else{
            setPrimaryReference(null);
            this.identifier = null;
        }
    }

    public String getAcceptedMessage() {
        return accepted;
    }

    public void onAcceptedChanged(ValueChangeEvent evt) {
        setUnsavedChanges(true);

        this.publication.onAccepted((String) evt.getNewValue());
        setExperimentAnnotation(Releasable.ACCEPTED, null, (String) evt.getNewValue());
    }

    public String getCurationDepth() {
        return this.curationDepth;
    }

    public String getShowCurationDepth() {
        if (publication.getCurationDepth() == CurationDepth.undefined) {
            return "curation depth undefined";
        }
        return publication.getCurationDepth().toString();
    }

    public void setCurationDepth(String curationDepth) {
        this.curationDepth = curationDepth;
    }

    public void setCurationDepthAnnot(String curationDepth) {
        if (curationDepth == null){
            this.publication.setCurationDepth(CurationDepth.undefined);
        }
        else{
            this.publication.setCurationDepth(CurationDepth.valueOf(curationDepth));
        }
    }

    public boolean isAccepted() {
        if (isAcceptedOrBeyond(publication)) return true;
        return isAllExperimentsAccepted();
    }

    public boolean isAcceptedOrBeyond(IntactPublication pub) {
        if (pub == null || pub.getStatus() == null) {
            return false;
        }

        return pub.getStatus() == LifeCycleStatus.ACCEPTED ||
                pub.getStatus() == LifeCycleStatus.ACCEPTED_ON_HOLD ||
                pub.getStatus() == LifeCycleStatus.READY_FOR_RELEASE ||
                pub.getStatus() == LifeCycleStatus.RELEASED;
    }

    public void setAcceptedMessage(String message) {
        this.accepted = message;
    }

    public boolean isToBeReviewed(IntactPublication pub) {
        if (pub.getExperiments().isEmpty()) {
            return false;
        }

        return hasExperimentToBeReviewed(pub.getExperiments());
    }

    public String getImexId() {
        return imexId;
    }

    public String getPublicationTitle() {
        return publication.getTitle();
    }

    public void setPublicationTitle(String publicationTitle) {
        publication.setTitle(publicationTitle);
    }

    public String getFirstAuthor() {
        if (!publication.getAuthors().isEmpty()) {
            return publication.getAuthors().get(0);
        }

        return null;
    }

    public boolean isPublicationOnHold(){
        return this.publication != null && this.publication.isOnHold();
    }

    public boolean isPublicationToBeReviewed(){
        return this.publication != null && this.publication.isToBeReviewed();
    }

    public void removeOnHold(ActionEvent evt){
        publication.removeOnHold();
        this.onHold = null;
        Collection<String> parentAcs = new ArrayList<String>();
        if (publication.getAc() != null) {
            parentAcs.add(publication.getAc());
        }
        for (Experiment experiment : this.publication.getExperiments()) {
            AnnotationUtils.removeAllAnnotationsWithTopic(experiment.getAnnotations(), null, Releasable.ON_HOLD);
            getChangesController().markAsUnsaved((IntactExperiment)experiment, getEditorService().getIntactDao().getSynchronizerContext().getExperimentSynchronizer(),
                    "Experiment: "+((IntactExperiment)experiment).getShortLabel(), parentAcs);
        }
    }

    public void removeToBeReviewed(ActionEvent evt){
        publication.removeToBeReviewed();
        this.toBeReviewed = null;
        Collection<String> parentAcs = new ArrayList<String>();
        if (publication.getAc() != null) {
            parentAcs.add(publication.getAc());
        }
        for (Experiment experiment : this.publication.getExperiments()) {
            AnnotationUtils.removeAllAnnotationsWithTopic(experiment.getAnnotations(), null, Releasable.TO_BE_REVIEWED);
            getChangesController().markAsUnsaved((IntactExperiment)experiment, getEditorService().getIntactDao().getSynchronizerContext().getExperimentSynchronizer(),
                    "Experiment: "+((IntactExperiment)experiment).getShortLabel(), parentAcs);
        }
    }

    public void acceptPublication(ActionEvent evt) {
        String accepted = "Accepted " + new SimpleDateFormat("yyyy-MMM-dd").format(new Date()).toUpperCase() + " by " + userSessionController.getCurrentUser().getLogin().toUpperCase();
        setAcceptedMessage(accepted);

        addInfoMessage("Publication accepted", "");

        try{
            getEditorService().accept(publication, userSessionController.getCurrentUser(), accepted);

            // refresh experiments with possible changes in publication title, annotations and publication identifier
            copyAnnotationsToExperiments(null);
            copyPrimaryIdentifierToExperiments();
        }
        catch (IllegalTransitionException e){
            addErrorMessage("Cannot accept publication: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
        }
    }

    public PublicationService getPublicationService() {
        if (this.publicationService == null){
            this.publicationService = ApplicationContextProvider.getBean("publicationService");
        }
        return publicationService;
    }

    public PublicationEditorService getPublicationEditorService() {
        if (this.publicationEditorService == null){
            this.publicationEditorService = ApplicationContextProvider.getBean("publicationEditorService");
        }
        return publicationEditorService;
    }

    public void assignNewImex(ActionEvent evt) {
        // save publication changes first
        doSave();

        registerEditorListenerIfNotDoneYet();

        try {

            if (Pattern.matches(ImexCentralManager.PUBMED_REGEXP.toString(), publication.getPubmedId())) {
                getImexCentralManager().assignImexAndUpdatePublication(publication.getAc());

                addInfoMessage("Successfully assigned new IMEx identifier to the publication " + publication.getShortLabel(), "");
            } else {
                addErrorMessage("Impossible to assign new IMEx id to unassigned publication. Must be assigned manually in IMEx central.", "");
            }

        } catch (PublicationImexUpdaterException e) {
            addErrorMessage("Impossible to assign new IMEx id", e.getMessage());
        } catch (EnricherException e) {
            IcentralFault f = null;
            if (e.getCause() instanceof IcentralFault){
                f = (IcentralFault) e.getCause();
            }
            else if (e.getCause().getCause() instanceof IcentralFault) {
                f = (IcentralFault) e.getCause().getCause();
            }

            processImexCentralException(publication.getPubmedId(), e, f);
        } catch (Exception e) {
            addErrorMessage("Impossible to assign new IMEx id", e.getMessage());
        }

        setAc(this.publication.getAc());
        setPublication(null);
        loadByAc();
        getChangesController().removeFromUnsaved(publication, collectParentAcsOfCurrentAnnotatedObject());
        imexId = this.publication.getImexId();
    }

    private void processImexCentralException(String publication, Exception e, IcentralFault f) {
        if (f.getFaultInfo().getFaultCode() == ImexCentralClient.USER_NOT_AUTHORIZED) {
            addErrorMessage("User not authorized", e.getMessage());
        } else if (f.getFaultInfo().getFaultCode() == ImexCentralClient.OPERATION_NOT_VALID) {
            addErrorMessage("Operation not valid", e.getMessage());
        } else if (f.getFaultInfo().getFaultCode() == ImexCentralClient.IDENTIFIER_MISSING) {
            addErrorMessage("Publication identifier is missing", e.getMessage());
        } else if (f.getFaultInfo().getFaultCode() == ImexCentralClient.IDENTIFIER_UNKNOWN) {
            addErrorMessage("Publication identifier is unknown (must be valid pubmed)", e.getMessage());
        } else if (f.getFaultInfo().getFaultCode() == ImexCentralClient.NO_RECORD) {
            addErrorMessage("No IMEx record could be found for " + publication, e.getMessage());
        } else if (f.getFaultInfo().getFaultCode() == ImexCentralClient.NO_RECORD_CREATED) {
            addErrorMessage("The publication could not be registered in IMEx central. Must be a valid pubmed Id", e.getMessage());
        } else if (f.getFaultInfo().getFaultCode() == ImexCentralClient.STATUS_UNKNOWN) {
            addErrorMessage("The status of the publication is unknown is IMEx central", e.getMessage());
        } else if (f.getFaultInfo().getFaultCode() == ImexCentralClient.NO_IMEX_ID) {
            addErrorMessage("No IMEx identifier could be found for this publication", e.getMessage());
        } else if (f.getFaultInfo().getFaultCode() == ImexCentralClient.UNKNOWN_USER) {
            addErrorMessage("Unknown user in IMEx central", e.getMessage());
        } else if (f.getFaultInfo().getFaultCode() == ImexCentralClient.UNKNOWN_GROUP) {
            addErrorMessage("Unknown group in IMEx central", e.getMessage());
        } else if (f.getFaultInfo().getFaultCode() == ImexCentralClient.OPERATION_NOT_SUPPORTED) {
            addErrorMessage("Operation not supported in IMEx central", e.getMessage());
        } else if (f.getFaultInfo().getFaultCode() == ImexCentralClient.INTERNAL_SERVER_ERROR) {
            addErrorMessage("Internal server error (IMEx central not responding)", e.getMessage());
        } else {
            addErrorMessage("Fatal error (IMEx central not responding)", e.getMessage());
        }
    }

    private void registerEditorListenerIfNotDoneYet() {
        if (imexCentralManager.getListenerList().getListenerCount() == 0) {
            imexCentralManager.addListener(new EditorImexCentralListener());
        }
    }

    public void rejectPublication(ActionEvent evt) {

        List<String> rejectionComments = new ArrayList<String>();

        for (Experiment exp : publication.getExperiments()) {
            Annotation toBeReviewed = AnnotationUtils.collectFirstAnnotationWithTopic(exp.getAnnotations(), null, Releasable.TO_BE_REVIEWED);
            if (toBeReviewed != null) {
                rejectionComments.add("[" + ((IntactExperiment)exp).getShortLabel() + ": " + toBeReviewed.getValue() + "]");
            }
        }

        rejectPublication(this.toBeReviewed + (rejectionComments.isEmpty() ? "" : " - " + StringUtils.join(rejectionComments, ", ")));

    }

    public void rejectPublication(String reasonForRejection) {
        String date = "Rejected " + new SimpleDateFormat("yyyy-MMM-dd").format(new Date()).toUpperCase() + " by " + userSessionController.getCurrentUser().getLogin().toUpperCase();

        setToBeReviewed(date + ". " + reasonForRejection);

        try{
            getEditorService().reject(publication, getCurrentUser(), this.toBeReviewed);
            addInfoMessage("Publication rejected", "");

            copyPrimaryIdentifierToExperiments();
        }
        catch (IllegalTransitionException e){
            addErrorMessage("Cannot reject publication: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
        }
    }

    public boolean isRejected(IntactPublication publication) {
        return ReleasableUtils.isRejected(publication);
    }

    public String getReasonForRejection(IntactPublication publication) {
        LifeCycleEvent event = ReleasableUtils.getLastEventOfType(publication, LifeCycleEventType.REJECTED);

        if (event != null) {
            return event.getNote();
        }

        return null;
    }

    public boolean isBeenRejectedBefore() {
        for (LifeCycleEvent evt : publication.getLifecycleEvents()) {
            if (LifeCycleEventType.REJECTED == evt.getEvent()) {
                return true;
            }
        }

        return false;
    }

    public void setToBeReviewed(String toBeReviewed) {
        this.toBeReviewed = toBeReviewed;
    }

    public void onToBeReviewedChanged(ValueChangeEvent evt) {
        setUnsavedChanges(true);
        String newValue = (String) evt.getNewValue();
        this.publication.onToBeReviewed(newValue);
        this.toBeReviewed = newValue;
    }

    public String getToBeReviewed() {
        return this.toBeReviewed;
    }

    public void copyAnnotationsToExperiments(ActionEvent evt) {
        for (Experiment exp : publication.getExperiments()) {
            for (Annotation annot : publication.getDbAnnotations()){
                Annotation existingAnnot = AnnotationUtils.collectFirstAnnotationWithTopic(exp.getAnnotations(),
                        annot.getTopic().getMIIdentifier(), annot.getTopic().getShortName());
                if (existingAnnot != null){
                    existingAnnot.setValue(annot.getValue());
                }
                else{
                    exp.getAnnotations().add(new ExperimentAnnotation(annot.getTopic(), annot.getValue()));
                }
            }
            Collection<String> parent = new ArrayList<String>();
            if (publication.getAc() != null) {
                parent.add(publication.getAc());
            }
            getChangesController().markAsUnsaved((IntactExperiment) exp, getEditorService().getIntactDao().getSynchronizerContext().getExperimentSynchronizer(),
                    "Experiment: " + ((IntactExperiment) exp).getShortLabel(), parent);
        }

        addInfoMessage("Annotations copied", publication.getExperiments().size() + " experiments were modified");
    }

    public void copyPrimaryIdentifierToExperiments() {
        Collection<Experiment> experiments = publication.getExperiments();

        if (publication.getPubmedId() != null) {
            if (!experiments.isEmpty()) {
                Collection<String> parentAcs = new ArrayList<String>();
                if (publication.getAc() != null) {
                    parentAcs.add(publication.getAc());
                }
                for (Experiment experiment : experiments) {
                    Collection<Xref> primaryRefs = XrefUtils.collectAllXrefsHavingDatabaseAndQualifier(experiment.getXrefs(), Xref.PUBMED_MI,
                            Xref.PUBMED, Xref.PRIMARY_MI, Xref.PRIMARY);
                    Xref existingPrimary = primaryRefs.isEmpty() ? null : primaryRefs.iterator().next();
                    if (existingPrimary instanceof AbstractIntactXref){
                        ((AbstractIntactXref)existingPrimary).setId(publication.getPubmedId());
                    }
                    else{
                        experiment.getXrefs().removeAll(primaryRefs);
                        experiment.getXrefs().add(new ExperimentXref(IntactUtils.createMIDatabase(Xref.PUBMED, Xref.PUBMED_MI), publication.getPubmedId(),
                                IntactUtils.createMIQualifier(Xref.PRIMARY, Xref.PRIMARY_MI)));
                    }
                    getChangesController().markAsUnsaved((IntactExperiment) experiment, getEditorService().getIntactDao().getSynchronizerContext().getExperimentSynchronizer(),
                            "Experiment: " + ((IntactExperiment) experiment).getShortLabel(), parentAcs);
                }
            }
        }
    }

    public String getDatasetToAdd() {
        return datasetToAdd;
    }

    public void setDatasetToAdd(String datasetToAdd) {
        this.datasetToAdd = datasetToAdd;
    }

    public String[] getDatasetsToRemove() {
        return datasetsToRemove;
    }

    public void setDatasetsToRemove(String[] datasetsToRemove) {
        this.datasetsToRemove = datasetsToRemove;
    }

    public DatasetPopulator getDatasetPopulator() {
        if (this.datasetPopulator == null){
            this.datasetPopulator = ApplicationContextProvider.getBean("datasetPopulator");
        }
        return datasetPopulator;
    }

    public LazyDataModel<InteractionSummary> getInteractionDataModel() {
        return interactionDataModel;
    }

    public String getIdentifierToOpen() {
        return identifierToOpen;
    }

    public void setIdentifierToOpen(String identifierToOpen) {
        this.identifierToOpen = identifierToOpen;
    }

    public String getIdentifierToImport() {
        return identifierToImport;
    }

    public void setIdentifierToImport(String identifierToImport) {
        this.identifierToImport = identifierToImport;
    }

    @Override
    protected AnnotatedObjectController getParentController() {
        return null;
    }

    @Override
    protected String getPageContext() {
        return "publication";
    }

    @Override
    protected void postRevert() {
        loadFormFields();
    }

    public boolean isAssignToMe() {
        return assignToMe;
    }

    public void setAssignToMe(boolean assignToMe) {
        this.assignToMe = assignToMe;
    }

    public String getReasonForReadyForChecking() {
        return reasonForReadyForChecking;
    }

    public void setReasonForReadyForChecking(String reasonForReadyForChecking) {
        this.reasonForReadyForChecking = reasonForReadyForChecking;
    }

    public String getReasonForOnHoldFromDialog() {
        return reasonForOnHoldFromDialog;
    }

    public void setReasonForOnHoldFromDialog(String reasonForOnHoldFromDialog) {
        this.reasonForOnHoldFromDialog = reasonForOnHoldFromDialog;
    }

    public boolean isLifeCycleDisabled() {
        return isLifeCycleDisabled;
    }

    public void setLifeCycleDisabled(boolean lifeCycleDisabled) {
        isLifeCycleDisabled = lifeCycleDisabled;
    }

    public boolean isAssignedIMEx() {
        return getImexId() != null;
    }

    public boolean isAssignableIMEx() {
        return getImexId() == null && curationDepth != null
                && "IMEx".equalsIgnoreCase(curationDepth) && Pattern.matches(ImexCentralManager.PUBMED_REGEXP.toString(), publication.getPubmedId());
    }

    @Override
    public IntactDbSynchronizer getDbSynchronizer() {
        return getEditorService().getIntactDao().getSynchronizerContext().getPublicationSynchronizer();
    }

    @Override
    public String getObjectName() {
        return publication != null ? publication.getPubmedId() : null;
    }

    public String getNewDatasetDescriptionToCreate() {
        return newDatasetDescriptionToCreate;
    }

    public void setNewDatasetDescriptionToCreate(String newDatasetDescriptionToCreate) {
        this.newDatasetDescriptionToCreate = newDatasetDescriptionToCreate;
    }

    public String getNewDatasetNameToCreate() {
        return newDatasetNameToCreate;
    }

    public void setNewDatasetNameToCreate(String newDatasetNameToCreate) {
        this.newDatasetNameToCreate = newDatasetNameToCreate;
    }

    @Override
    protected EditorCloner<Publication, IntactPublication> newClonerInstance() {
        return null;
    }

    @Override
    public Collection<String> collectParentAcsOfCurrentAnnotatedObject() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Class<? extends IntactPrimaryObject> getAnnotatedObjectClass() {
        return IntactPublication.class;
    }

    @Override
    public boolean isAliasNotEditable(Alias alias) {
        return false;
    }

    @Override
    public boolean isAnnotationNotEditable(psidev.psi.mi.jami.model.Annotation annot) {
        if (AnnotationUtils.doesAnnotationHaveTopic(annot, null, Releasable.ON_HOLD)){
            return true;
        }
        else if (AnnotationUtils.doesAnnotationHaveTopic(annot, null, Releasable.TO_BE_REVIEWED)){
            return true;
        }
        else if (AnnotationUtils.doesAnnotationHaveTopic(annot, Annotation.CONTACT_EMAIL_MI, Annotation.CONTACT_EMAIL)){
            return true;
        }
        else if (AnnotationUtils.doesAnnotationHaveTopic(annot, null, Releasable.ACCEPTED)){
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public boolean isXrefNotEditable(Xref ref) {
        if (XrefUtils.isXrefFromDatabase(ref, Xref.PUBMED_MI, Xref.PUBMED)
                && XrefUtils.doesXrefHaveQualifier(ref, Xref.PRIMARY_MI, Xref.PRIMARY)){
            return true;
        }
        else{
            return false;
        }
    }

    public List<Annotation> collectAnnotations() {
        List<Annotation> annotations = new ArrayList<Annotation>(publication.getAnnotations());
        Collections.sort(annotations, new AuditableComparator());
        // annotations are always initialised
        return annotations;
    }

    @Override
    public void newAlias(ActionEvent evt) {
        // nothing to do
    }

    @Override
    protected void addNewAlias(AbstractIntactAlias newAlias) {
        // nothing to do
    }

    @Override
    public <T extends AbstractIntactAlias> T newAlias(CvTerm aliasType, String name) {
        return null;
    }

    @Override
    public <T extends AbstractIntactAlias> T newAlias(String alias, String aliasMI, String name) {
        return null;
    }

    @Override
    public void removeAlias(Alias alias) {
        // nothing to do
    }

    public List<Alias> collectAliases() {
        return Collections.EMPTY_LIST;
    }

    public List<Xref> collectXrefs() {
        List<Xref> xrefs = new ArrayList<Xref>(publication.getDbXrefs());
        Collections.sort(xrefs, new AuditableComparator());
        // annotations are always initialised
        return xrefs;
    }

    public List<ExperimentSummary> collectExperiments(){
        return experiments;
    }

    public void refreshExperiments() {
        this.experiments = new ArrayList<ExperimentSummary>(publication.getExperiments().size());
        for (Experiment exp : publication.getExperiments()){
            ExperimentSummary summary = getExperimentSummaryService().createSummaryFrom((IntactExperiment)exp);
            experiments.add(summary);
        }
    }

    @Override
    public void removeXref(Xref xref) {

        if (!this.publication.getIdentifiers().remove(xref)){
            this.publication.getXrefs().remove(xref);
        }
        this.identifier = this.publication.getPubmedId();
        this.imexId = this.publication.getImexId();
    }

    @Override
    protected void addNewAnnotation(AbstractIntactAnnotation newAnnot) {
        this.publication.getAnnotations().add(newAnnot);
    }

    @Override
    public PublicationAnnotation newAnnotation(CvTerm annotation, String text) {
        return new PublicationAnnotation(annotation, text);
    }

    @Override
    public PublicationAnnotation newAnnotation(String topic, String topicMI, String text) {
        return new PublicationAnnotation(getCvService().findCvObject(IntactUtils.TOPIC_OBJCLASS, topicMI != null ? topicMI: topic), text);
    }

    @Override
    public void removeAnnotation(psidev.psi.mi.jami.model.Annotation annotation) {
        publication.getAnnotations().remove(annotation);
    }

    public List<LifeCycleEvent> collectLifeCycleEvents() {
        return new ArrayList<LifeCycleEvent>(publication.getLifecycleEvents());
    }

    public int getExperimentsSize() {
        if (publication == null){
            return 0;
        }
        else {
            return publication.getExperiments().size();
        }
    }

    public void reloadSingleExperiment(IntactExperiment exp){
        Iterator<Experiment> evIterator = publication.getExperiments().iterator();
        boolean add = true;
        while (evIterator.hasNext()){
            IntactExperiment intactEv = (IntactExperiment)evIterator.next();
            if (intactEv.getAc() == null && exp == intactEv){
                add = false;
            }
            else if (intactEv.getAc() != null && intactEv.getAc().equals(exp.getAc())){
                evIterator.remove();
            }
        }
        if (add){
            publication.getExperiments().add(exp);
        }
        refreshExperiments();
        refreshDataModels();
    }

    public void removeExperiment(IntactExperiment exp){
        Iterator<Experiment> evIterator = publication.getExperiments().iterator();
        while (evIterator.hasNext()){
            IntactExperiment intactEv = (IntactExperiment)evIterator.next();
            if (intactEv.getAc() == null && exp == intactEv){
                evIterator.remove();
            }
            else if (intactEv.getAc() != null && intactEv.getAc().equals(exp.getAc())){
                evIterator.remove();
            }
        }
        refreshExperiments();
        refreshDataModels();
    }

    public ImexCentralManager getImexCentralManager() {
        if (this.imexCentralManager == null){
            this.imexCentralManager = ApplicationContextProvider.getBean("imexCentralManager");
        }
        return imexCentralManager;
    }

    public LifeCycleManager getLifecycleManager() {
        if (this.lifecycleManager == null){
            this.lifecycleManager = ApplicationContextProvider.getBean("jamiLifeCycleManager");
        }
        return lifecycleManager;
    }

    public boolean isInteractionTabDisabled() {
        return isInteractionTabDisabled;
    }

    public void setInteractionTabDisabled(boolean isInteractionTabDisabled) {
        this.isInteractionTabDisabled = isInteractionTabDisabled;
    }

    public boolean isExperimentTabDisabled() {
        return isExperimentTabDisabled;
    }

    public void setExperimentTabDisabled(boolean isExperimentTabDisabled) {
        this.isExperimentTabDisabled = isExperimentTabDisabled;
    }

    public ExperimentSummaryService getExperimentSummaryService() {
        if (this.experimentSummaryService == null){
            this.experimentSummaryService = ApplicationContextProvider.getBean("experimentSummaryService");
        }
        return experimentSummaryService;
    }

    public InteractionSummaryService getInteractionSummaryService() {
        if (this.interactionSummaryService == null){
            this.interactionSummaryService = ApplicationContextProvider.getBean("interactionSummaryService");
        }
        return interactionSummaryService;
    }

    public List<SelectItem> getDatasetsSelectItems(){
        return datasetsSelectItems;
    }

    @Override
    protected void postProcessDeletedEvent(UnsavedChange unsaved) {
        super.postProcessDeletedEvent(unsaved);
        if (unsaved.getUnsavedObject() instanceof IntactExperiment){
            removeExperiment((IntactExperiment)unsaved.getUnsavedObject());
        }
        else if (unsaved.getUnsavedObject() instanceof IntactInteractionEvidence){
            refreshExperiments();
            refreshDataModels();
        }
    }
}
