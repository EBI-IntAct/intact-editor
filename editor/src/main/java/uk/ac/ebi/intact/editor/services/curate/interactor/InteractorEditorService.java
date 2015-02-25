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
package uk.ac.ebi.intact.editor.services.curate.interactor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.Annotation;
import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.Interactor;
import psidev.psi.mi.jami.model.InteractorPool;
import uk.ac.ebi.intact.editor.controller.curate.cloner.InteractorCloner;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.jami.model.extension.IntactInteractor;
import uk.ac.ebi.intact.jami.model.extension.IntactInteractorPool;

import java.util.ArrayList;
import java.util.Collection;

/**
 */
@Service
public class InteractorEditorService extends AbstractEditorService {

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countAnnotations(IntactInteractor interactor) {
        return getIntactDao().getInteractorDao(IntactInteractor.class).countAnnotationsForInteractor(interactor.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countXrefs(IntactInteractor interactor) {
        return getIntactDao().getInteractorDao(IntactInteractor.class).countXrefsForInteractor(interactor.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countAliases(IntactInteractor interactor) {
        return getIntactDao().getInteractorDao(IntactInteractor.class).countAliasesForInteractor(interactor.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countPoolMembers(IntactInteractorPool pool) {
        return getIntactDao().getInteractorPoolDao().countMembersOfPool(pool.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public IntactInteractor loadInteractorByAc(String ac) {
        IntactInteractor interactor = getIntactDao().getEntityManager().find(IntactInteractor.class, ac);

        if (interactor != null){
            // initialise xrefs because needs id
            initialiseXrefs(interactor.getDbXrefs());
            // initialise annotations because needs caution
            initialiseAnnotations(interactor.getDbAnnotations());
            // initialise aliases
            initialiseAliases(interactor.getDbAliases());

            // load base types
            if (interactor.getInteractorType() != null){
                initialiseCv(interactor.getInteractorType());
            }

            // load set members
            if (interactor instanceof IntactInteractorPool){
                initialiseInteractorMembers((IntactInteractorPool) interactor);
            }
        }

        return interactor;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public IntactInteractor reloadFullyInitialisedInteractor(IntactInteractor interactor) {
        if (interactor == null){
            return null;
        }

        IntactInteractor reloaded = null;
        if (areAllInteractorCollectionsLazy(interactor)
                && interactor.getAc() != null
                && !getIntactDao().getEntityManager().contains(interactor)){
            reloaded = loadInteractorByAc(interactor.getAc());
        }

        // we need first to merge with reloaded complex
        if (reloaded != null){
            // detach reloaded now so not changes will be committed
            getIntactDao().getEntityManager().detach(reloaded);
            InteractorCloner cloner = new InteractorCloner();
            cloner.copyInitialisedProperties(interactor, reloaded);
            interactor = reloaded;
        }

        // initialise xrefs because needs id
        initialiseXrefs(interactor.getDbXrefs());
        // initialise annotations because needs caution
        initialiseAnnotations(interactor.getDbAnnotations());
        // initialise aliases
        initialiseAliases(interactor.getDbAliases());

        // load base types
        if (interactor.getInteractorType() != null && !isCvInitialised(interactor.getInteractorType())){
            CvTerm cv = initialiseCv(interactor.getInteractorType() );
            if (cv != interactor.getInteractorType() ){
                interactor.setInteractorType(cv);
            }
        }

        // load set members
        if (interactor instanceof IntactInteractorPool){
            initialiseInteractorMembers((IntactInteractorPool) interactor);
        }

        return interactor;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public Collection<Annotation> initialiseInteractorAnnotations(IntactInteractor releasable) {
        // reload complex without flushing changes
        IntactInteractor reloaded = releasable;
        // merge current user because detached
        if (releasable.getAc() != null && !getIntactDao().getEntityManager().contains(releasable)){
            reloaded = getIntactDao().getEntityManager().find(IntactInteractor.class, releasable.getAc());
            if (reloaded == null){
                reloaded = releasable;
            }
        }

        initialiseAnnotations(reloaded.getAnnotations());
        return reloaded.getAnnotations();
    }

    private boolean areAllInteractorCollectionsLazy(IntactInteractor interactor) {
        return !interactor.areAnnotationsInitialized()
                || !interactor.areAliasesInitialized()
                || !interactor.areXrefsInitialized()
                || (interactor instanceof IntactInteractorPool && !((IntactInteractorPool) interactor).areInteractorsInitialized());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public boolean isInteractorFullyLoaded(IntactInteractor interactor){
        if (interactor == null){
            return true;
        }
        if (!interactor.areAnnotationsInitialized()
                || !interactor.areXrefsInitialized()
                || !interactor.areAliasesInitialized()
                || !isCvInitialised(interactor.getInteractorType())
                || (interactor instanceof InteractorPool && !isInteractorSetInitialised((InteractorPool)interactor))){
            return false;
        }
        return true;
    }

    private boolean isInteractorSetInitialised(InteractorPool participant) {
        if (participant instanceof IntactInteractorPool
                && !((IntactInteractorPool) participant).areInteractorsInitialized()){
            return false;
        }
        else {
            for (Interactor f : participant){
                if (!isInteractorInitialised(f)){
                    return false;
                }
            }
        }
        return true;
    }

    private void initialiseInteractorMembers(Collection<Interactor> interactors) {
        Collection<Interactor> originalMembers = new ArrayList<Interactor>(interactors);
        InteractorCloner cloner = new InteractorCloner();
         for (Interactor interactor : originalMembers){
             if (!isInteractorInitialised(interactor)){
                 Interactor reloaded = initialiseInteractor(interactor, cloner);
                 if (reloaded != null){
                     interactors.remove(interactor);
                     interactors.add(reloaded);
                 }
             }
         }
    }

    @Override
    protected void initialiseOtherInteractorProperties(IntactInteractor inter) {
        // initialise aliases
        initialiseAliases(inter.getAliases());
    }

    protected boolean areInteractorCollectionsLazy(IntactInteractor inter) {
        return !inter.areAliasesInitialized() || !inter.areXrefsInitialized()
                || !inter.areAnnotationsInitialized();
    }

    protected boolean isInteractorInitialised(Interactor interactor) {
        if(interactor instanceof IntactInteractor){
            IntactInteractor intactInteractor = (IntactInteractor)interactor;
            if (!intactInteractor.areXrefsInitialized()
                    || !intactInteractor.areAnnotationsInitialized()
                    || !intactInteractor.areAliasesInitialized()){
                return false;
            }
        }
        return true;
    }
}
