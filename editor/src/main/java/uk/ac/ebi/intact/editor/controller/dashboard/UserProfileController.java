package uk.ac.ebi.intact.editor.controller.dashboard;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import uk.ac.ebi.intact.editor.controller.misc.AbstractUserController;
import uk.ac.ebi.intact.editor.services.UserSessionService;
import uk.ac.ebi.intact.editor.services.dashboard.UserProfileService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.user.User;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Component
@Scope("conversation.access")
public class UserProfileController extends AbstractUserController {

    private String hashedPassword;
    private String newPassword1;
    private String newPassword2;

    @Resource(name = "userSessionService")
    private transient UserSessionService userSessionService;

    @Resource(name = "userProfileService")
    private transient UserProfileService userProfileService;

    public void loadData(ComponentSystemEvent cse) {
        setUser(getUserSessionService().loadUser(getCurrentUser().getLogin()));
    }

    public String updateProfile() {
        User user = getUser();

        // validate password if set
         if (newPassword1 != null && !newPassword1.isEmpty()) {
             if (newPassword1.equals(newPassword2)) {
                user.setPassword(hashedPassword);
             } else {
                 addErrorMessage("Wrong password", "Passwords do not match");
                 FacesContext.getCurrentInstance().renderResponse();
                 return null;
             }
         }

        try {
            getUserSessionService().getIntactDao().getUserContext().setUser(getCurrentUser());
            getUserSessionController().setCurrentUser(getUserProfileService().updateProfile(user));
            addInfoMessage("User profile", "Profile was updated successfully");
        } catch (SynchronizerException e) {
            addErrorMessage("Cannot save user " + user.getLogin(), e.getCause() + ": " + e.getMessage());
        } catch (FinderException e) {
            addErrorMessage("Cannot save user " + user.getLogin(), e.getCause() + ": " + e.getMessage());
        } catch (PersisterException e) {
            addErrorMessage("Cannot save user " + user.getLogin(), e.getCause() + ": " + e.getMessage());
        } catch (Throwable e) {
            addErrorMessage("Cannot save user " + user.getLogin(), e.getCause() + ": " + e.getMessage());
        }

        return "/dashboard/dashboard";
    }

    public String getNewPassword1() {
        return newPassword1;
    }

    public void setNewPassword1(String newPassword1) {
        this.newPassword1 = newPassword1;
    }

    public String getNewPassword2() {
        return newPassword2;
    }

    public void setNewPassword2(String newPassword2) {
        this.newPassword2 = newPassword2;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public UserSessionService getUserSessionService() {
        if (this.userSessionService == null){
            this.userSessionService = ApplicationContextProvider.getBean("userSessionService");
        }
        return userSessionService;
    }

    public UserProfileService getUserProfileService() {
        if (this.userProfileService == null){
            this.userProfileService = ApplicationContextProvider.getBean("userProfileService");
        }
        return userProfileService;
    }
}
