package uk.ac.ebi.intact.service.complex.ws.utils;

import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.utils.AliasUtils;
import psidev.psi.mi.jami.utils.AnnotationUtils;
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

    public static final String FEATURE_TYPE = "feature type";
    public static final String FEATURE_TYPE_MI = "MI:0116";

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
        for (Xref xref : complex.getXrefs()) {
            cross = new ComplexDetailsCrossReferences();
            for (Xref id : xref.getDatabase().getIdentifiers()) {
                System.out.println("ID = " + id);
            }
//            CvDatabase cvDatabase = xref.getCvDatabase();
//            CvXrefQualifier cvXrefQualifier = xref.getCvXrefQualifier();
//            String primaryId = xref.getPrimaryId();
//            String secondayId = xref.getSecondaryId();
//            cross.setIdentifier(primaryId);
//            cross.setDescription(secondayId);
//            cross.setDatabase(cvDatabase.getFullName() != null ? cvDatabase.getFullName() : cvDatabase.getShortLabel());
//            cross.setDbMI(cvDatabase.getIdentifier());
//            for ( Annotation annotation : cvDatabase.getAnnotations() ) {
//                if ( annotation.getCvTopic() != null && CvTopic.SEARCH_URL_MI_REF.equals(annotation.getCvTopic().getIdentifier()) ) {
//                    cross.setSearchURL(annotation.getAnnotationText().replaceAll("\\$*\\{ac\\}",primaryId));
//                }
//                else if( annotation.getCvTopic().getShortLabel().equalsIgnoreCase(CvTopic.DEFINITION) ){
//                    cross.setDbdefinition(annotation.getAnnotationText());
//                }
//            }
//            if( cvXrefQualifier != null ) {
//                setXrefQualifier(cross, cvXrefQualifier);
//            }
            crossReferences.add(cross);
        }
    }

    // This method fills the participants table for the view
    public static void setParticipants(IntactComplex complex, ComplexDetails details) {
//        Collection<ComplexDetailsParticipants> participants = details.getParticipants();
//        ComplexDetailsParticipants part;
//        for ( Component component : complex.getComponents() ) {
//            part = new ComplexDetailsParticipants();
//            Interactor interactor = component.getInteractor();
//            if ( interactor != null ) {
//                part.setInteractorAC(interactor.getAc());
//                part.setDescription(interactor.getFullName());
//                Xref xref = null;
//                if (CvObjectUtils.isProteinType(interactor.getCvInteractorType())) {
//                    xref = ProteinUtils.getUniprotXref(interactor);
//
//                    String geneName = null;
//                    for (Alias alias : interactor.getAliases()) {
//                        if ( alias.getCvAliasType() != null && CvAliasType.GENE_NAME_MI_REF.equals(alias.getCvAliasType().getIdentifier())) {
//                            geneName = alias.getName();
//                            break;
//                        }
//                    }
//                    part.setName(geneName !=null ? geneName : interactor.getShortLabel());
//
//                }
//                else if( CvObjectUtils.isSmallMoleculeType(interactor.getCvInteractorType()) || CvObjectUtils.isPolysaccharideType(interactor.getCvInteractorType()) ){
//                    xref = SmallMoleculeUtils.getChebiXref(interactor);
//                    part.setName(interactor.getShortLabel());
//                }
//                else {
//                    part.setName(interactor.getShortLabel());
//                    xref = XrefUtils.getIdentityXref(interactor, CvDatabase.ENSEMBL_MI_REF);
//                    xref = xref != null ? xref : XrefUtils.getIdentityXref(interactor, "MI:1013");
//                }
//                if (xref != null) {
//                    part.setIdentifier(xref.getPrimaryId());
//                    for ( Annotation annotation : xref.getCvDatabase().getAnnotations() ) {
//                        if ( annotation.getCvTopic() != null && CvTopic.SEARCH_URL_MI_REF.equals(annotation.getCvTopic().getIdentifier()) ) {
//                            part.setIdentifierLink(annotation.getAnnotationText().replaceAll("\\$*\\{ac\\}", xref.getPrimaryId()));
//                        }
//                    }
//                }
//                setInteractorType(part, interactor.getCvInteractorType());
//            }
//            part.setStochiometry(component.getStoichiometry() == 0.0f ? null : Float.toString(component.getStoichiometry()));
//            if (component.getCvBiologicalRole() != null) {
//                setBiologicalRole(part, component.getCvBiologicalRole());
//            }
//
//            setFeatures(part, component);
//
//            participants.add(part);
//        }
    }

    // this method fills the linked features and the other features cells in the participants table
    protected static void setFeatures(ComplexDetailsParticipants part, Interactor interactor) {
//        for( Feature feature : component.getFeatures() ) {
//            ComplexDetailsFeatures complexDetailsFeatures = new ComplexDetailsFeatures();
//            if ( feature.getBoundDomain() != null ) {
//                part.getLinkedFeatures().add(complexDetailsFeatures);
//                Component featureComponent = feature.getBoundDomain().getComponent();
//                if (featureComponent != null) {
//                    Interactor linkedInteractor = featureComponent.getInteractor();
//                    if ( linkedInteractor != null ) {
//                        Xref xref = null;
//                        if (CvObjectUtils.isProteinType(linkedInteractor.getCvInteractorType())) {
//                            xref = ProteinUtils.getUniprotXref(linkedInteractor);
//                        }
//                        else if( CvObjectUtils.isSmallMoleculeType(linkedInteractor.getCvInteractorType()) || CvObjectUtils.isPolysaccharideType(linkedInteractor.getCvInteractorType()) ){
//                            xref = SmallMoleculeUtils.getChebiXref(linkedInteractor);
//                        }
//                        else {
//                            xref = XrefUtils.getIdentityXref(linkedInteractor, CvDatabase.ENSEMBL_MI_REF);
//                            xref = xref != null ? xref : XrefUtils.getIdentityXref(linkedInteractor, "MI:1013");
//                        }
//                        if (xref != null) {
//                            complexDetailsFeatures.setParticipantId(xref.getPrimaryId());
//                        }
//                    }
//                }
//            }
//            else {
//                part.getOtherFeatures().add(complexDetailsFeatures);
//            }
//            if (feature.getCvFeatureType() != null) {
//                setFeatureType(complexDetailsFeatures, feature.getCvFeatureType(), component);
//            }
//            for ( Range range : feature.getRanges() ) {
//                complexDetailsFeatures.getRanges().add(FeatureUtils.convertRangeIntoString(range));
//            }
//        }
    }


    // This method is a generic method to get the annotations of a CvDagObject
//    protected static String getAnnotation(CvDagObject cv) {
//        if (cv != null){
//            for ( Annotation annotation : cv.getAnnotations() ) {
//                if( annotation.getCvTopic().getShortLabel().equalsIgnoreCase(CvTopic.DEFINITION) ){
//                    return annotation.getAnnotationText();
//                }
//            }
//        }
//        return null;
//    }

    // This method sets the interactor type information
    protected static void setInteractorType(ComplexDetailsParticipants part, Interactor interactor) {
        CvTerm term = interactor.getInteractorType();
        part.setInteractorType(term.getFullName());
        part.setInteractorTypeMI(term.getMIIdentifier());
        Annotation annotation = AnnotationUtils.collectFirstAnnotationWithTopic(term.getAnnotations(), Annotation.COMPLEX_PROPERTIES, Annotation.COMPLEX_PROPERTIES_MI);
        if (annotation != null) {
            part.setInteractorTypeDefinition(annotation.getValue());
        }
    }

    // This method sets the biological role information
    protected static void setBiologicalRole(ComplexDetailsParticipants part, Interactor interactor) {
//        part.setBioRole(cvBiologicalRole.getFullName() != null ? cvBiologicalRole.getFullName() : cvBiologicalRole.getShortLabel());
//        part.setBioRoleMI(cvBiologicalRole.getIdentifier());
//        String annotation = getAnnotation(cvBiologicalRole);
//        if (annotation != null) {
//            part.setBioRoleDefinition(annotation);
//        }
        CvTerm term = interactor.getInteractorType();
        part.setBioRole(term.getFullName());
        part.setBioRoleMI(term.getMIIdentifier());
        Annotation annotation = AnnotationUtils.collectFirstAnnotationWithTopic(term.getAnnotations(), BIO_ROLE_MI, BIO_ROLE);
        if (annotation != null) {
            part.setBioRoleDefinition(annotation.getValue());
        }
    }

    // This method sets the feature type information
    protected static void setFeatureType(ComplexDetailsFeatures complexDetailsFeatures, Interactor interactor) {
//        complexDetailsFeatures.setFeatureType(feature.getFullName() != null ? feature.getFullName() : feature.getShortLabel());
//        complexDetailsFeatures.setFeatureTypeMI(feature.getIdentifier());
//        String annotation = getAnnotation(component.getCvBiologicalRole());
//        if (annotation != null) {
//            complexDetailsFeatures.setFeatureTypeDefinition(annotation);
//        }
        CvTerm term = interactor.getInteractorType();
        complexDetailsFeatures.setFeatureType(term.getFullName());
        complexDetailsFeatures.setFeatureTypeMI(term.getMIIdentifier());
        Annotation annotation = AnnotationUtils.collectFirstAnnotationWithTopic(term.getAnnotations(), FEATURE_TYPE_MI, FEATURE_TYPE);
        if (annotation != null) {
            complexDetailsFeatures.setFeatureTypeDefinition(annotation.getValue());
        }
    }

    // This method sets the xref qualifier information
    protected static void setXrefQualifier(ComplexDetailsCrossReferences cross, Xref xref) {
        cross.setQualifier(xref.getQualifier().getFullName() != null ? xref.getQualifier().getFullName() : xref.getQualifier().getShortName());
        cross.setQualifierMI(xref.getQualifier().getMIIdentifier());
//        cross.setQualifier(cvXrefQualifier.getFullName() != null ? cvXrefQualifier.getFullName() : cvXrefQualifier.getShortLabel());
//        cross.setQualifierMI(cvXrefQualifier.getIdentifier());
//        String annotation = getAnnotation(cvXrefQualifier);
//        if (annotation != null) {
//            cross.setQualifierDefinition(annotation);
//        }
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
