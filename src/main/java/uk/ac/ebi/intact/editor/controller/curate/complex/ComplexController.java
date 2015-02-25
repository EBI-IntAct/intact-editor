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

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.joda.time.DateTime;
import org.primefaces.event.TabChangeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.jami.bridges.exception.BridgeFailedException;
import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.utils.AliasUtils;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import psidev.psi.mi.jami.utils.CvTermUtils;
import psidev.psi.mi.jami.utils.XrefUtils;
import uk.ac.ebi.intact.editor.controller.UserSessionController;
import uk.ac.ebi.intact.editor.controller.curate.AnnotatedObjectController;
import uk.ac.ebi.intact.editor.controller.curate.UnsavedChange;
import uk.ac.ebi.intact.editor.controller.curate.cloner.ComplexCloner;
import uk.ac.ebi.intact.editor.controller.curate.cloner.EditorCloner;
import uk.ac.ebi.intact.editor.controller.curate.cloner.ModelledParticipantCloner;
import uk.ac.ebi.intact.editor.controller.curate.interaction.FeatureWrapper;
import uk.ac.ebi.intact.editor.controller.curate.interaction.ParticipantWrapper;
import uk.ac.ebi.intact.editor.controller.curate.util.ParticipantWrapperCreatedDateComparator;
import uk.ac.ebi.intact.editor.services.curate.complex.ComplexEditorService;
import uk.ac.ebi.intact.editor.services.curate.organism.BioSourceService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.lifecycle.ComplexBCLifecycleEventListener;
import uk.ac.ebi.intact.jami.lifecycle.IllegalTransitionException;
import uk.ac.ebi.intact.jami.lifecycle.LifeCycleManager;
import uk.ac.ebi.intact.jami.model.IntactPrimaryObject;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.model.lifecycle.LifeCycleEvent;
import uk.ac.ebi.intact.jami.model.lifecycle.LifeCycleEventType;
import uk.ac.ebi.intact.jami.model.lifecycle.LifeCycleStatus;
import uk.ac.ebi.intact.jami.model.lifecycle.Releasable;
import uk.ac.ebi.intact.jami.model.user.User;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.IntactDbSynchronizer;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;
import uk.ac.ebi.intact.jami.utils.IntactUtils;
import uk.ac.ebi.intact.jami.utils.ReleasableUtils;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ValueChangeEvent;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope( "conversation.access" )
@ConversationName( "general" )
public class ComplexController extends AnnotatedObjectController {

    private static final Log log = LogFactory.getLog( ComplexController.class );

    private IntactComplex complex;
    private String ac;

    private LinkedList<ParticipantWrapper> participantWrappers;

    @Autowired
    private UserSessionController userSessionController;

    private boolean isParticipantDisabled;
    private boolean isParameterDisabled;
    private boolean isConfidenceDisabled;
    private boolean isLifeCycleDisabled;

    private boolean assignToMe = true;

    @Resource(name = "jamiLifeCycleManager")
    private transient LifeCycleManager lifecycleManager;

    @Resource(name = "complexEditorService")
    private transient ComplexEditorService complexEditorService;

    @Resource(name = "bioSourceService")
    private transient BioSourceService bioSourceService;

    private String name = null;
    private String toBeReviewed = null;
    private String onHold = null;
    private String correctionComment = null;
    private String cautionMessage = null;
    private String internalRemark = null;
    private String recommendedName = null;
    private String systematicName = null;
    private String description = null;
    private String complexProperties = null;

    private String newXrefPubmed;
    private CvTerm newXrefEvidenceCode;

    private CvTerm newConfidenceType;
    private String newConfidenceValue;

    private CvTerm newParameterType;
    private Double newParameterFactor;
    private CvTerm newParameterUnit;
    private Integer newParameterBase;
    private Integer newParameterExponent;
    private Double newParameterUncertainty;

    public ComplexController() {
    }

    @Override
    public IntactPrimaryObject getAnnotatedObject() {
        return getComplex();
    }

    @Override
    public void setAnnotatedObject(IntactPrimaryObject annotatedObject) {
        setComplex((IntactComplex)annotatedObject);
    }

    public String getName(){
        return this.name;
    }

    @Override
    protected void initialiseDefaultProperties(IntactPrimaryObject annotatedObject) {
        IntactComplex interaction = (IntactComplex)annotatedObject;
        if (!getComplexEditorService().isComplexFullyLoaded(interaction)){
            this.complex = getComplexEditorService().reloadFullyInitialisedComplex(interaction);
        }

        refreshParticipants();
        refreshName();
    }

    public String getOrganism(){
        return this.complex.getOrganism() != null ? this.complex.getOrganism().getCommonName():"organism unknown";
    }

    @Override
    protected EditorCloner<Complex, IntactComplex> newClonerInstance() {
        return new ComplexCloner();
    }

    @Override
    public void modifyClone(IntactPrimaryObject clone) {
        super.modifyClone(clone);
        // to be overrided
        IntactComplex complex = (IntactComplex) clone;
        try{
            getLifecycleManager().getStartStatus().create(complex, "Created in Editor",
                    getCurrentUser());

            if (assignToMe) {
                User user = getCurrentUser();
                lifecycleManager.getNewStatus().claimOwnership(complex, user);
                lifecycleManager.getAssignedStatus().startCuration(complex, user);
            }
        }
        catch (IllegalTransitionException e){
            addErrorMessage("Cannot create complex: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
        }
    }

    @Override
    public void refreshTabs(){
        super.refreshTabs();
        isParticipantDisabled = false;
        isParameterDisabled = true;
        isConfidenceDisabled = true;
        isLifeCycleDisabled = true;
    }

    @Override
    protected AnnotatedObjectController getParentController() {
        return null;
    }

    @Override
    protected String getPageContext() {
        return "complex";
    }

    @Override
    protected void generalLoadChecks() {
        super.generalLoadChecks();
        generalComplexLoadChecks();
    }

    @Override
    protected void loadCautionMessages() {
        if (this.complex != null) {
            refreshInfoMessages();
        }
    }

    public String getOnHold(){
        return onHold != null ? onHold : "";
    }

    public String getCorrectionComment(){return correctionComment != null ? correctionComment : null;}

    public void loadData( ComponentSystemEvent event ) {
        if (!FacesContext.getCurrentInstance().isPostback()) {

            if ( ac != null ) {
                if ( complex == null || !ac.equals( complex.getAc() ) ) {
                    setComplex(getComplexEditorService().loadComplexByAc(ac));
                }
            } else {
                if ( complex != null ) ac = complex.getAc();
            }

            if (complex == null) {
                addErrorMessage("No Complex with this AC", ac);
                return;
            }

            refreshTabs();
        }
        generalLoadChecks();
    }

    @Override
    protected void postProcessDeletedEvent(UnsavedChange unsaved) {
        super.postProcessDeletedEvent(unsaved);
        if (unsaved.getUnsavedObject() instanceof IntactModelledParticipant){
            removeParticipant((IntactModelledParticipant) unsaved.getUnsavedObject());
        }
    }

    @Override
    public void doPreSave() {
        // create master proteins from the unsaved manager
        final List<UnsavedChange> transcriptCreated = super.getChangesController().getAllUnsavedProteinTranscripts();
        String currentAc = complex != null ? complex.getAc() : null;

        for (UnsavedChange unsaved : transcriptCreated) {
            IntactInteractor transcript = (IntactInteractor)unsaved.getUnsavedObject();

            // the object to save is different from the current object. Checks that the scope of this object to save is the ac of the current object being saved
            // if the scope is null or different, the object should not be saved at this stage because we only save the current object and changes associated with it
            // if current ac is null, no unsaved event should be associated with it as this object has not been saved yet
            if (unsaved.getScope() != null && unsaved.getScope().equals(currentAc)){
                try {
                    getEditorService().doSaveMasterProteins(transcript);
                } catch (BridgeFailedException e) {
                    addErrorMessage("Cannot save master protein " + transcript.toString(), e.getCause() + ": " + e.getMessage());
                } catch (SynchronizerException e) {
                    addErrorMessage("Cannot save master protein " + transcript.toString(), e.getCause() + ": " + e.getMessage());
                } catch (FinderException e) {
                    addErrorMessage("Cannot save master protein " + transcript.toString(), e.getCause() + ": " + e.getMessage());
                } catch (PersisterException e) {
                    addErrorMessage("Cannot save master protein " + transcript.toString(), e.getCause() + ": " + e.getMessage());
                } catch (Throwable e) {
                    addErrorMessage("Cannot save master protein " + transcript.toString(), e.getCause() + ": " + e.getMessage());
                }

                getChangesController().removeFromHiddenChanges(unsaved);

            }
            else if (unsaved.getScope() == null && currentAc == null){
                try {
                    getEditorService().doSaveMasterProteins(transcript);
                } catch (BridgeFailedException e) {
                    addErrorMessage("Cannot save master protein " + transcript.toString(), e.getCause() + ": " + e.getMessage());
                } catch (SynchronizerException e) {
                    addErrorMessage("Cannot save master protein " + transcript.toString(), e.getCause() + ": " + e.getMessage());
                } catch (FinderException e) {
                    addErrorMessage("Cannot save master protein " + transcript.toString(), e.getCause() + ": " + e.getMessage());
                } catch (PersisterException e) {
                    addErrorMessage("Cannot save master protein " + transcript.toString(), e.getCause() + ": " + e.getMessage());
                } catch (Throwable e) {
                    addErrorMessage("Cannot save master protein " + transcript.toString(), e.getCause() + ": " + e.getMessage());
                }
                getChangesController().removeFromHiddenChanges(unsaved);
            }
        }
    }

    public void markParticipantToDelete(IntactModelledParticipant component) {
        if (component == null) return;

        if (component.getAc() == null) {
            complex.removeParticipant(component);
            refreshParticipants();
        } else {
            Collection<String> parents = collectParentAcsOfCurrentAnnotatedObject();
            if (this.complex.getAc() != null){
                parents.add(this.complex.getAc());
            }
            getChangesController().markToDelete(component, this.complex, getEditorService().getIntactDao().getSynchronizerContext().getModelledParticipantSynchronizer(),
                    "participant "+component.getAc(), parents);
        }
    }

    public void refreshParticipants() {
        participantWrappers = new LinkedList<ParticipantWrapper>();

        final Collection<ModelledParticipant> components = complex.getParticipants();

        for ( ModelledParticipant component : components ) {
            participantWrappers.add( new ParticipantWrapper( (IntactModelledParticipant)component) );
        }

        if (participantWrappers.size() > 0) {
            //We sort the participants for avoiding confusion with the place that a new participant should be appeared.
            Collections.sort(participantWrappers, new ParticipantWrapperCreatedDateComparator());
        }
    }

    public void addParticipant(IntactModelledParticipant component) {
        complex.addParticipant(component);

        participantWrappers.add(new ParticipantWrapper(component));

        if (participantWrappers.size() > 0) {
            Collections.sort(participantWrappers, new ParticipantWrapperCreatedDateComparator());

        }

        setUnsavedChanges(true);
    }

    public String getAc() {
        return ac;
    }

    public void cloneParticipant(ParticipantWrapper participantWrapper) {
        ModelledParticipantCloner cloner = new ModelledParticipantCloner();

        IntactModelledParticipant clone = getEditorService().cloneAnnotatedObject((IntactModelledParticipant)participantWrapper.getParticipant(), cloner);
        addParticipant(clone);
    }

    public void linkSelectedFeatures(ActionEvent evt) {
        List<AbstractIntactFeature> selected = new ArrayList<AbstractIntactFeature>();

        for (ParticipantWrapper pw : participantWrappers) {
            for (FeatureWrapper fw : pw.getFeatures()) {
                if (fw.isSelected()) {
                    selected.add(fw.getFeature());
                }
            }
        }

        int selectedSize = selected.size();

        Iterator<AbstractIntactFeature> fIterator1 = selected.iterator();
        while (fIterator1.hasNext()){
            AbstractIntactFeature f1 = fIterator1.next();

            for (AbstractIntactFeature f2 : selected){
                if (f1.getAc() == null && f1 != f2){
                    f1.getLinkedFeatures().add(f2);
                    f2.getLinkedFeatures().add(f1);
                }
                else if (f1.getAc() != null && !f1.getAc().equals(f2.getAc())){
                    f1.getLinkedFeatures().add(f2);
                    f2.getLinkedFeatures().add(f1);
                }
            }
            fIterator1.remove();
        }


        addInfoMessage("Features linked", "Size of linked features : "+selectedSize);
        setUnsavedChanges(true);
        refreshParticipants();
    }

    public void unlinkFeature(FeatureWrapper wrapper) {
        AbstractIntactFeature feature1 = wrapper.getFeature();
        AbstractIntactFeature feature2 = wrapper.getSelectedLinkedFeature();
        if (feature2 != null){
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
            setUnsavedChanges(true);
            refreshParticipants();
        }
    }

    public void selectLinkedFeature(FeatureWrapper wrapper, IntactModelledFeature linked){
         wrapper.setSelectedLinkedFeature(linked);
    }

    public void setAc( String ac ) {
        this.ac = ac;
    }

    public IntactComplex getComplex() {
        return complex;
    }

    public void setComplex(IntactComplex complex) {
        this.complex = complex;
        if (complex != null) {
            this.ac = complex.getAc();

            initialiseDefaultProperties(complex);
        }
        else{
            this.ac = null;
        }
    }

    private void refreshName() {
        this.name = this.complex.getShortName();
        if (this.complex.getRecommendedName() != null){
            this.name = this.complex.getRecommendedName();
        }
        else if (this.complex.getSystematicName() != null){
            this.name = this.complex.getSystematicName();
        }
        else if (!this.complex.getAliases().isEmpty()){
            this.name = this.complex.getAliases().iterator().next().getName();
        }

        this.systematicName = this.complex.getSystematicName();
        this.recommendedName = this.complex.getRecommendedName();
    }

    private void refreshInfoMessages() {
        this.toBeReviewed = this.complex.getToBeReviewedComment();
        this.onHold = this.complex.getOnHoldComment();
        this.correctionComment = this.complex.getCorrectionComment();
        Annotation remark = AnnotationUtils.collectFirstAnnotationWithTopic(this.complex.getAnnotations(), null,
                "remark-internal");
        this.internalRemark = remark != null ? remark.getValue() : null;
        Annotation caution = AnnotationUtils.collectFirstAnnotationWithTopic(this.complex.getAnnotations(), Annotation.CAUTION_MI,
                Annotation.CAUTION);
        this.cautionMessage = caution != null ? caution.getValue() : null;
        Annotation desc = AnnotationUtils.collectFirstAnnotationWithTopic(this.complex.getAnnotations(), null,
                "curated-complex");
        this.description = desc != null ? desc.getValue() : null;
        this.complexProperties = this.complex.getPhysicalProperties();
    }

    public String getComplexProperties() {
        return complexProperties;
    }

    public void setComplexProperties(String complexProperties) {
        this.complex.setPhysicalProperties(complexProperties);
        this.complexProperties = complexProperties;
    }

    public LinkedList<ParticipantWrapper> getParticipants() {
        return participantWrappers;
    }


    // Confidence
    ///////////////////////////////////////////////

    public void newConfidence(ActionEvent evet) {
        if (this.newConfidenceType != null && this.newConfidenceValue != null){
            ComplexConfidence confidence = new ComplexConfidence(this.newConfidenceType, this.newConfidenceValue);
            complex.getModelledConfidences().add(confidence);
            doSave(false);

            this.newConfidenceValue = null;
            this.newConfidenceType = null;
        }
        else{
            addErrorMessage("Cannot add new confidence as it does not have any type/value", "Missing confidence type/value");
        }
    }

    public void newParameter(ActionEvent evt) {
        if (this.newParameterType != null && this.newParameterFactor != null
                && this.newParameterBase != null && this.newParameterExponent != null){
            ComplexParameter param = new ComplexParameter(this.newParameterType, new ParameterValue(new BigDecimal(this.newParameterFactor), this.newParameterBase.shortValue(),
                    this.newParameterExponent.shortValue()));
            if (this.newParameterUncertainty != null){
                param.setUncertainty(new BigDecimal(this.newParameterUncertainty));
            }
            param.setUnit(this.newParameterUnit);
            complex.getModelledParameters().add(param);
            doSave(false);

            this.newParameterBase = null;
            this.newParameterFactor = null;
            this.newParameterType = null;
            this.newParameterUncertainty = null;
            this.newParameterUnit = null;
            this.newParameterExponent = null;
        }
        else{
            addErrorMessage("Cannot add new parameter as it does not have any type/value", "Missing parameter type/value");
        }
    }

    public boolean isParticipantDisabled() {
        return isParticipantDisabled;
    }

    public void setParticipantDisabled(boolean participantDisabled) {
        isParticipantDisabled = participantDisabled;
    }

    public boolean isParameterDisabled() {
        return isParameterDisabled;
    }

    public void setParameterDisabled(boolean parameterDisabled) {
        isParameterDisabled = parameterDisabled;
    }

    public boolean isConfidenceDisabled() {
        return isConfidenceDisabled;
    }

    public void setConfidenceDisabled(boolean confidenceDisabled) {
        isConfidenceDisabled = confidenceDisabled;
    }

    public boolean isLifeCycleDisabled() {
        return isLifeCycleDisabled;
    }

    public void setLifeCycleDisabled(boolean advancedDisabled) {
        isLifeCycleDisabled = advancedDisabled;
    }

    public void onTabChanged(TabChangeEvent e) {

        // the xref tab is active
        super.onTabChanged(e);

        // all the tabs selectOneMenu are disabled, we can process the tabs specific to interaction
        if (isAliasDisabled() && isXrefDisabled() && isAnnotationTopicDisabled()){
            if (e.getTab().getId().equals("participantsTab")){
                isParticipantDisabled = false;
                isParameterDisabled = true;
                isConfidenceDisabled = true;
                isLifeCycleDisabled = true;
            }
            else if (e.getTab().getId().equals("parametersTab")){
                isParticipantDisabled = true;
                isParameterDisabled = false;
                isConfidenceDisabled = true;
                isLifeCycleDisabled = true;
            }
            else if (e.getTab().getId().equals("confidencesTab")){
                isParticipantDisabled = true;
                isParameterDisabled = true;
                isConfidenceDisabled = false;
                isLifeCycleDisabled = true;
            }
            else {
                isParticipantDisabled = true;
                isParameterDisabled = true;
                isConfidenceDisabled = true;
                isLifeCycleDisabled = false;
            }
        }
        else {
            isParticipantDisabled = true;
            isParameterDisabled = true;
            isConfidenceDisabled = true;
            isLifeCycleDisabled = true;
        }
    }

    @Override
    protected void addNewXref(AbstractIntactXref newRef) {
        if (XrefUtils.isXrefAnIdentifier(newRef) || XrefUtils.doesXrefHaveQualifier(newRef, null, "intact-secondary")){
            this.complex.getIdentifiers().add(newRef);
        }
        else{
            this.complex.getXrefs().add(newRef);
        }
        this.newXrefPubmed = null;
        this.newXrefEvidenceCode = null;
    }

    @Override
    protected InteractorXref newXref(CvTerm db, String id, String secondaryId, String version, CvTerm qualifier) {
        if (CvTermUtils.isCvTerm(db, Xref.GO_MI, Xref.GO)){
            ComplexGOXref goRef = new ComplexGOXref(id, version, qualifier);
            goRef.setSecondaryId(secondaryId);
            goRef.setPubmed(this.newXrefPubmed);
            goRef.setEvidenceType(this.newXrefEvidenceCode);
            goRef.setDatabase(getCvService().findCvObject(IntactUtils.DATABASE_OBJCLASS, Xref.GO_MI));
            return goRef;
        }
        else{
            InteractorXref ref = new InteractorXref(db, id, version, qualifier);
            ref.setSecondaryId(secondaryId);

            if (this.newXrefPubmed != null || this.newXrefEvidenceCode != null){
                addWarningMessage("Ignored pubmed id and evidence code as they are only added to go references", "Ignored pubmed id and evidence code");
            }
            return ref;
        }
    }

    @Override
    public InteractorXref newXref(String db, String dbMI, String id, String secondaryId, String qualifier, String qualifierMI) {
        return new InteractorXref(getCvService().findCvObject(IntactUtils.DATABASE_OBJCLASS, dbMI != null ? dbMI : db),
                id, secondaryId, getCvService().findCvObject(IntactUtils.QUALIFIER_OBJCLASS, qualifierMI != null ? qualifierMI : qualifier));
    }


    @Override
    protected void addNewAnnotation(AbstractIntactAnnotation newAnnot) {
         this.complex.getAnnotations().add(newAnnot);
    }

    @Override
    public InteractorAnnotation newAnnotation(CvTerm annotation, String text) {
        return new InteractorAnnotation(annotation, text);
    }

    @Override
    public InteractorAnnotation newAnnotation(String topic, String topicMI, String text) {
        return new InteractorAnnotation(getCvService().findCvObject(IntactUtils.TOPIC_OBJCLASS, topicMI != null ? topicMI: topic), text);
    }

    @Override
    public void removeAnnotation(Annotation annotation) {
         this.complex.getAnnotations().remove(annotation);
    }


    @Override
    public InteractorAlias newAlias(String alias, String aliasMI, String name) {
        return new InteractorAlias(getCvService().findCvObject(IntactUtils.ALIAS_TYPE_OBJCLASS, aliasMI != null ? aliasMI : alias),
                name);
    }

    @Override
    public void removeAlias(Alias alias) {
         this.complex.getAliases().remove(alias);
    }

    @Override
    public Collection<String> collectParentAcsOfCurrentAnnotatedObject() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Class<? extends IntactPrimaryObject> getAnnotatedObjectClass() {
        return IntactComplex.class;
    }

    @Override
    public List<Annotation> collectAnnotations() {
        List<Annotation> annotations = new ArrayList<Annotation>(complex.getAnnotations());
        Collections.sort(annotations, new AuditableComparator());
        // annotations are always initialised
        return annotations;
    }

    @Override
    protected void addNewAlias(AbstractIntactAlias newAlias) {
        this.complex.getAliases().add(newAlias);
    }

    @Override
    public InteractorAlias newAlias(CvTerm aliasType, String name) {
        return new InteractorAlias(aliasType, name);
    }

    @Override
    public List<Alias> collectAliases() {
        List<Alias> aliases = new ArrayList<Alias>(complex.getAliases());
        Collections.sort(aliases, new AuditableComparator());
        // annotations are always initialised
        return aliases;
    }

    public boolean isAliasNotEditable(Alias alias){
        if (AliasUtils.doesAliasHaveType(alias, Alias.COMPLEX_RECOMMENDED_NAME_MI, Alias.COMPLEX_RECOMMENDED_NAME)){
            return true;
        }
        else if (AliasUtils.doesAliasHaveType(alias, Alias.COMPLEX_SYSTEMATIC_NAME_MI, Alias.COMPLEX_SYSTEMATIC_NAME)){
            return true;
        }
        else {
            return false;
        }
    }

    public boolean isAnnotationNotEditable(Annotation annot){
        if (AnnotationUtils.doesAnnotationHaveTopic(annot, null, Releasable.ON_HOLD)){
            return true;
        }
        else if (AnnotationUtils.doesAnnotationHaveTopic(annot, Annotation.COMPLEX_PROPERTIES_MI, Annotation.COMPLEX_PROPERTIES)){
            return true;
        }
        else if (AnnotationUtils.doesAnnotationHaveTopic(annot, null, Releasable.TO_BE_REVIEWED)){
            return true;
        }
        else if (AnnotationUtils.doesAnnotationHaveTopic(annot, null, Releasable.CORRECTION_COMMENT)){
            return true;
        }
        else if (AnnotationUtils.doesAnnotationHaveTopic(annot, null, "curated-complex")){
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

    public boolean isComplexGoRef(Xref ref) {
        return ref instanceof ComplexGOXref;
    }

    @Override
    public List<Xref> collectXrefs() {
        List<Xref> xrefs = new ArrayList<Xref>(this.complex.getDbXrefs());
        Collections.sort(xrefs, new AuditableComparator());
        return xrefs;
    }

    @Override
    public void removeXref(Xref xref) {
        // we need to be careful as curators can add complexGoRef which can be identical to other normal refs
        if (XrefUtils.isXrefFromDatabase(xref, Xref.GO_MI, Xref.GO)){
            Iterator<Xref> refIterator = complex.getXrefs().iterator();
            boolean hasRemoved = false;
            while (refIterator.hasNext()){
                InteractorXref next = (InteractorXref)refIterator.next();
                 if (xref == next){
                     hasRemoved = true;
                     refIterator.remove();
                 }
                else if (next.getAc() != null && next.getAc().equals((InteractorXref)xref)){
                     hasRemoved = true;
                     refIterator.remove();
                 }
            }
            if (!hasRemoved){
                refIterator = complex.getIdentifiers().iterator();
                while (refIterator.hasNext()){
                    InteractorXref next = (InteractorXref)refIterator.next();
                    if (xref == next){
                        hasRemoved = true;
                        refIterator.remove();
                    }
                    else if (next.getAc() != null && next.getAc().equals((InteractorXref)xref)){
                        hasRemoved = true;
                        refIterator.remove();
                    }
                }
            }
        }
        else{
            this.complex.getXrefs().remove(xref);
            this.complex.getIdentifiers().remove(xref);
        }
    }

    public boolean isNewPublication() {
        return complex.getStatus() == LifeCycleStatus.NEW;
    }

    public boolean isAssigned() {
        return complex.getStatus() == LifeCycleStatus.ASSIGNED;
    }

    public boolean isCurationInProgress() {
        return complex.getStatus() == LifeCycleStatus.CURATION_IN_PROGRESS;
    }

    public boolean isReadyForChecking() {
        return complex.getStatus() == LifeCycleStatus.READY_FOR_CHECKING;
    }

    public boolean isReadyForRelease() {
        return complex.getStatus() == LifeCycleStatus.READY_FOR_RELEASE;
    }

    public boolean isAcceptedOnHold() {
        return complex.getStatus() == LifeCycleStatus.ACCEPTED_ON_HOLD;
    }

    public boolean isReleased() {
        return complex.getStatus() == LifeCycleStatus.RELEASED;
    }

    public void claimOwnership(ActionEvent evt) {

        try{
            getEditorService().claimOwnership(complex, getCurrentUser(), isAssigned());

            // automatically set as curation in progress if no one was assigned before
            if (isAssigned()) {
                addInfoMessage("Curation started", "Curation is now in progress");
            }

            addInfoMessage("Claimed complex ownership", "You are now the owner of this complex");
        }
        catch (IllegalTransitionException e){
            addErrorMessage("Cannot claim ownership: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
        }
    }

    public void markAsAssignedToMe(ActionEvent evt) {
        try{
            getEditorService().markAsAssignedToMe(complex, getCurrentUser());

            addInfoMessage("Ownership claimed", "The complex has been assigned to you");

            addInfoMessage("Curation started", "Curation is now in progress");

        }
        catch (IllegalTransitionException e){
            addErrorMessage("Cannot assign complex: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
        }
    }

    public void markAsCurationInProgress(ActionEvent evt) {

        if (!userSessionController.isItMe(complex.getCurrentOwner())) {
            addErrorMessage("Cannot mark as curation in progress", "You are not the owner of this complex");
            return;
        }
        try{
            getEditorService().markAsCurationInProgress(complex, getCurrentUser());

            addInfoMessage("Curation started", "Curation is now in progress");
        }
        catch (IllegalTransitionException e){
            addErrorMessage("Cannot mark as curation in progress: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
        }
    }

    public void markAsReadyForChecking(ActionEvent evt) {
        if (!userSessionController.isItMe(complex.getCurrentOwner())) {
            addErrorMessage("Cannot mark as Ready for checking", "You are not the owner of this complex");
            return;
        }

        try{
            getEditorService().markAsReadyForChecking(complex, getCurrentUser(), correctionComment);

            addInfoMessage("Complex ready for checking", "Assigned to reviewer: " + complex.getCurrentReviewer().getLogin());
        }
        catch (IllegalTransitionException e){
            addErrorMessage("Cannot mark as ready for checking: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
        }
    }

    public void revertReadyForChecking(ActionEvent evt) {
        try{
            getEditorService().revertReadyForChecking(this.complex, getCurrentUser());
        }
        catch (IllegalTransitionException e){
            addErrorMessage("Cannot revert ready for checking: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
        }
    }

    public void revertAccepted(ActionEvent evt) {
        try{
            getEditorService().revertAccepted(complex, getCurrentUser(), isReadyForRelease());
        }
        catch (IllegalTransitionException e){
            addErrorMessage("Cannot revert accepted: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
        }
    }

    public void removeOnHold(ActionEvent evt) {

        this.complex.removeOnHold();
    }

    public void removeToBeReviewed(ActionEvent evt) {

        this.complex.removeToBeReviewed();
    }

    public void putOnHold(ActionEvent evt) {
        try{
            getEditorService().putOnHold(complex, getCurrentUser(), onHold, isReadyForChecking(), isReleased());

            if (isReadyForRelease()) {
                addInfoMessage("On-hold added to complex", "Complex won't be released until the 'on hold' is removed");
            } else if (isReleased()) {
                addInfoMessage("On-hold added to released complex", "Data will be publicly visible until the next release");
            }
        }
        catch (IllegalTransitionException e){
            addErrorMessage("Cannot put on-hold: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
        }
    }

    public void readyForReleaseFromOnHold(ActionEvent evt) {
        setOnHold(null);
        try{
            getEditorService().readyForReleaseFromOnHold(complex, getCurrentUser());

        }
        catch (IllegalTransitionException e){
            addErrorMessage("Cannot mark as ready for release: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
        }
    }

    public void setOnHold(String reason) {
        this.onHold = reason;
    }

    public void setCorrectionComment(String reason) {
        this.correctionComment = reason;
    }

    public void setToBeReviewed(String reason) {
        this.toBeReviewed = reason;
    }

    public void onHoldChanged(ValueChangeEvent evt) {
        String newValue = (String) evt.getNewValue();
        if (newValue != null && newValue.length() > 0){
            setUnsavedChanges(true);
            this.complex.onHold(newValue);
            this.onHold = newValue;
        }
    }

    public void onToBeReviewedChanged(ValueChangeEvent evt) {
        String newValue = (String) evt.getNewValue();
        if (newValue != null && newValue.length() > 0){
            setUnsavedChanges(true);
            this.complex.onToBeReviewed(newValue);
            this.toBeReviewed = newValue;
        }
    }

    public void onCorrectionCommentChanged(ValueChangeEvent evt) {
        String newValue = (String) evt.getNewValue();
        if (newValue != null && newValue.length() > 0){
            setUnsavedChanges(true);
            this.complex.onCorrectionComment(newValue);
            this.correctionComment = newValue;
        }
    }

    public String getRecommendedName() {
        return recommendedName;
    }

    public void setRecommendedName(String recommendedName) {
        this.recommendedName = recommendedName;
    }

    public String getSystematicName() {
        return systematicName;
    }

    public void setSystematicName(String systematicName) {
        this.systematicName = systematicName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void onRecommendedNameChanged(ValueChangeEvent evt) {
        String newValue = (String) evt.getNewValue();
        if (newValue == null || newValue.length() == 0){
            this.complex.setRecommendedName(null);
            this.recommendedName = null;
            setUnsavedChanges(true);
        }
        else{
            this.complex.setRecommendedName(newValue);
            this.recommendedName = newValue;
            setUnsavedChanges(true);
        }
    }

    public void onSystematicNameChanged(ValueChangeEvent evt) {

        String newValue = (String) evt.getNewValue();
        if (newValue == null || newValue.length() == 0){
            this.complex.setSystematicName(null);
            this.systematicName = null;
            setUnsavedChanges(true);
        }
        else{
            this.complex.setSystematicName(newValue);
            this.systematicName = newValue;
            setUnsavedChanges(true);
        }
    }

    public void onDescriptionChanged(ValueChangeEvent evt) {
        setUnsavedChanges(true);

        this.description = (String) evt.getNewValue();

        Annotation curatedComplex = AnnotationUtils.collectFirstAnnotationWithTopic(this.complex.getAnnotations(), null, "curated-complex");
        if (curatedComplex != null){
            curatedComplex.setValue(this.description);
        }
        else{
            this.complex.getAnnotations().add(new InteractorAnnotation(IntactUtils.createMITopic("curated-complex", null), this.description));
        }
    }

    public void onComplexPropertiesChanged(ValueChangeEvent evt) {

        String newValue = (String) evt.getNewValue();
        if (newValue == null || newValue.length() == 0){
            this.complex.setPhysicalProperties(null);
            this.complexProperties = null;
            setUnsavedChanges(true);
        }
        else{
            this.complex.setPhysicalProperties(newValue);
            this.complexProperties = newValue;
            setUnsavedChanges(true);
        }
    }

    public boolean isAccepted() {
        if (complex == null || complex.getStatus() == null) {
            return false;
        }

        return complex.getStatus() == LifeCycleStatus.ACCEPTED ||
                complex.getStatus() == LifeCycleStatus.ACCEPTED_ON_HOLD ||
                complex.getStatus() == LifeCycleStatus.READY_FOR_RELEASE ||
                complex.getStatus() == LifeCycleStatus.RELEASED;
    }

    public boolean isToBeReviewed(IntactComplex pub) {
        return pub.isToBeReviewed();
    }

    public boolean isOnHold(IntactComplex pub) {
        return pub.isOnHold();
    }

    public boolean isCorrectionComment(IntactComplex pub) {
        return pub.hasCorrectionComment();
    }

    public boolean isComplexToBeReviewed() {
        return this.complex.isToBeReviewed();
    }

    public boolean isComplexOnHold() {
        return this.complex.isOnHold();
    }

    public boolean isComplexWithCorrectionComment() {
        return this.complex.hasCorrectionComment();
    }

    public void acceptComplex(ActionEvent evt) {
        try{
            String accepted = "Accepted " + new SimpleDateFormat("yyyy-MMM-dd").format(new Date()).toUpperCase() + " by " + userSessionController.getCurrentUser().getLogin().toUpperCase();

            getEditorService().accept(complex, userSessionController.getCurrentUser(), accepted);

        }
        catch (IllegalTransitionException e){
            addErrorMessage("Cannot accept complex: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
        }
    }

    public void rejectComplex(ActionEvent evt) {

        rejectComplex(toBeReviewed);

    }

    public void rejectComplex(String reasonForRejection) {
        String date = "Rejected " + new SimpleDateFormat("yyyy-MMM-dd").format(new Date()).toUpperCase() + " by " + userSessionController.getCurrentUser().getLogin().toUpperCase();

        try{
            getEditorService().reject(complex, getCurrentUser(), date + ". " + reasonForRejection);

            addInfoMessage("Complex rejected", "");

            this.toBeReviewed = this.complex.getToBeReviewedComment();
        }
        catch (IllegalTransitionException e){
            addErrorMessage("Cannot reject complex: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
        }
    }

    public boolean isBeenRejectedBefore() {
        for (LifeCycleEvent evt : complex.getLifecycleEvents()) {
            if (LifeCycleEventType.REJECTED.equals(evt.getEvent())) {
                return true;
            }
        }

        return false;
    }

    public String getToBeReviewed() {
        return toBeReviewed != null ? toBeReviewed : "";
    }

    public void clearToBeReviewed(ActionEvent evt) {
        this.complex.removeToBeReviewed();
        toBeReviewed = null;
    }

    public boolean isAssignToMe() {
        return assignToMe;
    }

    public void setAssignToMe(boolean assignToMe) {
        this.assignToMe = assignToMe;
    }

    public boolean isNewComplex() {
        return complex.getStatus().equals(LifeCycleStatus.NEW);
    }


    public String newComplex(IntactInteractionEvidence interactionEvidence) {
        if (interactionEvidence == null || interactionEvidence.getAc() == null) {
            addErrorMessage("Cannot create biological complex", "Interaction evidence is empty or not saved");
            return null;
        }
        CvTerm type = getCvService().findCvObject(IntactUtils.INTERACTOR_TYPE_OBJCLASS, Complex.COMPLEX_MI);
        User user = getCurrentUser();
        // the interaction evidence is loaded with jami
        if (interactionEvidence != null){
            try {
                IntactComplex complex = getComplexEditorService().cloneInteractionEvidence(interactionEvidence, new ComplexCloner());
                setComplex(complex);
            } catch (SynchronizerException e) {
                addErrorMessage("Cannot clone the interaction evidence as a complex: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
            } catch (FinderException e) {
                addErrorMessage("Cannot clone the interaction evidence as a complex: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
            } catch (PersisterException e) {
                addErrorMessage("Cannot clone the interaction evidence as a complex: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
            }
        }

        this.complex.setInteractorType(type);

        try{
            getLifecycleManager().getStartStatus().create(this.complex, "Created in Editor",
                    user);

            if (assignToMe) {
                lifecycleManager.getNewStatus().claimOwnership(this.complex,
                        user);
                lifecycleManager.getAssignedStatus().startCuration(this.complex,
                        user);
            }
        }catch (IllegalTransitionException e){
            addErrorMessage("Cannot create new complex: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
        }

        return "/curate/complex?faces-redirect=true";
    }

    public String newComplex() {

        CvTerm type = getCvService().findCvObject(IntactUtils.INTERACTOR_TYPE_OBJCLASS, Complex.COMPLEX_MI);
        User user = getCurrentUser();

        setComplex(new IntactComplex("name to specify"));
        UserSessionController userSessionController = ApplicationContextProvider.getBean("userSessionController");
        this.complex.setSource(userSessionController.getUserInstitution());
        this.complex.setCreatedDate(new Date());
        this.complex.setUpdatedDate(this.complex.getCreatedDate());
        this.complex.setCreator(user.getLogin());
        this.complex.setUpdator(user.getLogin());
        this.complex.setInteractorType(type);
        try{
            getLifecycleManager().getStartStatus().create(this.complex, "Created in Editor", user);

            if (assignToMe) {
                lifecycleManager.getNewStatus().claimOwnership(this.complex, user);
                lifecycleManager.getAssignedStatus().startCuration(this.complex, user);
            }
        }
        catch (IllegalTransitionException e){
            addErrorMessage("Cannot create new complex: "+e.getMessage(), ExceptionUtils.getFullStackTrace(e));
        }

        return "/curate/complex?faces-redirect=true";
    }

    @Override
    public IntactDbSynchronizer getDbSynchronizer() {
        return getEditorService().getIntactDao().getSynchronizerContext().getComplexSynchronizer();
    }

    @Override
    public String getObjectName() {
        return getName();
    }

    /**
     * No transactional as it should always be initialised when loaded recommended name and systematic name when loading the page
     * @return
     */
    public int getAliasesSize() {
        if (this.complex == null){
            return 0;
        }
        else{
            return this.complex.getAliases().size();
        }
    }

    /**
     * No transactional as it should always be initialised when loaded recommended name and systematic name when loading the page
     * @return
     */
    public int getAnnotationsSize() {
        if (this.complex == null){
            return 0;
        }
        else{
            return this.complex.getAnnotations().size();
        }
    }

    public int getConfidencesSize() {
        if (this.complex == null){
            return 0;
        }
        else {
            return this.complex.getModelledConfidences().size();
        }
    }

    public int getParametersSize() {
        if (this.complex == null){
            return 0;
        }
        else{
            return this.complex.getModelledParameters().size();
        }
    }


    public int getXrefsSize() {
        if (this.complex == null){
            return 0;
        }
        else{
            return this.complex.getDbXrefs().size();
        }
    }

    public List<Confidence> collectConfidences() {
        List<Confidence> confidences = new ArrayList<Confidence>(this.complex.getModelledConfidences());
        Collections.sort(confidences, new AuditableComparator());
        return confidences;
    }

    public List<Parameter> collectParameters() {
        List<Parameter> params = new ArrayList<Parameter>(this.complex.getModelledParameters());
        Collections.sort(params, new AuditableComparator());
        return params;
    }

    public void removeConfidence(ModelledConfidence conf){
        this.complex.getModelledConfidences().remove(conf);
    }

    public void removeParameter(ModelledParameter param){
        this.complex.getModelledParameters().remove(param);
    }

    public Collection<LifeCycleEvent> collectLifecycleEvents() {
        return new ArrayList<LifeCycleEvent>(complex.getLifecycleEvents());
    }

    public LifeCycleManager getLifecycleManager() {
        if (this.lifecycleManager == null){
           this.lifecycleManager = ApplicationContextProvider.getBean("jamiLifeCycleManager");
        }
        this.lifecycleManager.registerListener(new ComplexBCLifecycleEventListener());
        return lifecycleManager;
    }

    public void addCorrectionComment(ActionEvent evt) {
        addInfoMessage("Added correction comment", correctionComment);
        this.complex.onCorrectionComment(correctionComment);
    }

    public void removeCorrectionComment(ActionEvent evt) {
        addInfoMessage("Removed correction comment", correctionComment);
        this.complex.removeCorrectionComment();
    }

    public void reloadSingleParticipant(IntactModelledParticipant f){
        Iterator<ModelledParticipant> evIterator = complex.getParticipants().iterator();
        boolean add = true;
        while (evIterator.hasNext()){
            IntactModelledParticipant intactEv = (IntactModelledParticipant)evIterator.next();
            if (intactEv.getAc() == null && f == intactEv){
                add = false;
            }
            else if (intactEv.getAc() != null && intactEv.getAc().equals(f.getAc())){
                evIterator.remove();
            }
        }

        if (add){
            complex.getParticipants().add(f);
        }

        refreshParticipants();
    }

    public void removeParticipant(IntactModelledParticipant f){
        Iterator<ModelledParticipant> evIterator = complex.getParticipants().iterator();
        while (evIterator.hasNext()){
            IntactModelledParticipant intactEv = (IntactModelledParticipant)evIterator.next();
            if (intactEv.getAc() == null && f == intactEv){
                evIterator.remove();
            }
            else if (intactEv.getAc() != null && intactEv.getAc().equals(f.getAc())){
                evIterator.remove();
            }
        }

        refreshParticipants();
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
        LifeCycleEvent event = ReleasableUtils.getLastEventOfType(complex, eventType);

        if (event == null) {
            return false;
        }

        DateTime eventTime = new DateTime(event.getWhen());

        return new DateTime().isBefore(eventTime.plusMinutes(getEditorConfig().getRevertDecisionTime()));
    }

    public ComplexEditorService getComplexEditorService() {
        if (this.complexEditorService == null){
           this.complexEditorService = ApplicationContextProvider.getBean("complexEditorService");
        }
        return complexEditorService;
    }

    public BioSourceService getBioSourceService() {
        if (this.bioSourceService == null){
            this.bioSourceService = ApplicationContextProvider.getBean("bioSourceService");
        }
        return bioSourceService;
    }

    public CvTerm getNewXrefEvidenceCode() {
        return newXrefEvidenceCode;
    }

    public void setNewXrefEvidenceCode(CvTerm newXrefEvidenceCode) {
        this.newXrefEvidenceCode = newXrefEvidenceCode;
    }

    public String getNewXrefPubmed() {
        return newXrefPubmed;
    }

    public void setNewXrefPubmed(String newXrefPubmed) {
        this.newXrefPubmed = newXrefPubmed;
    }

    public String getNewConfidenceValue() {
        return newConfidenceValue;
    }

    public void setNewConfidenceValue(String newConfidenceValue) {
        this.newConfidenceValue = newConfidenceValue;
    }

    public CvTerm getNewConfidenceType() {
        return newConfidenceType;
    }

    public void setNewConfidenceType(CvTerm newConfidenceType) {
        this.newConfidenceType = newConfidenceType;
    }

    public Double getNewParameterUncertainty() {
        return newParameterUncertainty;
    }

    public void setNewParameterUncertainty(Double newParameterUncertainty) {
        this.newParameterUncertainty = newParameterUncertainty;
    }

    public Integer getNewParameterExponent() {
        return newParameterExponent;
    }

    public void setNewParameterExponent(Integer newParameterExponent) {
        this.newParameterExponent = newParameterExponent;
    }

    public Integer getNewParameterBase() {
        return newParameterBase;
    }

    public void setNewParameterBase(Integer newParameterBase) {
        this.newParameterBase = newParameterBase;
    }

    public CvTerm getNewParameterUnit() {
        return newParameterUnit;
    }

    public void setNewParameterUnit(CvTerm newParameterUnit) {
        this.newParameterUnit = newParameterUnit;
    }

    public Double getNewParameterFactor() {
        return newParameterFactor;
    }

    public void setNewParameterFactor(Double newParameterFactor) {
        this.newParameterFactor = newParameterFactor;
    }

    public CvTerm getNewParameterType() {
        return newParameterType;
    }

    public void setNewParameterType(CvTerm newParameterType) {
        this.newParameterType = newParameterType;
    }

    public String getShortName(){
        return complex !=null && !"name to specify".equals(complex.getShortName()) ? complex.getShortName() : null;
    }

    public void setShortName(String name){
        this.complex.setShortName(name);
    }
}
