package uk.ac.ebi.intact.service.complex.ws.utils;

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
    public static List<String> getComplexSynonyms(IntactComplex complex) {
        List<String> synosyms = new ArrayList<String>();
        for (Alias alias : complex.getAliases()) {
            if (alias.getName() != null && alias.getCvAliasType() != null && alias.getCvAliasType().getIdentifier().equals(CvAliasType.COMPLEX_SYNONYM_NAME_MI_REF)) {
                synosyms.add(alias.getName());
            }
        }
        return synosyms;
    }

    public static String getComplexName(IntactComplex complex){
        String name = uk.ac.ebi.intact.model.util.ComplexUtils.getRecommendedName(complex);
        if (name != null) return name;
        name = uk.ac.ebi.intact.model.util.ComplexUtils.getSystematicName(complex);
        if (name != null) return name;
        List<String> synonyms = getComplexSynonyms(complex);
        if (! synonyms.isEmpty()) return synonyms.get(0);
        name = uk.ac.ebi.intact.model.util.ComplexUtils.getFirstAlias(complex);
        if (name != null) return name;
        return complex.getShortLabel();
    }


    // This method fills the cross references table for the view
    public static void setCrossReferences(IntactComplex complex, ComplexDetails details) {
        Collection<ComplexDetailsCrossReferences> crossReferences = details.getCrossReferences();
        ComplexDetailsCrossReferences cross;
        for ( Xref xref : complex.getXrefs()) {
            cross = new ComplexDetailsCrossReferences();
            CvDatabase cvDatabase = xref.getCvDatabase();
            CvXrefQualifier cvXrefQualifier = xref.getCvXrefQualifier();
            String primaryId = xref.getPrimaryId();
            String secondayId = xref.getSecondaryId();
            cross.setIdentifier(primaryId);
            cross.setDescription(secondayId);
            cross.setDatabase(cvDatabase.getFullName() != null ? cvDatabase.getFullName() : cvDatabase.getShortLabel());
            cross.setDbMI(cvDatabase.getIdentifier());
            for ( Annotation annotation : cvDatabase.getAnnotations() ) {
                if ( annotation.getCvTopic() != null && CvTopic.SEARCH_URL_MI_REF.equals(annotation.getCvTopic().getIdentifier()) ) {
                    cross.setSearchURL(annotation.getAnnotationText().replaceAll("\\$*\\{ac\\}",primaryId));
                }
                else if( annotation.getCvTopic().getShortLabel().equalsIgnoreCase(CvTopic.DEFINITION) ){
                    cross.setDbdefinition(annotation.getAnnotationText());
                }
            }
            if( cvXrefQualifier != null ) {
                setXrefQualifier(cross, cvXrefQualifier);
            }
            crossReferences.add(cross);
        }
    }

    // This method fills the participants table for the view
    public static void setParticipants(IntactComplex complex, ComplexDetails details) {
        Collection<ComplexDetailsParticipants> participants = details.getParticipants();
        ComplexDetailsParticipants part;
        for ( Component component : complex.getComponents() ) {
            part = new ComplexDetailsParticipants();
            Interactor interactor = component.getInteractor();
            if ( interactor != null ) {
                part.setInteractorAC(interactor.getAc());
                part.setDescription(interactor.getFullName());
                Xref xref = null;
                if (CvObjectUtils.isProteinType(interactor.getCvInteractorType())) {
                    xref = ProteinUtils.getUniprotXref(interactor);

                    String geneName = null;
                    for (Alias alias : interactor.getAliases()) {
                        if ( alias.getCvAliasType() != null && CvAliasType.GENE_NAME_MI_REF.equals(alias.getCvAliasType().getIdentifier())) {
                            geneName = alias.getName();
                            break;
                        }
                    }
                    part.setName(geneName !=null ? geneName : interactor.getShortLabel());

                }
                else if( CvObjectUtils.isSmallMoleculeType(interactor.getCvInteractorType()) || CvObjectUtils.isPolysaccharideType(interactor.getCvInteractorType()) ){
                    xref = SmallMoleculeUtils.getChebiXref(interactor);
                    part.setName(interactor.getShortLabel());
                }
                else {
                    part.setName(interactor.getShortLabel());
                    xref = XrefUtils.getIdentityXref(interactor, CvDatabase.ENSEMBL_MI_REF);
                    xref = xref != null ? xref : XrefUtils.getIdentityXref(interactor, "MI:1013");
                }
                if (xref != null) {
                    part.setIdentifier(xref.getPrimaryId());
                    for ( Annotation annotation : xref.getCvDatabase().getAnnotations() ) {
                        if ( annotation.getCvTopic() != null && CvTopic.SEARCH_URL_MI_REF.equals(annotation.getCvTopic().getIdentifier()) ) {
                            part.setIdentifierLink(annotation.getAnnotationText().replaceAll("\\$*\\{ac\\}", xref.getPrimaryId()));
                        }
                    }
                }
                setInteractorType(part, interactor.getCvInteractorType());
            }
            part.setStochiometry(component.getStoichiometry() == 0.0f ? null : Float.toString(component.getStoichiometry()));
            if (component.getCvBiologicalRole() != null) {
                setBiologicalRole(part, component.getCvBiologicalRole());
            }

            setFeatures(part, component);

            participants.add(part);
        }
    }

    // this method fills the linked features and the other features cells in the participants table
    protected static void setFeatures(ComplexDetailsParticipants part, Component component) {
        for( Feature feature : component.getFeatures() ) {
            ComplexDetailsFeatures complexDetailsFeatures = new ComplexDetailsFeatures();
            if ( feature.getBoundDomain() != null ) {
                part.getLinkedFeatures().add(complexDetailsFeatures);
                Component featureComponent = feature.getBoundDomain().getComponent();
                if (featureComponent != null) {
                    Interactor linkedInteractor = featureComponent.getInteractor();
                    if ( linkedInteractor != null ) {
                        Xref xref = null;
                        if (CvObjectUtils.isProteinType(linkedInteractor.getCvInteractorType())) {
                            xref = ProteinUtils.getUniprotXref(linkedInteractor);
                        }
                        else if( CvObjectUtils.isSmallMoleculeType(linkedInteractor.getCvInteractorType()) || CvObjectUtils.isPolysaccharideType(linkedInteractor.getCvInteractorType()) ){
                            xref = SmallMoleculeUtils.getChebiXref(linkedInteractor);
                        }
                        else {
                            xref = XrefUtils.getIdentityXref(linkedInteractor, CvDatabase.ENSEMBL_MI_REF);
                            xref = xref != null ? xref : XrefUtils.getIdentityXref(linkedInteractor, "MI:1013");
                        }
                        if (xref != null) {
                            complexDetailsFeatures.setParticipantId(xref.getPrimaryId());
                        }
                    }
                }
            }
            else {
                part.getOtherFeatures().add(complexDetailsFeatures);
            }
            if (feature.getCvFeatureType() != null) {
                setFeatureType(complexDetailsFeatures, feature.getCvFeatureType(), component);
            }
            for ( Range range : feature.getRanges() ) {
                complexDetailsFeatures.getRanges().add(FeatureUtils.convertRangeIntoString(range));
            }
        }
    }


    // This method is a generic method to get the annotations of a CvDagObject
    protected static String getAnnotation(CvDagObject cv) {
        if (cv != null){
            for ( Annotation annotation : cv.getAnnotations() ) {
                if( annotation.getCvTopic().getShortLabel().equalsIgnoreCase(CvTopic.DEFINITION) ){
                    return annotation.getAnnotationText();
                }
            }
        }
        return null;
    }

    // This method sets the interactor type information
    protected static void setInteractorType(ComplexDetailsParticipants part, CvInteractorType cvInteractorType) {
        part.setInteractorType(cvInteractorType.getFullName() != null ? cvInteractorType.getFullName() : cvInteractorType.getShortLabel());
        part.setInteractorTypeMI(cvInteractorType.getIdentifier());
        String annotation = getAnnotation(cvInteractorType);
        if (annotation != null) {
            part.setInteractorTypeDefinition(annotation);
        }
    }

    // This method sets the biological role information
    protected static void setBiologicalRole(ComplexDetailsParticipants part, CvBiologicalRole cvBiologicalRole) {
        part.setBioRole(cvBiologicalRole.getFullName() != null ? cvBiologicalRole.getFullName() : cvBiologicalRole.getShortLabel());
        part.setBioRoleMI(cvBiologicalRole.getIdentifier());
        String annotation = getAnnotation(cvBiologicalRole);
        if (annotation != null) {
            part.setBioRoleDefinition(annotation);
        }
    }

    // This method sets the feature type information
    protected static void setFeatureType(ComplexDetailsFeatures complexDetailsFeatures, CvFeatureType feature, Component component) {
        complexDetailsFeatures.setFeatureType(feature.getFullName() != null ? feature.getFullName() : feature.getShortLabel());
        complexDetailsFeatures.setFeatureTypeMI(feature.getIdentifier());
        String annotation = getAnnotation(component.getCvBiologicalRole());
        if (annotation != null) {
            complexDetailsFeatures.setFeatureTypeDefinition(annotation);
        }
    }

    // This method sets the xref qualifier information
    protected static void setXrefQualifier(ComplexDetailsCrossReferences cross, CvXrefQualifier cvXrefQualifier) {
        cross.setQualifier(cvXrefQualifier.getFullName() != null ? cvXrefQualifier.getFullName() : cvXrefQualifier.getShortLabel());
        cross.setQualifierMI(cvXrefQualifier.getIdentifier());
        String annotation = getAnnotation(cvXrefQualifier);
        if (annotation != null) {
            cross.setQualifierDefinition(annotation);
        }
    }

    //
    // ALIASES
    //
    //Generic method to get information stored in the aliases
    private static String getAlias(IntactComplex complex, String id) {
        for (Alias alias : complex.getAliases()) {
            if (alias.getName() != null && alias.getCvAliasType() != null && alias.getCvAliasType().getIdentifier().equals(id)) {
                return alias.getName();
            }
        }
        return null;
    }

    public static String getSystematicName(IntactComplex complex) {
        return getAlias(complex, CvAliasType.COMPLEX_SYSTEMATIC_NAME_MI_REF);
    }

    public static String getRecommendedName(IntactComplex complex) {
        return getAlias(complex, CvAliasType.COMPLEX_RECOMMENDED_NAME_MI_REF);
    }

    //Retrieve all the synosyms of the complex
    public static List<String> getSynonyms(IntactComplex complex) {
        List<String> synosyms = new ArrayList<String>();
        for (Alias alias : complex.getAliases()) {
            if (alias.getName() != null && alias.getCvAliasType() != null && alias.getCvAliasType().getIdentifier().equals(CvAliasType.COMPLEX_SYSTEMATIC_NAME_MI_REF)) {
                synosyms.add(alias.getName());
            }
        }
        return synosyms;
    }

    //Retrieve the first alias found
    public static String getFirstAlias(IntactComplex complex) {
        for (Alias alias : complex.getAliases()) {
            if (alias.getName() != null) {
                return alias.getName();
            }
        }
        return null;
    }

    public static String getName(IntactComplex complex) {
        String name = getRecommendedName(complex);
        if (name != null) return name;
        name = getSystematicName(complex);
        if (name != null) return name;
        List<String> synonyms = getSynonyms(complex);
        if (synonyms != Collections.EMPTY_LIST) return synonyms.get(0);
        name = getFirstAlias(complex);
        if (name != null) return name;
        return complex.getShortLabel();
    }

    //
    // SPECIES
    //
    //
    public static String getSpeciesName(IntactComplex complex) {
        if (!complex.getExperiments().isEmpty()) {
            Experiment exp = complex.getExperiments().iterator().next();
            BioSource bioSource = exp.getBioSource();
            if (bioSource != null) {
                return bioSource.getFullName() != null ? bioSource.getFullName() : bioSource.getShortLabel();
            }
        }
        return null;
    }

    public static String getSpeciesTaxId(IntactComplex complex) {
        if (!complex.getExperiments().isEmpty()) {
            Experiment exp = complex.getExperiments().iterator().next();
            BioSource bioSource = exp.getBioSource();
            if (bioSource != null) {
                return bioSource.getTaxId();
            }
        }
        return null;
    }

    //
    // ANNOTATIONS
    //
    //Generic method to retrieve information stored in the annotations
    private static String getAnnotationById(IntactComplex complex, String id) {
        for (Annotation annotation : complex.getAnnotations()) {
            if (annotation.getCvTopic() != null && annotation.getCvTopic().getIdentifier() != null && annotation.getCvTopic().getIdentifier().equalsIgnoreCase(id)) {
                return annotation.getAnnotationText();
            }
        }
        return null;
    }

    public static String getProperties(IntactComplex complex) {
        return getAnnotationById(complex, CvTopic.COMPLEX_PROPERTIES_MI_REF);
    }

    public static String getDisease(IntactComplex complex) {
        return getAnnotationById(complex, CvTopic.COMPLEX_DISEASE_MI_REF);
    }

    private static String getAnnotationByShortLabel(IntactComplex complex, String shortLabel) {
        for (Annotation annotation : complex.getAnnotations()) {
            if (annotation.getCvTopic() != null && annotation.getCvTopic().getShortLabel() != null && annotation.getCvTopic().getShortLabel().equalsIgnoreCase(shortLabel)) {
                return annotation.getAnnotationText();
            }
        }
        return null;
    }

    public static String getLigand(IntactComplex complex) {
        return getAnnotationByShortLabel(complex, CvTopic.COMPLEX_LIGAND);
    }

    public static String getComplexAssembly(IntactComplex complex) {
        return getAnnotationByShortLabel(complex, CvTopic.COMPLEX_ASSEMBLY);
    }

    public static String getFunction(IntactComplex complex) {
        return getAnnotationByShortLabel(complex, CvTopic.CURATED_COMPLEX);
    }

}
