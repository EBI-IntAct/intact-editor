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

import org.springframework.beans.factory.annotation.Autowired;
import psidev.psi.mi.jami.bridges.fetcher.OrganismFetcher;
import psidev.psi.mi.jami.enricher.CvTermEnricher;
import psidev.psi.mi.jami.enricher.OrganismEnricher;
import psidev.psi.mi.jami.enricher.exception.EnricherException;
import psidev.psi.mi.jami.enricher.listener.OrganismEnricherListener;
import psidev.psi.mi.jami.model.Alias;
import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.Organism;
import uk.ac.ebi.intact.dataexchange.enricher.EnricherContext;
import uk.ac.ebi.intact.editor.services.enricher.DbEnricherService;
import uk.ac.ebi.intact.jami.model.extension.CvTermAnnotation;
import uk.ac.ebi.intact.jami.model.extension.OrganismAlias;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.annotation.Resource;
import java.util.Collection;

/**
 * Editor import enricher for organisms
 */
public class EditorOrganismEnricher implements OrganismEnricher {

    @Resource(name = "intactBioSourceEnricher")
    private OrganismEnricher intactBioSourceEnricher;
    @Resource(name = "editorCvObjectEnricher")
    private CvTermEnricher<CvTerm> editorOlsEnricher;
    private String importTag;
    @Resource(name = "dbEnricherService")
    private DbEnricherService dbEnricherService;
    @Autowired
    private EnricherContext enricherContext;

    @Override
    public OrganismFetcher getOrganismFetcher() {
        return this.intactBioSourceEnricher.getOrganismFetcher();
    }

    @Override
    public OrganismEnricherListener getOrganismEnricherListener() {
        return this.intactBioSourceEnricher.getOrganismEnricherListener();
    }

    @Override
    public CvTermEnricher<CvTerm> getCvTermEnricher() {
        return editorOlsEnricher;
    }

    @Override
    public void setOrganismEnricherListener(OrganismEnricherListener listener) {
         this.intactBioSourceEnricher.setOrganismEnricherListener(listener);
    }

    @Override
    public void setCvTermEnricher(CvTermEnricher<CvTerm> enricher) {
        this.editorOlsEnricher = enricher;
    }

    @Override
    public void enrich(Organism object) throws EnricherException {
        intactBioSourceEnricher.setCvTermEnricher(this.editorOlsEnricher);

        intactBioSourceEnricher.enrich(object);

        if (getImportTag() != null && object != null){
            // check if object exists in database before adding a tag
            if (dbEnricherService.isNewOrganism(object)){
                object.getAliases().add(new OrganismAlias(IntactUtils.createMIAliasType(Alias.SYNONYM, Alias.SYNONYM_MI), getImportTag()));
            }

            // tag cell types and tissues if not done
            if (!enricherContext.getConfig().isUpdateCellTypesAndTissues()){
                if (object.getCellType() != null && dbEnricherService.isNewCvTerm(object.getCellType())){
                    object.getCellType().getAnnotations().add(new CvTermAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
                }
                if (object.getTissue() != null && dbEnricherService.isNewCvTerm(object.getTissue())){
                    object.getTissue().getAnnotations().add(new CvTermAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
                }
            }
        }
    }

    @Override
    public void enrich(Collection<Organism> objects) throws EnricherException {
        for (Organism o : objects){
            enrich(o);
        }
    }

    @Override
    public void enrich(Organism object, Organism objectSource) throws EnricherException {
        intactBioSourceEnricher.setCvTermEnricher(this.editorOlsEnricher);

        intactBioSourceEnricher.enrich(object, objectSource);

        if (getImportTag() != null && object != null){
            // check if object exists in database before adding a tag
            if (dbEnricherService.isNewOrganism(object)){
                object.getAliases().add(new OrganismAlias(IntactUtils.createMIAliasType(Alias.SYNONYM, Alias.SYNONYM_MI), getImportTag()));
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
