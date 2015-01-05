package uk.ac.ebi.intact.service.complex.ws.utils;

import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.utils.AliasUtils;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import psidev.psi.mi.jami.utils.XrefUtils;
import uk.ac.ebi.intact.jami.model.extension.IntactComplex;
import uk.ac.ebi.intact.service.complex.ws.model.ComplexDetails;
import uk.ac.ebi.intact.service.complex.ws.model.ComplexDetailsCrossReferences;
import uk.ac.ebi.intact.service.complex.ws.model.ComplexDetailsFeatures;
import uk.ac.ebi.intact.service.complex.ws.model.ComplexDetailsParticipants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by maitesin on 09/12/2014.
 */
public class IntactComplexUtils {

    public static final String COMPLEX_PROPERTIES = "properties";
    public static final String COMPLEX_PROPERTIES_MI = "MI:0629";

    public static final String COMPLEX_DISEASE = "disease";
    public static final String COMPLEX_DISEASE_MI = "MI:0617";

    public static final String COMPLEX_LIGAND = "ligand";
    public static final String COMPLEX_LIGAND_IA = "IA:2738";

    public static final String COMPLEX_ASSEMBLY = "complex-assembly";
    public static final String COMPLEX_ASSEMBLY_IA = "IA:2783";

    public static final String CURATED_COMPLEX = "curated-complex";
    public static final String CURATED_COMPLEX_IA = "IA:0285";

    public static final String BIO_ROLE = "biological role";
    public static final String BIO_ROLE_MI = "MI:0500";

    public static final String INTACT = "intact";
    public static final String INTACT_MI = "MI:0469";

    public static final String SEARCH = "search-url";
    public static final String SEARCH_MI = "MI:0615";

    public static List<String> getComplexSynonyms(IntactComplex complex) {
        List<String> synosyms = new ArrayList<String>();
        for (Alias alias : AliasUtils.collectAllAliasesHavingType(complex.getAliases(), Alias.COMPLEX_SYNONYM_MI, Alias.COMPLEX_SYNONYM)) {
            synosyms.add(alias.getName());
        }
        return synosyms;
    }

    public static String getComplexName(IntactComplex complex){
        String name = complex.getRecommendedName();
        if (name != null) return name;
        name = complex.getSystematicName();
        if (name != null) return name;
        List<String> synonyms = getComplexSynonyms(complex);
        if (! synonyms.isEmpty()) return synonyms.get(0);
        return complex.getShortName();
    }


    // This method fills the cross references table for the view
    public static void setCrossReferences(IntactComplex complex, ComplexDetails details) {
        Collection<ComplexDetailsCrossReferences> crossReferences = details.getCrossReferences();
        ComplexDetailsCrossReferences cross;
        //TODO check why wwpdb and Reactome cross references are not shown here
        for (Xref xref : complex.getXrefs()) {
            cross = new ComplexDetailsCrossReferences();
            if (xref.getDatabase() != null) {
                cross.setDatabase(xref.getDatabase().getFullName());
//            TODO check how I have to get the definition from an annotation
//            cross.setDbdefinition(xref.getDatabase().getAnnotations());
                cross.setDbMI(xref.getDatabase().getMIIdentifier());
            }
            if (xref.getQualifier() != null) {
                cross.setQualifier(xref.getQualifier().getFullName());
//            TODO check how I have to get the definition from an annotation
//            cross.setQualifierDefinition(xref.getQualifier().getAnnotations());
                cross.setQualifierMI(xref.getQualifier().getMIIdentifier());
            }
            cross.setIdentifier(xref.getId());
            Annotation searchUrl = AnnotationUtils.collectFirstAnnotationWithTopic(xref.getDatabase().getAnnotations(), SEARCH_MI, SEARCH);
            if (searchUrl != null) {
                //TODO check why IntAct has to search url
                cross.setSearchURL(searchUrl.getValue().replaceAll("\\$*\\{ac\\}",cross.getIdentifier()));
            }
            crossReferences.add(cross);
        }
    }

    // This method fills the participants table for the view
    public static void setParticipants(IntactComplex complex, ComplexDetails details) {
        Collection<ComplexDetailsParticipants> participants = details.getParticipants();
        ComplexDetailsParticipants part;
        for (Participant participant : complex.getParticipants()) {
            part = new ComplexDetailsParticipants();
            Interactor interactor = participant.getInteractor();
            if (interactor != null) {
                part.setInteractorAC(XrefUtils.collectFirstIdentifierWithDatabase(interactor.getIdentifiers(), INTACT_MI, INTACT).getId());
                setInteractorType(part, interactor);
                part.setDescription(interactor.getFullName());
                if (interactor instanceof Protein) {
                    Alias alias = AliasUtils.collectFirstAliasWithType(interactor.getAliases(), Alias.COMPLEX_SYNONYM_MI, Alias.COMPLEX_SYNONYM);
                    part.setName(alias != null ? alias.getName() : ((Protein) interactor).getGeneName());
                    part.setIdentifier(interactor.getPreferredIdentifier().getId());
                }
                else if (interactor instanceof BioactiveEntity) {
                    part.setName(interactor.getShortName());
                    part.setIdentifier(((BioactiveEntity) interactor).getChebi());
                }
                else {
                    part.setName(interactor.getShortName());
                    part.setIdentifier(interactor.getFullName());
                }
                //TODO Identifier Link
//                part.setIdentifier(xref.getPrimaryId());
//                for ( Annotation annotation : xref.getCvDatabase().getAnnotations() ) {
//                    if ( annotation.getCvTopic() != null && CvTopic.SEARCH_URL_MI_REF.equals(annotation.getCvTopic().getIdentifier()) ) {
//                        part.setIdentifierLink(annotation.getAnnotationText().replaceAll("\\$*\\{ac\\}", xref.getPrimaryId()));
//                    }
//                }
                part.setStochiometry(participant.getStoichiometry().toString());
                if (participant.getBiologicalRole() != null) {
                    setBiologicalRole(part, participant);
                }
            }
            setFeatures(part, participant);
            participants.add(part);
        }
    }

    // this method fills the linked features and the other features cells in the participants table
    protected static void setFeatures(ComplexDetailsParticipants part, Participant participant) {
        for (Feature feature : (List<Feature>) participant.getFeatures()) {
            for (Feature linked : (List<Feature>) feature.getLinkedFeatures()) {
                ComplexDetailsFeatures complexDetailsFeatures = new ComplexDetailsFeatures();
                part.getLinkedFeatures().add(complexDetailsFeatures);
                complexDetailsFeatures.setFeatureType(linked.getType().getShortName());
                //TODO check how I have to get the definition from an annotation
                //complexDetailsFeatures.setFeatureTypeDefinition(linked.getType().getAnnotations().toString());
                complexDetailsFeatures.setFeatureTypeMI(linked.getType().getMIIdentifier());
                complexDetailsFeatures.setParticipantId(linked.getParticipant().getInteractor().getPreferredIdentifier().getId());
                for (Range range : (List<Range>) linked.getRanges()) {
                    complexDetailsFeatures.getRanges().add(range.getStart().getStart() + ".." + range.getStart().getEnd() + " - " + range.getEnd().getStart() + ".." + range.getEnd().getEnd());
                }
            }
            //TODO Other features
            //What about other features?
        }
    }

    // This method sets the interactor type information
    protected static void setInteractorType(ComplexDetailsParticipants part, Interactor interactor) {
        CvTerm term = interactor.getInteractorType();
        part.setInteractorType(term.getFullName());
        part.setInteractorTypeMI(term.getMIIdentifier());
        //TODO check how I have to get the definition from an annotation
        Annotation annotation = AnnotationUtils.collectFirstAnnotationWithTopic(term.getAnnotations(), Annotation.COMPLEX_PROPERTIES, Annotation.COMPLEX_PROPERTIES_MI);
        if (annotation != null) {
            part.setInteractorTypeDefinition(annotation.getValue());
        }
    }

    // This method sets the biological role information
    protected static void setBiologicalRole(ComplexDetailsParticipants part, Participant participant) {
        CvTerm term = participant.getBiologicalRole();
        part.setBioRole(term.getFullName());
        part.setBioRoleMI(term.getMIIdentifier());
        //TODO check how I have to get the definition from an annotation
        Annotation annotation = AnnotationUtils.collectFirstAnnotationWithTopic(term.getAnnotations(), BIO_ROLE_MI, BIO_ROLE);
        if (annotation != null) {
            part.setBioRoleDefinition(annotation.getValue());
        }
    }

    //
    // ALIASES
    //
    public static String getSystematicName(IntactComplex complex) {
        return complex.getSystematicName();
    }

    //Retrieve all the synosyms of the complex
    public static List<String> getSynonyms(IntactComplex complex) {
        List<String> synosyms = new ArrayList<String>();
        for (Alias alias : AliasUtils.collectAllAliasesHavingType(complex.getAliases(), Alias.COMPLEX_SYNONYM_MI, Alias.COMPLEX_SYNONYM)) {
            synosyms.add(alias.getName());
        }
        return synosyms;
    }

    public static String getName(IntactComplex complex) {
        String name = complex.getRecommendedName();
        if (name != null) return name;
        name = complex.getSystematicName();
        if (name != null) return name;
        List<String> synonyms = getSynonyms(complex);
        if (synonyms != Collections.EMPTY_LIST) return synonyms.get(0);
        return complex.getShortName();
    }

    //
    // SPECIES
    //
    //
    public static String getSpeciesName(IntactComplex complex) {
        return complex.getOrganism().getScientificName();
    }

    public static String getSpeciesTaxId(IntactComplex complex) {
        return Integer.toString(complex.getOrganism().getTaxId());
    }

    //
    // ANNOTATIONS
    //
    public static String getProperties(IntactComplex complex) {
        Annotation annotation = AnnotationUtils.collectFirstAnnotationWithTopic(complex.getAnnotations(), COMPLEX_PROPERTIES_MI, COMPLEX_PROPERTIES);
        if (annotation != null)
            return annotation.getValue();
        else
            return null;
    }

    public static String getDisease(IntactComplex complex) {
        Annotation annotation = AnnotationUtils.collectFirstAnnotationWithTopic(complex.getAnnotations(), COMPLEX_DISEASE_MI, COMPLEX_DISEASE);
        if (annotation != null)
            return annotation.getValue();
        else
            return null;
    }

    public static String getLigand(IntactComplex complex) {
        Annotation annotation = AnnotationUtils.collectFirstAnnotationWithTopic(complex.getAnnotations(), COMPLEX_LIGAND_IA, COMPLEX_LIGAND);
        if (annotation != null)
            return annotation.getValue();
        else
            return null;
    }

    public static String getComplexAssembly(IntactComplex complex) {
        Annotation annotation = AnnotationUtils.collectFirstAnnotationWithTopic(complex.getAnnotations(), COMPLEX_ASSEMBLY_IA, COMPLEX_ASSEMBLY);
        if (annotation != null)
            return annotation.getValue();
        else
            return null;
    }

    public static String getFunction(IntactComplex complex) {
        Annotation annotation = AnnotationUtils.collectFirstAnnotationWithTopic(complex.getAnnotations(), CURATED_COMPLEX_IA, CURATED_COMPLEX);
        if (annotation != null)
            return annotation.getValue();
        else
            return null;
    }

}
