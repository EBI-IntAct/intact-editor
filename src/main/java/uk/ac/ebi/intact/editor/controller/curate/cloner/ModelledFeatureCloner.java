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
package uk.ac.ebi.intact.editor.controller.curate.cloner;

import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.utils.XrefUtils;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.context.IntactConfiguration;
import uk.ac.ebi.intact.jami.dao.IntactDao;
import uk.ac.ebi.intact.jami.model.extension.*;

/**
 * Editor specific cloning routine for complex features.
 *
 * @author Prem Anand (prem@ebi.ac.uk)
 * @version $Id: InteractionIntactCloner.java 14783 2010-07-29 12:52:28Z brunoaranda $
 * @since 2.0.1-SNAPSHOT
 */
public class ModelledFeatureCloner extends AbstractEditorCloner<Feature, IntactModelledFeature> {


    public IntactModelledFeature clone(Feature feature, IntactDao dao) {
        IntactModelledFeature clone = new IntactModelledFeature();

        initAuditProperties(clone, dao);

        clone.setShortName(feature.getShortName());
        clone.setFullName(feature.getFullName());
        clone.setRole(feature.getRole());
        clone.setType(feature.getType());
        if (feature.getParticipant() instanceof ModelledParticipant){
            clone.setParticipant((ModelledParticipant)feature.getParticipant());
        }

        for (Object obj : feature.getAliases()){
            Alias alias = (Alias)obj;
            clone.getAliases().add(new ModelledFeatureAlias(alias.getType(), alias.getName()));
        }

        IntactConfiguration config = ApplicationContextProvider.getBean("intactJamiConfiguration");
        for (Object obj : feature.getIdentifiers()){
            Xref ref = (Xref)obj;
            // exclude feature accession
            if (!XrefUtils.isXrefFromDatabase(ref, config.getDefaultInstitution().getMIIdentifier(), config.getDefaultInstitution().getShortName())
                    || !XrefUtils.doesXrefHaveQualifier(ref, Xref.IDENTITY_MI, Xref.IDENTITY)){
                AbstractIntactXref intactRef = new ModelledFeatureXref(ref.getDatabase(), ref.getId(), ref.getVersion(), ref.getQualifier());
                if (ref instanceof AbstractIntactXref){
                    intactRef.setSecondaryId(((AbstractIntactXref) ref).getSecondaryId());
                }
                clone.getIdentifiers().add(intactRef);
            }
        }
        for (Object obj : feature.getXrefs()){
            Xref ref = (Xref)obj;
            AbstractIntactXref intactRef = new ModelledFeatureXref(ref.getDatabase(), ref.getId(), ref.getVersion(), ref.getQualifier());
            if (ref instanceof AbstractIntactXref){
                intactRef.setSecondaryId(((AbstractIntactXref) ref).getSecondaryId());
            }
            clone.getXrefs().add(intactRef);
        }

        for (Object obj : feature.getAnnotations()){
            Annotation annotation = (Annotation)obj;
            clone.getAnnotations().add(new ModelledFeatureAnnotation(annotation.getTopic(), annotation.getValue()));
        }

        for (Object obj : feature.getRanges()){
            Range range = (Range)obj;
            ModelledRange r = new ModelledRange(range.getStart(), range.getEnd(), range.isLink());
            r.setResultingSequence(new ModelledResultingSequence(range.getResultingSequence().getOriginalSequence(), range.getResultingSequence().getNewSequence()));

            for (Object obj2 : range.getResultingSequence().getXrefs()){
                Xref ref = (Xref)obj2;
                r.getResultingSequence().getXrefs().add(new ModelledResultingSequenceXref(ref.getDatabase(), ref.getId(), ref.getVersion(), ref.getQualifier()));
            }

            clone.getRanges().add(r);
        }

        // don't need to add it to the feature component because it is already done by the cloner
        return clone;
    }

    @Override
    public void copyInitialisedProperties(IntactModelledFeature source, IntactModelledFeature target) {
        target.setShortName(source.getShortName());
        target.setFullName(source.getFullName());
        target.setRole(source.getRole());
        target.setType(source.getType());
        target.setParticipant(source.getParticipant());

        if (source.areAliasesInitialized()){
            target.getAliases().clear();
            target.getAliases().addAll(source.getAliases());
        }

        if (source.areXrefsInitialized()){
            target.getIdentifiers().clear();
            target.getIdentifiers().addAll(source.getIdentifiers());
            target.getXrefs().clear();
            target.getXrefs().addAll(source.getXrefs());
        }

        if (source.areAnnotationsInitialized()){
            target.getAnnotations().clear();
            target.getAnnotations().addAll(source.getAnnotations());
        }

        if (source.areRangesInitialized()){
            target.getRanges().clear();
            target.getRanges().addAll(source.getRanges());
        }
    }
}

