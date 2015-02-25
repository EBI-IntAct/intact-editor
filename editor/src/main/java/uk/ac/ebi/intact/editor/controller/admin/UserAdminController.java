package uk.ac.ebi.intact.editor.controller.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.primefaces.model.DualListModel;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import uk.ac.ebi.intact.editor.controller.misc.AbstractUserController;
import uk.ac.ebi.intact.editor.services.admin.InstitutionAdminService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.user.Role;
import uk.ac.ebi.intact.jami.model.user.User;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.model.DataModel;
import javax.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller used when administrating users.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 2.0
 */
@Controller
@Scope("conversation.access")
@ConversationName("general")
public class UserAdminController extends AbstractUserController {

    private static final Log log = LogFactory.getLog( UserAdminController.class );

    private String loginParam;
    private DualListModel<String> roles;

    private transient DataModel<User> allUsers;

    private User[] selectedUsers;

    private List<SelectItem> reviewerSelectItems;
    private List<SelectItem> complexReviewerSelectItems;
    private List<SelectItem> allReviewerSelectItems;
    @Resource(name = "institutionAdminService")
    private transient InstitutionAdminService institutionAdminService;

    /////////////////
    // Users

    public String getLoginParam() {
        return loginParam;
    }

    public void setLoginParam( String loginParam ) {
        this.loginParam = loginParam;
    }

    ///////////////
    // Actions
    public void loadData( ComponentSystemEvent event ) {

        if (!FacesContext.getCurrentInstance().isPostback()) {
            refreshContext();
            log.info( "AbstractUserController.loadUserToUpdate" );

            this.reviewerSelectItems = getUserAdminService().loadPublicationReviewerSelectItems();
            this.complexReviewerSelectItems = getUserAdminService().loadComplexReviewerSelectItems();

            if ( loginParam != null ) {
                // load user and prepare for update
                log.debug("Loading user by login '" + loginParam + "'...");
                User user = getUserSessionService().loadUser( loginParam );
                setUser(user);

                if ( user == null ) {
                    addWarningMessage( "Could not find user by login: " + loginParam, "Please try again." );
                } else {
                    log.debug( "User password hash: " + user.getPassword() );
                }
            }

            log.debug( "UserAdminController.loadUsers" );
            allUsers = getUserAdminService().loadAllUsers();
            log.info( "AbstractUserController.loadRoles" );
            roles = getUserAdminService().loadRoles(getUser());
        }
    }

    public String saveUser() {
        User user = getUser();

        // handle roles
        final List<String> includedRoles = roles.getTarget();
        for ( String roleName : includedRoles ) {
            if ( !user.hasRole( roleName ) ) {
                final Role r = new Role( roleName );
                user.addRole( r );
                log.info( "Added role " + roleName + "to user " + user.getLogin() );
            }
        }

        final List<String> excludedRoles = roles.getSource();
        for ( String roleName : excludedRoles ) {
            if ( user.hasRole( roleName ) ) {
                final Role r = new Role( roleName );
                user.removeRole( r );
                log.info( "Removed role " + roleName + "to user " + user.getLogin() );
            }
        }

        try {
            user = getUserAdminService().saveUser(user);
            addInfoMessage( "User " + user.getLogin() + " was updated successfully", "" );
        } catch (SynchronizerException e) {
            addErrorMessage("Cannot save user " + user.getLogin(), e.getCause() + ": " + e.getMessage());
        } catch (FinderException e) {
            addErrorMessage("Cannot save user " + user.getLogin(), e.getCause() + ": " + e.getMessage());
        } catch (PersisterException e) {
            addErrorMessage("Cannot save user " + user.getLogin(), e.getCause() + ": " + e.getMessage());
        } catch (Throwable e) {
            addErrorMessage("Cannot save user " + user.getLogin(), e.getCause() + ": " + e.getMessage());
        }

        // reset properties before redirecting to the user list.
        setUser(null);
        setLoginParam(null);

        this.reviewerSelectItems=null;
        this.complexReviewerSelectItems=null;
        this.allReviewerSelectItems=null;

        return "admin.users.list";
    }

    public String deleteUser() {
        User user = getUser();

        try {
            getUserAdminService().deleteUser(user);
            addInfoMessage( "User " + user.getLogin() + " was deleted successfully", "" );
        } catch (SynchronizerException e) {
            addErrorMessage("Cannot delete user " + user.getLogin(), e.getCause() + ": " + e.getMessage());
        } catch (FinderException e) {
            addErrorMessage("Cannot delete user " + user.getLogin(), e.getCause() + ": " + e.getMessage());
        } catch (PersisterException e) {
            addErrorMessage("Cannot delete user " + user.getLogin(), e.getCause() + ": " + e.getMessage());
        } catch (Throwable e) {
            addErrorMessage("Cannot delete user " + user.getLogin(), e.getCause() + ": " + e.getMessage());
        }

        // reset properties before redirecting to the user list.
        setUser(null);
        setLoginParam(null);

        this.reviewerSelectItems=null;
        this.complexReviewerSelectItems=null;
        this.allReviewerSelectItems=null;

        return "admin.users.list";
    }

    public String newUser() {
        loginParam = null;
        setUser(new User("to set", "to set","to set","to set"));
        return "/admin/users/edit?faces-redirect=true";
    }

    public List<Role> createRoleList( User user ) {
        if ( user != null ) {
            return new ArrayList<Role>( user.getRoles() );
        }
        return null;
    }

    public void setRoles( DualListModel<String> roles ) {
        this.roles = roles;
    }

    public DualListModel<String> getRoles() {
        return roles;
    }

    public DataModel<User> getAllUsers() {
        return allUsers;
    }

    public User[] getSelectedUsers() {
        return selectedUsers;
    }

    public void setSelectedUsers( User[] selectedUsers ) {
        this.selectedUsers = selectedUsers;
    }

    public boolean hasSelectedUsers() {
        if( this.selectedUsers != null ) {
            for ( int i = 0; i < selectedUsers.length; i++ ) {
                if ( selectedUsers[i] != null ) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getSelectedUsersLogin( String separator ) {
        String logins = null;
        if( this.selectedUsers != null ) {
            StringBuilder sb = new StringBuilder( 128 );
            for ( int i = 0; i < selectedUsers.length; i++ ) {
                User selectedUser = selectedUsers[i];
                if( selectedUser != null ) {
                    sb.append( selectedUser.getLogin() );
                    if((i+1) < selectedUsers.length ) {
                        sb.append( separator );
                    }
                }
            }
            logins = sb.toString();
        }

        return logins;
    }

    public List<SelectItem> getReviewerSelectItems() {
        if (reviewerSelectItems == null){
            this.reviewerSelectItems = getUserAdminService().loadPublicationReviewerSelectItems();
        }
        return reviewerSelectItems;
    }

    public List<SelectItem> getComplexReviewerSelectItems() {
        if (complexReviewerSelectItems == null){
            this.complexReviewerSelectItems = getUserAdminService().loadComplexReviewerSelectItems();
        }
        return complexReviewerSelectItems;
    }

    public List<SelectItem> getAllReviewerSelectItems() {
        if (allReviewerSelectItems == null){
            this.allReviewerSelectItems = getUserAdminService().loadAllReviewerSelectItems();
        }
        return allReviewerSelectItems;
    }

    public InstitutionAdminService getInstitutionAdminService() {
        if (this.institutionAdminService == null){
            this.institutionAdminService = ApplicationContextProvider.getBean("institutionAdminService");
        }
        return institutionAdminService;
    }

    public List<SelectItem> getInstitutionItems() {

        return getInstitutionAdminService().getInstitutionItems();
    }
}
