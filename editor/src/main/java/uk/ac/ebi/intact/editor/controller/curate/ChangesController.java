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
package uk.ac.ebi.intact.editor.controller.curate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import uk.ac.ebi.intact.editor.controller.BaseController;
import uk.ac.ebi.intact.editor.controller.UserListener;
import uk.ac.ebi.intact.editor.controller.UserSessionController;
import uk.ac.ebi.intact.editor.services.curate.EditorObjectService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.IntactPrimaryObject;
import uk.ac.ebi.intact.jami.model.user.User;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.IntactDbSynchronizer;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import javax.annotation.Resource;
import javax.faces.event.ActionEvent;
import java.util.*;

/**
 * Contains the information about current changes.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Component
public class ChangesController extends BaseController implements UserListener {

    private static final Log log = LogFactory.getLog(ChangesController.class);

    @Resource(name = "editorObjectService")
    private transient EditorObjectService editorService;

    /**
     * Map containing the user name as the key, and a list with his/her changes.
     */
    private Map<String,List<UnsavedChange>> changesPerUser;
    /**
     * Map containing the user name as the key, and a list with his/her changes which are behind the scene and should be hidden from the user.
     */
    private Map<String,List<UnsavedChange>> hiddenChangesPerUser;

    public ChangesController() {
        changesPerUser = new HashMap<String, List<UnsavedChange>>();
        hiddenChangesPerUser = new HashMap<String, List<UnsavedChange>>();
    }

    @Override
    public void userLoggedIn(User user) {
        if (user == null) return;

        changesPerUser.put(user.getLogin(), new ArrayList<UnsavedChange>());
        hiddenChangesPerUser.put(user.getLogin(), new ArrayList<UnsavedChange>());
    }

    @Override
    public void userLoggedOut(User user) {
        userLoggedOut(user.getLogin());
    }

    private void userLoggedOut(String user) {
        final List<UnsavedChange> unsavedChanges = getUnsavedChangesForUser(user);
        final List<UnsavedChange> hiddenUnsavedChanges = getHiddenUnsavedChangesForUser(user);

        if (unsavedChanges == null || hiddenUnsavedChanges == null) {
            throw new IllegalStateException("No unsaved changes found for user: "+user);
        }

        if (!unsavedChanges.isEmpty()) {
            int totalChanges = unsavedChanges.size();
            if (log.isInfoEnabled()) log.info("User logged out with "+totalChanges+" pending changes: "+user);

            unsavedChanges.clear();
        }

        if (!hiddenUnsavedChanges.isEmpty()) {
            int totalChanges = hiddenUnsavedChanges.size();
            if (log.isInfoEnabled()) log.info("User logged out with "+totalChanges+" hidden pending changes: "+user);

            hiddenUnsavedChanges.clear();
        }

        removeUserFromUnsaved(user);
        removeUserFromHiddenUnsaved(user);
    }

    public void markAsUnsaved(IntactPrimaryObject io, IntactDbSynchronizer dbSynchronizer,
                              String description, Collection<String> parentAcs) {
        if (io == null) return;

        UnsavedChange change;
        if (io.getAc() != null) {
            change = new UnsavedChange(io, UnsavedChange.UPDATED, null, dbSynchronizer, description);
        } else {
            change = new UnsavedChange(io, UnsavedChange.CREATED, null, dbSynchronizer, description);
        }

        change.getAcsToDeleteOn().addAll(parentAcs);
        addUnsavedChange(change);
    }

    public boolean markToDelete(IntactPrimaryObject object, IntactPrimaryObject parent,
                             IntactDbSynchronizer dbSynchronizer, String description,
                             Collection<String> parentAcs) {
        if (object.getAc() != null) {

            String scope;

            if (parent != null && parent.getAc() != null){
                scope = parent.getAc();
                parentAcs.add(scope);
            }
            else {
                scope = null;
            }

            // very important to delete all changes which can be affected by the delete of this object!!!
            removeObsoleteChangesOnDelete(object);

            // collect parent acs for this intact object if possible
            UnsavedChange change = new UnsavedChange(object, UnsavedChange.DELETED, parent, scope, dbSynchronizer, description);
            change.getAcsToDeleteOn().addAll(parentAcs);

            addChange(change);
            return true;
        }
        return false;
    }

    /**
     * When deleting an object, all save/created/deleted events attached to one of the children of this object became obsolete because will be deleted with the current object
     * @param object
     */
    public void removeObsoleteChangesOnDelete(IntactPrimaryObject object){
        if (object.getAc() != null){

            List<UnsavedChange> changes = new ArrayList(getUnsavedChangesForCurrentUser());
            for (UnsavedChange change : changes){

                if (change.getAcsToDeleteOn().contains(object.getAc())){
                    getUnsavedChangesForCurrentUser().remove(change);
                }
            }

            List<UnsavedChange> changes2 = new ArrayList(getHiddenUnsavedChangesForCurrentUser());
            for (UnsavedChange change : changes2){

                if (change.getScope().contains(object.getAc())){
                    getHiddenUnsavedChangesForCurrentUser().remove(change);
                }
            }
        }
    }

    /**
     * When saving an object, all save/created/deleted events attached to one of the children (or parent) of this object became obsolete because will be updated with the current object.
     * However, in case of new publication, new experiment, new interaction, new participant, new feature, it is important to keep the change as it will not be created while updating this event.
     * New objects are only attached to their parents if saved. So when saving the parent, it will not create the child because not added yet
     *
     * @param object
     */
    public void removeObsoleteChangesOnSave(IntactPrimaryObject object, Collection<String> parentAcs){
        if (object.getAc() != null){

            List<UnsavedChange> changes = new ArrayList(getUnsavedChangesForCurrentUser());
            for (UnsavedChange change : changes){

                // very important to check that the ac is not null. Any new children event is not obsolete after saving the parent because not added yet
                // checks the unsaved change is not new and is not a children of the current object to save. If it is a children, the unsaved event of the children becomes obsolete because the parent object will be saved
                if (change.getAcsToDeleteOn().contains(object.getAc()) && change.getUnsavedObject().getAc() != null){
                    getUnsavedChangesForCurrentUser().remove(change);
                }
                // we gave a list of parent acs for this object. It means that we want to remove all unsaved change event which refers to one of the parent acs
                else if (!parentAcs.isEmpty()){
                    // the save event concerns one of the parent of the current object being saved, we can remove this unsaved event as it will be saved with current object
                    if (change.getUnsavedObject().getAc() != null && parentAcs.contains(change.getUnsavedObject().getAc())){
                        getUnsavedChangesForCurrentUser().remove(change);
                    }
                }
            }
        }
    }

    public void markAsHiddenChange(IntactPrimaryObject object, IntactPrimaryObject parent, Collection<String> contextAcs,
                                   IntactDbSynchronizer dbSynchronizer, String description) {
        String scope;

        if (parent != null && parent.getAc() != null){
            scope = parent.getAc();
            contextAcs.add(scope);
        }
        else {
            scope = null;
        }
        UnsavedChange change = new UnsavedChange(object, UnsavedChange.CREATED_TRANSCRIPT, scope, dbSynchronizer, description);
        change.getAcsToDeleteOn().addAll(contextAcs);
        addUnsavedHiddenChange(change);
    }

    /**
     * When removing a save event from unsaved events, we have to refresh the unsaved events which have been saved while saving this specific change
     * @param io
     */
    public void removeFromUnsaved(IntactPrimaryObject io, Collection<String> parentAcs) {
        List<UnsavedChange> changes = getUnsavedChangesForCurrentUser();

        changes.remove(new UnsavedChange(io, UnsavedChange.CREATED, null, null, null));
        changes.remove(new UnsavedChange(io, UnsavedChange.UPDATED, null, null, null));

        removeObsoleteChangesOnSave(io, parentAcs);
    }

    public void removeFromHiddenChanges(UnsavedChange unsavedChange) {
        getHiddenUnsavedChangesForCurrentUser().remove(unsavedChange);
    }

    public void removeFromDeleted(UnsavedChange unsavedChange) {
        getUnsavedChangesForCurrentUser().remove(unsavedChange);
        removeObsoleteChangesOnDelete(unsavedChange.getUnsavedObject());
    }

    public void removeFromDeleted(IntactPrimaryObject object) {

        getUnsavedChangesForCurrentUser().remove(new UnsavedChange(object, UnsavedChange.DELETED, null, null, null, null));
        removeObsoleteChangesOnDelete(object);
    }

    public void revert(IntactPrimaryObject io) {
        Iterator<UnsavedChange> iterator = getUnsavedChangesForCurrentUser().iterator();

        Collection<UnsavedChange> additionnalUnsavedEventToRevert = new ArrayList<UnsavedChange>(getUnsavedChangesForCurrentUser().size());

        // removed the passed object from the list of unsaved changes. If this object is the scope of another change as well, delete it
        while (iterator.hasNext()) {
            UnsavedChange unsavedChange = iterator.next();

            // the object has an ac, we can compare using ac
            if (io.getAc() != null){
                // change concerning the object, we can remove it because the revertJami has been done
                if (io.getAc().equals(unsavedChange.getUnsavedObject().getAc())) {
                    iterator.remove();
                }
                // change concerning the object but which need to be reverted because don't touch the object itself
                else if (io.getAc().equals(unsavedChange.getScope())){
                    additionnalUnsavedEventToRevert.add(unsavedChange);
                    iterator.remove();
                }
                else if (unsavedChange.getAcsToDeleteOn().contains(unsavedChange.getUnsavedObject().getAc())) {
                    additionnalUnsavedEventToRevert.add(unsavedChange);
                    iterator.remove();
                }
            }
            // the object is new, we can only checks if we have an unchanged event which is new and does not have a collection of parent acs (interactors, organism, cvobject terms)
            else if (unsavedChange.getUnsavedObject().getAc() == null) {

                if (unsavedChange.getUnsavedObject() instanceof IntactPrimaryObject){
                    IntactPrimaryObject unsavedAnnObj = (IntactPrimaryObject) unsavedChange.getUnsavedObject();

                    if (io == unsavedAnnObj){
                        iterator.remove();
                    }
                }
            }
        }

        CurateController curateController = (CurateController) getSpringContext().getBean("curateController");

        // now revert additonal changes related to this revert
        if (!additionnalUnsavedEventToRevert.isEmpty()){

            for (UnsavedChange addChange : additionnalUnsavedEventToRevert){
                curateController.discard(addChange.getUnsavedObject());
            }
        }
        // revert current object
        curateController.discard(io);
    }

    public boolean isUnsaved(IntactPrimaryObject io) {
        if (io == null) return false;
        if (io.getAc() == null) return true;

        return isUnsavedAc(io.getAc());
    }

    public boolean isUnsavedOrDeleted(IntactPrimaryObject io) {
        if (isUnsaved(io)) {
            return true;
        } else if (io.getAc() != null && isDeletedAc(io.getAc())) {
            return true;
        }

        return false;
    }

    public boolean isUnsavedAc(String ac) {
        if (ac == null) return true;

        for (UnsavedChange unsavedChange : getUnsavedChangesForCurrentUser()) {
            // if one unsaved event exists and matches the ac of the current object or the scope of the unsaved event matches the current object, this event can be considered as unsaved
            if (ac.equals(unsavedChange.getUnsavedObject().getAc()) || ac.equals(unsavedChange.getScope())) {
                return true;
            }
            // if the current ac is the parent ac of one of the elements to save, mark it as unsaved
            else if (!unsavedChange.getAcsToDeleteOn().isEmpty()){
                if (unsavedChange.getAcsToDeleteOn().contains(ac)){
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isDeletedAc(String ac) {
        if (ac == null) return true;

        for (UnsavedChange unsavedChange : getUnsavedChangesForCurrentUser()) {
            if (UnsavedChange.DELETED.equals(unsavedChange.getAction()) &&
                    ac.equals(unsavedChange.getUnsavedObject().getAc())) {
                return true;
            }
        }

        return false;
    }

    public List<String> getDeletedAcs(Class type, String parentAc) {
        return acList(getDeleted(type, parentAc));
    }

    private List<String> acList( Collection<? extends IntactPrimaryObject> intactObjects ) {
        List<String> acs = new ArrayList<String>( intactObjects.size() );

        for ( IntactPrimaryObject io : intactObjects ) {
            acs.add( io.getAc() );
        }

        return acs;
    }

    public List<String> getDeletedAcsByClassName(String className, String parentAc) {
        try {
            return getDeletedAcs(Thread.currentThread().getContextClassLoader().loadClass(className), parentAc);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return Collections.EMPTY_LIST;
    }

    public List<IntactPrimaryObject> getDeleted(Class type, String parentAc) {
        List<IntactPrimaryObject> ios = new ArrayList<IntactPrimaryObject>();

        for (UnsavedChange change : getUnsavedChangesForCurrentUser()) {
            if (UnsavedChange.DELETED.equals(change.getAction()) &&
                    type.isAssignableFrom(change.getUnsavedObject().getClass())) {

                if (change.getParentObject() != null && change.getParentObject().getAc() != null){
                    if (change.getParentObject().getAc().equals(parentAc)){
                        IntactPrimaryObject intactObject = change.getUnsavedObject();
                        ios.add(intactObject);
                    }
                }
                else if (change.getScope() != null && change.getScope().equals(parentAc)){
                    IntactPrimaryObject intactObject = change.getUnsavedObject();
                    ios.add(intactObject);
                }
            }
        }

        return ios;
    }

    public List<IntactPrimaryObject> getAllUnsaved() {
        List<IntactPrimaryObject> ios = new ArrayList<IntactPrimaryObject>();

        for (UnsavedChange change : getUnsavedChangesForCurrentUser()) {
            if (UnsavedChange.UPDATED.equals(change.getAction()) || UnsavedChange.CREATED.equals(change.getAction())) {
                IntactPrimaryObject intactObject = change.getUnsavedObject();
                ios.add(intactObject);
            }
        }

        return ios;
    }

    public List<UnsavedChange> getAllUnsavedChanges() {
        List<UnsavedChange> ios = new ArrayList<UnsavedChange>();

        for (UnsavedChange change : getUnsavedChangesForCurrentUser()) {
            if (UnsavedChange.UPDATED.equals(change.getAction()) || UnsavedChange.CREATED.equals(change.getAction())) {
                ios.add(change);
            }
        }

        return ios;
    }

    public List<IntactPrimaryObject> getAllDeleted() {
        List<IntactPrimaryObject> ios = new ArrayList<IntactPrimaryObject>();

        for (UnsavedChange change : getUnsavedChangesForCurrentUser()) {
            if (UnsavedChange.DELETED.equals(change.getAction())) {
                IntactPrimaryObject intactObject = change.getUnsavedObject();
                ios.add(intactObject);
            }
        }

        return ios;
    }

    public List<UnsavedChange> getAllUnsavedDeleted() {
        List<UnsavedChange> unsaved = new ArrayList<UnsavedChange>();

        for (UnsavedChange change : getUnsavedChangesForCurrentUser()) {
            if (UnsavedChange.DELETED.equals(change.getAction())) {
                unsaved.add(change);
            }
        }

        return unsaved;
    }

    public List<UnsavedChange> getAllUnsavedProteinTranscripts() {
        List<UnsavedChange> unsaved = new ArrayList<UnsavedChange>();

        for (UnsavedChange change : getHiddenUnsavedChangesForCurrentUser()) {
            if (UnsavedChange.CREATED_TRANSCRIPT.equals(change.getAction())) {
                unsaved.add(change);
            }
        }

        return unsaved;
    }

    public List<IntactPrimaryObject> getAllCreatedProteinTranscripts() {
        List<IntactPrimaryObject> ios = new ArrayList<IntactPrimaryObject>();

        for (UnsavedChange change : getUnsavedChangesForCurrentUser()) {
            if (UnsavedChange.CREATED_TRANSCRIPT.equals(change.getAction())) {
                IntactPrimaryObject intactObject = change.getUnsavedObject();
                ios.add(intactObject);
            }
        }

        return ios;
    }

    public IntactPrimaryObject findByAc(String ac) {
        for (UnsavedChange change : getUnsavedChangesForCurrentUser()) {
            if (ac.equals(change.getUnsavedObject().getAc())) {
                return change.getUnsavedObject();
            }
        }

        return null;
    }

    public List<String> getUsernames() {
        return new ArrayList<String>(changesPerUser.keySet());
    }

    public boolean isObjectBeingEdited(IntactPrimaryObject io, boolean includeMyself) {
        if (io.getAc() == null) return false;

        UserSessionController userSessionController = (UserSessionController) getSpringContext().getBean("userSessionController");
        String me = userSessionController.getCurrentUser().getLogin();

        for (String user : getUsernames()) {
            if (!includeMyself && user.equals(me)) continue;

            for (UnsavedChange unsavedChange : getUnsavedChangesForUser(user)) {
                if (io.getAc().equals(unsavedChange.getUnsavedObject().getAc())) {
                    return true;
                }
            }
        }

        return false;
    }

    //TODO: probably this should be a list
    public String whoIsEditingObject(IntactPrimaryObject io) {
        if (io.getAc() == null) return null;

        for (String user : getUsernames()) {
            for (UnsavedChange unsavedChange : getUnsavedChangesForUser(user)) {
                if (io.getAc().equals(unsavedChange.getUnsavedObject().getAc())) {
                    return user;
                }
            }
        }

        return null;
    }

    public void clearCurrentUserChanges() {
        getUnsavedChangesForCurrentUser().clear();
        getHiddenUnsavedChangesForCurrentUser().clear();
    }

    public int getNumberUnsavedChangedForCurrentUser(){
        return getUnsavedChangesForCurrentUser().size();
    }

    public List<UnsavedChange> getUnsavedChangesForCurrentUser() {
        return getUnsavedChangesForUser(getCurrentUser().getLogin());
    }

    public List<UnsavedChange> getHiddenUnsavedChangesForCurrentUser() {
        return getHiddenUnsavedChangesForUser(getCurrentUser().getLogin());
    }

    public List<UnsavedChange> getUnsavedChangesForUser(String userId) {
        List<UnsavedChange> unsavedChanges;

        if (changesPerUser.containsKey(userId)) {
            unsavedChanges = changesPerUser.get(userId);
        } else {
            unsavedChanges = new ArrayList<UnsavedChange>();
            changesPerUser.put(userId, unsavedChanges);
        }

        return unsavedChanges;
    }

    public List<UnsavedChange> getHiddenUnsavedChangesForUser(String userId) {
        List<UnsavedChange> unsavedChanges;

        if (hiddenChangesPerUser.containsKey(userId)) {
            unsavedChanges = hiddenChangesPerUser.get(userId);
        } else {
            unsavedChanges = new ArrayList<UnsavedChange>();
            hiddenChangesPerUser.put(userId, unsavedChanges);
        }

        return unsavedChanges;
    }

    private void addChange(UnsavedChange unsavedChange) {
        List<UnsavedChange> unsavedChanges = getUnsavedChangesForCurrentUser();

        unsavedChanges.remove(unsavedChange);
        unsavedChanges.add(unsavedChange);
    }

    private boolean addUnsavedChange(UnsavedChange unsavedChange) {

        // check first if the current object will not be deleted
        List<UnsavedChange> deletedChanges = getAllUnsavedDeleted();

        for (UnsavedChange deleteChange : deletedChanges){

            // if one deleted event is in conflict with the current save event (one of the parents of the current object will be deleted), don't add an update event (if experiment is deleted, new changes on the interaction does not make any sense)
            if (unsavedChange.getAcsToDeleteOn().contains(deleteChange.getUnsavedObject().getAc())){
                addWarningMessage("Save not allowed", "This object cannot be updated because it will be deleted when deleting " + deleteChange.getDescription());

                return false;
            }
            // the current object will be deleted itself
            else if (unsavedChange.getUnsavedObject().getAc() != null && unsavedChange.getUnsavedObject().getAc().equals(deleteChange.getUnsavedObject().getAc())){
                addWarningMessage("Save not allowed", "This object cannot be updated because it will be deleted when deleting " + deleteChange.getDescription());
                return false;
            }
        }

        // the current object will not be deleted (or any of its parents), we can remove safely the current changes if it exists and replace it with the new one
        List<UnsavedChange> unsavedChanges = getUnsavedChangesForCurrentUser();

        unsavedChanges.remove(unsavedChange);
        unsavedChanges.add(unsavedChange);
        return true;
    }

    private boolean addUnsavedHiddenChange(UnsavedChange unsavedChange) {

        List<UnsavedChange> deletedChanges = getAllUnsavedDeleted();

        for (UnsavedChange deleteChange : deletedChanges){

            // if one deleted event is in conflict with the current save event, don't add an update event (if experiment is deleted, new changes on the interaction does not make any sense)
            if (unsavedChange.getAcsToDeleteOn().contains(deleteChange.getUnsavedObject().getAc())){
                addWarningMessage("Save not allowed", "This object will be deleted when deleting the intact object : " + deleteChange.getDescription());
                return false;
            }
            // the current object will be deleted itself
            else if (unsavedChange.getUnsavedObject().getAc() != null && unsavedChange.getUnsavedObject().getAc().equals(deleteChange.getUnsavedObject().getAc())){
                addWarningMessage("Save not allowed", "This object cannot be updated because it will be deleted when deleting " + deleteChange.getDescription());

                return false;
            }
        }

        List<UnsavedChange> unsavedChanges = getHiddenUnsavedChangesForCurrentUser();
        unsavedChanges.remove(unsavedChange);
        unsavedChanges.add(unsavedChange);
        return true;
    }

    private void removeUserFromUnsaved(String user) {
        changesPerUser.remove(user);
    }

    private void removeUserFromHiddenUnsaved(String user) {
        hiddenChangesPerUser.remove(user);
    }

    public void saveAll(ActionEvent actionEvent) {
        CurateController curateController = ApplicationContextProvider.getBean("curateController");

        Collection<UnsavedChange> changes = new ArrayList(getUnsavedChangesForCurrentUser());

        try {
            // set user before saving
            getEditorService().setUser(getCurrentUser());
            getEditorService().saveAll(changes, getUnsavedChangesForCurrentUser());

        }  catch (SynchronizerException e) {
            addErrorMessage("Cannot save changes ", e.getCause() + ": " + e.getMessage());
        } catch (FinderException e) {
            addErrorMessage("Cannot save changes ", e.getCause() + ": " + e.getMessage());
        } catch (PersisterException e) {
            addErrorMessage("Cannot save changes ", e.getCause() + ": " + e.getMessage());
        }catch (Throwable e) {
            addErrorMessage("Cannot save changes ", e.getCause() + ": " + e.getMessage());
        }

        // refresh current view now
        final AnnotatedObjectController currentAoController = curateController.getCurrentAnnotatedObjectController();
        currentAoController.forceRefreshCurrentViewObject();

        getUnsavedChangesForCurrentUser().clear();
        getHiddenUnsavedChangesForCurrentUser().clear();
    }

    public void revertAll(ActionEvent actionEvent) {
        CurateController curateController = ApplicationContextProvider.getBean("curateController");

        Collection<UnsavedChange> changes = new ArrayList(getUnsavedChangesForCurrentUser());

        getEditorService().revertAll(changes);

        // refresh current view now
        final AnnotatedObjectController currentAoController = curateController.getCurrentAnnotatedObjectController();
        currentAoController.forceRefreshCurrentViewObject();

        getUnsavedChangesForCurrentUser().clear();
        getHiddenUnsavedChangesForCurrentUser().clear();
    }

    public EditorObjectService getEditorService() {
        if (this.editorService == null){
            this.editorService = ApplicationContextProvider.getBean("editorObjectService");
        }
        return editorService;
    }
}
