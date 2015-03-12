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
package uk.ac.ebi.intact.editor.services.curate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.bridges.exception.BridgeFailedException;
import psidev.psi.mi.jami.bridges.fetcher.ProteinFetcher;
import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.utils.XrefUtils;
import uk.ac.ebi.intact.editor.controller.curate.UnsavedChange;
import uk.ac.ebi.intact.editor.controller.curate.cloner.EditorCloner;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.context.IntactConfiguration;
import uk.ac.ebi.intact.jami.lifecycle.LifeCycleManager;
import uk.ac.ebi.intact.jami.model.IntactPrimaryObject;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.model.lifecycle.LifeCycleEvent;
import uk.ac.ebi.intact.jami.model.lifecycle.LifeCycleEventType;
import uk.ac.ebi.intact.jami.model.lifecycle.LifeCycleStatus;
import uk.ac.ebi.intact.jami.model.lifecycle.Releasable;
import uk.ac.ebi.intact.jami.model.user.User;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.IntactDbSynchronizer;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;
import uk.ac.ebi.intact.jami.utils.IntactUtils;
import uk.ac.ebi.intact.jami.utils.ReleasableUtils;

import javax.annotation.Resource;
import java.util.*;

/**
 * General Editor service to save objects in the database
 */

@Service
@Scope( BeanDefinition.SCOPE_PROTOTYPE )
public class EditorObjectService extends AbstractEditorService {

    private static final Log log = LogFactory.getLog( EditorObjectService.class );

    @Resource(name = "proteinFetcher")
    private ProteinFetcher proteinFetcher;
    @Resource(name = "jamiLifeCycleManager")
    private transient LifeCycleManager lifecycleManager;

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public <T extends IntactPrimaryObject> T doSave( IntactPrimaryObject object,
                                                     IntactDbSynchronizer dbSynchronizer) throws SynchronizerException,
            FinderException, PersisterException {

        if ( object == null ) {
            log.error( "No annotated object to save");
            return null;
        }
        else{
            // attach dao to transaction manager to clear cache
            attachDaoToTransactionManager();

            return (T)synchronizeIntactObject(object, dbSynchronizer, true);
        }
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public void deleteVariableParameter( IntactVariableParameter param) throws SynchronizerException,
            FinderException, PersisterException {

        if ( param == null ) {
            log.error( "No parameter to delete");
        }
        else{
            // attach dao to transaction manager to clear cache
            attachDaoToTransactionManager();

            deleteIntactObject(param, getIntactDao().getVariableParameterDao());
        }
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public void deleteVariableParameterValueFromInteractions(IntactVariableParameterValue param, IntactExperiment experiment) throws SynchronizerException,
            FinderException, PersisterException {

        if ( param == null ) {
            log.error( "No parameter to delete");
        }
        else{
            // attach dao to transaction manager to clear cache
            attachDaoToTransactionManager();

            if (experiment.areInteractionEvidencesInitialized()){
                for (InteractionEvidence ev : experiment.getInteractionEvidences()){
                    IntactInteractionEvidence intactEv = (IntactInteractionEvidence)ev;
                    if (intactEv.areVariableParameterValuesInitialized()){
                        for (VariableParameterValueSet v : intactEv.getVariableParameterValues()){
                            Iterator<VariableParameterValue> valueIterator = v.iterator();
                            while (valueIterator.hasNext()){
                                IntactVariableParameterValue value = (IntactVariableParameterValue)valueIterator.next();
                                if (param.getId() != null && param.getId().equals(value.getId())){
                                    valueIterator.remove();
                                    break;
                                }
                                else if (param.getId() == null && param == value){
                                    valueIterator.remove();
                                    break;
                                }
                            }
                        }
                        updateIntactObject(intactEv, getIntactDao().getInteractionDao());
                    }
                }
            }

            deleteIntactObject(param, getIntactDao().getVariableParameterValueDao());
        }
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public <T extends IntactPrimaryObject> T doRevert(IntactPrimaryObject intactObject) {
        if (intactObject.getAc() != null) {
            if (log.isDebugEnabled()) log.debug("Reverting: " + intactObject.getClass()+", Ac="+intactObject.getAc());

            // clear manager first to avaoid to have remaining objects from other transactions
            getIntactDao().getEntityManager().clear();

            intactObject = getIntactDao().getEntityManager().find(intactObject.getClass(), intactObject.getAc());
        }

        return (T)intactObject;
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public boolean doDelete(IntactPrimaryObject intactObject, IntactDbSynchronizer dbSynchronizer) {
        if (intactObject.getAc() != null) {
            if (log.isDebugEnabled()) log.debug("Deleting " + intactObject.getClass()+", Ac="+intactObject.getAc());
            // attach dao to transaction manager to clear cache
            attachDaoToTransactionManager();
            getIntactDao().getEntityManager().clear();

            return dbSynchronizer.delete(intactObject);
        }

        return false;
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public void saveAll(Collection<UnsavedChange> changes, Collection<UnsavedChange> currentChangesForUser) throws SynchronizerException, FinderException, PersisterException {

        for (UnsavedChange unsaved : changes){
            IntactPrimaryObject object = unsaved.getUnsavedObject();

            // checks that the current unsaved change is not obsolete because of a previous change (when saving/deleting, some unsaved change became obsolete and have been removed from the unsaved changes)
            if (currentChangesForUser.contains(unsaved)){
                doSave(object, unsaved.getDbSynchronizer());

            }
        }
    }

    /**
     * Save a master protein and update the cross reference of a protein transcript which will be created later
     * @param intactObject
     */
    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public void doSaveMasterProteins(IntactPrimaryObject intactObject) throws BridgeFailedException, SynchronizerException, FinderException, PersisterException {

        if (intactObject instanceof Protein){
            attachDaoToTransactionManager();
            getIntactDao().getEntityManager().clear();

            Protein proteinTranscript = (Protein) intactObject;
            Collection<psidev.psi.mi.jami.model.Xref> xrefsToDelete = new ArrayList<psidev.psi.mi.jami.model.Xref>(proteinTranscript.getXrefs().size());
            List<Xref> transcriptXrefs = new ArrayList<Xref>(proteinTranscript.getXrefs());
            for (Xref xref : transcriptXrefs) {
                CvTerm qualifier = xref.getQualifier();

                if (qualifier != null){
                    if (XrefUtils.doesXrefHaveQualifier(xref, Xref.CHAIN_PARENT_MI, Xref.CHAIN_PARENT) ||
                            XrefUtils.doesXrefHaveQualifier(xref, Xref.ISOFORM_PARENT_MI, Xref.ISOFORM_PARENT)) {
                        String primaryId = xref.getId().replaceAll("\\?", "");

                        Collection<Protein> proteins = proteinFetcher.fetchByIdentifier(primaryId);

                        if (proteins.size() > 0){
                            xrefsToDelete.add(xref);

                            IntactConfiguration intactConfig = ApplicationContextProvider.getBean("intactJamiConfiguration");
                            for (Protein prot : proteins){
                                try{
                                    IntactInteractor intactprotein = getIntactDao().getSynchronizerContext().getProteinSynchronizer().synchronize(prot, true);
                                    ((Protein) intactObject).getXrefs().add(new InteractorXref(IntactUtils.createMIDatabase(intactConfig.getDefaultInstitution().getShortName(),
                                            intactConfig.getDefaultInstitution().getMIIdentifier()), intactprotein.getAc(), xref.getQualifier()));
                                }
                                catch (SynchronizerException e){
                                    getIntactDao().getSynchronizerContext().clearCache();
                                    getIntactDao().getEntityManager().clear();
                                    throw e;
                                }
                                catch (FinderException e){
                                    getIntactDao().getSynchronizerContext().clearCache();
                                    getIntactDao().getEntityManager().clear();
                                    throw e;
                                }
                                catch (PersisterException e){
                                    getIntactDao().getSynchronizerContext().clearCache();
                                    getIntactDao().getEntityManager().clear();
                                    throw e;
                                }
                                catch (Throwable e){
                                    getIntactDao().getSynchronizerContext().clearCache();
                                    getIntactDao().getEntityManager().clear();
                                    throw new PersisterException(e.getMessage(), e);
                                }
                            }
                        }
                    }
                }
            }

            proteinTranscript.getXrefs().removeAll(xrefsToDelete);

            try{
                getIntactDao().getProteinDao().update((IntactProtein)intactObject);
            }
            catch (SynchronizerException e){
                getIntactDao().getSynchronizerContext().clearCache();
                getIntactDao().getEntityManager().clear();
                throw e;
            }
            catch (FinderException e){
                getIntactDao().getSynchronizerContext().clearCache();
                getIntactDao().getEntityManager().clear();
                throw e;
            }
            catch (PersisterException e){
                getIntactDao().getSynchronizerContext().clearCache();
                getIntactDao().getEntityManager().clear();
                throw e;
            }
            catch (Throwable e){
                getIntactDao().getSynchronizerContext().clearCache();
                getIntactDao().getEntityManager().clear();
                throw new PersisterException(e.getMessage(), e);
            }
        }
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public void revertAll(Collection<UnsavedChange> jamiChanges) {

        for (UnsavedChange unsaved : jamiChanges){
            doRevert(unsaved.getUnsavedObject());
        }
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public Collection<String> findObjectDuplicates(IntactPrimaryObject jamiObject, IntactDbSynchronizer dbSynchronizer){
        // if the annotated object does not have an ac, check if another one similar exists in the db
        if (jamiObject.getAc() == null) {
            return dbSynchronizer.findAllMatchingAcs(jamiObject);
        }
        return Collections.EMPTY_LIST;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public <T extends IntactPrimaryObject> T loadByAc(String ac) {

        ac = ac.trim();

        IntactPrimaryObject primary = getIntactDao().getEntityManager().find(IntactPublication.class, ac);
        if (primary == null){
            primary = getIntactDao().getEntityManager().find(IntactExperiment.class, ac);
            if (primary == null){
                primary = getIntactDao().getEntityManager().find(IntactInteractionEvidence.class, ac);

                if (primary == null){
                    primary = getIntactDao().getEntityManager().find(IntactComplex.class, ac);

                    if (primary == null){
                        primary = getIntactDao().getEntityManager().find(IntactParticipantEvidence.class, ac);

                        if (primary == null){
                            primary = getIntactDao().getEntityManager().find(IntactModelledParticipant.class, ac);

                            if (primary == null){
                                primary = getIntactDao().getEntityManager().find(IntactFeatureEvidence.class, ac);

                                if (primary == null){
                                    primary = getIntactDao().getEntityManager().find(IntactModelledFeature.class, ac);

                                    if (primary == null){
                                        primary = getIntactDao().getEntityManager().find(IntactInteractor.class, ac);

                                        if (primary == null){
                                            primary = getIntactDao().getEntityManager().find(IntactOrganism.class, ac);

                                            if (primary == null){
                                                primary = getIntactDao().getEntityManager().find(IntactCvTerm.class, ac);

                                                if (primary == null){
                                                    primary = getIntactDao().getEntityManager().find(IntactSource.class, ac);

                                                    return (T)primary;
                                                }
                                                else{
                                                    return (T)primary;
                                                }
                                            }
                                            else{
                                                return (T)primary;
                                            }
                                        }
                                        else{
                                            return (T)primary;
                                        }
                                    }
                                    else{
                                        return (T)primary;
                                    }
                                }
                                else{
                                    return (T)primary;
                                }
                            }
                            else{
                                return (T)primary;
                            }
                        }
                        else{
                            return (T)primary;
                        }
                    }
                    else{
                        return (T)primary;
                    }
                }
                else{
                    return (T)primary;
                }
            }
            else{
                return (T)primary;
            }
        }
        else{
            return (T)primary;
        }
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public <T extends IntactPrimaryObject> T loadByAc(String ac, Class<T> clazz) {

        ac = ac.trim();

        return getIntactDao().getEntityManager().find(clazz, ac);
    }

    public void setUser(User user){
        getIntactDao().getUserContext().setUser(user);
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public Collection<LifeCycleEvent> initialiseLifecycleEvents(Releasable releasable) {
        // reload complex without flushing changes
        Releasable reloaded = releasable;
        // merge current user because detached
        if (((IntactPrimaryObject)releasable).getAc() != null && !getIntactDao().getEntityManager().contains(releasable)){
            reloaded = getIntactDao().getEntityManager().find(releasable.getClass(), ((IntactPrimaryObject) releasable).getAc());
            if (reloaded == null){
                reloaded = releasable;
            }
        }

        Collection<LifeCycleEvent> events = reloaded.getLifecycleEvents();
        if (((IntactPrimaryObject) releasable).getAc() != null){
            Hibernate.initialize(events);
        }
        return reloaded.getLifecycleEvents();
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public IntactPrimaryObject refresh(IntactPrimaryObject object) {
        return getIntactDao().getEntityManager().find(object.getClass(), object.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public void synchronizeExperimentShortLabel(IntactExperiment object) {
        IntactUtils.synchronizeExperimentShortLabel(object, getIntactDao().getEntityManager(), Collections.EMPTY_SET);
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public void synchronizeInteractionShortLabel(IntactInteractionEvidence object) {
        IntactUtils.synchronizeInteractionEvidenceShortName(object, getIntactDao().getEntityManager(), Collections.EMPTY_SET);
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public <T extends IntactPrimaryObject> T cloneAnnotatedObject(T ao, EditorCloner cloner) {
        getIntactDao().getEntityManager().clear();
        T reloaded = ao;
        // merge current user because detached
        if (ao.getAc() != null){
            reloaded = (T)getIntactDao().getEntityManager().find(ao.getClass(), ao.getAc());
        }

        T clone = (T)cloner.clone(reloaded, getIntactDao());

        return clone;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public <T extends IntactPrimaryObject> void detachObject(T ao) {
        // detach current object if necessary
        if (ao.getAc() != null && getIntactDao().getEntityManager().contains(ao)){
            getIntactDao().getEntityManager().detach(ao);
        }
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public void claimOwnership(Releasable releasable, User user, boolean isAssigned) {
        if (releasable != null){
            detachObject((IntactPrimaryObject)releasable);

            lifecycleManager.getGlobalStatus().changeOwnership(releasable, user, null);

            // automatically set as curation in progress if no one was assigned before
            if (isAssigned) {
                lifecycleManager.getAssignedStatus().startCuration(releasable, user);
            }
        }
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public void markAsCurationInProgress(Releasable releasable, User user) {
        if (releasable != null){
            detachObject((IntactPrimaryObject)releasable);

            lifecycleManager.getAssignedStatus().startCuration(releasable, user);
        }
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public void markAsReadyForChecking(Releasable releasable, User user, String reasonForReadyForChecking) {
        if (releasable != null){
            detachObject((IntactPrimaryObject)releasable);

            lifecycleManager.getCurationInProgressStatus().readyForChecking(releasable, reasonForReadyForChecking, true, user);
        }
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public void revertReadyForChecking(Releasable releasable, User user) {
        if (releasable != null){
            detachObject((IntactPrimaryObject)releasable);

            lifecycleManager.getReadyForCheckingStatus().revert(releasable, user);
        }
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public void revertAccepted(Releasable releasable, User user, boolean isReadyForReleased) {
        if (releasable != null){
            detachObject((IntactPrimaryObject)releasable);

            if (isReadyForReleased){
                lifecycleManager.getReadyForReleaseStatus().revert(releasable, user);
            }
            else {
                LifeCycleEvent acceptedEvt = ReleasableUtils.getLastEventOfType(releasable, LifeCycleEventType.ACCEPTED);
                releasable.getLifecycleEvents().remove(acceptedEvt);
                releasable.setStatus(LifeCycleStatus.READY_FOR_CHECKING);
            }
        }
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public void putOnHold(Releasable releasable, User user, String reasonForOnHold, boolean isReadyForReleased, boolean isReleased) {
        if (releasable != null){
            detachObject((IntactPrimaryObject)releasable);

            if (isReadyForReleased) {
                lifecycleManager.getReadyForReleaseStatus().putOnHold(releasable, reasonForOnHold, user);
            } else if (isReleased) {
                lifecycleManager.getReleasedStatus().putOnHold(releasable, reasonForOnHold, user);
            }
        }
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public void readyForReleaseFromOnHold(Releasable releasable, User user) {
        if (releasable != null){
            detachObject((IntactPrimaryObject)releasable);

            lifecycleManager.getAcceptedOnHoldStatus().onHoldRemoved(releasable, null, user);
        }
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public void accept(Releasable releasable, User user, String message) {
        if (releasable != null){
            detachObject((IntactPrimaryObject)releasable);

            lifecycleManager.getReadyForCheckingStatus().accept(releasable, message, user);

            if (!releasable.isOnHold()) {
                lifecycleManager.getAcceptedStatus().readyForRelease(releasable, "Accepted and not on-hold", user);
            }
        }
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public void reject(Releasable releasable, User user, String message) {
        if (releasable != null){
            detachObject((IntactPrimaryObject)releasable);

            lifecycleManager.getReadyForCheckingStatus().reject(releasable, message, user);
        }
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public void markAsAssignedToMe(Releasable releasable, User user) {
        if (releasable != null){
            detachObject((IntactPrimaryObject)releasable);

            lifecycleManager.getNewStatus().assignToCurator(releasable, user, user);

            markAsCurationInProgress(releasable, user);
        }
    }
}
