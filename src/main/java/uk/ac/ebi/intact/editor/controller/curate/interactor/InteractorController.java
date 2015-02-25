package uk.ac.ebi.intact.editor.controller.curate.interactor;

import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.primefaces.event.TabChangeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.jami.bridges.exception.BridgeFailedException;
import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.model.impl.DefaultChecksum;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import psidev.psi.mi.jami.utils.XrefUtils;
import uk.ac.ebi.intact.editor.controller.UserSessionController;
import uk.ac.ebi.intact.editor.controller.curate.AnnotatedObjectController;
import uk.ac.ebi.intact.editor.controller.curate.cloner.EditorCloner;
import uk.ac.ebi.intact.editor.controller.curate.cloner.InteractorCloner;
import uk.ac.ebi.intact.editor.controller.curate.interaction.ImportCandidate;
import uk.ac.ebi.intact.editor.services.curate.interaction.ParticipantImportService;
import uk.ac.ebi.intact.editor.services.curate.interactor.InteractorEditorService;
import uk.ac.ebi.intact.editor.services.curate.organism.BioSourceService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.IntactPrimaryObject;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.IntactDbSynchronizer;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.annotation.Resource;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.model.SelectItem;
import javax.faces.validator.ValidatorException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Interactor controller.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 2.0
 */
@Controller
@Scope( "conversation.access" )
@ConversationName( "general" )
public class InteractorController extends AnnotatedObjectController {

    private IntactInteractor interactor;

    private String ac;

    private String newInteractorType;

    @Autowired
    private UserSessionController userSessionController;

    @Resource(name = "interactorEditorService")
    private transient InteractorEditorService interactorEditorService;

    @Resource(name = "bioSourceService")
    private transient BioSourceService bioSourceService;

    @Resource(name = "participantImportService")
    private transient ParticipantImportService participantImportService;

    private boolean isNoUniprotUpdate = false;
    private boolean isInteractorMemberTab = false;

    private  List<SelectItem> typeSelectItems;
    private  String topicRootTerm;

    private String[] setMembers = null;
    private List<ImportCandidate> interactorCandidates;

    public InteractorController() {
    }

    @Override
    protected void generalLoadChecks() {
        super.generalLoadChecks();
    }

    @Override
    public IntactPrimaryObject getAnnotatedObject() {
        return getInteractor();
    }

    public boolean isInteractorPool(){
        return this.interactor instanceof InteractorPool;
    }

    public boolean isPolymer(){
        return this.interactor instanceof IntactPolymer;
    }

    @Override
    protected EditorCloner<Interactor, IntactInteractor> newClonerInstance() {
        return new InteractorCloner();
    }

    @Override
    protected void addNewXref(AbstractIntactXref newRef) {
        if (XrefUtils.isXrefAnIdentifier(newRef) || XrefUtils.doesXrefHaveQualifier(newRef, null, "intact-secondary")){
            this.interactor.getIdentifiers().add(newRef);
        }
        else{
            this.interactor.getXrefs().add(newRef);
        }
    }

    @Override
    protected InteractorXref newXref(CvTerm db, String id, String secondaryId, String version, CvTerm qualifier) {
        InteractorXref ref = new InteractorXref(db, id, version, qualifier);
        ref.setSecondaryId(secondaryId);
        return ref;
    }

    @Override
    public InteractorXref newXref(String db, String dbMI, String id, String secondaryId, String qualifier, String qualifierMI) {
        return new InteractorXref(getCvService().findCvObjectByIdentifier(IntactUtils.DATABASE_OBJCLASS,
                dbMI != null ? dbMI : db),
                id,
                secondaryId,
                getCvService().findCvObjectByIdentifier(IntactUtils.QUALIFIER_OBJCLASS,
                        qualifierMI != null ? qualifierMI : qualifier));
    }


    @Override
    public void setAnnotatedObject(IntactPrimaryObject annotatedObject) {
        setInteractor((IntactInteractor) annotatedObject);
    }

    @Override
    protected AnnotatedObjectController getParentController() {
        return null;
    }

    @Override
    protected String getPageContext() {
        return "interactor";
    }

    @Override
    protected void loadCautionMessages() {
        if (this.interactor != null){

            Annotation caution = AnnotationUtils.collectFirstAnnotationWithTopic(this.interactor.getDbAnnotations(), Annotation.CAUTION_MI, Annotation.CAUTION);
            setCautionMessage(caution != null ? caution.getValue() : null);
            Annotation internal = AnnotationUtils.collectFirstAnnotationWithTopic(this.interactor.getDbAnnotations(), null, "remark-internal");
            setInternalRemark(internal != null ? internal.getValue() : null);
            Annotation noUniprotUpdate = AnnotationUtils.collectFirstAnnotationWithTopic(this.interactor.getDbAnnotations(), null, "no-uniprot-update");
            this.isNoUniprotUpdate = noUniprotUpdate != null;

            this.newInteractorType = interactor.getInteractorType().getShortName();
        }
    }

    @Override
    public void refreshTabsAndFocusXref() {
        if (this.interactor instanceof InteractorPool){
            super.refreshTabs();
            this.isInteractorMemberTab = true;
        }
        else{
            super.refreshTabsAndFocusXref();
        }
    }

    public String getIdentityXref( IntactInteractor molecule ) {
        // TODO handle multiple identities (return xref and iterate to display them all)
        Xref xrefs = molecule.getPreferredIdentifier();


        if ( xrefs == null ) {
            return "-";
        }

        return xrefs.getId();
    }

    public void loadData( ComponentSystemEvent event ) {
        if (!FacesContext.getCurrentInstance().isPostback()) {

            if ( ac != null ) {
                if ( interactor == null || !ac.equals(interactor.getAc())) {
                    setInteractor(getInteractorEditorService().loadInteractorByAc(ac));
                }
            } else {
                if ( interactor != null ) ac = interactor.getAc();
            }

            if (interactor == null) {
                super.addErrorMessage("Interactor does not exist", ac);
                return;
            }

            refreshTabsAndFocusXref();
        }

        generalLoadChecks();
    }

    public void validateAnnotatedObject(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (!(interactor instanceof BioactiveEntity)
                && interactor.getOrganism() == null) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Organism is mandatory", "No biosource was defined");
            throw new ValidatorException(message);
        }

        super.validateAnnotatedObject(context, component, value);
    }

    public boolean isOrganismRequired(){
        return !(interactor instanceof BioactiveEntity);
    }

    @Override
    public void doPreSave() {
        super.doPreSave();
        cleanSequence(null);
    }

    public String newInteractor() {
        IntactInteractor interactor = newInstance(newInteractorType);
        interactor.setInteractorType(getCvService().findCvObject(IntactUtils.INTERACTOR_TYPE_OBJCLASS, interactor.getInteractorType().getMIIdentifier()));

        setInteractor(interactor);

        setUnsavedChanges(true);

        return "/curate/interactor";
    }

    public IntactInteractor newInstance(String interactorType) {
        if (interactorType.equals(Protein.PROTEIN)) {
            this.typeSelectItems = getCvService().getProteinTypeSelectItems();
            this.topicRootTerm = Protein.PROTEIN_MI;
            return new IntactProtein("to set");
        } else if (interactorType.equals(Protein.PEPTIDE)) {
            this.typeSelectItems = getCvService().getProteinTypeSelectItems();
            this.topicRootTerm = Protein.PEPTIDE_MI;
            return new IntactProtein("to set", IntactUtils.createMIInteractorType(Protein.PEPTIDE, Protein.PEPTIDE_MI));
        } else if (interactorType.equals(BioactiveEntity.BIOACTIVE_ENTITY)) {
            this.topicRootTerm = BioactiveEntity.BIOACTIVE_ENTITY_MI;
            this.typeSelectItems = getCvService().getBioactiveEntitySelectItems();
            return new IntactBioactiveEntity("to set");
        } else if (interactorType.equals(NucleicAcid.NULCEIC_ACID)) {
            this.typeSelectItems = getCvService().getNucleicAcidSelectItems();
            this.topicRootTerm = NucleicAcid.NULCEIC_ACID_MI;
            return new IntactNucleicAcid("to set");
        } else if (interactorType.equals(Complex.COMPLEX)) {
            this.typeSelectItems = getCvService().getComplexTypeSelectItems();
            this.topicRootTerm = Complex.COMPLEX_MI;
            IntactComplex complex = new IntactComplex("to set");
            // set source
            complex.setSource(getUserSessionController().getUserInstitution());
            // set evidence code
            complex.setEvidenceType(getCvService().loadCvByIdentifier("ECO:0000000", IntactUtils.DATABASE_OBJCLASS));
            return complex;
        } else if (interactorType.equals(Polymer.POLYMER)) {
            this.typeSelectItems = getCvService().getPolymerTypeSelectItems();
            this.topicRootTerm = Polymer.POLYMER_MI;
            return new IntactPolymer("to set");
        } else if (interactorType.equals(InteractorPool.MOLECULE_SET)) {
            this.typeSelectItems = getCvService().getMoleculeSetTypeSelectItems();
            this.topicRootTerm = InteractorPool.MOLECULE_SET_MI;
            return new IntactInteractorPool("to set");
        }
        else if (interactorType.equals(Gene.GENE)) {
            this.typeSelectItems = getCvService().getGeneTypeSelectItems();
            this.topicRootTerm = Gene.GENE_MI;
            return new IntactGene("to set");
        } else {
            this.typeSelectItems = getCvService().getInteractorTypeSelectItems();
            this.topicRootTerm = "MI:0313";
            return new IntactInteractor("to set");
        }
    }

    public void cleanSequence(AjaxBehaviorEvent evt) {
        String seq = getSequence();
        String originalSeq = seq;

        boolean changedSequence = false;

        if (seq != null) {
            // remove all non-alphabetical characters
            seq = seq.replaceAll("\\P{Alpha}", "");
            seq = seq.toUpperCase();

            changedSequence = !(seq.equals(originalSeq));

            if (changedSequence) {
                setSequence(seq);

                addWarningMessage("Sequence updated", "Illegal characters were found in the sequence and were removed automatically");
            }
        }
    }

    public IntactInteractor getInteractor() {
        return interactor;
    }

    public void setInteractor( IntactInteractor interactor ) {
        this.interactor = interactor;

        if (interactor != null){
            this.ac = interactor.getAc();

            initialiseDefaultProperties(this.interactor);
        }
        else{
            this.ac = null;
        }
    }

    public String getSequence() {
        if (interactor instanceof Polymer) {
            Polymer polymer = (Polymer) interactor;
            return polymer.getSequence();
        }

        return null;
    }

    public void setSequence(String sequence) {
        if (interactor instanceof Polymer) {
            Polymer polymer = (Polymer) interactor;
            polymer.setSequence(sequence);
        }
    }

    public String getNewInteractorType() {
        return newInteractorType;
    }

    public void setNewInteractorType(String newInteractorType) {
        this.newInteractorType = newInteractorType;
    }

    public String getAc() {
        return ac;
    }

    @Override
    public int getXrefsSize() {
        if (interactor == null){
            return 0;
        }
        else{
            return interactor.getDbXrefs().size();
        }
    }

    @Override
    public int getAliasesSize() {
        if (interactor == null){
            return 0;
        }
        else {
            return interactor.getAliases().size();
        }
    }

    @Override
    public int getAnnotationsSize() {
        if (interactor == null){
            return 0;
        }
        else{
            return interactor.getDbAnnotations().size();
        }
    }

    public int getMembersPoolSize() {
        if (!(interactor instanceof InteractorPool)){
            return 0;
        }
        else{
            return ((InteractorPool) interactor).size();
        }
    }

    public void setAc(String ac) {
        this.ac = ac;
    }

    public boolean isNoUniprotUpdate() {
        return isNoUniprotUpdate;
    }

    public boolean isNoUniprotUpdate(Interactor interactor) {
        Collection<Annotation> annots = Collections.EMPTY_LIST;
        if (interactor == null) return false;
        else{
            annots = interactor.getAnnotations();
        }

        if (annots.isEmpty()){
            return false;
        }
        Annotation caution = AnnotationUtils.collectFirstAnnotationWithTopic(annots, null, AnnotatedObjectController.NON_UNIPROT);
        return caution != null ? true : false;
    }

    @Override
    protected void initialiseDefaultProperties(IntactPrimaryObject annotatedObject) {
        IntactInteractor interactor = (IntactInteractor)annotatedObject;
        if (!getInteractorEditorService().isInteractorFullyLoaded(interactor)) {
            this.interactor = getInteractorEditorService().reloadFullyInitialisedInteractor(interactor);
        }

        if (interactor instanceof Protein) {
            this.typeSelectItems = getCvService().getProteinTypeSelectItems();
            this.topicRootTerm = Protein.PROTEIN_MI;
        } else if (interactor instanceof BioactiveEntity) {
            this.typeSelectItems = getCvService().getBioactiveEntitySelectItems();
            this.topicRootTerm = BioactiveEntity.BIOACTIVE_ENTITY_MI;
        } else if (interactor instanceof NucleicAcid) {
            this.typeSelectItems = getCvService().getNucleicAcidSelectItems();
            this.topicRootTerm = NucleicAcid.NULCEIC_ACID_MI;
        } else if (interactor instanceof Complex) {
            this.typeSelectItems = getCvService().getComplexTypeSelectItems();
            this.topicRootTerm = Complex.COMPLEX_MI;
        } else if (interactor instanceof Polymer) {
            this.typeSelectItems = getCvService().getPolymerTypeSelectItems();
            this.topicRootTerm = Polymer.POLYMER_MI;
        } else if (interactor instanceof IntactInteractorPool) {
            this.typeSelectItems = getCvService().getMoleculeSetTypeSelectItems();
            if (!((IntactInteractorPool)interactor).areInteractorsInitialized()){
                this.interactor = getInteractorEditorService().reloadFullyInitialisedInteractor(interactor);
            }
            this.topicRootTerm = InteractorPool.MOLECULE_SET_MI;
        }
        else if (interactor instanceof Gene) {
            this.typeSelectItems = getCvService().getGeneTypeSelectItems();
            this.topicRootTerm = Gene.GENE_MI;
        } else {
            this.typeSelectItems = getCvService().getInteractorTypeSelectItems();
            this.topicRootTerm = "MI:0313";
        }

        setDescription("Interactor "+ interactor.getShortName());
    }

    @Override
    public Collection<String> collectParentAcsOfCurrentAnnotatedObject() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Class<? extends IntactPrimaryObject> getAnnotatedObjectClass() {
        return this.interactor != null ? this.interactor.getClass() : IntactInteractor.class;
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
        return getEditorService().getIntactDao().getSynchronizerContext().getInteractorSynchronizer();
    }

    @Override
    public String getObjectName() {
        return interactor != null ? interactor.getShortName() : null;
    }

    public List<Annotation> collectAnnotations() {
        List<Annotation> annotations = new ArrayList<Annotation>(interactor.getDbAnnotations());
        Collections.sort(annotations, new AuditableComparator());
        // annotations are always initialised
        return annotations;
    }

    public List<Interactor> collectPoolMembers() {
        List<Interactor> members = new ArrayList<Interactor>((InteractorPool)this.interactor);
        Collections.sort(members, new AuditableComparator());
        // pool members are always initialised
        return members;
    }

    public void removePoolMember(Interactor interactor) {

        ((InteractorPool)this.interactor).remove(interactor);
    }

    public void importInteractor(ActionEvent evt) {
        interactorCandidates = new ArrayList<ImportCandidate>();
        for (String member : setMembers){
            try {
                interactorCandidates.addAll(getParticipantImportService().importParticipant(member));
            } catch (BridgeFailedException e) {
                addErrorMessage("Cannot load interactor "+member, e.getCause()+": "+e.getMessage());
            } catch (FinderException e) {
                addErrorMessage("Cannot load interactor " + member, e.getCause() + ": " + e.getMessage());
            } catch (SynchronizerException e) {
                addErrorMessage("Cannot load interactor " + member, e.getCause() + ": " + e.getMessage());
            } catch (PersisterException e) {
                addErrorMessage("Cannot load interactor " + member, e.getCause() + ": " + e.getMessage());
            } catch (Throwable e) {
                addErrorMessage("Cannot load interactor " + member, e.getCause() + ": " + e.getMessage());
            }
        }

        if (interactorCandidates.size() == 1) {
            interactorCandidates.get(0).setSelected(true);
        }
    }

    @Override
    protected void addNewAlias(AbstractIntactAlias newAlias) {
        this.interactor.getAliases().add(newAlias);
    }

    @Override
    public InteractorAlias newAlias(CvTerm aliasType, String name) {
        return new InteractorAlias(aliasType, name);
    }

    @Override
    public InteractorAlias newAlias(String alias, String aliasMI, String name) {
        return new InteractorAlias(getCvService().findCvObject(IntactUtils.ALIAS_TYPE_OBJCLASS, aliasMI != null ? aliasMI : alias),
                name);
    }

    @Override
    public void removeAlias(Alias alias) {

        interactor.getAliases().remove(alias);
    }

    public List<Alias> collectAliases() {

        List<Alias> aliases = new ArrayList<Alias>(this.interactor.getAliases());
        Collections.sort(aliases, new AuditableComparator());
        return aliases;
    }

    public List<Xref> collectXrefs() {
        List<Xref> xrefs = new ArrayList<Xref>(this.interactor.getDbXrefs());
        Collections.sort(xrefs, new AuditableComparator());
        return xrefs;
    }

    @Override
    public void removeXref(Xref xref) {
        if (!this.interactor.getIdentifiers().remove(xref)){
            this.interactor.getXrefs().remove(xref);
        }
    }

    @Override
    protected void addNewAnnotation(AbstractIntactAnnotation newAnnot) {
        // we have a checksum
        if (newAnnot.getValue() != null
                && AnnotationUtils.doesAnnotationHaveTopic(newAnnot, Checksum.SMILE_MI, Checksum.SMILE)){
            interactor.getChecksums().add(new DefaultChecksum(newAnnot.getTopic(), newAnnot.getValue()));
            if (!interactor.getDbAnnotations().contains(newAnnot)){
                interactor.getDbAnnotations().add(newAnnot);
            }
        }
        else if (newAnnot.getValue() != null
                && AnnotationUtils.doesAnnotationHaveTopic(newAnnot, Checksum.INCHI_MI, Checksum.INCHI)){
            interactor.getChecksums().add(new DefaultChecksum(newAnnot.getTopic(), newAnnot.getValue()));
            if (!interactor.getDbAnnotations().contains(newAnnot)){
                interactor.getDbAnnotations().add(newAnnot);
            }

        }
        else if (newAnnot.getValue() != null &&
                AnnotationUtils.doesAnnotationHaveTopic(newAnnot, Checksum.STANDARD_INCHI_KEY_MI, Checksum.STANDARD_INCHI_KEY)){
            interactor.getChecksums().add(new DefaultChecksum(newAnnot.getTopic(), newAnnot.getValue()));
            if (!interactor.getDbAnnotations().contains(newAnnot)){
                interactor.getDbAnnotations().add(newAnnot);
            }
        }
        // we have a simple annotation
        else{
            this.interactor.getAnnotations().add(newAnnot);
        }
    }

    @Override
    public InteractorAnnotation newAnnotation(CvTerm annotation, String text) {
        return new InteractorAnnotation(annotation, text);
    }

    @Override
    public InteractorAnnotation newAnnotation(String topic, String topicMI, String text) {
        return new InteractorAnnotation(getCvService().findCvObject(IntactUtils.TOPIC_OBJCLASS, topicMI != null ? topicMI: topic), text);
    }

    @Override
    public void removeAnnotation(Annotation annotation) {
        interactor.getDbAnnotations().remove(annotation);
    }

    public InteractorEditorService getInteractorEditorService() {
        if (this.interactorEditorService == null){
            this.interactorEditorService = ApplicationContextProvider.getBean("interactorEditorService");
        }
        return interactorEditorService;
    }

    public List<SelectItem> getTypeSelectItems() {
        return typeSelectItems;
    }

    public String getTopicRootTerm() {
        return topicRootTerm;
    }

    public String[] getSetMembers() {
        return setMembers;
    }

    public void setSetMembers(String[] setMember) {
        this.setMembers = setMember;
    }

    public List<ImportCandidate> getInteractorCandidates() {
        return interactorCandidates;
    }

    public void setInteractorCandidates(List<ImportCandidate> interactorCandidates) {
        this.interactorCandidates = interactorCandidates;
    }

    public void addInteractorToPool(ActionEvent evt) {
        for (ImportCandidate importCandidate : interactorCandidates) {
            if (importCandidate.isSelected()) {
                // chain or isoform, we may have to update it later
                if (importCandidate.isChain() || importCandidate.isIsoform()){
                    Collection<String> parentAcs = new ArrayList<String>();

                    getChangesController().markAsHiddenChange((IntactInteractor)importCandidate.getInteractor(), null, parentAcs,
                            getEditorService().getIntactDao().getSynchronizerContext().getInteractorSynchronizer(),
                            "Interactor "+importCandidate.getInteractor().getShortName());
                }
                ((InteractorPool)this.interactor).add((IntactInteractor) importCandidate.getInteractor());

                // if the pool is a new pool, we don't need to add a unsaved notice because one already exists for creating a new participant
                if (interactor.getAc() != null){
                    setUnsavedChanges(true);
                }
            }
        }
    }

    public void onTabChanged(TabChangeEvent e) {

        // the xref tab is active
        super.onTabChanged(e);

        // all the tabs selectOneMenu are disabled, we can process the tabs specific to interaction
        if (isAliasDisabled() && isXrefDisabled() && isAnnotationTopicDisabled()){
            if (e.getTab().getId().equals("membersTab")){
                isInteractorMemberTab = true;
            }
            else {
                isInteractorMemberTab = false;
            }
        }
        else {
            isInteractorMemberTab = false;
        }
    }

    public BioSourceService getBioSourceService() {
        if (this.bioSourceService == null){
            this.bioSourceService = ApplicationContextProvider.getBean("bioSourceService");
        }
        return bioSourceService;
    }

    public ParticipantImportService getParticipantImportService() {
        if (this.participantImportService == null){
            this.participantImportService = ApplicationContextProvider.getBean("participantImportService");
        }
        return participantImportService;
    }

    public String getShortName(){
        if (this.interactor == null || this.interactor.getShortName().equals("to set")){
            return null;
        }
        return this.interactor.getShortName();
    }

    public void setShortName(String name){
        this.interactor.setShortName(name);
    }
}
