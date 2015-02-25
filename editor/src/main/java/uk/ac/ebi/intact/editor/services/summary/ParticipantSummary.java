package uk.ac.ebi.intact.editor.services.summary;

import java.io.Serializable;

/**
 * Summary for a participant
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>16/12/14</pre>
 */

public class ParticipantSummary implements Serializable{

    private String ac;
    private String caution;
    private String internalRemark;
    private String interactorShortName;
    private boolean isNoUniprotUpdate;
    private String identityXref;
    private String expressedInOrganism;
    private String experimentalRole;
    private String biologicalRole;
    private int featuresNumber;
    private int minStoichiometry;
    private int maxStoichiometry;

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

    public String getInteractorShortName() {
        return interactorShortName;
    }

    public void setInteractorShortName(String interactorShortName) {
        this.interactorShortName = interactorShortName;
    }

    public boolean isNoUniprotUpdate() {
        return isNoUniprotUpdate;
    }

    public void setNoUniprotUpdate(boolean isNoUniprotUpdate) {
        this.isNoUniprotUpdate = isNoUniprotUpdate;
    }

    public String getIdentityXref() {
        return identityXref;
    }

    public void setIdentityXref(String identityXref) {
        this.identityXref = identityXref;
    }

    public String getExpressedInOrganism() {
        return expressedInOrganism;
    }

    public void setExpressedInOrganism(String expressedInOrganism) {
        this.expressedInOrganism = expressedInOrganism;
    }

    public String getExperimentalRole() {
        return experimentalRole;
    }

    public void setExperimentalRole(String experimentalRole) {
        this.experimentalRole = experimentalRole;
    }

    public String getBiologicalRole() {
        return biologicalRole;
    }

    public void setBiologicalRole(String biologicalRole) {
        this.biologicalRole = biologicalRole;
    }

    public int getFeaturesNumber() {
        return featuresNumber;
    }

    public void setFeaturesNumber(int featuresNumber) {
        this.featuresNumber = featuresNumber;
    }

    public int getMinStoichiometry() {
        return minStoichiometry;
    }

    public void setMinStoichiometry(int minStoichiometry) {
        this.minStoichiometry = minStoichiometry;
    }

    public int getMaxStoichiometry() {
        return maxStoichiometry;
    }

    public void setMaxStoichiometry(int maxStoichiometry) {
        this.maxStoichiometry = maxStoichiometry;
    }
}
