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

import org.apache.commons.lang.StringUtils;
import psidev.psi.mi.jami.exception.IllegalRangeException;
import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.utils.RangeUtils;
import uk.ac.ebi.intact.editor.controller.curate.AnnotatedObjectController;
import uk.ac.ebi.intact.editor.services.curate.cvobject.CvObjectService;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.faces.application.FacesMessage;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.validator.ValidatorException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class RangeWrapper {

    private AbstractIntactRange range;
    private String sequence;
    private String rangeAsString;

    private CvObjectService cvObjectService;
    private Class<? extends AbstractIntactResultingSequence> resultingSequenceClass;
    private Class<? extends AbstractIntactXref> resSequenceXrefClass;

    private boolean isInvalid = false;
    private String badRangeInfo = null;

    private CvTerm newDatabase;
    private String newXrefId;
    private String newSecondaryId;
    private String newXrefVersion;
    private CvTerm newQualifier;
    private AnnotatedObjectController featureController;

    public RangeWrapper(AbstractIntactRange range, String sequence, CvObjectService cvService,
                        Class<? extends AbstractIntactResultingSequence> resultingSequenceClass,
                        Class<? extends AbstractIntactXref> resSequenceXrefClass,
                        AnnotatedObjectController featureController) {
        this.range = range;
        this.rangeAsString = RangeUtils.convertRangeToString(range);
        if (range.getParticipant() != null){
            if (range.getParticipant().getInteractor() instanceof Polymer){
                 this.sequence = ((Polymer)range.getParticipant().getInteractor()).getSequence();
            }
            else{
                this.sequence = null;
            }
        }
        else{
            this.sequence = sequence;
        }

        this.cvObjectService = cvService;
        this.resultingSequenceClass = resultingSequenceClass;
        this.resSequenceXrefClass = resSequenceXrefClass;

        List<String> messages = RangeUtils.validateRange(this.range, this.sequence);
        isInvalid = !messages.isEmpty();
        if (isInvalid){
            this.badRangeInfo = StringUtils.join(messages, ", ");
        }
        this.featureController = featureController;
    }

    public void onRangeAsStringChanged(AjaxBehaviorEvent evt) throws NoSuchMethodException,InstantiationException, IllegalAccessException,InvocationTargetException {
        Range newRange = null;
        try {
            newRange = RangeUtils.createRangeFromString(rangeAsString, false);
            IntactPosition start = new IntactPosition(cvObjectService.findCvObject(IntactUtils.RANGE_STATUS_OBJCLASS, newRange.getStart().getStatus().getMIIdentifier()),
                    newRange.getStart().getStart(), newRange.getStart().getEnd());
            IntactPosition end = new IntactPosition(cvObjectService.findCvObject(IntactUtils.RANGE_STATUS_OBJCLASS, newRange.getEnd().getStatus().getMIIdentifier()),
                    newRange.getEnd().getStart(), newRange.getEnd().getEnd());

            this.range.setPositions(start, end);
            if (this.range.getResultingSequence() != null){
                this.range.getResultingSequence().setOriginalSequence(RangeUtils.extractRangeSequence(this.range, this.sequence));
            }
            else{
                this.range.setResultingSequence(this.resultingSequenceClass.getConstructor(String.class, String.class).newInstance(RangeUtils.extractRangeSequence(this.range, this.sequence),null));
            }

            List<String> messages = RangeUtils.validateRange(this.range, this.sequence);
            isInvalid = !messages.isEmpty();
            if (isInvalid){
                this.badRangeInfo = StringUtils.join(messages, ", ");
            }
        } catch (IllegalRangeException e) {
            String problemMsg =e.getMessage();
            featureController.addErrorMessage("Range is not valid: "+rangeAsString, problemMsg);
            return;
        }
    }

    public void onFuzzyTypeChanged(AjaxBehaviorEvent evt) {

        this.rangeAsString = RangeUtils.convertRangeToString(range);
        List<String> messages = RangeUtils.validateRange(this.range, this.sequence);
        isInvalid = !messages.isEmpty();
        if (isInvalid){
            this.badRangeInfo = StringUtils.join(messages, ", ");
        }
    }

    public void validateRange(FacesContext context, UIComponent component, Object value) throws ValidatorException {

        String rangeAsStr = (String) value;
        try {
            psidev.psi.mi.jami.model.Range newRange = RangeUtils.createRangeFromString(rangeAsStr, false);
            List<String> messages = RangeUtils.validateRange(newRange, sequence);
            if (!messages.isEmpty()) {
                EditableValueHolder valueHolder = (EditableValueHolder) component;
                valueHolder.setValid(false);

                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid range: "+ StringUtils.join(messages, ", "), "Range syntax is invalid: "+rangeAsStr);
                throw new ValidatorException(message);
            }
        } catch (IllegalRangeException e) {
            EditableValueHolder valueHolder = (EditableValueHolder) component;
            valueHolder.setValid(false);

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid range: "+e.getMessage(), "Range syntax is invalid: "+rangeAsStr);
            throw new ValidatorException(message);
        }
    }

    public AbstractIntactRange getRange() {
        return range;
    }

    public void setRange(AbstractIntactRange range) {
        this.range = range;
    }

    public String getRangeAsString() {
        return rangeAsString;
    }

    public void setRangeAsString(String rangeAsString) {
        this.rangeAsString = rangeAsString;
    }

    public boolean isValidRange() {
        return !isInvalid;
    }

    public String getBadRangeInfo() {
        return badRangeInfo;
    }

    public Entity getParticipant(){
        return this.range.getParticipant();
    }

    public void setParticipant(Entity entity) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        this.range.setParticipant(entity);

        if (entity != null && entity.getInteractor() instanceof Polymer){
            sequence = ((Polymer)entity.getInteractor()).getSequence();
        }
        else{
            sequence = null;
        }

        if (this.range.getResultingSequence() != null){
            this.range.getResultingSequence().setOriginalSequence(RangeUtils.extractRangeSequence(this.range, this.sequence));
        }
        else{
            this.range.setResultingSequence(this.resultingSequenceClass.getConstructor(String.class, String.class).newInstance(RangeUtils.extractRangeSequence(this.range, this.sequence),null));
        }
    }

    public void newXref(ActionEvent evt) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        range.getResultingSequence().getXrefs().add(this.resSequenceXrefClass.getConstructor(CvTerm.class, String.class)
                .newInstance(IntactUtils.createMIDatabase("to set", null), "to set"));
        if (this.newDatabase != null && this.newXrefId != null){
            AbstractIntactXref newRef = this.resSequenceXrefClass.getConstructor(CvTerm.class, String.class)
                    .newInstance(this.newDatabase, this.newXrefId);
            newRef.setSecondaryId(this.newSecondaryId);
            newRef.setVersion(this.newXrefVersion);
            newRef.setQualifier(this.newQualifier);
            // add xref to object
            range.getResultingSequence().getXrefs().add(newRef);
            // save
            featureController.doSave(false);

            this.newDatabase = null;
            this.newXrefId = null;
            this.newXrefVersion = null;
            this.newQualifier = null;
            this.newSecondaryId = null;
        }
        else{
            featureController.addErrorMessage("Cannot add new xref as the database and/or primary identifier is(are) missing","No database/primary identifier provided");
        }
    }

    public void removeXref(Xref xref) {

        this.range.getResultingSequence().getXrefs().remove(xref);
    }

    public List<Xref> collectXrefs() {

        List<Xref> xrefs = new ArrayList<Xref>(this.range.getResultingSequence().getXrefs());
        Collections.sort(xrefs, new AnnotatedObjectController.AuditableComparator());
        return xrefs;
    }

    public boolean isXrefsTableEnabled(){
        return !range.getResultingSequence().getXrefs().isEmpty();
    }

    public CvTerm getNewDatabase() {
        return newDatabase;
    }

    public void setNewDatabase(CvTerm newDatabase) {
        this.newDatabase = newDatabase;
    }

    public String getNewXrefId() {
        return newXrefId;
    }

    public void setNewXrefId(String newXrefId) {
        this.newXrefId = newXrefId;
    }

    public String getNewSecondaryId() {
        return newSecondaryId;
    }

    public void setNewSecondaryId(String newSecondaryId) {
        this.newSecondaryId = newSecondaryId;
    }

    public String getNewXrefVersion() {
        return newXrefVersion;
    }

    public void setNewXrefVersion(String newXrefVersion) {
        this.newXrefVersion = newXrefVersion;
    }

    public CvTerm getNewQualifier() {
        return newQualifier;
    }

    public void setNewQualifier(CvTerm newQualifier) {
        this.newQualifier = newQualifier;
    }
}
