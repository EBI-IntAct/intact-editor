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

import psidev.psi.mi.jami.bridges.fetcher.CvTermFetcher;
import psidev.psi.mi.jami.enricher.CvTermEnricher;
import psidev.psi.mi.jami.enricher.exception.EnricherException;
import psidev.psi.mi.jami.enricher.listener.CvTermEnricherListener;
import psidev.psi.mi.jami.model.CvTerm;
import uk.ac.ebi.intact.dataexchange.enricher.standard.AbstractCvObjectEnricher;
import uk.ac.ebi.intact.editor.services.enricher.DbEnricherService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.extension.CvTermAnnotation;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.annotation.Resource;
import java.util.Collection;

/**
 * Editor ols enricher for imports
 */
public class EditorCvObjectEnricher implements CvTermEnricher<CvTerm> {
    private String importTag;

    private CvTermEnricher<CvTerm> intactCvObjectEnricher;
    @Resource(name = "dbEnricherService")
    private DbEnricherService dbEnricherService;

    public EditorCvObjectEnricher() {
    }

    public String getImportTag() {
        return importTag;
    }

    public void setImportTag(String importTag) {
        this.importTag = importTag;
    }

    @Override
    public CvTermFetcher<CvTerm> getCvTermFetcher() {
        return intactCvObjectEnricher.getCvTermFetcher();
    }

    @Override
    public CvTermEnricherListener<CvTerm> getCvTermEnricherListener() {
        return intactCvObjectEnricher.getCvTermEnricherListener();
    }

    @Override
    public void setCvTermEnricherListener(CvTermEnricherListener<CvTerm> listener) {
        intactCvObjectEnricher.setCvTermEnricherListener(listener);
    }

    @Override
    public void enrich(CvTerm object) throws EnricherException {
        if (intactCvObjectEnricher instanceof AbstractCvObjectEnricher){
            ((AbstractCvObjectEnricher)intactCvObjectEnricher).setCvEnricher((CvTermEnricher<CvTerm>)ApplicationContextProvider.getBean("simpleEditorMiEnricher"));
        }

        intactCvObjectEnricher.enrich(object);

        if (getImportTag() != null && object != null){
            // check if object exists in database before adding a tag
            if (dbEnricherService.isNewCvTerm(object)){
                object.getAnnotations().add(new CvTermAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
            }
        }
    }

    @Override
    public void enrich(Collection<CvTerm> objects) throws EnricherException {
        for (CvTerm cv : objects){
            enrich(cv);
        }
    }

    @Override
    public void enrich(CvTerm objectToEnrich, CvTerm objectSource) throws EnricherException {
        if (intactCvObjectEnricher instanceof AbstractCvObjectEnricher){
            ((AbstractCvObjectEnricher)intactCvObjectEnricher).setCvEnricher((CvTermEnricher<CvTerm>)ApplicationContextProvider.getBean("simpleEditorMiEnricher"));
        }

        intactCvObjectEnricher.enrich(objectToEnrich);

        if (getImportTag() != null && objectToEnrich != null){
            // check if object exists in database before adding a tag
            if (dbEnricherService.isNewCvTerm(objectToEnrich)){
                objectToEnrich.getAnnotations().add(new CvTermAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
            }
        }
    }

    public CvTermEnricher<CvTerm> getIntactCvObjectEnricher() {
        return intactCvObjectEnricher;
    }

    public void setIntactCvObjectEnricher(CvTermEnricher<CvTerm> intactCvObjectEnricher) {
        this.intactCvObjectEnricher = intactCvObjectEnricher;
    }
}
