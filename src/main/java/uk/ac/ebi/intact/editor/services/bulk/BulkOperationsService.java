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
package uk.ac.ebi.intact.editor.services.bulk;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.Annotation;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.jami.model.IntactPrimaryObject;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Service to do bulk operations on the database.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Service
@Scope( BeanDefinition.SCOPE_PROTOTYPE )
public class BulkOperationsService extends AbstractEditorService {

    /**
     * Adds an annotation to all the passed annotated object ACs. The annotation is not shared and it is basically
     * a clone from the passed annotation.
     * @param annotation the annotation to copy to the annotated objects
     * @param acs array of ACs to modify
     * @param aoClass type of class of the ACs
     * @return the accessions that have been modified
     */
    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public String[] addAnnotation(Annotation annotation, String[] acs, Class<? extends IntactPrimaryObject> aoClass, boolean replaceIfTopicMatch) throws
            SynchronizerException, FinderException, PersisterException {
        attachDaoToTransactionManager();
        Collection<String> updatedAcs = new ArrayList<String>(acs.length);

        for (String ac : acs) {
            IntactPrimaryObject ao = getIntactDao().getEntityManager().find(aoClass, ac);

            if (ao != null) {

                if (IntactPublication.class.getName().equals(aoClass.getName())) {
                    updatedAcs.add(ac);

                    IntactPublication intactPublication =(IntactPublication)ao;
                    if (!replaceIfTopicMatch) {
                        AnnotationUtils.removeAllAnnotationsWithTopic(intactPublication.getAnnotations(), annotation.getTopic().getMIIdentifier(),
                                annotation.getTopic().getShortName());
                        intactPublication.getAnnotations().add(annotation);
                    }
                    else {
                        Annotation existingAnnot = AnnotationUtils.collectFirstAnnotationWithTopic(intactPublication.getAnnotations(),
                                annotation.getTopic().getMIIdentifier(),
                                annotation.getTopic().getShortName());

                        if (existingAnnot != null) {
                            existingAnnot.setValue(annotation.getValue());
                        } else {
                            intactPublication.getAnnotations().add(annotation);
                        }
                    }

                    updateIntactObject(intactPublication, getIntactDao().getPublicationDao());
                } else if (IntactExperiment.class.getName().equals(aoClass.getName())) {
                    updatedAcs.add(ac);

                    IntactExperiment intactExperiment =(IntactExperiment)ao;
                    if (!replaceIfTopicMatch) {
                        AnnotationUtils.removeAllAnnotationsWithTopic(intactExperiment.getAnnotations(), annotation.getTopic().getMIIdentifier(),
                                annotation.getTopic().getShortName());
                        intactExperiment.getAnnotations().add(annotation);
                    }
                    else {
                        Annotation existingAnnot = AnnotationUtils.collectFirstAnnotationWithTopic(intactExperiment.getAnnotations(),
                                annotation.getTopic().getMIIdentifier(),
                                annotation.getTopic().getShortName());

                        if (existingAnnot != null) {
                            existingAnnot.setValue(annotation.getValue());
                        } else {
                            intactExperiment.getAnnotations().add(annotation);
                        }
                    }

                    updateIntactObject(intactExperiment, getIntactDao().getExperimentDao());
                } else if (IntactInteractionEvidence.class.getName().equals(aoClass.getName())) {
                    updatedAcs.add(ac);

                    IntactInteractionEvidence intactInteraction =(IntactInteractionEvidence)ao;
                    if (!replaceIfTopicMatch) {
                        AnnotationUtils.removeAllAnnotationsWithTopic(intactInteraction.getAnnotations(), annotation.getTopic().getMIIdentifier(),
                                annotation.getTopic().getShortName());
                        intactInteraction.getAnnotations().add(annotation);
                    }
                    else {
                        Annotation existingAnnot = AnnotationUtils.collectFirstAnnotationWithTopic(intactInteraction.getAnnotations(),
                                annotation.getTopic().getMIIdentifier(),
                                annotation.getTopic().getShortName());

                        if (existingAnnot != null) {
                            existingAnnot.setValue(annotation.getValue());
                        } else {
                            intactInteraction.getAnnotations().add(annotation);
                        }
                    }

                    updateIntactObject(intactInteraction, getIntactDao().getInteractionDao());
                } else if (IntactInteractor.class.isAssignableFrom(aoClass)) {
                    updatedAcs.add(ac);

                    IntactInteractor intactInteractor =(IntactInteractor)ao;
                    if (!replaceIfTopicMatch) {
                        AnnotationUtils.removeAllAnnotationsWithTopic(intactInteractor.getAnnotations(), annotation.getTopic().getMIIdentifier(),
                                annotation.getTopic().getShortName());
                        intactInteractor.getAnnotations().add(annotation);
                    }
                    else {
                        Annotation existingAnnot = AnnotationUtils.collectFirstAnnotationWithTopic(intactInteractor.getAnnotations(),
                                annotation.getTopic().getMIIdentifier(),
                                annotation.getTopic().getShortName());

                        if (existingAnnot != null) {
                            existingAnnot.setValue(annotation.getValue());
                        } else {
                            intactInteractor.getAnnotations().add(annotation);
                        }
                    }

                    synchronizeIntactObject(intactInteractor, getIntactDao().getSynchronizerContext().getInteractorSynchronizer(), true);
                } else if (AbstractIntactParticipant.class.isAssignableFrom(aoClass)) {
                    updatedAcs.add(ac);

                    AbstractIntactParticipant intactInteractor =(AbstractIntactParticipant)ao;
                    if (!replaceIfTopicMatch) {
                        AnnotationUtils.removeAllAnnotationsWithTopic(intactInteractor.getAnnotations(), annotation.getTopic().getMIIdentifier(),
                                annotation.getTopic().getShortName());
                        intactInteractor.getAnnotations().add(annotation);
                    }
                    else {
                        Annotation existingAnnot = AnnotationUtils.collectFirstAnnotationWithTopic(intactInteractor.getAnnotations(),
                                annotation.getTopic().getMIIdentifier(),
                                annotation.getTopic().getShortName());

                        if (existingAnnot != null) {
                            existingAnnot.setValue(annotation.getValue());
                        } else {
                            intactInteractor.getAnnotations().add(annotation);
                        }
                    }

                    synchronizeIntactObject(intactInteractor, getIntactDao().getSynchronizerContext().getParticipantSynchronizer(), true);
                } else if (AbstractIntactFeature.class.isAssignableFrom(aoClass)) {
                    updatedAcs.add(ac);

                    AbstractIntactFeature intactFeature =(AbstractIntactFeature)ao;
                    if (!replaceIfTopicMatch) {
                        AnnotationUtils.removeAllAnnotationsWithTopic(intactFeature.getAnnotations(), annotation.getTopic().getMIIdentifier(),
                                annotation.getTopic().getShortName());
                        intactFeature.getAnnotations().add(annotation);
                    }
                    else {
                        Annotation existingAnnot = AnnotationUtils.collectFirstAnnotationWithTopic(intactFeature.getAnnotations(),
                                annotation.getTopic().getMIIdentifier(),
                                annotation.getTopic().getShortName());

                        if (existingAnnot != null) {
                            existingAnnot.setValue(annotation.getValue());
                        } else {
                            intactFeature.getAnnotations().add(annotation);
                        }
                    }

                    synchronizeIntactObject(intactFeature, getIntactDao().getSynchronizerContext().getFeatureSynchronizer(), true);
                } else if (IntactCvTerm.class.isAssignableFrom(aoClass)) {
                    updatedAcs.add(ac);

                    IntactCvTerm intactCv =(IntactCvTerm)ao;
                    if (!replaceIfTopicMatch) {
                        AnnotationUtils.removeAllAnnotationsWithTopic(intactCv.getAnnotations(), annotation.getTopic().getMIIdentifier(),
                                annotation.getTopic().getShortName());
                        intactCv.getAnnotations().add(annotation);
                    }
                    else {
                        Annotation existingAnnot = AnnotationUtils.collectFirstAnnotationWithTopic(intactCv.getAnnotations(),
                                annotation.getTopic().getMIIdentifier(),
                                annotation.getTopic().getShortName());

                        if (existingAnnot != null) {
                            existingAnnot.setValue(annotation.getValue());
                        } else {
                            intactCv.getAnnotations().add(annotation);
                        }
                    }

                    updateIntactObject(intactCv, getIntactDao().getCvTermDao());
                } else if (IntactSource.class.isAssignableFrom(aoClass)) {
                    updatedAcs.add(ac);

                    IntactSource intactCv =(IntactSource)ao;
                    if (!replaceIfTopicMatch) {
                        AnnotationUtils.removeAllAnnotationsWithTopic(intactCv.getAnnotations(), annotation.getTopic().getMIIdentifier(),
                                annotation.getTopic().getShortName());
                        intactCv.getAnnotations().add(annotation);
                    }
                    else {
                        Annotation existingAnnot = AnnotationUtils.collectFirstAnnotationWithTopic(intactCv.getAnnotations(),
                                annotation.getTopic().getMIIdentifier(),
                                annotation.getTopic().getShortName());

                        if (existingAnnot != null) {
                            existingAnnot.setValue(annotation.getValue());
                        } else {
                            intactCv.getAnnotations().add(annotation);
                        }
                    }

                    updateIntactObject(intactCv, getIntactDao().getSourceDao());
                }
            }
        }

        return updatedAcs.toArray(new String[updatedAcs.size()]);

    }
}
