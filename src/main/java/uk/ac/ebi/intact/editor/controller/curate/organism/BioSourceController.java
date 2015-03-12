package uk.ac.ebi.intact.editor.controller.curate.organism;

import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.jami.bridges.fetcher.OrganismFetcher;
import psidev.psi.mi.jami.model.*;
import uk.ac.ebi.intact.editor.controller.curate.AnnotatedObjectController;
import uk.ac.ebi.intact.editor.controller.curate.cloner.EditorCloner;
import uk.ac.ebi.intact.editor.services.curate.cvobject.CvObjectService;
import uk.ac.ebi.intact.editor.services.curate.organism.BioSourceService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.IntactPrimaryObject;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.synchronizer.IntactDbSynchronizer;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ComponentSystemEvent;
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
public class BioSourceController extends AnnotatedObjectController {

    private String ac;
    private IntactOrganism bioSource;

    @Resource(name = "bioSourceService")
    private transient BioSourceService bioSourceService;

    @Resource(name = "taxonomyService")
    private transient OrganismFetcher organismFetcher;

    @Override
    public IntactPrimaryObject getAnnotatedObject() {
        return getBioSource();
    }

    @Override
    public void setAnnotatedObject(IntactPrimaryObject annotatedObject) {
        setBioSource((IntactOrganism)annotatedObject);
    }

    @Override
    protected AnnotatedObjectController getParentController() {
        return null;
    }

    @Override
    protected String getPageContext() {
        return "organism";
    }

    @Override
    protected void loadCautionMessages() {
        // nothing to do
    }

    @Override
    protected void generalLoadChecks() {
        super.generalLoadChecks();
    }

    public void loadData(ComponentSystemEvent evt) {
        if (!FacesContext.getCurrentInstance().isPostback()) {

            if (ac != null) {
                if ( bioSource == null || !ac.equals(bioSource.getAc())) {
                    setBioSource(getBioSourceService().loadOrganismByAc(ac));
                }

            } else {
                bioSource = new IntactOrganism(-3);
            }

            if (bioSource == null) {
                super.addErrorMessage("Organism does not exist", ac);
                return;
            }

            refreshTabs();
        }

        generalLoadChecks();
    }

    @Override
    protected EditorCloner<Organism, IntactOrganism> newClonerInstance() {
        return new uk.ac.ebi.intact.editor.controller.curate.cloner.OrganismCloner();
    }

    @Override
    public void newXref(ActionEvent evt) {
        // nothing to do
    }

    @Override
    protected void addNewXref(AbstractIntactXref newRef) {

    }

    @Override
    protected <T extends AbstractIntactXref> T newXref(CvTerm db, String id, String secondaryId, String version, CvTerm qualifier) {
        return null;
    }

    @Override
    public <T extends AbstractIntactXref> T newXref(String db, String dbMI, String id, String secondaryId, String qualifier, String qualifierMI) {
        return null;
    }

    public String newOrganism() {
        IntactOrganism bioSource = new IntactOrganism(-3);

        changed();
        setBioSource(bioSource);
        return navigateToObject(bioSource);
    }

    public void autoFill(ActionEvent evt) {
        final int taxIdStr = bioSource.getTaxId();

        try {
            final int taxId = Integer.valueOf(taxIdStr);

            final Organism term = getOrganismFetcher().fetchByTaxID(taxId);

            if (term == null){
                addErrorMessage("Problem auto-filling from Uniprot Taxonomy for "+taxId, "Cannot find organism with taxid "+taxId);
            }

            String name;

            if (term != null && term.getCommonName() != null) {
                name = term.getCommonName().toLowerCase();
            } else {
                name = Integer.toString(taxId);
            }

            if (term != null){
                this.bioSource.setCommonName(name);
                this.bioSource.setScientificName(name);
                if (!term.getAliases().isEmpty()){
                    CvObjectService cvService = ApplicationContextProvider.getBean("cvObjectService");
                    for (Alias alias :term.getAliases()){
                        OrganismAlias organismAlias = new OrganismAlias(alias.getType(), alias.getName());
                        if (organismAlias.getType() != null){
                            if (organismAlias.getType().getMIIdentifier() != null){
                                organismAlias.setType(cvService.findCvObjectByIdentifier(IntactUtils.ALIAS_TYPE_OBJCLASS, alias.getType().getMIIdentifier()));
                            }
                            else{
                                organismAlias.setType(cvService.findCvObjectByIdentifier(IntactUtils.ALIAS_TYPE_OBJCLASS, alias.getType().getShortName()));
                            }
                        }
                        this.bioSource.getAliases().add(organismAlias);
                    }
                }
            }

            if (this.bioSource.getCellType() != null){
                this.bioSource.setCommonName(this.bioSource.getCommonName()+"-"+this.bioSource.getCellType().getShortName());
            }
            if (this.bioSource.getTissue() != null){
                this.bioSource.setCommonName(this.bioSource.getCommonName()+"-"+this.bioSource.getTissue().getShortName());
            }

            changed();
        } catch (Throwable e) {
            addErrorMessage("Problem auto-filling from Uniprot Taxonomy", e.getMessage());
            handleException(e);
        }
    }

    @Override
    public String doDelete() {
        String value = super.doDelete();
        getBioSourceService().loadData();
        return value;
    }

    @Override
    public void doPostSave() {
        getBioSourceService().loadData();
    }

    public String getAc() {
        return ac;
    }

    @Override
    public int getXrefsSize() {
        return 0;
    }

    @Override
    public int getAliasesSize() {
        if (bioSource == null){
            return 0;
        }
        else{
            return bioSource.getAliases().size();
        }
    }

    @Override
    public int getAnnotationsSize() {
        return 0;
    }

    public void setAc(String ac) {
        this.ac = ac;
    }

    public IntactOrganism getBioSource() {
        return bioSource;
    }

    public void setBioSource(IntactOrganism bioSource) {
        this.bioSource = bioSource;
        if (bioSource != null){
            this.ac = bioSource.getAc();

            initialiseDefaultProperties(this.bioSource);
        }
        else{
            this.ac = null;
        }
    }

    public void setTaxId(String taxId) {
        this.bioSource.setTaxId(Integer.parseInt(taxId));
    }

    public String getTaxId() {
        if (bioSource == null || (bioSource.getAc() == null && bioSource.getTaxId() == -3)){
            return null;
        }
        return Integer.toString(bioSource.getTaxId());
    }

    @Override
    public boolean isAliasDisabled() {
        return false;
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
        return getEditorService().getIntactDao().getSynchronizerContext().getOrganismSynchronizer();
    }

    @Override
    public String getObjectName() {
        return this.bioSource != null ? this.bioSource.getCommonName() : null;
    }

    @Override
    protected void initialiseDefaultProperties(IntactPrimaryObject annotatedObject) {
        IntactOrganism organism = (IntactOrganism)annotatedObject;
        if (!getBioSourceService().isOrganismFullyLoaded(organism)) {
            this.bioSource = getBioSourceService().reloadFullyInitialisedOrganism(organism);
        }
        setDescription("BioSource "+ organism.getCommonName());
    }

    @Override
    public Collection<String> collectParentAcsOfCurrentAnnotatedObject() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Class<? extends IntactPrimaryObject> getAnnotatedObjectClass() {
        return IntactOrganism.class;
    }

    public List<Annotation> collectAnnotations() {
        return Collections.EMPTY_LIST;
    }

    @Override
    protected void addNewAlias(AbstractIntactAlias newAlias) {
        bioSource.getAliases().add(newAlias);
    }

    @Override
    public OrganismAlias newAlias(CvTerm aliasType, String name) {
        return new OrganismAlias(aliasType, name);
    }

    @Override
    public OrganismAlias newAlias(String alias, String aliasMI, String name) {
        return new OrganismAlias(getCvService().findCvObject(IntactUtils.ALIAS_TYPE_OBJCLASS, aliasMI != null ? aliasMI : alias),
                name);
    }

    @Override
    public void removeAlias(Alias alias) {
        bioSource.getAliases().remove(alias);
    }

    public List<Alias> collectAliases() {
        List<Alias> aliases = new ArrayList<Alias>(this.bioSource.getAliases());
        Collections.sort(aliases, new AuditableComparator());
        return aliases;
    }

    public List<Xref> collectXrefs() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public void removeXref(Xref xref) {
        // nothing to do
    }

    @Override
    public void newAnnotation(ActionEvent evt) {
        // nothing to do
    }

    @Override
    protected void addNewAnnotation(AbstractIntactAnnotation newAnnot) {
        // nothing to do
    }

    @Override
    public <T extends AbstractIntactAnnotation> T newAnnotation(CvTerm annotation, String text) {
        return null;
    }

    @Override
    public <T extends AbstractIntactAnnotation> T newAnnotation(String topic, String topicMI, String text) {
        return null;
    }

    @Override
    public void removeAnnotation(Annotation annotation) {
        // nothing to do
    }

    public BioSourceService getBioSourceService() {
        if (this.bioSourceService == null){
            this.bioSourceService = ApplicationContextProvider.getBean("bioSourceService");
        }
        return bioSourceService;
    }

    public OrganismFetcher getOrganismFetcher() {
        if (this.organismFetcher == null){
            this.organismFetcher = ApplicationContextProvider.getBean("taxonomyService");
        }
        return organismFetcher;
    }
}
