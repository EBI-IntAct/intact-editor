package uk.ac.ebi.intact.editor.services.enricher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.*;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;

/**
 * Db import service
 */
@Service
public class DbEnricherService extends AbstractEditorService {

    private static final Log log = LogFactory.getLog( DbEnricherService.class );

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public boolean isNewComplex(Complex complex){
        return getIntactDao().getSynchronizerContext().getComplexSynchronizer().findAllMatchingAcs(complex).isEmpty();
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public boolean isNewParticipantEvidence(ParticipantEvidence participant){
        return getIntactDao().getSynchronizerContext().getParticipantEvidenceSynchronizer().findAllMatchingAcs(participant).isEmpty();
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public boolean isNewInteractor(Interactor interactor){
        return getIntactDao().getSynchronizerContext().getInteractorSynchronizer().findAllMatchingAcs(interactor).isEmpty();
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public boolean isNewCvTerm(CvTerm cv){
        return getIntactDao().getSynchronizerContext().getGeneralCvSynchronizer().findAllMatchingAcs(cv).isEmpty();
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public boolean isNewExperiment(Experiment experiment){
        return getIntactDao().getSynchronizerContext().getExperimentSynchronizer().findAllMatchingAcs(experiment).isEmpty();
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public boolean isNewFeatureEvidence(FeatureEvidence feature){
        return getIntactDao().getSynchronizerContext().getFeatureEvidenceSynchronizer().findAllMatchingAcs(feature).isEmpty();
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public boolean isNewInteractionEvidence(InteractionEvidence interactionEvidence){
        return getIntactDao().getSynchronizerContext().getInteractionSynchronizer().findAllMatchingAcs(interactionEvidence).isEmpty();
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public boolean isNewModelledParticipant(ModelledParticipant participant){
        return getIntactDao().getSynchronizerContext().getModelledParticipantSynchronizer().findAllMatchingAcs(participant).isEmpty();
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public boolean isNewModelledFeature(ModelledFeature feature){
        return getIntactDao().getSynchronizerContext().getModelledFeatureSynchronizer().findAllMatchingAcs(feature).isEmpty();
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public boolean isNewOrganism(Organism organism){
        return getIntactDao().getSynchronizerContext().getOrganismSynchronizer().findAllMatchingAcs(organism).isEmpty();
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public boolean isNewPublication(Publication publication){
        return getIntactDao().getSynchronizerContext().getPublicationSynchronizer().findAllMatchingAcs(publication).isEmpty();
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public boolean isNewSource(Source source){
        return getIntactDao().getSynchronizerContext().getSourceSynchronizer().findAllMatchingAcs(source).isEmpty();
    }
}
