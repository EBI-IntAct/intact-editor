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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.ModelledFeature;
import psidev.psi.mi.jami.model.Participant;
import uk.ac.ebi.intact.editor.controller.curate.AnnotatedObjectController;
import uk.ac.ebi.intact.editor.controller.curate.UnsavedChange;
import uk.ac.ebi.intact.editor.controller.curate.cloner.EditorCloner;
import uk.ac.ebi.intact.editor.controller.curate.cloner.ModelledFeatureCloner;
import uk.ac.ebi.intact.editor.controller.curate.cloner.ModelledParticipantCloner;
import uk.ac.ebi.intact.editor.controller.curate.complex.ComplexController;
import uk.ac.ebi.intact.editor.controller.curate.interaction.FeatureWrapper;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.synchronizer.IntactDbSynchronizer;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.faces.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Modelled Participant controller.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope( "conversation.access" )
@ConversationName( "general" )
public class ModelledParticipantController extends AbstractParticipantController<IntactModelledParticipant> {

    private static final Log log = LogFactory.getLog( ModelledParticipantController.class );

    @Autowired
    private ComplexController interactionController;

    @Override
    public Class<IntactModelledParticipant> getParticipantClass() {
        return IntactModelledParticipant.class;
    }

    @Override
    protected EditorCloner newFeatureClonerInstance() {
        return new ModelledFeatureCloner();
    }

    @Override
    protected void generalLoadChecks() {
        super.generalLoadChecks();
        generalComplexLoadChecks();
    }

    @Override
    protected void refreshParentControllers() {
        // different loaded interaction
        if (interactionController.getComplex() != getParticipant().getInteraction()){
            // different participant to load
            if (interactionController.getAc() == null ||
                    (getParticipant().getInteraction() instanceof IntactComplex
                            && !interactionController.getAc().equals(((IntactComplex)getParticipant().getInteraction()).getAc()))){
                IntactComplex intactInteraction = (IntactComplex)getParticipant().getInteraction();
                interactionController.setComplex(intactInteraction);
            }
            // replace old feature instance with new one in feature tables of participant
            else{
                getParticipant().setInteraction(interactionController.getComplex());
                interactionController.reloadSingleParticipant(getParticipant());
            }
        }
    }

    @Override
    public Collection<String> collectParentAcsOfCurrentAnnotatedObject(){
        Collection<String> parentAcs = new ArrayList<String>();

        if (getParticipant().getInteraction() != null){
            addParentAcsTo(parentAcs, (IntactComplex)getParticipant().getInteraction());
        }

        return parentAcs;
    }

    /**
     * Add all the parent acs of this interaction
     * @param parentAcs
     * @param inter
     */
    protected void addParentAcsTo(Collection<String> parentAcs, IntactComplex inter) {
        if (inter.getAc() != null){
            parentAcs.add(inter.getAc());
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
    public void addInteractorToParticipant(ActionEvent evt) {
        super.addInteractorToParticipant(evt);
        interactionController.reloadSingleParticipant(getParticipant());

    }

    @Override
    protected AnnotatedObjectController getParentController() {
        return interactionController;
    }

    @Override
    protected String getPageContext() {
        return "cparticipant";
    }

    @Override
    protected ModelledParticipantXref newXref(CvTerm db, String id, String secondaryId, String version, CvTerm qualifier) {
        ModelledParticipantXref ref = new ModelledParticipantXref(db, id, version, qualifier);
        ref.setSecondaryId(secondaryId);
        return ref;
    }

    @Override
    public ModelledParticipantXref newXref(String db, String dbMI, String id, String secondaryId, String qualifier, String qualifierMI) {
        return new ModelledParticipantXref(getCvService().findCvObject(IntactUtils.DATABASE_OBJCLASS, dbMI != null ? dbMI : db),
                id,
                secondaryId,
                getCvService().findCvObject(IntactUtils.QUALIFIER_OBJCLASS, qualifierMI != null ? qualifierMI : qualifier));
    }

    @Override
    public ModelledParticipantAnnotation newAnnotation(CvTerm annotation, String text) {
        return new ModelledParticipantAnnotation(annotation, text);
    }

    @Override
    public ModelledParticipantAnnotation newAnnotation(String topic, String topicMI, String text) {
        return new ModelledParticipantAnnotation(getCvService().findCvObject(IntactUtils.TOPIC_OBJCLASS, topicMI != null ? topicMI: topic), text);
    }

    @Override
    public ModelledParticipantAlias newAlias(CvTerm aliasType, String name) {
        return new ModelledParticipantAlias(aliasType, name);
    }

    @Override
    public ModelledParticipantAlias newAlias(String alias, String aliasMI, String name) {
        return new ModelledParticipantAlias(getCvService().findCvObject(IntactUtils.ALIAS_TYPE_OBJCLASS, aliasMI != null ? aliasMI : alias),
                name);
    }

    @Override
    public IntactDbSynchronizer getDbSynchronizer() {
        return getEditorService().getIntactDao().getSynchronizerContext().getModelledParticipantSynchronizer();
    }

    @Override
    protected EditorCloner<Participant, IntactModelledParticipant> newClonerInstance() {
        return new ModelledParticipantCloner();
    }

    public void reloadSingleFeature(IntactModelledFeature f){
        // only update if not lazy loaded
        if (getParticipant().areFeaturesInitialized()){
            Iterator<ModelledFeature> evIterator = getParticipant().getFeatures().iterator();
            boolean add = true;
            while (evIterator.hasNext()){
                IntactModelledFeature intactEv = (IntactModelledFeature)evIterator.next();
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

    public void removeFeature(IntactModelledFeature f){
        // only update if not lazy loaded
        if (getParticipant().areFeaturesInitialized()){
            Iterator<ModelledFeature> evIterator = getParticipant().getFeatures().iterator();
            while (evIterator.hasNext()){
                IntactModelledFeature intactEv = (IntactModelledFeature)evIterator.next();
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
    protected void postProcessDeletedEvent(UnsavedChange unsaved) {
        super.postProcessDeletedEvent(unsaved);
        if (unsaved.getUnsavedObject() instanceof IntactModelledFeature){
            removeFeature((IntactModelledFeature)unsaved.getUnsavedObject());
        }
        interactionController.reloadSingleParticipant(getParticipant());
    }

    @Override
    public void unlinkFeature(FeatureWrapper wrapper) {
        super.unlinkFeature(wrapper);
        interactionController.reloadSingleParticipant(getParticipant());
    }
}