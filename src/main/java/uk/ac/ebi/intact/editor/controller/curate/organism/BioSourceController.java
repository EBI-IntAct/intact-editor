package uk.ac.ebi.intact.editor.controller.curate.organism;

import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.hibernate.Hibernate;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.bridges.taxonomy.TaxonomyTerm;
import uk.ac.ebi.intact.bridges.taxonomy.UniprotTaxonomyService;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.editor.controller.curate.AnnotatedObjectController;
import uk.ac.ebi.intact.editor.controller.curate.ChangesController;
import uk.ac.ebi.intact.editor.controller.curate.PersistenceController;
import uk.ac.ebi.intact.editor.controller.curate.cloner.BiosourceIntactCloner;
import uk.ac.ebi.intact.jami.model.IntactPrimaryObject;
import uk.ac.ebi.intact.model.*;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ComponentSystemEvent;
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
    private BioSource bioSource;

    @Override
    public AnnotatedObject getAnnotatedObject() {
        return bioSource;
    }

    @Override
    public IntactPrimaryObject getJamiObject() {
        return null;
    }

    @Override
    public void setJamiObject(IntactPrimaryObject annotatedObject) {
        // nothing to do
    }

    @Override
    public void setAnnotatedObject(AnnotatedObject annotatedObject) {
        this.bioSource = (BioSource) annotatedObject;

        if (bioSource != null){
            this.ac = annotatedObject.getAc();
        }
    }

    @Transactional(value = "transactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public void loadData(ComponentSystemEvent evt) {
        if (!FacesContext.getCurrentInstance().isPostback()) {

            if (ac != null) {
                bioSource = loadByAc(getDaoFactory().getBioSourceDao(), ac);
                // initialise aliases
                Hibernate.initialize(bioSource.getAliases());
            } else {
                bioSource = new BioSource();
            }

            if (bioSource == null) {
                super.addErrorMessage("Organism does not exist", ac);
                return;
            }

            refreshTabsAndFocusXref();
        }

        generalLoadChecks();
    }

    @Override
    @Transactional(value = "transactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public String clone() {
        if (!getCoreEntityManager().contains(bioSource)){
            setBioSource(getCoreEntityManager().merge(this.bioSource));
        }
        String value = clone(bioSource, new BiosourceIntactCloner());

        getCoreEntityManager().detach(this.bioSource);

        return value;
    }

    public String newOrganism() {
        BioSource bioSource = new BioSource();
        setBioSource(bioSource);

        //getUnsavedChangeManager().markAsUnsaved(bioSource);
        changed();
        return navigateToObject(bioSource);
    }

    public void autoFill(ActionEvent evt) {
        final String taxIdStr = bioSource.getTaxId();

        if (taxIdStr == null || taxIdStr.isEmpty()) {
            return;
        }

        try {
            final int taxId = Integer.valueOf(taxIdStr);

            UniprotTaxonomyService uniprotTaxonomyService = new UniprotTaxonomyService();
            final TaxonomyTerm term = uniprotTaxonomyService.getTaxonomyTerm(taxId);

            String name;

            if (term.getCommonName() != null) {
                name = term.getCommonName();
            } else {
                name = term.getScientificName();
            }

            String commonName = name.toLowerCase();

            if (term.getMnemonic() != null){
                bioSource.setShortLabel(term.getMnemonic().toLowerCase());
            }
            else {
                bioSource.setShortLabel(commonName);
            }

            bioSource.setFullName(term.getScientificName());

            if (!term.getSynonyms().isEmpty()){
                CvAliasType synType = IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(CvAliasType.class).getByPsiMiRef(CvAliasType.SYNONYM_MI_REF);
                for (String syn : term.getSynonyms()){
                    bioSource.getAliases().add(new BioSourceAlias(IntactContext.getCurrentInstance().getInstitution(), bioSource, synType, syn));
                }
            }

            setTaxId(taxIdStr, commonName);
        } catch (Throwable e) {
            addErrorMessage("Problem auto-filling from Uniprot Taxonomy", e.getMessage());
            handleException(e);
        }
    }

    @Override
    public void doPostSave() {
        EditorOrganismService organismService = (EditorOrganismService) getSpringContext().getBean("editorOrganismService");
        organismService.clearAll();
        BioSourceService bioSourceService = (BioSourceService) getSpringContext().getBean("bioSourceService");
        bioSourceService.refresh(null);
    }

    public String getAc() {
        return ac;
    }

    public void setAc(String ac) {
        this.ac = ac;
    }

    public BioSource getBioSource() {
        return bioSource;
    }

    public void setBioSource(BioSource bioSource) {
        this.bioSource = bioSource;
        this.ac = bioSource.getAc();

        refreshTabsAndFocusXref();
    }

    public void setTaxId(String taxId) {
        setTaxId(taxId, null);
    }

    public void setTaxId(String taxId, String organismName) {
        bioSource.setTaxId(taxId);
    }

    public String getTaxId() {
        return bioSource.getTaxId();
    }

    @Override
    public boolean isAliasDisabled() {
        return false;
    }

    @Override
    @Transactional(value = "transactionManager", propagation = Propagation.REQUIRED)
    public void doSave(boolean refreshCurrentView) {
        ChangesController changesController = (ChangesController) getSpringContext().getBean("changesController");
        PersistenceController persistenceController = getPersistenceController();

        doSaveIntact(refreshCurrentView, changesController, persistenceController);
    }

    @Override
    @Transactional(value = "transactionManager", propagation = Propagation.REQUIRED)
    public String doSave() {
        return super.doSave();
    }

    @Override
    @Transactional(value = "transactionManager", propagation = Propagation.REQUIRED)
    public void doSaveIfNecessary(ActionEvent evt) {
        super.doSaveIfNecessary(evt);
    }

    @Override
    @Transactional(value = "transactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public List collectAliases() {
        if (!Hibernate.isInitialized(this.bioSource.getAliases())){
            BioSource reloadedBiosource = getCoreEntityManager().merge(this.bioSource);
            setBioSource(reloadedBiosource);
        }

        List aliases = super.collectAliases();

        getCoreEntityManager().detach(this.bioSource);
        return aliases;
    }

    @Override
    public String getCautionMessage() {
        return null;
    }

    @Override
    public String getCautionMessage(AnnotatedObject ao) {
        return null;
    }

    @Override
    public String getInternalRemarkMessage() {
        return null;
    }

}
