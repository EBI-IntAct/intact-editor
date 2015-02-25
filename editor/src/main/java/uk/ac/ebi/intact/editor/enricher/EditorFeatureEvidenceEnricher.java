package uk.ac.ebi.intact.editor.enricher;

import psidev.psi.mi.jami.enricher.CvTermEnricher;
import psidev.psi.mi.jami.enricher.FeatureEnricher;
import psidev.psi.mi.jami.enricher.exception.EnricherException;
import psidev.psi.mi.jami.enricher.listener.FeatureEnricherListener;
import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.FeatureEvidence;
import uk.ac.ebi.intact.dataexchange.enricher.standard.FeatureEvidenceEnricher;
import uk.ac.ebi.intact.editor.services.enricher.DbEnricherService;
import uk.ac.ebi.intact.jami.model.extension.FeatureEvidenceAnnotation;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.annotation.Resource;
import java.util.Collection;

/**
 * Editor feature evidence enricher
 */
public class EditorFeatureEvidenceEnricher implements psidev.psi.mi.jami.enricher.FeatureEnricher<FeatureEvidence> {
    private String importTag;

    @Resource(name = "intactFeatureEvidenceEnricher")
    private FeatureEnricher<FeatureEvidence> intactFeatureEvidenceEnricher;
    @Resource(name = "editorMiEnricher")
    private CvTermEnricher<CvTerm> editorMiEnricher;
    @Resource(name = "editorCvObjectEnricher")
    private CvTermEnricher<CvTerm> editorCvObjectEnricher;
    @Resource(name = "dbEnricherService")
    private DbEnricherService dbEnricherService;

    @Override
    public void setFeaturesWithRangesToUpdate(Collection<FeatureEvidence> features) {
        intactFeatureEvidenceEnricher.setFeaturesWithRangesToUpdate(features);
    }

    @Override
    public FeatureEnricherListener<FeatureEvidence> getFeatureEnricherListener() {
        return intactFeatureEvidenceEnricher.getFeatureEnricherListener();
    }

    @Override
    public CvTermEnricher<CvTerm> getCvTermEnricher() {
        return editorMiEnricher;
    }

    @Override
    public void setFeatureEnricherListener(FeatureEnricherListener<FeatureEvidence> listener) {
         intactFeatureEvidenceEnricher.setFeatureEnricherListener(listener);
    }

    @Override
    public void setCvTermEnricher(CvTermEnricher<CvTerm> cvEnricher) {
        editorMiEnricher = cvEnricher;
    }

    @Override
    public void enrich(FeatureEvidence object) throws EnricherException {
        intactFeatureEvidenceEnricher.setCvTermEnricher(editorMiEnricher);
        if (intactFeatureEvidenceEnricher instanceof FeatureEvidenceEnricher){
            ((FeatureEvidenceEnricher)intactFeatureEvidenceEnricher).setIntactCvObjectEnricher(editorCvObjectEnricher);
        }

        intactFeatureEvidenceEnricher.enrich(object);

        if (getImportTag() != null && object != null){
            // check if object exists in database before adding a tag
            if (dbEnricherService.isNewFeatureEvidence(object)){
                object.getAnnotations().add(new FeatureEvidenceAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
            }
        }
    }

    @Override
    public void enrich(Collection<FeatureEvidence> objects) throws EnricherException {
        for (FeatureEvidence f : objects){
            enrich(f);
        }
    }

    @Override
    public void enrich(FeatureEvidence object, FeatureEvidence objectSource) throws EnricherException {
        intactFeatureEvidenceEnricher.setCvTermEnricher(editorMiEnricher);
        if (intactFeatureEvidenceEnricher instanceof FeatureEvidenceEnricher){
            ((FeatureEvidenceEnricher)intactFeatureEvidenceEnricher).setIntactCvObjectEnricher(editorCvObjectEnricher);
        }

        intactFeatureEvidenceEnricher.enrich(object, objectSource);

        if (getImportTag() != null && object != null){
            // check if object exists in database before adding a tag
            if (dbEnricherService.isNewFeatureEvidence(object)){
                object.getAnnotations().add(new FeatureEvidenceAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
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
