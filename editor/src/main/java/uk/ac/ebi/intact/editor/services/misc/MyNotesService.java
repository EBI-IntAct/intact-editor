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
package uk.ac.ebi.intact.editor.services.misc;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.editor.controller.admin.UserAdminController;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.editor.util.LazyDataModelFactory;
import uk.ac.ebi.intact.jami.model.IntactPrimaryObject;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.model.user.Preference;
import uk.ac.ebi.intact.jami.model.user.User;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import javax.faces.model.DataModel;


@Service
@Scope( BeanDefinition.SCOPE_PROTOTYPE )
public class MyNotesService extends AbstractEditorService {

    public MyNotesService() {
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public void saveNotes(User user, String rawNotes) throws SynchronizerException, FinderException, PersisterException {

        Preference pref = user.getPreference(UserAdminController.RAW_NOTES);
        pref.setValue(rawNotes);

        updateIntactObject(user, getIntactDao().getUserDao());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public DataModel createDataModel(String hqlQuery) {
        return LazyDataModelFactory.createLazyDataModel(getIntactDao().getEntityManager(), hqlQuery);
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public Class<? extends IntactPrimaryObject> loadClassFromAc(String ac){
        IntactPrimaryObject primary = getIntactDao().getEntityManager().find(IntactPublication.class, ac);
        if (primary == null){
            primary = getIntactDao().getEntityManager().find(IntactExperiment.class, ac);
            if (primary == null){
                primary = getIntactDao().getEntityManager().find(IntactInteractionEvidence.class, ac);

                if (primary == null){
                    primary = getIntactDao().getEntityManager().find(IntactComplex.class, ac);

                    if (primary == null){
                        primary = getIntactDao().getEntityManager().find(IntactParticipantEvidence.class, ac);

                        if (primary == null){
                            primary = getIntactDao().getEntityManager().find(IntactModelledParticipant.class, ac);

                            if (primary == null){
                                primary = getIntactDao().getEntityManager().find(IntactFeatureEvidence.class, ac);

                                if (primary == null){
                                    primary = getIntactDao().getEntityManager().find(IntactModelledFeature.class, ac);

                                    if (primary == null){
                                        primary = getIntactDao().getEntityManager().find(IntactInteractor.class, ac);

                                        if (primary == null){
                                            primary = getIntactDao().getEntityManager().find(IntactOrganism.class, ac);

                                            if (primary == null){
                                                primary = getIntactDao().getEntityManager().find(IntactCvTerm.class, ac);

                                                if (primary == null){
                                                    primary = getIntactDao().getEntityManager().find(IntactSource.class, ac);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return primary != null ? primary.getClass() : null;
    }
}
