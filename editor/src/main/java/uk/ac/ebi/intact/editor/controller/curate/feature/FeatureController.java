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
package uk.ac.ebi.intact.editor.controller.curate.feature;

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
import uk.ac.ebi.intact.editor.controller.curate.experiment.ExperimentController;
import uk.ac.ebi.intact.editor.controller.curate.interaction.InteractionController;
import uk.ac.ebi.intact.editor.controller.curate.participant.ParticipantController;
import uk.ac.ebi.intact.editor.controller.curate.publication.PublicationController;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.synchronizer.IntactDbSynchronizer;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.faces.event.ActionEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Feature controller.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id: ParticipantController.java 14281 2010-04-12 21:48:43Z samuel.kerrien $
 */
@Controller
@Scope( "conversation.access" )
@ConversationName( "general" )
public class FeatureController extends AbstractFeatureController<IntactFeatureEvidence> {

    private static final Log log = LogFactory.getLog( FeatureController.class );

    @Autowired
    private PublicationController publicationController;

    @Autowired
    private ExperimentController experimentController;

    @Autowired
    private InteractionController interactionController;

    @Autowired
    private ParticipantController participantController;

    private boolean isParametersDisabled;
    private boolean isDetectionMethodDisabled;
    private IntactCvTerm detectionMethodToAdd=null;

    private CvTerm newParameterType;
    private Double newParameterFactor;
    private CvTerm newParameterUnit;
    private Integer newParameterBase;
    private Integer newParameterExponent;
    private Double newParameterUncertainty;

    @Override
    public Class<IntactFeatureEvidence> getFeatureClass() {
        return IntactFeatureEvidence.class;
    }

    @Override
    public Class<? extends AbstractIntactResultingSequence> getResultingSequenceClass() {
        return ExperimentalResultingSequence.class;
    }

    @Override
    public Class<? extends AbstractIntactXref> getResultingSequenceXrefClass() {
        return ExperimentalResultingSequenceXref.class;
    }

    @Override
    protected AnnotatedObjectController getParentController() {
        return participantController;
    }

    @Override
    protected String getPageContext() {
        return "participant";
    }

    @Override
    protected void refreshParentControllers() {
        // different loaded participant
        if (participantController.getParticipant() != getFeature().getParticipant()){
            // different participant to load
            if (participantController.getAc() == null ||
                    (getFeature().getParticipant() instanceof IntactParticipantEvidence
                            && !participantController.getAc().equals(((IntactParticipantEvidence)getFeature().getParticipant()).getAc()))){
                IntactParticipantEvidence intactParticipant = (IntactParticipantEvidence)getFeature().getParticipant();
                participantController.setParticipant(intactParticipant);

                // reload other parents
                if( intactParticipant.getInteraction() instanceof IntactInteractionEvidence ) {
                    final IntactInteractionEvidence interaction = (IntactInteractionEvidence)intactParticipant.getInteraction();
                    interactionController.setInteraction( interaction );

                    if (interaction.getExperiment() instanceof IntactExperiment){
                        IntactExperiment exp = (IntactExperiment)interaction.getExperiment();
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
                else{
                    interactionController.setInteraction(null);
                }
            }
            // replace old feature instance with new one in feature tables of participant
            else{
                getFeature().setParticipant(participantController.getParticipant());
                participantController.reloadSingleFeature(getFeature());
            }
        }
    }

    @Override
    protected void generalLoadChecks() {
        super.generalLoadChecks();
        generalPublicationLoadChecks();
    }

    @Override
    protected EditorCloner<FeatureEvidence, IntactFeatureEvidence> newClonerInstance() {
        return new FeatureEvidenceCloner();
    }

    @Override
    protected ExperimentalRange instantiateRange(Position start, Position end) {
        return new ExperimentalRange(start, end);
    }

    @Override
    protected ExperimentalResultingSequence instantiateResultingSequence(String original, String newSequence) {
        return new ExperimentalResultingSequence(original, newSequence);
    }

    public int getParametersSize() {
        if (getFeature() == null){
            return 0;
        }
        else {
            return getFeature().getParameters().size();
        }
    }

    public int getDetectionMethodsSize() {
        if (getFeature() == null){
            return 0;
        }
        else {
            return getFeature().getDetectionMethods().size();
        }
    }

    @Override
    public void refreshTabs(){
        super.refreshTabs();

        this.isParametersDisabled = true;
        this.isDetectionMethodDisabled = true;
    }

    @Override
    public void doPostSave() {
        // the feature was just created, add it to the list of features of the participant
        if (getFeature().getParticipant() != null){
            participantController.reloadSingleFeature(getFeature());
        }
    }

    /**
     * When reverting, we need to refresh the collection of wrappers because they are not part of the IntAct model.
     */
    @Override
    protected void postRevert() {
        // the feature was just created, add it to the list of features of the participant
        if (getFeature().getParticipant() != null){
            participantController.reloadSingleFeature(getFeature());
        }
    }

    @Override
    public String doDelete() {
        participantController.removeFeature(getFeature());
        return super.doDelete();
    }

    @Override
    protected FeatureEvidenceXref newXref(CvTerm db, String id, String secondaryId, String version, CvTerm qualifier) {
        FeatureEvidenceXref ref = new FeatureEvidenceXref(db, id, version, qualifier);
        ref.setSecondaryId(secondaryId);
        return ref;
    }

    @Override
    public FeatureEvidenceXref newXref(String db, String dbMI, String id, String secondaryId, String qualifier, String qualifierMI) {
        return new FeatureEvidenceXref(getCvService().findCvObject(IntactUtils.DATABASE_OBJCLASS, dbMI != null ? dbMI : db),
                id,
                secondaryId,
                getCvService().findCvObject(IntactUtils.QUALIFIER_OBJCLASS, qualifierMI != null ? qualifierMI : qualifier));
    }

    @Override
    public Collection<String> collectParentAcsOfCurrentAnnotatedObject(){
        Collection<String> parentAcs = new ArrayList<String>();

        if (getFeature().getParticipant() instanceof IntactParticipantEvidence){
            IntactParticipantEvidence comp = (IntactParticipantEvidence)getFeature().getParticipant();
            if (comp.getAc() != null){
                parentAcs.add(comp.getAc());
            }

            addParentAcsTo(parentAcs, comp);
        }

        return parentAcs;
    }

    /**
     * Get the publication ac of this participant if it exists, the ac of the interaction if it exists and the component ac if it exists and add it to the list or parentAcs
     * @param parentAcs
     * @param comp
     */
    private void addParentAcsTo(Collection<String> parentAcs, IntactParticipantEvidence comp) {
        if (comp.getInteraction() instanceof IntactInteractionEvidence){
            IntactInteractionEvidence inter = (IntactInteractionEvidence)comp.getInteraction();
            addParentAcsTo(parentAcs, inter);
        }
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

    public void onTabChanged(TabChangeEvent e) {

        // the xref tab is active
        super.onTabChanged(e);

        // all the tabs selectOneMenu are disabled, we can process the tabs specific to interaction
        if (isRangeDisabled() && isAliasDisabled() && isXrefDisabled() && isAnnotationTopicDisabled()){
            if (e.getTab().getId().equals("parametersTab")){
                isParametersDisabled = false;
                isDetectionMethodDisabled = true;
            }
            else {
                isParametersDisabled = true;
                isDetectionMethodDisabled = false;
            }
        }
        else {
            isParametersDisabled = true;
            isDetectionMethodDisabled = true;
        }

    }

    public boolean isParametersDisabled() {
        return isParametersDisabled;
    }

    public boolean isDetectionMethodDisabled() {
        return isDetectionMethodDisabled;
    }

    @Override
    public IntactDbSynchronizer getDbSynchronizer() {
        return getEditorService().getIntactDao().getSynchronizerContext().getFeatureEvidenceSynchronizer();
    }

    @Override
    public FeatureEvidenceAlias newAlias(CvTerm aliasType, String name) {
        return new FeatureEvidenceAlias(aliasType, name);
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
            getFeature().getParameters().add(param);
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

    public void newDetectionMethod(ActionEvent evt) {
        if (this.detectionMethodToAdd != null){
            getFeature().getDetectionMethods().add(this.detectionMethodToAdd);
            doSave(false);

            this.detectionMethodToAdd = null;
        }
    }

    @Override
    public FeatureEvidenceAlias newAlias(String alias, String aliasMI, String name) {
        return new FeatureEvidenceAlias(getCvService().findCvObject(IntactUtils.ALIAS_TYPE_OBJCLASS, aliasMI != null ? aliasMI : alias),
                name);
    }

    public void removeParameter(Parameter parameter) {
        getFeature().getParameters().remove(parameter);
    }

    public void removeDetectionMethod(CvTerm cv) {

        getFeature().getDetectionMethods().remove(cv);
    }

    public List<Parameter> collectParameters() {

        List<Parameter> variableParameters = new ArrayList<Parameter>(getFeature().getParameters());
        Collections.sort(variableParameters, new AuditableComparator());
        return variableParameters;
    }

    public List<CvTerm> collectDetectionMethods() {

        List<CvTerm> methods = new ArrayList<CvTerm>(getFeature().getDetectionMethods());
        Collections.sort(methods, new AuditableComparator());
        return methods;
    }

    @Override
    public FeatureEvidenceAnnotation newAnnotation(CvTerm annotation, String text) {
        return new FeatureEvidenceAnnotation(annotation, text);
    }

    @Override
    public FeatureEvidenceAnnotation newAnnotation(String topic, String topicMI, String text) {
        return new FeatureEvidenceAnnotation(getCvService().findCvObject(IntactUtils.TOPIC_OBJCLASS, topicMI != null ? topicMI: topic), text);
    }

    @Override
    protected IntactDbSynchronizer getRangeSynchronzer() {
        return getEditorService().getIntactDao().getSynchronizerContext().getExperimentalRangeSynchronizer();
    }

    public IntactCvTerm getDetectionMethodToAdd() {
        return detectionMethodToAdd;
    }

    public void setDetectionMethodToAdd(IntactCvTerm detectionMethodToAdd) {
        this.detectionMethodToAdd = detectionMethodToAdd;
    }

    @Override
    protected void postProcessDeletedEvent(UnsavedChange unsaved) {
        super.postProcessDeletedEvent(unsaved);
        participantController.reloadSingleFeature(getFeature());
    }

    @Override
    public void newRange(ActionEvent evt) {
        super.newRange(evt);
        participantController.reloadSingleFeature(getFeature());
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


}