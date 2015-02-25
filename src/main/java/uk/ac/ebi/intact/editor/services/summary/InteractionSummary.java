package uk.ac.ebi.intact.editor.services.summary;

import uk.ac.ebi.intact.jami.model.extension.IntactInteractionEvidence;

import java.io.Serializable;

/**
 * Class that summarizes a complex for the dashboard
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>15/12/14</pre>
 */

public class InteractionSummary implements Serializable{
    private String ac;
    private String caution;
    private String internalRemark;
    private String interactionType;
    private int numberParticipants;
    private String shortName;
    private String experimentLabel;
    private IntactInteractionEvidence interaction;

    public String getAc() {
        return ac;
    }

    public void setAc(String ac) {
        this.ac = ac;
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

    public String getInteractionType() {
        return interactionType;
    }

    public void setInteractionType(String interactionType) {
        this.interactionType = interactionType;
    }

    public int getNumberParticipants() {
        return numberParticipants;
    }

    public void setNumberParticipants(int numberParticipants) {
        this.numberParticipants = numberParticipants;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getExperimentLabel() {
        return experimentLabel;
    }

    public void setExperimentLabel(String experimentLabel) {
        this.experimentLabel = experimentLabel;
    }

    public IntactInteractionEvidence getInteraction() {
        return interaction;
    }

    public void setInteraction(IntactInteractionEvidence interaction) {
        this.interaction = interaction;
    }
}
