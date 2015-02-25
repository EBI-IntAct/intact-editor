package uk.ac.ebi.intact.editor.enricher;

import psidev.psi.mi.jami.enricher.CvTermEnricher;
import psidev.psi.mi.jami.enricher.exception.EnricherException;
import psidev.psi.mi.jami.enricher.listener.FeatureEnricherListener;
import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.ModelledFeature;
import uk.ac.ebi.intact.dataexchange.enricher.standard.ModelledFeatureEnricher;
import uk.ac.ebi.intact.editor.services.enricher.DbEnricherService;
import uk.ac.ebi.intact.jami.model.extension.ModelledFeatureAnnotation;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.annotation.Resource;
import java.util.Collection;

/**
 * Editor enricher for modelled features
 *
 *
 * @since 13/08/13
 */
public class EditorModelledFeatureEnricher implements psidev.psi.mi.jami.enricher.FeatureEnricher<ModelledFeature> {
    private String importTag;

    @Resource(name = "intactModelledFeatureEnricher")
    private psidev.psi.mi.jami.enricher.FeatureEnricher<ModelledFeature> intactModelledFeatureEnricher;
    @Resource(name = "editorMiEnricher")
    private CvTermEnricher<CvTerm> editorMiEnricher;
    @Resource(name = "editorCvObjectEnricher")
    private CvTermEnricher<CvTerm> editorCvObjectEnricher;
    @Resource(name = "dbEnricherService")
    private DbEnricherService dbEnricherService;

    @Override
    public void setFeaturesWithRangesToUpdate(Collection<ModelledFeature> features) {
        intactModelledFeatureEnricher.setFeaturesWithRangesToUpdate(features);
    }

    @Override
    public FeatureEnricherListener<ModelledFeature> getFeatureEnricherListener() {
        return intactModelledFeatureEnricher.getFeatureEnricherListener();
    }

    @Override
    public CvTermEnricher<CvTerm> getCvTermEnricher() {
        return editorMiEnricher;
    }

    @Override
    public void setFeatureEnricherListener(FeatureEnricherListener<ModelledFeature> listener) {
        intactModelledFeatureEnricher.setFeatureEnricherListener(listener);
    }

    @Override
    public void setCvTermEnricher(CvTermEnricher<CvTerm> cvEnricher) {
        editorMiEnricher = cvEnricher;
    }

    @Override
    public void enrich(ModelledFeature object) throws EnricherException {
        intactModelledFeatureEnricher.setCvTermEnricher(editorMiEnricher);
        if (intactModelledFeatureEnricher instanceof ModelledFeatureEnricher){
            ((ModelledFeatureEnricher) intactModelledFeatureEnricher).setIntactCvObjectEnricher(editorCvObjectEnricher);
        }

        intactModelledFeatureEnricher.enrich(object);

        if (getImportTag() != null && object != null){
            // check if object exists in database before adding a tag
            if (dbEnricherService.isNewModelledFeature(object)){
                object.getAnnotations().add(new ModelledFeatureAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
            }
        }
    }

    @Override
    public void enrich(Collection<ModelledFeature> objects) throws EnricherException {
        for (ModelledFeature f : objects){
            enrich(f);
        }
    }

    @Override
    public void enrich(ModelledFeature object, ModelledFeature objectSource) throws EnricherException {
        intactModelledFeatureEnricher.setCvTermEnricher(editorMiEnricher);
        if (intactModelledFeatureEnricher instanceof ModelledFeatureEnricher){
            ((ModelledFeatureEnricher) intactModelledFeatureEnricher).setIntactCvObjectEnricher(editorCvObjectEnricher);
        }

        intactModelledFeatureEnricher.enrich(object, objectSource);

        if (getImportTag() != null && object != null){
            // check if object exists in database before adding a tag
            if (dbEnricherService.isNewModelledFeature(object)){
                object.getAnnotations().add(new ModelledFeatureAnnotation(IntactUtils.createMITopic("remark-internal", null), getImportTag()));
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

