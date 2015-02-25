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
package uk.ac.ebi.intact.editor.controller.curate.feature;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.primefaces.event.TabChangeEvent;
import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import psidev.psi.mi.jami.utils.RangeUtils;
import psidev.psi.mi.jami.utils.XrefUtils;
import uk.ac.ebi.intact.editor.controller.curate.AnnotatedObjectController;
import uk.ac.ebi.intact.editor.controller.curate.UnsavedChange;
import uk.ac.ebi.intact.editor.services.curate.feature.FeatureEditorService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.IntactPrimaryObject;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.synchronizer.IntactDbSynchronizer;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.annotation.Resource;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.model.SelectItem;
import javax.faces.validator.ValidatorException;
import java.util.*;

/**
 * Abstract Feature controller.
 */
public abstract class AbstractFeatureController<T extends AbstractIntactFeature> extends AnnotatedObjectController {

    private static final Log log = LogFactory.getLog( AbstractFeatureController.class );

    private T feature;
    private List<RangeWrapper> rangeWrappers;
    private boolean containsInvalidRanges;

    /**
     * The AC of the feature to be loaded.
     */
    private String ac;

    private String newRangeValue;

    private boolean isRangeDisabled;
    private List<SelectItem> participantSelectItems;
    private boolean isComplexFeature=false;
    private boolean isRangeSequenceDisabled =false;

    @Resource(name = "featureEditorService")
    private transient FeatureEditorService featureEditorService;

    private Map<String, AbstractIntactParticipant> participantsMap;

    public AbstractFeatureController() {
        participantsMap = new HashMap<String, AbstractIntactParticipant>();
    }

    public abstract Class<T> getFeatureClass();

    public abstract Class<? extends AbstractIntactResultingSequence> getResultingSequenceClass();

    public abstract Class<? extends AbstractIntactXref> getResultingSequenceXrefClass();

    @Override
    public IntactPrimaryObject getAnnotatedObject() {
        return getFeature();
    }

    @Override
    public void setAnnotatedObject(IntactPrimaryObject annotatedObject) {
         setFeature((T)annotatedObject);
    }

    @Override
    protected void loadCautionMessages() {
        if (this.feature != null){
            Annotation caution = AnnotationUtils.collectFirstAnnotationWithTopic(this.feature.getAnnotations(), Annotation.CAUTION_MI, Annotation.CAUTION);
            setCautionMessage(caution != null ? caution.getValue() : null);
            Annotation internal = AnnotationUtils.collectFirstAnnotationWithTopic(this.feature.getAnnotations(), null, "remark-internal");
            setInternalRemark(internal != null ? internal.getValue() : null);
        }
    }

    public void loadData( ComponentSystemEvent event ) {
        if (!FacesContext.getCurrentInstance().isPostback()) {

            if ( ac != null ) {
                if ( feature == null || !ac.equals( feature.getAc() ) ) {
                    setFeature(getFeatureEditorService().loadFeatureByAc(ac, getFeatureClass()));
                }
            } else {
                if ( feature != null ) ac = feature.getAc();
            }

            if (feature == null) {
                super.addErrorMessage("Feature does not exist", ac);
                return;
            }

            refreshParentControllers();
            refreshTabs();
        }

        if (containsInvalidRanges) {
            addWarningMessage("This feature contains invalid ranges", "Ranges must be fixed before being able to save");
        }

        generalLoadChecks();
    }

    protected abstract void refreshParentControllers();

    public void refreshRangeWrappers() {
        String sequence = getSequence();
        this.rangeWrappers = new ArrayList<RangeWrapper>(this.feature.getRanges().size());
        for (Object r : this.feature.getRanges()){
            this.rangeWrappers.add(new RangeWrapper((AbstractIntactRange)r, sequence, getCvService(), getResultingSequenceClass(),
                    getResultingSequenceXrefClass(), this));
        }

        containsInvalidRanges = false;

        for (RangeWrapper range : this.rangeWrappers) {

            if (!containsInvalidRanges && !range.isValidRange()) {
                containsInvalidRanges = true;
            }
        }
    }

    public Map<String, AbstractIntactParticipant> getParticipantsMap() {
        return participantsMap;
    }

    protected void refreshParticipantSelectItems() {
        participantSelectItems = new ArrayList<SelectItem>();
        participantSelectItems.add(new SelectItem(null, "select participant", "select participant", false, false, true));
        participantsMap.clear();
        if (isComplexFeature && this.feature.getParticipant() != null){
            Entity participant = this.feature.getParticipant();
            if (isComplexFeature){
                loadParticipants((Complex)participant.getInteractor(), this.participantSelectItems);
            }
        }
    }

    private void loadParticipants(Complex parent, List<SelectItem> selectItems){
        for (ModelledParticipant child : parent.getParticipants()){
            IntactModelledParticipant part = (IntactModelledParticipant)child;
            if (part.getInteractor() instanceof Complex){
                loadParticipants((Complex)part.getInteractor(), selectItems);
            }
            else{
                SelectItem item = new SelectItem( part, part.getInteractor().getShortName()+"("+part.getAc()+")", part.getInteractor().getFullName()+", Complex "+part.getInteraction().getShortName());
                selectItems.add(item);
                participantsMap.put(part.getAc(), part);
            }
        }
    }
    public boolean isComplexFeature(){
        return this.isComplexFeature;
    }

    public String newFeature(Participant participant) throws InstantiationException, IllegalAccessException{
        T feature = getFeatureClass().newInstance();
        feature.setShortName(null);
        feature.setType(null);
        feature.setParticipant(participant);

        setFeature(feature);
        changed();
        return navigateToObject(feature);
    }

    public void newRange(ActionEvent evt) {
        if (newRangeValue == null || newRangeValue.isEmpty()) {
            addErrorMessage("Range value field is empty", "Please provide a range value before clicking on the New Range button");
            return;
        }

        newRangeValue = newRangeValue.trim();

        if (!newRangeValue.contains("-")) {
            addErrorMessage("Illegal range value", "The range must contain a hyphen");
            return;
        }

        String sequence = getSequence();
        try{
            Range newRange = RangeUtils.createRangeFromString(newRangeValue, false);

            IntactPosition start = new IntactPosition(getCvService().findCvObject(IntactUtils.RANGE_STATUS_OBJCLASS, newRange.getStart().getStatus().getMIIdentifier()),
                    newRange.getStart().getStart(), newRange.getStart().getEnd());
            IntactPosition end = new IntactPosition(getCvService().findCvObject(IntactUtils.RANGE_STATUS_OBJCLASS, newRange.getEnd().getStatus().getMIIdentifier()),
                    newRange.getEnd().getStart(), newRange.getEnd().getEnd());

            AbstractIntactRange intactRange = instantiateRange(start, end);
            intactRange.setResultingSequence(instantiateResultingSequence(RangeUtils.extractRangeSequence(intactRange, sequence), null));

            feature.getRanges().add(intactRange);
            newRangeValue = null;
            doSave(false);
        }
        catch (Throwable e){
            String problemMsg =e.getMessage();
            addErrorMessage("Range is not valid: "+newRangeValue, problemMsg);
            log.error(problemMsg, e);
            return;
        }
    }

    protected abstract <I extends AbstractIntactRange> I instantiateRange(Position start, Position end);

    protected abstract <I extends AbstractIntactResultingSequence> I instantiateResultingSequence(String original, String newSequence);

    public void validateFeature(FacesContext context, UIComponent component, Object value) throws ValidatorException {

        if (rangeWrappers.isEmpty()) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Feature without ranges", "One range is mandatory");
            throw new ValidatorException(message);
        }
    }

    private String getSequence() {
        Interactor interactor = feature.getParticipant() != null ? feature.getParticipant().getInteractor():null;

        String sequence = null;

        if (interactor instanceof Polymer) {
            Polymer polymer = (Polymer) interactor;
            sequence = polymer.getSequence();
        }
        return sequence;
    }

    public void markRangeToDelete(RangeWrapper range) {
        if (range == null) return;

        if (range.getRange().getAc() == null) {
            feature.getRanges().remove(range.getRange());
            rangeWrappers.remove(range);
        } else {
            Collection<String> parents = collectParentAcsOfCurrentAnnotatedObject();
            parents.add(feature.getAc());
            getChangesController().markToDelete(range.getRange(), this.feature, getRangeSynchronzer(), "Range: "+RangeUtils.convertRangeToString(range.getRange()), parents);
        }
    }

    @Override
    public String getObjectName() {
        return this.feature != null ? this.feature.getShortName() : null;
    }

    public List<RangeWrapper> getWrappedRanges() {
        return rangeWrappers;
    }

    public String getAc() {
        return ac;
    }

    @Override
    public int getXrefsSize() {
        if (feature == null){
            return 0;
        }
        else {
            return feature.getDbXrefs().size();
        }
    }

    @Override
    public int getAliasesSize() {
        if (feature == null){
            return 0;
        }
        else {
            return feature.getAliases().size();
        }
    }

    @Override
    public int getAnnotationsSize() {
        if (feature == null){
            return 0;
        }
        else {
            return feature.getAnnotations().size();
        }
    }

    public void setAc( String ac ) {
        this.ac = ac;
    }

    public T getFeature() {
        return feature;
    }

    @Override
    public void refreshTabs(){
        super.refreshTabs();

        this.isRangeDisabled = false;
    }

    public void setFeature( T feature ) {
        this.feature = feature;

        if (feature != null){
            this.ac = feature.getAc();

            initialiseDefaultProperties(this.feature);
        }
        else{
            this.ac = null;
        }
    }

    public String getNewRangeValue() {
        return newRangeValue;
    }

    public void setNewRangeValue(String newRangeValue) {
        this.newRangeValue = newRangeValue;
    }

    public boolean isContainsInvalidRanges() {
        return containsInvalidRanges;
    }

    public void setContainsInvalidRanges(boolean containsInvalidRanges) {
        this.containsInvalidRanges = containsInvalidRanges;
    }

    @Override
    public Class<? extends IntactPrimaryObject> getAnnotatedObjectClass() {
        return getFeatureClass();
    }

    @Override
    public boolean isAliasNotEditable(Alias alias) {
        return false;
    }

    @Override
    public boolean isAnnotationNotEditable(Annotation annot) {
        return false;
    }

    public boolean isRangeDisabled() {
        return isRangeDisabled;
    }

    public void setRangeDisabled(boolean rangeDisabled) {
        isRangeDisabled = rangeDisabled;
    }

    public void onTabChanged(TabChangeEvent e) {

        // the xref tab is active
        super.onTabChanged(e);

        // all the tabs selectOneMenu are disabled, we can process the tabs specific to interaction
        if (isAliasDisabled() && isXrefDisabled() && isAnnotationTopicDisabled()){
            if (e.getTab().getId().equals("rangesTab")){
                isRangeDisabled = false;
                isRangeSequenceDisabled = true;

            }
            else if (e.getTab().getId().equals("sequencesTab")){
                isRangeDisabled = true;
                isRangeSequenceDisabled = false;
            }
            else {
                isRangeDisabled = true;
                isRangeSequenceDisabled = true;
            }
        }
        else {
            isRangeDisabled = true;
            isRangeSequenceDisabled = true;
        }
    }

    @Override
    protected void initialiseDefaultProperties(IntactPrimaryObject annotatedObject) {
        T feature = (T) annotatedObject;
        if (!getFeatureEditorService().isFeatureFullyLoaded(feature)) {
            this.feature = getFeatureEditorService().reloadFullyInitialisedFeature(feature, newClonerInstance());
        }

        if (feature.getParticipant() != null && feature.getParticipant().getInteractor() instanceof Complex){
            isComplexFeature = true;
        }
        else{
            isComplexFeature = false;
            this.participantSelectItems=Collections.EMPTY_LIST;
        }

        refreshParticipantSelectItems();
        refreshRangeWrappers();

        setDescription("Feature: "+feature.getShortName());
    }

    public int getFeatureRangeSize() {
        if (feature == null){
            return 0;
        }
        else{
            return this.feature.getRanges().size();
        }
    }

    public List<Annotation> collectAnnotations() {
        List<Annotation> annotations = new ArrayList<Annotation>(feature.getAnnotations());
        Collections.sort(annotations, new AuditableComparator());
        // annotations are always initialised
        return annotations;
    }

    @Override
    public void removeAlias(Alias alias) {

        this.feature.getAliases().remove(alias);
    }

    @Override
    protected void addNewAlias(AbstractIntactAlias newAlias) {
        this.feature.getAliases().add(newAlias);
    }

    @Override
    protected void addNewAnnotation(AbstractIntactAnnotation newAnnot) {
        this.feature.getAnnotations().add(newAnnot);
    }

    public List<Alias> collectAliases() {

        List<Alias> aliases = new ArrayList<Alias>(this.feature.getAliases());
        Collections.sort(aliases, new AuditableComparator());
        return aliases;
    }

    public List<Xref> collectXrefs() {

        List<Xref> xrefs = new ArrayList<Xref>(this.feature.getDbXrefs());
        Collections.sort(xrefs, new AuditableComparator());
        return xrefs;
    }

    @Override
    public void removeXref(Xref xref) {

        if (!this.feature.getIdentifiers().remove(xref)){
            this.feature.getXrefs().remove(xref);
        }
    }

    @Override
    public void removeAnnotation(Annotation annotation) {
        feature.getAnnotations().remove(annotation);
    }

    public FeatureEditorService getFeatureEditorService() {
        if (this.featureEditorService == null){
            this.featureEditorService = ApplicationContextProvider.getBean("featureEditorService");
        }
        return featureEditorService;
    }

    public List<SelectItem> getParticipantSelectItems() {
        return participantSelectItems;
    }


    @Override
    protected void postProcessDeletedEvent(UnsavedChange unsaved) {
        super.postProcessDeletedEvent(unsaved);
        super.postProcessDeletedEvent(unsaved);
        if (unsaved.getUnsavedObject() instanceof AbstractIntactRange){
            Iterator<Range> rangeIterator = feature.getRanges().iterator();
            while (rangeIterator.hasNext()){
                AbstractIntactRange intactEv = (AbstractIntactRange)rangeIterator.next();
                if (intactEv.getAc() == null && unsaved.getUnsavedObject() == intactEv){
                    rangeIterator.remove();
                }
                else if (intactEv.getAc() != null && intactEv.getAc().equals(unsaved.getUnsavedObject().getAc())){
                    rangeIterator.remove();
                }
            }

            refreshRangeWrappers();
        }
    }

    protected abstract IntactDbSynchronizer getRangeSynchronzer();


    @Override
    public boolean isXrefNotEditable(Xref ref) {
        return false;
    }

    @Override
    protected void addNewXref(AbstractIntactXref newRef) {
        if (XrefUtils.isXrefAnIdentifier(newRef) || XrefUtils.doesXrefHaveQualifier(newRef, null, "intact-secondary")){
            this.feature.getIdentifiers().add(newRef);
        }
        else{
            this.feature.getXrefs().add(newRef);
        }
    }

    public boolean isRangeSequenceDisabled() {
        return isRangeSequenceDisabled;
    }

    @Override
    public void doPreSave() {
        super.doPostSave();
    }
}