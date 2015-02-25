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
package uk.ac.ebi.intact.editor.controller.curate.participant;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.model.SelectableDataModelWrapper;
import psidev.psi.mi.jami.bridges.exception.BridgeFailedException;
import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import uk.ac.ebi.intact.editor.controller.curate.AnnotatedObjectController;
import uk.ac.ebi.intact.editor.controller.curate.UnsavedChange;
import uk.ac.ebi.intact.editor.controller.curate.cloner.EditorCloner;
import uk.ac.ebi.intact.editor.controller.curate.interaction.FeatureWrapper;
import uk.ac.ebi.intact.editor.controller.curate.interaction.ImportCandidate;
import uk.ac.ebi.intact.editor.services.curate.interaction.ParticipantImportService;
import uk.ac.ebi.intact.editor.services.curate.participant.ParticipantEditorService;
import uk.ac.ebi.intact.editor.util.SelectableCollectionDataModel;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.IntactPrimaryObject;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.model.DataModel;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public abstract class AbstractParticipantController<T extends AbstractIntactParticipant> extends AnnotatedObjectController {

    private static final Log log = LogFactory.getLog( AbstractParticipantController.class );

    private T participant;
    @Resource(name = "participantEditorService")
    private transient ParticipantEditorService participantEditorService;

    @Resource(name = "participantImportService")
    private transient ParticipantImportService participantImportService;

    private String interactor;
    private List<ImportCandidate> interactorCandidates;

    private DataModel<FeatureWrapper> featuresDataModel;
    private FeatureWrapper[] selectedFeatures;

    /**
     * The AC of the participant to be loaded.
     */
    private String ac;

    private boolean isFeatureDisabled=false;
    private String participantId;
    private boolean isNoUniprotUpdate=false;

    private boolean isLoadAsMoleculeSet=false;

    public AbstractParticipantController() {
    }

    public abstract Class<T> getParticipantClass();

    @Override
    public IntactPrimaryObject getAnnotatedObject() {
        return getParticipant();
    }

    @Override
    public void setAnnotatedObject(IntactPrimaryObject annotatedObject) {
        setParticipant((T)annotatedObject);
    }

    @Override
    protected void loadCautionMessages() {
        if (this.participant != null){

            Annotation caution = AnnotationUtils.collectFirstAnnotationWithTopic(this.participant.getAnnotations(), Annotation.CAUTION_MI, Annotation.CAUTION);
            setCautionMessage(caution != null ? caution.getValue() : null);
            Annotation internal = AnnotationUtils.collectFirstAnnotationWithTopic(this.participant.getAnnotations(), null, "remark-internal");
            setInternalRemark(internal != null ? internal.getValue() : null);

            if (this.participant.getInteractor() instanceof IntactInteractor
                    && !((IntactInteractor)participant.getInteractor()).areXrefsInitialized()){
                setParticipant(getParticipantEditorService().reloadFullyInitialisedParticipant(this.participant, newClonerInstance(), newFeatureClonerInstance()));
            }
            this.participantId = this.participant.getInteractor().getPreferredIdentifier() != null ?
                    this.participant.getInteractor().getPreferredIdentifier().getId():this.participant.getInteractor().getShortName();

            Annotation noUniprotUpdate = AnnotationUtils.collectFirstAnnotationWithTopic(this.participant.getInteractor().getAnnotations(), null, "no-uniprot-update");
            this.isNoUniprotUpdate = noUniprotUpdate != null;
        }
    }

    public void loadData( ComponentSystemEvent event ) {
        if (!FacesContext.getCurrentInstance().isPostback()) {

            if ( ac != null ) {
                if ( participant == null || !ac.equals( participant.getAc() ) ) {
                    setParticipant(getParticipantEditorService().loadParticipantByAc(ac, getParticipantClass(), newFeatureClonerInstance()));
                }
            } else {
                if ( participant != null ) ac = participant.getAc();
            }

            if (participant == null) {
                addErrorMessage("No participant with this AC", ac);
                return;
            }

            refreshParentControllers();
            refreshTabs();
        }

        generalLoadChecks();
    }

    protected abstract EditorCloner newFeatureClonerInstance();

    @Override
    public void refreshTabs(){
        super.refreshTabs();

        this.isFeatureDisabled = false;
    }

    protected abstract void refreshParentControllers();

    @Override
    public void doPreSave() {
        // create master proteins from the unsaved manager
        final List<UnsavedChange> transcriptCreated = getChangesController().getAllUnsavedProteinTranscripts();

        for (UnsavedChange unsaved : transcriptCreated) {
            IntactPrimaryObject transcript = unsaved.getUnsavedObject();

            String currentAc = participant != null ? participant.getAc() : null;

            // the object to save is different from the current object. Checks that the scope of this object to save is the ac of the current object being saved
            // if the scope is null or different, the object should not be saved at this stage because we only save the current object and changes associated with it
            // if current ac is null, no unsaved event should be associated with it as this object has not been saved yet
            if (unsaved.getScope() != null && unsaved.getScope().equals(currentAc)){
                try {
                    getEditorService().doSaveMasterProteins(transcript);
                } catch (BridgeFailedException e) {
                    addErrorMessage("Cannot save master protein " + transcript.toString(), e.getCause() + ": " + e.getMessage());
                    log.error("Cannot save master protein ", e);
                } catch (SynchronizerException e) {
                    addErrorMessage("Cannot save master protein " + transcript.toString(), e.getCause() + ": " + e.getMessage());
                    log.error("Cannot save master protein ", e);
                } catch (FinderException e) {
                    addErrorMessage("Cannot save master protein " + transcript.toString(), e.getCause() + ": " + e.getMessage());
                    log.error("Cannot save master protein ", e);
                } catch (PersisterException e) {
                    addErrorMessage("Cannot save master protein " + transcript.toString(), e.getCause() + ": " + e.getMessage());
                    log.error("Cannot save master protein ", e);
                }  catch (Throwable e) {
                    addErrorMessage("Cannot save master protein " + transcript.toString(), e.getCause() + ": " + e.getMessage());
                    log.error("Cannot save master protein ", e);
                }
                getChangesController().removeFromHiddenChanges(unsaved);
            }
            else if (unsaved.getScope() == null && currentAc == null){
                try {
                    getEditorService().doSaveMasterProteins(transcript);
                } catch (BridgeFailedException e) {
                    addErrorMessage("Cannot save master protein " + transcript.toString(), e.getCause() + ": " + e.getMessage());
                    log.error("Cannot save master protein ", e);
                } catch (SynchronizerException e) {
                    addErrorMessage("Cannot save master protein " + transcript.toString(), e.getCause() + ": " + e.getMessage());
                    log.error("Cannot save master protein ", e);
                } catch (FinderException e) {
                    addErrorMessage("Cannot save master protein " + transcript.toString(), e.getCause() + ": " + e.getMessage());
                    log.error("Cannot save master protein ", e);
                } catch (PersisterException e) {
                    addErrorMessage("Cannot save master protein " + transcript.toString(), e.getCause() + ": " + e.getMessage());
                    log.error("Cannot save master protein ", e);
                } catch (Throwable e) {
                    addErrorMessage("Cannot save master protein " + transcript.toString(), e.getCause() + ": " + e.getMessage());
                    log.error("Cannot save master protein ", e);
                }
                getChangesController().removeFromHiddenChanges(unsaved);
            }
        }
    }

    @Override
    public Class<? extends IntactPrimaryObject> getAnnotatedObjectClass() {
        return getParticipantClass();
    }

    @Override
    public boolean isAliasNotEditable(Alias alias) {
        return false;
    }

    @Override
    public boolean isAnnotationNotEditable(Annotation annot) {
        return false;
    }

    public String newParticipant(Interaction interaction) throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException {
        this.interactor = null;

        IntactCvTerm defaultBiologicalRole = getCvService().getDefaultBiologicalRole();

        T participant = getParticipantClass().getConstructor(Interactor.class).newInstance(new IntactInteractor("to set"));
        participant.setStoichiometry(new IntactStoichiometry(getEditorConfig().getDefaultStoichiometry()));
        participant.setBiologicalRole(defaultBiologicalRole);

        // by setting the interaction of a participant, we don't add the participant to the collection of participants for this interaction so if we revertJami, it will not affect anything.
        // when saving, it will be added to the list of participants for this interaction. we just have to refresh the list of participants
        participant.setInteraction(interaction);

        setParticipant(participant);

        changed();

        return navigateToObject(participant);
    }

    public void importInteractor(ActionEvent evt) {
        try {
            interactorCandidates = new ArrayList<ImportCandidate>(getParticipantImportService().importParticipant(interactor));

            if (interactorCandidates.size() == 1) {
                interactorCandidates.get(0).setSelected(true);
            }
        } catch (BridgeFailedException e) {
            addErrorMessage("Cannot load interactor "+interactor, e.getCause()+": "+e.getMessage());
            log.error("Cannot load interactor  ", e);
        } catch (FinderException e) {
            addErrorMessage("Cannot load interactor " + interactor, e.getCause() + ": " + e.getMessage());
            log.error("Cannot load interactor  ", e);
        } catch (SynchronizerException e) {
            addErrorMessage("Cannot load interactor " + interactor, e.getCause() + ": " + e.getMessage());
            log.error("Cannot load interactor  ", e);
        } catch (PersisterException e) {
            addErrorMessage("Cannot load interactor " + interactor, e.getCause() + ": " + e.getMessage());
            log.error("Cannot load interactor  ", e);
        }
        catch (Throwable e){
            addErrorMessage("Cannot load interactor " + interactor, e.getCause() + ": " + e.getMessage());
            log.error("Cannot load interactor  ", e);
        }
    }

    public void addInteractorToParticipant(ActionEvent evt) {

        if (!isLoadAsMoleculeSet){
            for (ImportCandidate importCandidate : interactorCandidates) {
                if (importCandidate.isSelected()) {
                    // chain or isoform, we may have to update it later
                    if (importCandidate.isChain() || importCandidate.isIsoform()){
                        Collection<String> parentAcs = new ArrayList<String>();

                        getChangesController().markAsHiddenChange(importCandidate.getInteractor(), participant, parentAcs,
                                getEditorService().getIntactDao().getSynchronizerContext().getInteractorSynchronizer(), "Interactor "+importCandidate.getInteractor().getShortName());
                    }
                    participant.setInteractor(importCandidate.getInteractor());
                    // if the participant is a new participant, we don't need to add a unsaved notice because one already exists for creating a new participant
                    if (participant.getAc() != null){
                        setUnsavedChanges(true);
                    }
                }
            }
        }
        else if (!interactorCandidates.isEmpty()){
            IntactInteractorPool newPool = new IntactInteractorPool(interactor);
            for (ImportCandidate candidate : interactorCandidates) {
                if (candidate.isSelected()) {
                    // chain or isoform, we may have to update it later
                    if (candidate.isChain() || candidate.isIsoform()){
                        Collection<String> parentAcs = new ArrayList<String>();

                        getChangesController().markAsHiddenChange(candidate.getInteractor(), participant, parentAcs,
                                getEditorService().getIntactDao().getSynchronizerContext().getInteractorSynchronizer(), "Interactor "+
                                        candidate.getInteractor().getShortName());
                    }
                    newPool.add(candidate.getInteractor());
                }
            }

            participant.setInteractor(newPool);
            // if the participant is a new participant, we don't need to add a unsaved notice because one already exists for creating a new participant
            if (participant.getAc() != null){
                setUnsavedChanges(true);
            }
        }

        // reset load as molecule set
        this.isLoadAsMoleculeSet = false;
    }

    public void markFeatureToDelete(AbstractIntactFeature feature) {

        // don't forget to unlink features first
        if (feature.getAc() == null) {
            for (Object object : feature.getLinkedFeatures()){
               Feature linked = (Feature)object;
                linked.getLinkedFeatures().remove(feature);
            }
            feature.getLinkedFeatures().clear();
            participant.removeFeature(feature);
        } else {
            for (Object object : feature.getLinkedFeatures()){
                Feature linked = (Feature)object;
                linked.getLinkedFeatures().remove(feature);
            }
            feature.getLinkedFeatures().clear();
            Collection<String> parents = collectParentAcsOfCurrentAnnotatedObject();
            if (this.participant.getAc() != null){
                parents.add(this.participant.getAc());
            }
            getChangesController().markToDelete(feature, (AbstractIntactParticipant)feature.getParticipant(),
                    getEditorService().getIntactDao().getSynchronizerContext().getFeatureSynchronizer(), "Feature "+feature.getShortName(), parents);
        }
    }

    public void deleteSelectedFeatures(ActionEvent evt) {
        for (FeatureWrapper feature : selectedFeatures) {
            markFeatureToDelete(feature.getFeature());
        }

        addInfoMessage("Features to be deleted", selectedFeatures.length+" have been marked to be deleted");
    }

    public String getAc() {
        return ac;
    }

    @Override
    public int getXrefsSize() {
        if (participant == null){
            return 0;
        }
        else {
            return participant.getXrefs().size();
        }
    }

    @Override
    public int getAliasesSize() {
        if (participant == null){
            return 0;
        }
        else{
            return getParticipantEditorService().countAliases(this.participant);
        }
    }

    @Override
    public int getAnnotationsSize() {
        if (participant == null){
            return 0;
        }
        else {
            return participant.getAnnotations().size();
        }
    }

    public void setAc( String ac ) {
        this.ac = ac;
    }

    public T getParticipant() {
        return participant;
    }

    public void setParticipant( T participant ) {
        this.participant = participant;

        if (participant != null){
            this.ac = participant.getAc();

            initialiseDefaultProperties(participant);
        }
        else{
            this.ac = null;
        }
    }

    protected String joinIds(Collection<Xref> xrefs) {
        Collection<String> ids = new ArrayList<String>(xrefs.size());

        for (Xref xref : xrefs) {
            ids.add(xref.getId());
        }

        return StringUtils.join(ids, ", ");
    }

    public String getInteractor() {
        return interactor;
    }

    public void setInteractor(String interactor) {
        this.interactor = interactor;
    }

    public List<ImportCandidate> getInteractorCandidates() {
        return interactorCandidates;
    }

    public void setInteractorCandidates(List<ImportCandidate> interactorCandidates) {
        this.interactorCandidates = interactorCandidates;
    }

    public FeatureWrapper[] getSelectedFeatures() {
        return selectedFeatures;
    }

    public void setSelectedFeatures(FeatureWrapper[] selectedFeatures) {
        this.selectedFeatures = selectedFeatures;
    }

    public DataModel<FeatureWrapper> getFeaturesDataModel() {
        return featuresDataModel;
    }

    public void onTabChanged(TabChangeEvent e) {

        // the xref tab is active
        super.onTabChanged(e);

        // all the tabs selectOneMenu are disabled, we can process the tabs specific to interaction
        if (isAliasDisabled() && isXrefDisabled() && isAnnotationTopicDisabled()){
            if (e.getTab().getId().equals("featureTab")){
                isFeatureDisabled = false;
            }
            else {
                isFeatureDisabled = true;
            }
        }
        else {
            isFeatureDisabled = true;
        }
    }

    public void refreshFeatures() {
        List<FeatureWrapper> wrappers = new ArrayList<FeatureWrapper>(this.participant.getFeatures().size());

        for ( Object obj : this.participant.getFeatures() ) {
            wrappers.add( new FeatureWrapper( (AbstractIntactFeature)obj ) );
        }

        featuresDataModel = new SelectableDataModelWrapper(new SelectableCollectionDataModel<FeatureWrapper>(wrappers), wrappers);
    }


    public void reloadSingleFeature(AbstractIntactFeature f){
        Iterator<? extends Feature> evIterator = participant.getFeatures().iterator();
        boolean add = true;
        while (evIterator.hasNext()){
            AbstractIntactFeature intactEv = (AbstractIntactFeature)evIterator.next();
            if (intactEv.getAc() == null && f == intactEv){
                add = false;
            }
            else if (intactEv.getAc() != null && !intactEv.getAc().equals(f.getAc())){
                evIterator.remove();
            }
        }

        if (add){
            participant.getFeatures().add(f);
        }

        refreshFeatures();
    }

    public void removeFeature(AbstractIntactFeature f){
        Iterator<? extends Feature> evIterator = participant.getFeatures().iterator();
        while (evIterator.hasNext()){
            AbstractIntactFeature intactEv = (AbstractIntactFeature)evIterator.next();
            if (intactEv.getAc() == null && f == intactEv){
                evIterator.remove();
            }
            else if (intactEv.getAc() != null && intactEv.getAc().equals(f.getAc())){
                evIterator.remove();
            }
        }

        refreshFeatures();
    }

    @Override
    protected void initialiseDefaultProperties(IntactPrimaryObject annotatedObject) {
        T part = (T) annotatedObject;
        if (!getParticipantEditorService().isParticipantFullyLoaded(part)) {
            this.participant = getParticipantEditorService().reloadFullyInitialisedParticipant(part, newClonerInstance(), newFeatureClonerInstance());
        }

        refreshFeatures();

        interactor = participant.getInteractor().getShortName().equals("to set") ? null : participant.getInteractor().getShortName();

        setDescription("Participant: "+(participant.getAc() != null ? participant.getAc(): participant.getInteractor().getShortName()));
    }

    @Override
    public String getObjectName() {
        return participant != null && participant.getAc() != null ? participant.getAc() : participantId;
    }

    public int getFeaturesSize() {
        if (participant == null){
            return 0;
        }
        else {
            return participant.getFeatures().size();
        }
    }

    public List<Annotation> collectAnnotations() {
        List<Annotation> annotations = new ArrayList<Annotation>(participant.getAnnotations());
        Collections.sort(annotations, new AuditableComparator());
        // annotations are always initialised
        return annotations;
    }

    @Override
    public void removeAlias(Alias alias) {

        participant.getAliases().remove(alias);
    }

    @Override
    protected void addNewAnnotation(AbstractIntactAnnotation newAnnot) {
        this.participant.getAnnotations().add(newAnnot);
    }

    public List<Alias> collectAliases() {
        List<Alias> aliases = new ArrayList<Alias>(this.participant.getAliases());
        Collections.sort(aliases, new AuditableComparator());
        return aliases;
    }

    public List<Xref> collectXrefs() {

        List<Xref> xrefs = new ArrayList<Xref>(this.participant.getXrefs());
        Collections.sort(xrefs, new AuditableComparator());
        return xrefs;
    }

    @Override
    public void removeXref(Xref xref) {
        this.participant.getXrefs().remove(xref);
    }

    @Override
    public void removeAnnotation(Annotation annotation) {
         this.participant.getAnnotations().remove(annotation);
    }

    public ParticipantEditorService getParticipantEditorService() {
        if (this.participantEditorService == null){
            this.participantEditorService = ApplicationContextProvider.getBean("participantEditorService");
        }
        return participantEditorService;
    }

    public boolean isFeatureDisabled() {
        return isFeatureDisabled;
    }

    public int getMinStoichiometry(){
        return this.participant.getStoichiometry() != null ? this.participant.getStoichiometry().getMinValue() : 0;
    }

    public int getMaxStoichiometry(){
        return this.participant.getStoichiometry() != null ? this.participant.getStoichiometry().getMaxValue() : 0;
    }

    public void setMinStoichiometry(int stc){
        if (this.participant.getStoichiometry() == null){
            this.participant.setStoichiometry(new IntactStoichiometry(stc));
        }
        else {
            Stoichiometry stoichiometry = participant.getStoichiometry();
            this.participant.setStoichiometry(new IntactStoichiometry(stc, Math.max(stc, stoichiometry.getMaxValue())));
        }
    }

    public void setMaxStoichiometry(int stc){
        if (this.participant.getStoichiometry() == null){
            this.participant.setStoichiometry(new IntactStoichiometry(stc));
        }
        else {
            Stoichiometry stoichiometry = participant.getStoichiometry();
            this.participant.setStoichiometry(new IntactStoichiometry(Math.min(stc, stoichiometry.getMinValue()), stc));
        }
    }

    @Override
    protected void addNewAlias(AbstractIntactAlias newAlias) {
        this.participant.getAliases().add(newAlias);
    }

    public String getParticipantPrimaryId() {
        return this.participantId;
    }

    public boolean isNoUniprotUpdate() {
        return isNoUniprotUpdate;
    }

    public void selectLinkedFeature(FeatureWrapper wrapper, AbstractIntactFeature linked){
        wrapper.setSelectedLinkedFeature(linked);
    }

    public void unlinkFeature(FeatureWrapper wrapper) {
        AbstractIntactFeature feature1 = wrapper.getFeature();
        AbstractIntactFeature feature2 = wrapper.getSelectedLinkedFeature();
        Iterator<Feature> featureIterator = feature1.getLinkedFeatures().iterator();
        Iterator<Feature> feature2Iterator = feature2.getLinkedFeatures().iterator();
        while (featureIterator.hasNext()){
            AbstractIntactFeature f1 = (AbstractIntactFeature)featureIterator.next();
            if (f1.getAc() == null && f1 == feature2){
                featureIterator.remove();
            }
            else if (f1.getAc() != null && f1.getAc().equals(feature2.getAc())){
                featureIterator.remove();
            }
        }
        while (feature2Iterator.hasNext()){
            AbstractIntactFeature f2 = (AbstractIntactFeature)feature2Iterator.next();
            if (f2.getAc() == null && f2 == feature1){
                feature2Iterator.remove();
            }
            else if (f2.getAc() != null && f2.getAc().equals(feature1.getAc())){
                feature2Iterator.remove();
            }
        }

        addInfoMessage("Feature unlinked", feature2.toString());
        doSave(false);

        try {
            getEditorService().doSave((IntactPrimaryObject) feature2.getParticipant(), getDbSynchronizer());
        }catch (Throwable t) {
            handleException(t);
        }
    }

    @Override
    public boolean isXrefNotEditable(Xref ref) {
        return false;
    }

    public ParticipantImportService getParticipantImportService() {
        if (this.participantImportService == null){
            this.participantImportService = ApplicationContextProvider.getBean("participantImportService");
        }
        return participantImportService;
    }

    @Override
    protected void postProcessDeletedEvent(UnsavedChange unsaved) {
        super.postProcessDeletedEvent(unsaved);
        if (unsaved.getUnsavedObject() instanceof AbstractIntactFeature){
            removeFeature((AbstractIntactFeature)unsaved.getUnsavedObject());
        }
    }

    @Override
    protected void addNewXref(AbstractIntactXref newRef) {
        this.participant.getXrefs().add(newRef);
    }

    public boolean isLoadAsMoleculeSet() {
        return isLoadAsMoleculeSet;
    }

    public void setLoadAsMoleculeSet(boolean isLoadAsMoleculeSet) {
        this.isLoadAsMoleculeSet = isLoadAsMoleculeSet;
    }
}