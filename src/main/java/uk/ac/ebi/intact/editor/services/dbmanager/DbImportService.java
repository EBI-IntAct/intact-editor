package uk.ac.ebi.intact.editor.services.dbmanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.Alias;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;

/**
 * Db import service
 */
@Service
public class DbImportService extends AbstractEditorService {

    private static final Log log = LogFactory.getLog( DbImportService.class );

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public void acceptImport(String importId){
        if (importId != null && importId.length() > 0){
            // first delete features imported
            int updated = getIntactDao().getEntityManager().createNativeQuery("delete from intact.ia_annotation where ac in ( " +
                    "select distinct a.ac from intact.ia_annotation a, intact.ia_controlledvocab cv where a.topic_ac = cv.ac and cv.shortlabel = :remark and a.description = :jobId " +
                    ")")
                    .setParameter("remark", "remark-internal")
                    .setParameter("jobId", importId)
                    .executeUpdate();

            log.info("Deleted import annotations "+updated);
        }
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public void deleteImportedFeatures(String importId){
        if (importId != null && importId.length() > 0){

            // first delete features imported
            int updated = getIntactDao().getEntityManager().createNativeQuery("delete from intact.ia_feature where ac in ( " +
                    "select distinct f.ac from intact.ia_feature f, intact.ia_feature2annot fa, intact.ia_annotation a, intact.ia_controlledvocab cv " +
                    "where cv.ac = a.topic_ac and a.ac = fa.annotation_ac and f.ac = fa.feature_ac and " +
                    "cv.shortlabel = :remark and a.description = :jobId" +
                    " )")
                    .setParameter("remark", "remark-internal")
                    .setParameter("jobId", importId)
                    .executeUpdate();

            log.info("Deleted features "+updated);
        }
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public void deleteImportedParticipants(String importId){
        if (importId != null && importId.length() > 0){

            // then delete participants imported
            int updated = getIntactDao().getEntityManager().createNativeQuery("delete from intact.ia_component where ac in ( " +
                    "select distinct f.ac from intact.ia_component f, intact.ia_component2annot fa, intact.ia_annotation a, intact.ia_controlledvocab cv " +
                    "where cv.ac = a.topic_ac and a.ac = fa.annotation_ac and f.ac = fa.component_ac and " +
                    "cv.shortlabel = :remark and a.description = :jobId" +
                    " )")
                    .setParameter("remark", "remark-internal")
                    .setParameter("jobId", importId)
                    .executeUpdate();

            log.info("Deleted participants "+updated);
        }
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public void deleteImportedInteractions(String importId){
        if (importId != null && importId.length() > 0){

            // then delete interactions imported
            int updated = getIntactDao().getEntityManager().createNativeQuery("delete from intact.ia_interactor where ac in ( " +
                    "select distinct f.ac from intact.ia_interactor f, intact.ia_int2annot fa, intact.ia_annotation a, intact.ia_controlledvocab cv " +
                    "where cv.ac = a.topic_ac and a.ac = fa.annotation_ac and f.ac = fa.interactor_ac and " +
                    "cv.shortlabel = :remark and a.description = :jobId" +
                    " )")
                    .setParameter("remark", "remark-internal")
                    .setParameter("jobId", importId)
                    .executeUpdate();

            log.info("Deleted interactions/complexes/interactors"+updated);
        }
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public void deleteImportedExperiments(String importId){
        if (importId != null && importId.length() > 0){

            // then delete experiments imported
            int updated = getIntactDao().getEntityManager().createNativeQuery("delete from intact.ia_experiment where ac in ( " +
                    "select distinct f.ac from intact.ia_experiment f, intact.ia_exp2annot fa, intact.ia_annotation a, intact.ia_controlledvocab cv " +
                    "where cv.ac = a.topic_ac and a.ac = fa.annotation_ac and f.ac = fa.experiment_ac and " +
                    "cv.shortlabel = :remark and a.description = :jobId" +
                    " )")
                    .setParameter("remark", "remark-internal")
                    .setParameter("jobId", importId)
                    .executeUpdate();

            log.info("Deleted experiments "+updated);
        }
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public void deleteImportedPublications(String importId){
        if (importId != null && importId.length() > 0){

            // then delete publications imported
            int updated = getIntactDao().getEntityManager().createNativeQuery("delete from intact.ia_publication where ac in ( " +
                    "select distinct f.ac from intact.ia_publication f, intact.ia_pub2annot fa, intact.ia_annotation a, intact.ia_controlledvocab cv " +
                    "where cv.ac = a.topic_ac and a.ac = fa.annotation_ac and f.ac = fa.publication_ac and " +
                    "cv.shortlabel = :remark and a.description = :jobId" +
                    " )")
                    .setParameter("remark", "remark-internal")
                    .setParameter("jobId", importId)
                    .executeUpdate();

            log.info("Deleted publications "+updated);
        }
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public void deleteImportedOrganisms(String importId){
        if (importId != null && importId.length() > 0){

            // then delete organisms imported
            int updated = getIntactDao().getEntityManager().createNativeQuery("delete from intact.ia_biosource where ac in ( " +
                    "select distinct f.ac from intact.ia_biosource f, intact.ia_biosource_alias a, intact.ia_controlledvocab cv " +
                    "where cv.ac = a.aliastype_ac and a.parent_ac = f.ac and " +
                    "cv.shortlabel = :synonym and a.name = :jobId" +
                    " )")
                    .setParameter("synonym", Alias.SYNONYM)
                    .setParameter("jobId", importId)
                    .executeUpdate();

            log.info("Deleted Organisms "+updated);
        }
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public void deleteImportedSources(String importId){
        if (importId != null && importId.length() > 0){

            // then delete source imported
            int updated = getIntactDao().getEntityManager().createNativeQuery("delete from intact.ia_institution where ac in ( " +
                    "select distinct f.ac from intact.ia_institution f, intact.ia_institution2annot fa, intact.ia_annotation a, intact.ia_controlledvocab cv " +
                    "where cv.ac = a.topic_ac and a.ac = fa.annotation_ac and f.ac = fa.institution_ac and " +
                    "cv.shortlabel = :remark and a.description = :jobId" +
                    " )")
                    .setParameter("remark", "remark-internal")
                    .setParameter("jobId", importId)
                    .executeUpdate();

            log.info("Deleted sources "+updated);
        }
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public void deleteImportedCvs(String importId){
        if (importId != null && importId.length() > 0){

            // before deleting cvs, check all annotations used in this cv
            int updated = getIntactDao().getEntityManager().createNativeQuery("delete from intact.ia_annotation where topic_ac in ( " +
                    "select distinct f.ac from intact.ia_controlledvocab f, intact.ia_cvobject2annot fa, intact.ia_annotation a, intact.ia_controlledvocab cv " +
                    "where cv.ac = a.topic_ac and a.ac = fa.annotation_ac and f.ac = fa.cvobject_ac and " +
                    "cv.shortlabel = :remark and a.description = :jobId " +
                    ")")
                    .setParameter("remark", "remark-internal")
                    .setParameter("jobId", importId)
                    .executeUpdate();
            log.info("Deleted annotations involving new cv "+updated);

            updated = getIntactDao().getEntityManager().createNativeQuery("delete from intact.ia_controlledvocab_xref where database_ac in ( " +
                    "select distinct f.ac from intact.ia_controlledvocab f, intact.ia_cvobject2annot fa, intact.ia_annotation a, intact.ia_controlledvocab cv " +
                    "where cv.ac = a.topic_ac and a.ac = fa.annotation_ac and f.ac = fa.cvobject_ac and " +
                    "cv.shortlabel = :remark and a.description = :jobId " +
                    ") " +
                    " or qualifier_ac in ( " +
                    "select distinct f.ac from intact.ia_controlledvocab f, intact.ia_cvobject2annot fa, intact.ia_annotation a, intact.ia_controlledvocab cv " +
                    "where cv.ac = a.topic_ac and a.ac = fa.annotation_ac and f.ac = fa.cvobject_ac and " +
                    "cv.shortlabel = :remark and a.description = :jobId" +
                    " )")
                    .setParameter("remark", "remark-internal")
                    .setParameter("jobId", importId)
                    .executeUpdate();
            log.info("Deleted xrefs involving new cv "+updated);

            updated = getIntactDao().getEntityManager().createNativeQuery("delete from intact.ia_controlledvocab_alias where aliastype_ac in ( " +
                    "select distinct f.ac from intact.ia_controlledvocab f, intact.ia_cvobject2annot fa, intact.ia_annotation a, intact.ia_controlledvocab cv " +
                    "where cv.ac = a.topic_ac and a.ac = fa.annotation_ac and f.ac = fa.cvobject_ac and " +
                    "cv.shortlabel = :remark and a.description = :jobId " +
                    ")")
                    .setParameter("remark", "remark-internal")
                    .setParameter("jobId", importId)
                    .executeUpdate();
            log.info("Deleted aliases involving new cv "+updated);

            // then delete cv imported
            updated = getIntactDao().getEntityManager().createNativeQuery("delete from intact.ia_controlledvocab where ac in ( " +
                    "select distinct f.ac from intact.ia_controlledvocab f, intact.ia_cvobject2annot fa, intact.ia_annotation a, intact.ia_controlledvocab cv " +
                    "where cv.ac = a.topic_ac and a.ac = fa.annotation_ac and f.ac = fa.cvobject_ac and " +
                    "cv.shortlabel = :remark and a.description = :jobId" +
                    " )")
                    .setParameter("remark", "remark-internal")
                    .setParameter("jobId", importId)
                    .executeUpdate();

            log.info("Deleted cvs "+updated);
        }
    }
}
