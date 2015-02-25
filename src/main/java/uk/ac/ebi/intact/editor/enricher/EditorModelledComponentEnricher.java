/*
 * Copyright 2001-2007 The European Bioinformatics Institute.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.intact.editor.enricher;

import psidev.psi.mi.jami.enricher.CvTermEnricher;
import psidev.psi.mi.jami.enricher.FeatureEnricher;
import psidev.psi.mi.jami.enricher.ParticipantEnricher;
import psidev.psi.mi.jami.enricher.exception.EnricherException;
import psidev.psi.mi.jami.enricher.impl.CompositeInteractorEnricher;
import psidev.psi.mi.jami.enricher.listener.EntityEnricherListener;
import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.ModelledFeature;
import psidev.psi.mi.jami.model.ModelledParticipant;
import uk.ac.ebi.intact.editor.services.enricher.DbEnricherService;
import uk.ac.ebi.intact.jami.model.extension.ModelledParticipantAnnotation;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.annotation.Resource;
import java.util.Collection;

/**
 * Editor enricher of modelled participants
 */
public class EditorModelledComponentEnricher implements psidev.psi.mi.jami.enricher.ParticipantEnricher<ModelledParticipant, ModelledFeature> {
    @Resource(name = "intactModelledParticipantEnricher")
    private ParticipantEnricher<ModelledParticipant, ModelledFeature> intactModelledParticipantEnricher;
    @Resource(name = "dbEnricherService")
    private DbEnricherService dbEnricherService;
    @Resource(name = "editorMiEnricher")
    private CvTermEnricher<CvTerm> editorMiEnricher;
    @Resource(name = "editorModelledFeatureEnricher")
    private FeatureEnricher<ModelledFeature> editorModelledFeatureEnricher;

    private String importTag;

    public EditorModelledComponentEnricher() {
    }


    @Override
    public CvTermEnricher<CvTerm> getCvTermEnricher() {
        return editorMiEnricher;
    }

    @Override
    public void setCvTermEnricher(CvTermEnricher<CvTerm> enricher) {
         editorMiEnricher = enricher;
    }

    @Override
    public CompositeInteractorEnricher getInteractorEnricher() {
        return intactModelledParticipantEnricher.getInteractorEnricher();
    }

    @Override
    public FeatureEnricher<ModelledFeature> getFeatureEnricher() {
        return editorModelledFeatureEnricher;
    }

    @Override
    public EntityEnricherListener getParticipantEnricherListener() {
        return intactModelledParticipantEnricher.getParticipantEnricherListener();
    }

    @Override
    public void setInteractorEnricher(CompositeInteractorEnricher interactorEnricher) {
         intactModelledParticipantEnricher.setInteractorEnricher(interactorEnricher);
    }

    @Override
    public void setFeatureEnricher(FeatureEnricher<ModelledFeature> enricher) {
        editorModelledFeatureEnricher = enricher;
    }

    @Override
    public void setParticipantEnricherListener(EntityEnricherListener listener) {
         intactModelledParticipantEnricher.setParticipantEnricherListener(listener);
    }

    @Override
    public void enrich(ModelledParticipant objectToEnrich) throws EnricherException {
        intactModelledParticipantEnricher.setCvTermEnricher(editorMiEnricher);
        intactModelledParticipantEnricher.setFeatureEnricher(editorModelledFeatureEnricher);

        intactModelledParticipantEnricher.enrich(objectToEnrich);

        if (getImportTag() != null && objectToEnrich != null){
            // check if object exists in database before adding a tag
            if (dbEnricherService.isNewModelledParticipant(objectToEnrich)){
                objectToEnrich.getAnnotations().add(new ModelledParticipantAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
            }

            // check interactor
            if (dbEnricherService.isNewInteractor(objectToEnrich.getInteractor())){
                objectToEnrich.getInteractor().getAnnotations().add(new ModelledParticipantAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
            }
        }
    }

    @Override
    public void enrich(Collection<ModelledParticipant> objects) throws EnricherException {
        for (ModelledParticipant p : objects){
            enrich(p);
        }
    }

    @Override
    public void enrich(ModelledParticipant objectToEnrich, ModelledParticipant objectSource) throws EnricherException {
        intactModelledParticipantEnricher.setCvTermEnricher(editorMiEnricher);
        intactModelledParticipantEnricher.setFeatureEnricher(editorModelledFeatureEnricher);

        intactModelledParticipantEnricher.enrich(objectToEnrich, objectSource);

        if (getImportTag() != null && objectToEnrich != null){
            // check if object exists in database before adding a tag
            if (dbEnricherService.isNewModelledParticipant(objectToEnrich)){
                objectToEnrich.getAnnotations().add(new ModelledParticipantAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
            }

            // check interactor
            if (dbEnricherService.isNewInteractor(objectToEnrich.getInteractor())){
                objectToEnrich.getInteractor().getAnnotations().add(new ModelledParticipantAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
            }
        }
    }

    public String getImportTag() {
        return importTag;
    }

    public void setImportTag(String importTag) {
        this.importTag = importTag;
    }
}
