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

import psidev.psi.mi.jami.bridges.fetcher.PublicationFetcher;
import psidev.psi.mi.jami.enricher.CuratedPublicationEnricher;
import psidev.psi.mi.jami.enricher.SourceEnricher;
import psidev.psi.mi.jami.enricher.exception.EnricherException;
import psidev.psi.mi.jami.enricher.listener.PublicationEnricherListener;
import psidev.psi.mi.jami.model.Publication;
import uk.ac.ebi.intact.editor.services.enricher.DbEnricherService;
import uk.ac.ebi.intact.jami.model.extension.PublicationAnnotation;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.annotation.Resource;
import java.util.Collection;

/**
 * Editor extension of publication enricher for importing files
 *
 */
public class EditorPublicationEnricher implements CuratedPublicationEnricher {

    @Resource(name = "dbEnricherService")
    private DbEnricherService dbEnricherService;
    @Resource(name = "intactPublicationEnricher")
    private CuratedPublicationEnricher intactPublicationEnricher;
    @Resource(name = "editorSourceEnricher")
    private SourceEnricher editorSourceEnricher;

    private String importTag;

    @Override
    public PublicationFetcher getPublicationFetcher() {
        return intactPublicationEnricher.getPublicationFetcher();
    }

    @Override
    public PublicationEnricherListener getPublicationEnricherListener() {
        return intactPublicationEnricher.getPublicationEnricherListener();
    }

    @Override
    public void setPublicationEnricherListener(PublicationEnricherListener listener) {
        intactPublicationEnricher.setPublicationEnricherListener(listener);
    }

    @Override
    public void enrich(Publication object) throws EnricherException {
        intactPublicationEnricher.setSourceEnricher(editorSourceEnricher);

        intactPublicationEnricher.enrich(object);

        if (getImportTag() != null && object != null){
            // check if object exists in database before adding a tag
            if (dbEnricherService.isNewPublication(object)){
                object.getAnnotations().add(new PublicationAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
            }
        }
    }

    @Override
    public void enrich(Collection<Publication> objects) throws EnricherException {
        for (Publication pub : objects){
             enrich(pub);
        }
    }

    @Override
    public void enrich(Publication object, Publication objectSource) throws EnricherException {
        intactPublicationEnricher.setSourceEnricher(editorSourceEnricher);

        intactPublicationEnricher.enrich(object, objectSource);


        if (getImportTag() != null && object != null){
            // check if object exists in database before adding a tag
            if (dbEnricherService.isNewPublication(object)){
                object.getAnnotations().add(new PublicationAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
            }
        }
    }

    public String getImportTag() {
        return importTag;
    }

    public void setImportTag(String importTag) {
        this.importTag = importTag;
    }

    @Override
    public SourceEnricher getSourceEnricher() {
        return editorSourceEnricher;
    }

    @Override
    public void setSourceEnricher(SourceEnricher enricher) {
         enricher = editorSourceEnricher;
    }
}
