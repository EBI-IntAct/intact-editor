/**
 * Copyright 2011 The European Bioinformatics Institute, and others.
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
package uk.ac.ebi.intact.editor.services.admin;

import org.primefaces.model.DualListModel;
import org.primefaces.model.SelectableDataModelWrapper;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.Source;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.editor.services.UserSessionService;
import uk.ac.ebi.intact.editor.services.curate.institution.InstitutionService;
import uk.ac.ebi.intact.editor.util.SelectableCollectionDataModel;
import uk.ac.ebi.intact.jami.model.extension.IntactSource;
import uk.ac.ebi.intact.jami.service.ComplexService;
import uk.ac.ebi.intact.jami.service.PublicationService;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import javax.annotation.Resource;
import javax.faces.model.DataModel;
import javax.faces.model.SelectItem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Service
@Scope( BeanDefinition.SCOPE_PROTOTYPE )
public class InstitutionAdminService extends AbstractEditorService {

    @Resource(name = "complexService")
    private ComplexService complexService;

    @Resource(name = "publicationService")
    private PublicationService publicationService;

    @Resource(name = "institutionService")
    private InstitutionService institutionService;

    @Resource(name = "userSessionService")
    private UserSessionService userSessionService;

    public InstitutionAdminService() {

    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public DataModel<IntactSource> getAllInstitutions() {
        final List<IntactSource> intactSources = getIntactDao().getSourceDao().getAll();
        return new SelectableDataModelWrapper(new SelectableCollectionDataModel<IntactSource>(intactSources), intactSources);
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public DualListModel<uk.ac.ebi.intact.jami.model.user.User> createUserDualListModel(List<uk.ac.ebi.intact.jami.model.user.User> selection) {
        final List<uk.ac.ebi.intact.jami.model.user.User> users = getIntactDao().getUserDao().getAll();
        return new DualListModel<uk.ac.ebi.intact.jami.model.user.User>(users, selection);
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public int mergeSelected(IntactSource mergeDestinationInstitution, IntactSource[] selectedInstitutions) throws SynchronizerException,
            FinderException, PersisterException {
        return merge(selectedInstitutions, mergeDestinationInstitution, true);
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public void deleteSelected(IntactSource[] selectedInstitutions) throws SynchronizerException, FinderException, PersisterException {
        for (IntactSource selectedInstitution : selectedInstitutions) {
            deleteIntactObject(selectedInstitution, getIntactDao().getSourceDao());
        }

        institutionService.loadInstitutions();
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public int fixReleasableOwners(List<uk.ac.ebi.intact.jami.model.user.User> selectedUsers) throws PersisterException{

        int updated = 0;
        for (uk.ac.ebi.intact.jami.model.user.User selected : selectedUsers){
            Source userInstitution = userSessionService.getUserInstitution(selected);

            if (userInstitution instanceof IntactSource) {

                try{
                    updated += publicationService.replaceSource((IntactSource)userInstitution, selected.getLogin());
                    updated += complexService.replaceSource((IntactSource)userInstitution, selected.getLogin());
                }
                catch (Throwable e){
                     throw new PersisterException("Cannot update institutions ", e);
                }
            }
        }

        institutionService.loadInstitutions();
        return updated;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true,propagation = Propagation.REQUIRED)
    public String getInstitutionNameFor(uk.ac.ebi.intact.jami.model.user.User user) {
        Source source = userSessionService.getUserInstitution(user);

        return source != null ? source.getShortName() : "-";
    }

    public List<SelectItem> getInstitutionItems() {
        if (!institutionService.isInitialised()){
            institutionService.loadInstitutions();
        }
        return institutionService.getInstitutionSelectItems();
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countNumberPublicationsFor(String institutionAc) {
        Map<String,Object> parameters = new HashMap<String,Object>(1);
        parameters.put("ac", institutionAc);
        return (int)getIntactDao().getPublicationDao().countByQuery("select count(distinct p.ac) from IntactPublication p join p.source as s " +
                "where s.ac = :ac", parameters);
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countNumberExperimentsFor(String institutionAc) {
        Map<String,Object> parameters = new HashMap<String,Object>(1);
        parameters.put("ac", institutionAc);
        return (int)getIntactDao().getPublicationDao().countByQuery("select count(distinct e.ac) from IntactExperiment e join e.publication as p " +
                "join p.source as s " +
                "where s.ac = :ac", parameters);
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countNumberInteractionsFor(String institutionAc) {
        Map<String,Object> parameters = new HashMap<String,Object>(1);
        parameters.put("ac", institutionAc);
        return (int)getIntactDao().getPublicationDao().countByQuery("select count(distinct i.ac) from IntactInteractionEvidence i join i.dbExperiments as e " +
                "join e.publication as p join p.source as s " +
                "where s.ac = :ac", parameters);
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public int countNumberComplexesFor(String institutionAc) {
        Map<String,Object> parameters = new HashMap<String,Object>(1);
        parameters.put("ac", institutionAc);
        return (int)getIntactDao().getPublicationDao().countByQuery("select count(distinct p.ac) from IntactComplex p join p.source as s " +
                "where s.ac = :ac", parameters);
    }

    private String findPreference(uk.ac.ebi.intact.jami.model.user.User user, String prefKey, String defaultValue) {
        if (user.getPreferences() == null){
           return null;
        }
        for (uk.ac.ebi.intact.jami.model.user.Preference pref : user.getPreferences()) {
            if (prefKey.equals(pref.getKey())) {
                return pref.getValue();
            }
        }
        return defaultValue;
    }

    private int merge(IntactSource[] sourceInstitutions, IntactSource destinationInstitution, boolean removeMerged) throws SynchronizerException,
            FinderException, PersisterException {
        int updated = 0;
        for (IntactSource selected : sourceInstitutions){
            if (destinationInstitution.getAc() != null && !destinationInstitution.getAc().equals(selected.getAc())){
                updated += publicationService.replaceSource(selected, destinationInstitution);
                updated += complexService.replaceSource(selected, destinationInstitution);

                if (removeMerged){
                    deleteIntactObject(selected, getIntactDao().getSourceDao());
                }
            }
        }

        institutionService.loadInstitutions();

        return updated;
    }
}
