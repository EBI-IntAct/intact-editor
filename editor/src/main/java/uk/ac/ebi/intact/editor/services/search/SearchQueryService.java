package uk.ac.ebi.intact.editor.services.search;

import com.google.common.collect.Maps;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.primefaces.model.LazyDataModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.Experiment;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.editor.services.summary.*;
import uk.ac.ebi.intact.editor.util.LazyDataModelFactory;

import javax.annotation.Resource;
import java.util.HashMap;

/**
 * Search query service.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 2.0
 */
@Service
public class SearchQueryService extends AbstractEditorService {

    private static final Log log = LogFactory.getLog( SearchQueryService.class );

    @Resource(name = "publicationSummaryService")
    private PublicationSummaryService publicationSummaryService;

    @Resource(name = "complexSummaryService")
    private ComplexSummaryService complexSummaryService;

    @Resource(name = "experimentSummaryService")
    private ExperimentSummaryService experimentSummaryService;

    @Resource(name = "interactionSummaryService")
    private InteractionSummaryService interactionSummaryService;

    @Resource(name = "moleculeSummaryService")
    private MoleculeSummaryService moleculeSummaryService;

    @Resource(name = "cvSummaryService")
    private CvSummaryService cvSummaryService;

    @Resource(name = "organismSummaryService")
    private OrganismSummaryService organismSummaryService;

    @Resource(name = "featureEvidenceSummaryService")
    private FeatureEvidenceSummaryService featureEvidenceSummaryService;

    @Resource(name = "modelledFeatureSummaryService")
    private ModelledFeatureSummaryService modelledFeatureSummaryService;

    @Resource(name = "participantEvidenceSummaryService")
    private ParticipantEvidenceSummaryService participantEvidenceSummaryService;

    @Resource(name = "modelledParticipantSummaryService")
    private ModelledParticipantSummaryService modelledParticipantSummaryService;

    //////////////////
    // Constructors

    public SearchQueryService() {
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<CvSummary> loadCvObjects( String query, String originalQuery ) {

        log.info( "Searching for CvObject matching '" + query + "'..." );

        final HashMap<String, String> params = Maps.<String, String>newHashMap();
        params.put( "query", query );
        params.put( "ac", originalQuery );

        // all cvobjects
        LazyDataModel<CvSummary> cvobjects = LazyDataModelFactory.createLazyDataModel( this.cvSummaryService, "cvSummaryService",

                                                              "select distinct i " +
                                                              "from IntactCvTerm i left join i.dbXrefs as x " +
                                                              "where    ( i.ac = :ac " +
                                                              "      or lower(i.shortName) like :query " +
                                                              "      or lower(i.fullName) like :query " +
                                                              "      or lower(x.id) like :query ) ",

                                                              "select count(distinct i) " +
                                                              "from IntactCvTerm i left join i.dbXrefs as x " +
                                                              "where   (i.ac = :ac " +
                                                              "      or lower(i.shortName) like :query " +
                                                              "      or lower(i.fullName) like :query " +
                                                              "      or lower(x.id) like :query )",

                                                              params, "i", "updated, i.ac", false);

        log.info( "CvObject found: " + cvobjects.getRowCount() );

        return cvobjects;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<CvSummary> loadImportedCvs( String query) {

        log.info( "Searching for CvObject imported with '" + query + "'..." );

        final HashMap<String, String> params = Maps.<String, String>newHashMap();
        params.put( "query", query );
        params.put("remark", "remark-internal");

        // all cvobjects
        LazyDataModel<CvSummary> cvobjects = LazyDataModelFactory.createLazyDataModel( this.cvSummaryService, "cvSummaryService",

                "select distinct i " +
                        "from IntactCvTerm i join i.dbAnnotations as a " +
                        "where a.value = :query and a.topic.shortName = :remark",

                "select count(distinct i) " +
                        "from IntactCvTerm i join i.dbAnnotations as a " +
                        "where a.value = :query and a.topic.shortName = :remark",

                params, "i", "updated, i.ac", false);

        log.info( "CvObject imported: " + cvobjects.getRowCount() );

        return cvobjects;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<MoleculeSummary> loadMolecules( String query, String originalQuery ) {

        log.info( "Searching for Molecules matching '" + query + "'..." );

        final HashMap<String, String> params = Maps.<String, String>newHashMap();
        params.put( "query", query );
        params.put( "ac", originalQuery );

        // all molecules but interactions
        LazyDataModel<MoleculeSummary> molecules = LazyDataModelFactory.createLazyDataModel( moleculeSummaryService, "moleculeSummaryService",

                                                              "select distinct i " +
                                                              "from IntactInteractor i left join i.dbXrefs as x " +
                                                              "where    ( i.ac = :ac " +
                                                              "      or lower(i.shortName) like :query " +
                                                              "      or lower(i.fullName) like :query " +
                                                              "      or lower(x.id) like :query ) ",
                                                              "select count(distinct i) " +
                                                              "from IntactInteractor i left join i.dbXrefs as x " +
                                                              "where   (i.ac = :ac " +
                                                              "      or lower(i.shortName) like :query " +
                                                              "      or lower(i.fullName) like :query " +
                                                              "      or lower(x.id) like :query )",

                                                              params, "i", "updated, i.ac", false );

        log.info( "Molecules found: " + molecules.getRowCount() );
        return molecules;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<MoleculeSummary> loadImportedMolecules( String query) {

        log.info( "Searching for Molecules Imported with '" + query + "'..." );

        final HashMap<String, String> params = Maps.<String, String>newHashMap();
        params.put( "query", query );
        params.put("remark","remark-internal");

        // all molecules but interactions
        LazyDataModel<MoleculeSummary> molecules = LazyDataModelFactory.createLazyDataModel( moleculeSummaryService, "moleculeSummaryService",

                "select distinct i " +
                        "from IntactInteractor i join i.dbAnnotations as a " +
                        "where a.value = :query and a.topic.shortName = :remark ",
                "select count(distinct i) " +
                        "from IntactInteractor i join i.dbAnnotations as a " +
                        "where a.value = :query and a.topic.shortName = :remark ",

                params, "i", "updated, i.ac", false );

        log.info( "Molecules imported: " + molecules.getRowCount() );
        return molecules;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<MoleculeSummary> loadMoleculesByOrganism( String organismAc ) {

        log.info( "Searching for Molecules matching organism '" + organismAc + "'..." );

        final HashMap<String, String> params = Maps.<String, String>newHashMap();
        params.put( "ac", organismAc );

        // all molecules but interactions
        LazyDataModel<MoleculeSummary> molecules = LazyDataModelFactory.createLazyDataModel( moleculeSummaryService, "moleculeSummaryService",

                "select distinct i " +
                        "from IntactInteractor i join i.organism as o " +
                        "where  o.ac = :ac ",

                        "select count(distinct i) " +
                        "from IntactInteractor i join i.organism as o " +
                        "where o.ac = :ac",

                        params, "i", "updated, i.ac", false );

        log.info( "Molecules found: " + molecules.getRowCount() );
        return molecules;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<ComplexSummary> loadComplexesByOrganism( String organismAc ) {

        log.info( "Searching for Complexes matching organism '" + organismAc + "'..." );

        final HashMap<String, String> params = Maps.<String, String>newHashMap();
        params.put( "ac", organismAc );

        // all molecules but interactions
        LazyDataModel<ComplexSummary> molecules = LazyDataModelFactory.createLazyDataModel( complexSummaryService, "complexSummaryService",

                "select distinct i " +
                        "from IntactComplex i join i.organism as o " +
                        "where  o.ac = :ac ",

                        "select count(distinct i) " +
                        "from IntactComplex i join i.organism as o " +
                        "where o.ac = :ac",

                        params, "i", "updated, i.ac", false );

        log.info( "Molecules found: " + molecules.getRowCount() );
        return molecules;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<InteractionSummary>  loadInteractions( String query, String originalQuery ) {

        log.info( "Searching for Interactions matching '" + query + "'..." );

        final HashMap<String, String> params = Maps.<String, String>newHashMap();
        params.put( "query", query );
        params.put( "ac", originalQuery );

        // Load experiment eagerly to avoid LazyInitializationException when rendering the view
        LazyDataModel<InteractionSummary> interactions = LazyDataModelFactory.createLazyDataModel( interactionSummaryService, "interactionSummaryService",

                                                                 "select distinct i " +
                                                                 "from IntactInteractionEvidence i left join i.dbXrefs as x " +
                                                                 "where    (i.ac = :ac " +
                                                                 "      or lower(i.shortName) like :query " +
                                                                 "      or lower(x.id) like :query )",

                                                                 "select count(distinct i.ac) " +
                                                                 "from IntactInteractionEvidence i left join i.dbXrefs as x " +
                                                                 "where    (i.ac = :ac " +
                                                                 "      or lower(i.shortName) like :query " +
                                                                 "      or lower(x.id) like :query )",

                                                                 params, "i", "updated, i.ac", false );

        log.info( "Interactions found: " + interactions.getRowCount() );
        return interactions;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<InteractionSummary>  loadImportedInteractions( String query) {

        log.info( "Searching for Interactions imported with '" + query + "'..." );

        final HashMap<String, String> params = Maps.<String, String>newHashMap();
        params.put( "query", query );
        params.put( "remark", "remark-internal" );

        // Load experiment eagerly to avoid LazyInitializationException when rendering the view
        LazyDataModel<InteractionSummary> interactions = LazyDataModelFactory.createLazyDataModel( interactionSummaryService,"interactionSummaryService",

                "select distinct i " +
                        "from IntactInteractionEvidence i join i.dbAnnotations as a " +
                        "where a.value = :query and a.topic.shortName = :remark ",
                "select count(distinct i) " +
                        "from IntactInteractionEvidence i join i.dbAnnotations as a " +
                        "where a.value = :query and a.topic.shortName = :remark ",

                params, "i", "updated, i.ac", false );

        log.info( "Interactions imported: " + interactions.getRowCount() );
        return interactions;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<InteractionSummary>  loadInteractionsByMolecule( String moleculeAc ) {

        log.info( "Searching for Interactions with molecule '" + moleculeAc + "'..." );

        final HashMap<String, String> params = Maps.<String, String>newHashMap();
        params.put( "ac", moleculeAc );

        // Load experiment eagerly to avoid LazyInitializationException when rendering the view
        LazyDataModel<InteractionSummary> interactions = LazyDataModelFactory.createLazyDataModel( interactionSummaryService,"interactionSummaryService",

                "select distinct i " +
                        "from IntactInteractionEvidence i join i.participants as p join p.interactor as inter " +
                        "where  inter.ac = :ac",

                        "select count(distinct i.ac) " +
                        "from IntactInteractionEvidence i join i.participants as p join p.interactor as inter " +
                        "where  inter.ac = :ac",

                        params, "i", "updated, i.ac", false );

        log.info( "Interactions found: " + interactions.getRowCount() );
        return interactions;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<ComplexSummary>  loadComplexesByMolecule( String moleculeAc ) {

        log.info( "Searching for Complexes with molecule '" + moleculeAc + "'..." );

        final HashMap<String, String> params = Maps.<String, String>newHashMap();
        params.put( "ac", moleculeAc );

        // Load experiment eagerly to avoid LazyInitializationException when rendering the view
        LazyDataModel<ComplexSummary> interactions = LazyDataModelFactory.createLazyDataModel( complexSummaryService,"complexSummaryService",

                "select distinct i " +
                        "from IntactComplex i join i.participants as p join p.interactor as inter " +
                        "where  inter.ac = :ac",

                        "select count(distinct i.ac) " +
                        "from IntactComplex i join i.participants as p join p.interactor as inter " +
                        "where  inter.ac = :ac",

                        params, "i", "updated, i.ac", false );

        log.info( "Interactions found: " + interactions.getRowCount() );
        return interactions;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<MoleculeSummary>  loadMoleculeSetsByMolecule( String moleculeAc ) {

        log.info( "Searching for Molecule sets with molecule '" + moleculeAc + "'..." );

        final HashMap<String, String> params = Maps.<String, String>newHashMap();
        params.put( "ac", moleculeAc );
        // Load experiment eagerly to avoid LazyInitializationException when rendering the view
        LazyDataModel<MoleculeSummary> interactions = LazyDataModelFactory.createLazyDataModel( moleculeSummaryService,"moleculeSummaryService",

                "select distinct i " +
                        "from IntactInteractorPool i join i.interactors as inter " +
                        "where  inter.ac = :ac",

                "select count(distinct i.ac) " +
                        "from IntactInteractorPool i join i.interactors as inter " +
                        "where  inter.ac = :ac",

                params, "i", "updated, i.ac", false );

        log.info( "Interactions found: " + interactions.getRowCount() );
        return interactions;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<ComplexSummary> loadComplexes( String query, String originalQuery ) {

        log.info( "Searching for Complexes matching '" + query + "'..." );

        final HashMap<String, String> params = Maps.<String, String>newHashMap();
        params.put( "query", query );
        params.put( "ac", originalQuery );
        // Load experiment eagerly to avoid LazyInitializationException when rendering the view
        LazyDataModel<ComplexSummary> complexes = LazyDataModelFactory.createLazyDataModel( complexSummaryService,"complexSummaryService",

                "select distinct i " +
                        "from IntactComplex i left join i.dbXrefs as x " +
                        "where    i.ac = :ac " +
                        "      or lower(i.shortName) like :query " +
                        "      or lower(x.id) like :query "+
                        "      or i.ac in (select distinct i2.ac from IntactComplex i2 left join i2.dbAliases as a " +
                        "      where lower(a.name) like :query ) "+
                        "      or i.ac in (select distinct i3.ac from IntactComplex i3 left join i3.organism as o " +
                        "      where lower(o.dbTaxid) = :ac )",

                "select count( distinct i.ac ) " +
                        "from IntactComplex i left join i.dbXrefs as x " +
                        "where    i.ac = :ac " +
                        "      or lower(i.shortName) like :query " +
                        "      or lower(x.id) like :query "+
                        "      or i.ac in (select distinct i2.ac from IntactComplex i2 left join i2.dbAliases as a " +
                        "      where lower(a.name) like :query ) "+
                        "      or i.ac in (select distinct i3.ac from IntactComplex i3 left join i3.organism as o " +
                        "      where lower(o.dbTaxid) = :ac )",

                params, "i", "updated, i.ac", false );

        log.info( "Complexes found: " + complexes.getRowCount() );
        return complexes;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<ComplexSummary> loadImportedComplexes( String query ) {

        log.info( "Searching for Complexes imported with '" + query + "'..." );

        final HashMap<String, String> params = Maps.<String, String>newHashMap();
        params.put( "query", query );
        params.put( "remark", "remark-internal" );
        // Load experiment eagerly to avoid LazyInitializationException when rendering the view
        LazyDataModel<ComplexSummary> complexes = LazyDataModelFactory.createLazyDataModel( complexSummaryService,"complexSummaryService",

                "select distinct i " +
                        "from IntactComplex i join i.dbAnnotations as a " +
                        "where a.value = :query and a.topic.shortName = :remark ",
                "select count(distinct i) " +
                        "from IntactComplex i join i.dbAnnotations as a " +
                        "where a.value = :query and a.topic.shortName = :remark ",

                params, "i", "updated, i.ac", false );

        log.info( "Complexes imported: " + complexes.getRowCount() );
        return complexes;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<ExperimentSummary> loadExperiments( String query, String originalQuery ) {

        log.info( "Searching for experiments matching '" + query + "'..." );

        final HashMap<String, String> params = Maps.<String, String>newHashMap();
        params.put( "query", query );
        params.put( "ac", originalQuery );
        params.put( "inferred", Experiment.INFERRED_BY_CURATOR );

        LazyDataModel<ExperimentSummary> experiments = LazyDataModelFactory.createLazyDataModel( experimentSummaryService,"experimentSummaryService",

                                                                "select distinct e " +
                                                                "from IntactExperiment e left join e.xrefs as x " +
                                                                        "left join e.interactionDetectionMethod as d " +
                                                                "where  d.shortName <> :inferred and  (e.ac = :ac " +
                                                                "      or lower(e.shortLabel) like :query " +
                                                                "      or lower(x.id) like :query)) ",

                                                                "select count(distinct e) " +
                                                                "from IntactExperiment e left join e.xrefs as x " +
                                                                        "left join e.interactionDetectionMethod as d " +
                                                                "where  d.shortName <> :inferred and (e.ac = :ac " +
                                                                "      or lower(e.shortLabel) like :query " +
                                                                "      or lower(x.id) like :query) ",

                                                                params, "e", "updated, e.ac", false );

        log.info( "Experiment found: " + experiments.getRowCount() );
        return experiments;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<ExperimentSummary> loadImportedExperiments( String query ) {

        log.info( "Searching for experiments imported with '" + query + "'..." );

        final HashMap<String, String> params = Maps.<String, String>newHashMap();
        params.put( "query", query );
        params.put( "remark", "remark-internal" );

        LazyDataModel<ExperimentSummary> experiments = LazyDataModelFactory.createLazyDataModel( experimentSummaryService,"experimentSummaryService",

                "select distinct i " +
                        "from IntactExperiment i join i.annotations as a " +
                        "where a.value = :query and a.topic.shortName = :remark ",
                "select count(distinct i) " +
                        "from IntactExperiment i join i.annotations as a " +
                        "where a.value = :query and a.topic.shortName = :remark ",

                params, "i", "updated, i.ac", false );

        log.info( "Experiment imported: " + experiments.getRowCount() );
        return experiments;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<ExperimentSummary> loadExperimentsByHostOrganism( String organismAc ) {

        log.info( "Searching for experiments matching organism '" + organismAc + "'..." );

        final HashMap<String, String> params = Maps.<String, String>newHashMap();
        params.put( "ac", organismAc );
        params.put( "inferred", Experiment.INFERRED_BY_CURATOR );

        LazyDataModel<ExperimentSummary> experiments = LazyDataModelFactory.createLazyDataModel( experimentSummaryService,"experimentSummaryService",

                "select distinct e " +
                        "from IntactExperiment e join e.hostOrganism as o " +
                        "left join e.interactionDetectionMethod as d " +
                        "where d.shortName <> :inferred and o.ac = :ac ",

                "select count(distinct e) " +
                        "from IntactExperiment e join e.hostOrganism as o " +
                        "left join e.interactionDetectionMethod as d " +
                        "where d.shortName <> :inferred and o.ac = :ac ",

                params, "e", "updated, e.ac", false );

        log.info( "Experiment found: " + experiments.getRowCount() );
        return experiments;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<PublicationSummary> loadPublication( String query, String originalQuery ) {
        log.info( "Searching for publications matching '" + query + "'..." );

        final HashMap<String, String> params = Maps.<String, String>newHashMap();
        params.put( "query", query );
        params.put( "ac", originalQuery );
        params.put( "intactReleased", "14681455" );
        params.put( "intactOnHold", "unassigned638" );
        params.put( "pdbOnHold", "24288376" );
        params.put( "chemblOnHold", "24214965" );

        LazyDataModel<PublicationSummary> publications = LazyDataModelFactory.createLazyDataModel( publicationSummaryService,"publicationSummaryService",

                                                                 "select distinct p " +
                                                                 "from IntactPublication p left join p.dbXrefs as x " +
                                                                 "where  p.shortLabel not in (:intactReleased, :intactOnHold, :pdbOnHold, :chemblOnHold) " +
                                                                 "and  (p.ac = :ac " +
                                                                 "      or lower(p.shortLabel) like :query " +
                                                                 "      or lower(p.title) like :query " +
                                                                 "      or lower(x.id) like :query) ",

                                                                 "select count(distinct p) " +
                                                                 "from IntactPublication p left join p.dbXrefs as x " +
                                                                 "where  p.shortLabel not in (:intactReleased, :intactOnHold, :pdbOnHold, :chemblOnHold) " +
                                                                 "and  (p.ac = :ac " +
                                                                 "      or lower(p.shortLabel) like :query " +
                                                                 "      or lower(p.title) like :query " +
                                                                 "      or lower(x.id) like :query) ",

                                                                 params, "p", "updated, p.ac", false );

        log.info( "Publications found: " + publications.getRowCount() );
        return publications;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<PublicationSummary> loadImportedPublication( String query) {
        log.info( "Searching for publications imported with '" + query + "'..." );

        final HashMap<String, String> params = Maps.<String, String>newHashMap();
        params.put( "query", query );
        params.put( "remark", "remark-internal" );

        LazyDataModel<PublicationSummary> publications = LazyDataModelFactory.createLazyDataModel( publicationSummaryService,"publicationSummaryService",

                "select distinct i " +
                        "from IntactPublication i join i.dbAnnotations as a " +
                        "where a.value = :query and a.topic.shortName = :remark ",
                "select count(distinct i) " +
                        "from IntactPublication i join i.dbAnnotations as a " +
                        "where a.value = :query and a.topic.shortName = :remark ",

                params, "i", "updated, i.ac", false );

        log.info( "Publications imported: " + publications.getRowCount() );
        return publications;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<FeatureSummary> loadFeatures( String query, String originalQuery ) {
        log.info( "Searching for features matching '" + query + "' or AC '"+originalQuery+"'..." );

        final HashMap<String, String> params = Maps.<String, String>newHashMap();
        params.put( "query", query );
        params.put( "ac", originalQuery );

        LazyDataModel<FeatureSummary> features = LazyDataModelFactory.createLazyDataModel( featureEvidenceSummaryService,"featureEvidenceSummaryService",

                                                                 "select distinct p " +
                                                                 "from IntactFeatureEvidence p left join p.dbXrefs as x " +
                                                                 "where  (p.ac = :ac " +
                                                                 "      or lower(p.shortName) like :query " +
                                                                 "      or lower(p.fullName) like :query " +
                                                                 "      or lower(x.id) like :query) ",

                                                                 "select count(distinct p) " +
                                                                 "from IntactFeatureEvidence p left join p.dbXrefs as x " +
                                                                 "where (p.ac = :ac " +
                                                                 "      or lower(p.shortName) like :query " +
                                                                 "      or lower(p.fullName) like :query " +
                                                                 "      or lower(x.id) like :query) ",

                                                                 params, "p", "updated, p.ac", false);

        log.info( "Features found: " + features.getRowCount() );
        return features;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<FeatureSummary> loadImportedFeatures( String query) {
        log.info( "Searching for features imported with '" + query  );

        final HashMap<String, String> params = Maps.<String, String>newHashMap();
        params.put( "query", query );
        params.put( "remark", "remark-internal" );

        LazyDataModel<FeatureSummary> features = LazyDataModelFactory.createLazyDataModel( featureEvidenceSummaryService,"featureEvidenceSummaryService",

                "select distinct i " +
                        "from IntactFeatureEvidence i join i.annotations as a " +
                        "where a.value = :query and a.topic.shortName = :remark ",
                "select count(distinct i) " +
                        "from IntactFeatureEvidence i join i.annotations as a " +
                        "where a.value = :query and a.topic.shortName = :remark ",

                params, "i", "updated, i.ac", false);

        log.info( "Features imported: " + features.getRowCount() );
        return features;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<OrganismSummary> loadOrganisms( String query, String originalQuery ) {
        log.info( "Searching for organisms matching '" + query + "'..." );

        final HashMap<String, String> params = Maps.newHashMap();
        params.put( "query", query );
        params.put( "ac", originalQuery );

        // Load experiment eagerly to avoid LazyInitializationException when redering the view
        LazyDataModel<OrganismSummary> organisms = LazyDataModelFactory.createLazyDataModel( organismSummaryService,"organismSummaryService",

                                                                 "select distinct b " +
                                                                 "from IntactOrganism b " +
                                                                 "where    b.ac = :ac " +
                                                                 "      or lower(b.commonName) like :query " +
                                                                 "      or lower(b.scientificName) like :query " +
                                                                 "      or lower(b.dbTaxid) like :query ",

                                                                 "select count(distinct b) " +
                                                                 "from IntactOrganism b " +
                                                                 "where    b.ac = :ac " +
                                                                 "      or lower(b.commonName) like :query " +
                                                                 "      or lower(b.scientificName) like :query " +
                                                                 "      or lower(b.dbTaxid) like :query ",

                                                                 params, "b", "updated, b.ac", false);

        log.info( "Organisms found: " + organisms.getRowCount() );
        return organisms;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<OrganismSummary> loadImportedOrganisms( String query ) {
        log.info( "Searching for organisms imported with '" + query + "'..." );

        final HashMap<String, String> params = Maps.newHashMap();
        params.put( "query", query );
        params.put( "synonym", "synonym" );

        // Load experiment eagerly to avoid LazyInitializationException when redering the view
        LazyDataModel<OrganismSummary> organisms = LazyDataModelFactory.createLazyDataModel( organismSummaryService,"organismSummaryService",

                "select distinct i " +
                        "from IntactOrganism i join i.aliases as a " +
                        "where a.name = :query and a.type.shortName = :synonym ",
                "select count(distinct i) " +
                        "from IntactOrganism i join i.aliases as a " +
                        "where a.name = :query and a.type.shortName = :synonym ",

                params, "i", "updated, i.ac", false);

        log.info( "Organisms imported: " + organisms.getRowCount() );
        return organisms;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<ParticipantSummary> loadParticipants( String query, String originalQuery ) {
        log.info( "Searching for participants matching '" + query + "'..." );

        final HashMap<String, String> params = Maps.newHashMap();
        params.put( "query", query );
        params.put( "ac", originalQuery );

        // Load experiment eagerly to avoid LazyInitializationException when redering the view
        LazyDataModel<ParticipantSummary> participants = LazyDataModelFactory.createLazyDataModel( participantEvidenceSummaryService,"participantEvidenceSummaryService",

                "select distinct p " +
                        "from IntactParticipantEvidence p left join p.xrefs as x " +
                        "where  (p.ac = :ac " +
                        "      or lower(x.id) like :query) ",

                "select count(distinct p) " +
                        "from IntactParticipantEvidence p left join p.xrefs as x " +
                        "where (p.ac = :ac " +
                        "      or lower(x.id) like :query) ",

                params, "p", "updated, p.ac", false);

        log.info( "Participants found: " + participants.getRowCount() );
        return participants;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<ParticipantSummary> loadImportedParticipants( String query) {
        log.info( "Searching for participants imported with '" + query + "'..." );

        final HashMap<String, String> params = Maps.newHashMap();
        params.put( "query", query );
        params.put( "remark", "remark-internal" );

        // Load experiment eagerly to avoid LazyInitializationException when redering the view
        LazyDataModel<ParticipantSummary> participants = LazyDataModelFactory.createLazyDataModel( participantEvidenceSummaryService,"participantEvidenceSummaryService",

                "select distinct i " +
                        "from IntactParticipantEvidence i join i.annotations as a " +
                        "where a.value = :query and a.topic.shortName = :remark ",
                "select count(distinct i) " +
                        "from IntactParticipantEvidence i join i.annotations as a " +
                        "where a.value = :query and a.topic.shortName = :remark ",

                params, "i", "updated, i.ac", false);

        log.info( "Participants imported: " + participants.getRowCount() );
        return participants;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<ParticipantSummary> loadParticipantsByOrganism( String organismAc ) {
        log.info( "Searching for participants with organism '" + organismAc + "'..." );

        final HashMap<String, String> params = Maps.newHashMap();
        params.put( "ac", organismAc );

        // Load experiment eagerly to avoid LazyInitializationException when redering the view
        LazyDataModel<ParticipantSummary> participants = LazyDataModelFactory.createLazyDataModel( participantEvidenceSummaryService,"participantEvidenceSummaryService",

                "select distinct p " +
                        "from IntactParticipantEvidence p join p.expressedInOrganism as o " +
                        "where o.ac = :ac ",

                "select count(distinct p) " +
                        "from IntactParticipantEvidence p join p.expressedInOrganism as o " +
                        "where o.ac = :ac ",

                params, "p", "updated, p.ac", false);

        log.info( "Participants found: " + participants.getRowCount() );
        return participants;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<FeatureSummary>  loadModelledFeatures(String query, String originalQuery) {
        log.info( "Searching for complex features matching '" + query + "' or AC '"+originalQuery+"'..." );

        final HashMap<String, String> params = Maps.<String, String>newHashMap();
        params.put( "query", query );
        params.put( "ac", originalQuery );

        LazyDataModel<FeatureSummary>  modelledFeatures = LazyDataModelFactory.createLazyDataModel( modelledFeatureSummaryService,"modelledFeatureSummaryService",

                "select distinct p " +
                        "from IntactModelledFeature p left join p.dbXrefs as x " +
                        "where  p.ac = :ac " +
                        "      or lower(p.shortName) like :query " +
                        "      or lower(p.fullName) like :query " +
                        "      or lower(x.id) like :query ",

                "select count(distinct p) " +
                        "from IntactModelledFeature p left join p.dbXrefs as x " +
                        "where p.ac = :ac " +
                        "      or lower(p.shortName) like :query " +
                        "      or lower(p.fullName) like :query " +
                        "      or lower(x.id) like :query ",

                params, "p", "updated, p.ac", false);

        log.info( "Complex Features found: " + modelledFeatures.getRowCount() );
        return modelledFeatures;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<FeatureSummary>  loadImportedModelledFeatures(String query) {
        log.info( "Searching for complex features imported with '" + query  );

        final HashMap<String, String> params = Maps.<String, String>newHashMap();
        params.put( "query", query );
        params.put( "remark", "remark-internal" );

        LazyDataModel<FeatureSummary>  modelledFeatures = LazyDataModelFactory.createLazyDataModel( modelledFeatureSummaryService,"modelledFeatureSummaryService",

                "select distinct i " +
                        "from IntactModelledFeature i join i.annotations as a " +
                        "where a.value = :query and a.topic.shortName = :remark ",
                "select count(distinct i) " +
                        "from IntactModelledFeature i join i.annotations as a " +
                        "where a.value = :query and a.topic.shortName = :remark ",

                params, "i", "updated, i.ac", false);

        log.info( "Complex Features imported: " + modelledFeatures.getRowCount() );
        return modelledFeatures;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<ParticipantSummary> loadModelledParticipants(String finalQuery, String originalQuery) {
        log.info( "Searching for complex participants matching '" + finalQuery + "'..." );

        final HashMap<String, String> params = Maps.newHashMap();
        params.put( "query", finalQuery );
        params.put( "ac", originalQuery );

        // Load experiment eagerly to avoid LazyInitializationException when redering the view
        LazyDataModel<ParticipantSummary> modelledParticipants = LazyDataModelFactory.createLazyDataModel( modelledParticipantSummaryService,"modelledParticipantSummaryService",

                "select distinct p " +
                        "from IntactModelledParticipant p left join p.xrefs as x " +
                        "where  p.ac = :ac " +
                        "      or lower(x.id) like :query ",

                "select count(distinct p) " +
                        "from IntactModelledParticipant p left join p.xrefs as x " +
                        "where p.ac = :ac " +
                        "      or lower(x.id) like :query ",

                params, "p", "updated, p.ac", false);

        log.info( "Complex Participants found: " + modelledParticipants.getRowCount() );
        return modelledParticipants;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public LazyDataModel<ParticipantSummary> loadImportedModelledParticipants(String finalQuery) {
        log.info( "Searching for complex participants imported with '" + finalQuery + "'..." );

        final HashMap<String, String> params = Maps.newHashMap();
        params.put( "query", finalQuery );
        params.put( "remark", "remark-internal" );

        // Load experiment eagerly to avoid LazyInitializationException when redering the view
        LazyDataModel<ParticipantSummary> modelledParticipants = LazyDataModelFactory.createLazyDataModel( modelledParticipantSummaryService,"modelledParticipantSummaryService",

                "select distinct i " +
                        "from IntactModelledParticipant i join i.annotations as a " +
                        "where a.value = :query and a.topic.shortName = :remark ",
                "select count(distinct i) " +
                        "from IntactModelledParticipant i join i.annotations as a " +
                        "where a.value = :query and a.topic.shortName = :remark ",

                params, "i", "updated, i.ac", false);

        log.info( "Complex Participants imported: " + modelledParticipants.getRowCount() );
        return modelledParticipants;
    }
}
