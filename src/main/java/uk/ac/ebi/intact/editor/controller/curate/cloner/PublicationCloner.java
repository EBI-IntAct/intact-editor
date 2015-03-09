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

import psidev.psi.mi.jami.model.Annotation;
import psidev.psi.mi.jami.model.Publication;
import psidev.psi.mi.jami.model.Xref;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import psidev.psi.mi.jami.utils.XrefUtils;
import uk.ac.ebi.intact.editor.controller.UserSessionController;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.dao.IntactDao;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.model.lifecycle.Releasable;

/**
 * Editor specific cloning routine for publication.
 *
 * @author Prem Anand (prem@ebi.ac.uk)
 * @version $Id: InteractionIntactCloner.java 14783 2010-07-29 12:52:28Z brunoaranda $
 * @since 2.0.1-SNAPSHOT
 */
public class PublicationCloner extends AbstractEditorCloner<Publication, IntactPublication> {

    public PublicationCloner() {
    }

    public IntactPublication clone(Publication publication, IntactDao dao) {
        IntactPublication clone = new IntactPublication();

        initAuditProperties(clone, dao);

        clone.setJournal(publication.getJournal());
        clone.setCurationDepth(publication.getCurationDepth());
        clone.getAuthors().addAll(publication.getAuthors());
        clone.setPublicationDate(publication.getPublicationDate());
        UserSessionController userSessionController = ApplicationContextProvider.getBean("userSessionController");
        clone.setSource(userSessionController.getUserInstitution());

        for (Xref ref : publication.getXrefs()){
            if (!(XrefUtils.doesXrefHaveQualifier(ref, Xref.IMEX_PRIMARY_MI, Xref.IMEX_PRIMARY)
                    && XrefUtils.isXrefFromDatabase(ref, Xref.IMEX_MI, Xref.IMEX))){
                AbstractIntactXref intactRef = new PublicationXref(ref.getDatabase(), ref.getId(), ref.getVersion(), ref.getQualifier());
                if (ref instanceof AbstractIntactXref){
                    intactRef.setSecondaryId(((AbstractIntactXref) ref).getSecondaryId());
                }
                clone.getXrefs().add(intactRef);
            }
        }

        for (Annotation annotation : publication.getAnnotations()){
            if (!AnnotationUtils.doesAnnotationHaveTopic(annotation, null, Releasable.CORRECTION_COMMENT)
                    && !AnnotationUtils.doesAnnotationHaveTopic(annotation, null, Releasable.ON_HOLD)
                    && !AnnotationUtils.doesAnnotationHaveTopic(annotation, null, Releasable.TO_BE_REVIEWED)
                    && !AnnotationUtils.doesAnnotationHaveTopic(annotation, null, Releasable.ACCEPTED) ){
                clone.getAnnotations().add(new PublicationAnnotation(annotation.getTopic(), annotation.getValue()));
            }
        }

        return clone;
    }

    @Override
    public void copyInitialisedProperties(IntactPublication source, IntactPublication target) {
        target.setShortLabel(source.getShortLabel());
        target.setJournal(source.getJournal());
        target.setCurationDepth(source.getCurationDepth());
        target.getAuthors().addAll(source.getAuthors());
        target.setPublicationDate(source.getPublicationDate());

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

        if (source.areExperimentsInitialized()){
            target.getExperiments().clear();
            target.addAllExperiments(source.getExperiments());
        }
    }
}

