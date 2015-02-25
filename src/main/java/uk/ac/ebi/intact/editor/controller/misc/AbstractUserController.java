package uk.ac.ebi.intact.editor.controller.misc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import psidev.psi.mi.jami.model.Source;
import uk.ac.ebi.intact.editor.controller.BaseController;
import uk.ac.ebi.intact.editor.services.UserSessionService;
import uk.ac.ebi.intact.editor.services.admin.UserAdminService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.extension.IntactSource;
import uk.ac.ebi.intact.jami.model.user.Preference;
import uk.ac.ebi.intact.jami.model.user.User;

import javax.annotation.Resource;

/**
 * Controller used when administrating users.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 2.0
 */
public abstract class AbstractUserController extends BaseController {

    private static final Log log = LogFactory.getLog(AbstractUserController.class);

    private static final String CURATION_DEPTH = "curation.depth";
    public static final String RAW_NOTES = "editor.notes";
    public static final String GOOGLE_USERNAME = "google.username";

    public static final String INSTITUTION_AC = "editor.institution.ac";
    public static final String INSTITUTION_NAME = "editor.institution.name";

    private User user;
    private Source institution;
    private User mentor;

    @Resource(name = "userSessionService")
    private transient UserSessionService userSessionService;
    @Resource(name = "userAdminService")
    private transient UserAdminService userAdminService;

    /////////////////
    // Users

    public String getInstitutionNameForUser(User user) {
        return findPreference(user, INSTITUTION_NAME, null);
    }

    public String getInstitutionAcForUser(User user) {
        return findPreference(user, INSTITUTION_AC, null);
    }


    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCurationDepth() {
        return findPreference(CURATION_DEPTH, getEditorConfig().getDefaultCurationDepth());
    }

    public void setCurationDepth(String curationDepth) {
        setPreference(CURATION_DEPTH, curationDepth);
    }

    public String getRawNotes() {
        return findPreference(RAW_NOTES, null);
    }

    public void setRawNotes(String notes) {
        setPreference(RAW_NOTES, notes);
    }

    public String getGoogleUsername() {
        return findPreference(GOOGLE_USERNAME, null);
    }

    public void setGoogleUsername(String notes) {
        setPreference(GOOGLE_USERNAME, notes);
    }

    public Source getInstitution() {
        if (this.institution == null){
            this.institution = getUserSessionService().getUserInstitution(getUser());
        }
        return this.institution;
    }

    public void setInstitution(Source institution) {
        if (institution != null) {
            if (institution instanceof IntactSource){
                setPreference(INSTITUTION_AC, ((IntactSource)institution).getAc());
            }
            setPreference(INSTITUTION_NAME, institution.getShortName());
        }
        this.institution = institution;
    }

    public User getMentorReviewer() {
        if (this.mentor == null){
            this.mentor = getUserAdminService().loadMentorReviewer(getUser());
        }
        return mentor;
    }

    public void setMentorReviewer(User mentor) {
        if (user != null) {
            if (mentor == null) {
                Preference pref = user.getPreference(Preference.KEY_MENTOR_REVIEWER);
                // If we have null as a mentor, we want to assign random reviewers again and for that we need
                // to remove the preference reviewer
                if (pref != null){
                    user.removePreference(user.getPreference(Preference.KEY_MENTOR_REVIEWER));
                }
            } else {
                user.addOrUpdatePreference(Preference.KEY_MENTOR_REVIEWER, mentor.getAc());

            }
        }
        this.mentor = mentor;
    }

    public UserSessionService getUserSessionService() {
        if (this.userSessionService == null){
           userSessionService = ApplicationContextProvider.getBean("userSessionService");
        }
        return userSessionService;
    }

    public UserAdminService getUserAdminService() {
        if (this.userAdminService == null){
            userAdminService = ApplicationContextProvider.getBean("userAdminService");
        }
        return userAdminService;
    }

    protected void refreshContext(){
        this.institution = null;
        this.mentor = null;
    }

    private String findPreference(String prefKey) {
        return findPreference(getUser(), prefKey, null);
    }

    private String findPreference(String prefKey, String defaultValue) {
        return findPreference(getUser(), prefKey, defaultValue);
    }

    private String findPreference(User user, String prefKey, String defaultValue) {
        for (Preference pref : user.getPreferences()) {
            if (prefKey.equals(pref.getKey())) {
                return pref.getValue();
            }
        }
        return defaultValue;
    }

    private void setPreference(String prefKey, String prefValue) {
        Preference preference = null;

        for (Preference pref : user.getPreferences()) {
            if (prefKey.equals(pref.getKey())) {
                preference = pref;
            }
        }

        if (preference == null) {
            preference = new Preference(prefKey);
            user.getPreferences().add(preference);
        }

        preference.setValue(prefValue);
    }


}
