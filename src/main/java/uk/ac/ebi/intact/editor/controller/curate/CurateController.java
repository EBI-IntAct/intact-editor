package uk.ac.ebi.intact.editor.controller.curate;

import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.jami.model.*;
import uk.ac.ebi.intact.editor.controller.BaseController;
import uk.ac.ebi.intact.editor.controller.UserSessionController;
import uk.ac.ebi.intact.editor.controller.curate.complex.ComplexController;
import uk.ac.ebi.intact.editor.controller.curate.cvobject.CvObjectController;
import uk.ac.ebi.intact.editor.controller.curate.experiment.ExperimentController;
import uk.ac.ebi.intact.editor.controller.curate.feature.FeatureController;
import uk.ac.ebi.intact.editor.controller.curate.feature.ModelledFeatureController;
import uk.ac.ebi.intact.editor.controller.curate.institution.InstitutionController;
import uk.ac.ebi.intact.editor.controller.curate.interaction.InteractionController;
import uk.ac.ebi.intact.editor.controller.curate.interactor.InteractorController;
import uk.ac.ebi.intact.editor.controller.curate.organism.BioSourceController;
import uk.ac.ebi.intact.editor.controller.curate.participant.ModelledParticipantController;
import uk.ac.ebi.intact.editor.controller.curate.participant.ParticipantController;
import uk.ac.ebi.intact.editor.controller.curate.publication.PublicationController;
import uk.ac.ebi.intact.editor.services.curate.EditorObjectService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.IntactPrimaryObject;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.model.user.Role;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Helper controller on conversation scope, that helps to load/save objects within the same transaction as the other AnnotatedObjectControllers.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope( "conversation.access" )
@ConversationName( "general" )
public class CurateController extends BaseController {

    @Resource(name = "editorObjectService")
    private transient EditorObjectService editorObjectService;

    private String acToOpen;

    private AnnotatedObjectController currentAnnotatedObjectController;

    public String edit(IntactPrimaryObject intactObject) {
        if (intactObject == null) {
            addErrorMessage("Cannot edit an object that is not set", "No AC provided");
            FacesContext.getCurrentInstance().renderResponse();
            return "";
        }
        String suffix = (intactObject.getAc() != null)? "?faces-redirect=true&includeViewParams=true" : "";

        CurateObjectMetadata metadata = getMetadata(intactObject);
        setCurrentAnnotatedObjectController(metadata.getAnnotatedObjectController());

        getCurrentAnnotatedObjectController().refreshTabsAndFocusXref();
        return "/curate/"+metadata.getSlug()+suffix;
    }

    public String editByAc(String ac) {
        if (ac == null) {
            addErrorMessage("Illegal AC", "No AC provided");
            FacesContext.getCurrentInstance().renderResponse();
            return "";
        }

        return edit(getEditorObjectService().loadByAc(ac));
    }

    public void save(IntactPrimaryObject o) {
        save(o, true);
    }

    public void save(IntactPrimaryObject object, boolean refreshCurrentView) {
        AnnotatedObjectController annotatedObjectController = getMetadata(object).getAnnotatedObjectController();
        annotatedObjectController.doSave(refreshCurrentView);
    }

    public void discard(IntactPrimaryObject object) {

        AnnotatedObjectController annotatedObjectController = getMetadata(object).getAnnotatedObjectController();
        annotatedObjectController.doRevertChanges(null);
    }

    public String cancelEdition(IntactPrimaryObject object) {

        AnnotatedObjectController annotatedObjectController = getMetadata(object).getAnnotatedObjectController();
        return annotatedObjectController.doCancelEdition();
    }

    public String newIntactObject(IntactPrimaryObject object) {
        AnnotatedObjectController annotatedObjectController;
        CurateObjectMetadata meta = getMetadata(object);
        annotatedObjectController = meta.getAnnotatedObjectController();
        setCurrentAnnotatedObjectController(annotatedObjectController);
        getCurrentAnnotatedObjectController().refreshTabsAndFocusXref();

        return "/curate/"+meta.getSlug();
    }

    public CurateObjectMetadata getMetadata(IntactPrimaryObject intactObject) {
        Class<?> iaClass = intactObject.getClass();

        if (IntactPublication.class.isAssignableFrom(iaClass)) {
            PublicationController publicationController = (PublicationController) getSpringContext().getBean("publicationController");
            publicationController.setPublication((IntactPublication)intactObject);
            return new CurateObjectMetadata(publicationController, "publication");
        } else if (IntactExperiment.class.isAssignableFrom(iaClass)) {
            ExperimentController experimentController = (ExperimentController) getSpringContext().getBean("experimentController");
            experimentController.setExperiment((IntactExperiment)intactObject);
            CurateObjectMetadata meta = new CurateObjectMetadata(experimentController, "experiment");

            Experiment exp = (Experiment)intactObject;
            if (exp.getPublication() instanceof IntactPublication){
                IntactPublication parent = (IntactPublication)exp.getPublication();
                if (parent.getAc() != null){
                    meta.getParents().add(parent.getAc());
                }
            }
            return meta;
        } else if (IntactInteractionEvidence.class.isAssignableFrom(iaClass)) {
            InteractionController interactionController = (InteractionController) getSpringContext().getBean("interactionController");
            interactionController.setInteraction((IntactInteractionEvidence)intactObject);
            CurateObjectMetadata meta = new CurateObjectMetadata(interactionController, "interaction");

            InteractionEvidence inter = (InteractionEvidence)intactObject;
            if (inter.getExperiment() instanceof IntactExperiment){
                IntactExperiment parent = (IntactExperiment)inter.getExperiment();
                if (parent.getAc() != null){
                    meta.getParents().add(parent.getAc());
                }
                if (parent.getPublication() instanceof IntactPublication){
                    IntactPublication parent2 = (IntactPublication)parent.getPublication();
                    if (parent2.getAc() != null){
                        meta.getParents().add(parent2.getAc());
                    }
                }
            }
            return meta;
        }
        else if (IntactComplex.class.isAssignableFrom(iaClass)) {
            ComplexController interactionController = (ComplexController) getSpringContext().getBean("complexController");
            interactionController.setComplex((IntactComplex)intactObject);
            return new CurateObjectMetadata((ComplexController) getSpringContext().getBean("complexController"), "complex");
        }
        else if (IntactParticipantEvidence.class.isAssignableFrom(iaClass)) {
            ParticipantController participantController = (ParticipantController) getSpringContext().getBean("participantController");
            participantController.setParticipant((IntactParticipantEvidence) intactObject);
            CurateObjectMetadata meta = new CurateObjectMetadata(participantController, "participant");

            ParticipantEvidence participant = (ParticipantEvidence)intactObject;
            if (participant.getInteraction() instanceof IntactInteractionEvidence){
                IntactInteractionEvidence inter = (IntactInteractionEvidence)participant.getInteraction();
                if (inter.getAc() != null){
                    meta.getParents().add(inter.getAc());
                }
                if (inter.getExperiment() instanceof IntactExperiment){
                    IntactExperiment parent = (IntactExperiment)inter.getExperiment();
                    if (parent.getAc() != null){
                        meta.getParents().add(parent.getAc());
                    }
                    if (parent.getPublication() instanceof IntactPublication){
                        IntactPublication parent2 = (IntactPublication)parent.getPublication();
                        if (parent2.getAc() != null){
                            meta.getParents().add(parent2.getAc());
                        }
                    }
                }
            }
            return meta;
        }
        else if (IntactModelledParticipant.class.isAssignableFrom(iaClass)) {
            ModelledParticipantController participantController = (ModelledParticipantController) getSpringContext().getBean("modelledParticipantController");
            participantController.setParticipant((IntactModelledParticipant)intactObject);
            CurateObjectMetadata meta = new CurateObjectMetadata(participantController,
                    "cparticipant");
            ModelledParticipant part = (ModelledParticipant)intactObject;
            if (part.getInteraction() instanceof IntactComplex){
                IntactComplex parent = (IntactComplex)part.getInteraction();
                if (parent.getAc() != null){
                    meta.getParents().add(parent.getAc());
                }
            }
            return meta;
        } else if (IntactModelledFeature.class.isAssignableFrom(iaClass)) {
            ModelledFeatureController featureController = (ModelledFeatureController) getSpringContext().getBean("modelledFeatureController");
            featureController.setFeature((IntactModelledFeature) intactObject);
            CurateObjectMetadata meta = new CurateObjectMetadata(featureController, "cfeature");
            ModelledFeature feat = (ModelledFeature)intactObject;
            if (feat.getParticipant() instanceof IntactModelledParticipant){
                IntactModelledParticipant part = (IntactModelledParticipant)feat.getParticipant();
                if (part.getAc() != null){
                    meta.getParents().add(part.getAc());
                }
                if (part.getInteraction() instanceof IntactComplex){
                    IntactComplex parent = (IntactComplex)part.getInteraction();
                    if (parent.getAc() != null){
                        meta.getParents().add(parent.getAc());
                    }
                }
            }
            return meta;
        } else if (IntactFeatureEvidence.class.isAssignableFrom(iaClass)) {
            FeatureController featureController = (FeatureController) getSpringContext().getBean("featureController");
            featureController.setFeature((IntactFeatureEvidence) intactObject);
            CurateObjectMetadata meta = new CurateObjectMetadata(featureController, "feature");

            FeatureEvidence feature = (FeatureEvidence)intactObject;
            if (feature.getParticipant() instanceof IntactParticipantEvidence){
                IntactParticipantEvidence participant = (IntactParticipantEvidence)feature.getParticipant();
                if (participant.getAc() != null){
                    meta.getParents().add(participant.getAc());
                }
                if (participant.getInteraction() instanceof IntactInteractionEvidence){
                    IntactInteractionEvidence inter = (IntactInteractionEvidence)participant.getInteraction();
                    if (inter.getAc() != null){
                        meta.getParents().add(inter.getAc());
                    }
                    if (inter.getExperiment() instanceof IntactExperiment){
                        IntactExperiment parent = (IntactExperiment)inter.getExperiment();
                        if (parent.getAc() != null){
                            meta.getParents().add(parent.getAc());
                        }
                        if (parent.getPublication() instanceof IntactPublication){
                            IntactPublication parent2 = (IntactPublication)parent.getPublication();
                            if (parent2.getAc() != null){
                                meta.getParents().add(parent2.getAc());
                            }
                        }
                    }
                }
            }
            return meta;
        } else if (IntactInteractor.class.isAssignableFrom(iaClass)) {
            InteractorController interactorController = (InteractorController) getSpringContext().getBean("interactorController");
            interactorController.setInteractor((IntactInteractor) intactObject);
            CurateObjectMetadata meta = new CurateObjectMetadata(interactorController, "interactor");

            return meta;
        } else if (IntactCvTerm.class.isAssignableFrom(iaClass)) {
            CvObjectController cvObjectController = (CvObjectController) getSpringContext().getBean("cvObjectController");
            cvObjectController.setCvObject((IntactCvTerm) intactObject);
            CurateObjectMetadata meta = new CurateObjectMetadata(cvObjectController, "cvobject");

            return meta;
        } else if (IntactOrganism.class.isAssignableFrom(iaClass)) {
            BioSourceController bioSourceController = (BioSourceController) getSpringContext().getBean("bioSourceController");
            bioSourceController.setBioSource((IntactOrganism) intactObject);
            CurateObjectMetadata meta = new CurateObjectMetadata(bioSourceController, "organism");

            return meta;
        } else if (IntactSource.class.isAssignableFrom(iaClass)) {
            InstitutionController institutionController = (InstitutionController) getSpringContext().getBean("institutionController");
            institutionController.setInstitution((IntactSource) intactObject);
            CurateObjectMetadata meta = new CurateObjectMetadata(institutionController, "institution");

            return meta;
        } else {
            throw new IllegalArgumentException("No view defined for object with type: "+iaClass);
        }
    }

    public String openByAc() {
        if (acToOpen == null) {
            addErrorMessage("Illegal AC", "No AC provided");
            FacesContext.getCurrentInstance().renderResponse();
            return "";
        }

        acToOpen = acToOpen.trim();

        return editByAc(acToOpen);
    }

    public class CurateObjectMetadata {
        private String slug;
        private AnnotatedObjectController annotatedObjectController;
        private Collection<String> parents = new ArrayList<String>();

        private CurateObjectMetadata(AnnotatedObjectController annotatedObjectController, String slug) {
            this.annotatedObjectController = annotatedObjectController;
            this.slug = slug;
        }

        public String getSlug() {
            return slug;
        }

        public Collection<String> getParents() {
            return parents;
        }

        public AnnotatedObjectController getAnnotatedObjectController() {
            return annotatedObjectController;
        }
    }

    public AnnotatedObjectController getCurrentAnnotatedObjectController() {
        return currentAnnotatedObjectController;
    }

    public void setCurrentAnnotatedObjectController(AnnotatedObjectController currentAnnotatedObjectController) {
        this.currentAnnotatedObjectController = currentAnnotatedObjectController;
    }

    public String getAcToOpen() {
        return acToOpen;
    }

    public void setAcToOpen(String acToOpen) {
        this.acToOpen = acToOpen;
    }

    public EditorObjectService getEditorObjectService() {
        if (this.editorObjectService == null){
            this.editorObjectService = ApplicationContextProvider.getBean("editorObjectService");
        }
        return editorObjectService;
    }

    public boolean isComplexCurationEnabled(){
        UserSessionController userSessionController = ApplicationContextProvider.getBean("userSessionController");
        if (userSessionController.hasRole(Role.ROLE_COMPLEX_CURATOR) || userSessionController.hasRole(Role.ROLE_COMPLEX_REVIEWER) ){
            return true;
        }
        return false;
    }

    public boolean isPublicationCurationEnabled(){
        UserSessionController userSessionController = ApplicationContextProvider.getBean("userSessionController");
        if (userSessionController.hasRole(Role.ROLE_CURATOR) || userSessionController.hasRole(Role.ROLE_REVIEWER) ){
            return true;
        }
        return false;
    }
}
