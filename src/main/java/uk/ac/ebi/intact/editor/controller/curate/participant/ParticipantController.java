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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.primefaces.event.TabChangeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.jami.model.*;
import uk.ac.ebi.intact.editor.controller.curate.AnnotatedObjectController;
import uk.ac.ebi.intact.editor.controller.curate.UnsavedChange;
import uk.ac.ebi.intact.editor.controller.curate.cloner.EditorCloner;
import uk.ac.ebi.intact.editor.controller.curate.cloner.FeatureEvidenceCloner;
import uk.ac.ebi.intact.editor.controller.curate.cloner.InteractorCloner;
import uk.ac.ebi.intact.editor.controller.curate.cloner.ParticipantEvidenceCloner;
import uk.ac.ebi.intact.editor.controller.curate.experiment.ExperimentController;
import uk.ac.ebi.intact.editor.controller.curate.interaction.FeatureWrapper;
import uk.ac.ebi.intact.editor.controller.curate.interaction.InteractionController;
import uk.ac.ebi.intact.editor.controller.curate.publication.PublicationController;
import uk.ac.ebi.intact.editor.services.curate.organism.BioSourceService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.synchronizer.IntactDbSynchronizer;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.annotation.Resource;
import javax.faces.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.*;

/**
 * Participant controller.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope( "conversation.access" )
@ConversationName( "general" )
public class ParticipantController extends AbstractParticipantController<IntactParticipantEvidence> {

    private static final Log log = LogFactory.getLog( ParticipantController.class );

    private IntactCvTerm preparationToAdd;
    private IntactCvTerm identificationToAdd;

    @Autowired
    private PublicationController publicationController;

    @Autowired
    private ExperimentController experimentController;
    @Autowired
    private InteractionController interactionController;

    private boolean isParameterDisabled;
    private boolean isConfidenceDisabled;
    private boolean isExperimentalPreparationDisabled;
    private boolean isIdentificationMethodDisabled;

    private CvTerm newConfidenceType;
    private String newConfidenceValue;

    private CvTerm newParameterType;
    private Double newParameterFactor;
    private CvTerm newParameterUnit;
    private Integer newParameterBase;
    private Integer newParameterExponent;
    private Double newParameterUncertainty;

    @Resource(name = "bioSourceService")
    private transient BioSourceService bioSourceService;

    @Override
    public Class<IntactParticipantEvidence> getParticipantClass() {
        return IntactParticipantEvidence.class;
    }


    @Override
    protected EditorCloner newFeatureClonerInstance() {
        return new FeatureEvidenceCloner();
    }

    @Override
    public Collection<String> collectParentAcsOfCurrentAnnotatedObject(){
        Collection<String> parentAcs = new ArrayList<String>();

        if (getParticipant().getInteraction() != null){
            addParentAcsTo(parentAcs, (IntactInteractionEvidence)getParticipant().getInteraction());
        }

        return parentAcs;
    }

    /**
     * Add all the parent acs of this interaction
     * @param parentAcs
     * @param inter
     */
    protected void addParentAcsTo(Collection<String> parentAcs, IntactInteractionEvidence inter) {
        if (inter.getAc() != null){
            parentAcs.add(inter.getAc());
        }

        if (inter.getExperiment() instanceof IntactExperiment){
            addParentAcsTo(parentAcs, (IntactExperiment)inter.getExperiment());
        }
    }

    public void removeExperimentalPreparation(CvTerm prep){
        if (prep != null){
            if (getParticipant().getExperimentalPreparations().remove(prep)){
                changed();
            }
            else{
                addWarningMessage("The experimental preparation " + prep.getFullName()+" has not been removed.","The experimental preparation " + prep.getFullName()+" has not been removed because was not attached to this participant.");
            }
        }
    }

    public void addExperimentalPreparation(){
        if (this.preparationToAdd != null){
            getParticipant().getExperimentalPreparations().add(this.preparationToAdd);
            doSave(false);

            this.preparationToAdd = null;
        }
    }

    public void removeIdentificationMethod(CvTerm prep){
        if (prep != null){
            if (getParticipant().getIdentificationMethods().remove(prep)){
                changed();
            }
            else{
                addWarningMessage("The identification method " + prep.getFullName() + " has not been removed.", "The identification method " + prep.getFullName() + " has not been removed because was not attached to this participant.");
            }
        }
    }

    public void addIdentificationMethod(){
        if (this.identificationToAdd != null){
            getParticipant().getIdentificationMethods().add(this.identificationToAdd);
            doSave(false);

            this.identificationToAdd = null;
        }
    }

    @Override
    public void doPostSave(){
        // the participant was just created, add it to the list of participant of the interaction
        if (getParticipant().getInteraction() != null){
            interactionController.reloadSingleParticipant(getParticipant());
        }
    }

    @Override
    public void postRevert() {
        // the participant was just created, add it to the list of participant of the interaction
        if (getParticipant().getInteraction() != null){
            interactionController.reloadSingleParticipant(getParticipant());
        }
    }

    @Override
    protected EditorCloner<ParticipantEvidence, IntactParticipantEvidence> newClonerInstance() {
        return new ParticipantEvidenceCloner();
    }

    public String newParticipant(InteractionEvidence interaction) throws InvocationTargetException, NoSuchMethodException,
            InstantiationException, IllegalAccessException {
        String value = super.newParticipant(interaction);

        CvTerm defaultExperimentalRole = getCvService().getDefaultExperimentalRole();
        getParticipant().setExperimentalRole(defaultExperimentalRole);

        return value;
    }

    @Override
    public String doDelete(){
        interactionController.removeParticipant(getParticipant());
        return super.doDelete();
    }

    @Override
    protected ParticipantEvidenceXref newXref(CvTerm db, String id, String secondaryId, String version, CvTerm qualifier) {
        ParticipantEvidenceXref ref = new ParticipantEvidenceXref(db, id, version, qualifier);
        ref.setSecondaryId(secondaryId);
        return ref;
    }

    @Override
    public ParticipantEvidenceXref newXref(String db, String dbMI, String id, String secondaryId, String qualifier, String qualifierMI) {
        return new ParticipantEvidenceXref(getCvService().findCvObject(IntactUtils.DATABASE_OBJCLASS, dbMI != null ? dbMI : db),
                id,
                secondaryId,
                getCvService().findCvObject(IntactUtils.QUALIFIER_OBJCLASS, qualifierMI != null ? qualifierMI : qualifier));
    }

    @Override
    public void refreshTabs(){
        super.refreshTabsAndFocusXref();
        this.isConfidenceDisabled = true;
        this.isParameterDisabled = true;
        this.isExperimentalPreparationDisabled = true;
        this.isIdentificationMethodDisabled = true;
    }

    @Override
    protected AnnotatedObjectController getParentController() {
        return interactionController;
    }

    @Override
    protected String getPageContext() {
        return "participant";
    }

    @Override
    protected void refreshParentControllers() {
        // different loaded interaction
        if (interactionController.getInteraction() != getParticipant().getInteraction()){
            // different participant to load
            if (interactionController.getAc() == null ||
                    (getParticipant().getInteraction() instanceof IntactInteractionEvidence
                            && !interactionController.getAc().equals(((IntactInteractionEvidence)getParticipant().getInteraction()).getAc()))){
                IntactInteractionEvidence intactInteraction = (IntactInteractionEvidence)getParticipant().getInteraction();
                interactionController.setInteraction(intactInteraction);

                // reload other parents
                if (intactInteraction.getExperiment() instanceof IntactExperiment){
                    IntactExperiment exp = (IntactExperiment)intactInteraction.getExperiment();
                    experimentController.setExperiment( exp );

                    if ( exp.getPublication() instanceof IntactPublication ) {
                        IntactPublication publication = (IntactPublication)exp.getPublication();
                        publicationController.setPublication( publication );
                    }
                    else{
                        publicationController.setPublication(null);
                    }
                }
                else{
                    experimentController.setExperiment(null);
                }
            }
            // replace old feature instance with new one in feature tables of participant
            else{
                getParticipant().setInteraction(interactionController.getInteraction());
                interactionController.reloadSingleParticipant(getParticipant());
            }
        }
    }

    // Confidence
    ///////////////////////////////////////////////

    public void newConfidence(ActionEvent action) {
        if (this.newConfidenceType != null && this.newConfidenceValue != null){
            ParticipantEvidenceConfidence confidence = new ParticipantEvidenceConfidence(this.newConfidenceType, this.newConfidenceValue);
            getParticipant().getConfidences().add(confidence);
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
            FeatureEvidenceParameter param = new FeatureEvidenceParameter(this.newParameterType, new ParameterValue(new BigDecimal(this.newParameterFactor), this.newParameterBase.shortValue(),
                    this.newParameterExponent.shortValue()));
            if (this.newParameterUncertainty != null){
                param.setUncertainty(new BigDecimal(this.newParameterUncertainty));
            }
            param.setUnit(this.newParameterUnit);
            getParticipant().getParameters().add(param);
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

    public List<Confidence> collectConfidences() {

        final List<Confidence> confidences = new ArrayList<Confidence>( getParticipant().getConfidences() );
        Collections.sort( confidences, new AuditableComparator() );
        return confidences;
    }

    public List<Parameter> collectParameters() {

        final List<Parameter> parameters = new ArrayList<Parameter>( getParticipant().getParameters() );
        Collections.sort( parameters, new AuditableComparator() );
        return parameters;
    }

    public List<CvTerm> collectExperimentalPreparations() {

        final List<CvTerm> parameters = new ArrayList<CvTerm>( getParticipant().getExperimentalPreparations() );
        Collections.sort( parameters, new AuditableComparator() );
        return parameters;
    }

    public List<CvTerm> collectIdentificationMethods() {

        final List<CvTerm> parameters = new ArrayList<CvTerm>( getParticipant().getDbIdentificationMethods() );
        Collections.sort( parameters, new AuditableComparator() );
        return parameters;
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

    public boolean isIdentificationMethodDisabled() {
        return isIdentificationMethodDisabled;
    }

    public boolean isExperimentalPreparationDisabled() {
        return isExperimentalPreparationDisabled;
    }

    public void onTabChanged(TabChangeEvent e) {

        // the xref tab is active
        super.onTabChanged(e);

        // all the tabs selectOneMenu are disabled, we can process the tabs specific to interaction
        if (isAliasDisabled() && isXrefDisabled() && isAnnotationTopicDisabled()){
            if (e.getTab().getId().equals("parametersTab")){
                isParameterDisabled = false;
                isConfidenceDisabled = true;
                isExperimentalPreparationDisabled = true;
                isIdentificationMethodDisabled = true;
            }
            else if (e.getTab().getId().equals("confidencesTab")){
                isParameterDisabled = true;
                isConfidenceDisabled = false;
                isExperimentalPreparationDisabled = true;
                isIdentificationMethodDisabled = true;
            }
            else if (e.getTab().getId().equals("experimentalPreparationsTab")){
                isParameterDisabled = true;
                isConfidenceDisabled = true;
                isExperimentalPreparationDisabled = false;
                isIdentificationMethodDisabled = true;
            }
            else if (e.getTab().getId().equals("identificationMethodsTab")){
                isParameterDisabled = true;
                isConfidenceDisabled = true;
                isExperimentalPreparationDisabled = true;
                isIdentificationMethodDisabled = false;
            }
            else {
                isParameterDisabled = true;
                isConfidenceDisabled = true;
                isExperimentalPreparationDisabled = true;
                isIdentificationMethodDisabled = true;
            }
        }
        else {
            isParameterDisabled = true;
            isConfidenceDisabled = true;
            isExperimentalPreparationDisabled = true;
            isIdentificationMethodDisabled = true;
        }
    }

    @Override
    public IntactDbSynchronizer getDbSynchronizer() {
        return getEditorService().getIntactDao().getSynchronizerContext().getParticipantEvidenceSynchronizer();
    }

    public IntactCvTerm getPreparationToAdd() {
        return preparationToAdd;
    }

    public void setPreparationToAdd(IntactCvTerm preparationToAdd) {
        this.preparationToAdd = preparationToAdd;
    }

    public IntactCvTerm getIdentificationToAdd() {
        return identificationToAdd;
    }

    public void setIdentificationToAdd(IntactCvTerm identificationToAdd) {
        this.identificationToAdd = identificationToAdd;
    }

    @Override
    protected void generalLoadChecks() {
        super.generalLoadChecks();
        generalPublicationLoadChecks();
    }

    public int getExperimentalPreparationsSize() {
        if (getParticipant() == null){
            return 0;
        }
        else {
            return getParticipant().getExperimentalPreparations().size();
        }
    }

    public int getIdentificationMethodsSize() {
        if (getParticipant() == null){
            return 0;
        }
        else {
            return getParticipant().getDbIdentificationMethods().size();
        }
    }

    @Override
    public ParticipantEvidenceAlias newAlias(CvTerm aliasType, String name) {
        return new ParticipantEvidenceAlias(aliasType, name);
    }

    @Override
    public ParticipantEvidenceAlias newAlias(String alias, String aliasMI, String name) {

        return new ParticipantEvidenceAlias(getCvService().findCvObject(IntactUtils.ALIAS_TYPE_OBJCLASS, aliasMI != null ? aliasMI : alias),
                name);
    }

    @Override
    public ParticipantEvidenceAnnotation newAnnotation(CvTerm annotation, String text) {
        return new ParticipantEvidenceAnnotation(annotation, text);
    }

    @Override
    public ParticipantEvidenceAnnotation newAnnotation(String topic, String topicMI, String text) {
        return new ParticipantEvidenceAnnotation(getCvService().findCvObject(IntactUtils.TOPIC_OBJCLASS, topicMI != null ? topicMI: topic), text);
    }

    public void reloadSingleFeature(IntactFeatureEvidence f){
        // only update if not lazy loaded
        if (getParticipant().areFeaturesInitialized()){
            Iterator<FeatureEvidence> evIterator = getParticipant().getFeatures().iterator();
            boolean add = true;
            while (evIterator.hasNext()){
                IntactFeatureEvidence intactEv = (IntactFeatureEvidence)evIterator.next();
                if (intactEv.getAc() == null && f == intactEv){
                    add = false;
                }
                else if (intactEv.getAc() != null && intactEv.getAc().equals(f.getAc())){
                    evIterator.remove();
                }
            }
            if (add){
                getParticipant().getFeatures().add(f);
            }
        }

        refreshFeatures();

        interactionController.reloadSingleParticipant(getParticipant());
    }

    public void removeFeature(IntactFeatureEvidence f){
        // only update if not lazy loaded
        if (getParticipant().areFeaturesInitialized()){
            Iterator<FeatureEvidence> evIterator = getParticipant().getFeatures().iterator();
            while (evIterator.hasNext()){
                IntactFeatureEvidence intactEv = (IntactFeatureEvidence)evIterator.next();
                if (intactEv.getAc() == null && f == intactEv){
                    evIterator.remove();
                }
                else if (intactEv.getAc() != null && intactEv.getAc().equals(f.getAc())){
                    evIterator.remove();
                }
            }
        }

        refreshFeatures();

        interactionController.reloadSingleParticipant(getParticipant());
    }

    @Override
    public void addInteractorToParticipant(ActionEvent evt) {
        super.addInteractorToParticipant(evt);
        interactionController.reloadSingleParticipant(getParticipant());
    }

    public BioSourceService getBioSourceService() {
        if (this.bioSourceService == null){
            this.bioSourceService = ApplicationContextProvider.getBean("bioSourceService");
        }
        return bioSourceService;
    }

    public void removeConfidence(Confidence conf) {

        getParticipant().getConfidences().remove(conf);
    }

    public void removeParameter(Confidence conf) {

        getParticipant().getConfidences().remove(conf);
    }

    @Override
    protected void postProcessDeletedEvent(UnsavedChange unsaved) {
        super.postProcessDeletedEvent(unsaved);
        if (unsaved.getUnsavedObject() instanceof IntactFeatureEvidence){
            removeFeature((IntactFeatureEvidence)unsaved.getUnsavedObject());
        }
        interactionController.reloadSingleParticipant(getParticipant());
    }

    public int getParametersSize() {
        if (getParticipant() == null){
            return 0;
        }
        else {
            return getParticipant().getParameters().size();
        }
    }

    public int getConfidencesSize() {
        if (getParticipant() == null){
            return 0;
        }
        else {
            return getParticipant().getConfidences().size();
        }
    }

    @Override
    public void unlinkFeature(FeatureWrapper wrapper) {
        super.unlinkFeature(wrapper);
        interactionController.reloadSingleParticipant(getParticipant());
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

    public Integer getNewParameterExponent() {
        return newParameterExponent;
    }

    public void setNewParameterExponent(Integer newParameterExponent) {
        this.newParameterExponent = newParameterExponent;
    }

    public Double getNewParameterUncertainty() {
        return newParameterUncertainty;
    }

    public void setNewParameterUncertainty(Double newParameterUncertainty) {
        this.newParameterUncertainty = newParameterUncertainty;
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

    public String participantPrimaryId(IntactParticipantEvidence component) {
        if (component == null) return null;

        Interactor interactor = component.getInteractor();
        if (!getParticipantEditorService().isInteractorInitialised(interactor)){
            interactor = getParticipantEditorService().initialiseInteractor(interactor, new InteractorCloner());
        }

        final Xref xref = interactor.getPreferredIdentifier();

        if (xref == null) {
            if (interactor instanceof IntactInteractor){
                return ((IntactInteractor)interactor).getAc();
            }
            else{
                return interactor.getShortName();
            }
        }

        return xref.getId();
    }
}