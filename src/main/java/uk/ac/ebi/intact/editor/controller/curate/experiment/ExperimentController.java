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
package uk.ac.ebi.intact.editor.controller.curate.experiment;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.primefaces.context.RequestContext;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.model.LazyDataModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import uk.ac.ebi.intact.editor.controller.UserSessionController;
import uk.ac.ebi.intact.editor.controller.admin.UserManagerController;
import uk.ac.ebi.intact.editor.controller.curate.AnnotatedObjectController;
import uk.ac.ebi.intact.editor.controller.curate.UnsavedChange;
import uk.ac.ebi.intact.editor.controller.curate.cloner.EditorCloner;
import uk.ac.ebi.intact.editor.controller.curate.cloner.ExperimentCloner;
import uk.ac.ebi.intact.editor.controller.curate.publication.PublicationController;
import uk.ac.ebi.intact.editor.services.curate.experiment.ExperimentEditorService;
import uk.ac.ebi.intact.editor.services.curate.organism.BioSourceService;
import uk.ac.ebi.intact.editor.services.summary.InteractionSummary;
import uk.ac.ebi.intact.editor.services.summary.InteractionSummaryService;
import uk.ac.ebi.intact.editor.util.LazyDataModelFactory;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.lifecycle.IllegalTransitionException;
import uk.ac.ebi.intact.jami.model.IntactPrimaryObject;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.model.lifecycle.LifeCycleStatus;
import uk.ac.ebi.intact.jami.model.lifecycle.Releasable;
import uk.ac.ebi.intact.jami.service.PublicationService;
import uk.ac.ebi.intact.jami.synchronizer.IntactDbSynchronizer;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ValueChangeEvent;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope( "conversation.access" )
@ConversationName( "general" )
public class ExperimentController extends AnnotatedObjectController {

    private IntactExperiment experiment;
    private String ac;
    private LazyDataModel<InteractionSummary> interactionDataModel;

    private String reasonForRejection;
    private String correctedComment;
    private String accepted;

    private String publicationToMoveTo;

    @Autowired
    private PublicationController publicationController;

    @Autowired
    private UserSessionController userSessionController;

    @Resource(name = "experimentEditorService")
    private transient uk.ac.ebi.intact.editor.services.curate.experiment.ExperimentEditorService experimentService;

    @Resource(name = "publicationService")
    private transient PublicationService publicationService;

    @Resource(name = "bioSourceService")
    private transient BioSourceService biosourceService;

    @Resource(name = "interactionSummaryService")
    private transient InteractionSummaryService interactionSummaryService;

    private boolean isInteractionTab = true;
    private boolean isVariableParameterTab = false;

    private String newParameterDescription;
    private CvTerm newParameterUnit;

    private String newValue;
    private Integer newValueOrder;

    public ExperimentController() {

    }

    @Override
    public IntactPrimaryObject getAnnotatedObject() {
        return getExperiment();
    }

    @Override
    public void setAnnotatedObject(IntactPrimaryObject annotatedObject) {
        setExperiment((IntactExperiment) annotatedObject);
    }

    @Override
    protected AnnotatedObjectController getParentController() {
        return publicationController;
    }

    @Override
    protected String getPageContext() {
        return "publication";
    }

    @Override
    protected void loadCautionMessages() {
        if (this.experiment != null){

            Annotation caution = AnnotationUtils.collectFirstAnnotationWithTopic(this.experiment.getAnnotations(), Annotation.CAUTION_MI, Annotation.CAUTION);
            setCautionMessage(caution != null ? caution.getValue() : null);
            Annotation internal = AnnotationUtils.collectFirstAnnotationWithTopic(this.experiment.getAnnotations(), null, "remark-internal");
            setInternalRemark(internal != null ? internal.getValue() : null);
            Annotation correctionComment = AnnotationUtils.collectFirstAnnotationWithTopic(this.experiment.getAnnotations(),
                    null, Releasable.CORRECTION_COMMENT);
            this.correctedComment = correctionComment != null ? correctionComment.getValue() : null;
            Annotation toBeReviewed = AnnotationUtils.collectFirstAnnotationWithTopic(this.experiment.getAnnotations(),
                    null, Releasable.TO_BE_REVIEWED);
            this.reasonForRejection = toBeReviewed != null ? toBeReviewed.getValue() : null;
            Annotation accepted = AnnotationUtils.collectFirstAnnotationWithTopic(this.experiment.getAnnotations(),
                    null, Releasable.ACCEPTED);
            this.accepted = accepted != null ? accepted.getValue() : null;
        }
    }

    public void loadData( ComponentSystemEvent event ) {
        if (!FacesContext.getCurrentInstance().isPostback()) {
            if ( ac != null ) {
                if ( experiment == null || !ac.equals( experiment.getAc() ) ) {
                    setExperiment(getExperimentService().loadExperimentByAc(ac));
                }
            } else if ( experiment != null ) {
                ac = experiment.getAc();
            }

            if (experiment == null) {
                addErrorMessage("No Experiment with this AC", ac);
                return;
            }

            // load parent if not done yet
            refreshParentControllers();

            refreshTabs();
        }

        generalLoadChecks();
    }

    @Override
    public void refreshTabs() {
        super.refreshTabs();
        isInteractionTab = true;
        isVariableParameterTab = false;
    }

    @Override
    public void onTabChanged(TabChangeEvent e) {

        // the xref tab is active
        super.onTabChanged(e);

        // all the tabs selectOneMenu are disabled, we can process the tabs specific to experiment
        if (isAliasDisabled() && isXrefDisabled() && isAnnotationTopicDisabled()){
            if (e.getTab().getId().equals("interactionsTab")){
                isInteractionTab = true;
                isVariableParameterTab = false;
            }
            else if (e.getTab().getId().equals("vParametersTab")){
                isVariableParameterTab = true;
                isInteractionTab = false;
            }
            else {
                isInteractionTab = false;
                isVariableParameterTab = false;
            }
        }
        else {
            isInteractionTab = false;
            isVariableParameterTab = false;
        }
    }

    @Override
    protected void generalLoadChecks() {
        super.generalLoadChecks();
        super.generalPublicationLoadChecks();
    }

    protected void refreshParentControllers() {
        // different loaded publication
        if (publicationController.getPublication() != experiment.getPublication()){
            // different publication to load
            if (publicationController.getAc() == null ||
                    (experiment.getPublication() instanceof IntactPublication
                            && !publicationController.getAc().equals(((IntactPublication)experiment.getPublication()).getAc()))){
                publicationController.setPublication((IntactPublication)experiment.getPublication());
            }
            // replace old experiment instance with new one in experiment tables of publication
            else{
                experiment.setPublication(publicationController.getPublication());
                publicationController.reloadSingleExperiment(experiment);
            }
        }
    }

    public void reloadSingleInteractionEvidence(IntactInteractionEvidence ev){
        // only update if not lazy loaded
        if (experiment.areInteractionEvidencesInitialized()){
            Iterator<InteractionEvidence> evIterator = experiment.getInteractionEvidences().iterator();
            boolean add = true;
            while (evIterator.hasNext()){
                IntactInteractionEvidence intactEv = (IntactInteractionEvidence)evIterator.next();
                if (intactEv.getAc() == null && ev == intactEv){
                    add = false;
                }
                else if (intactEv.getAc() != null && intactEv.getAc().equals(ev.getAc())){
                    evIterator.remove();
                }
            }
            if (add){
                experiment.getInteractionEvidences().add(ev);
            }
        }
        else{
            refreshInteractions();
        }

        publicationController.reloadSingleExperiment(experiment);
    }

    public void removeInteractionEvidence(IntactInteractionEvidence ev){
        // only update if not lazy loaded
        if (experiment.areInteractionEvidencesInitialized()){
            Iterator<InteractionEvidence> evIterator = experiment.getInteractionEvidences().iterator();
            while (evIterator.hasNext()){
                IntactInteractionEvidence intactEv = (IntactInteractionEvidence)evIterator.next();
                if (intactEv.getAc() == null && ev == intactEv){
                    evIterator.remove();
                }
                else if (intactEv.getAc() != null && intactEv.getAc().equals(ev.getAc())){
                    evIterator.remove();
                }
            }
        }
        else{
            refreshInteractions();
        }

        publicationController.reloadSingleExperiment(experiment);
    }

    @Override
    protected EditorCloner<Experiment, IntactExperiment> newClonerInstance() {
        return new ExperimentCloner(false);
    }


    @Override
    protected void addNewXref(AbstractIntactXref newRef) {
        this.experiment.getXrefs().add(newRef);
    }

    @Override
    protected ExperimentXref newXref(CvTerm db, String id, String secondaryId, String version, CvTerm qualifier) {
        ExperimentXref ref = new ExperimentXref(db, id, version, qualifier);
        ref.setSecondaryId(secondaryId);
        return ref;
    }

    @Override
    public ExperimentXref newXref(String db, String dbMI, String id, String secondaryId, String qualifier, String qualifierMI) {
        return new ExperimentXref(getCvService().findCvObject(IntactUtils.DATABASE_OBJCLASS, dbMI != null ? dbMI : db),
                id, secondaryId, getCvService().findCvObject(IntactUtils.QUALIFIER_OBJCLASS, qualifierMI != null ? qualifierMI : qualifier));
    }

    public String cloneWithInteractions() {

        String value = clone(experiment, new ExperimentCloner(true));

        return value;
    }

    private void refreshInteractions() {
        if (experiment == null) return;

        if (experiment.areInteractionEvidencesInitialized()){
            List<InteractionSummary> evidences = new ArrayList<InteractionSummary>(experiment.getInteractionEvidences().size());
            for (InteractionEvidence ev : experiment.getInteractionEvidences()){
                evidences.add(getInteractionSummaryService().createSummaryFrom((IntactInteractionEvidence)ev));
            }
            interactionDataModel = LazyDataModelFactory.createLazyDataModel(evidences);
        }
        else{
            interactionDataModel = LazyDataModelFactory.createLazyDataModel(getInteractionSummaryService(), "interactionSummaryService",
                    "select distinct i from IntactInteractionEvidence i join i.dbExperiments as exp where exp.ac = '" + experiment.getAc() + "'",
                    "select count(distinct i.ac) from IntactInteractionEvidence i join i.dbExperiments as exp where exp.ac = '" + experiment.getAc() + "'",
                    "i", "i.ac", true);
        }
    }

    @Override
    protected void initialiseDefaultProperties(IntactPrimaryObject annotatedObject) {
        IntactExperiment experiment = (IntactExperiment)annotatedObject;
        if (!getExperimentService().isExperimentFullyLoaded(experiment)){
            this.experiment = getExperimentService().reloadFullyInitialisedExperiment(experiment);
        }

        refreshInteractions();

        setDescription("Experiment: "+experiment.getShortLabel());
    }

    @Override
    public void doPostSave(){
        // new object, add it to the list of experiments of its publication before saving
        if (experiment.getPublication() != null) {
            publicationController.reloadSingleExperiment(experiment);
        }
    }


    /**
     * When reverting, we need to refresh the collection of wrappers because they are not part of the IntAct model.
     */
    @Override
    protected void postRevert() {
        // new object, add it to the list of experiments of its publication before saving
        if (experiment.getPublication() != null) {
            publicationController.reloadSingleExperiment(experiment);
        }
    }

    @Override
    public String doDelete() {
        publicationController.removeExperiment(experiment);
        return super.doDelete();
    }

    public String newExperiment(Publication publication) {
        IntactExperiment experiment = new IntactExperiment(publication);
        experiment.setShortLabel(IntactUtils.generateAutomaticExperimentShortlabelFor(experiment, IntactUtils.MAX_SHORT_LABEL_LEN));
        // synchronize with db
        getEditorService().synchronizeExperimentShortLabel(experiment);
        if (publicationController.getPublication() != publication){
            publicationController.setPublication((IntactPublication)publication);
        }

        if (publication.getPubmedId() != null) {
            CvTerm pubmed = getCvService().findCvObjectByIdentifier(IntactUtils.DATABASE_OBJCLASS, Xref.PUBMED_MI);
            CvTerm primaryRef = getCvService().findCvObjectByIdentifier(IntactUtils.QUALIFIER_OBJCLASS, Xref.PRIMARY_MI);

            experiment.getXrefs().add(new ExperimentXref(pubmed, publication.getPubmedId(), primaryRef));
        }

        setExperiment(experiment);

        copyPublicationAnnotations(null);

        return navigateToObject(experiment);
    }

    public void acceptExperiment(ActionEvent actionEvent) {

        UserSessionController userSessionController = (UserSessionController) getSpringContext().getBean("userSessionController");
        this.accepted = "Accepted "+new SimpleDateFormat("yyyy-MMM-dd").format(new Date()).toUpperCase()
                +" by "+userSessionController.getCurrentUser().getLogin().toUpperCase();

        updateAnnotation(Releasable.ACCEPTED, null,accepted, experiment.getAnnotations());

        // remove other annotations
        removeAnnotation(Releasable.TO_BE_REVIEWED, null, experiment.getAnnotations());
        removeAnnotation(Releasable.CORRECTION_COMMENT, null, experiment.getAnnotations());
        removeAnnotation(Releasable.ON_HOLD, null, experiment.getAnnotations());

        doSave(actionEvent);

        addInfoMessage("Experiment accepted", experiment.getShortLabel());

        // check if all the experiments have been acted upon, be it to accept them or reject them.
        globalPublicationDecision();
    }

    public void revertAccepted(ActionEvent evt) {
        this.accepted = null;
        removeAnnotation(Releasable.ACCEPTED, null, experiment.getAnnotations());
        doSave(evt);

        addInfoMessage("Experiment accepted annotation has been removed, publication reverted as well", experiment.getShortLabel());

        // only if publication ready for release
        if (publicationController.isReadyForRelease()){
            try{
                getPublicationService().putReleasableOnHoldFromReadyForRelease(publicationController.getAc(),
                        "Reverted accepted annotation of experiment " + experiment.getShortLabel(), ((UserManagerController) ApplicationContextProvider.getBean("userManagerController")).getCurrentUser().getLogin());
                // refresh publication
                setExperiment(getExperimentService().reloadFullyInitialisedExperiment(experiment));
                publicationController.setPublication((IntactPublication) experiment.getPublication());
            }
            catch (IllegalTransitionException e){
                addErrorMessage("Cannot put publication on-hold: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
            }
        }
        // also if released, revert from release
        else if (publicationController.isReleased()){
            try{
                getPublicationService().moveReleasableFromReleasedToOnHold(publicationController.getAc(),
                        "Reverted accepted annotation of experiment " + experiment.getShortLabel(), ((UserManagerController) ApplicationContextProvider.getBean("userManagerController")).getCurrentUser().getLogin());
                // refresh publication
                setExperiment(getExperimentService().reloadFullyInitialisedExperiment(experiment));
                publicationController.setPublication((IntactPublication)experiment.getPublication());
            }
            catch (IllegalTransitionException e){
                addErrorMessage("Cannot put publication on-hold: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
            }
        }
    }

    public void rejectExperiment(ActionEvent actionEvent) {

        UserSessionController userSessionController = (UserSessionController) getSpringContext().getBean("userSessionController");
        if (reasonForRejection != null && reasonForRejection.startsWith("Rejected")) {
            reasonForRejection = reasonForRejection.substring(reasonForRejection.indexOf(".")+2);
        }
        String date = "Rejected " +new SimpleDateFormat("yyyy-MMM-dd").format(new Date()).toUpperCase()+
                " by "+userSessionController.getCurrentUser().getLogin().toUpperCase();

        setToBeReviewed(date+". "+reasonForRejection);
        this.accepted = null;

        updateAnnotation(Releasable.TO_BE_REVIEWED, null, date+". "+reasonForRejection, experiment.getAnnotations());
        removeAnnotation(Releasable.ACCEPTED, null, experiment.getAnnotations());
        removeAnnotation(Releasable.CORRECTION_COMMENT, null, experiment.getAnnotations());

        doSave(actionEvent);

        addInfoMessage("Experiment rejected", experiment.getShortLabel()+": "+reasonForRejection);

        globalPublicationDecision();
    }

    private void globalPublicationDecision() {
        int expAccepted = getExperimentService().countAcceptedExperiments(publicationController.getAc());
        int expRejected = getExperimentService().countRejectedExperiments(publicationController.getAc());
        int expSize = publicationController.getExperimentsSize();

        boolean allActedUpon = ((expRejected+expAccepted) == expSize);
        boolean allAccepted = expAccepted == expSize;

        if (allAccepted) {
            try{
                // accept publication
                getPublicationService().acceptReleasable(publicationController.getAc(), "Accepted " + new SimpleDateFormat("yyyy-MMM-dd").
                                format(new Date()).toUpperCase() + " by " + userSessionController.getCurrentUser().getLogin().toUpperCase(),
                        userSessionController.getCurrentUser().getLogin());

                // ready for relase publication if not already on-hold
                if (((Releasable)experiment.getPublication()).getStatus() != LifeCycleStatus.ACCEPTED_ON_HOLD) {
                    getPublicationService().readyForRelease(publicationController.getAc(), "Accepted and not on-hold",
                            userSessionController.getCurrentUser().getLogin());
                }
                addInfoMessage("Publication accepted", "All of its experiments have been accepted");

                // refresh publication
                setExperiment(getExperimentService().reloadFullyInitialisedExperiment(experiment));
                publicationController.setPublication((IntactPublication)experiment.getPublication());

                // refresh experiments with possible changes in publication title, annotations and publication identifier
                publicationController.copyAnnotationsToExperiments(null);
                publicationController.copyPrimaryIdentifierToExperiments();
            }
            catch (IllegalTransitionException e){
                addErrorMessage("Cannot accept publication: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
            }

        } else if (allActedUpon) {
            RequestContext requestContext = RequestContext.getCurrentInstance();
            requestContext.execute("publicationActionDlg.show()");
        }
    }

    public void addCorrectionComment(ActionEvent evt) {
        addInfoMessage("Added correction comment", correctedComment);
        // annotations are always loaded
        updateAnnotation(Releasable.CORRECTION_COMMENT, null, correctedComment, experiment.getAnnotations());
    }


    public void onToBeReviewedChanged(ValueChangeEvent evt) {
        String newValue = (String) evt.getNewValue();
        if (newValue != null && newValue.length() > 0){
            updateAnnotation(Releasable.TO_BE_REVIEWED, null, newValue, experiment.getAnnotations());
            setUnsavedChanges(true);
        }
        else{
            removeAnnotation(Releasable.TO_BE_REVIEWED, null, experiment.getAnnotations());
            setUnsavedChanges(true);
        }
        this.reasonForRejection = newValue;
    }

    public void onCorrectionCommentChanged(ValueChangeEvent evt) {
        String newValue = (String) evt.getNewValue();
        if (newValue != null && newValue.length() > 0){
            updateAnnotation(Releasable.CORRECTION_COMMENT, null, newValue, experiment.getAnnotations());
            setUnsavedChanges(true);
        }
        else{
            removeAnnotation(Releasable.CORRECTION_COMMENT, null, experiment.getAnnotations());
            setUnsavedChanges(true);
        }
        this.correctedComment = newValue;
    }

    public void removeToBeReviewed(ActionEvent evt){
        addInfoMessage("Removed to-be-reviewed annotation", reasonForRejection);
        // annotations are always loaded
        removeAnnotation(Releasable.TO_BE_REVIEWED, null, experiment.getAnnotations());
    }

    public void removeCorrectionComment(ActionEvent evt){
        addInfoMessage("Removed correction annotation", correctedComment);
        // annotations are always loaded
        removeAnnotation(Releasable.CORRECTION_COMMENT, null, experiment.getAnnotations());
    }

    public void setToBeReviewed(String toBeReviewed) {
        this.reasonForRejection = toBeReviewed;
    }

    public String getToBeReviewed() {
        return this.reasonForRejection;
    }

    public boolean isAccepted() {
        return accepted != null;
    }

    public boolean isRejected() {
        return reasonForRejection != null;
    }

    public boolean isCorrected() {
        return correctedComment != null;
    }

    public String getAcceptedMessage() {
        return accepted;
    }

    public void copyPublicationAnnotations(ActionEvent evt) {
        if (experiment.getPublication() instanceof IntactPublication){
            for (Annotation annot : ((IntactPublication)experiment.getPublication()).getDbAnnotations()){
                Annotation existingAnnot = AnnotationUtils.collectFirstAnnotationWithTopic(experiment.getAnnotations(),
                        annot.getTopic().getMIIdentifier(), annot.getTopic().getShortName());
                if (existingAnnot != null){
                    existingAnnot.setValue(annot.getValue());
                }
                else{
                    experiment.getAnnotations().add(new ExperimentAnnotation(annot.getTopic(), annot.getValue()));
                }
            }

            addInfoMessage("Annotations copied from publication", "");
            setUnsavedChanges(true);
        }
    }

    public String moveToPublication() {
        if (publicationToMoveTo != null && !publicationToMoveTo.isEmpty()) {
            IntactPublication publication = getExperimentService().loadPublicationByAcOrPubmedId(publicationToMoveTo);

            if (publication == null) {
                addErrorMessage("Cannot move", "No publication found with this AC or PMID: "+publicationToMoveTo);
                return null;
            }

            // set publication of publication controller
            publicationController.setPublication(publication);

            // don't remove the experiment from the parent publication yet so the revert will work properly. It will be added only after saving
            // As an experiment can have only one publication, it will be removed from the previous publication
            experiment.setPublication(publicationController.getPublication());
            experiment.setShortLabel(IntactUtils.generateAutomaticExperimentShortlabelFor(experiment, IntactUtils.MAX_SHORT_LABEL_LEN));
            getEditorService().synchronizeExperimentShortLabel(experiment);

            setExperiment(experiment);

            copyPublicationAnnotations(null);

            // update the primary reference when moving the experiment
            if (publicationController.getPublication().getPubmedId() != null) {
                updateXref(Xref.PUBMED, Xref.PUBMED_MI, publicationController.getPublication().getPubmedId(), Xref.PRIMARY, Xref.PRIMARY_MI, experiment.getXrefs());
            }
            else{
                removeXref(Xref.PUBMED, Xref.PUBMED_MI, Xref.PRIMARY, Xref.PRIMARY_MI, experiment.getXrefs());
            }

            // update the imex reference when moving the experiment
            if (publicationController.getPublication().getImexId() != null) {
                updateXref(Xref.IMEX, Xref.IMEX_MI,  publicationController.getPublication().getImexId(), Xref.IMEX_PRIMARY, Xref.IMEX_PRIMARY_MI, experiment.getXrefs());
            }
            else{
                removeXref(Xref.IMEX, Xref.IMEX_MI, Xref.IMEX, Xref.IMEX_PRIMARY_MI, experiment.getXrefs());
            }

        } else {
            return null;
        }

        refreshParentControllers();
        setUnsavedChanges(true);

        addInfoMessage("Moved experiment", "To publication: "+publicationToMoveTo);

        return null;
    }

    public String getAc() {
        return ac;
    }

    @Override
    public int getXrefsSize() {
        if (experiment == null){
            return 0;
        }
        else {
            return experiment.getXrefs().size();
        }
    }

    public int getVariableParametersSize() {
        if (experiment == null){
            return 0;
        }
        else{
            return experiment.getVariableParameters().size();
        }
    }

    @Override
    public int getAliasesSize() {
        return 0;
    }

    @Override
    public int getAnnotationsSize() {
        if (experiment == null){
            return 0;
        }
        else {
            return experiment.getAnnotations().size();
        }
    }

    public void setAc( String ac ) {
        this.ac = ac;
    }

    public IntactExperiment getExperiment() {
        return experiment;
    }

    public void setExperiment( IntactExperiment experiment ) {
        this.experiment = experiment;

        if (experiment != null) {
            this.ac = experiment.getAc();

            initialiseDefaultProperties(this.experiment);
        }
        else{
            this.ac = null;
        }
    }

    public LazyDataModel<InteractionSummary> getInteractionDataModel() {
        return interactionDataModel;
    }

    public String getReasonForRejection() {
        return reasonForRejection;
    }

    public void setReasonForRejection(String reasonForRejection) {
        this.reasonForRejection = reasonForRejection;
    }

    public String getPublicationToMoveTo() {
        return publicationToMoveTo;
    }

    public void setPublicationToMoveTo(String publicationToMoveTo) {
        this.publicationToMoveTo = publicationToMoveTo;
    }

    @Override
    public Collection<String> collectParentAcsOfCurrentAnnotatedObject(){
        Collection<String> parentAcs = new ArrayList<String>(1);

        addPublicationAcToParentAcs(parentAcs, experiment);

        return parentAcs;
    }

    @Override
    public Class<? extends IntactPrimaryObject> getAnnotatedObjectClass() {
        return IntactExperiment.class;
    }

    @Override
    public boolean isAliasNotEditable(Alias alias) {
        return false;
    }

    @Override
    public boolean isAnnotationNotEditable(Annotation annot) {
        if (AnnotationUtils.doesAnnotationHaveTopic(annot, null, Releasable.TO_BE_REVIEWED)){
            return true;
        }
        else if (AnnotationUtils.doesAnnotationHaveTopic(annot, null, Releasable.CORRECTION_COMMENT)){
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
        return false;
    }

    @Override
    public IntactDbSynchronizer getDbSynchronizer() {
        return getEditorService().getIntactDao().getSynchronizerContext().getExperimentSynchronizer();
    }

    @Override
    public String getObjectName() {
        return this.experiment != null ? this.experiment.getShortLabel() : null;
    }

    public String getCorrectionComment() {
        return correctedComment;
    }

    public void setCorrectionComment(String correctionComment) {
        this.correctedComment = correctionComment;
    }

    public List<Annotation> collectAnnotations() {
        List<Annotation> annotations = new ArrayList<Annotation>(experiment.getAnnotations());
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
        // xrefs are not always initialise

        List<Xref> xrefs = new ArrayList<Xref>(this.experiment.getXrefs());
        Collections.sort(xrefs, new AuditableComparator());
        return xrefs;
    }

    public List<VariableParameter> collectVariableParameters() {
        if (experiment == null){
            return Collections.EMPTY_LIST;
        }
        List<VariableParameter> variableParameters = new ArrayList<VariableParameter>(this.experiment.getVariableParameters());
        Collections.sort(variableParameters, new AuditableComparator());
        return variableParameters;
    }

    public void newVariableParameter(ActionEvent evt){
        if (this.newParameterDescription != null){
            experiment.addVariableParameter(new IntactVariableParameter(this.newParameterDescription, this.newParameterUnit));
            doSave(false);

            this.newParameterDescription = null;
            this.newParameterUnit = null;
        }
        else{
            addErrorMessage("The variable parameter description cannot be null","Missing parameter description");
        }
    }

    public void newVariableParameterValue(VariableParameter param){
        if (this.newValue != null){
            param.getVariableValues().add(new IntactVariableParameterValue(newValue, param, newValueOrder));
            doSave(false);

            this.newValue = null;
            this.newValueOrder = null;
        }
        else{
            addErrorMessage("The value is required and cannot be null","Missing parameter value");
        }
    }

    public void removeVariableParameter(VariableParameter param){
        experiment.removeVariableParameter(param);
    }

    public void removeVariableParameterValue(IntactVariableParameterValue value, IntactVariableParameter param){
        param.getVariableValues().remove(value);
    }

    @Override
    public void removeXref(Xref xref) {
        experiment.getXrefs().remove(xref);
    }

    @Override
    protected void addNewAnnotation(AbstractIntactAnnotation newAnnot) {
        experiment.getAnnotations().add(newAnnot);
    }

    @Override
    public ExperimentAnnotation newAnnotation(CvTerm annotation, String text) {
        return new ExperimentAnnotation(annotation, text);
    }

    @Override
    public ExperimentAnnotation newAnnotation(String topic, String topicMI, String text) {
        return new ExperimentAnnotation(getCvService().findCvObject(IntactUtils.TOPIC_OBJCLASS, topicMI != null ? topicMI: topic), text);
    }

    @Override
    public void removeAnnotation(Annotation annotation) {
        experiment.getAnnotations().remove(annotation);
    }

    public ExperimentEditorService getExperimentService() {
        if (this.experimentService == null){
            this.experimentService = ApplicationContextProvider.getBean("experimentEditorService");
        }
        return experimentService;
    }

    public PublicationService getPublicationService() {
        if (this.publicationService == null){
            this.publicationService = ApplicationContextProvider.getBean("publicationService");
        }
        return publicationService;
    }

    public BioSourceService getBiosourceService() {
        if (this.biosourceService == null){
            this.biosourceService = ApplicationContextProvider.getBean("bioSourceService");
        }
        return biosourceService;
    }

    public InteractionSummaryService getInteractionSummaryService() {
        if (this.interactionSummaryService == null){
            this.interactionSummaryService = ApplicationContextProvider.getBean("interactionSummaryService");
        }
        return interactionSummaryService;
    }

    public boolean isVariableParameterTab() {
        return isVariableParameterTab;
    }

    public boolean isInteractionTab() {
        return isInteractionTab;
    }

    @Override
    protected void postProcessDeletedEvent(UnsavedChange unsaved) {
        super.postProcessDeletedEvent(unsaved);
        if (unsaved.getUnsavedObject() instanceof IntactInteractionEvidence){
            removeInteractionEvidence((IntactInteractionEvidence)unsaved.getUnsavedObject());
        }
    }

    public String getNewParameterDescription() {
        return newParameterDescription;
    }

    public void setNewParameterDescription(String newParameterDescription) {
        this.newParameterDescription = newParameterDescription;
    }

    public CvTerm getNewParameterUnit() {
        return newParameterUnit;
    }

    public void setNewParameterUnit(CvTerm newParameterUnit) {
        this.newParameterUnit = newParameterUnit;
    }

    public Integer getNewValueOrder() {
        return newValueOrder;
    }

    public void setNewValueOrder(Integer newValueOrder) {
        this.newValueOrder = newValueOrder;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public void markExperimentToDelete(IntactExperiment exp){
        Collection<String> parentAcs = new ArrayList<String>(1);
        addPublicationAcToParentAcs(parentAcs, exp);
        getChangesController().markToDelete(exp, (IntactPublication)exp.getPublication(),
                getDbSynchronizer(), exp.getShortLabel(), parentAcs);
    }
}
