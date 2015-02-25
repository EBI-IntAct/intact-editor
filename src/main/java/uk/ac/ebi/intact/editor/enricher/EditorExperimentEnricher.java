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
import psidev.psi.mi.jami.enricher.OrganismEnricher;
import psidev.psi.mi.jami.enricher.PublicationEnricher;
import psidev.psi.mi.jami.enricher.exception.EnricherException;
import psidev.psi.mi.jami.enricher.listener.ExperimentEnricherListener;
import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.Experiment;
import uk.ac.ebi.intact.editor.services.enricher.DbEnricherService;
import uk.ac.ebi.intact.jami.model.extension.ExperimentAnnotation;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.annotation.Resource;
import java.util.Collection;

/**
 * Editor enricher for experiments when importing files
 *
 */
public class EditorExperimentEnricher implements ExperimentEnricher {

    @Resource(name = "intactExperimentEnricher")
    private ExperimentEnricher intactExperimentEnricher;
    @Resource(name = "editorOrganismEnricher")
    private OrganismEnricher editorOrganismEnricher;
    @Resource(name = "editorMiEnricher")
    private CvTermEnricher<CvTerm> editorMiEnricher;
    @Resource(name = "editorPublicationEnricher")
    private PublicationEnricher editorPublicationEnricher;
    @Resource(name = "dbEnricherService")
    private DbEnricherService dbEnricherService;

    private String importTag;

    public EditorExperimentEnricher() {
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
    public CvTermEnricher<CvTerm> getCvTermEnricher() {
        return editorMiEnricher;
    }

    @Override
    public PublicationEnricher getPublicationEnricher() {
        return editorPublicationEnricher;
    }

    @Override
    public ExperimentEnricherListener getExperimentEnricherListener() {
        return intactExperimentEnricher.getExperimentEnricherListener();
    }

    @Override
    public void setOrganismEnricher(OrganismEnricher organismEnricher) {
        editorOrganismEnricher = organismEnricher;
    }

    @Override
    public void setCvTermEnricher(CvTermEnricher<CvTerm> cvEnricher) {
        editorMiEnricher = cvEnricher;
    }

    @Override
    public void setPublicationEnricher(PublicationEnricher publicationEnricher) {
       editorPublicationEnricher = publicationEnricher;
    }

    @Override
    public void setExperimentEnricherListener(ExperimentEnricherListener listener) {
        intactExperimentEnricher.setExperimentEnricherListener(listener);
    }

    @Override
    public void enrich(Experiment object) throws EnricherException {
        intactExperimentEnricher.setPublicationEnricher(editorPublicationEnricher);
        intactExperimentEnricher.setCvTermEnricher(editorMiEnricher);
        intactExperimentEnricher.setOrganismEnricher(editorOrganismEnricher);

        intactExperimentEnricher.enrich(object);

        if (getImportTag() != null && object != null){
            // check if object exists in database before adding a tag
            if (dbEnricherService.isNewExperiment(object)){
                object.getAnnotations().add(new ExperimentAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
            }
        }
    }

    @Override
    public void enrich(Collection<Experiment> objects) throws EnricherException {
        for (Experiment exp : objects){
            enrich(exp);
        }
    }

    @Override
    public void enrich(Experiment object, Experiment objectSource) throws EnricherException {
        intactExperimentEnricher.setPublicationEnricher(editorPublicationEnricher);
        intactExperimentEnricher.setCvTermEnricher(editorMiEnricher);
        intactExperimentEnricher.setOrganismEnricher(editorOrganismEnricher);

        intactExperimentEnricher.enrich(object, objectSource);

        if (getImportTag() != null && object != null){
            // check if object exists in database before adding a tag
            if (dbEnricherService.isNewExperiment(object)){
                object.getAnnotations().add(new ExperimentAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
            }
        }
    }
}
