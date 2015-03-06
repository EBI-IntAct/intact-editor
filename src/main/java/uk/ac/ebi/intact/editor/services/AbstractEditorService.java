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
package uk.ac.ebi.intact.editor.services;

import org.apache.commons.collections.map.IdentityMap;
import psidev.psi.mi.jami.model.*;
import uk.ac.ebi.intact.jami.dao.IntactBaseDao;
import uk.ac.ebi.intact.jami.dao.IntactDao;
import uk.ac.ebi.intact.jami.interceptor.IntactTransactionSynchronization;
import uk.ac.ebi.intact.jami.model.audit.Auditable;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.IntactDbSynchronizer;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * IntAct editor abstract service class.
 *
 * @version $Id: AbstractEditorService.java 20630 2014-08-22 08:10:26Z mdumousseau@yahoo.com $
 */
public abstract class AbstractEditorService implements EditorService {

    @Resource(name = "intactDao")
    private IntactDao intactDao;

    @Resource(name = "intactTransactionSynchronization")
    private IntactTransactionSynchronization afterCommitExecutor;

    public IntactDao getIntactDao() {
        return intactDao;
    }

    public IntactTransactionSynchronization getAfterCommitExecutor() {
        return afterCommitExecutor;
    }

    protected void attachDaoToTransactionManager(){
        getAfterCommitExecutor().registerDaoForSynchronization(getIntactDao());
    }

    protected <T extends Auditable> void updateIntactObject(T intactObject, IntactBaseDao<T> dao) throws SynchronizerException,
            FinderException, PersisterException {
        // clear manager first to avaoid to have remaining objects from other transactions
        getIntactDao().getEntityManager().clear();

        try{
            dao.update(intactObject);
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

    protected <T extends Auditable> void persistIntactObject(T intactObject, IntactBaseDao<T> dao) throws SynchronizerException,
            FinderException, PersisterException {
        // clear manager first to avaoid to have remaining objects from other transactions
        getIntactDao().getEntityManager().clear();

        try{
            dao.persist(intactObject);
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

    protected <T extends Auditable> void deleteIntactObject(T intactObject, IntactBaseDao<T> dao) throws SynchronizerException,
            FinderException, PersisterException {
        // clear manager first to avaoid to have remaining objects from other transactions
        getIntactDao().getEntityManager().clear();

        try{
            dao.delete(intactObject);
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

    protected <T extends Auditable,I> T synchronizeIntactObject(I intactObject, IntactDbSynchronizer<I,T> synchronizer, boolean persist) throws SynchronizerException,
            FinderException, PersisterException {
        try{
            // clear manager first to avaoid to have remaining objects from other transactions
            getIntactDao().getEntityManager().clear();

            return synchronizer.synchronize(intactObject, persist);
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

    protected <T extends Auditable,I> T convertToPersistentIntactObject(I intactObject, IntactDbSynchronizer<I,T> synchronizer) throws SynchronizerException,
            FinderException, PersisterException {
        try{
            // clear manager first to avaoid to have remaining objects from other transactions
            getIntactDao().getEntityManager().clear();

            return synchronizer.convertToPersistentObject(intactObject);
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

    protected void initialiseParameters(Collection<? extends Parameter> parameters) {
        for (Parameter parameter : parameters){
            if (!isCvInitialised(parameter.getType())){
                CvTerm type = initialiseCv(parameter.getType());
                if (type != parameter.getType()){
                    ((AbstractIntactParameter)parameter).setType(type);
                }
            }

            if (parameter.getUnit() != null && !isCvInitialised(parameter.getUnit())){
                CvTerm unit = initialiseCv(parameter.getUnit());
                if (unit != parameter.getUnit()){
                    ((AbstractIntactParameter)parameter).setUnit(unit);
                }
            }
        }
    }

    protected void initialiseConfidence(Collection<? extends Confidence> confidences) {
        for (Confidence det : confidences){
            if (!isCvInitialised(det.getType())){
                CvTerm type = initialiseCv(det.getType());
                if (type != det.getType()){
                    ((AbstractIntactConfidence)det).setType(type);
                }
            }
        }
    }

    protected void initialiseAliases(Collection<Alias> aliases) {
        for (Alias alias : aliases){
            if (alias.getType() != null && !isCvInitialised(alias.getType())){
                CvTerm type = initialiseCv(alias.getType());
                if (type != alias.getType()){
                    ((AbstractIntactAlias)alias).setType(type);
                }
            }
        }
    }

    protected void initialiseRanges(Collection<Range> ranges) {
        List<Range> originalRanges = new ArrayList<Range>(ranges);
        for (Range r : originalRanges){
            if (!isRangeInitialised(r)){
                Range reloaded = initialiseRange(r);
                if (reloaded != r){
                    ranges.remove(r);
                    ranges.add(reloaded);
                }
            }
        }
    }

    protected Range initialiseRange(Range range) {
        if (range instanceof AbstractIntactRange){
            if (range.getResultingSequence() instanceof AbstractIntactResultingSequence
                    && !((AbstractIntactResultingSequence) range.getResultingSequence()).areXrefsInitialized()
                    && ((AbstractIntactRange) range).getAc() != null
                    && !getIntactDao().getEntityManager().contains(range)){
                range = getIntactDao().getEntityManager().find(range.getClass(), ((AbstractIntactRange)range).getAc());
            }
            initialisePosition(range.getStart());
            initialisePosition(range.getEnd());
            if (range.getResultingSequence() != null){
                initialiseXrefs(range.getResultingSequence().getXrefs());
            }
        }
        return range;
    }

    protected boolean isRangeInitialised(Range range) {
        if (range instanceof AbstractIntactRange){
            AbstractIntactRange r = (AbstractIntactRange)range;
            if (r.getResultingSequence() != null
                    && r.getResultingSequence() instanceof AbstractIntactResultingSequence
                    && !((AbstractIntactResultingSequence)r.getResultingSequence()).areXrefsInitialized()){
                return false;
            }
        }
        if (!isPositionInitialised(range.getStart()) || !isPositionInitialised(range.getEnd())){
            return false;
        }
        return true;
    }

    protected void initialisePosition(Position pos) {
        if (pos != null && !isCvInitialised(pos.getStatus())){
            CvTerm reloaded = initialiseCv(pos.getStatus());
            if (reloaded != pos.getStatus()){
                ((IntactPosition)pos).setStatus(reloaded);
            }
        }
    }

    protected boolean isPositionInitialised(Position pos) {
        return isCvInitialised(pos.getStatus());
    }

    protected void initialiseXrefs(Collection<Xref> xrefs) {
        Map<CvTerm, CvTerm> cvMap = new IdentityMap();
        for (Xref ref : xrefs){
            if (!isCvInitialised(ref.getDatabase())){
                CvTerm db = initialiseCvWithCache(ref.getDatabase(), cvMap);
                if (db != ref.getDatabase()){
                    ((AbstractIntactXref)ref).setDatabase(db);
                }
            }
            if (ref.getQualifier() != null && !isCvInitialised(ref.getQualifier())){
                CvTerm qual = initialiseCvWithCache(ref.getQualifier(), cvMap);
                if (qual != ref.getQualifier()){
                    ((AbstractIntactXref)ref).setQualifier(qual);
                }
            }
        }
    }

    protected void initialiseAnnotations(Collection<Annotation> annotations) {
        Map<CvTerm, CvTerm> cvMap = new IdentityMap();
        for (Annotation annot : annotations){
            if (!isCvInitialised(annot.getTopic())){
                CvTerm type = initialiseCvWithCache(annot.getTopic(), cvMap);
                if (type != annot.getTopic()){
                    ((AbstractIntactAnnotation)annot).setTopic(type);
                }
            }
        }
    }

    protected void initialiseXrefs(Collection<Xref> xrefs, Map<CvTerm, CvTerm> cvMap) {
        for (Xref ref : xrefs){
            if (!isCvInitialised(ref.getDatabase())){
                CvTerm db = initialiseCvWithCache(ref.getDatabase(), cvMap);
                if (db != ref.getDatabase()){
                    ((AbstractIntactXref)ref).setDatabase(db);
                }
            }
            if (ref.getQualifier() != null && !isCvInitialised(ref.getQualifier())){
                CvTerm qual = initialiseCvWithCache(ref.getQualifier(), cvMap);
                if (qual != ref.getQualifier()){
                    ((AbstractIntactXref)ref).setQualifier(qual);
                }
            }
        }
    }

    protected void initialiseAnnotations(Collection<Annotation> annotations, Map<CvTerm, CvTerm> cvMap) {
        for (Annotation annot : annotations){
            if (!isCvInitialised(annot.getTopic())){
                CvTerm type = initialiseCvWithCache(annot.getTopic(), cvMap);
                if (type != annot.getTopic()){
                    ((AbstractIntactAnnotation)annot).setTopic(type);
                }
            }
        }
    }

    protected CvTerm initialiseCv(CvTerm cv) {
        if (cv instanceof IntactCvTerm){
            IntactCvTerm intactCv = (IntactCvTerm)cv;
            if ((!intactCv.areXrefsInitialized() || !intactCv.areAnnotationsInitialized())
                    && ((IntactCvTerm) cv).getAc() != null
                    && !getIntactDao().getEntityManager().contains(cv)){
                cv = getIntactDao().getEntityManager().find(IntactCvTerm.class, ((IntactCvTerm)cv).getAc());
            }
            initialiseAnnotations(((IntactCvTerm) cv).getDbAnnotations());
            initialiseXrefs(((IntactCvTerm)cv).getDbXrefs());
        }
        return cv;
    }

    protected boolean isCvInitialised(CvTerm cv) {
        if (cv instanceof IntactCvTerm){
            IntactCvTerm intactCv = (IntactCvTerm)cv;
            return intactCv.areXrefsInitialized() && intactCv.areAnnotationsInitialized();
        }
        return true;
    }

    protected CvTerm initialiseCvWithCache(CvTerm cv, Map<CvTerm,CvTerm> cvMap) {
        if (cvMap.containsKey(cv)){
            return cvMap.get(cv);
        }
        else{
            cvMap.put(cv,cv);
        }

        if (cv != null){
            if (cv instanceof IntactCvTerm
                    && ((IntactCvTerm)cv).getAc() != null && !getIntactDao().getEntityManager().contains(cv)){
                cv = getIntactDao().getEntityManager().find(IntactCvTerm.class, ((IntactCvTerm)cv).getAc());
                if (cvMap.containsKey(cv)){
                    return cvMap.get(cv);
                }
                else{
                    cvMap.put(cv,cv);
                }
            }
            initialiseAnnotations(((IntactCvTerm) cv).getDbAnnotations(), cvMap);
            initialiseXrefs(((IntactCvTerm)cv).getDbXrefs(), cvMap);
        }
        return cv;
    }

    protected Interactor initialiseInteractor(Interactor inter, uk.ac.ebi.intact.editor.controller.curate.cloner.InteractorCloner interactorCloner) {
        if (inter instanceof IntactInteractor){
            if (areInteractorCollectionsLazy((IntactInteractor) inter)
                    && ((IntactInteractor)inter).getAc() != null && !getIntactDao().getEntityManager().contains(inter)){
                IntactInteractor reloaded = getIntactDao().getEntityManager().find(IntactInteractor.class, ((IntactInteractor)inter).getAc());
                if (reloaded != null){
                    // initialise freshly loaded properties
                    initialiseAnnotations(reloaded.getDbAnnotations());
                    initialiseXrefs(reloaded.getDbXrefs());
                    initialiseOtherInteractorProperties(reloaded);
                    if (reloaded instanceof Polymer){
                        ((Polymer)reloaded).getSequence().length();
                    }
                    // detach object so no changes will be flushed
                    getIntactDao().getEntityManager().detach(reloaded);
                    interactorCloner.copyInitialisedProperties((IntactInteractor) inter, reloaded);
                    // will return reloaded object
                    inter = reloaded;
                }
            }
            initialiseAnnotations(((IntactInteractor) inter).getDbAnnotations());
            initialiseXrefs(((IntactInteractor)inter).getDbXrefs());
            initialiseOtherInteractorProperties((IntactInteractor)inter);
            if (inter instanceof Polymer && ((Polymer)inter).getSequence() != null){
                ((Polymer)inter).getSequence().length();
            }
        }
        return inter;
    }

    protected void initialiseOtherInteractorProperties(IntactInteractor inter){

    }

    protected boolean areInteractorCollectionsLazy(IntactInteractor inter) {
        return !inter.areXrefsInitialized()
                || !inter.areAnnotationsInitialized();
    }

    protected boolean isInteractorInitialised(Interactor interactor) {
        if(interactor instanceof IntactInteractor){
            IntactInteractor intactInteractor = (IntactInteractor)interactor;
            if (!intactInteractor.areXrefsInitialized()
                    || !intactInteractor.areAnnotationsInitialized()){
                return false;
            }
        }
        return true;
    }
}