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
package uk.ac.ebi.intact.editor.controller.curate.cloner;

import psidev.psi.mi.jami.model.*;
import uk.ac.ebi.intact.jami.dao.IntactDao;
import uk.ac.ebi.intact.jami.model.extension.*;

/**
 * Editor specific cloning routine for biological complexes participants.
 *
 * @author Prem Anand (prem@ebi.ac.uk)
 * @version $Id: InteractionIntactCloner.java 14783 2010-07-29 12:52:28Z brunoaranda $
 * @since 2.0.1-SNAPSHOT
 */
public class ModelledParticipantCloner extends AbstractEditorCloner<Participant, IntactModelledParticipant> {

    private EditorCloner<Feature, IntactModelledFeature> featureCloner;

    public IntactModelledParticipant clone(Participant participant, IntactDao dao) {
        IntactModelledParticipant clone = new IntactModelledParticipant(participant.getInteractor());
        // set current user
        initAuditProperties(clone, dao);

        clone.setBiologicalRole(participant.getBiologicalRole());

        if (participant.getInteraction() instanceof Complex){
            clone.setInteraction((Complex)participant.getInteraction());
        }
        clone.setStoichiometry(new IntactStoichiometry(participant.getStoichiometry().getMinValue(), participant.getStoichiometry().getMaxValue()));

        for (Object obj : participant.getAliases()){
            Alias alias = (Alias)obj;
            clone.getAliases().add(new ModelledParticipantAlias(alias.getType(), alias.getName()));
        }

        for (Object obj : participant.getXrefs()){
            Xref ref = (Xref)obj;
            AbstractIntactXref intactRef = new ModelledParticipantXref(ref.getDatabase(), ref.getId(), ref.getVersion(), ref.getQualifier());
            if (ref instanceof AbstractIntactXref){
                intactRef.setSecondaryId(((AbstractIntactXref) ref).getSecondaryId());
            }
            clone.getXrefs().add(intactRef);
        }

        for (Object obj : participant.getAnnotations()){
            Annotation annotation = (Annotation)obj;
            clone.getAnnotations().add(new ModelledParticipantAnnotation(annotation.getTopic(), annotation.getValue()));
        }

        for (Object obj : participant.getFeatures()){
            Feature feature = (Feature)obj;
            ModelledFeature r = getFeatureCloner().clone(feature, dao);
            clone.addFeature(r);
        }

        // don't need to add it to the feature component because it is already done by the cloner
        return clone;
    }

    @Override
    public void copyInitialisedProperties(IntactModelledParticipant source, IntactModelledParticipant target) {
        target.setBiologicalRole(source.getBiologicalRole());
        target.setStoichiometry(source.getStoichiometry());
        target.setInteraction(source.getInteraction());
        if (source.areAliasesInitialized()){
            target.getAliases().clear();
            target.getAliases().addAll(source.getAliases());
        }

        if (source.areXrefsInitialized()){
            target.getXrefs().clear();
            target.getXrefs().addAll(source.getXrefs());
        }

        if (source.areAnnotationsInitialized()){
            target.getAnnotations().clear();
            target.getAnnotations().addAll(source.getAnnotations());
        }

        if (source.areFeaturesInitialized()){
           target.getFeatures().clear();
            target.addAllFeatures(source.getFeatures());
        }
    }

    public EditorCloner<Feature, IntactModelledFeature> getFeatureCloner() {
        if (this.featureCloner == null){
            this.featureCloner = new ModelledFeatureCloner();
        }
        return featureCloner;
    }

    protected void setFeatureCloner(EditorCloner<Feature, IntactModelledFeature> featureCloner) {
        this.featureCloner = featureCloner;
    }
}

