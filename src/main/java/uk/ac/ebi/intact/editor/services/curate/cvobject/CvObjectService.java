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
package uk.ac.ebi.intact.editor.services.curate.cvobject;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.DualListModel;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import psidev.psi.mi.jami.utils.CvTermUtils;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.jami.model.extension.IntactComplex;
import uk.ac.ebi.intact.jami.model.extension.IntactCvTerm;
import uk.ac.ebi.intact.jami.utils.IntactUtils;

import javax.faces.event.ComponentSystemEvent;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
import javax.persistence.Query;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Service
@Lazy
public class CvObjectService extends AbstractEditorService {

    private static final Log log = LogFactory.getLog( CvObjectService.class );

    public static final String NO_CLASS = "no_class";

    private Map<CvKey, IntactCvTerm> allCvObjectMap;
    private Map<String, IntactCvTerm> acCvObjectMap;

    private List<SelectItem> publicationTopicSelectItems;
    private List<SelectItem> experimentTopicSelectItems;
    private List<SelectItem> interactionTopicSelectItems;
    private List<SelectItem> interactorTopicSelectItems;
    private List<SelectItem> participantTopicSelectItems;
    private List<SelectItem> featureTopicSelectItems;
    private List<SelectItem> complexTopicSelectItems;
    private List<SelectItem> cvObjectTopicSelectItems;
    private List<SelectItem> noClassSelectItems;

    private List<SelectItem> databaseSelectItems;

    private List<SelectItem> qualifierSelectItems;

    private List<SelectItem> aliasTypeSelectItems;

    private List<SelectItem> interactionDetectionMethodSelectItems;

    private List<SelectItem> participantDetectionMethodSelectItems;

    private List<SelectItem> participantExperimentalPreparationsSelectItems;

    private List<SelectItem> interactionTypeSelectItems;

    private List<SelectItem> interactorTypeSelectItems;
    private List<SelectItem> proteinTypeSelectItems;
    private List<SelectItem> bioactiveEntitySelectItems;
    private List<SelectItem> nucleicAcidSelectItems;
    private List<SelectItem> polymerTypeSelectItems;
    private List<SelectItem> moleculeSetTypeSelectItems;
    private List<SelectItem> geneTypeSelectItems;

    private List<SelectItem> experimentalRoleSelectItems;

    private List<SelectItem> biologicalRoleSelectItems;

    private List<SelectItem> featureDetectionMethodSelectItems;

    private List<SelectItem> featureTypeSelectItems;

    private List<SelectItem> fuzzyTypeSelectItems;

    private List<SelectItem> cellTypeSelectItems;

    private List<SelectItem> tissueSelectItems;

    private List<SelectItem> parameterTypeSelectItems;

    private List<SelectItem> parameterUnitSelectItems;

    private List<SelectItem> confidenceTypeSelectItems;

    private List<SelectItem> featureRoleSelectItems;

    private List<SelectItem> complexTypeSelectItems;

    private List<SelectItem> evidenceTypeSelectItems;

    private IntactCvTerm defaultExperimentalRole;
    private IntactCvTerm defaultBiologicalRole;

    public static final String USED_IN_CLASS = "used-in-class";
    public static final String OBSOLETE = "obsolete";
    public static final String OBSOLETE_MI_REF = "MI:0431";

    private boolean isInitialised = false;

    public CvObjectService() {
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public void loadData( ) {
        if ( log.isDebugEnabled() ) log.debug( "Loading Controlled Vocabularies" );

        synchronized (this){
            String cvQuery = "select c from IntactCvTerm c " +
                    "where c.ac not in (" +
                    " select c2.ac from IntactCvTerm c2 join c2.dbAnnotations as a join a.topic as t " +
                    "where t.shortName = :hidden)";
            Query query = getIntactDao().getEntityManager().createQuery(cvQuery);
            query.setParameter("hidden","hidden");

            final List<IntactCvTerm> allCvObjects = query.getResultList();

            allCvObjectMap = new HashMap<CvKey, IntactCvTerm>( allCvObjects.size() * 2 );
            acCvObjectMap = new HashMap<String, IntactCvTerm>( allCvObjects.size() );

            HashMultimap<String, IntactCvTerm> cvObjectsByUsedInClass = HashMultimap.create();
            HashMultimap<String, IntactCvTerm> cvObjectsByClass = HashMultimap.create();

            for ( IntactCvTerm cvObject : allCvObjects ) {
                acCvObjectMap.put( cvObject.getAc(), cvObject );
                cvObjectsByClass.put(cvObject.getObjClass(), cvObject);

                CvKey keyId = null;
                if ( cvObject.getMIIdentifier() != null ) {
                    keyId = new CvKey( cvObject.getMIIdentifier(), cvObject.getObjClass() );
                }
                else if(cvObject.getMODIdentifier() != null){
                    keyId = new CvKey( cvObject.getMIIdentifier(), cvObject.getObjClass() );
                }
                else if (!cvObject.getIdentifiers().isEmpty()){
                    keyId = new CvKey(cvObject.getIdentifiers().iterator().next().getId(), cvObject.getObjClass());
                }
                else {
                    keyId = new CvKey(cvObject.getAc(), cvObject.getObjClass());
                }
                CvKey keyLabel = new CvKey( cvObject.getShortName(), cvObject.getObjClass() );
                allCvObjectMap.put( keyId, cvObject );
                allCvObjectMap.put( keyLabel, cvObject );

                if (IntactUtils.TOPIC_OBJCLASS.equals(cvObject.getObjClass())) {
                    String[] usedInClasses = findUsedInClass( cvObject );

                    for ( String usedInClass : usedInClasses ) {
                        cvObjectsByUsedInClass.put( usedInClass, cvObject );
                    }

                    if ( usedInClasses.length == 0 ) {
                        cvObjectsByUsedInClass.put(NO_CLASS, cvObject );
                    }
                }
                else if (cvObject.getAc() != null){
                    Hibernate.initialize(cvObject.getDbAnnotations());
                }
            }

            // topics
            final List<IntactCvTerm>publicationTopics = getSortedTopicList("uk.ac.ebi.intact.model.Publication", cvObjectsByUsedInClass);
            final List<IntactCvTerm>experimentTopics = getSortedTopicList("uk.ac.ebi.intact.model.Experiment", cvObjectsByUsedInClass);
            final List<IntactCvTerm>interactionTopics = getSortedTopicList( "uk.ac.ebi.intact.model.Interaction", cvObjectsByUsedInClass);
            final List<IntactCvTerm>interactorTopics = getSortedTopicList( "uk.ac.ebi.intact.model.Interactor", cvObjectsByUsedInClass);
            interactorTopics.addAll(getSortedTopicList("uk.ac.ebi.intact.model.NulceicAcid", cvObjectsByUsedInClass));
            interactorTopics.addAll(getSortedTopicList("uk.ac.ebi.intact.model.SmallMolecule", cvObjectsByUsedInClass));
            interactorTopics.addAll(getSortedTopicList("uk.ac.ebi.intact.model.Protein", cvObjectsByUsedInClass));
            final List<IntactCvTerm>participantTopics = getSortedTopicList( "uk.ac.ebi.intact.model.Component", cvObjectsByUsedInClass);
            final List<IntactCvTerm>featureTopics = getSortedTopicList( "uk.ac.ebi.intact.model.Feature", cvObjectsByUsedInClass);
            final List<IntactCvTerm>cvObjectTopics = getSortedTopicList( "uk.ac.ebi.intact.model.CvObject", cvObjectsByUsedInClass);
            final Set<IntactCvTerm>complexTopics = new HashSet<IntactCvTerm>(getSortedTopicList(IntactComplex.class.getCanonicalName(), cvObjectsByUsedInClass));
            complexTopics.addAll(interactionTopics);
            interactorTopics.addAll(complexTopics);
            final List<IntactCvTerm>noClassTopics = getSortedTopicList( NO_CLASS, cvObjectsByUsedInClass);

            // select items
            noClassSelectItems = createSelectItems(noClassTopics, null);

            SelectItemGroup noClassSelectItemGroup = new SelectItemGroup("Not classified");
            noClassSelectItemGroup.setSelectItems(noClassSelectItems.toArray(new SelectItem[noClassSelectItems.size()]));

            SelectItemGroup pubSelectItemGroup = new SelectItemGroup("Publication");
            List<SelectItem> pubTopicSelectItems = createSelectItems(publicationTopics, null);
            pubSelectItemGroup.setSelectItems(pubTopicSelectItems.toArray(new SelectItem[pubTopicSelectItems.size()]));

            SelectItemGroup expSelectItemGroup = new SelectItemGroup("Experiment");
            List<SelectItem> expTopicSelectItems = createSelectItems(experimentTopics, null);
            expSelectItemGroup.setSelectItems(expTopicSelectItems.toArray(new SelectItem[expTopicSelectItems.size()]));

            publicationTopicSelectItems = new ArrayList<SelectItem>();
            publicationTopicSelectItems.add( new SelectItem( null, "-- Select topic --", "-- Select topic --", false, false, true ) );
            publicationTopicSelectItems.add(pubSelectItemGroup);
            publicationTopicSelectItems.add(expSelectItemGroup);
            publicationTopicSelectItems.add(noClassSelectItemGroup);

            experimentTopicSelectItems = createSelectItems( experimentTopics, "-- Select topic --" );
            experimentTopicSelectItems.add(noClassSelectItemGroup);

            interactionTopicSelectItems = createSelectItems( interactionTopics, "-- Select topic --" );
            interactionTopicSelectItems.add(noClassSelectItemGroup);

            SelectItemGroup interactorSelectItemGroup = new SelectItemGroup("Interactor");
            interactorTopicSelectItems = createSelectItems(interactorTopics, null);
            interactorSelectItemGroup.setSelectItems(interactorTopicSelectItems.toArray(new SelectItem[interactorTopicSelectItems.size()]));

            interactorTopicSelectItems = new ArrayList<SelectItem>();
            interactorTopicSelectItems.add( new SelectItem( null, "-- Select topic --", "-- Select topic --", false, false, true ) );
            interactorTopicSelectItems.add(interactorSelectItemGroup);
            interactorTopicSelectItems.add(noClassSelectItemGroup);

            participantTopicSelectItems = createSelectItems( participantTopics, "-- Select topic --" );
            participantTopicSelectItems.add(noClassSelectItemGroup);

            featureTopicSelectItems = createSelectItems( featureTopics, "-- Select topic --" );
            featureTopicSelectItems.add(noClassSelectItemGroup);

            cvObjectTopicSelectItems = createSelectItems( cvObjectTopics, "-- Select topic --" );
            cvObjectTopicSelectItems.add(noClassSelectItemGroup);

            complexTopicSelectItems = createSelectItems( complexTopics, "-- Select topic --" );
            complexTopicSelectItems.add(noClassSelectItemGroup);

            final List<IntactCvTerm> databases = getSortedList( IntactUtils.DATABASE_OBJCLASS, cvObjectsByClass);
            databaseSelectItems = createSelectItems(databases, "-- Select database --", "ECO:");

            final List<IntactCvTerm> qualifiers = getSortedList( IntactUtils.QUALIFIER_OBJCLASS, cvObjectsByClass);
            qualifierSelectItems = createSelectItems( qualifiers, "-- Select qualifier --" );

            final List<IntactCvTerm> aliasTypes = getSortedList( IntactUtils.ALIAS_TYPE_OBJCLASS, cvObjectsByClass);
            aliasTypeSelectItems = createSelectItems( aliasTypes, "-- Select type --" );

            final List<IntactCvTerm> interactionDetectionMethods = getSortedList( IntactUtils.INTERACTION_DETECTION_METHOD_OBJCLASS, cvObjectsByClass);
            interactionDetectionMethodSelectItems = createSelectItems( interactionDetectionMethods, "-- Select method --" );

            final List<IntactCvTerm> participantDetectionMethods = getSortedList( IntactUtils.PARTICIPANT_DETECTION_METHOD_OBJCLASS, cvObjectsByClass);
            participantDetectionMethodSelectItems = createSelectItems( participantDetectionMethods, "-- Select method --" );

            final List<IntactCvTerm> participantExperimentalPreparations = getSortedList( IntactUtils.PARTICIPANT_EXPERIMENTAL_PREPARATION_OBJCLASS, cvObjectsByClass);
            participantExperimentalPreparationsSelectItems = createSelectItems( participantExperimentalPreparations, "-- Select experimental preparation --" );

            final List<IntactCvTerm> interactionTypes = getSortedList( IntactUtils.INTERACTION_TYPE_OBJCLASS, cvObjectsByClass);
            interactionTypeSelectItems = createSelectItems( interactionTypes, "-- Select type --" );

            final List<IntactCvTerm> interactorTypes = getSortedList( IntactUtils.INTERACTOR_TYPE_OBJCLASS, cvObjectsByClass);
            interactorTypeSelectItems = createSelectItems(interactorTypes, "-- Select type --");

            final List<IntactCvTerm> experimentalRoles = getSortedList( IntactUtils.EXPERIMENTAL_ROLE_OBJCLASS, cvObjectsByClass);
            // must have one experimental role
            experimentalRoleSelectItems = createExperimentalRoleSelectItems( experimentalRoles, null );

            final List<IntactCvTerm> biologicalRoles = getSortedList( IntactUtils.BIOLOGICAL_ROLE_OBJCLASS, cvObjectsByClass);
            // must have one biological role
            biologicalRoleSelectItems = createBiologicalRoleSelectItems( biologicalRoles, null );

            final List<IntactCvTerm> featureDetectionMethods = getSortedList( IntactUtils.FEATURE_METHOD_OBJCLASS, cvObjectsByClass);
            featureDetectionMethodSelectItems = createSelectItems( featureDetectionMethods, "-- Select method --" );

            final List<IntactCvTerm> featureTypes = getSortedList( IntactUtils.FEATURE_TYPE_OBJCLASS, cvObjectsByClass);
            featureTypeSelectItems = createSelectItems( featureTypes, "-- Select type --" );

            final List<IntactCvTerm> fuzzyTypes = getSortedList( IntactUtils.RANGE_STATUS_OBJCLASS, cvObjectsByClass);
            fuzzyTypeSelectItems = createSelectItems( fuzzyTypes, "-- Select type --" );

            final List<IntactCvTerm> cellTypes = getSortedList( IntactUtils.CELL_TYPE_OBJCLASS, cvObjectsByClass);
            cellTypeSelectItems = createSelectItems( cellTypes, "-- Select cell type --" );

            final List<IntactCvTerm> tissues = getSortedList( IntactUtils.TISSUE_OBJCLASS, cvObjectsByClass);
            tissueSelectItems = createSelectItems( tissues, "-- Select tissue --" );

            final List<IntactCvTerm> parameterTypes = getSortedList( IntactUtils.PARAMETER_TYPE_OBJCLASS, cvObjectsByClass);
            parameterTypeSelectItems = createSelectItems( parameterTypes, "-- Select type --" );

            final List<IntactCvTerm> parameterUnits = getSortedList( IntactUtils.UNIT_OBJCLASS, cvObjectsByClass);
            parameterUnitSelectItems = createSelectItems( parameterUnits, "-- Select unit --" );

            final List<IntactCvTerm> confidenceTypes = getSortedList( IntactUtils.CONFIDENCE_TYPE_OBJCLASS, cvObjectsByClass);
            confidenceTypeSelectItems = createSelectItems( confidenceTypes, "-- Select type --" );

            // evidence type
            evidenceTypeSelectItems = new ArrayList<SelectItem>();
            evidenceTypeSelectItems.add(new SelectItem(null, "-- Select evidence code --", "-- Select evidence code --", false, false, true));
            IntactCvTerm evidenceTypeParent = getIntactDao().getCvTermDao().getByMIIdentifier("MI:1331", IntactUtils.DATABASE_OBJCLASS);
            if (evidenceTypeParent != null){
                loadChildren(evidenceTypeParent, evidenceTypeSelectItems, false, new HashSet<String>());
            }

            // complex type
            complexTypeSelectItems = new ArrayList<SelectItem>();
            IntactCvTerm complexTypeParent = getIntactDao().getCvTermDao().getByMIIdentifier(Complex.COMPLEX_MI, IntactUtils.INTERACTOR_TYPE_OBJCLASS);
            SelectItem item = complexTypeParent != null ? createSelectItem(complexTypeParent, true):null;
            if (item != null){
                complexTypeSelectItems.add(item);
            }
            if (complexTypeParent != null){
                loadChildren(complexTypeParent, complexTypeSelectItems, false, new HashSet<String>());
            }

            // nucleic acid type
            nucleicAcidSelectItems = new ArrayList<SelectItem>();
            IntactCvTerm nucleicAcidTypeParent = getIntactDao().getCvTermDao().getByMIIdentifier(NucleicAcid.NULCEIC_ACID_MI, IntactUtils.INTERACTOR_TYPE_OBJCLASS);
            SelectItem itemNucleicAcid = nucleicAcidTypeParent != null ? createSelectItem(nucleicAcidTypeParent, true):null;
            if (itemNucleicAcid != null){
                nucleicAcidSelectItems.add(itemNucleicAcid);
            }
            if (nucleicAcidTypeParent != null){
                loadChildren(nucleicAcidTypeParent, nucleicAcidSelectItems, false, new HashSet<String>());
            }

            // polymer and protein type
            this.polymerTypeSelectItems = new ArrayList<SelectItem>();
            this.proteinTypeSelectItems = new ArrayList<SelectItem>();
            IntactCvTerm polymerType = getIntactDao().getCvTermDao().getByMIIdentifier(Polymer.POLYMER_MI, IntactUtils.INTERACTOR_TYPE_OBJCLASS);
            IntactCvTerm proteinType = getIntactDao().getCvTermDao().getByMIIdentifier(Protein.PROTEIN_MI, IntactUtils.INTERACTOR_TYPE_OBJCLASS);
            IntactCvTerm peptideType = getIntactDao().getCvTermDao().getByMIIdentifier(Protein.PEPTIDE_MI, IntactUtils.INTERACTOR_TYPE_OBJCLASS);
            SelectItem itemPolymer = polymerType != null ? createSelectItem(polymerType, true):null;
            SelectItem itemProtein = proteinType != null ? createSelectItem(proteinType, true):null;
            SelectItem itemPeptide = peptideType != null ? createSelectItem(peptideType, true):null;
            if (itemPolymer != null){
                polymerTypeSelectItems.add(itemPolymer);
            }
            if (itemProtein != null){
                proteinTypeSelectItems.add(itemProtein);
            }
            if (itemPeptide != null){
                proteinTypeSelectItems.add(itemPeptide);
            }
            if (polymerType != null){
                loadChildren(polymerType, polymerTypeSelectItems, false, new HashSet<String>());
            }
            if (proteinType != null){
                loadChildren(proteinType, proteinTypeSelectItems, false, new HashSet<String>());
            }
            if (peptideType != null){
                loadChildren(peptideType, proteinTypeSelectItems, false, new HashSet<String>());
            }

            // gene type
            this.geneTypeSelectItems = new ArrayList<SelectItem>();
            IntactCvTerm geneTypeParent = getIntactDao().getCvTermDao().getByMIIdentifier(Gene.GENE_MI, IntactUtils.INTERACTOR_TYPE_OBJCLASS);
            SelectItem itemGene = geneTypeParent != null ? createSelectItem(geneTypeParent, true):null;
            if (itemGene != null){
                geneTypeSelectItems.add(itemGene);
            }
            if (itemGene != null){
                loadChildren(geneTypeParent, geneTypeSelectItems, false, new HashSet<String>());
            }

            // molecule set type
            moleculeSetTypeSelectItems = new ArrayList<SelectItem>();
            IntactCvTerm moleculeSetParent = getIntactDao().getCvTermDao().getByMIIdentifier(InteractorPool.MOLECULE_SET_MI, IntactUtils.INTERACTOR_TYPE_OBJCLASS);
            SelectItem itemSet = moleculeSetParent != null ? createSelectItem(moleculeSetParent, true):null;
            if (itemSet != null){
                moleculeSetTypeSelectItems.add(itemSet);
            }
            if (moleculeSetParent != null){
                loadChildren(moleculeSetParent, moleculeSetTypeSelectItems, false, new HashSet<String>());
            }

            // bioactive entity type
            bioactiveEntitySelectItems = new ArrayList<SelectItem>();
            IntactCvTerm bioactiveEntityParent = getIntactDao().getCvTermDao().getByMIIdentifier(BioactiveEntity.BIOACTIVE_ENTITY_MI, IntactUtils.INTERACTOR_TYPE_OBJCLASS);
            SelectItem itemEntity = bioactiveEntityParent != null ? createSelectItem(bioactiveEntityParent, true):null;
            if (itemEntity != null){
                bioactiveEntitySelectItems.add(itemEntity);
            }
            if (bioactiveEntityParent != null){
                loadChildren(bioactiveEntityParent, bioactiveEntitySelectItems, false, new HashSet<String>());
            }

            // feature role
            featureRoleSelectItems = new ArrayList<SelectItem>();
            featureRoleSelectItems.add(new SelectItem(null, "-- Select role --", "-- Select role --", false, false, true));
            IntactCvTerm roleParent = getIntactDao().getCvTermDao().getByMIIdentifier("MI:0925", IntactUtils.TOPIC_OBJCLASS);
            SelectItem item2 = roleParent != null ? createSelectItem(roleParent, false):null;
            if (item2 != null){
                featureRoleSelectItems.add(item2);
            }
            if (roleParent != null){
                loadChildren(roleParent, featureRoleSelectItems, false, new HashSet<String>());
            }

            isInitialised=true;
        }
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public void refreshCvs(String objClass){
         if (objClass != null){
             if (objClass.equals(IntactUtils.ALIAS_TYPE_OBJCLASS)){
                 refreshSelectItems(IntactUtils.ALIAS_TYPE_OBJCLASS, this.aliasTypeSelectItems, null, "-- Select type --");
             }
             else if (objClass.equals(IntactUtils.QUALIFIER_OBJCLASS)){
                 refreshSelectItems(IntactUtils.QUALIFIER_OBJCLASS, this.qualifierSelectItems, null, "-- Select qualifier --");
             }
             else if (objClass.equals(IntactUtils.DATABASE_OBJCLASS)){
                 refreshSelectItems(IntactUtils.DATABASE_OBJCLASS, this.databaseSelectItems, "ECO:", "-- Select database --");

                 refreshDependencies(this.evidenceTypeSelectItems, "MI:1331", IntactUtils.DATABASE_OBJCLASS, "Select evidence type", false);
             }
             else if (objClass.equals(IntactUtils.TOPIC_OBJCLASS)){

                 refreshAllTopics();

                 refreshDependencies(this.featureRoleSelectItems, "MI:0925", IntactUtils.TOPIC_OBJCLASS, "Select role", true);
             }
             else if (objClass.equals(IntactUtils.BIOLOGICAL_ROLE_OBJCLASS)){
                 refreshSelectItems(IntactUtils.BIOLOGICAL_ROLE_OBJCLASS, this.biologicalRoleSelectItems, null, "-- Select biological role --");
             }
             else if (objClass.equals(IntactUtils.CELL_TYPE_OBJCLASS)){
                 refreshSelectItems(IntactUtils.CELL_TYPE_OBJCLASS, this.cellTypeSelectItems, null, "-- Select cell type --");
             }
             else if (objClass.equals(IntactUtils.CONFIDENCE_TYPE_OBJCLASS)){
                 refreshSelectItems(IntactUtils.CONFIDENCE_TYPE_OBJCLASS, this.confidenceTypeSelectItems, null, "-- Select type --");
             }
             else if (objClass.equals(IntactUtils.EXPERIMENTAL_ROLE_OBJCLASS)){
                 refreshSelectItems(IntactUtils.EXPERIMENTAL_ROLE_OBJCLASS, this.experimentalRoleSelectItems, null, "-- Select experimental role --");
             }
             else if (objClass.equals(IntactUtils.FEATURE_METHOD_OBJCLASS)){
                 refreshSelectItems(IntactUtils.FEATURE_METHOD_OBJCLASS, this.featureDetectionMethodSelectItems, null, "-- Select method --");
             }
             else if (objClass.equals(IntactUtils.FEATURE_TYPE_OBJCLASS)){
                 refreshSelectItems(IntactUtils.FEATURE_TYPE_OBJCLASS, this.featureTypeSelectItems, null, "-- Select type --");
             }
             else if (objClass.equals(IntactUtils.INTERACTION_DETECTION_METHOD_OBJCLASS)){
                 refreshSelectItems(IntactUtils.INTERACTION_DETECTION_METHOD_OBJCLASS, this.interactionDetectionMethodSelectItems, null, "-- Select detection method --");
             }
             else if (objClass.equals(IntactUtils.INTERACTION_TYPE_OBJCLASS)){
                 refreshSelectItems(IntactUtils.INTERACTION_TYPE_OBJCLASS, this.interactionTypeSelectItems, null, "-- Select type --");
             }
             else if (objClass.equals(IntactUtils.INTERACTOR_TYPE_OBJCLASS)){
                 // reset all interactor types
                 refreshSelectItems(IntactUtils.INTERACTOR_TYPE_OBJCLASS, this.interactorTypeSelectItems, null, "-- Select type --");
                 // reset specialiased types
                 // complex type
                 refreshDependencies(this.complexTypeSelectItems, Complex.COMPLEX_MI, IntactUtils.INTERACTOR_TYPE_OBJCLASS, "-- Select complex type --", true);

                 // nucleic acid type
                 refreshDependencies(this.nucleicAcidSelectItems, NucleicAcid.NULCEIC_ACID_MI, IntactUtils.INTERACTOR_TYPE_OBJCLASS, "-- Select type --", true);

                 // polymer and protein type
                 List<SelectItem> proteins = new ArrayList<SelectItem>(proteinTypeSelectItems);
                 List<SelectItem> peptides = new ArrayList<SelectItem>();
                 refreshDependencies(this.polymerTypeSelectItems, Polymer.POLYMER_MI, IntactUtils.INTERACTOR_TYPE_OBJCLASS, "-- Select type --", true);
                 synchronized (this.proteinTypeSelectItems){
                     refreshDependencies(proteins, Protein.PROTEIN, IntactUtils.INTERACTOR_TYPE_OBJCLASS, "-- Select type --", true);
                     refreshDependencies(peptides, Protein.PEPTIDE, IntactUtils.INTERACTOR_TYPE_OBJCLASS, null, true);

                     this.proteinTypeSelectItems.clear();
                     this.proteinTypeSelectItems.addAll(proteins);
                     this.proteinTypeSelectItems.addAll(peptides);
                 }

                 // gene type
                 refreshDependencies(this.geneTypeSelectItems, Gene.GENE_MI, IntactUtils.INTERACTOR_TYPE_OBJCLASS, "-- Select type --", true);

                 // molecule set type
                 refreshDependencies(this.moleculeSetTypeSelectItems, InteractorPool.MOLECULE_SET_MI, IntactUtils.INTERACTOR_TYPE_OBJCLASS, "-- Select type --", true);

                 // bioactive entity type
                 refreshDependencies(this.bioactiveEntitySelectItems, BioactiveEntity.BIOACTIVE_ENTITY_MI, IntactUtils.INTERACTOR_TYPE_OBJCLASS, "-- Select type --", true);
             }
             else if (objClass.equals(IntactUtils.LIFECYCLE_EVENT_OBJCLASS)){
                 refreshSelectItems(IntactUtils.LIFECYCLE_EVENT_OBJCLASS, null, null, null);
             }
             else if (objClass.equals(IntactUtils.PARAMETER_TYPE_OBJCLASS)){
                 refreshSelectItems(IntactUtils.PARAMETER_TYPE_OBJCLASS, parameterTypeSelectItems, null, null);
             }
             else if (objClass.equals(IntactUtils.PARTICIPANT_DETECTION_METHOD_OBJCLASS)){
                 refreshSelectItems(IntactUtils.PARTICIPANT_DETECTION_METHOD_OBJCLASS, participantDetectionMethodSelectItems, null, "-- Select identification method --");
             }
             else if (objClass.equals(IntactUtils.PARTICIPANT_EXPERIMENTAL_PREPARATION_OBJCLASS)){
                 refreshSelectItems(IntactUtils.PARTICIPANT_EXPERIMENTAL_PREPARATION_OBJCLASS, participantExperimentalPreparationsSelectItems, null, "-- Select experimental preparation --");
             }
             else if (objClass.equals(IntactUtils.PUBLICATION_STATUS_OBJCLASS)){
                 refreshSelectItems(IntactUtils.PUBLICATION_STATUS_OBJCLASS, null, null, null);
             }
             else if (objClass.equals(IntactUtils.RANGE_STATUS_OBJCLASS)){
                 refreshSelectItems(IntactUtils.RANGE_STATUS_OBJCLASS, fuzzyTypeSelectItems, null, "-- Select status --");
             }
             else if (objClass.equals(IntactUtils.UNIT_OBJCLASS)){
                 refreshSelectItems(IntactUtils.UNIT_OBJCLASS, parameterUnitSelectItems, null, "-- Select unit --");
             }
             else if (objClass.equals(IntactUtils.TISSUE_OBJCLASS)){
                 refreshSelectItems(IntactUtils.TISSUE_OBJCLASS, tissueSelectItems, null, "-- Select tissue --");
             }
         }
    }

    private void refreshAllTopics() {
        HashMultimap<String, IntactCvTerm> cvObjectsByUsedInClass = HashMultimap.create();
        HashMultimap<String, IntactCvTerm> cvObjectsByClass = HashMultimap.create();

        final Collection<IntactCvTerm> topics = getIntactDao().getCvTermDao().getByObjClass(IntactUtils.TOPIC_OBJCLASS);

        for ( IntactCvTerm cvObject : topics ) {
            cvObjectsByClass.put(cvObject.getObjClass(), cvObject);
            Hibernate.initialize(cvObject.getDbXrefs());
            String[] usedInClasses = findUsedInClass( cvObject );

            for ( String usedInClass : usedInClasses ) {
                cvObjectsByUsedInClass.put( usedInClass, cvObject );
            }

            if ( usedInClasses.length == 0 ) {
                cvObjectsByUsedInClass.put(NO_CLASS, cvObject );
            }
        }

        // topics
        final List<IntactCvTerm> publicationTopics = getSortedTopicList("uk.ac.ebi.intact.model.Publication", cvObjectsByUsedInClass);
        final List<IntactCvTerm>experimentTopics = getSortedTopicList("uk.ac.ebi.intact.model.Experiment", cvObjectsByUsedInClass);
        final List<IntactCvTerm>interactionTopics = getSortedTopicList( "uk.ac.ebi.intact.model.Interaction", cvObjectsByUsedInClass);
        final List<IntactCvTerm>interactorTopics = getSortedTopicList( "uk.ac.ebi.intact.model.Interactor", cvObjectsByUsedInClass);
        interactorTopics.addAll(getSortedTopicList("uk.ac.ebi.intact.model.NulceicAcid", cvObjectsByUsedInClass));
        interactorTopics.addAll(getSortedTopicList("uk.ac.ebi.intact.model.SmallMolecule", cvObjectsByUsedInClass));
        interactorTopics.addAll(getSortedTopicList("uk.ac.ebi.intact.model.Protein", cvObjectsByUsedInClass));
        final List<IntactCvTerm>participantTopics = getSortedTopicList( "uk.ac.ebi.intact.model.Component", cvObjectsByUsedInClass);
        final List<IntactCvTerm>featureTopics = getSortedTopicList( "uk.ac.ebi.intact.model.Feature", cvObjectsByUsedInClass);
        final List<IntactCvTerm>cvObjectTopics = getSortedTopicList( "uk.ac.ebi.intact.model.CvObject", cvObjectsByUsedInClass);
        final List<IntactCvTerm> complexTopics = getSortedTopicList(IntactComplex.class.getCanonicalName(), cvObjectsByUsedInClass);
        for (IntactCvTerm cv : interactionTopics){
           if (!complexTopics.contains(cv)){
               complexTopics.add(cv);
           }
        }
        for (IntactCvTerm cv : interactorTopics){
            if (!complexTopics.contains(cv)){
                complexTopics.add(cv);
            }
        }
        final List<IntactCvTerm>noClassTopics = getSortedTopicList( NO_CLASS, cvObjectsByUsedInClass);

        // select items
        noClassSelectItems = createSelectItems(noClassTopics, null);

        SelectItemGroup noClassSelectItemGroup = new SelectItemGroup("Not classified");
        noClassSelectItemGroup.setSelectItems(noClassSelectItems.toArray(new SelectItem[noClassSelectItems.size()]));

        SelectItemGroup pubSelectItemGroup = new SelectItemGroup("Publication");
        List<SelectItem> pubTopicSelectItems = createSelectItems(publicationTopics, null);
        pubSelectItemGroup.setSelectItems(pubTopicSelectItems.toArray(new SelectItem[pubTopicSelectItems.size()]));

        SelectItemGroup expSelectItemGroup = new SelectItemGroup("Experiment");
        List<SelectItem> expTopicSelectItems = createSelectItems(experimentTopics, null);
        expSelectItemGroup.setSelectItems(expTopicSelectItems.toArray(new SelectItem[expTopicSelectItems.size()]));

        synchronized (publicationTopicSelectItems){
            publicationTopicSelectItems = new ArrayList<SelectItem>();
            publicationTopicSelectItems.add( new SelectItem( null, "-- Select topic --", "-- Select topic --", false, false, true ) );
            publicationTopicSelectItems.add(pubSelectItemGroup);
            publicationTopicSelectItems.add(expSelectItemGroup);
            publicationTopicSelectItems.add(noClassSelectItemGroup);
        }

        synchronized (experimentTopicSelectItems){
            experimentTopicSelectItems = createSelectItems( experimentTopics, "-- Select topic --" );
            experimentTopicSelectItems.add(noClassSelectItemGroup);
        }

        synchronized (interactionTopicSelectItems){
            interactionTopicSelectItems = createSelectItems( interactionTopics, "-- Select topic --" );
            interactionTopicSelectItems.add(noClassSelectItemGroup);
        }
        synchronized (interactorTopicSelectItems){
            SelectItemGroup interactorSelectItemGroup = new SelectItemGroup("Interactor");
            interactorTopicSelectItems = createSelectItems(interactorTopics, null);
            interactorSelectItemGroup.setSelectItems(interactorTopicSelectItems.toArray(new SelectItem[interactorTopicSelectItems.size()]));

            interactorTopicSelectItems = new ArrayList<SelectItem>();
            interactorTopicSelectItems.add( new SelectItem( null, "-- Select topic --", "-- Select topic --", false, false, true ) );
            interactorTopicSelectItems.add(interactorSelectItemGroup);
            interactorTopicSelectItems.add(noClassSelectItemGroup);
        }
        synchronized (participantTopicSelectItems) {
            participantTopicSelectItems = createSelectItems( participantTopics, "-- Select topic --" );
            participantTopicSelectItems.add(noClassSelectItemGroup);
        }
        synchronized (featureTopicSelectItems) {
            featureTopicSelectItems = createSelectItems( featureTopics, "-- Select topic --" );
            featureTopicSelectItems.add(noClassSelectItemGroup);
        }
        synchronized (cvObjectTopicSelectItems){
            cvObjectTopicSelectItems = createSelectItems( cvObjectTopics, "-- Select topic --" );
            cvObjectTopicSelectItems.add(noClassSelectItemGroup);
        }
        synchronized (complexTopicSelectItems){
            complexTopicSelectItems = createSelectItems( complexTopics, "-- Select topic --" );
            complexTopicSelectItems.add(noClassSelectItemGroup);
        }
    }

    private void refreshDependencies(List<SelectItem> items, String MIIdentifier, String objClass, String select, boolean addParent) {
        synchronized (items){
            // collect all cvs and give previous select item
            IntactCvTerm evidenceTypeParent = getIntactDao().getCvTermDao().getByMIIdentifier(MIIdentifier, objClass);
            Set<IntactCvTerm> terms = new HashSet<IntactCvTerm>();
            if (evidenceTypeParent != null){
                terms = loadChildren(evidenceTypeParent, false, new HashSet<String>());
                refreshMaps(terms, items);
            }
            items.clear();
            if (addParent && evidenceTypeParent != null){
               items.add(createSelectItem(evidenceTypeParent, true));
            }
            items.addAll(createSelectItems(terms, select));

            Collections.sort(items, new CvSelectItemComparator());
        }
    }

    private void refreshSelectItems(String objClass, List<SelectItem> items, String filter, String select) {
        synchronized (items){
            final Collection<IntactCvTerm> intactCvs = getIntactDao().getCvTermDao().getByObjClass(objClass);
            refreshMaps(intactCvs, items);
            if (items != null){
                items.clear();
                if (filter == null){
                    items.addAll(createSelectItems(intactCvs, select));

                }else{
                    items.addAll(createSelectItems(intactCvs, select, filter));
                }
            }

            Collections.sort(items, new CvSelectItemComparator());
        }
    }

    private void refreshMaps(Collection<IntactCvTerm> intactCvs, List<SelectItem> items) {
        synchronized (this.allCvObjectMap){
            synchronized (this.acCvObjectMap){
                // first remove previous intact objects
                if (items != null){
                    for (SelectItem item : items){
                        IntactCvTerm term = (IntactCvTerm)item.getValue();
                        if (term != null){
                            acCvObjectMap.remove(term.getAc());
                            CvKey keyId = null;
                            if ( term.getMIIdentifier() != null ) {
                                keyId = new CvKey( term.getMIIdentifier(), term.getObjClass() );
                            }
                            else if(term.getMODIdentifier() != null){
                                keyId = new CvKey( term.getMIIdentifier(), term.getObjClass() );
                            }
                            else if (!term.getIdentifiers().isEmpty()){
                                keyId = new CvKey(term.getIdentifiers().iterator().next().getId(), term.getObjClass());
                            }
                            else {
                                keyId = new CvKey(term.getAc(), term.getObjClass());
                            }
                            CvKey keyLabel = new CvKey( term.getShortName(), term.getObjClass() );
                            allCvObjectMap.remove( keyId );
                            allCvObjectMap.remove( keyLabel );
                        }
                    }
                }
                // add reloaded intact objects
                for (IntactCvTerm cvObject : intactCvs){
                    acCvObjectMap.put( cvObject.getAc(), cvObject );
                    CvKey keyId = null;
                    if ( cvObject.getMIIdentifier() != null ) {
                        keyId = new CvKey( cvObject.getMIIdentifier(), cvObject.getObjClass() );
                    }
                    else if(cvObject.getMODIdentifier() != null){
                        keyId = new CvKey( cvObject.getMIIdentifier(), cvObject.getObjClass() );
                    }
                    else if (!cvObject.getIdentifiers().isEmpty()){
                        keyId = new CvKey(cvObject.getIdentifiers().iterator().next().getId(), cvObject.getObjClass());
                    }
                    else {
                        keyId = new CvKey(cvObject.getAc(), cvObject.getObjClass());
                    }
                    CvKey keyLabel = new CvKey( cvObject.getShortName(), cvObject.getObjClass() );
                    allCvObjectMap.put( keyId, cvObject );
                    allCvObjectMap.put( keyLabel, cvObject );
                }
            }
        }
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public IntactCvTerm loadCvByAc(String ac) {
        IntactCvTerm cv = getIntactDao().getEntityManager().find(IntactCvTerm.class, ac);

        if (cv != null){
            // initialise xrefs because are first tab visible
            initialiseXrefs(cv.getDbXrefs());
            // initialise annotations because needs caution
            initialiseAnnotations(cv.getDbAnnotations());
            // initialise aliases
            initialiseAliases(cv.getSynonyms());
            // initialise parents
            initialiseParents(cv, cv.getParents());
        }

        return cv;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public IntactCvTerm loadCvByIdentifier(String id, String objClass) {
        IntactCvTerm cv = getIntactDao().getCvTermDao().getByUniqueIdentifier(id, objClass);

        if (cv != null){
            // initialise xrefs because are first tab visible
            initialiseXrefs(cv.getDbXrefs());
            // initialise annotations because needs caution
            initialiseAnnotations(cv.getDbAnnotations());
            // initialise aliases
            initialiseAliases(cv.getSynonyms());
            // initialise parents
            initialiseParents(cv, cv.getParents());
        }
        return cv;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countAliases(IntactCvTerm cv) {
        return getIntactDao().getCvTermDao().countSynonymsForCvTerm(cv.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countAnnotations(IntactCvTerm cv) {
        return getIntactDao().getCvTermDao().countAnnotationsForCvTerm(cv.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countXrefs(IntactCvTerm cv) {
        return getIntactDao().getCvTermDao().countXrefsForCvTerm(cv.getAc());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public DualListModel<IntactCvTerm> loadParentsList(IntactCvTerm cv) {

        List<IntactCvTerm> cvObjectsByClass = new ArrayList<IntactCvTerm>(getIntactDao().getCvTermDao().getByObjClass(cv.getObjClass()));
        List<IntactCvTerm> existingParents = new ArrayList<IntactCvTerm>(cv.getParents().size());

        // reload parents
        for (OntologyTerm parent : cv.getParents()){
            existingParents.add((IntactCvTerm)parent);
        }

        // remove parents from source
        cvObjectsByClass.removeAll(existingParents);

        Collections.sort( existingParents, new CvObjectComparator() );
        Collections.sort( cvObjectsByClass, new CvObjectComparator() );

        DualListModel<IntactCvTerm> parents = new DualListModel<IntactCvTerm>(cvObjectsByClass, existingParents);

        return parents;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public org.primefaces.model.TreeNode loadCvTreeNode(String id, String cvClass) {
        org.primefaces.model.TreeNode tree = null;
        // reload cv
        IntactCvTerm reloaded = getIntactDao().getCvTermDao().getByUniqueIdentifier(id, cvClass);
        if (reloaded != null){
            initialiseXrefs(reloaded.getDbXrefs());
            initialiseAnnotations(reloaded.getDbAnnotations());
            tree = buildTreeNode(reloaded, tree);
        }

        return tree;
    }

    private org.primefaces.model.TreeNode buildTreeNode( IntactCvTerm cv, org.primefaces.model.TreeNode node ) {

        org.primefaces.model.TreeNode childNode = null;
        // the parent is not null, we just append new child
        if (node != null){
            childNode = new DefaultTreeNode(cv, node);
        }
        // the parent is null. We add the root if no children
        else{
            childNode = new DefaultTreeNode();
            if (cv.getChildren().isEmpty()){
                buildTreeNode(cv, childNode);
            }
        }
        for ( OntologyTerm child : cv.getChildren() ) {
            if (child instanceof IntactCvTerm && cv.getObjClass().equals(((IntactCvTerm)child).getObjClass())){
                // load laxzy collections collections needed
                if (((IntactCvTerm) child).getAc() != null){
                    Hibernate.initialize(((IntactCvTerm)child).getDbAnnotations());
                    Hibernate.initialize(((IntactCvTerm)child).getDbXrefs());
                }
                buildTreeNode( (IntactCvTerm)child, childNode );
            }
        }

        return childNode;
    }

    public List<IntactCvTerm> getSortedTopicList( String key, Multimap<String, IntactCvTerm> topicMultimap ) {
        if ( topicMultimap.containsKey( key ) ) {
            List<IntactCvTerm> list = new ArrayList<IntactCvTerm>( topicMultimap.get( key ) );

            Collections.sort( list, new CvObjectComparator() );
            return list;
        } else {
            return new ArrayList<IntactCvTerm>();
        }
    }

    public List<IntactCvTerm> getSortedList( String key, Multimap<String, IntactCvTerm> classMultimap ) {
        if ( classMultimap.containsKey( key ) ) {
            List<IntactCvTerm> list = new ArrayList<IntactCvTerm>( classMultimap.get( key ) );
            Collections.sort( list, new CvObjectComparator() );
            return list;
        } else {
            return new ArrayList<IntactCvTerm>();
        }
    }

    private String[] findUsedInClass( IntactCvTerm cvObject ) {
        final Annotation annotation = AnnotationUtils.collectFirstAnnotationWithTopic(cvObject.getAnnotations(), null, USED_IN_CLASS);

        if ( annotation != null && annotation.getValue() != null) {
            String annotText = annotation.getValue();
            annotText = annotText.replaceAll( " ", "" );
            return annotText.split( "," );
        } else {
            return new String[0];
        }
    }

    public List<SelectItem> createSelectItems( Collection<IntactCvTerm> cvObjects, String noSelectionText ) {
        List<SelectItem> selectItems = new CopyOnWriteArrayList<SelectItem>();

        if ( noSelectionText != null ) {
            selectItems.add( new SelectItem( null, noSelectionText, noSelectionText, false, false, true ) );
        }

        for ( IntactCvTerm cvObject : cvObjects ) {
            selectItems.add( createSelectItem( cvObject ) );
        }

        return selectItems;
    }

    public List<SelectItem> createSelectItems(Collection<IntactCvTerm> cvObjects, String noSelectionText, String idPrefixToIgnore) {
        List<SelectItem> selectItems = new CopyOnWriteArrayList<SelectItem>();

        if ( noSelectionText != null ) {
            selectItems.add( new SelectItem( null, noSelectionText, noSelectionText, false, false, true ) );
        }

        for ( IntactCvTerm cvObject : cvObjects ) {
            boolean ignore = false;
            if (!cvObject.getIdentifiers().isEmpty()){
                for (Xref ref : cvObject.getIdentifiers()){
                    if (ref.getId().startsWith(idPrefixToIgnore)){
                        ignore = true;
                    }
                }
            }
            if (!ignore){
                selectItems.add( createSelectItem( cvObject ) );

            }
        }

        return selectItems;
    }

    public List<SelectItem> createExperimentalRoleSelectItems( Collection<IntactCvTerm> cvObjects, String noSelectionText ) {
        List<SelectItem> selectItems = new CopyOnWriteArrayList<SelectItem>();

        if ( noSelectionText != null ) {
            selectItems.add( new SelectItem( null, noSelectionText, noSelectionText, false, false, true ) );
        }

        for ( IntactCvTerm cvObject : cvObjects ) {
            selectItems.add( createSelectItem( cvObject ) );

            if (CvTermUtils.isCvTerm(cvObject, Participant.UNSPECIFIED_ROLE_MI, Participant.UNSPECIFIED_ROLE)){
                defaultExperimentalRole = cvObject;
            }
        }

        return selectItems;
    }

    public List<SelectItem> createBiologicalRoleSelectItems( Collection<IntactCvTerm> cvObjects, String noSelectionText ) {
        List<SelectItem> selectItems = new CopyOnWriteArrayList<SelectItem>();

        if ( noSelectionText != null ) {
            selectItems.add( new SelectItem( null, noSelectionText, noSelectionText, false, false, true ) );
        }

        for ( IntactCvTerm cvObject : cvObjects ) {
            selectItems.add( createSelectItem( cvObject ) );

            if (CvTermUtils.isCvTerm(cvObject, Participant.UNSPECIFIED_ROLE_MI, Participant.UNSPECIFIED_ROLE)){
                defaultBiologicalRole = cvObject;
            }
        }

        return selectItems;
    }

    private SelectItem createSelectItem( IntactCvTerm cv ) {

        boolean obsolete = !AnnotationUtils.collectAllAnnotationsHavingTopic(cv.getAnnotations(), OBSOLETE_MI_REF, OBSOLETE).isEmpty();
        return new SelectItem( cv, cv.getShortName()+((obsolete? " (obsolete)" : "")), cv.getFullName());
    }

    public IntactCvTerm findCvObjectByAc( String ac ) {
        return acCvObjectMap.get( ac );
    }

    public IntactCvTerm findCvObjectByIdentifier( String objClass, String identifier ) {
        return allCvObjectMap.get( new CvKey(identifier, objClass) );
    }

    public IntactCvTerm findCvObject( String clazz, String idOrLabel ) {
        CvKey keyId = new CvKey( idOrLabel, clazz );
        CvKey keyLabel = new CvKey( idOrLabel, clazz );

        if ( allCvObjectMap.containsKey( keyId ) ) {
            return allCvObjectMap.get( keyId );
        } else if ( allCvObjectMap.containsKey( keyLabel ) ) {
            return allCvObjectMap.get( keyLabel );
        }

        return null;
    }

    public class CvKey {
        private String id;
        private String className;

        private CvKey( String id, String clazz ) {
            this.id = id;
            this.className = clazz;
        }

        public String getId() {
            return id;
        }

        public String getClassName() {
            return className;
        }

        @Override
        public boolean equals( Object o ) {
            if ( this == o ) return true;
            if ( o == null || getClass() != o.getClass() ) return false;

            CvKey cvKey = ( CvKey ) o;

            if ( className != null ? !className.equals( cvKey.className ) : cvKey.className != null ) return false;
            if ( id != null ? !id.equals( cvKey.id ) : cvKey.id != null ) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + ( className != null ? className.hashCode() : 0 );
            return result;
        }
    }

    public List<SelectItem> getPublicationTopicSelectItems() {
        return publicationTopicSelectItems;
    }

    public List<SelectItem> getExperimentTopicSelectItems() {
        return experimentTopicSelectItems;
    }

    public List<SelectItem> getInteractionTopicSelectItems() {
        return interactionTopicSelectItems;
    }

    public List<SelectItem> getInteractorTopicSelectItems() {
        return interactorTopicSelectItems;
    }

    public List<SelectItem> getParticipantTopicSelectItems() {
        return participantTopicSelectItems;
    }

    public List<SelectItem> getFeatureTopicSelectItems() {
        return featureTopicSelectItems;
    }

    public List<SelectItem> getCvObjectTopicSelectItems() {
        return cvObjectTopicSelectItems;
    }

    public List<SelectItem> getComplexTopicSelectItems() {
        return complexTopicSelectItems;
    }

    public List<SelectItem> getFeatureRoleSelectItems() {
        return featureRoleSelectItems;
    }

    public List<SelectItem> getComplexTypeSelectItems() {
        return complexTypeSelectItems;
    }

    public List<SelectItem> getEvidenceTypeSelectItems() {
        return evidenceTypeSelectItems;
    }

    public List<SelectItem> getDatabaseSelectItems() {
        return databaseSelectItems;
    }

    public List<SelectItem> getQualifierSelectItems() {
        return qualifierSelectItems;
    }

    public List<SelectItem> getAliasTypeSelectItems() {
        return aliasTypeSelectItems;
    }

    public List<SelectItem> getInteractionDetectionMethodSelectItems() {
        return interactionDetectionMethodSelectItems;
    }

    public List<SelectItem> getParticipantDetectionMethodSelectItems() {
        return participantDetectionMethodSelectItems;
    }

    public List<SelectItem> getParticipantExperimentalPreparationsSelectItems() {
        return participantExperimentalPreparationsSelectItems;
    }

    public List<SelectItem> getInteractionTypeSelectItems() {
        return interactionTypeSelectItems;
    }

    public List<SelectItem> getInteractorTypeSelectItems() {
        return interactorTypeSelectItems;
    }

    public List<SelectItem> getFeatureDetectionMethodSelectItems() {
        return featureDetectionMethodSelectItems;
    }

    public List<SelectItem> getFeatureTypeSelectItems() {
        return featureTypeSelectItems;
    }

    public List<SelectItem> getBiologicalRoleSelectItems() {
        return biologicalRoleSelectItems;
    }

    public List<SelectItem> getExperimentalRoleSelectItems() {
        return experimentalRoleSelectItems;
    }

    public List<SelectItem> getFuzzyTypeSelectItems() {
        return fuzzyTypeSelectItems;
    }

    public List<SelectItem> getTissueSelectItems() {
        return tissueSelectItems;
    }

    public List<SelectItem> getCellTypeSelectItems() {
        return cellTypeSelectItems;
    }

    public List<SelectItem> getParameterTypeSelectItems() {
        return parameterTypeSelectItems;
    }

    public List<SelectItem> getParameterUnitSelectItems() {
        return parameterUnitSelectItems;
    }

    public List<SelectItem> getConfidenceTypeSelectItems() {
        return confidenceTypeSelectItems;
    }

    public List<SelectItem> getNoClassSelectItems() {
        return noClassSelectItems;
    }

    public IntactCvTerm getDefaultExperimentalRole() {
        return defaultExperimentalRole;
    }

    public IntactCvTerm getDefaultBiologicalRole() {
        return defaultBiologicalRole;
    }

    public List<SelectItem> getProteinTypeSelectItems() {
        return proteinTypeSelectItems;
    }

    public List<SelectItem> getBioactiveEntitySelectItems() {
        return bioactiveEntitySelectItems;
    }

    public List<SelectItem> getNucleicAcidSelectItems() {
        return nucleicAcidSelectItems;
    }

    public List<SelectItem> getPolymerTypeSelectItems() {
        return polymerTypeSelectItems;
    }

    public List<SelectItem> getMoleculeSetTypeSelectItems() {
        return moleculeSetTypeSelectItems;
    }

    public List<SelectItem> getGeneTypeSelectItems() {
        return geneTypeSelectItems;
    }

    public boolean isInitialised() {
        return isInitialised;
    }

    public static class CvObjectComparator implements Comparator<IntactCvTerm> {
        @Override
        public int compare( IntactCvTerm o1, IntactCvTerm o2 ) {
            if ( o1 == null ) {
                return 1;
            }

            if ( o2 == null ) {
                return -1;
            }

            return o1.getShortName().compareTo(o2.getShortName());
        }
    }

    public static class CvSelectItemComparator implements Comparator<SelectItem> {
        @Override
        public int compare( SelectItem o1, SelectItem o2 ) {
            if ( o1 == null ) {
                return 1;
            }

            if ( o2 == null ) {
                return -1;
            }

            return o1.getLabel().compareTo(o2.getLabel());
        }
    }

    private SelectItem createSelectItem( IntactCvTerm cv, boolean ignoreHidden ) {
        if (!ignoreHidden && AnnotationUtils.collectAllAnnotationsHavingTopic(cv.getAnnotations(), null, "hidden").isEmpty()){
            acCvObjectMap.put(cv.getAc(), cv);
            boolean obsolete = !AnnotationUtils.collectAllAnnotationsHavingTopic(cv.getAnnotations(), OBSOLETE_MI_REF, OBSOLETE).isEmpty();
            return new SelectItem( cv, cv.getShortName()+((obsolete? " (obsolete)" : "")),
                    cv.getFullName() + (cv.getMIIdentifier() != null ? "("+cv.getMIIdentifier()+")":""));
        }
        else if (ignoreHidden){
            acCvObjectMap.put(cv.getAc(), cv);
            boolean obsolete = !AnnotationUtils.collectAllAnnotationsHavingTopic(cv.getAnnotations(), OBSOLETE_MI_REF, OBSOLETE).isEmpty();
            return new SelectItem( cv, cv.getShortName()+((obsolete? " (obsolete)" : "")),
                    cv.getFullName()+ (cv.getMIIdentifier() != null ? "("+cv.getMIIdentifier()+")":""));
        }
        return null;
    }

    private List<String> loadChildren(IntactCvTerm parent, List<SelectItem> selectItems, boolean ignoreHidden, Set<String> processedAcs){
        List<String> list = new ArrayList<String>(parent.getChildren().size());
        for (OntologyTerm child : parent.getChildren()){
            IntactCvTerm cv = (IntactCvTerm)child;
            if (!processedAcs.contains(cv.getAc())){
                processedAcs.add(cv.getAc());
                SelectItem item = createSelectItem(cv, ignoreHidden);
                if (item != null){
                    list.add(cv.getAc());
                    selectItems.add(item);
                }
                if (!cv.getChildren().isEmpty()){
                    list.addAll(loadChildren(cv, selectItems, ignoreHidden, processedAcs));
                }
            }
        }
        return list;
    }

    private Set<IntactCvTerm> loadChildren(IntactCvTerm parent, boolean ignoreHidden, Set<String> processedAcs){
        Set<IntactCvTerm> list = new HashSet<IntactCvTerm>(parent.getChildren().size());
        for (OntologyTerm child : parent.getChildren()){
            IntactCvTerm cv = (IntactCvTerm)child;
            if (!processedAcs.contains(cv.getAc())){
                processedAcs.add(cv.getAc());
                if (!ignoreHidden && AnnotationUtils.collectAllAnnotationsHavingTopic(cv.getAnnotations(), null, "hidden").isEmpty()){
                    list.add(cv);
                }
                else if (ignoreHidden){
                    list.add(cv);
                }
                if (!cv.getChildren().isEmpty()){
                    list.addAll(loadChildren(cv, ignoreHidden, processedAcs));
                }
            }
        }
        return list;
    }

    private void initialiseParents(IntactCvTerm child, Collection<OntologyTerm> parents) {
        List<OntologyTerm> originalParents = new ArrayList<OntologyTerm>(parents);
        for (OntologyTerm r : originalParents){
            if (r instanceof IntactCvTerm && !isCvParentFullyLoaded((IntactCvTerm)r)){
                IntactCvTerm reloaded = getIntactDao().getEntityManager().find(IntactCvTerm.class, ((IntactCvTerm) r).getAc());
                if (reloaded != null){
                    initialiseXrefs(reloaded.getDbXrefs());
                    initialiseAnnotations(reloaded.getDbAnnotations());
                    Hibernate.initialize(reloaded.getChildren());
                    // detach reloaded object so it is not changed
                    getIntactDao().getEntityManager().detach(reloaded);
                    if (reloaded != r ){
                        parents.remove(r);
                        child.addParent(reloaded);
                    }
                }
            }
        }
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public synchronized void loadDataIfNecessary( ComponentSystemEvent event ) {
        if (!isInitialised()) {

            loadData();
        }
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public boolean isCvFullyLoaded(IntactCvTerm cv){
        if (cv == null){
            return true;
        }
        return !areCvCollectionsLazy(cv);
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public Collection<Annotation> initialiseCvAnnotations(IntactCvTerm releasable) {
        // reload complex without flushing changes
        IntactCvTerm reloaded = releasable;
        // merge current user because detached
        if (releasable.getAc() != null && !getIntactDao().getEntityManager().contains(releasable)){
            reloaded = getIntactDao().getEntityManager().find(IntactCvTerm.class, releasable.getAc());
            if (reloaded == null){
                reloaded = releasable;
            }
        }

        initialiseAnnotations(reloaded.getAnnotations());
        return reloaded.getAnnotations();
    }

    private boolean areCvCollectionsLazy(IntactCvTerm cv) {
        return !cv.areAnnotationsInitialized()
                || !cv.areParentsInitialized()
                || !cv.areXrefsInitialized()
                || !cv.areSynonymsInitialized();
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public IntactCvTerm reloadFullyInitialisedCv(IntactCvTerm cv) {
        if (cv == null){
            return null;
        }
        IntactCvTerm reloaded = null;
        if (areCvCollectionsLazy(cv)
                && cv.getAc() != null
                && !getIntactDao().getEntityManager().contains(cv)){
            reloaded = loadCvByAc(cv.getAc());
        }

        // we need first to merge with reloaded complex
        if (reloaded != null){
            // detach reloaded now so not changes will be committed
            getIntactDao().getEntityManager().detach(reloaded);
            uk.ac.ebi.intact.editor.controller.curate.cloner.CvTermCloner cloner = new uk.ac.ebi.intact.editor.controller.curate.cloner.CvTermCloner();
            cloner.copyInitialisedProperties(cv, reloaded);
            cv = reloaded;
        }

        // initialise xrefs because are first tab visible
        initialiseXrefs(cv.getDbXrefs());
        // initialise annotations because needs caution
        initialiseAnnotations(cv.getDbAnnotations());
        // initialise aliases
        initialiseAliases(cv.getSynonyms());
        // initialise parents
        initialiseParents(cv, cv.getParents());

        return cv;
    }

    private boolean isCvParentFullyLoaded(IntactCvTerm cv){
        if (cv == null){
            return true;
        }
        if (!cv.areAnnotationsInitialized()
                || !cv.areChildrenInitialized()
                || !cv.areXrefsInitialized()){
            return false;
        }
        return true;
    }
}
