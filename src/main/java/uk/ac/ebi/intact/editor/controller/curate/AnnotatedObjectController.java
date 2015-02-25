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
package uk.ac.ebi.intact.editor.controller.curate;

import org.apache.commons.lang.StringUtils;
import org.primefaces.event.TabChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import psidev.psi.mi.jami.bridges.exception.BridgeFailedException;
import psidev.psi.mi.jami.bridges.fetcher.OntologyTermFetcher;
import psidev.psi.mi.jami.bridges.ols.CachedOlsOntologyTermFetcher;
import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.utils.AliasUtils;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import psidev.psi.mi.jami.utils.XrefUtils;
import uk.ac.ebi.intact.editor.controller.BaseController;
import uk.ac.ebi.intact.editor.controller.curate.cloner.EditorCloner;
import uk.ac.ebi.intact.editor.controller.curate.complex.ComplexController;
import uk.ac.ebi.intact.editor.controller.curate.publication.PublicationController;
import uk.ac.ebi.intact.editor.services.curate.EditorObjectService;
import uk.ac.ebi.intact.editor.services.curate.cvobject.CvObjectService;
import uk.ac.ebi.intact.editor.services.curate.experiment.ExperimentEditorService;
import uk.ac.ebi.intact.editor.services.curate.feature.FeatureEditorService;
import uk.ac.ebi.intact.editor.services.curate.institution.InstitutionService;
import uk.ac.ebi.intact.editor.services.curate.interaction.InteractionEditorService;
import uk.ac.ebi.intact.editor.services.curate.interactor.InteractorEditorService;
import uk.ac.ebi.intact.editor.services.curate.participant.ParticipantEditorService;
import uk.ac.ebi.intact.editor.services.curate.publication.PublicationEditorService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.IntactPrimaryObject;
import uk.ac.ebi.intact.jami.model.audit.Auditable;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.model.lifecycle.LifeCycleEvent;
import uk.ac.ebi.intact.jami.model.lifecycle.LifeCycleEventType;
import uk.ac.ebi.intact.jami.model.lifecycle.Releasable;
import uk.ac.ebi.intact.jami.synchronizer.IntactDbSynchronizer;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.annotation.Resource;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.validator.ValidatorException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public abstract class AnnotatedObjectController extends BaseController implements ValueChangeAware {

    private final Logger log = LoggerFactory.getLogger(AnnotatedObjectController.class);

    private Date lastSaved;

    @Autowired
    private CuratorContextController curatorContextController;

    @Autowired
    private CurateController curateController;

    @Autowired
    private ChangesController changesController;

    @Resource(name = "editorObjectService")
    private transient EditorObjectService editorService;

    @Resource(name = "cvObjectService")
    private transient CvObjectService cvService;

    private boolean isAnnotationTopicDisabled;
    private boolean isXrefDisabled;
    private boolean isAliasDisabled;

    public static final String PROCESS = "process";
    public static final String PROCESS_MI_REF = "MI:0359";
    public static final String COMPONENT = "component";
    public static final String COMPONENT_MI_REF = "MI:0354";
    public static final String FUNCTION = "function";
    public static final String FUNCTION_MI_REF = "MI:0355";
    public static final String NON_UNIPROT = "no-uniprot-update";

    private transient OntologyTermFetcher goServerProxy;

    private String cautionMessage;
    private String internalRemark;
    private String description;

    private CvTerm newDatabase;
    private String newXrefId;
    private String newSecondaryId;
    private String newXrefVersion;
    private CvTerm newQualifier;

    private CvTerm newAliasType;
    private String newAliasName;

    private CvTerm newTopic;
    private String newAnnotationDescription;

    public AnnotatedObjectController() {
    }

    public abstract IntactPrimaryObject getAnnotatedObject();

    public abstract void setAnnotatedObject(IntactPrimaryObject annotatedObject);

    public void refreshTabsAndFocusXref() {
        isXrefDisabled = false;
        isAliasDisabled = true;
        isAnnotationTopicDisabled = true;
    }

    public void refreshTabs() {
        isXrefDisabled = true;
        isAliasDisabled = true;
        isAnnotationTopicDisabled = true;
    }

    public String goToParent() {
        AnnotatedObjectController parentController = getParentController();
        if (parentController == null || parentController.getAnnotatedObject() == null) {
            return "/curate/curate?faces-redirect=true";
        }

        return "/curate/"+getParentController().getPageContext()+"?faces-redirect=true&includeViewParams=true";
    }

    protected abstract AnnotatedObjectController getParentController();

    protected abstract String getPageContext();

    protected void generalLoadChecks(){
        if (getAnnotatedObject() != null){
            // set current user
            getEditorService().setUser(getCurrentUser());

            if (changesController.isObjectBeingEdited(getAnnotatedObject(), false)) {
                String who = changesController.whoIsEditingObject(getAnnotatedObject());

                addWarningMessage("This object is already being edited by: " + who, "Modifications may be lost or affect current work by the other curator");
            }

            loadCautionMessages();
        }
    }

    protected abstract void loadCautionMessages();

    protected void generalComplexLoadChecks() {

        ComplexController complexController = (ComplexController) getSpringContext().getBean("complexController");

        if (complexController.getComplex() != null) {
            IntactComplex complex = complexController.getComplex();

            switch (complex.getStatus()){
                case CURATION_IN_PROGRESS:
                    if (!getUserSessionController().isItMe(complex.getCurrentOwner()) && complex.getCurrentOwner() != null) {
                        addWarningMessage("Complex being curated by '" + complex.getCurrentOwner().getLogin() + "'",
                                "Please do not modify it without permission");
                    }
                    break;
                case READY_FOR_CHECKING:
                    if (!getUserSessionController().isItMe(complex.getCurrentReviewer()) && complex.getCurrentReviewer() != null) {
                        addWarningMessage("Complex under review", "This complex is being reviewed by '" + complex.getCurrentReviewer().getLogin() + "'");
                    }
                    break;
                case ACCEPTED_ON_HOLD:
                    addWarningMessage("Complex on-hold", "Reason: " + complex.getOnHoldComment());
                    break;
                case RELEASED:
                    SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy");
                    Date releasedDate=null;
                    for (LifeCycleEvent evt : collectLifecycleEvents(complex)){
                        if (LifeCycleEventType.RELEASED == evt.getEvent()){
                            releasedDate = evt.getWhen();
                        }
                    }
                    addInfoMessage("Complex already released", "This complex was released on " + (releasedDate != null ? sdf.format(releasedDate) : ""));
                    break;
                case NEW:

                    addWarningMessage("Complex with start status", "Assuming that it has been imported. Assign it to yourself if you are happy with this assumption");
                    break;
                default:
                    break;
            }
        }
    }

    protected void generalPublicationLoadChecks() {

        PublicationController publicationController = (PublicationController) getSpringContext().getBean("publicationController");

        if (publicationController.getPublication() != null) {
            IntactPublication publication = publicationController.getPublication();

            switch (publication.getStatus()){
                case CURATION_IN_PROGRESS:
                    if (!getUserSessionController().isItMe(publication.getCurrentOwner()) && publication.getCurrentOwner() != null) {
                        addWarningMessage("Publication being curated by '" + publication.getCurrentOwner().getLogin() + "'",
                                "Please do not modify it without permission");
                    }
                    break;
                case READY_FOR_CHECKING:
                    if (!getUserSessionController().isItMe(publication.getCurrentReviewer()) && publication.getCurrentReviewer() != null) {
                        addWarningMessage("Publication under review", "This publication is being reviewed by '" + publication.getCurrentReviewer().getLogin() + "'");
                    }
                    break;
                case ACCEPTED_ON_HOLD:
                    addWarningMessage("Publication on-hold", "Reason: " + publication.getOnHoldComment());
                    break;
                case RELEASED:
                    SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy");
                    Date releasedDate=null;
                    for (LifeCycleEvent evt : collectLifecycleEvents(publication)){
                        if (LifeCycleEventType.RELEASED == evt.getEvent()){
                            releasedDate = evt.getWhen();
                        }
                    }
                    addInfoMessage("Publication already released", "This publication was released on " + (releasedDate != null ? sdf.format(releasedDate) : ""));
                    break;
                case NEW:
                    addWarningMessage("Publication with start status", "Assuming that it has been imported. Assign it to yourself if you are happy with this assumption");
                    break;
                default:
                    break;
            }

        }
    }

    protected Collection<LifeCycleEvent> collectLifecycleEvents(Releasable releasable) {
        if (releasable.areLifeCycleEventsInitialized()){
            return releasable.getLifecycleEvents();
        }
        else{
            // reload releasable without flushing changes
            return getEditorService().initialiseLifecycleEvents(releasable);
        }
    }

    public void unsavedValueChange(ValueChangeEvent evt) {
        if (evt.getOldValue() != null && !evt.getOldValue().equals(evt.getNewValue())) {
            setUnsavedChanges(true);
        } else if (evt.getNewValue() != null && !evt.getNewValue().equals(evt.getOldValue())) {
            setUnsavedChanges(true);
        }
    }

    public String doSave() {
        doSave(true);
        return null;
    }

    /**
     * Executes the deletions and save the current object using the <code>CorePersister</code>. It invokes preSave()
     * before saving just in case a specific controller needs to prepare the object for the save operation. After invoking
     * the CorePersister's save(), it invokes the doSaveDetails() callback that can be used to handle whatever is not handled
     * by the CorePersister (ie. wrapped components). At the end, the current object is refreshed from the database.
     *
     * @param evt the action faces event
     */
    public void doSave(ActionEvent evt) {
        // this method will save and refresh the current view
        doSave(true);
    }

    /**
     * Executes the deletions and save the current object using the <code>CorePersister</code>. It invokes preSave()
     * before saving just in case a specific controller needs to prepare the object for the save operation. After invoking
     * the CorePersister's save(), it invokes the doSaveDetails() callback that can be used to handle whatever is not handled
     * by the CorePersister (ie. wrapped components). At the end, the current object is refreshed from the database.
     */
    public void doSave(boolean refreshCurrentView) {

        if (getAnnotatedObject() != null){
            doSaveIntact(refreshCurrentView, changesController);

        }
    }

    protected void doSaveIntact(boolean refreshCurrentView, ChangesController changesController) {

        String currentAc = getAnnotatedObject().getAc();
        boolean currentAnnotatedObjectDeleted = false;

        try{
            Collection<String> duplicatedAcs = getEditorService().findObjectDuplicates(getAnnotatedObject(), getDbSynchronizer());
            if (!duplicatedAcs.isEmpty()){
                addErrorMessage(duplicatedAcs.size()+" identical object exists: " + StringUtils.join(duplicatedAcs,", "), "Cannot save identical objects");
                FacesContext.getCurrentInstance().renderResponse();
            }
            else{

                // annotated objects specific tasks to prepare the save/delete
                doPreSave();

                boolean saved = false;

                // delete from the unsaved manager
                currentAnnotatedObjectDeleted = processDeleteEvents(currentAc);
                if (currentAnnotatedObjectDeleted){
                    saved = true;
                }

                // only save object if parent saved otherwise, call the save method on parent object
                IntactPrimaryObject annotatedObject = getAnnotatedObject();

                if (isParentObjectNotSaved()){
                    AnnotatedObjectController parent = getParentController();
                    if (parent != null){
                        parent.doSaveIntact(refreshCurrentView, changesController);
                        saved = true;
                    }
                }
                else{
                    if (!currentAnnotatedObjectDeleted) {
                        annotatedObject = getEditorService().doSave(annotatedObject, getDbSynchronizer());

                        if (annotatedObject != null) {
                            saved=true;
                            // saves specific elements for each annotated object (e.g. components in interactions)
                            boolean detailsSaved = doSaveDetails();

                            if (detailsSaved) saved = true;

                            lastSaved = new Date();
                            changesController.removeFromUnsaved(annotatedObject, collectParentAcsOfCurrentAnnotatedObject());
                        }
                    }
                }

                // we refresh the object if it has been saved
                if (annotatedObject != null){
                    setAnnotatedObject(annotatedObject);
                    addInfoMessage("Saved", getDescription());
                    doPostSave();
                }

                if (refreshCurrentView) {
                    refreshCurrentViewObject();
                }
            }
        }
        catch (Throwable t) {
            handleException(t);
        }
    }

    protected boolean processDeleteEvents(String currentAc){
        boolean delete = false;
        // delete from the unsaved manager
        final List<UnsavedChange> deletedObjects = new ArrayList(changesController.getAllUnsavedDeleted());

        EditorObjectService editorService = getEditorService();

        for (UnsavedChange unsaved : deletedObjects) {

            IntactPrimaryObject unsavedObject = unsaved.getUnsavedObject();

            // when an object is deleted, other deleted events can become obsolete and could have been removed from the deleted change events
            if (changesController.getAllUnsavedDeleted().contains(unsaved)) {
                // the object to delete is the current object itself. Should delete it now
                if (unsavedObject.getAc() != null && unsavedObject.getAc().equals(currentAc)) {

                    // remove the object to delete from its parent. If it is successful and the current object has been deleted, we can say that the save is successful
                    delete = editorService.doDelete(unsavedObject, unsaved.getDbSynchronizer());

                    postProcessDeletedEvent(unsaved);

                }
                // the object to delete is different from the current object. Checks that the scope of this object to delete is the ac of the current object being saved
                // if the scope is null or different, the object should not be deleted at this stage because we only save the current object and changes associated with it
                // if current ac is null, no deleted event should be associated with it as this object has not been saved yet
                else if (unsaved.getScope() != null && unsaved.getScope().equals(currentAc)) {
                    // remove the object to delete from its parent
                    delete = editorService.doDelete(unsavedObject, unsaved.getDbSynchronizer());

                    postProcessDeletedEvent(unsaved);
                }
            }

            if (delete){
                changesController.removeFromDeleted(unsavedObject);
            }
        }

        return delete;
    }

    protected void postProcessDeletedEvent(UnsavedChange unsaved){
        //nothing to do
    }

    private IntactPrimaryObject refresh(IntactPrimaryObject annotatedObject) {

        if (annotatedObject != null) {
            boolean isNew = false;

            if (getAnnotatedObject() != null) {
                isNew = (getAnnotatedObject().getAc() == null);
            }

            if (!isNew) {
                IntactPrimaryObject refreshed = getEditorService().refresh(annotatedObject);
                initialiseDefaultProperties(refreshed);
                return refreshed;
            }
        }

        return annotatedObject;
    }

    protected abstract void initialiseDefaultProperties(IntactPrimaryObject annotatedObject);

    protected void refreshCurrentViewObject() {

        final IntactPrimaryObject currentAo = curateController.getCurrentAnnotatedObjectController().getAnnotatedObject();

        if (currentAo != null && currentAo.getAc() != null) {
            refreshCurrentViewIntactObject(curateController, currentAo);
        }
    }

    protected void refreshCurrentViewIntactObject(CurateController curateController, IntactPrimaryObject currentAo) {
        // we have to refresh because the current annotated object is different from the annotated object of this controller
        if (getAnnotatedObject() != null && !currentAo.getAc().equals(getAnnotatedObject().getAc())) {
            if (log.isDebugEnabled())
                log.debug("Refreshing object in view: " + getAnnotatedObject().toString());

            IntactPrimaryObject refreshedAo = curateController.getCurrentAnnotatedObjectController().refresh(currentAo);
            curateController.getCurrentAnnotatedObjectController().setAnnotatedObject(refreshedAo);
        } else if (getAnnotatedObject() == null && currentAo != null) {
            if (log.isDebugEnabled())
                log.debug("Refreshing object in view: " + getAnnotatedObject().toString());

            IntactPrimaryObject refreshedAo = refresh(currentAo);
            curateController.getCurrentAnnotatedObjectController().setAnnotatedObject(refreshedAo);
        }
    }

    public void forceRefreshCurrentViewObject() {

        final IntactPrimaryObject currentAo = curateController.getCurrentAnnotatedObjectController().getAnnotatedObject();

        if (currentAo != null && currentAo.getAc() != null) {

            if (log.isDebugEnabled())
                log.debug("Refreshing object in view: " + currentAo.toString());

            IntactPrimaryObject refreshedAo = refresh(currentAo);
            curateController.getCurrentAnnotatedObjectController().setAnnotatedObject(refreshedAo);
        }
    }

    public void doSaveIfNecessary(ActionEvent evt) {
        if (getAnnotatedObject() != null && getAnnotatedObject().getAc() == null) {
            doSave(null);
        }
    }

    public void doPreSave() {
    }

    public void doPostSave() {
    }

    public boolean doSaveDetails() {
        return false;
    }

    public void validateAnnotatedObject(FacesContext context, UIComponent component, Object value) throws ValidatorException {

    }

    public void doRevertChanges(ActionEvent evt) {
        if (getAnnotatedObject() != null && getAnnotatedObject().getAc() == null) {
            doCancelEdition();
        }else {
            // revertJami first all unsaved events attached to any children of this object (will avoid to persist new annotations on children eg. copy publication annotations to experiments
            // could not be reverted otherwise)
            refreshUnsavedChangesBeforeRevert();

            EditorObjectService editorService = getEditorService();
            if (getAnnotatedObject() != null){
                setAnnotatedObject(editorService.doRevert(getAnnotatedObject()));
            }

            postRevert();

            addInfoMessage("Changes reverted", "");
        }
    }

    protected void postRevert() {
        // nothing by default
    }

    public String doCancelEdition() {
        addInfoMessage("Canceled", "");

        refreshUnsavedChangesBeforeRevert();

        return goToParent();

    }

    protected void refreshUnsavedChangesBeforeRevert() {
        if (getAnnotatedObject() != null){
            changesController.removeFromUnsaved(getAnnotatedObject(), Collections.EMPTY_LIST);
        }
    }

    public void changed() {
        setUnsavedChanges(true);
    }

    public void changed(ActionEvent evt) {
        setUnsavedChanges(true);
    }

    @Override
    public void changed(AjaxBehaviorEvent evt) {
        setUnsavedChanges(true);
    }

    public String clone() {
        if (getAnnotatedObject() != null){
            return clone(getAnnotatedObject(), newClonerInstance());
        }
        return null;
    }

    protected String clone(IntactPrimaryObject ao, EditorCloner cloner) {
        IntactPrimaryObject clone = cloneAnnotatedObject(ao, cloner);

        if (clone == null) return null;

        addInfoMessage("Cloned annotated object", null);

        setAnnotatedObject(clone);

        setUnsavedChanges(true);

        return getCurateController().edit(clone);
    }

    protected <T extends IntactPrimaryObject> T cloneAnnotatedObject(T ao, EditorCloner cloner) {
        T clone = null;

        try {
            clone = getEditorService().cloneAnnotatedObject(ao, cloner);
        } catch (Exception e) {
            addErrorMessage("Could not clone object", e.getMessage());
            handleException(e);
            return null;
        }

        modifyClone(clone);

        return clone;
    }

    public void modifyClone(IntactPrimaryObject clone) {
        refreshTabsAndFocusXref();
    }

    protected abstract EditorCloner newClonerInstance();

    public String doDelete() {
        EditorObjectService editorObjectService = getEditorService();

        if (getAnnotatedObject() != null && editorObjectService.doDelete(getAnnotatedObject(), getDbSynchronizer())) {
            setAnnotatedObject(null);
            return goToParent();
        }
        // if delete not successfull, just display the message and don't go to the parent because the message will be lost
        // keep editing this object
        else{
            return curateController.edit(getAnnotatedObject());
        }
    }

    protected void doPostDelete(){
        // nothing to do by default
    }

    // XREFS
    ///////////////////////////////////////////////

    public void xrefChanged(AbstractIntactXref xref) {

        CvTerm goDb = null;

        if (xref.getId() != null &&
                (xref.getId().startsWith("go:") ||
                        xref.getId().startsWith("GO:"))) {

            xref.setId(xref.getId().toUpperCase());

            if (xref.getAc() == null){
                try {
                    OntologyTerm goTerm = getGoServerProxy().fetchByIdentifier(xref.getId(), Xref.GO.toUpperCase());

                    if (goTerm != null) {
                        if (goDb == null)
                            goDb = IntactUtils.createMIDatabase(psidev.psi.mi.jami.model.Xref.GO,
                                    psidev.psi.mi.jami.model.Xref.GO_MI);

                        xref.setDatabase(goDb);
                        xref.setSecondaryId(goTerm.getFullName());

                        Collection<OntologyTerm> parents = new ArrayList<OntologyTerm>(goTerm.getParents());
                        // we have a root term
                        if (parents.isEmpty()) {
                            parents.add(goTerm);
                        }
                        CvTerm qualifier = calculateQualifier(parents);
                        xref.setQualifier(qualifier);
                    }
                } catch (Throwable e) {
                    handleException(e);
                    return;
                }
            }
        }
    }

    private CvTerm calculateQualifier(Collection<OntologyTerm> parents) {
        if (parents.isEmpty()) return null;

        Iterator<OntologyTerm> parentIterator = parents.iterator();
        OntologyTerm firstParent = parentIterator.next();
        while (firstParent.getIdentifiers().isEmpty() &&
                parentIterator.hasNext()){
            firstParent = parentIterator.next();
        }
        if (firstParent.getIdentifiers().isEmpty()){
            return null;
        }

        String goId = firstParent.getIdentifiers().iterator().next().getId();

        CvTerm terms = null;

        if ("GO:0008150".equals(goId)) {
            terms = IntactUtils.createMIQualifier(PROCESS, PROCESS_MI_REF);
        } else if ("GO:0003674".equals(goId)) { // GO:0005554 was an alternative id for molecular function
            terms = IntactUtils.createMIQualifier(FUNCTION, FUNCTION_MI_REF);
        } else if ("GO:0005575".equals(goId)) {
            terms = IntactUtils.createMIQualifier(COMPONENT, COMPONENT_MI_REF);
        }
        if (terms == null){
            for (OntologyTerm parent : parents){
                Collection<OntologyTerm> parents2 = parent.getParents();
                CvTerm id = calculateQualifier(parents2);
                if (id != null){
                   return id;
                }
            }
            if (log.isWarnEnabled()) log.warn("No qualifier found for category: " + goId);
        }

        return terms;
    }

    public void newXref(ActionEvent evt){
        if (this.newDatabase != null && this.newXrefId != null){
            AbstractIntactXref newRef = newXref(this.newDatabase, this.newXrefId, this.newSecondaryId, this.newXrefVersion, this.newQualifier);
            // check if go
            xrefChanged(newRef);
            // add xref to object
            addNewXref(newRef);
            // save
            doSave(false);

            this.newDatabase = null;
            this.newXrefId = null;
            this.newXrefVersion = null;
            this.newQualifier = null;
            this.newSecondaryId = null;
        }
        else{
            addErrorMessage("Cannot add new xref as the database and/or primary identifier is(are) missing","No database/primary identifier provided");
        }
    }

    protected abstract void addNewXref(AbstractIntactXref newRef);

    protected abstract <T extends AbstractIntactXref> T newXref(CvTerm db, String id, String secondaryId, String version, CvTerm qualifier);

    protected abstract <T extends AbstractIntactXref> T newXref(String db, String dbMI, String id, String secondaryId, String qualifier, String qualifierMI);

    public abstract List<psidev.psi.mi.jami.model.Xref> collectXrefs();

    public void updateXref(String database, String databaseMI, String primaryId, String qualifier, String qualifierMI,
                           Collection<Xref> refs ) {
        if (database == null){
            throw new IllegalArgumentException("Impossible to create/update/delete cross references if the database is not set.");
        }

        if ( primaryId != null && !primaryId.isEmpty() ) {
            replaceOrCreateXref( database, databaseMI, primaryId, null, qualifier, qualifierMI, refs );
        } else {
            removeXref( database, databaseMI, qualifier, qualifierMI, refs );
        }
    }

    public void replaceOrCreateXref( String database, String databaseMI, String primaryId, String secondaryId,
                                     String qualifier, String qualifierMI, Collection<Xref> refs ) {
        if (database == null){
            throw new IllegalArgumentException("Impossible to replace or create cross references if the database is not set.");
        }
        if (primaryId == null){
            throw new IllegalArgumentException("Impossible to replace or create cross references if the primary id is not set.");
        }

        // modify if exists
        Collection<Xref> existingRefs = XrefUtils.collectAllXrefsHavingDatabaseAndQualifier(refs, databaseMI, database, qualifierMI, qualifier);
        Xref existingRef = !existingRefs.isEmpty() ? existingRefs.iterator().next():null;
        // update if existing
        if (existingRef instanceof AbstractIntactXref){
            AbstractIntactXref intactRef = (AbstractIntactXref)existingRef;
            intactRef.setSecondaryId(secondaryId);
            intactRef.setId(primaryId);
        }
        // create if not exists
        else{
            refs.removeAll(existingRefs);
            refs.add(newXref(database, databaseMI, primaryId, secondaryId, qualifier, qualifierMI));
        }
        setUnsavedChanges(true);
    }

    protected void removeXref( String database, String databaseMI, String qualifier, String qualifierMI, Collection<Xref> refs ) {
        if (database == null){
            throw new IllegalArgumentException("Impossible to replace or create cross references if the database is not set.");
        }

        // modify if exists
        Collection<Xref> existingRefs = XrefUtils.collectAllXrefsHavingDatabaseAndQualifier(refs, databaseMI, database, qualifierMI, qualifier);
        refs.removeAll(existingRefs);
        setUnsavedChanges(true);
    }

    public abstract void removeXref(Xref xref);

    public void removeXref(Xref xref, Collection<Xref> refs) {
        Iterator<Xref> refIterator = refs.iterator();
        while (refIterator.hasNext()){
            if (refIterator.next() == xref){
                refIterator.remove();
            }
        }
        setUnsavedChanges(true);
    }

    public void addXref(String database, String databaseMI, String primaryId, String secondaryId,
                        String qualifier, String qualifierMI, Collection<Xref> refs) {
        if (database == null){
            throw new IllegalArgumentException("Impossible to add cross references if the database is not set.");
        }
        if (primaryId == null){
            throw new IllegalArgumentException("Impossible to add cross references if the primary id is not set.");
        }

        refs.add(newXref(database, databaseMI, primaryId, secondaryId, qualifier, qualifierMI));
        setUnsavedChanges(true);
    }

    public boolean isXrefValid(Xref xref) {
        if (xref == null) return false;
        if (xref.getId() == null || xref.getId().equals("to set")) return false;
        if (xref.getDatabase() == null || xref.getDatabase().getShortName().equals("to set")) return false;

        CvTerm ao = xref.getDatabase();

        final Annotation annotation = AnnotationUtils.collectFirstAnnotationWithTopic(ao.getAnnotations(),
                Annotation.VALIDATION_REGEXP_MI, Annotation.VALIDATION_REGEXP);

        if (annotation == null) return true;
        else if (annotation.getValue() == null){
            return false;
        }
        return xref.getId().matches(annotation.getValue());
    }

    public String externalLink(Xref xref) {
        if (xref == null) return null;
        if (xref.getId() == null || xref.getId().equals("to set")) return null;
        if (xref.getDatabase() == null || xref.getDatabase().getShortName().equals("to set")) return null;

        CvTerm ao = xref.getDatabase();

        final Annotation annotation = AnnotationUtils.collectFirstAnnotationWithTopic(ao.getAnnotations(),
                Annotation.SEARCH_URL_MI, Annotation.SEARCH_URL);
        if (annotation == null || annotation.getValue() == null) return null;

        String extUrl = annotation.getValue();
        return extUrl.replaceAll("\\$\\{ac\\}", xref.getId());
    }

    protected String navigateToObject(IntactPrimaryObject annotatedObject) {
        return curateController.newIntactObject(annotatedObject);
    }

    // ANNOTATIONS
    ///////////////////////////////////////////////

    public void newAnnotation(ActionEvent evt){
        if (this.newTopic != null){
            // new annot
            AbstractIntactAnnotation newAnnot = newAnnotation(this.newTopic, this.newAnnotationDescription);
            // add annot
            addNewAnnotation(newAnnot);
            // save changes
            doSave(false);

            this.newTopic = null;
            this.newAnnotationDescription = null;
        }
        else{
            addErrorMessage("Cannot add new annotation as it does not have any topics", "Missing annotation topic");
        }
    }

    protected abstract void addNewAnnotation(AbstractIntactAnnotation newAnnot);

    public abstract <T extends AbstractIntactAnnotation> T newAnnotation(CvTerm annotation, String text);

    public abstract <T extends AbstractIntactAnnotation> T newAnnotation(String topic, String topicMI, String text);

    public void addAnnotation(String topic, String topicMI, String text, Collection<Annotation> annots) {
        if (topic == null){
            throw new IllegalArgumentException("The topic must be set before creating an annotation.");
        }

        Annotation annotation = newAnnotation(topic, topicMI, text);
        annots.add(annotation);
        setUnsavedChanges(true);
    }

    public void removeAnnotation(String topic, String topicMI, Collection<Annotation> annots) {
        if (topic == null){
            throw new IllegalArgumentException("Impossible to replace or create annotations if the topic is not set.");
        }

        // modify if exists
        Collection<Annotation> existingAnnots = AnnotationUtils.collectAllAnnotationsHavingTopic(annots, topicMI, topic);
        annots.removeAll(existingAnnots);
        setUnsavedChanges(true);
    }

    public void removeAnnotation(String topic, String topicMI, String value, Collection<Annotation> annots) {
        if (topic == null){
            throw new IllegalArgumentException("Impossible to replace or create annotations if the topic is not set.");
        }

        // modify if exists
        Collection<Annotation> existingAnnots = AnnotationUtils.collectAllAnnotationsHavingTopic(annots, topicMI, topic);
        for (Annotation ann : existingAnnots){
            if (value == null && ann.getValue() == null){
                annots.remove(ann);
            }
            else if (value != null && value.equals(ann.getValue())){
                annots.remove(ann);
            }
        }
        setUnsavedChanges(true);
    }

    public abstract void removeAnnotation(Annotation annotation);

    public void removeAnnotation(Annotation annotation, Collection<Annotation> annots) {
        Iterator<Annotation> refIterator = annots.iterator();
        while (refIterator.hasNext()){
            if (refIterator.next() == annotation){
                refIterator.remove();
            }
        }
        setUnsavedChanges(true);
    }

    public void updateAnnotation(String topic, String topicMI, String value, Collection<Annotation> annots) {
        if (topic == null){
            throw new IllegalArgumentException("Impossible to create/update/delete annotations if the topic is not set.");
        }

        replaceOrCreateAnnotation( topic, topicMI, value, annots );
    }

    public void replaceOrCreateAnnotation( String topic, String topicMI, String text, Collection<Annotation> annots ) {
        if (topic == null){
            throw new IllegalArgumentException("The topic must be set before creating or replacing an annotation.");
        }

        // modify if exists
        Collection<Annotation> existingAnnots = AnnotationUtils.collectAllAnnotationsHavingTopic(annots, topicMI, topic);
        Annotation existingAnnot = !existingAnnots.isEmpty() ? existingAnnots.iterator().next():null;
        // update if existing
        if (existingAnnot != null){
            existingAnnot.setValue(text);
        }
        // create if not exists
        else{
            annots.add(newAnnotation(topic, topicMI, text));
        }
        setUnsavedChanges(true);
    }

    public abstract List<Annotation> collectAnnotations();

    // ALIASES
    ///////////////////////////////////////////////

    public void newAlias(ActionEvent evt){
        if (this.newAliasName != null && this.newAliasType != null){
            // create new alias
            AbstractIntactAlias newAlias = newAlias(this.newAliasType, this.newAliasName);
            // add alias
            addNewAlias(newAlias);
            // save
            doSave(false);

            this.newAliasType= null;
            this.newAliasName = null;
        }
        else{
            addErrorMessage("Cannot add the new alias as it does not have a name and/or type", "Alias without name and/or type");
        }
    }

    protected abstract void addNewAlias(AbstractIntactAlias newAlias);

    public abstract <T extends AbstractIntactAlias> T newAlias(CvTerm aliasType, String name);

    public abstract <T extends AbstractIntactAlias> T newAlias(String alias, String aliasMI, String name);

    public void addAlias(String alias, String aliasMI, String text, Collection<Alias> aliases) {
        if (text == null){
            throw new IllegalArgumentException("The alias name must be set before creating an alias.");
        }

        Alias al = newAlias(alias, aliasMI, text);
        aliases.add(al);
        setUnsavedChanges(true);
    }

    public void setAlias(String alias, String aliasMI, String text, Collection<Alias> aliases) {

        if ( text != null && !text.toString().isEmpty() ) {
            replaceOrCreateAlias(alias, aliasMI, text, aliases);
        } else {
            removeAlias(alias, aliasMI, aliases);
        }
    }

    public void replaceOrCreateAlias( String alias, String aliasMI, String name, Collection<Alias> aliases ) {
        if (name == null){
            throw new IllegalArgumentException("Impossible to replace or create aliases if the name is not set.");
        }

        // modify if exists
        Collection<Alias> existingAliases = AliasUtils.collectAllAliasesHavingType(aliases, aliasMI, alias);
        Alias existingAlias = !existingAliases.isEmpty() ? existingAliases.iterator().next():null;
        // update if existing
        if (existingAlias instanceof AbstractIntactAlias){
            AbstractIntactAlias intactAlias = (AbstractIntactAlias)existingAlias;
            intactAlias.setName(name);
        }
        // create if not exists
        else{
            aliases.removeAll(existingAliases);
            aliases.add(newAlias(alias, aliasMI, name));
        }
        setUnsavedChanges(true);
    }

    public void removeAlias(String alias, String aliasMI, String text, Collection<Alias> aliases) {
        if (text == null){
            throw new IllegalArgumentException("Impossible to replace or create aliases if the name is not set.");
        }

        // modify if exists
        Collection<Alias> existingAliases = AliasUtils.collectAllAliasesHavingTypeAndName(aliases, aliasMI, alias, text);
        aliases.removeAll(existingAliases);
        setUnsavedChanges(true);
    }

    public void removeAlias(String alias, String aliasMI, Collection<Alias> aliases) {

        // modify if exists
        Collection<Alias> existingAliases = AliasUtils.collectAllAliasesHavingType(aliases, aliasMI, alias);
        aliases.removeAll(existingAliases);
        setUnsavedChanges(true);
    }

    public abstract void removeAlias(Alias alias);

    public void removeAlias(Alias alias, Collection<Alias> aliases) {
        Iterator<Alias> refIterator = aliases.iterator();
        while (refIterator.hasNext()){
            if (refIterator.next() == alias){
                refIterator.remove();
            }
        }
        setUnsavedChanges(true);
    }

    public abstract List<Alias> collectAliases();

    // OTHER
    ////////////////////////////////////////////////////

    public String getCautionMessage() {
        return this.cautionMessage;
    }

    public String getCautionMessage(IntactPrimaryObject ao) {
        if (ao == null) return null;
        Collection<Annotation> annotations = Collections.EMPTY_LIST;
        if (ao instanceof IntactPublication){
            IntactPublication publication = (IntactPublication)ao;
            if (publication.areAnnotationsInitialized()){
               annotations = publication.getAnnotations();
            }
            else{
                annotations = ((PublicationEditorService)ApplicationContextProvider.getBean("publicationEditorService")).initialisePublicationAnnotations(publication);
            }
        }
        else if (ao instanceof IntactExperiment){
            IntactExperiment experiment = (IntactExperiment)ao;
            if (experiment.areAnnotationsInitialized()){
                annotations = experiment.getAnnotations();
            }
            else{
                annotations = ((ExperimentEditorService)ApplicationContextProvider.getBean("experimentEditorService")).initialiseExperimentAnnotations(experiment);
            }
        }
        else if (ao instanceof IntactInteractionEvidence){
            IntactInteractionEvidence interaction = (IntactInteractionEvidence)ao;
            if (interaction.areAnnotationsInitialized()){
                annotations = interaction.getAnnotations();
            }
            else{
                annotations = ((InteractionEditorService)ApplicationContextProvider.getBean("interactionEditorService")).initialiseInteractionAnnotations(interaction);
            }
        }
        else if (ao instanceof IntactInteractor){
            IntactInteractor interactor = (IntactInteractor)ao;
            if (interactor.areAnnotationsInitialized()){
                annotations = interactor.getAnnotations();
            }
            else{
                annotations = ((InteractorEditorService)ApplicationContextProvider.getBean("interactorEditorService")).initialiseInteractorAnnotations(interactor);
            }
        }
        else if (ao instanceof AbstractIntactParticipant){
            AbstractIntactParticipant participant = (AbstractIntactParticipant)ao;
            if (participant.areAnnotationsInitialized()){
                annotations = participant.getAnnotations();
            }
            else{
                annotations = ((ParticipantEditorService)ApplicationContextProvider.getBean("participantEditorService")).initialiseParticipantAnnotations(participant);
            }
        }
        else if (ao instanceof AbstractIntactFeature){
            AbstractIntactFeature participant = (AbstractIntactFeature)ao;
            if (participant.areAnnotationsInitialized()){
                annotations = participant.getAnnotations();
            }
            else{
                annotations = ((FeatureEditorService)ApplicationContextProvider.getBean("featureEditorService")).initialiseFeatureAnnotations(participant);
            }
        }
        else if (ao instanceof IntactCvTerm){
            IntactCvTerm cv = (IntactCvTerm)ao;
            if (cv.areAnnotationsInitialized()){
                annotations = cv.getAnnotations();
            }
            else{
                annotations = getCvService().initialiseCvAnnotations(cv);
            }
        }
        else if (ao instanceof IntactSource){
            IntactSource source = (IntactSource)ao;
            if (source.areAnnotationsInitialized()){
                annotations = source.getAnnotations();
            }
            else{
                annotations = ((InstitutionService)ApplicationContextProvider.getBean("institutionService")).initialiseSourceAnnotations(source);
            }
        }

        if (annotations.isEmpty()){
            return null;
        }
        Annotation caution = AnnotationUtils.collectFirstAnnotationWithTopic(annotations, Annotation.CAUTION_MI, Annotation.CAUTION);
        return caution != null ? caution.getValue() : null;
    }

    public String getInternalRemarkMessage() {
        return internalRemark;
    }

    public void setCautionMessage(String cautionMessage) {
        this.cautionMessage = cautionMessage;
    }

    public void setInternalRemark(String internalRemark) {
        this.internalRemark = internalRemark;
    }

    public boolean isUnsavedChanges() {
        return changesController.isUnsaved(getAnnotatedObject());
    }

    public void setUnsavedChanges(boolean unsavedChanges) {
        Collection<String> parentAcs = collectParentAcsOfCurrentAnnotatedObject();

        // we want to add a new change event for this annotated object
        if (unsavedChanges) {
            changesController.markAsUnsaved(getAnnotatedObject(), getDbSynchronizer(), getDescription(), parentAcs);
        }
        // we want to remove any change event concerning this object (or affecting parent and children)
        else {
            changesController.removeFromUnsaved(getAnnotatedObject(), parentAcs);
        }
    }

    public abstract Collection<String> collectParentAcsOfCurrentAnnotatedObject();

    public Date getLastSaved() {
        return lastSaved;
    }

    public void setLastSaved(Date lastSaved) {
        this.lastSaved = lastSaved;
    }

    public CuratorContextController getCuratorContextController() {
        return curatorContextController;
    }

    public CurateController getCurateController() {
        return curateController;
    }

    public boolean canIEditIt() {
        // get the root parent controller
        AnnotatedObjectController parentController = getParentController();
        if (parentController == null){
            parentController = this;
        }
        else{
            while (parentController.getParentController() != null){
                parentController = parentController.getParentController();
            }
        }

        if (parentController.getAnnotatedObject() instanceof Releasable) {
            return getUserSessionController().isItMe(((Releasable)parentController.getAnnotatedObject()).getCurrentOwner());
        }

        return true;
    }

    public ChangesController getChangesController() {
        return changesController;
    }

    protected boolean isParentObjectNotSaved() {
        AnnotatedObjectController parentController = getParentController();
        if (parentController == null){
            return false;
        }
        else{
            if (parentController.getAnnotatedObject() == null){
                return false;
            }
            else if (parentController.getAnnotatedObject().getAc() == null){
                return true;
            }
            // check with the root parent controller
            while (parentController.getParentController() != null){
                parentController = parentController.getParentController();
                if (parentController.getAnnotatedObject() != null && parentController.getAnnotatedObject().getAc() == null){
                    return true;
                }
            }
            return false;
        }
    }

    public abstract Class<? extends IntactPrimaryObject> getAnnotatedObjectClass();

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the publication ac of this experiment if it exists and add it to the list or parentAcs
     *
     * @param parentAcs
     * @param exp
     */
    public void addPublicationAcToParentAcs(Collection<String> parentAcs, Experiment exp) {
        if (exp != null && exp.getPublication() instanceof IntactPublication) {
            IntactPublication pub = (IntactPublication)exp.getPublication();

            if (pub.getAc() != null) {
                parentAcs.add(pub.getAc());
            }
        }
    }

    /**
     * Get the publication ac of this experiment if it exists, the ac of this experiment if it exists and add it to the list or parentAcs
     *
     * @param parentAcs
     * @param exp
     */
    public void addParentAcsTo(Collection<String> parentAcs, IntactExperiment exp) {
        if (exp != null && exp.getAc() != null) {
            parentAcs.add(exp.getAc());
        }

        addPublicationAcToParentAcs(parentAcs, exp);
    }

    public boolean isAnnotationTopicDisabled() {
        return isAnnotationTopicDisabled;
    }

    public void setAnnotationTopicDisabled(boolean annotationTopicDisabled) {
        isAnnotationTopicDisabled = annotationTopicDisabled;
    }

    public boolean isXrefDisabled() {
        return isXrefDisabled;
    }

    public void setXrefDisabled(boolean xrefDisabled) {
        isXrefDisabled = xrefDisabled;
    }

    public boolean isAliasDisabled() {
        return isAliasDisabled;
    }

    public void setAliasDisabled(boolean aliasDisabled) {
        isAliasDisabled = aliasDisabled;
    }

    public abstract boolean isAliasNotEditable(Alias alias);

    public abstract boolean isAnnotationNotEditable(Annotation annot);

    public abstract boolean isXrefNotEditable(Xref ref);

    /**
     * Bug jsf : selectOneMenu in a tab returns null if not active tab so we disable the selectOneMenu when it is disabled
     *
     * @param e
     */
    public void onTabChanged(TabChangeEvent e) {

        // the xref tab is active
        if (e.getTab().getId().equals("xrefsTab")) {
            isXrefDisabled = false;
            isAliasDisabled = true;
            isAnnotationTopicDisabled = true;
        } else if (e.getTab().getId().equals("annotationsTab")) {
            isXrefDisabled = true;
            isAliasDisabled = true;
            isAnnotationTopicDisabled = false;
        } else if (e.getTab().getId().equals("aliasesTab")) {
            isXrefDisabled = true;
            isAliasDisabled = false;
            isAnnotationTopicDisabled = true;
        } else {
            isXrefDisabled = true;
            isAliasDisabled = true;
            isAnnotationTopicDisabled = true;
        }
    }

    public abstract IntactDbSynchronizer getDbSynchronizer();

    public abstract String getObjectName();

    public String getObjectCategory(){
        return curatorContextController.intactObjectSimpleName(getAnnotatedObject());
    }

    public String getTitle(){
        if (getAnnotatedObject() != null){
            return getObjectCategory() + ": " + getObjectName()+" | Curate | Editor";
        }
        else{
            return " | Curate | Editor";
        }
    }

    public abstract String getAc();

    public abstract int getXrefsSize();

    public abstract int getAliasesSize();

    public abstract int getAnnotationsSize();

    public EditorObjectService getEditorService() {
        if (this.editorService == null){
            this.editorService = ApplicationContextProvider.getBean("editorObjectService");
        }
        return editorService;
    }

    public OntologyTermFetcher getGoServerProxy() throws BridgeFailedException {
        if (this.goServerProxy == null){
            this.goServerProxy = new CachedOlsOntologyTermFetcher();
        }
        return goServerProxy;
    }

    public CvObjectService getCvService() {
        if (this.cvService == null){
            this.cvService = ApplicationContextProvider.getBean("cvObjectService");
        }
        return cvService;
    }

    public static class AuditableComparator<T extends Auditable> implements Comparator<T>{

        @Override
        public int compare(T auditable, T auditable2) {
            if (auditable == null && auditable2 == null){
                return 0;
            }
            else if (auditable == null){
                return 1;
            }
            else if (auditable2 == null){
                return -1;
            }
            else{
                Date created1 = auditable.getCreated();
                Date created2 = auditable2.getCreated();
                if (created1 == null && created2 == null){
                    return 0;
                }
                else if (created1 == null){
                    return -1;
                }
                else if (created2 == null){
                    return 1;
                }
                else{
                    return -created1.compareTo(created2);
                }
            }
        }
    }

    public CvTerm getNewDatabase() {
        return newDatabase;
    }

    public void setNewDatabase(CvTerm newDatabase) {
        this.newDatabase = newDatabase;
    }

    public String getNewXrefId() {
        return newXrefId;
    }

    public void setNewXrefId(String newXrefId) {
        this.newXrefId = newXrefId;
    }

    public String getNewSecondaryId() {
        return newSecondaryId;
    }

    public void setNewSecondaryId(String newSecondaryId) {
        this.newSecondaryId = newSecondaryId;
    }

    public CvTerm getNewQualifier() {
        return newQualifier;
    }

    public void setNewQualifier(CvTerm newQualifier) {
        this.newQualifier = newQualifier;
    }

    public String getNewXrefVersion() {
        return newXrefVersion;
    }

    public void setNewXrefVersion(String newXrefVersion) {
        this.newXrefVersion = newXrefVersion;
    }

    public CvTerm getNewAliasType() {
        return newAliasType;
    }

    public void setNewAliasType(CvTerm newAliasType) {
        this.newAliasType = newAliasType;
    }

    public String getNewAliasName() {
        return newAliasName;
    }

    public void setNewAliasName(String newAliasName) {
        this.newAliasName = newAliasName;
    }

    public CvTerm getNewTopic() {
        return newTopic;
    }

    public void setNewTopic(CvTerm newTopic) {
        this.newTopic = newTopic;
    }

    public String getNewAnnotationDescription() {
        return newAnnotationDescription;
    }

    public void setNewAnnotationDescription(String newAnnotationDescription) {
        this.newAnnotationDescription = newAnnotationDescription;
    }
}
