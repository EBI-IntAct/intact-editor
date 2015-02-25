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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.Feature;
import psidev.psi.mi.jami.model.Position;
import uk.ac.ebi.intact.editor.controller.curate.AnnotatedObjectController;
import uk.ac.ebi.intact.editor.controller.curate.UnsavedChange;
import uk.ac.ebi.intact.editor.controller.curate.cloner.EditorCloner;
import uk.ac.ebi.intact.editor.controller.curate.cloner.ModelledFeatureCloner;
import uk.ac.ebi.intact.editor.controller.curate.complex.ComplexController;
import uk.ac.ebi.intact.editor.controller.curate.participant.ModelledParticipantController;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.synchronizer.IntactDbSynchronizer;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.faces.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Modelled Feature controller.
 *
 */
@Controller
@Scope( "conversation.access" )
@ConversationName( "general" )
public class ModelledFeatureController extends AbstractFeatureController<IntactModelledFeature> {

    private static final Log log = LogFactory.getLog( ModelledFeatureController.class );

    @Autowired
    private ComplexController complexController;

    @Autowired
    private ModelledParticipantController modelledParticipantController;

    @Override
    public Class<IntactModelledFeature> getFeatureClass() {
        return IntactModelledFeature.class;
    }

    @Override
    public Class<? extends AbstractIntactResultingSequence> getResultingSequenceClass() {
        return ModelledResultingSequence.class;
    }

    @Override
    public Class<? extends AbstractIntactXref> getResultingSequenceXrefClass() {
        return ModelledResultingSequenceXref.class;
    }

    @Override
    protected AnnotatedObjectController getParentController() {
        return modelledParticipantController;
    }

    @Override
    protected String getPageContext() {
        return "cfeature";
    }

    @Override
    protected EditorCloner<Feature, IntactModelledFeature> newClonerInstance() {
        return new ModelledFeatureCloner();
    }

    @Override
    protected ModelledFeatureXref newXref(CvTerm db, String id, String secondaryId, String version, CvTerm qualifier) {
        ModelledFeatureXref ref = new ModelledFeatureXref(db, id, version, qualifier);
        ref.setSecondaryId(secondaryId);
        return ref;
    }

    @Override
    public ModelledFeatureXref newXref(String db, String dbMI, String id, String secondaryId, String qualifier, String qualifierMI) {
        return new ModelledFeatureXref(getCvService().findCvObject(IntactUtils.DATABASE_OBJCLASS, dbMI != null ? dbMI : db),
                id,
                secondaryId,
                getCvService().findCvObject(IntactUtils.QUALIFIER_OBJCLASS, qualifierMI != null ? qualifierMI : qualifier));
    }

    @Override
    public ModelledFeatureAnnotation newAnnotation(CvTerm annotation, String text) {
        return new ModelledFeatureAnnotation(annotation, text);
    }

    @Override
    public ModelledFeatureAnnotation newAnnotation(String topic, String topicMI, String text) {
        return new ModelledFeatureAnnotation(getCvService().findCvObject(IntactUtils.TOPIC_OBJCLASS, topicMI != null ? topicMI: topic), text);
    }

    @Override
    public ModelledFeatureAlias newAlias(CvTerm aliasType, String name) {
        return new ModelledFeatureAlias(aliasType, name);
    }

    @Override
    public ModelledFeatureAlias newAlias(String alias, String aliasMI, String name) {
        return new ModelledFeatureAlias(getCvService().findCvObject(IntactUtils.ALIAS_TYPE_OBJCLASS, aliasMI != null ? aliasMI : alias),
                name);
    }

    @Override
    public Collection<String> collectParentAcsOfCurrentAnnotatedObject() {
        Collection<String> parentAcs = new ArrayList<String>();

        if (getFeature().getParticipant() instanceof IntactModelledParticipant){
            IntactModelledParticipant comp = (IntactModelledParticipant)getFeature().getParticipant();
            if (comp.getAc() != null){
                parentAcs.add(comp.getAc());
            }

            addParentAcsTo(parentAcs, comp);
        }

        return parentAcs;
    }

    private void addParentAcsTo(Collection<String> parentAcs, IntactModelledParticipant comp) {
        if (comp.getInteraction() instanceof IntactComplex){
            IntactComplex inter = (IntactComplex)comp.getInteraction();
            addParentAcsTo(parentAcs, inter);
        }
    }

    protected void addParentAcsTo(Collection<String> parentAcs, IntactComplex inter) {
        if (inter.getAc() != null){
            parentAcs.add(inter.getAc());
        }
    }

    @Override
    public IntactDbSynchronizer getDbSynchronizer() {
        return getEditorService().getIntactDao().getSynchronizerContext().getModelledFeatureSynchronizer();
    }

    @Override
    protected void refreshParentControllers() {
        // different loaded participant
        if (modelledParticipantController.getParticipant() != getFeature().getParticipant()){
            // different participant to load
            if (modelledParticipantController.getAc() == null ||
                    (getFeature().getParticipant() instanceof IntactModelledParticipant
                            && !modelledParticipantController.getAc().equals(((IntactModelledParticipant)getFeature().getParticipant()).getAc()))){
                IntactModelledParticipant intactParticipant = (IntactModelledParticipant)getFeature().getParticipant();
                modelledParticipantController.setParticipant(intactParticipant);

                // reload other parents
                if( intactParticipant.getInteraction() instanceof IntactComplex ) {
                    final IntactComplex interaction = (IntactComplex)intactParticipant.getInteraction();
                    complexController.setComplex( interaction );
                }
                else{
                    complexController.setComplex(null);
                }
            }
            // replace old feature instance with new one in feature tables of participant
            else{
                getFeature().setParticipant(modelledParticipantController.getParticipant());
                modelledParticipantController.reloadSingleFeature(getFeature());
            }
        }
    }

    @Override
    protected ModelledRange instantiateRange(Position start, Position end) {
        return new ModelledRange(start, end);
    }

    @Override
    protected ModelledResultingSequence instantiateResultingSequence(String original, String newSequence) {
        return new ModelledResultingSequence(original, newSequence);
    }

    @Override
    protected IntactDbSynchronizer getRangeSynchronzer() {
        return getEditorService().getIntactDao().getSynchronizerContext().getModelledRangeSynchronizer();
    }

    @Override
    public String doDelete() {
        modelledParticipantController.removeFeature(getFeature());
        return super.doDelete();
    }

    @Override
    public void doPostSave() {
        // the feature was just created, add it to the list of features of the participant
        if (getFeature().getParticipant() != null){
            modelledParticipantController.reloadSingleFeature(getFeature());
        }
    }

    @Override
    public void postRevert() {
        // the feature was just created, add it to the list of features of the participant
        if (getFeature().getParticipant() != null){
            modelledParticipantController.reloadSingleFeature(getFeature());
        }
    }

    @Override
    protected void generalLoadChecks() {
        super.generalLoadChecks();
        generalComplexLoadChecks();
    }

    @Override
    protected void postProcessDeletedEvent(UnsavedChange unsaved) {
        super.postProcessDeletedEvent(unsaved);
        modelledParticipantController.reloadSingleFeature(getFeature());
    }

    @Override
    public void newRange(ActionEvent evt) {
        super.newRange(evt);
        modelledParticipantController.reloadSingleFeature(getFeature());
    }
}