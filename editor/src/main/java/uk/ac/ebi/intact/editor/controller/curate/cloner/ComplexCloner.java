/**
 * Copyright 2010 The European Bioinformatics Institute, and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.intact.editor.controller.curate.cloner;

import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import psidev.psi.mi.jami.utils.XrefUtils;
import uk.ac.ebi.intact.editor.controller.UserSessionController;
import uk.ac.ebi.intact.editor.services.curate.organism.BioSourceService;
import uk.ac.ebi.intact.editor.services.curate.cvobject.CvObjectService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.dao.IntactDao;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.model.lifecycle.Releasable;
import uk.ac.ebi.intact.jami.service.CvTermService;
import uk.ac.ebi.intact.jami.service.OrganismService;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

/**
 * Editor specific cloning routine for complex participants.
 *
 * @author Prem Anand (prem@ebi.ac.uk)
 * @version $Id: InteractionIntactCloner.java 14783 2010-07-29 12:52:28Z brunoaranda $
 * @since 2.0.1-SNAPSHOT
 */
public class ComplexCloner extends AbstractEditorCloner<Complex, IntactComplex> {

    private EditorCloner<Participant, IntactModelledParticipant> participantCloner;

    public IntactComplex cloneFromEvidence(InteractionEvidence evidence, IntactDao dao) throws SynchronizerException, FinderException, PersisterException {
        IntactComplex clone = new IntactComplex(evidence.getShortName());

        initAuditProperties(clone, dao);

        clone.setInteractionType(evidence.getInteractionType());
        CvObjectService cvService = ApplicationContextProvider.getBean("cvObjectService");
        CvTermService cvTermService = ApplicationContextProvider.getBean("cvTermService");

        BioSourceService biosourceService = ApplicationContextProvider.getBean("bioSourceService");
        OrganismService organismService = ApplicationContextProvider.getBean("organismService");

        clone.setEvidenceType(cvService.findCvObjectByIdentifier(IntactUtils.DATABASE_OBJCLASS, "ECO:0000353"));
        clone.setInteractorType(cvService.findCvObjectByIdentifier(IntactUtils.INTERACTOR_TYPE_OBJCLASS, Complex.COMPLEX_MI));

        if (evidence.getExperiment() != null && evidence.getExperiment().getHostOrganism() != null){
            Organism host = evidence.getExperiment().getHostOrganism();
            if (host.getCellType() == null && host.getTissue() == null){
                clone.setOrganism(host);
            }
            else{
                IntactOrganism org = biosourceService.findBiosourceByTaxid(host.getTaxId());
                if (org == null){
                    org = new IntactOrganism(host.getTaxId(), host.getCommonName(), host.getScientificName());
                    organismService.saveOrUpdate(org);
                    biosourceService.loadData();
                    org = biosourceService.findBiosourceByTaxid(host.getTaxId());
                }
                clone.setOrganism(org);
            }
        }

        UserSessionController userSessionController = ApplicationContextProvider.getBean("userSessionController");
        clone.setSource(userSessionController.getUserInstitution());

        // get exp evidences
        if (!evidence.getIdentifiers().isEmpty()){
            IntactCvTerm expEvidence = cvService.findCvObject(IntactUtils.QUALIFIER_OBJCLASS, "exp-evidence");
            if (expEvidence == null){
                expEvidence = new IntactCvTerm("exp-evidence");
                expEvidence.setObjClass(IntactUtils.QUALIFIER_OBJCLASS);
                cvTermService.saveOrUpdate(expEvidence);
                cvService.refreshCvs(IntactUtils.QUALIFIER_OBJCLASS);
                expEvidence = cvService.findCvObject(IntactUtils.QUALIFIER_OBJCLASS, "exp-evidence");
            }

            for (Xref ref : evidence.getIdentifiers()){
                CvTerm db = ref.getDatabase();
                if (!(db instanceof IntactCvTerm)){
                    db = cvService.findCvObject(IntactUtils.DATABASE_OBJCLASS, ref.getDatabase().getShortName());
                    if (db == null){
                        db = new IntactCvTerm(ref.getDatabase().getShortName());
                        ((IntactCvTerm)db).setObjClass(IntactUtils.DATABASE_OBJCLASS);
                        cvTermService.saveOrUpdate(db);
                        cvService.refreshCvs(IntactUtils.DATABASE_OBJCLASS);
                        db = cvService.findCvObject(IntactUtils.DATABASE_OBJCLASS, ref.getDatabase().getShortName());
                    }
                }
                clone.getIdentifiers().add(new InteractorXref(db, ref.getId(), ref.getVersion(),
                        cvService.findCvObjectByAc(expEvidence.getAc())));
            }
        }

        for (Xref ref : evidence.getXrefs()){
            if (!XrefUtils.isXrefFromDatabase(ref, Xref.IMEX_MI, Xref.IMEX)
                    && !XrefUtils.doesXrefHaveQualifier(ref, Xref.PRIMARY_MI, Xref.PRIMARY)){
                clone.getXrefs().add(new InteractorXref(ref.getDatabase(), ref.getId(), ref.getVersion(), ref.getQualifier()));
            }
        }

        for (Annotation annotation : evidence.getAnnotations()){
            if (!AnnotationUtils.doesAnnotationHaveTopic(annotation, null, Releasable.CORRECTION_COMMENT)
                    && !AnnotationUtils.doesAnnotationHaveTopic(annotation, null, Releasable.ON_HOLD)
                    && !AnnotationUtils.doesAnnotationHaveTopic(annotation, null, Releasable.TO_BE_REVIEWED)
                    && !AnnotationUtils.doesAnnotationHaveTopic(annotation, null, Releasable.ACCEPTED)
                    && !AnnotationUtils.doesAnnotationHaveTopic(annotation, Annotation.FIGURE_LEGEND_MI, Annotation.FIGURE_LEGEND)){
                clone.getAnnotations().add(new InteractorAnnotation(annotation.getTopic(), annotation.getValue()));
            }
        }

        for (Object obj : evidence.getParticipants()){
            Participant participant = (Participant)obj;
            ModelledParticipant r = getModelledParticipantCloner().clone(participant, dao);
            clone.addParticipant(r);
        }

        for (Confidence confidence : evidence.getConfidences()){
            clone.getModelledConfidences().add(new ComplexConfidence(confidence.getType(), confidence.getValue()));
        }

        for (Parameter param : evidence.getParameters()){
            clone.getModelledParameters().add(new ComplexParameter(param.getType(), param.getValue(), param.getUnit(), param.getUncertainty()));
        }

        return clone;
    }

    public IntactComplex clone(Complex complex, IntactDao dao) {
        IntactComplex clone = new IntactComplex(complex.getShortName());

        initAuditProperties(clone, dao);

        clone.setOrganism(complex.getOrganism());
        clone.setEvidenceType(complex.getEvidenceType());
        clone.setInteractionType(complex.getInteractionType());
        clone.setInteractorType(complex.getInteractorType());
        clone.setFullName(complex.getFullName());

        UserSessionController userSessionController = ApplicationContextProvider.getBean("userSessionController");
        clone.setSource(userSessionController.getUserInstitution());

        for (Object obj : complex.getAliases()){
            Alias alias = (Alias)obj;
            clone.getAliases().add(new InteractorAlias(alias.getType(), alias.getName()));
        }

        // do not clone identifiers, only xrefs
        for (Xref ref : complex.getXrefs()){
            if (!XrefUtils.isXrefFromDatabase(ref, Xref.IMEX_MI, Xref.IMEX)
                    && !XrefUtils.doesXrefHaveQualifier(ref, Xref.PRIMARY_MI, Xref.PRIMARY)){
                clone.getXrefs().add(new InteractorXref(ref.getDatabase(), ref.getId(), ref.getVersion(), ref.getQualifier()));
            }
        }

        for (Annotation annotation : complex.getAnnotations()){
            if (!AnnotationUtils.doesAnnotationHaveTopic(annotation, null, Releasable.CORRECTION_COMMENT)
                    && !AnnotationUtils.doesAnnotationHaveTopic(annotation, null, Releasable.ON_HOLD)
                    && !AnnotationUtils.doesAnnotationHaveTopic(annotation, null, Releasable.TO_BE_REVIEWED)
                    && !AnnotationUtils.doesAnnotationHaveTopic(annotation, null, Releasable.ACCEPTED) ){
                clone.getAnnotations().add(new InteractorAnnotation(annotation.getTopic(), annotation.getValue()));
            }
        }

        for (Object obj : complex.getParticipants()){
            Participant participant = (Participant)obj;
            ModelledParticipant r = getModelledParticipantCloner().clone(participant, dao);
            clone.addParticipant(r);
        }

        for (Confidence confidence : complex.getModelledConfidences()){
            clone.getModelledConfidences().add(new ComplexConfidence(confidence.getType(), confidence.getValue()));
        }

        for (Parameter param : complex.getModelledParameters()){
            clone.getModelledParameters().add(new ComplexParameter(param.getType(), param.getValue(), param.getUnit(), param.getUncertainty()));
        }

        return clone;
    }

    public void copyInitialisedProperties(IntactComplex source, IntactComplex target) {
        target.setShortName(source.getShortName());
        target.setOrganism(source.getOrganism());
        target.setEvidenceType(source.getEvidenceType());
        target.setInteractionType(source.getInteractionType());
        target.setInteractorType(source.getInteractorType());
        target.setFullName(source.getFullName());
        target.setSource(source.getSource());

        if (source.areAliasesInitialized()){
            target.getAliases().clear();
            target.getAliases().addAll(source.getAliases());
        }

        // do not clone identifiers, only xrefs
        if (source.areXrefsInitialized()){
            target.getIdentifiers().clear();
            target.getIdentifiers().addAll(source.getIdentifiers());
            target.getXrefs().clear();
            target.getXrefs().addAll(source.getXrefs());
        }

        if (source.areAnnotationsInitialized()){
            target.getAnnotations().clear();
            target.getAnnotations().addAll(source.getAnnotations());
        }

        if (source.areParticipantsInitialized()){
            target.getParticipants().clear();
            target.addAllParticipants(source.getParticipants());
        }

        if (source.areConfidencesInitialized()){
            target.getModelledConfidences().clear();
            target.getModelledConfidences().addAll(source.getModelledConfidences());
        }

        if (source.areParametersInitialized()){
            target.getModelledParameters().clear();
            target.getModelledParameters().addAll(source.getModelledParameters());
        }
    }

    public EditorCloner<Participant, IntactModelledParticipant> getModelledParticipantCloner(){
        if (this.participantCloner == null){
            this.participantCloner = new ModelledParticipantCloner();
        }
        return this.participantCloner;
    }

    protected void setParticipantCloner(EditorCloner<Participant, IntactModelledParticipant> participantCloner) {
        this.participantCloner = participantCloner;
    }
}

