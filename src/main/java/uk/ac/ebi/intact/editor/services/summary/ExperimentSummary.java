package uk.ac.ebi.intact.editor.services.summary;

import uk.ac.ebi.intact.jami.model.extension.IntactExperiment;

import java.io.Serializable;

/**
 * Summary classes for viewing properties of an experiment
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>15/12/14</pre>
 */

public class ExperimentSummary implements Serializable{

    private String ac;
    private String rowStyleClass;
    private String caution;
    private String internalRemark;
    private String shortLabel;
    private String publicationAc;
    private String pubmedId;
    private String interactionDetectionMethod;
    private String participantIdentificationMethod;
    private String hostOrganism;
    private int interactionNumber;
    private IntactExperiment experiment;

    public String getAc() {
        return ac;
    }

    public void setAc(String ac) {
        this.ac = ac;
    }

    public String getRowStyleClass() {
        return rowStyleClass;
    }

    public void setRowStyleClass(String rowStyleClass) {
        this.rowStyleClass = rowStyleClass;
    }

    public String getCaution() {
        return caution;
    }

    public void setCaution(String caution) {
        this.caution = caution;
    }

    public String getInternalRemark() {
        return internalRemark;
    }

    public void setInternalRemark(String internalRemark) {
        this.internalRemark = internalRemark;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public void setShortLabel(String shortLabel) {
        this.shortLabel = shortLabel;
    }

    public String getPublicationAc() {
        return publicationAc;
    }

    public void setPublicationAc(String publicationAc) {
        this.publicationAc = publicationAc;
    }

    public String getPubmedId() {
        return pubmedId;
    }

    public void setPubmedId(String pubmedId) {
        this.pubmedId = pubmedId;
    }

    public String getInteractionDetectionMethod() {
        return interactionDetectionMethod;
    }

    public void setInteractionDetectionMethod(String interactionDetectionMethod) {
        this.interactionDetectionMethod = interactionDetectionMethod;
    }

    public String getParticipantIdentificationMethod() {
        return participantIdentificationMethod;
    }

    public void setParticipantIdentificationMethod(String participantIdentificationMethod) {
        this.participantIdentificationMethod = participantIdentificationMethod;
    }

    public String getHostOrganism() {
        return hostOrganism;
    }

    public void setHostOrganism(String hostOrganism) {
        this.hostOrganism = hostOrganism;
    }

    public int getInteractionNumber() {
        return interactionNumber;
    }

    public void setInteractionNumber(int interactionNumber) {
        this.interactionNumber = interactionNumber;
    }

    public IntactExperiment getExperiment() {
        return experiment;
    }

    public void setExperiment(IntactExperiment experiment) {
        this.experiment = experiment;
    }
}
