/**
 * Copyright 2010 The European Bioinformatics Institute, and others.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.intact.editor.services.curate.interaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.bridges.chebi.ChebiFetcher;
import psidev.psi.mi.jami.bridges.ensembl.EnsemblInteractorFetcher;
import psidev.psi.mi.jami.bridges.exception.BridgeFailedException;
import psidev.psi.mi.jami.bridges.rna.central.RNACentralFetcher;
import psidev.psi.mi.jami.bridges.uniprot.UniprotGeneFetcher;
import psidev.psi.mi.jami.bridges.uniprot.UniprotProteinFetcher;
import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.utils.XrefUtils;
import uk.ac.ebi.intact.editor.controller.curate.cloner.FeatureEvidenceCloner;
import uk.ac.ebi.intact.editor.controller.curate.cloner.ModelledFeatureCloner;
import uk.ac.ebi.intact.editor.controller.curate.interaction.CandidateType;
import uk.ac.ebi.intact.editor.controller.curate.interaction.ImportCandidate;
import uk.ac.ebi.intact.editor.controller.curate.util.CheckIdentifier;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.jami.context.IntactConfiguration;
import uk.ac.ebi.intact.jami.dao.InteractorDao;
import uk.ac.ebi.intact.jami.dao.InteractorPoolDao;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import javax.annotation.Resource;
import java.util.*;


@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ParticipantImportService extends AbstractEditorService {

    private static final Log log = LogFactory.getLog(ParticipantImportService.class);

    @Resource(name = "proteinFetcher")
    private UniprotProteinFetcher uniprotRemoteService;

    @Resource(name = "bioactiveEntityFetcher")
    private ChebiFetcher chebiFetcher;

    @Resource(name = "uniprotGeneFetcher")
    private UniprotGeneFetcher uniprotGeneFetcher;

    @Resource(name = "ensemblFetcher")
    private EnsemblInteractorFetcher ensemblFetcher;

    @Resource(name = "rnaCentralFetcher")
    private RNACentralFetcher rnaCentralFetcher;

    @Resource(name = "intactJamiConfiguration")
    private IntactConfiguration intactConfig;

    private final static String FEATURE_CHAIN = "PRO_";

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public Set<ImportCandidate> importParticipant(String participantToImport) throws BridgeFailedException, SynchronizerException, FinderException, PersisterException {
        log.debug("Importing participant: " + participantToImport);
        attachDaoToTransactionManager();
        getIntactDao().getEntityManager().clear();

        Set<ImportCandidate> candidates = importFromIntAct(participantToImport.toUpperCase());

        if (candidates.isEmpty()) {     //It is not an IntAct one

            CandidateType candidateType = detectCandidate(participantToImport.toUpperCase());

            switch (candidateType) {
                case BIO_ACTIVE_ENTITY:
                    candidates = importFromChebi(participantToImport.toUpperCase());
                    break;
                case GENE:
                    candidates = importFromEnsembl(participantToImport.toUpperCase());
                    break;
                case NUCLEIC_ACID:
                    candidates = importFromRNACentral(participantToImport.toUpperCase());
                    break;
                case PROTEIN:
                    candidates = importFromUniprot(participantToImport.toUpperCase());
                    break;
            }

            // only pre-select those that match the query
            for (ImportCandidate candidate : candidates) {
                candidate.setSelected(false);

                for (String primaryAc : candidate.getPrimaryAcs()) {
                    if (candidate.getQuery().equalsIgnoreCase(primaryAc)) {
                        candidate.setSelected(true);
                        break;
                    }
                    // for feature chains, in IntAct, we add the parent uniprot ac before the chain id so feature chains are never pre-selected
                    else if (candidate.isChain() && primaryAc.toUpperCase().contains(FEATURE_CHAIN)) {
                        int indexOfChain = primaryAc.indexOf(FEATURE_CHAIN);

                        String chain_ac = primaryAc.substring(indexOfChain);

                        if (candidate.getQuery().equalsIgnoreCase(chain_ac)) {
                            candidate.setSelected(true);
                            break;
                        }
                    }
                }
            }

            candidates.addAll(candidates);

        }

        return candidates;
    }

    private CandidateType detectCandidate(String candidateId) {
        if (CheckIdentifier.checkChebiId(candidateId)) {
            return CandidateType.BIO_ACTIVE_ENTITY;
        } else if (CheckIdentifier.checkEnsembleId(candidateId)) {
            return CandidateType.GENE;
        } else if (CheckIdentifier.checkRNACentralId(candidateId)) {
            return CandidateType.NUCLEIC_ACID;
        } else { //If the identifier is not one of the previous we suppose that is a UniprotKB
            return CandidateType.PROTEIN;
        }
    }

    private Set<ImportCandidate> importFromIntAct(String participantToImport) {
        Map<String, ImportCandidate> candidates = new HashMap<>();

        final InteractorDao<IntactInteractor> interactorDao = getIntactDao().getInteractorDao(IntactInteractor.class);

        // id
        if (participantToImport.startsWith(intactConfig.getAcPrefix())) {
            IntactInteractor interactor = interactorDao.getByAc(participantToImport);

            if (interactor != null) {
                candidates.put(interactor.getAc(), toImportCandidate(participantToImport, interactor));
            } else {
                IntactParticipantEvidence participantEvidence = getIntactDao().getParticipantEvidenceDao().getByAc(participantToImport);

                if (participantEvidence != null) {
                    candidates.put(participantEvidence.getAc(), toImportCandidate(participantToImport, participantEvidence));
                }

                IntactModelledParticipant modelledParticipant = getIntactDao().getModelledParticipantDao().getByAc(participantToImport);

                if (modelledParticipant != null) {
                    candidates.put(modelledParticipant.getAc(), toImportCandidate(participantToImport, modelledParticipant));
                }
            }
        } else {
            // identity xref
            Collection<IntactInteractor> interactorsByXref = interactorDao.getByXrefQualifier(Xref.IDENTITY, Xref.IDENTITY_MI, participantToImport);
            addCandidatesWithoutDuplicates(participantToImport, candidates, interactorsByXref);

            if (candidates.isEmpty()) {
                // shortLabel
                final Collection<IntactInteractor> interactorsByLabel = interactorDao.getByShortNameLike(participantToImport);
                addCandidatesWithoutDuplicates(participantToImport, candidates, interactorsByLabel);
            }
        }

        return new HashSet<>(candidates.values());
    }

    private void addCandidatesWithoutDuplicates(String participantToImport, Map<String, ImportCandidate> candidates, Collection<IntactInteractor> interactors) {
        final InteractorPoolDao poolDao = getIntactDao().getInteractorPoolDao();

        for (IntactInteractor interactor : interactors) {
            addCandidate(candidates, interactor.getAc(), toImportCandidate(participantToImport, interactor));
            Collection<IntactInteractorPool> pools = poolDao.getByInteractorAc(interactor.getAc());
            for (IntactInteractorPool pool : pools) {
                addCandidate(candidates, pool.getAc(), toImportCandidate(participantToImport, pool));
            }
        }
    }

    private void addCandidate(Map<String, ImportCandidate> candidates, String ac, ImportCandidate newCandidate) {
        if (candidates.containsKey(ac)) {
            ImportCandidate existingCandidate = candidates.get(ac);
            // Merge list of primary ACs
            Set<String> primaryAcs = new HashSet<>(existingCandidate.getPrimaryAcs());
            primaryAcs.addAll(newCandidate.getPrimaryAcs());
            existingCandidate.setPrimaryAcs(new ArrayList<>(primaryAcs));
            // Merge list of secondary ACs
            Set<String> secondaryAcs = new HashSet<>(existingCandidate.getSecondaryAcs());
            secondaryAcs.addAll(newCandidate.getSecondaryAcs());
            existingCandidate.setSecondaryAcs(new ArrayList<>(secondaryAcs));
        } else {
            candidates.put(ac, newCandidate);
        }
    }


    private Set<ImportCandidate> importFromUniprot(String participantToImport) throws BridgeFailedException, SynchronizerException, FinderException, PersisterException {
        Set<ImportCandidate> candidates = new HashSet<ImportCandidate>();

        final Collection<Protein> uniprotProteins = uniprotRemoteService.fetchByIdentifier(participantToImport);

        for (Protein uniprotProtein : uniprotProteins) {
            ImportCandidate candidate = new ImportCandidate(participantToImport, uniprotProtein);
            candidate.setSource("uniprotkb");
            candidate.setInteractor(toProtein(candidate));
            candidates.add(candidate);
        }

        return candidates;
    }

    private Set<ImportCandidate> importFromEnsembl(String participantToImport) throws BridgeFailedException, FinderException, SynchronizerException, PersisterException {
        Set<ImportCandidate> candidates = new HashSet<ImportCandidate>();

        final Collection<Interactor> interactors = ensemblFetcher.fetchByIdentifier(participantToImport);
        for (Interactor interactor : interactors) {
            IntactInteractor i = interactor instanceof Gene ? toGene((Gene) interactor) : toNucleicAcid((NucleicAcid) interactor);
            ImportCandidate candidate = toImportCandidate(participantToImport, i);
            candidate.setSource("ensembl");
            candidates.add(candidate);
        }

        return candidates;
    }

    private Set<ImportCandidate> importFromRNACentral(String participantToImport) throws BridgeFailedException, FinderException, SynchronizerException, PersisterException {
        Set<ImportCandidate> candidates = new HashSet<ImportCandidate>();

        final Collection<NucleicAcid> nucleicAcids = rnaCentralFetcher.fetchByIdentifier(participantToImport);
        for (NucleicAcid nucleicAcid : nucleicAcids) {
            ImportCandidate candidate = toImportCandidate(participantToImport, toNucleicAcid(nucleicAcid));
            candidate.setSource("RNA-Central");
            candidates.add(candidate);
        }

        return candidates;
    }


    private Set<ImportCandidate> importFromChebi(String participantToImport) throws BridgeFailedException, SynchronizerException, FinderException, PersisterException {
        Set<ImportCandidate> candidates = new HashSet<ImportCandidate>();

        final Collection<BioactiveEntity> smallMolecules = chebiFetcher.fetchByIdentifier(participantToImport);
        for (BioactiveEntity entity : smallMolecules) {
            ImportCandidate candidate = toImportCandidate(participantToImport, toBioactiveEntity(entity));
            candidate.setSource("chebi");
            candidates.add(candidate);
        }

        return candidates;
    }

    private Set<ImportCandidate> importFromSwissProtWithEnsemblId(String participantToImport) throws BridgeFailedException, SynchronizerException, FinderException, PersisterException {
        Set<ImportCandidate> candidates = new HashSet<ImportCandidate>();

        final Collection<Gene> genes = uniprotGeneFetcher.fetchByIdentifier(participantToImport);

        for (Gene gene : genes) {
            ImportCandidate candidate = toImportCandidate(participantToImport, toGene(gene));
            candidate.setSource("uniprot - ensembl");
            candidates.add(candidate);
        }
        return candidates;
    }


    private ImportCandidate toImportCandidate(String participantToImport, IntactInteractor interactor) {
        ImportCandidate candidate = new ImportCandidate(participantToImport, interactor);
        candidate.setSource(intactConfig.getDefaultInstitution().getShortName());

        // initialise some properties
        if (interactor.getAc() != null) {
            initialiseXrefs(interactor.getDbXrefs());
            initialiseAnnotations(interactor.getDbAnnotations());
            initialiseAliases(interactor.getDbAliases());
            initialiseCv(interactor.getInteractorType());
        }

        final Collection<Xref> identityXrefs = interactor.getIdentifiers();

        if (!identityXrefs.isEmpty()) {
            Set<String> ids = new HashSet<>(identityXrefs.size());
            Set<String> secondaryAcs = new HashSet<>();

            for (Xref xref : identityXrefs) {
                boolean isIntact = XrefUtils.isXrefFromDatabase(xref,
                        intactConfig.getDefaultInstitution().getMIIdentifier(), intactConfig.getDefaultInstitution().getShortName());
                // exclude intact acs
                if (XrefUtils.doesXrefHaveQualifier(xref, Xref.IDENTITY_MI, Xref.IDENTITY)) {
                    if (!isIntact) {
                        ids.add(xref.getId());
                    }
                } else {
                    secondaryAcs.add(xref.getId());
                }
            }

            candidate.setPrimaryAcs(new ArrayList<>(ids));
            candidate.setSecondaryAcs(new ArrayList<>(secondaryAcs));
        }

        return candidate;
    }

    private ImportCandidate toImportCandidate(String participantToImport, IntactParticipantEvidence p) {
        IntactInteractor interactor = (IntactInteractor) p.getInteractor();
        ImportCandidate candidate = toImportCandidate(participantToImport, interactor);

        if (interactor.getAc() != null) {
            initialiseXrefs(interactor.getDbXrefs());
            initialiseAnnotations(interactor.getDbAnnotations());
            initialiseAliases(interactor.getDbAliases());
            initialiseCv(interactor.getInteractorType());
        }

        if (!p.getFeatures().isEmpty()) {
            FeatureEvidenceCloner cloner = new FeatureEvidenceCloner();
            for (FeatureEvidence f : p.getFeatures()) {
                candidate.getClonedFeatures().add(cloner.clone(f, getIntactDao()));
            }
        }

        return candidate;
    }

    private ImportCandidate toImportCandidate(String participantToImport, IntactModelledParticipant p) {
        IntactInteractor interactor = (IntactInteractor) p.getInteractor();
        ImportCandidate candidate = toImportCandidate(participantToImport, interactor);

        if (interactor.getAc() != null) {
            initialiseXrefs(interactor.getDbXrefs());
            initialiseAnnotations(interactor.getDbAnnotations());
            initialiseAliases(interactor.getDbAliases());
            initialiseCv(interactor.getInteractorType());
        }

        if (!p.getFeatures().isEmpty()) {
            ModelledFeatureCloner cloner = new ModelledFeatureCloner();
            for (ModelledFeature f : p.getFeatures()) {
                candidate.getClonedFeatures().add(cloner.clone(f, getIntactDao()));
            }
        }

        return candidate;
    }

    private ImportCandidate toImportCandidate(String participantToImport, IntactInteractorPool interactor) {
        ImportCandidate candidate = new ImportCandidate(participantToImport, interactor);
        candidate.setSource(intactConfig.getDefaultInstitution().getShortName());
        Set<String> ids = new HashSet<>();
        Set<String> secondaryAcs = new HashSet<String>();

        if (interactor.getAc() != null) {
            initialiseXrefs(interactor.getDbXrefs());
            initialiseAnnotations(interactor.getDbAnnotations());
            initialiseAliases(interactor.getDbAliases());
            initialiseCv(interactor.getInteractorType());
        }
        for (Interactor member : interactor) {
            // initialise some properties
            if (((IntactInteractor) member).getAc() != null) {
                initialiseXrefs(((IntactInteractor) member).getDbXrefs());
                initialiseAnnotations(((IntactInteractor) member).getDbAnnotations());
                initialiseAliases(((IntactInteractor) member).getDbAliases());
                initialiseCv(member.getInteractorType());
            }

            final Collection<Xref> identityXrefs = member.getIdentifiers();

            if (!identityXrefs.isEmpty()) {

                for (Xref xref : identityXrefs) {
                    boolean isIntact = XrefUtils.isXrefFromDatabase(xref,
                            intactConfig.getDefaultInstitution().getMIIdentifier(), intactConfig.getDefaultInstitution().getShortName());
                    if (XrefUtils.doesXrefHaveQualifier(xref, Xref.IDENTITY_MI, Xref.IDENTITY)) {
                        if (!isIntact) {
                            ids.add(xref.getId());
                        }
                    } else {
                        secondaryAcs.add(xref.getId());
                    }
                }
            }
        }
        candidate.setPrimaryAcs(new ArrayList<>(ids));
        candidate.setSecondaryAcs(new ArrayList<>(secondaryAcs));

        return candidate;
    }

    private IntactProtein toProtein(ImportCandidate candidate) throws SynchronizerException, FinderException, PersisterException {
        IntactProtein protein = null;

        // use the protein service to create proteins (not persist!)
        if (candidate.getUniprotProtein() != null) {
            protein = convertToPersistentIntactObject(candidate.getUniprotProtein(), getIntactDao().getSynchronizerContext().getProteinSynchronizer());
        }

        return protein;
    }

    private IntactBioactiveEntity toBioactiveEntity(BioactiveEntity candidate) throws SynchronizerException, FinderException, PersisterException {
        IntactBioactiveEntity entity = null;

        if (candidate != null) {
            entity = convertToPersistentIntactObject(candidate, getIntactDao().getSynchronizerContext().getBioactiveEntitySynchronizer());
        }

        return entity;
    }

    private IntactGene toGene(Gene candidate) throws SynchronizerException, FinderException, PersisterException {
        IntactGene entity = null;

        if (candidate != null) {
            entity = convertToPersistentIntactObject(candidate, getIntactDao().getSynchronizerContext().getGeneSynchronizer());
        }

        return entity;
    }

    private IntactNucleicAcid toNucleicAcid(NucleicAcid candidate) throws FinderException, SynchronizerException, PersisterException {
        IntactNucleicAcid entity = null;

        if (candidate != null) {
            entity = convertToPersistentIntactObject(candidate, getIntactDao().getSynchronizerContext().getNucleicAcidSynchronizer());
        }

        return entity;
    }
}
