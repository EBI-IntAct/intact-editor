package uk.ac.ebi.intact.editor.services.summary;

import uk.ac.ebi.intact.jami.model.extension.IntactInteractor;

import java.io.Serializable;

/**
 * Class that summarizes a complex for the dashboard
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>15/12/14</pre>
 */

public class MoleculeSummary implements Serializable{
    private String ac;
    private String caution;
    private String internalRemark;
    private boolean isNoUniprotUpdate=false;
    private String interactorType;
    private String organism;
    private String shortName;
    private String fullName;
    private String identityXref;
    private int numberInteractions;
    private int numberComplexes;
    private int numberMoleculeSets;

    private IntactInteractor molecule;

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

    public boolean isNoUniprotUpdate() {
        return isNoUniprotUpdate;
    }

    public void setNoUniprotUpdate(boolean isNoUniprotUpdate) {
        this.isNoUniprotUpdate = isNoUniprotUpdate;
    }

    public String getInteractorType() {
        return interactorType;
    }

    public void setInteractorType(String interactorType) {
        this.interactorType = interactorType;
    }

    public String getOrganism() {
        return organism;
    }

    public void setOrganism(String organism) {
        this.organism = organism;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getIdentityXref() {
        return identityXref;
    }

    public void setIdentityXref(String identityXref) {
        this.identityXref = identityXref;
    }

    public int getNumberInteractions() {
        return numberInteractions;
    }

    public void setNumberInteractions(int numberInteractions) {
        this.numberInteractions = numberInteractions;
    }

    public int getNumberComplexes() {
        return numberComplexes;
    }

    public void setNumberComplexes(int numberComplexes) {
        this.numberComplexes = numberComplexes;
    }

    public IntactInteractor getMolecule() {
        return molecule;
    }

    public void setMolecule(IntactInteractor molecule) {
        this.molecule = molecule;
    }

    public int getNumberMoleculeSets() {
        return numberMoleculeSets;
    }

    public void setNumberMoleculeSets(int numberMoleculeSets) {
        this.numberMoleculeSets = numberMoleculeSets;
    }
}
