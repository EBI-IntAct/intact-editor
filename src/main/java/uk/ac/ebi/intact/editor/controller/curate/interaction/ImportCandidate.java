package uk.ac.ebi.intact.editor.controller.curate.interaction;

import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.utils.comparator.interactor.UnambiguousExactInteractorComparator;
import uk.ac.ebi.intact.jami.model.extension.IntactInteractor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class ImportCandidate {

    private boolean selected = true;
    private String query;
    private String organism;
    private List<String> primaryAcs;
    private List<String> secondaryAcs;
    private String source;
    private IntactInteractor interactor;
    private Protein uniprotProtein;
    private List<Feature> clonedFeatures=new ArrayList<Feature>();

    public ImportCandidate(String query, IntactInteractor interactor) {
        this.query = query;
        this.interactor = interactor;

        if (interactor.getOrganism() != null) {
            Organism biosource = interactor.getOrganism();

            if (biosource.getScientificName() != null){
                this.organism = biosource.getScientificName();
            }
            else {
                this.organism = biosource.getCommonName();
            }
        }
    }

    public ImportCandidate(String query, Protein uniprotProteinLike) {
        this.query = query;
        this.uniprotProtein = uniprotProteinLike;

        primaryAcs = new ArrayList<String>(1);

        primaryAcs.add(uniprotProtein.getUniprotkb());
        secondaryAcs = new ArrayList<String>(uniprotProtein.getIdentifiers().size());
        for (Xref ref : uniprotProtein.getIdentifiers()){
            if (!ref.getId().equals(uniprotProtein.getUniprotkb())){
                secondaryAcs.add(ref.getId());
            }
        }
        organism = uniprotProtein.getOrganism().getCommonName();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public IntactInteractor getInteractor() {
        return interactor;
    }

    public void setInteractor(IntactInteractor interactor) {
        this.interactor = interactor;
    }

    public List<String> getPrimaryAcs() {
        return primaryAcs;
    }

    public void setPrimaryAcs(List<String> primaryAcs) {
        this.primaryAcs = primaryAcs;
    }

    public List<String> getSecondaryAcs() {
        return secondaryAcs;
    }

    public void setSecondaryAcs(List<String> secondaryAcs) {
        this.secondaryAcs = secondaryAcs;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getOrganism() {
        return organism;
    }

    public void setOrganism(String organism) {
        this.organism = organism;
    }

    public Protein getUniprotProtein() {
        return uniprotProtein;
    }

    public void setUniprotProtein(Protein uniprotProtein) {
        this.uniprotProtein = uniprotProtein;
    }

    public boolean isIsoform() {
        return uniprotProtein != null && (uniprotProtein.getUniprotkb().contains("-"));
    }

    public boolean isChain() {
        return uniprotProtein != null && (uniprotProtein.getUniprotkb().contains("-PRO"));
    }

    public List<Feature> getClonedFeatures() {
        return clonedFeatures;
    }

    @Override
    public int hashCode() {
        int hashcode = 31;

        if (interactor != null){
            hashcode = 31*hashcode + interactor.hashCode();
        }

        return hashcode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }

        if (!(o instanceof Interactor)){
            return false;
        }

        return UnambiguousExactInteractorComparator.areEquals(interactor, (Interactor) o);
    }
}
