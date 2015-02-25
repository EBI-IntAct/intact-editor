package uk.ac.ebi.intact.editor.services.summary;

import java.io.Serializable;

/**
 * Class that summarizes a complex for the dashboard
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>15/12/14</pre>
 */

public class OrganismSummary implements Serializable{
    private String ac;
    private String commonName;
    private String scientificName;
    private int taxId;
    private int numberParticipants;
    private int numberExperiments;
    private int numberComplexes;
    private int numberMolecules;

    public String getAc() {
        return ac;
    }

    public void setAc(String ac) {
        this.ac = ac;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getScientificName() {
        return scientificName;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    public int getTaxId() {
        return taxId;
    }

    public void setTaxId(int taxId) {
        this.taxId = taxId;
    }

    public int getNumberParticipants() {
        return numberParticipants;
    }

    public void setNumberParticipants(int numberParticipants) {
        this.numberParticipants = numberParticipants;
    }

    public int getNumberExperiments() {
        return numberExperiments;
    }

    public void setNumberExperiments(int numberExperiments) {
        this.numberExperiments = numberExperiments;
    }

    public int getNumberComplexes() {
        return numberComplexes;
    }

    public void setNumberComplexes(int numberComplexes) {
        this.numberComplexes = numberComplexes;
    }

    public int getNumberMolecules() {
        return numberMolecules;
    }

    public void setNumberMolecules(int numberMolecules) {
        this.numberMolecules = numberMolecules;
    }
}
