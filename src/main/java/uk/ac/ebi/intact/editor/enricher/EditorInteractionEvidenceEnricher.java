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
import psidev.psi.mi.jami.enricher.ExperimentEnricher;
import psidev.psi.mi.jami.enricher.InteractionEvidenceEnricher;
import psidev.psi.mi.jami.enricher.exception.EnricherException;
import psidev.psi.mi.jami.enricher.listener.InteractionEnricherListener;
import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.InteractionEvidence;
import uk.ac.ebi.intact.editor.services.enricher.DbEnricherService;
import uk.ac.ebi.intact.jami.model.extension.InteractionAnnotation;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.annotation.Resource;
import java.util.Collection;

/**
 * IntAct enricher for interaction evidence
 *
 */
public class EditorInteractionEvidenceEnricher implements InteractionEvidenceEnricher {

    @Resource(name = "dbEnricherService")
    private DbEnricherService dbEnricherService;
    @Resource(name = "intactInteractionEvidenceEnricher")
    private InteractionEvidenceEnricher intactInteractionEvidenceEnricher;
    @Resource(name = "editorExperimentEnricher")
    private ExperimentEnricher editorExperimentEnricher;
    @Resource(name = "editorComponentEnricher")
    private psidev.psi.mi.jami.enricher.ParticipantEnricher editorParticipantEvidenceEnricher;
    @Resource(name = "editorMiEnricher")
    private CvTermEnricher<CvTerm> editorMiEnricher;

    private String importTag;

    @Override
    public ExperimentEnricher getExperimentEnricher() {
        return editorExperimentEnricher;
    }

    @Override
    public void setExperimentEnricher(ExperimentEnricher enricher) {
         editorExperimentEnricher = enricher;
    }

    @Override
    public psidev.psi.mi.jami.enricher.ParticipantEnricher getParticipantEnricher() {
        return editorParticipantEvidenceEnricher;
    }

    @Override
    public CvTermEnricher<CvTerm> getCvTermEnricher() {
        return editorMiEnricher;
    }

    @Override
    public InteractionEnricherListener<InteractionEvidence> getInteractionEnricherListener() {
        return this.intactInteractionEvidenceEnricher.getInteractionEnricherListener();
    }

    @Override
    public void setParticipantEnricher(psidev.psi.mi.jami.enricher.ParticipantEnricher enricher) {
       this.editorParticipantEvidenceEnricher = enricher;
    }

    @Override
    public void setCvTermEnricher(CvTermEnricher<CvTerm> enricher) {
        this.editorMiEnricher = enricher;
    }

    @Override
    public void setInteractionEnricherListener(InteractionEnricherListener<InteractionEvidence> listener) {
         this.intactInteractionEvidenceEnricher.setInteractionEnricherListener(listener);
    }

    @Override
    public void enrich(InteractionEvidence object) throws EnricherException {
        intactInteractionEvidenceEnricher.setExperimentEnricher(editorExperimentEnricher);
        intactInteractionEvidenceEnricher.setCvTermEnricher(editorMiEnricher);
        intactInteractionEvidenceEnricher.setParticipantEnricher(editorParticipantEvidenceEnricher);

        intactInteractionEvidenceEnricher.enrich(object);

        if (getImportTag() != null && object != null){
            // check if object exists in database before adding a tag
            if (dbEnricherService.isNewInteractionEvidence(object)){
                object.getAnnotations().add(new InteractionAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
            }
        }
    }

    @Override
    public void enrich(Collection<InteractionEvidence> objects) throws EnricherException {
        for (InteractionEvidence interaction : objects){
            enrich(interaction);
        }
    }

    @Override
    public void enrich(InteractionEvidence object, InteractionEvidence objectSource) throws EnricherException {
        intactInteractionEvidenceEnricher.setExperimentEnricher(editorExperimentEnricher);
        intactInteractionEvidenceEnricher.setCvTermEnricher(editorMiEnricher);
        intactInteractionEvidenceEnricher.setParticipantEnricher(editorParticipantEvidenceEnricher);

        intactInteractionEvidenceEnricher.enrich(object, objectSource);

        if (getImportTag() != null && object != null){
            // check if object exists in database before adding a tag
            if (dbEnricherService.isNewInteractionEvidence(object)){
                object.getAnnotations().add(new InteractionAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
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
