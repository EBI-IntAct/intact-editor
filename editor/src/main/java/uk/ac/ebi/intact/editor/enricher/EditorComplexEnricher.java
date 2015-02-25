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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import psidev.psi.mi.jami.bridges.fetcher.InteractorFetcher;
import psidev.psi.mi.jami.enricher.*;
import psidev.psi.mi.jami.enricher.exception.EnricherException;
import psidev.psi.mi.jami.enricher.listener.InteractionEnricherListener;
import psidev.psi.mi.jami.enricher.listener.InteractorEnricherListener;
import psidev.psi.mi.jami.model.Complex;
import psidev.psi.mi.jami.model.CvTerm;
import uk.ac.ebi.intact.editor.services.enricher.DbEnricherService;
import uk.ac.ebi.intact.jami.model.extension.InteractorAnnotation;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.annotation.Resource;
import java.util.Collection;

/**
 * Editor enricher for complexes when importing files
 *
 */
public class EditorComplexEnricher implements psidev.psi.mi.jami.enricher.ComplexEnricher{

    /**
     * Sets up a logger for that class.
     */
    private static final Log log = LogFactory.getLog(EditorComplexEnricher.class);

    @Resource(name = "intactComplexEnricher")
    private ComplexEnricher intactComplexEnricher;
    @Resource(name = "dbEnricherService")
    private DbEnricherService dbEnricherService;
    @Resource(name = "editorModelledComponentEnricher")
    private psidev.psi.mi.jami.enricher.ParticipantEnricher editorModelledParticipantEnricher;
    @Resource(name = "editorMiEnricher")
    private CvTermEnricher<CvTerm> editorMiEnricher;
    @Resource(name = "editorCvObjectEnricher")
    private CvTermEnricher<CvTerm> editorCvObjectEnricher;
    @Resource(name = "editorSourceEnricher")
    private SourceEnricher editorSourceEnricher;
    @Resource(name = "editorOrganismEnricher")
    private OrganismEnricher editorOrganismEnricher;

    private String importTag;

    public EditorComplexEnricher() {
    }

    @Override
    public ParticipantEnricher getParticipantEnricher() {
        return editorModelledParticipantEnricher;
    }

    @Override
    public InteractorFetcher<Complex> getInteractorFetcher() {
        return intactComplexEnricher.getInteractorFetcher();
    }

    @Override
    public InteractorEnricherListener<Complex> getListener() {
        return intactComplexEnricher.getListener();
    }

    @Override
    public CvTermEnricher<CvTerm> getCvTermEnricher() {
        return editorMiEnricher;
    }

    @Override
    public OrganismEnricher getOrganismEnricher() {
        return editorOrganismEnricher;
    }

    @Override
    public void setListener(InteractorEnricherListener<Complex> listener) {
        intactComplexEnricher.setListener(listener);
    }

    @Override
    public InteractionEnricherListener<Complex> getInteractionEnricherListener() {
        return intactComplexEnricher.getInteractionEnricherListener();
    }

    @Override
    public void setParticipantEnricher(psidev.psi.mi.jami.enricher.ParticipantEnricher enricher) {
        editorModelledParticipantEnricher = enricher;
    }

    @Override
    public void setCvTermEnricher(CvTermEnricher<CvTerm> enricher) {
         editorMiEnricher = enricher;
    }

    @Override
    public void setOrganismEnricher(OrganismEnricher enricher) {
        editorOrganismEnricher = enricher;
    }

    @Override
    public void setInteractionEnricherListener(InteractionEnricherListener<Complex> listener) {
        intactComplexEnricher.setInteractionEnricherListener(listener);
    }

    @Override
    public SourceEnricher getSourceEnricher() {
        return editorSourceEnricher;
    }

    @Override
    public void setSourceEnricher(SourceEnricher enricher) {
        editorSourceEnricher = enricher;
    }

    @Override
    public void enrich(Complex object) throws EnricherException {
        intactComplexEnricher.setParticipantEnricher(editorModelledParticipantEnricher);
        intactComplexEnricher.setCvTermEnricher(editorMiEnricher);
        intactComplexEnricher.setSourceEnricher(editorSourceEnricher);
        intactComplexEnricher.setOrganismEnricher(editorOrganismEnricher);
        if (intactComplexEnricher instanceof uk.ac.ebi.intact.dataexchange.enricher.standard.ComplexEnricher){
            ((uk.ac.ebi.intact.dataexchange.enricher.standard.ComplexEnricher)intactComplexEnricher).setIntactCvObjectEnricher(editorCvObjectEnricher);
        }

        intactComplexEnricher.enrich(object);

        if (getImportTag() != null && object != null){
            // check if object exists in database before adding a tag
            if (dbEnricherService.isNewComplex(object)){
                object.getAnnotations().add(new InteractorAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
            }
        }
    }

    @Override
    public void enrich(Collection<Complex> objects) throws EnricherException {
        for (Complex c : objects){
            enrich(c);
        }
    }

    @Override
    public void enrich(Complex objectToEnrich, Complex objectSource) throws EnricherException {
        intactComplexEnricher.setParticipantEnricher(editorModelledParticipantEnricher);
        intactComplexEnricher.setCvTermEnricher(editorMiEnricher);
        intactComplexEnricher.setSourceEnricher(editorSourceEnricher);
        intactComplexEnricher.setOrganismEnricher(editorOrganismEnricher);
        if (intactComplexEnricher instanceof uk.ac.ebi.intact.dataexchange.enricher.standard.ComplexEnricher){
            ((uk.ac.ebi.intact.dataexchange.enricher.standard.ComplexEnricher)intactComplexEnricher).setIntactCvObjectEnricher(editorCvObjectEnricher);
        }

        intactComplexEnricher.enrich(objectToEnrich, objectSource);

        if (getImportTag() != null && objectToEnrich != null){
            // check if object exists in database before adding a tag
            if (dbEnricherService.isNewComplex(objectToEnrich)){
                objectToEnrich.getAnnotations().add(new InteractorAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
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
