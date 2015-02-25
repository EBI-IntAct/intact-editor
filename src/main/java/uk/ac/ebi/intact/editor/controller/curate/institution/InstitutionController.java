package uk.ac.ebi.intact.editor.controller.curate.institution;

import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import psidev.psi.mi.jami.utils.XrefUtils;
import uk.ac.ebi.intact.editor.controller.curate.AnnotatedObjectController;
import uk.ac.ebi.intact.editor.controller.curate.cloner.EditorCloner;
import uk.ac.ebi.intact.editor.controller.curate.cloner.InstitutionCloner;
import uk.ac.ebi.intact.editor.services.curate.institution.InstitutionService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.IntactPrimaryObject;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.synchronizer.IntactDbSynchronizer;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ValueChangeEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope( "conversation.access" )
@ConversationName( "general" )
public class InstitutionController extends AnnotatedObjectController {

    private String ac;
    private IntactSource institution;

    private String url;
    private String postalAddress;

    @Resource(name = "institutionService")
    private transient InstitutionService institutionService;

    @Override
    public IntactPrimaryObject getAnnotatedObject() {
        return getInstitution();
    }

    @Override
    public void setAnnotatedObject(IntactPrimaryObject annotatedObject) {
        setInstitution((IntactSource)annotatedObject);
    }

    @Override
    protected AnnotatedObjectController getParentController() {
        return null;
    }

    @Override
    protected String getPageContext() {
        return "institution";
    }

    @Override
    protected void loadCautionMessages() {
        if (this.institution != null){

            Annotation caution = AnnotationUtils.collectFirstAnnotationWithTopic(this.institution.getAnnotations(), Annotation.CAUTION_MI, Annotation.CAUTION);
            setCautionMessage(caution != null ? caution.getValue() : null);
            Annotation internal = AnnotationUtils.collectFirstAnnotationWithTopic(this.institution.getAnnotations(), null, "remark-internal");
            setInternalRemark(internal != null ? internal.getValue() : null);

            this.url = this.institution.getUrl();
            this.postalAddress = this.institution.getPostalAddress();
        }
    }

    @Override
    protected void initialiseDefaultProperties(IntactPrimaryObject annotatedObject) {
        IntactSource institution = (IntactSource)annotatedObject;
        if (!getInstitutionService().isSourceFullyLoaded(institution)) {
            this.institution = getInstitutionService().reloadFullyInitialisedSource(institution);
        }

        setDescription("Institution "+institution.getShortName());
    }

    public void loadData(ComponentSystemEvent evt) {
        if (!FacesContext.getCurrentInstance().isPostback()) {

            if (ac != null) {
                if ( institution == null || !ac.equals( institution.getAc() ) ) {
                    setInstitution(getInstitutionService().loadSourceByAc(ac));
                }
            } else if (institution == null){
                setInstitution(new IntactSource("to set"));
            }

            if (institution == null) {
                super.addErrorMessage("Institution does not exist", ac);
                return;
            }

            refreshTabsAndFocusXref();
        }

        generalLoadChecks();
    }

    @Override
    public void doPostSave() {
        getInstitutionService().loadInstitutions();
    }

    @Override
    protected EditorCloner<Source,IntactSource> newClonerInstance() {
        return new InstitutionCloner();
    }

    @Override
    protected void addNewXref(AbstractIntactXref newRef) {
        if (XrefUtils.isXrefAnIdentifier(newRef) || XrefUtils.doesXrefHaveQualifier(newRef, null, "intact-secondary")){
            this.institution.getIdentifiers().add(newRef);
        }
        else{
            this.institution.getXrefs().add(newRef);
        }
    }

    @Override
    protected SourceXref newXref(CvTerm db, String id, String secondaryId, String version, CvTerm qualifier) {
        SourceXref ref = new SourceXref(db, id, version, qualifier);
        ref.setSecondaryId(secondaryId);
        return ref;
    }

    @Override
    public SourceXref newXref(String db, String dbMI, String id, String secondaryId, String qualifier, String qualifierMI) {
        return new SourceXref(getCvService().findCvObjectByIdentifier(IntactUtils.DATABASE_OBJCLASS,
                dbMI != null ? dbMI : db),
                id,
                secondaryId,
                getCvService().findCvObjectByIdentifier(IntactUtils.QUALIFIER_OBJCLASS,
                        qualifierMI != null ? qualifierMI : qualifier));
    }

    public String newInstitution() {
        IntactSource institution = new IntactSource("to set");
        setInstitution(institution);
        changed();
        return navigateToObject(institution);
    }

    public String getAc() {
        return ac;
    }

    @Override
    public int getXrefsSize() {
        if (institution == null){
           return 0;
        }
        else{
            return institution.getDbXrefs().size();
        }
    }

    @Override
    public int getAliasesSize() {
        if (institution == null){
            return 0;
        }
        else {
            return institution.getSynonyms().size();
        }
    }

    @Override
    public int getAnnotationsSize() {
        if (institution == null){
            return 0;
        }
        else{
            return institution.getAnnotations().size();
        }
    }

    public void setAc(String ac) {
        this.ac = ac;
    }

    public IntactSource getInstitution() {
        return institution;
    }

    public void setInstitution(IntactSource institution) {
        this.institution = institution;
        if (this.institution != null){
            this.ac = this.institution.getAc();

            initialiseDefaultProperties(this.institution);
        }
        else{
            this.ac = null;
        }
    }

    public String getPostalAddress() {
        return this.postalAddress;
    }

    public void setPostalAddress(String address) {
        this.postalAddress = address;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String address) {
        this.url = address;
    }

    public void onUrlChanged(ValueChangeEvent evt) {
        setUnsavedChanges(true);
        String newValue = (String) evt.getNewValue();
        if (newValue == null || newValue.length() == 0){
            this.institution.setUrl(null);
            this.url = null;
        }
        else{
            this.institution.setUrl(newValue);
            this.url = newValue;
        }
    }

    public void onPostalAddressChanged(ValueChangeEvent evt) {
        setUnsavedChanges(true);
        String newValue = (String) evt.getNewValue();
        if (newValue == null || newValue.length() == 0){
            this.institution.setPostalAddress(null);
            this.postalAddress = null;
        }
        else{
            this.institution.setPostalAddress(newValue);
            this.postalAddress = newValue;
        }
    }

    @Override
    public Collection<String> collectParentAcsOfCurrentAnnotatedObject() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Class<? extends IntactPrimaryObject> getAnnotatedObjectClass() {
        return IntactSource.class;
    }

    @Override
    public boolean isAliasNotEditable(Alias alias) {
        return false;
    }

    @Override
    public boolean isAnnotationNotEditable(Annotation annot) {
        if (AnnotationUtils.doesAnnotationHaveTopic(annot, null, Annotation.POSTAL_ADDRESS)){
            return true;
        }
        else if (AnnotationUtils.doesAnnotationHaveTopic(annot, Annotation.URL_MI, Annotation.URL)){
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public boolean isXrefNotEditable(Xref ref) {
        return false;
    }

    @Override
    public IntactDbSynchronizer getDbSynchronizer() {
        return getEditorService().getIntactDao().getSynchronizerContext().getSourceSynchronizer();
    }

    @Override
    public String getObjectName() {
        return this.institution != null ? this.institution.getShortName() : null;
    }

    public List<Annotation> collectAnnotations() {
        List<Annotation> annotations = new ArrayList<Annotation>(institution.getAnnotations());
        Collections.sort(annotations, new AuditableComparator());
        // annotations are always initialised
        return annotations;
    }

    @Override
    protected void addNewAlias(AbstractIntactAlias newAlias) {
        this.institution.getSynonyms().add(newAlias);
    }

    @Override
    public SourceAlias newAlias(CvTerm aliasType, String name) {
        return new SourceAlias(aliasType, name);
    }

    @Override
    public SourceAlias newAlias(String alias, String aliasMI, String name) {
        return new SourceAlias(getCvService().findCvObject(IntactUtils.ALIAS_TYPE_OBJCLASS, aliasMI != null ? aliasMI : alias),
                name);
    }

    @Override
    public void removeAlias(Alias alias) {

        this.institution.getSynonyms().remove(alias);
    }

    public List<Alias> collectAliases() {

        List<Alias> aliases = new ArrayList<Alias>(this.institution.getSynonyms());
        Collections.sort(aliases, new AuditableComparator());
        return aliases;
    }

    public List<Xref> collectXrefs() {
        List<Xref> xrefs = new ArrayList<Xref>(this.institution.getDbXrefs());
        Collections.sort(xrefs, new AuditableComparator());
        return xrefs;
    }

    @Override
    public void removeXref(Xref xref) {
        if (!this.institution.getIdentifiers().remove(xref)){
            this.institution.getXrefs().remove(xref);
        }
    }

    @Override
    protected void addNewAnnotation(AbstractIntactAnnotation newAnnot) {
        this.institution.getAnnotations().add(newAnnot);
    }

    @Override
    public SourceAnnotation newAnnotation(CvTerm annotation, String text) {
        return new SourceAnnotation(annotation, text);
    }

    @Override
    public SourceAnnotation newAnnotation(String topic, String topicMI, String text) {
        return new SourceAnnotation(getCvService().findCvObject(IntactUtils.TOPIC_OBJCLASS,
                topicMI != null ? topicMI : topic),
                text);
    }

    @Override
    public void removeAnnotation(Annotation annotation) {
         this.institution.getAnnotations().remove(annotation);
    }

    public InstitutionService getInstitutionService() {
        if (this.institutionService == null){
             this.institutionService = ApplicationContextProvider.getBean("institutionService");
        }
        return institutionService;
    }

    @Override
    public String doDelete() {
        String value = super.doDelete();
        getInstitutionService().loadInstitutions();
        return value;
    }

    public String getShortName(){
        if (this.institution == null || this.institution.getShortName().equals("to set")){
            return null;
        }
        return this.institution.getShortName();
    }

    public void setShortName(String name){
        this.institution.setShortName(name);
    }
}
