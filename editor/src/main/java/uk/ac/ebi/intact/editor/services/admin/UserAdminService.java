package uk.ac.ebi.intact.editor.services.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.primefaces.model.DualListModel;
import org.primefaces.model.SelectableDataModelWrapper;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.editor.util.SelectableCollectionDataModel;
import uk.ac.ebi.intact.jami.model.user.Role;
import uk.ac.ebi.intact.jami.model.user.User;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;
import uk.ac.ebi.intact.jami.utils.UserUtils;

import javax.faces.model.DataModel;
import javax.faces.model.SelectItem;
import java.util.*;

/**
 * Service used when administrating users.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 2.0
 */
@Service
@Scope( BeanDefinition.SCOPE_PROTOTYPE )
public class UserAdminService extends AbstractEditorService {

    private static final Log log = LogFactory.getLog( UserAdminService.class );

    ///////////////
    // Actions

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public List<SelectItem> loadPublicationReviewerSelectItems() {
        List<SelectItem> reviewerSelectItems = new ArrayList<SelectItem>();
        reviewerSelectItems.add(new SelectItem(null, "-- Random --", "Correction assigner", false, true, false));

        Collection<User> reviewers = getIntactDao().getUserDao().getByRole(Role.ROLE_REVIEWER);

        for (User reviewer : reviewers) {
            reviewerSelectItems.add(new SelectItem(reviewer, reviewer.getLogin()));
        }
        return reviewerSelectItems;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public List<SelectItem> loadComplexReviewerSelectItems() {
        List<SelectItem> reviewerSelectItems = new ArrayList<SelectItem>();
        reviewerSelectItems.add(new SelectItem(null, "-- Random --", "Correction assigner", false, true, false));

        Collection<User> reviewers = getIntactDao().getUserDao().getByRole(Role.ROLE_COMPLEX_REVIEWER);

        for (User reviewer : reviewers) {
            reviewerSelectItems.add(new SelectItem(reviewer, reviewer.getLogin()));
        }
        return reviewerSelectItems;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public List<SelectItem> loadAllReviewerSelectItems() {
        List<SelectItem> reviewerSelectItems = new ArrayList<SelectItem>();
        reviewerSelectItems.add(new SelectItem(null, "-- Random --", "Correction assigner", false, true, false));

        Set<User> reviewers = new HashSet<User>(getIntactDao().getUserDao().getByRole(Role.ROLE_COMPLEX_REVIEWER));
        reviewers.addAll(getIntactDao().getUserDao().getByRole(Role.ROLE_COMPLEX_REVIEWER));

        for (User reviewer : reviewers) {
            reviewerSelectItems.add(new SelectItem(reviewer, reviewer.getLogin()));
        }
        return reviewerSelectItems;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public DataModel<User> loadAllUsers() {

        Collection<User> users = getIntactDao().getUserDao().getAll();
        return new SelectableDataModelWrapper(new SelectableCollectionDataModel<User>(users), users);
    }


    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public User saveUser(User user) throws SynchronizerException, FinderException, PersisterException {
        attachDaoToTransactionManager();
        return synchronizeIntactObject(user, getIntactDao().getSynchronizerContext().getUserSynchronizer(), true);
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public void deleteUser(User user) throws SynchronizerException, FinderException, PersisterException {
        attachDaoToTransactionManager();
        deleteIntactObject(user, getIntactDao().getUserDao());
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public DualListModel<String> loadRoles( User user ) {

        List<String> source = new ArrayList<String>();
        List<String> target = new ArrayList<String>();

        Collection<Role> allRoles = getIntactDao().getRoleDao().getAll();
        log.debug( "Found " + allRoles.size() + " role(s) in the database." );


        if ( user == null ) {
            for ( Role role : allRoles ) {
                source.add( role.getName() );
            }
        } else {
            for ( Role role : allRoles ) {
                if ( user.getRoles().contains( role ) ) {
                    target.add( role.getName() );
                } else {
                    source.add( role.getName() );
                }
            }
        }

        return new DualListModel<String>( source, target );
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public User loadMentorReviewer(User user) {
        User mentor = UserUtils.getMentorReviewer(getIntactDao().getUserDao(), user);
        if (mentor != null){
            Hibernate.initialize(mentor.getRoles());
            Hibernate.initialize(mentor.getPreferences());
        }
        return mentor;
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public void createDefaultUsers() throws SynchronizerException, FinderException, PersisterException {
        User admin = getIntactDao().getUserDao().getByLogin( "admin" );
        if ( admin == null ) {
            admin = new User( "admin", "Admin", "N/A", "intact-admin@ebi.ac.uk" );
            admin.setPassword( "d033e22ae348aeb5660fc2140aec35850c4da997" );

            Role adminRole = getIntactDao().getRoleDao().getByName("ADMIN");
            if (adminRole == null){
                adminRole = new Role("ADMIN");
                persistIntactObject(adminRole, getIntactDao().getRoleDao());
            }
            admin.addRole( adminRole );

            persistIntactObject(admin, getIntactDao().getUserDao());
        }
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public void createDefaultRoles() throws SynchronizerException, FinderException, PersisterException {
        final List<Role> allRoles = getIntactDao().getRoleDao().getAll();
        addMissingRole( allRoles, "ADMIN" );
        addMissingRole( allRoles, "CURATOR" );
        addMissingRole( allRoles, "REVIEWER" );
        addMissingRole( allRoles, "COMPLEX_CURATOR" );
        addMissingRole( allRoles, "COMPLEX_REVIEWER" );
        addMissingRole( allRoles, "DATA_MANAGER" );

        log.info( "After loadParticipants: found " + getIntactDao().getRoleDao().getAll().size() + " role(s) in the database." );
    }

    private void addMissingRole( List<Role> allRoles, String roleName ) throws SynchronizerException, FinderException, PersisterException {
        boolean found = false;
        for ( Role role : allRoles ) {
            if ( role.getName().equals( roleName ) ) {
                found = true;
            }
        }

        if ( !found ) {
            Role role = new Role( roleName );
            persistIntactObject(role, getIntactDao().getRoleDao());
            if ( log.isInfoEnabled() ) {
                log.info( "Created user role: " + roleName );
            }
        }
    }
}
