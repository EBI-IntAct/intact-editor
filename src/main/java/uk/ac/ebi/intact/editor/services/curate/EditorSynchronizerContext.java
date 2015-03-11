package uk.ac.ebi.intact.editor.services.curate;

import psidev.psi.mi.jami.model.*;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.context.SynchronizerContext;
import uk.ac.ebi.intact.jami.context.UserContext;
import uk.ac.ebi.intact.jami.merger.IntactDbMergerIgnoringLocalObject;
import uk.ac.ebi.intact.jami.merger.UserMergerEnrichOnly;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.model.lifecycle.AbstractLifeCycleEvent;
import uk.ac.ebi.intact.jami.model.lifecycle.ComplexLifeCycleEvent;
import uk.ac.ebi.intact.jami.model.lifecycle.LifeCycleEvent;
import uk.ac.ebi.intact.jami.model.lifecycle.PublicationLifeCycleEvent;
import uk.ac.ebi.intact.jami.model.meta.Application;
import uk.ac.ebi.intact.jami.model.meta.ApplicationProperty;
import uk.ac.ebi.intact.jami.model.meta.DbInfo;
import uk.ac.ebi.intact.jami.model.user.Preference;
import uk.ac.ebi.intact.jami.model.user.Role;
import uk.ac.ebi.intact.jami.model.user.User;
import uk.ac.ebi.intact.jami.synchronizer.*;
import uk.ac.ebi.intact.jami.synchronizer.impl.*;
import uk.ac.ebi.intact.jami.synchronizer.listener.DbSynchronizerListener;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.persistence.EntityManager;

/**
 * Default implementation of SynchronizerContext
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19/02/14</pre>
 */

public class EditorSynchronizerContext implements SynchronizerContext {
    private EntityManager entityManager;

    private UserContext userContext;

    private DbSynchronizerListener listener;

    // cv synchronizer
    private IntactCvSynchronizer generalCvSynchronizer;
    private IntactCvSynchronizer databaseSynchronizer;
    private IntactCvSynchronizer qualifierSynchronizer;
    private IntactCvSynchronizer topicSynchronizer;
    private IntactCvSynchronizer aliasTypeSynchronizer;
    private IntactCvSynchronizer unitSynchronizer;
    private IntactCvSynchronizer featureTypeSynchronizer;
    private IntactCvSynchronizer experimentalRoleSynchronizer;
    private IntactCvSynchronizer biologicalRoleSynchronizer;
    private IntactCvSynchronizer interactionDetectionMethodSynchronizer;
    private IntactCvSynchronizer interactionTypeSynchronizer;
    private IntactCvSynchronizer participantDetectionMethodSynchronizer;
    private IntactCvSynchronizer experimentalPreparationSynchronizer;
    private IntactCvSynchronizer interactorTypeSynchronizer;
    private IntactCvSynchronizer rangeStatusSynchronizer;
    private IntactCvSynchronizer confidenceTypeSynchronizer;
    private IntactCvSynchronizer parameterTypeSynchronizer;
    private IntactCvSynchronizer cellTypeSynchronizer;
    private IntactCvSynchronizer tissueSynchronizer;
    private IntactCvSynchronizer featureDetectionMethodSynchronizer;
    private IntactCvSynchronizer lifecycleStatusSynchronizer;
    private IntactCvSynchronizer lifecycleEventSynchronizer;

    // source synchronizer
    private IntactSourceSynchronizer sourceSynchronizer;

    // aliases synchronizers
    private AliasSynchronizer aliasSynchronizer;

    // annotation synchronizers
    private AnnotationSynchronizer annotationSynchronizer;

    // xrefs synchronizers
    private XrefSynchronizer xrefSynchronizer;

    // cooperative effect synchronizers
    private CooperativeEffectSynchronizer cooperativeEffectSynchronizer;
    private CooperativeEffectSynchronizer preAssemblySynchronizer;
    private CooperativeEffectSynchronizer allosterySynchronizer;

    // interactor synchronizers
    private InteractorSynchronizer<Interactor, IntactInteractor> interactorSynchronizer;
    private InteractorSynchronizer interactorBaseSynchronizer;
    private InteractorSynchronizer polymerSynchronizer;
    private InteractorSynchronizer<Complex, IntactComplex> complexSynchronizer;
    private InteractorSynchronizer<InteractorPool, IntactInteractorPool> interactorPoolSynchronizer;

    // causal relationship synchronizers
    private IntactDbSynchronizer causalRelationshipSynchronizer;

    // confidence synchronizers
    private ConfidenceSynchronizer confidenceSynchronizer;

    // parameters synchronizers
    private ParameterSynchronizer parameterSynchronizer;

    // organism synchronizers
    private IntactDbSynchronizer<Organism, IntactOrganism> organismSynchronizer;

    // range synchronizers
    private IntactDbSynchronizer rangeSynchronizer;
    private IntactDbSynchronizer modelledRangeSynchronizer;
    private IntactDbSynchronizer experimentalRangeSynchronizer;

    // preference synchronizers
    private IntactDbSynchronizer<Preference, Preference> preferenceSynchronizer;

    // role synchronizers
    private IntactDbSynchronizer<Role, Role> roleSynchronizer;

    // user synchronizers
    private IntactDbSynchronizer<User, User> userSynchronizer;

    // publication synchronizers
    private IntactPublicationSynchronizer publicationSynchronizer;

    // experiment synchronizers
    private IntactExperimentSynchronizer experimentSynchronizer;

    // interaction synchronizers
    private IntactInteractionSynchronizer interactionEvidenceSynchronizer;

    // cooperativity evidence synchronizers
    private IntactDbSynchronizer<CooperativityEvidence, IntactCooperativityEvidence> cooperativityEvidenceSynchronizer;

    // lifecycle synchronizers
    private LifecycleEventSynchronizer lifecycleSynchronizer;

    // feature synchronizers
    private IntactDbSynchronizer<Feature, AbstractIntactFeature> featureSynchronizer;
    private IntactDbSynchronizer<FeatureEvidence, IntactFeatureEvidence> featureEvidenceSynchronizer;
    private IntactDbSynchronizer<ModelledFeature, IntactModelledFeature> modelledFeatureSynchronizer;

    // variable parameter synchronizers
    private IntactDbSynchronizer<VariableParameter, IntactVariableParameter> variableParameterSynchronizer;

    // variable parameter value synchronizers
    private IntactDbSynchronizer<VariableParameterValue, IntactVariableParameterValue> variableParameterValueSynchronizer;

    // variable parameter value set
    private IntactDbSynchronizer<VariableParameterValueSet, IntactVariableParameterValueSet> variableParameterValueSetSynchronizer;

    // participant synchronizers
    private ParticipantSynchronizer<Participant, AbstractIntactParticipant> participantSynchronizer;
    private ParticipantSynchronizer<ModelledParticipant, IntactModelledParticipant> modelledParticipantSynchronizer;
    private ParticipantSynchronizer<ParticipantEvidence, IntactParticipantEvidence> participantEvidenceSynchronizer;

    // complex xref synchronizer
    private XrefSynchronizer<InteractorXref> complexXrefSynchronizer;

    // db info snchronizer
    private IntactDbSynchronizer<DbInfo,DbInfo> dbInfoSynchronizer;
    // application synchronizer
    private IntactDbSynchronizer<Application,Application> applicationSynchronizer;
    // application property synchronizer
    private IntactDbSynchronizer<ApplicationProperty,ApplicationProperty> applicationPropertySynchronizer;

    public EditorSynchronizerContext(EntityManager entityManager){
        if (entityManager == null){
            throw new IllegalArgumentException("Entity manager cannot be null in an IntAct database synchronizer context");
        }
        this.entityManager = entityManager;
        this.userContext = ApplicationContextProvider.getBean("jamiUserContext");
    }

    public EditorSynchronizerContext(EntityManager entityManager, UserContext context){
        if (entityManager == null){
            throw new IllegalArgumentException("Entity manager cannot be null in an IntAct database synchronizer context");
        }
        this.entityManager = entityManager;
        this.userContext = context != null ? context : (UserContext) ApplicationContextProvider.getBean("jamiUserContext");
    }

    public EntityManager getEntityManager() {
        return this.entityManager;
    }

    public IntactCvSynchronizer getDatabaseSynchronizer() {
        if (this.databaseSynchronizer == null){
            this.databaseSynchronizer = new CvTermSynchronizer(this, IntactUtils.DATABASE_OBJCLASS);
            this.databaseSynchronizer.setListener(this.listener);
            this.databaseSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<CvTerm, IntactCvTerm>(this.databaseSynchronizer));
        }
        return databaseSynchronizer;
    }

    public IntactCvSynchronizer getQualifierSynchronizer() {
        if (this.qualifierSynchronizer == null){
            this.qualifierSynchronizer = new CvTermSynchronizer(this, IntactUtils.QUALIFIER_OBJCLASS);
            this.qualifierSynchronizer.setListener(this.listener);
            this.qualifierSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<CvTerm, IntactCvTerm>(this.qualifierSynchronizer));
        }
        return qualifierSynchronizer;
    }

    public IntactCvSynchronizer getTopicSynchronizer() {
        if (this.topicSynchronizer == null){
            this.topicSynchronizer = new CvTermSynchronizer(this, IntactUtils.TOPIC_OBJCLASS);
            this.topicSynchronizer.setListener(this.listener);
            this.topicSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<CvTerm, IntactCvTerm>(this.topicSynchronizer));
        }
        return topicSynchronizer;
    }

    public IntactCvSynchronizer getAliasTypeSynchronizer() {
        if (this.aliasTypeSynchronizer == null){
            this.aliasTypeSynchronizer = new CvTermSynchronizer(this, IntactUtils.ALIAS_TYPE_OBJCLASS);
            this.aliasTypeSynchronizer.setListener(this.listener);
            this.aliasTypeSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<CvTerm, IntactCvTerm>(this.aliasTypeSynchronizer));
        }
        return aliasTypeSynchronizer;
    }

    public IntactCvSynchronizer getUnitSynchronizer() {
        if (this.unitSynchronizer == null){
            this.unitSynchronizer = new CvTermSynchronizer(this, IntactUtils.UNIT_OBJCLASS);
            this.unitSynchronizer.setListener(this.listener);
            this.unitSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<CvTerm, IntactCvTerm>(this.unitSynchronizer));
        }
        return unitSynchronizer;
    }

    public IntactCvSynchronizer getFeatureTypeSynchronizer() {
        if (this.featureTypeSynchronizer == null){
            this.featureTypeSynchronizer = new CvTermSynchronizer(this, IntactUtils.FEATURE_TYPE_OBJCLASS);
            this.featureTypeSynchronizer.setListener(listener);
            this.featureTypeSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<CvTerm, IntactCvTerm>(this.featureTypeSynchronizer));
        }
        return featureTypeSynchronizer;
    }

    public IntactCvSynchronizer getExperimentalRoleSynchronizer() {
        if (this.experimentalRoleSynchronizer == null){
            this.experimentalRoleSynchronizer = new CvTermSynchronizer(this, IntactUtils.EXPERIMENTAL_ROLE_OBJCLASS);
            this.experimentalRoleSynchronizer.setListener(listener);
            this.experimentalRoleSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<CvTerm, IntactCvTerm>(this.experimentalRoleSynchronizer));
        }
        return experimentalRoleSynchronizer;
    }

    public IntactCvSynchronizer getBiologicalRoleSynchronizer() {
        if (this.biologicalRoleSynchronizer == null){
            this.biologicalRoleSynchronizer = new CvTermSynchronizer(this, IntactUtils.BIOLOGICAL_ROLE_OBJCLASS);
            this.biologicalRoleSynchronizer.setListener(listener);
            this.biologicalRoleSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<CvTerm, IntactCvTerm>(this.biologicalRoleSynchronizer));
        }
        return biologicalRoleSynchronizer;
    }

    public IntactCvSynchronizer getInteractionDetectionMethodSynchronizer() {
        if (this.interactionDetectionMethodSynchronizer == null){
            this.interactionDetectionMethodSynchronizer = new CvTermSynchronizer(this, IntactUtils.INTERACTION_DETECTION_METHOD_OBJCLASS);
            this.interactionDetectionMethodSynchronizer.setListener(listener);
            this.interactionDetectionMethodSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<CvTerm, IntactCvTerm>(this.interactionDetectionMethodSynchronizer));
        }
        return interactionDetectionMethodSynchronizer;
    }

    public IntactCvSynchronizer getInteractionTypeSynchronizer() {
        if (this.interactionTypeSynchronizer == null){
            this.interactionTypeSynchronizer = new CvTermSynchronizer(this, IntactUtils.INTERACTION_TYPE_OBJCLASS);
            this.interactionTypeSynchronizer.setListener(listener);
            this.interactionTypeSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<CvTerm, IntactCvTerm>(this.interactionTypeSynchronizer));
        }
        return interactionTypeSynchronizer;
    }

    public IntactCvSynchronizer getParticipantDetectionMethodSynchronizer() {
        if (this.participantDetectionMethodSynchronizer == null){
            this.participantDetectionMethodSynchronizer = new CvTermSynchronizer(this, IntactUtils.PARTICIPANT_DETECTION_METHOD_OBJCLASS);
            this.participantDetectionMethodSynchronizer.setListener(listener);
            this.participantDetectionMethodSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<CvTerm, IntactCvTerm>(this.participantDetectionMethodSynchronizer));
        }
        return participantDetectionMethodSynchronizer;
    }

    public IntactCvSynchronizer getExperimentalPreparationSynchronizer() {
        if (this.experimentalPreparationSynchronizer == null){
            this.experimentalPreparationSynchronizer = new CvTermSynchronizer(this, IntactUtils.PARTICIPANT_EXPERIMENTAL_PREPARATION_OBJCLASS);
            this.experimentalPreparationSynchronizer.setListener(listener);
            this.experimentalPreparationSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<CvTerm, IntactCvTerm>(this.experimentalPreparationSynchronizer));
        }
        return experimentalPreparationSynchronizer;
    }

    public IntactCvSynchronizer getInteractorTypeSynchronizer() {
        if (this.interactorTypeSynchronizer == null){
            this.interactorTypeSynchronizer = new CvTermSynchronizer(this, IntactUtils.INTERACTOR_TYPE_OBJCLASS);
            this.interactorTypeSynchronizer.setListener(listener);
            this.interactorTypeSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<CvTerm, IntactCvTerm>(this.interactorTypeSynchronizer));
        }
        return interactorTypeSynchronizer;
    }

    public IntactCvSynchronizer getRangeStatusSynchronizer() {
        if (this.rangeStatusSynchronizer == null){
            this.rangeStatusSynchronizer = new CvTermSynchronizer(this, IntactUtils.RANGE_STATUS_OBJCLASS);
            this.rangeStatusSynchronizer.setListener(listener);
            this.rangeStatusSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<CvTerm, IntactCvTerm>(this.rangeStatusSynchronizer));
        }
        return rangeStatusSynchronizer;
    }

    public IntactCvSynchronizer getConfidenceTypeSynchronizer() {
        if (this.confidenceTypeSynchronizer == null){
            this.confidenceTypeSynchronizer = new CvTermSynchronizer(this, IntactUtils.CONFIDENCE_TYPE_OBJCLASS);
            this.confidenceTypeSynchronizer.setListener(listener);
            this.confidenceTypeSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<CvTerm, IntactCvTerm>(this.confidenceTypeSynchronizer));
        }
        return confidenceTypeSynchronizer;
    }

    public IntactCvSynchronizer getParameterTypeSynchronizer() {
        if (this.parameterTypeSynchronizer == null){
            this.parameterTypeSynchronizer = new CvTermSynchronizer(this, IntactUtils.PARAMETER_TYPE_OBJCLASS);
            this.parameterTypeSynchronizer.setListener(listener);
            this.parameterTypeSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<CvTerm, IntactCvTerm>(this.parameterTypeSynchronizer));
        }
        return parameterTypeSynchronizer;
    }

    public IntactCvSynchronizer getCellTypeSynchronizer() {
        if (this.cellTypeSynchronizer == null){
            this.cellTypeSynchronizer = new CvTermSynchronizer(this, IntactUtils.CELL_TYPE_OBJCLASS);
            this.cellTypeSynchronizer.setListener(listener);
            this.cellTypeSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<CvTerm, IntactCvTerm>(this.cellTypeSynchronizer));
        }
        return cellTypeSynchronizer;
    }

    public IntactCvSynchronizer getTissueSynchronizer() {
        if (this.tissueSynchronizer == null){
            this.tissueSynchronizer = new CvTermSynchronizer(this, IntactUtils.TISSUE_OBJCLASS);
            this.tissueSynchronizer.setListener(listener);
            this.tissueSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<CvTerm, IntactCvTerm>(this.tissueSynchronizer));
        }
        return tissueSynchronizer;
    }

    public IntactCvSynchronizer getFeatureDetectionMethodSynchronizer() {
        if (this.featureDetectionMethodSynchronizer == null){
            this.featureDetectionMethodSynchronizer = new CvTermSynchronizer(this, IntactUtils.FEATURE_METHOD_OBJCLASS);
            this.featureDetectionMethodSynchronizer.setListener(listener);
            this.featureDetectionMethodSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<CvTerm, IntactCvTerm>(this.featureDetectionMethodSynchronizer));
        }
        return featureDetectionMethodSynchronizer;
    }

    public IntactCvSynchronizer getLifecycleStatusSynchronizer() {
        if (this.lifecycleStatusSynchronizer == null){
            this.lifecycleStatusSynchronizer = new CvTermSynchronizer(this, IntactUtils.PUBLICATION_STATUS_OBJCLASS);
            this.lifecycleStatusSynchronizer.setListener(listener);
            this.lifecycleStatusSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<CvTerm, IntactCvTerm>(this.lifecycleStatusSynchronizer));
        }
        return lifecycleStatusSynchronizer;
    }

    public IntactCvSynchronizer getLifecycleEventSynchronizer() {
        if (this.lifecycleEventSynchronizer == null){
            this.lifecycleEventSynchronizer = new CvTermSynchronizer(this, IntactUtils.LIFECYCLE_EVENT_OBJCLASS);
            this.lifecycleEventSynchronizer.setListener(listener);
            this.lifecycleEventSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<CvTerm, IntactCvTerm>(this.lifecycleSynchronizer));
        }
        return lifecycleEventSynchronizer;
    }

    public <A extends AbstractIntactAlias> AliasSynchronizer<A> getAliasSynchronizer(Class<A> aliasclass) {
        initialiseAliasTemplateIfNotDone();
        this.aliasSynchronizer.setIntactClass(aliasclass);
        return this.aliasSynchronizer;
    }

    public <A extends AbstractIntactAnnotation> AnnotationSynchronizer<A> getAnnotationSynchronizer(Class<A> annotationclass) {
        initialiseAnnotationTemplateIfNotDone();
        this.annotationSynchronizer.setIntactClass(annotationclass);
        return this.annotationSynchronizer;
    }

    public <A extends AbstractIntactXref> XrefSynchronizer<A> getXrefSynchronizer(Class<A> xrefclass) {
        initialiseXrefTemplateIfNotDone();
        this.xrefSynchronizer.setIntactClass(xrefclass);
        return this.xrefSynchronizer;
    }

    public <A extends AbstractIntactConfidence> ConfidenceSynchronizer<Confidence, A> getConfidenceSynchronizer(Class<A> confidenceclass) {
        initialiseConfidenceTemplateIfNotDone();
        this.confidenceSynchronizer.setIntactClass(confidenceclass);
        return this.confidenceSynchronizer;
    }

    public <A extends AbstractIntactParameter> ParameterSynchronizer<Parameter, A> getParameterSynchronizer(Class<A> parameterclass) {
        initialiseParameterTemplateIfNotDone();
        this.parameterSynchronizer.setIntactClass(parameterclass);
        return this.parameterSynchronizer;
    }

    public <A extends AbstractLifeCycleEvent> LifecycleEventSynchronizer<A> getLifecycleSynchronizer(Class<A> eventclass) {
        initialiseLifecycleTemplateIfNotDone();
        this.lifecycleSynchronizer.setIntactClass(eventclass);
        return this.lifecycleSynchronizer;
    }

    public AliasSynchronizer<CvTermAlias> getCvAliasSynchronizer() {
        initialiseAliasTemplateIfNotDone();
        this.aliasSynchronizer.setIntactClass(CvTermAlias.class);
        return this.aliasSynchronizer;
    }

    public XrefSynchronizer<CvTermXref> getCvXrefSynchronizer() {
        initialiseXrefTemplateIfNotDone();
        this.xrefSynchronizer.setIntactClass(CvTermXref.class);
        return this.xrefSynchronizer;
    }

    public AnnotationSynchronizer<CvTermAnnotation> getCvAnnotationSynchronizer() {
        initialiseAnnotationTemplateIfNotDone();
        this.annotationSynchronizer.setIntactClass(CvTermAnnotation.class);
        return this.annotationSynchronizer;
    }

    public IntactSourceSynchronizer getSourceSynchronizer() {
        if (this.sourceSynchronizer == null){
            this.sourceSynchronizer = new SourceSynchronizer(this);
            this.sourceSynchronizer.setListener(listener);
            this.sourceSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<Source, IntactSource>(this.sourceSynchronizer));
        }
        return sourceSynchronizer;
    }

    public AliasSynchronizer<SourceAlias> getSourceAliasSynchronizer() {
        initialiseAliasTemplateIfNotDone();
        this.aliasSynchronizer.setIntactClass(SourceAlias.class);
        return this.aliasSynchronizer;
    }

    public XrefSynchronizer<SourceXref> getSourceXrefSynchronizer() {
        initialiseXrefTemplateIfNotDone();
        this.xrefSynchronizer.setIntactClass(SourceXref.class);
        return this.xrefSynchronizer;
    }

    public AnnotationSynchronizer<SourceAnnotation> getSourceAnnotationSynchronizer() {
        initialiseAnnotationTemplateIfNotDone();
        this.annotationSynchronizer.setIntactClass(SourceAnnotation.class);
        return this.annotationSynchronizer;
    }

    public AliasSynchronizer<OrganismAlias> getOrganismAliasSynchronizer() {
        initialiseAliasTemplateIfNotDone();
        this.aliasSynchronizer.setIntactClass(OrganismAlias.class);
        return this.aliasSynchronizer;
    }

    public AliasSynchronizer<FeatureEvidenceAlias> getFeatureEvidenceAliasSynchronizer() {
        initialiseAliasTemplateIfNotDone();
        this.aliasSynchronizer.setIntactClass(FeatureEvidenceAlias.class);
        return this.aliasSynchronizer;
    }

    public AliasSynchronizer<ModelledFeatureAlias> getModelledFeatureAliasSynchronizer() {
        initialiseAliasTemplateIfNotDone();
        this.aliasSynchronizer.setIntactClass(ModelledFeatureAlias.class);
        return this.aliasSynchronizer;
    }

    public AliasSynchronizer<ParticipantEvidenceAlias> getParticipantEvidenceAliasSynchronizer() {
        initialiseAliasTemplateIfNotDone();
        this.aliasSynchronizer.setIntactClass(ParticipantEvidenceAlias.class);
        return this.aliasSynchronizer;
    }

    public AliasSynchronizer<ModelledParticipantAlias> getModelledParticipantAliasSynchronizer() {
        initialiseAliasTemplateIfNotDone();
        this.aliasSynchronizer.setIntactClass(ModelledParticipantAlias.class);
        return this.aliasSynchronizer;
    }

    public AliasSynchronizer<InteractorAlias> getInteractorAliasSynchronizer() {
        initialiseAliasTemplateIfNotDone();
        this.aliasSynchronizer.setIntactClass(InteractorAlias.class);
        return this.aliasSynchronizer;
    }

    public XrefSynchronizer<PublicationXref> getPublicationXrefSynchronizer() {
        initialiseXrefTemplateIfNotDone();
        this.xrefSynchronizer.setIntactClass(PublicationXref.class);
        return this.xrefSynchronizer;
    }

    public XrefSynchronizer<ExperimentXref> getExperimentXrefSynchronizer() {
        initialiseXrefTemplateIfNotDone();
        this.xrefSynchronizer.setIntactClass(ExperimentXref.class);
        return this.xrefSynchronizer;
    }

    public XrefSynchronizer<InteractionXref> getInteractionXrefSynchronizer() {
        initialiseXrefTemplateIfNotDone();
        this.xrefSynchronizer.setIntactClass(InteractionXref.class);
        return this.xrefSynchronizer;
    }

    public XrefSynchronizer<InteractorXref> getInteractorXrefSynchronizer() {
        initialiseXrefTemplateIfNotDone();
        this.xrefSynchronizer.setIntactClass(InteractorXref.class);
        return this.xrefSynchronizer;
    }

    public XrefSynchronizer<FeatureEvidenceXref> getFeatureEvidenceXrefSynchronizer() {
        initialiseXrefTemplateIfNotDone();
        this.xrefSynchronizer.setIntactClass(FeatureEvidenceXref.class);
        return this.xrefSynchronizer;
    }

    public XrefSynchronizer<ModelledFeatureXref> getModelledFeatureXrefSynchronizer() {
        initialiseXrefTemplateIfNotDone();
        this.xrefSynchronizer.setIntactClass(ModelledFeatureXref.class);
        return this.xrefSynchronizer;
    }

    public XrefSynchronizer<ParticipantEvidenceXref> getParticipantEvidenceXrefSynchronizer() {
        initialiseXrefTemplateIfNotDone();
        this.xrefSynchronizer.setIntactClass(ParticipantEvidenceXref.class);
        return this.xrefSynchronizer;
    }

    public XrefSynchronizer<ModelledParticipantXref> getModelledParticipantXrefSynchronizer() {
        initialiseXrefTemplateIfNotDone();
        this.xrefSynchronizer.setIntactClass(ModelledParticipantXref.class);
        return this.xrefSynchronizer;
    }

    public XrefSynchronizer<ExperimentalResultingSequenceXref> getExperimentalResultingSequenceXrefSynchronizer() {
        initialiseXrefTemplateIfNotDone();
        this.xrefSynchronizer.setIntactClass(ExperimentalResultingSequenceXref.class);
        return this.xrefSynchronizer;
    }

    public XrefSynchronizer<ModelledResultingSequenceXref> getModelledResultingSequenceXrefSynchronizer() {
        initialiseXrefTemplateIfNotDone();
        this.xrefSynchronizer.setIntactClass(ModelledResultingSequenceXref.class);
        return this.xrefSynchronizer;
    }

    public AnnotationSynchronizer<PublicationAnnotation> getPublicationAnnotationSynchronizer() {
        initialiseAnnotationTemplateIfNotDone();
        this.annotationSynchronizer.setIntactClass(PublicationAnnotation.class);
        return this.annotationSynchronizer;
    }

    public AnnotationSynchronizer<ExperimentAnnotation> getExperimentAnnotationSynchronizer() {
        initialiseAnnotationTemplateIfNotDone();
        this.annotationSynchronizer.setIntactClass(ExperimentAnnotation.class);
        return this.annotationSynchronizer;
    }

    public AnnotationSynchronizer<InteractionAnnotation> getInteractionAnnotationSynchronizer() {
        initialiseAnnotationTemplateIfNotDone();
        this.annotationSynchronizer.setIntactClass(InteractionAnnotation.class);
        return this.annotationSynchronizer;
    }

    public AnnotationSynchronizer<InteractorAnnotation> getInteractorAnnotationSynchronizer() {
        initialiseAnnotationTemplateIfNotDone();
        this.annotationSynchronizer.setIntactClass(InteractorAnnotation.class);
        return this.annotationSynchronizer;
    }

    public AnnotationSynchronizer<FeatureEvidenceAnnotation> getFeatureEvidenceAnnotationSynchronizer() {
        initialiseAnnotationTemplateIfNotDone();
        this.annotationSynchronizer.setIntactClass(FeatureEvidenceAnnotation.class);
        return this.annotationSynchronizer;
    }

    public AnnotationSynchronizer<ModelledFeatureAnnotation> getModelledFeatureAnnotationSynchronizer() {
        initialiseAnnotationTemplateIfNotDone();
        this.annotationSynchronizer.setIntactClass(ModelledFeatureAnnotation.class);
        return this.annotationSynchronizer;
    }

    public AnnotationSynchronizer<ParticipantEvidenceAnnotation> getParticipantEvidenceAnnotationSynchronizer() {
        initialiseAnnotationTemplateIfNotDone();
        this.annotationSynchronizer.setIntactClass(ParticipantEvidenceAnnotation.class);
        return this.annotationSynchronizer;
    }

    public AnnotationSynchronizer<ModelledParticipantAnnotation> getModelledParticipantAnnotationSynchronizer() {
        initialiseAnnotationTemplateIfNotDone();
        this.annotationSynchronizer.setIntactClass(ModelledParticipantAnnotation.class);
        return this.annotationSynchronizer;
    }

    public AnnotationSynchronizer<CooperativeEffectAnnotation> getCooperativeEffectAnnotationSynchronizer() {
        initialiseAnnotationTemplateIfNotDone();
        this.annotationSynchronizer.setIntactClass(CooperativeEffectAnnotation.class);
        return this.annotationSynchronizer;
    }

    public CooperativeEffectSynchronizer<CooperativeEffect, AbstractIntactCooperativeEffect> getCooperativeEffectSynchronizer() {
        if (this.cooperativeEffectSynchronizer == null){
           this.cooperativeEffectSynchronizer = new CompositeCooperativeEffectSynchronizer(this);
            this.cooperativeEffectSynchronizer.setListener(listener);
        }
        return cooperativeEffectSynchronizer;
    }

    public CooperativeEffectSynchronizer<Preassembly, IntactPreassembly> getPreAssemblySynchronizer() {
        if (this.preAssemblySynchronizer == null){
            this.preAssemblySynchronizer = new CooperativeEffectSynchronizerTemplate(this, IntactPreassembly.class);
            this.preAssemblySynchronizer.setListener(listener);
        }
        return this.preAssemblySynchronizer;
    }

    public CooperativeEffectSynchronizer<Allostery, AbstractIntactAllostery> getAllosterySynchronizer() {
        if (this.allosterySynchronizer == null){
            this.allosterySynchronizer = new AllosterySynchronizer(this);
            this.allosterySynchronizer.setListener(listener);
        }
        return this.allosterySynchronizer;
    }

    public InteractorSynchronizer<Complex, IntactComplex> getComplexSynchronizer() {
        if (this.complexSynchronizer == null){
           this.complexSynchronizer = new ComplexSynchronizer(this);
            this.complexSynchronizer.setListener(listener);
        }
        return this.complexSynchronizer;
    }

    public InteractorSynchronizer<Interactor, IntactInteractor> getInteractorSynchronizer() {
        if (this.interactorSynchronizer == null){
            this.interactorSynchronizer = new CompositeInteractorSynchronizer(this);
            this.interactorSynchronizer.setListener(listener);
        }
        return this.interactorSynchronizer;
    }

    public InteractorSynchronizer<Interactor, IntactInteractor> getInteractorBaseSynchronizer() {
        initialiseInteractorTemplateIfNotDone();
        this.interactorBaseSynchronizer.setIntactClass(IntactInteractor.class);
        return this.interactorBaseSynchronizer;
    }

    public InteractorSynchronizer<Polymer, IntactPolymer> getPolymerSynchronizer() {
        initialisePolymerTemplateIfNotDone();
        this.polymerSynchronizer.setIntactClass(IntactPolymer.class);
        return this.polymerSynchronizer;
    }

    public InteractorSynchronizer<Protein, IntactProtein> getProteinSynchronizer() {
        initialisePolymerTemplateIfNotDone();
        this.polymerSynchronizer.setIntactClass(IntactProtein.class);
        return this.polymerSynchronizer;
    }

    public InteractorSynchronizer<NucleicAcid, IntactNucleicAcid> getNucleicAcidSynchronizer() {
        initialisePolymerTemplateIfNotDone();
        this.polymerSynchronizer.setIntactClass(IntactNucleicAcid.class);
        return this.polymerSynchronizer;
    }

    public InteractorSynchronizer<Molecule, IntactMolecule> getMoleculeSynchronizer() {
        initialiseInteractorTemplateIfNotDone();
        this.interactorBaseSynchronizer.setIntactClass(IntactMolecule.class);
        return this.interactorBaseSynchronizer;
    }

    public InteractorSynchronizer<Gene, IntactGene> getGeneSynchronizer() {
        initialiseInteractorTemplateIfNotDone();
        this.interactorBaseSynchronizer.setIntactClass(IntactGene.class);
        return this.interactorBaseSynchronizer;
    }

    public InteractorSynchronizer<InteractorPool, IntactInteractorPool> getInteractorPoolSynchronizer() {
        if (this.interactorPoolSynchronizer == null){
            this.interactorPoolSynchronizer = new InteractorPoolSynchronizer(this);
            this.interactorPoolSynchronizer.setListener(listener);
        }
        return this.interactorPoolSynchronizer;
    }

    public InteractorSynchronizer<BioactiveEntity, IntactBioactiveEntity> getBioactiveEntitySynchronizer() {
        initialiseInteractorTemplateIfNotDone();
        this.interactorBaseSynchronizer.setIntactClass(IntactBioactiveEntity.class);
        return this.interactorBaseSynchronizer;
    }

    public <T extends AbstractIntactCausalRelationship> IntactDbSynchronizer<CausalRelationship, T> getCausalRelationshipSynchronizer(Class<T> intactClass) {
        initializeCausalRelationshipSynchronizerIfNotDone();
        this.causalRelationshipSynchronizer.setIntactClass(intactClass);
        return this.causalRelationshipSynchronizer;
    }

    public IntactDbSynchronizer<CausalRelationship, ModelledCausalRelationship> getModelledCausalRelationshipSynchronizer() {
        return getCausalRelationshipSynchronizer(ModelledCausalRelationship.class);
    }

    public IntactDbSynchronizer<CausalRelationship, ExperimentalCausalRelationship> getExperimentalCausalRelationshipSynchronizer() {
        return getCausalRelationshipSynchronizer(ExperimentalCausalRelationship.class);
    }

    public ConfidenceSynchronizer<Confidence, InteractionEvidenceConfidence> getInteractionConfidenceSynchronizer() {
        initialiseConfidenceTemplateIfNotDone();
        this.confidenceSynchronizer.setIntactClass(InteractionEvidenceConfidence.class);
        return this.confidenceSynchronizer;
    }

    public ConfidenceSynchronizer<ModelledConfidence, ComplexConfidence> getComplexConfidenceSynchronizer() {
        initialiseConfidenceTemplateIfNotDone();
        this.confidenceSynchronizer.setIntactClass(ComplexConfidence.class);
        return this.confidenceSynchronizer;
    }

    public ConfidenceSynchronizer<Confidence, ParticipantEvidenceConfidence> getParticipantEvidenceConfidenceSynchronizer() {
        initialiseConfidenceTemplateIfNotDone();
        this.confidenceSynchronizer.setIntactClass(ParticipantEvidenceConfidence.class);
        return this.confidenceSynchronizer;
    }

    public ParameterSynchronizer<Parameter, InteractionEvidenceParameter> getInteractionParameterSynchronizer(){
        initialiseParameterTemplateIfNotDone();
        this.parameterSynchronizer.setIntactClass(InteractionEvidenceParameter.class);
        return this.parameterSynchronizer;
    }

    public ParameterSynchronizer<Parameter, ParticipantEvidenceParameter> getParticipantEvidenceParameterSynchronizer(){
        initialiseParameterTemplateIfNotDone();
        this.parameterSynchronizer.setIntactClass(ParticipantEvidenceParameter.class);
        return this.parameterSynchronizer;
    }

    public ParameterSynchronizer<Parameter, FeatureEvidenceParameter> getFeatureParameterSynchronizer() {
        initialiseParameterTemplateIfNotDone();
        this.parameterSynchronizer.setIntactClass(FeatureEvidenceParameter.class);
        return this.parameterSynchronizer;
    }

    public ParameterSynchronizer<ModelledParameter, ComplexParameter> getComplexParameterSynchronizer() {
        initialiseParameterTemplateIfNotDone();
        this.parameterSynchronizer.setIntactClass(ComplexParameter.class);
        return this.parameterSynchronizer;
    }

    public IntactDbSynchronizer<Organism, IntactOrganism> getOrganismSynchronizer() {
        if (this.organismSynchronizer == null){
            this.organismSynchronizer = new OrganismSynchronizer(this);
            this.organismSynchronizer.setListener(listener);
            this.organismSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<Organism, IntactOrganism>(this.organismSynchronizer));
        }
        return organismSynchronizer;
    }

    public <I extends AbstractIntactRange> IntactDbSynchronizer<Range, I> getRangeSynchronizer(Class<I> intactClass) {
        if (this.rangeSynchronizer == null){
            this.rangeSynchronizer = new RangeSynchronizerTemplate(this, AbstractIntactRange.class);
            this.rangeSynchronizer.setListener(listener);
        }
        this.rangeSynchronizer.setIntactClass(intactClass);
        return rangeSynchronizer;
    }

    public IntactDbSynchronizer<Range, ModelledRange> getModelledRangeSynchronizer() {
        if (this.modelledRangeSynchronizer == null){
            this.modelledRangeSynchronizer = new ModelledRangeSynchronizer(this);
            this.modelledRangeSynchronizer.setListener(listener);
        }
        return this.modelledRangeSynchronizer;
    }

    public IntactDbSynchronizer<Range, ExperimentalRange> getExperimentalRangeSynchronizer() {
        if (this.experimentalRangeSynchronizer == null){
            this.experimentalRangeSynchronizer = new ExperimentalRangeSynchronizer(this);
            this.experimentalRangeSynchronizer.setListener(listener);
        }
        return this.experimentalRangeSynchronizer;
    }

    public IntactDbSynchronizer<Preference, Preference> getPreferenceSynchronizer() {
        if (this.preferenceSynchronizer == null){
            this.preferenceSynchronizer = new PreferenceSynchronizer(this);
            this.preferenceSynchronizer.setListener(listener);
        }
        return preferenceSynchronizer;
    }

    public IntactDbSynchronizer<Role, Role> getRoleSynchronizer() {
        if (this.roleSynchronizer == null){
            this.roleSynchronizer = new RoleSynchronizer(this);
            this.roleSynchronizer.setListener(listener);
        }
        return roleSynchronizer;
    }

    public IntactDbSynchronizer<User, User> getUserSynchronizer() {
        if (this.userSynchronizer == null){
            this.userSynchronizer = new UserSynchronizer(this);
            this.userSynchronizer.setListener(listener);
        }
        else{
            this.userSynchronizer.setIntactMerger(new UserMergerEnrichOnly());
        }
        return userSynchronizer;
    }

    public IntactDbSynchronizer<User, User> getUserReadOnlySynchronizer() {
        if (this.userSynchronizer == null){
            this.userSynchronizer = new UserSynchronizer(this);
            this.userSynchronizer.setListener(listener);
        }
        this.userSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<User, User>(this.userSynchronizer));
        return userSynchronizer;
    }

    public IntactPublicationSynchronizer getPublicationSynchronizer() {
        if (this.publicationSynchronizer == null){
            this.publicationSynchronizer = new PublicationSynchronizer(this);
            this.publicationSynchronizer.setListener(listener);
        }
        return publicationSynchronizer;
    }

    public IntactExperimentSynchronizer getExperimentSynchronizer() {
        if (this.experimentSynchronizer == null){
            this.experimentSynchronizer = new uk.ac.ebi.intact.jami.synchronizer.impl.ExperimentSynchronizer(this);
            this.experimentSynchronizer.setListener(listener);
        }
        return experimentSynchronizer;
    }

    public IntactInteractionSynchronizer getInteractionSynchronizer() {
        if (this.interactionEvidenceSynchronizer == null){
            this.interactionEvidenceSynchronizer = new InteractionEvidenceSynchronizer(this);
            this.interactionEvidenceSynchronizer.setListener(listener);
        }
        return interactionEvidenceSynchronizer;
    }

    public IntactDbSynchronizer<CooperativityEvidence, IntactCooperativityEvidence> getCooperativityEvidenceSynchronizer() {
        if (this.cooperativityEvidenceSynchronizer == null){
            this.cooperativityEvidenceSynchronizer = new CooperativityEvidenceSynchronizer(this);
            this.cooperativityEvidenceSynchronizer.setListener(listener);
        }
        return cooperativityEvidenceSynchronizer;
    }

    public IntactDbSynchronizer<LifeCycleEvent, ComplexLifeCycleEvent> getComplexLifecycleSynchronizer() {
        initialiseLifecycleTemplateIfNotDone();
        this.lifecycleSynchronizer.setIntactClass(ComplexLifeCycleEvent.class);
        return this.lifecycleSynchronizer;
    }

    public IntactDbSynchronizer<LifeCycleEvent, PublicationLifeCycleEvent> getPublicationLifecycleSynchronizer() {
        initialiseLifecycleTemplateIfNotDone();
        this.lifecycleSynchronizer.setIntactClass(PublicationLifeCycleEvent.class);
        return this.lifecycleSynchronizer;
    }

    public IntactDbSynchronizer<Feature, AbstractIntactFeature> getFeatureSynchronizer(){
        if (this.featureSynchronizer == null){
            this.featureSynchronizer = new CompositeFeatureSynchronizer(this);
            this.featureSynchronizer.setListener(listener);
        }
        return this.featureSynchronizer;
    }

    public IntactDbSynchronizer<FeatureEvidence, IntactFeatureEvidence> getFeatureEvidenceSynchronizer() {
        if (this.featureEvidenceSynchronizer == null){
            this.featureEvidenceSynchronizer = new FeatureEvidenceSynchronizer(this);
            this.featureEvidenceSynchronizer.setListener(listener);
        }
        return featureEvidenceSynchronizer;
    }

    public IntactDbSynchronizer<ModelledFeature, IntactModelledFeature> getModelledFeatureSynchronizer() {
        if (this.modelledFeatureSynchronizer == null){
            this.modelledFeatureSynchronizer = new ModelledFeatureSynchronizer(this);
            this.modelledFeatureSynchronizer.setListener(listener);
        }
        return modelledFeatureSynchronizer;
    }

    public IntactDbSynchronizer<VariableParameter, IntactVariableParameter> getVariableParameterSynchronizer() {
        if (this.variableParameterSynchronizer == null){
            this.variableParameterSynchronizer = new VariableParameterSynchronizer(this);
            this.variableParameterSynchronizer.setListener(listener);
        }
        return variableParameterSynchronizer;
    }

    public IntactDbSynchronizer<VariableParameterValue, IntactVariableParameterValue> getVariableParameterValueSynchronizer() {
        if (this.variableParameterValueSynchronizer == null){
           this.variableParameterValueSynchronizer = new VariableParameterValueSynchronizer(this);
            this.variableParameterValueSynchronizer.setListener(listener);
        }
        return variableParameterValueSynchronizer;
    }

    public IntactDbSynchronizer<VariableParameterValueSet, IntactVariableParameterValueSet> getVariableParameterValueSetSynchronizer() {
        if (this.variableParameterValueSetSynchronizer == null){
            this.variableParameterValueSetSynchronizer = new VariableParameterValueSetSynchronizer(this);
            this.variableParameterValueSetSynchronizer.setListener(listener);
        }
        return variableParameterValueSetSynchronizer;
    }

    public ParticipantSynchronizer<Participant, AbstractIntactParticipant> getParticipantSynchronizer() {
        if (this.participantSynchronizer == null){
            this.participantSynchronizer = new CompositeParticipantSynchronizer(this);
            this.participantSynchronizer.setListener(listener);
        }
        return participantSynchronizer;
    }

    public ParticipantSynchronizer<ModelledParticipant, IntactModelledParticipant> getModelledParticipantSynchronizer() {
        if (this.modelledParticipantSynchronizer == null){
            this.modelledParticipantSynchronizer = new ModelledParticipantSynchronizer(this);
            this.modelledParticipantSynchronizer.setListener(listener);
        }
        return modelledParticipantSynchronizer;
    }


    public ParticipantSynchronizer<ParticipantEvidence, IntactParticipantEvidence> getParticipantEvidenceSynchronizer(){
        if (this.participantEvidenceSynchronizer == null){
            this.participantEvidenceSynchronizer = new ParticipantEvidenceSynchronizer(this);
            this.participantEvidenceSynchronizer.setListener(listener);
        }
        return participantEvidenceSynchronizer;
    }

    @Override
    public XrefSynchronizer<InteractorXref> getComplexXrefSynchronizer() {
        if (this.complexXrefSynchronizer == null){
            this.complexXrefSynchronizer = new ComplexXrefSynchronizerTemplate(this);
            this.complexXrefSynchronizer.setListener(listener);
        }
        return complexXrefSynchronizer;
    }

    @Override
    public IntactDbSynchronizer<DbInfo, DbInfo> getDbInfoSynchronizer() {
        if (this.dbInfoSynchronizer == null){
            this.dbInfoSynchronizer = new DbInfoSynchronizer(this);
            this.dbInfoSynchronizer.setListener(listener);
        }
        return this.dbInfoSynchronizer;
    }

    @Override
    public IntactDbSynchronizer<Application, Application> getApplicationSynchronizer() {
        if (this.applicationSynchronizer == null){
            this.applicationSynchronizer = new ApplicationSynchronizer(this);
            this.applicationSynchronizer.setListener(listener);
        }
        return this.applicationSynchronizer;
    }

    @Override
    public IntactDbSynchronizer<ApplicationProperty, ApplicationProperty> getApplicationPropertySynchronizer() {
        if (this.applicationPropertySynchronizer == null){
            this.applicationPropertySynchronizer = new ApplicationPropertySynchronizer(this);
            this.applicationPropertySynchronizer.setListener(listener);
        }
        return this.applicationPropertySynchronizer;
    }

    @Override
    public UserContext getUserContext() {
        return this.userContext;
    }

    @Override
    public DbSynchronizerListener getSynchronizerListener() {
        return this.listener;
    }

    @Override
    public void initialiseDbSynchronizerListener(DbSynchronizerListener listener) {
        this.listener = listener;
        initListener(this.databaseSynchronizer);
        initListener(this.qualifierSynchronizer);
        initListener(this.topicSynchronizer);
        initListener(this.aliasTypeSynchronizer);
        initListener(this.unitSynchronizer);
        initListener(this.featureTypeSynchronizer);
        initListener(this.experimentalRoleSynchronizer);
        initListener(this.biologicalRoleSynchronizer);
        initListener(this.interactionDetectionMethodSynchronizer);
        initListener(this.interactionTypeSynchronizer);
        initListener(this.participantDetectionMethodSynchronizer);
        initListener(this.experimentalPreparationSynchronizer);
        initListener(this.interactorTypeSynchronizer);
        initListener(this.rangeStatusSynchronizer);
        initListener(this.confidenceTypeSynchronizer);
        initListener(this.parameterTypeSynchronizer);
        initListener(this.cellTypeSynchronizer);
        initListener(this.tissueSynchronizer);
        initListener(this.featureDetectionMethodSynchronizer);
        initListener(this.lifecycleStatusSynchronizer);
        initListener(this.lifecycleEventSynchronizer);
        initListener(this.sourceSynchronizer);
        initListener(this.aliasSynchronizer);
        initListener(this.annotationSynchronizer);
        initListener(this.xrefSynchronizer);
        initListener(this.complexXrefSynchronizer);
        initListener(this.cooperativeEffectSynchronizer);
        initListener(this.preAssemblySynchronizer);
        initListener(this.allosterySynchronizer);
        initListener(this.interactorSynchronizer);
        initListener(this.interactorBaseSynchronizer);
        initListener(this.polymerSynchronizer);
        initListener(this.complexSynchronizer);
        initListener(this.interactorPoolSynchronizer);
        initListener(this.causalRelationshipSynchronizer);
        initListener(this.confidenceSynchronizer);
        initListener(this.parameterSynchronizer);
        initListener(this.organismSynchronizer);
        initListener(this.rangeSynchronizer);
        initListener(this.modelledRangeSynchronizer);
        initListener(this.experimentalRangeSynchronizer);
        initListener(this.preferenceSynchronizer);
        initListener(this.roleSynchronizer);
        initListener(this.userSynchronizer);
        initListener(this.publicationSynchronizer);
        initListener(this.experimentSynchronizer);
        initListener(this.interactionEvidenceSynchronizer);
        initListener(this.cooperativityEvidenceSynchronizer);
        initListener(this.lifecycleSynchronizer);
        initListener(this.featureSynchronizer);
        initListener(this.featureEvidenceSynchronizer);
        initListener(this.modelledFeatureSynchronizer);
        initListener(this.variableParameterSynchronizer);
        initListener(this.variableParameterValueSynchronizer);
        initListener(this.variableParameterValueSetSynchronizer);
        initListener(this.participantSynchronizer);
        initListener(this.modelledParticipantSynchronizer);
        initListener(this.participantEvidenceSynchronizer);
        initListener(this.generalCvSynchronizer);
        initListener(this.dbInfoSynchronizer);
        initListener(this.applicationPropertySynchronizer);
        initListener(this.applicationSynchronizer);
    }

    public void clearCache() {
        clearCache(this.databaseSynchronizer);
        clearCache(this.qualifierSynchronizer);
        clearCache(this.topicSynchronizer);
        clearCache(this.aliasTypeSynchronizer);
        clearCache(this.unitSynchronizer);
        clearCache(this.featureTypeSynchronizer);
        clearCache(this.experimentalRoleSynchronizer);
        clearCache(this.biologicalRoleSynchronizer);
        clearCache(this.interactionDetectionMethodSynchronizer);
        clearCache(this.interactionTypeSynchronizer);
        clearCache(this.participantDetectionMethodSynchronizer);
        clearCache(this.experimentalPreparationSynchronizer);
        clearCache(this.interactorTypeSynchronizer);
        clearCache(this.rangeStatusSynchronizer);
        clearCache(this.confidenceTypeSynchronizer);
        clearCache(this.parameterTypeSynchronizer);
        clearCache(this.cellTypeSynchronizer);
        clearCache(this.tissueSynchronizer);
        clearCache(this.featureDetectionMethodSynchronizer);
        clearCache(this.lifecycleStatusSynchronizer);
        clearCache(this.lifecycleEventSynchronizer);
        clearCache(this.sourceSynchronizer);
        clearCache(this.aliasSynchronizer);
        clearCache(this.annotationSynchronizer);
        clearCache(this.xrefSynchronizer);
        clearCache(this.complexXrefSynchronizer);
        clearCache(this.cooperativeEffectSynchronizer);
        clearCache(this.preAssemblySynchronizer);
        clearCache(this.allosterySynchronizer);
        clearCache(this.interactorSynchronizer);
        clearCache(this.interactorBaseSynchronizer);
        clearCache(this.polymerSynchronizer);
        clearCache(this.complexSynchronizer);
        clearCache(this.interactorPoolSynchronizer);
        clearCache(this.causalRelationshipSynchronizer);
        clearCache(this.confidenceSynchronizer);
        clearCache(this.parameterSynchronizer);
        clearCache(this.organismSynchronizer);
        clearCache(this.rangeSynchronizer);
        clearCache(this.modelledRangeSynchronizer);
        clearCache(this.experimentalRangeSynchronizer);
        clearCache(this.preferenceSynchronizer);
        clearCache(this.roleSynchronizer);
        clearCache(this.userSynchronizer);
        clearCache(this.publicationSynchronizer);
        clearCache(this.experimentSynchronizer);
        clearCache(this.interactionEvidenceSynchronizer);
        clearCache(this.cooperativityEvidenceSynchronizer);
        clearCache(this.lifecycleSynchronizer);
        clearCache(this.featureSynchronizer);
        clearCache(this.featureEvidenceSynchronizer);
        clearCache(this.modelledFeatureSynchronizer);
        clearCache(this.variableParameterSynchronizer);
        clearCache(this.variableParameterValueSynchronizer);
        clearCache(this.variableParameterValueSetSynchronizer);
        clearCache(this.participantSynchronizer);
        clearCache(this.modelledParticipantSynchronizer);
        clearCache(this.participantEvidenceSynchronizer);
        clearCache(this.generalCvSynchronizer);
        clearCache(this.dbInfoSynchronizer);
        clearCache(this.applicationSynchronizer);
        clearCache(this.applicationPropertySynchronizer);
    }

    public IntactCvSynchronizer getGeneralCvSynchronizer() {
        if (this.generalCvSynchronizer == null){
            this.generalCvSynchronizer = new CvTermSynchronizer(this);
            this.generalCvSynchronizer.setListener(listener);
            this.generalCvSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<CvTerm, IntactCvTerm>(this.generalCvSynchronizer));
        }
        ((CvTermSynchronizer)this.generalCvSynchronizer).setObjClass(null);
        return this.generalCvSynchronizer;
    }

    public IntactCvSynchronizer getCvSynchronizer(String objclass) {
        // overrides intact merger to ignore local object when merging
        if (this.generalCvSynchronizer == null){
            this.generalCvSynchronizer = new CvTermSynchronizer(this);
            this.generalCvSynchronizer.setListener(listener);
            this.generalCvSynchronizer.setIntactMerger(new IntactDbMergerIgnoringLocalObject<CvTerm, IntactCvTerm>(this.generalCvSynchronizer));
        }
        ((CvTermSynchronizer)this.generalCvSynchronizer).setObjClass(objclass);
        return this.generalCvSynchronizer;
    }

    private void initialiseAliasTemplateIfNotDone() {
        if (this.aliasSynchronizer == null){
            this.aliasSynchronizer = new AliasSynchronizerTemplate(this, AbstractIntactAlias.class);
            this.aliasSynchronizer.setListener(listener);
        }
    }

    private void initialiseInteractorTemplateIfNotDone() {
        if (this.interactorBaseSynchronizer == null){
            this.interactorBaseSynchronizer = new InteractorSynchronizerTemplate(this, IntactInteractor.class);
            this.interactorBaseSynchronizer.setListener(listener);
        }
    }

    private void initialisePolymerTemplateIfNotDone() {
        if (this.polymerSynchronizer == null){
            this.polymerSynchronizer = new PolymerSynchronizerTemplate(this, IntactPolymer.class);
            this.polymerSynchronizer.setListener(listener);
        }
    }

    private void initialiseAnnotationTemplateIfNotDone() {
        if (this.annotationSynchronizer == null){
            this.annotationSynchronizer = new AnnotationSynchronizerTemplate(this, AbstractIntactAnnotation.class);
            this.annotationSynchronizer.setListener(listener);
        }
    }

    private void initialiseXrefTemplateIfNotDone() {
        if (this.xrefSynchronizer == null){
            this.xrefSynchronizer = new XrefSynchronizerTemplate(this, AbstractIntactXref.class);
            this.xrefSynchronizer.setListener(listener);
        }
    }

    private void initialiseConfidenceTemplateIfNotDone() {
        if (this.confidenceSynchronizer == null){
            this.confidenceSynchronizer = new ConfidenceSynchronizerTemplate(this, AbstractIntactConfidence.class);
            this.confidenceSynchronizer.setListener(listener);
        }
    }

    private void initialiseParameterTemplateIfNotDone() {
        if (this.parameterSynchronizer == null){
            this.parameterSynchronizer = new ParameterSynchronizerTemplate(this, AbstractIntactParameter.class);
            this.parameterSynchronizer.setListener(listener);
        }
    }

    private void initialiseLifecycleTemplateIfNotDone() {
        if (this.lifecycleSynchronizer == null){
            this.lifecycleSynchronizer = new LifeCycleSynchronizerTemplate(this, AbstractLifeCycleEvent.class);
            this.lifecycleSynchronizer.setListener(listener);
        }
    }

    private void initializeCausalRelationshipSynchronizerIfNotDone() {
        if (this.causalRelationshipSynchronizer == null){
            this.causalRelationshipSynchronizer = new CausalRelationchipSynchronizerTemplate(this, AbstractIntactCausalRelationship.class);
            this.causalRelationshipSynchronizer.setListener(listener);
        }
    }

    private void clearCache(IntactDbSynchronizer delegate){
        if (delegate != null){
            delegate.clearCache();
        }
    }

    private void initListener(IntactDbSynchronizer delegate){
        if (delegate != null){
            delegate.setListener(listener);
        }
    }
}
