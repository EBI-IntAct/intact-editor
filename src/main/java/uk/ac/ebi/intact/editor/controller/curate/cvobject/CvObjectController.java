package uk.ac.ebi.intact.editor.controller.curate.cvobject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.primefaces.model.DualListModel;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import psidev.psi.mi.jami.utils.XrefUtils;
import uk.ac.ebi.intact.editor.controller.curate.AnnotatedObjectController;
import uk.ac.ebi.intact.editor.controller.curate.cloner.CvTermCloner;
import uk.ac.ebi.intact.editor.controller.curate.cloner.EditorCloner;
import uk.ac.ebi.intact.editor.services.curate.cvobject.CvObjectService;
import uk.ac.ebi.intact.jami.model.IntactPrimaryObject;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.synchronizer.IntactDbSynchronizer;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ValueChangeEvent;
import java.util.*;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope( "conversation.access" )
@ConversationName( "general" )
public class CvObjectController extends AnnotatedObjectController {

    private String ac;
    private IntactCvTerm cvObject;
    private String cvClassName;
    private String newCvObjectType;
    private boolean isTopic;

    private DualListModel<IntactCvTerm> parents;
    private Map<String, String> classMap;

    private String definition;

    @PostConstruct
    public void initializeClassMap(){
        classMap = new HashMap<String, String>();
        classMap.put(IntactUtils.INTERACTION_DETECTION_METHOD_OBJCLASS, "MI:0001" );
        classMap.put( IntactUtils.INTERACTION_TYPE_OBJCLASS, "MI:0190" );
        classMap.put( IntactUtils.PARTICIPANT_DETECTION_METHOD_OBJCLASS, "MI:0002" );
        classMap.put( IntactUtils.FEATURE_METHOD_OBJCLASS, "MI:0003" );
        classMap.put( IntactUtils.FEATURE_TYPE_OBJCLASS, "MI:0116" );
        classMap.put( IntactUtils.INTERACTOR_TYPE_OBJCLASS, "MI:0313" );
        classMap.put( IntactUtils.PARTICIPANT_EXPERIMENTAL_PREPARATION_OBJCLASS, "MI:0346" );
        classMap.put( IntactUtils.RANGE_STATUS_OBJCLASS, "MI:0333" );
        classMap.put( IntactUtils.QUALIFIER_OBJCLASS, "MI:0353" );
        classMap.put( IntactUtils.DATABASE_OBJCLASS, "MI:0444" );
        classMap.put( IntactUtils.EXPERIMENTAL_ROLE_OBJCLASS, "MI:0495" );
        classMap.put( IntactUtils.BIOLOGICAL_ROLE_OBJCLASS, "MI:0500" );
        classMap.put( IntactUtils.ALIAS_TYPE_OBJCLASS, "MI:0300" );
        classMap.put( IntactUtils.TOPIC_OBJCLASS, "MI:0590" );
        classMap.put( IntactUtils.PARAMETER_TYPE_OBJCLASS, "MI:0640" );
        classMap.put( IntactUtils.UNIT_OBJCLASS, "MI:0647" );
        classMap.put( IntactUtils.CONFIDENCE_TYPE_OBJCLASS, "MI:1064" );
        classMap.put(IntactUtils.FEATURE_TYPE_OBJCLASS, "MOD:00000");
        classMap.put(IntactUtils.DATABASE_OBJCLASS, "ECO:0000000");
    }

    public void loadData(ComponentSystemEvent evt) {
        if (!FacesContext.getCurrentInstance().isPostback()) {
            if (ac != null) {
                if ( cvObject == null || !ac.equals( cvObject.getAc() ) ) {
                    setCvObject(getCvService().loadCvByAc(ac));
                }
            } else if (cvClassName != null) {
                setCvObject(newInstance(cvClassName));
                ac = null;
            }

            if (cvObject == null) {
                addErrorMessage("No CvObject with this AC", ac);
                return;
            }

            refreshTabsAndFocusXref();
        }
        generalLoadChecks();
    }

    @Override
    public IntactPrimaryObject getAnnotatedObject() {
        return getCvObject();
    }

    @Override
    public void setAnnotatedObject(IntactPrimaryObject annotatedObject) {
        setCvObject((IntactCvTerm) annotatedObject);
    }

    @Override
    protected AnnotatedObjectController getParentController() {
        return null;
    }

    @Override
    protected String getPageContext() {
        return "cv";
    }

    @Override
    protected void loadCautionMessages() {
        if (this.cvObject != null){

            Annotation caution = AnnotationUtils.collectFirstAnnotationWithTopic(this.cvObject.getAnnotations(), Annotation.CAUTION_MI, Annotation.CAUTION);
            setCautionMessage(caution != null ? caution.getValue() : null);
            Annotation internal = AnnotationUtils.collectFirstAnnotationWithTopic(this.cvObject.getAnnotations(), null, "remark-internal");
            setInternalRemark(internal != null ? internal.getValue() : null);

            this.definition = this.cvObject.getDefinition();
        }
    }

    @Override
    protected EditorCloner<CvTerm, IntactCvTerm> newClonerInstance() {
        return new CvTermCloner();
    }


    @Override
    protected void addNewXref(AbstractIntactXref newRef) {
        if (XrefUtils.isXrefAnIdentifier(newRef) || XrefUtils.doesXrefHaveQualifier(newRef, null, "intact-secondary")){
            this.cvObject.getIdentifiers().add(newRef);
        }
        else{
            this.cvObject.getXrefs().add(newRef);
        }
    }

    @Override
    protected CvTermXref newXref(CvTerm db, String id, String secondaryId, String version, CvTerm qualifier) {
        CvTermXref ref = new CvTermXref(db, id, version, qualifier);
        ref.setSecondaryId(secondaryId);
        return ref;
    }

    @Override
    public List<Xref> collectXrefs() {
        List<Xref> refs = new ArrayList<Xref>(cvObject.getDbXrefs());
        Collections.sort(refs, new AuditableComparator());
        // xrefs are always initialised
        return refs;
    }

    @Override
    public CvTermXref newXref(String db, String dbMI, String id, String secondaryId, String qualifier, String qualifierMI) {
        return new CvTermXref(getCvService().findCvObjectByIdentifier(IntactUtils.DATABASE_OBJCLASS,
                dbMI != null ? dbMI : db),
                id,
                secondaryId,
                getCvService().findCvObjectByIdentifier(IntactUtils.QUALIFIER_OBJCLASS,
                        qualifierMI != null ? qualifierMI : qualifier));
    }

    private void prepareView() {
        if (cvObject != null) {

            parents = getCvService().loadParentsList(this.cvObject);

            if (IntactUtils.TOPIC_OBJCLASS.equals(cvObject.getObjClass())) {
                isTopic = true;
            }
            else{
                isTopic = false;
            }
        }
    }

    public String newCvObject() {
        if (newCvObjectType != null) {
            setCvObject(newInstance(newCvObjectType));
        }

        return navigateToObject(cvObject);
    }

    private IntactCvTerm newInstance(String cvClassName) {
        IntactCvTerm obj = new IntactCvTerm("to set");
        obj.setObjClass(cvClassName);

        // if the cv does not have any parents, try to add one by default
        if (this.classMap.containsKey(cvClassName)){
            IntactCvTerm parent = getCvService().loadCvByIdentifier(this.classMap.get(cvClassName), cvClassName);
            if (parent != null){
                obj.getParents().add(parent);
            }
        }

        setUnsavedChanges(true);
        return obj;
    }

    public void onDefinitionChanged(ValueChangeEvent evt) {
        String newValue = (String) evt.getNewValue();
        if (newValue == null || newValue.length() == 0){
            setUnsavedChanges(true);
            this.cvObject.setDefinition(null);
            this.definition = null;
        }
        else{
            setUnsavedChanges(true);
            this.cvObject.setDefinition(newValue);
            this.definition = newValue;
        }
    }

    @Override
    public void doPreSave() {

        Collection<IntactCvTerm> parentsToRemove = CollectionUtils.subtract(cvObject.getParents(), parents.getTarget());
        Collection<IntactCvTerm> parentsToAdd = CollectionUtils.subtract(parents.getTarget(), cvObject.getParents());

        cvObject.getParents().removeAll(parentsToRemove);
        cvObject.getParents().addAll(parentsToAdd);

        // if the cv does not have any parents, try to add one by default
        if (this.classMap.containsKey(cvClassName) && cvObject.getParents().isEmpty()){
            IntactCvTerm parent = getCvService().loadCvByIdentifier(this.classMap.get(cvClassName), cvClassName);
            if (parent != null){
                this.cvObject.getParents().add(parent);
            }
        }

        super.doPreSave();
    }

    @Override
    public void postRevert(){
        prepareView();
    }

    @Override
    public void doPostSave() {
        // refresh cv service
        getCvService().refreshCvs(getObjClass());
        super.doPostSave();
    }

    public String[] getUsedIn() {
        if (cvObject != null){
            // cv annotations are always initialised by default
            Annotation usedInAnnot = AnnotationUtils.collectFirstAnnotationWithTopic(cvObject.getAnnotations(), null, CvObjectService.USED_IN_CLASS);
            String usedInArr = usedInAnnot != null ? usedInAnnot.getValue() : null;

            if (usedInArr != null) {
                String[] rawClasses = usedInArr.split(",");
                String[] classes = new String[rawClasses.length];

                for (int i=0; i<rawClasses.length; i++) {
                    classes[i] = rawClasses[i].trim();
                }

                return classes;
            }
        }
        return new String[0];
    }

    public void setUsedIn(String[] usedIn) {
        String usedInArr = StringUtils.join(usedIn, ",");
        super.updateAnnotation(CvObjectService.USED_IN_CLASS, null, usedInArr, this.cvObject.getAnnotations());
    }

    public String getAc() {
        return ac;
    }

    @Override
    public int getXrefsSize() {
        // cv xrefs are always initialised by default
        return this.cvObject != null ? this.cvObject.getDbXrefs().size() : 0;
    }

    @Override
    public int getAliasesSize() {
        // aliases may not be initialised
        if (this.cvObject == null){
            return 0;
        }
        else {
            return this.cvObject.getSynonyms().size();
        }
    }

    @Override
    public int getAnnotationsSize() {
        // cv annotations are always initialised by default
        return this.cvObject != null ? this.cvObject.getAnnotations().size() : 0;
    }

    public void setAc(String ac) {
        this.ac = ac;
    }

    public IntactCvTerm getCvObject() {
        return cvObject;
    }

    public void setCvObject(IntactCvTerm cvObject) {
        this.cvObject = cvObject;
        if (this.cvObject != null){
            this.ac = cvObject.getAc();

            initialiseDefaultProperties(cvObject);
        }
        else{
            this.ac = null;
        }
    }

    public String getCvClassName() {
        return cvClassName;
    }

    public void setCvClassName(String cvClassName) {
        this.cvClassName = cvClassName;
    }

    public boolean isTopic() {
        return isTopic;
    }

    public DualListModel<IntactCvTerm> getParents() {
        return parents;
    }

    public void setParents(DualListModel<IntactCvTerm> parents) {
        this.parents = parents;
    }

    public String getNewCvObjectType() {
        return newCvObjectType;
    }

    public void setNewCvObjectType(String newCvObjectType) {
        this.newCvObjectType = newCvObjectType;
    }

    @Override
    protected void initialiseDefaultProperties(IntactPrimaryObject annotatedObject) {
        IntactCvTerm cv = (IntactCvTerm)annotatedObject;
        if (!getCvService().isCvFullyLoaded(cv)){
            this.cvObject = getCvService().reloadFullyInitialisedCv(cv);
        }
        prepareView();

        setDescription("Cv Object: " + cv.getShortName());

        this.cvClassName = this.cvObject.getObjClass();
    }

    @Override
    public Collection<String> collectParentAcsOfCurrentAnnotatedObject() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Class<? extends IntactPrimaryObject> getAnnotatedObjectClass() {
        return IntactCvTerm.class;
    }

    @Override
    public boolean isAliasNotEditable(Alias alias) {
        return false;
    }

    @Override
    public boolean isAnnotationNotEditable(Annotation annot) {
        return false;
    }

    @Override
    public boolean isXrefNotEditable(Xref ref) {
        return false;
    }

    @Override
    public IntactDbSynchronizer getDbSynchronizer() {
        if (this.cvClassName == null){
            return getEditorService().getIntactDao().getSynchronizerContext().getCvSynchronizer(null);
        }
        else if (this.cvClassName.equals(IntactUtils.ALIAS_TYPE_OBJCLASS)){
            return getEditorService().getIntactDao().getSynchronizerContext().getAliasTypeSynchronizer();
        }
        else if (this.cvClassName.equals(IntactUtils.QUALIFIER_OBJCLASS)){
            return getEditorService().getIntactDao().getSynchronizerContext().getQualifierSynchronizer();
        }
        else if (this.cvClassName.equals(IntactUtils.DATABASE_OBJCLASS)){
            return getEditorService().getIntactDao().getSynchronizerContext().getDatabaseSynchronizer();
        }
        else if (this.cvClassName.equals(IntactUtils.TOPIC_OBJCLASS)){
            return getEditorService().getIntactDao().getSynchronizerContext().getTopicSynchronizer();
        }
        else if (this.cvClassName.equals(IntactUtils.BIOLOGICAL_ROLE_OBJCLASS)){
            return getEditorService().getIntactDao().getSynchronizerContext().getBiologicalRoleSynchronizer();
        }
        else if (this.cvClassName.equals(IntactUtils.CELL_TYPE_OBJCLASS)){
            return getEditorService().getIntactDao().getSynchronizerContext().getCellTypeSynchronizer();
        }
        else if (this.cvClassName.equals(IntactUtils.CONFIDENCE_TYPE_OBJCLASS)){
            return getEditorService().getIntactDao().getSynchronizerContext().getConfidenceTypeSynchronizer();
        }
        else if (this.cvClassName.equals(IntactUtils.EXPERIMENTAL_ROLE_OBJCLASS)){
            return getEditorService().getIntactDao().getSynchronizerContext().getExperimentalRoleSynchronizer();
        }
        else if (this.cvClassName.equals(IntactUtils.FEATURE_METHOD_OBJCLASS)){
            return getEditorService().getIntactDao().getSynchronizerContext().getFeatureDetectionMethodSynchronizer();
        }
        else if (this.cvClassName.equals(IntactUtils.FEATURE_TYPE_OBJCLASS)){
            return getEditorService().getIntactDao().getSynchronizerContext().getFeatureTypeSynchronizer();
        }
        else if (this.cvClassName.equals(IntactUtils.INTERACTION_DETECTION_METHOD_OBJCLASS)){
            return getEditorService().getIntactDao().getSynchronizerContext().getInteractionDetectionMethodSynchronizer();
        }
        else if (this.cvClassName.equals(IntactUtils.INTERACTION_TYPE_OBJCLASS)){
            return getEditorService().getIntactDao().getSynchronizerContext().getInteractionTypeSynchronizer();
        }
        else if (this.cvClassName.equals(IntactUtils.INTERACTOR_TYPE_OBJCLASS)){
            return getEditorService().getIntactDao().getSynchronizerContext().getInteractorTypeSynchronizer();
        }
        else if (this.cvClassName.equals(IntactUtils.LIFECYCLE_EVENT_OBJCLASS)){
            return getEditorService().getIntactDao().getSynchronizerContext().getLifecycleEventSynchronizer();
        }
        else if (this.cvClassName.equals(IntactUtils.PARAMETER_TYPE_OBJCLASS)){
            return getEditorService().getIntactDao().getSynchronizerContext().getParameterTypeSynchronizer();
        }
        else if (this.cvClassName.equals(IntactUtils.PARTICIPANT_DETECTION_METHOD_OBJCLASS)){
            return getEditorService().getIntactDao().getSynchronizerContext().getParticipantDetectionMethodSynchronizer();
        }
        else if (this.cvClassName.equals(IntactUtils.PARTICIPANT_EXPERIMENTAL_PREPARATION_OBJCLASS)){
            return getEditorService().getIntactDao().getSynchronizerContext().getExperimentalPreparationSynchronizer();
        }
        else if (this.cvClassName.equals(IntactUtils.PUBLICATION_STATUS_OBJCLASS)){
            return getEditorService().getIntactDao().getSynchronizerContext().getLifecycleStatusSynchronizer();
        }
        else if (this.cvClassName.equals(IntactUtils.RANGE_STATUS_OBJCLASS)){
            return getEditorService().getIntactDao().getSynchronizerContext().getRangeStatusSynchronizer();
        }
        else if (this.cvClassName.equals(IntactUtils.UNIT_OBJCLASS)){
            return getEditorService().getIntactDao().getSynchronizerContext().getRangeStatusSynchronizer();
        }
        else if (this.cvClassName.equals(IntactUtils.TISSUE_OBJCLASS)){
            return getEditorService().getIntactDao().getSynchronizerContext().getRangeStatusSynchronizer();
        }
        else{
            return getEditorService().getIntactDao().getSynchronizerContext().getCvSynchronizer(null);
        }
    }

    @Override
    public String getObjectName() {
        return this.cvObject != null ? this.cvObject.getShortName() : null;
    }

    @Override
    protected void addNewAlias(AbstractIntactAlias newAlias) {
        this.cvObject.getSynonyms().add(newAlias);
    }

    @Override
    public CvTermAlias newAlias(CvTerm aliasType, String name) {
        return new CvTermAlias(aliasType, name);
    }

    @Override
    public void removeAlias(Alias alias) {

        this.cvObject.getSynonyms().remove(alias);
    }

    @Override
    public CvTermAlias newAlias(String alias, String aliasMI, String name) {
        return new CvTermAlias(getCvService().findCvObject(IntactUtils.ALIAS_TYPE_OBJCLASS,
                aliasMI != null ? aliasMI : alias),
                name);
    }

    @Override
    public List<Alias> collectAliases() {
        // aliases are not always initialised

        List<Alias> aliases = new ArrayList<Alias>(cvObject.getSynonyms());
        Collections.sort(aliases, new AuditableComparator());
        return aliases;
    }

    @Override
    public void removeXref(Xref xref) {
        if (!this.cvObject.getIdentifiers().remove(xref)){
            this.cvObject.getXrefs().remove(xref);
        }
    }

    @Override
    protected void addNewAnnotation(AbstractIntactAnnotation newAnnot) {
        this.cvObject.getAnnotations().add(newAnnot);
    }

    @Override
    public CvTermAnnotation newAnnotation(CvTerm annotation, String text) {
        return new CvTermAnnotation(annotation, text);
    }

    @Override
    public CvTermAnnotation newAnnotation(String topic, String topicMI, String text) {
        return new CvTermAnnotation(getCvService().findCvObject(IntactUtils.TOPIC_OBJCLASS,
                topicMI != null ? topicMI : topic),
                text);
    }

    @Override
    public void removeAnnotation(Annotation annotation) {
        this.cvObject.getAnnotations().remove(annotation);
    }

    @Override
    public List<Annotation> collectAnnotations() {
        List<Annotation> annotations = new ArrayList<Annotation>(cvObject.getAnnotations());
        Collections.sort(annotations, new AuditableComparator());
        // annotations are always initialised
        return annotations;
    }

    public String getDefinition() {
        return this.definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    @Override
    public String doDelete() {
        String objClass = getObjClass();
        String value = super.doDelete();
        getCvService().refreshCvs(objClass);
        return value;
    }

    public String getObjClass(){
        if (this.cvObject == null){
            return null;
        }
        else if (this.cvObject.getObjClass() == null){
            return "Cv Object";
        }
        else{
            return "Cv Object: "+this.cvObject.getObjClass().substring(this.cvObject.getObjClass().lastIndexOf(".")+1);
        }
    }

    public String getShortName(){
        if (this.cvObject == null || this.cvObject.getShortName().equals("to set")){
            return null;
        }
        return this.cvObject.getShortName();
    }

    public void setShortName(String name){
        this.cvObject.setShortName(name);
    }
}
