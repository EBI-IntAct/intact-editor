/*
 * Copyright 2001-2008 The European Bioinformatics Institute.
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

import psidev.psi.mi.jami.bridges.fetcher.CvTermFetcher;
import psidev.psi.mi.jami.enricher.PublicationEnricher;
import psidev.psi.mi.jami.enricher.SourceEnricher;
import psidev.psi.mi.jami.enricher.exception.EnricherException;
import psidev.psi.mi.jami.enricher.listener.CvTermEnricherListener;
import psidev.psi.mi.jami.model.Source;
import uk.ac.ebi.intact.editor.services.enricher.DbEnricherService;
import uk.ac.ebi.intact.jami.model.extension.SourceAnnotation;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.annotation.Resource;
import java.util.Collection;

/**
 * Editor enricher for institutions
 */
public class EditorSourceEnricher implements SourceEnricher{
    private String importTag;

    @Resource(name = "intactInstitutionEnricher")
    private SourceEnricher intactInstitutionEnricher;
    @Resource(name = "dbEnricherService")
    private DbEnricherService dbEnricherService;

    @Override
    public PublicationEnricher getPublicationEnricher() {
        return intactInstitutionEnricher.getPublicationEnricher();
    }

    @Override
    public void setPublicationEnricher(PublicationEnricher enricher) {
         intactInstitutionEnricher.setPublicationEnricher(enricher);
    }

    @Override
    public CvTermFetcher<Source> getCvTermFetcher() {
        return intactInstitutionEnricher.getCvTermFetcher();
    }

    @Override
    public CvTermEnricherListener<Source> getCvTermEnricherListener() {
        return intactInstitutionEnricher.getCvTermEnricherListener();
    }

    @Override
    public void setCvTermEnricherListener(CvTermEnricherListener<Source> listener) {
        intactInstitutionEnricher.setCvTermEnricherListener(listener);
    }

    @Override
    public void enrich(Source object) throws EnricherException {

        intactInstitutionEnricher.enrich(object);

        if (getImportTag() != null && object != null){
            // check if object exists in database before adding a tag
            if (dbEnricherService.isNewSource(object)){
                object.getAnnotations().add(new SourceAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
            }
        }
    }

    @Override
    public void enrich(Collection<Source> objects) throws EnricherException {
        for (Source source : objects){
           enrich(source);
        }
    }

    @Override
    public void enrich(Source object, Source objectSource) throws EnricherException {
        intactInstitutionEnricher.enrich(object, objectSource);

        if (getImportTag() != null && object != null){
            // check if object exists in database before adding a tag
            if (dbEnricherService.isNewSource(object)){
                object.getAnnotations().add(new SourceAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
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
