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
import psidev.psi.mi.jami.enricher.OrganismEnricher;
import psidev.psi.mi.jami.enricher.ParticipantEvidenceEnricher;
import psidev.psi.mi.jami.enricher.exception.EnricherException;
import psidev.psi.mi.jami.enricher.impl.CompositeInteractorEnricher;
import psidev.psi.mi.jami.enricher.listener.EntityEnricherListener;
import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.FeatureEvidence;
import psidev.psi.mi.jami.model.ParticipantEvidence;
import uk.ac.ebi.intact.editor.services.enricher.DbEnricherService;
import uk.ac.ebi.intact.jami.model.extension.InteractorAnnotation;
import uk.ac.ebi.intact.jami.model.extension.ParticipantEvidenceAnnotation;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.annotation.Resource;
import java.util.Collection;

/**
 * Editor participant evidence enricher for importes
 */
public class EditorComponentEnricher implements ParticipantEvidenceEnricher<ParticipantEvidence>{

    @Resource(name = "intactParticipantEvidenceEnricher")
    private  ParticipantEvidenceEnricher intactParticipantEvidenceEnricher;
    @Resource(name = "dbEnricherService")
    private DbEnricherService dbEnricherService;
    @Resource(name = "editorOrganismEnricher")
    private OrganismEnricher editorOrganismEnricher;
    @Resource(name = "editorMiEnricher")
    private CvTermEnricher<CvTerm> editorMiEnricher;
    @Resource(name = "editorFeatureEvidenceEnricher")
    private FeatureEnricher<FeatureEvidence> editorFeatureEvidenceEnricher;

    private String importTag;

    public EditorComponentEnricher() {
    }

    public String getImportTag() {
        return importTag;
    }

    public void setImportTag(String importTag) {
        this.importTag = importTag;
    }

    @Override
    public OrganismEnricher getOrganismEnricher() {
        return editorOrganismEnricher;
    }

    @Override
    public void setOrganismEnricher(OrganismEnricher enricher) {
        editorOrganismEnricher = enricher;
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
        return intactParticipantEvidenceEnricher.getInteractorEnricher();
    }

    @Override
    public FeatureEnricher<FeatureEvidence> getFeatureEnricher() {
        return editorFeatureEvidenceEnricher;
    }

    @Override
    public EntityEnricherListener getParticipantEnricherListener() {
        return null;
    }

    @Override
    public void setInteractorEnricher(CompositeInteractorEnricher interactorEnricher) {
        intactParticipantEvidenceEnricher.setInteractorEnricher(interactorEnricher);
    }

    @Override
    public void setFeatureEnricher(FeatureEnricher<FeatureEvidence> enricher) {
         editorFeatureEvidenceEnricher = enricher;
    }

    @Override
    public void setParticipantEnricherListener(EntityEnricherListener listener) {
        this.intactParticipantEvidenceEnricher.setParticipantEnricherListener(listener);
    }

    @Override
    public void enrich(Collection<ParticipantEvidence> objects) throws EnricherException {
         for (ParticipantEvidence p : objects){
             enrich(p);
         }
    }

    @Override
    public void enrich(ParticipantEvidence objectToEnrich, ParticipantEvidence objectSource) throws EnricherException {
        intactParticipantEvidenceEnricher.setOrganismEnricher(editorOrganismEnricher);
        intactParticipantEvidenceEnricher.setCvTermEnricher(editorMiEnricher);
        intactParticipantEvidenceEnricher.setFeatureEnricher(editorFeatureEvidenceEnricher);

        intactParticipantEvidenceEnricher.enrich(objectToEnrich, objectSource);

        if (getImportTag() != null && objectToEnrich != null){
            // check if object exists in database before adding a tag
            if (dbEnricherService.isNewParticipantEvidence(objectToEnrich)){
                objectToEnrich.getAnnotations().add(new ParticipantEvidenceAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
            }

            // check interactor
            if (dbEnricherService.isNewInteractor(objectToEnrich.getInteractor())){
                objectToEnrich.getInteractor().getAnnotations().add(new InteractorAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
            }
        }
    }

    @Override
    public void enrich(ParticipantEvidence objectToEnrich) throws EnricherException {
        intactParticipantEvidenceEnricher.setOrganismEnricher(editorOrganismEnricher);
        intactParticipantEvidenceEnricher.setCvTermEnricher(editorMiEnricher);
        intactParticipantEvidenceEnricher.setFeatureEnricher(editorFeatureEvidenceEnricher);

        intactParticipantEvidenceEnricher.enrich(objectToEnrich);

        if (getImportTag() != null && objectToEnrich != null){
            // check if object exists in database before adding a tag
            if (dbEnricherService.isNewParticipantEvidence(objectToEnrich)){
                objectToEnrich.getAnnotations().add(new ParticipantEvidenceAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
            }

            // check interactor
            if (dbEnricherService.isNewInteractor(objectToEnrich.getInteractor())){
                objectToEnrich.getInteractor().getAnnotations().add(new InteractorAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
            }
        }
    }
}
