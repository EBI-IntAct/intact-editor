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
package uk.ac.ebi.intact.editor.controller.reviewer;

import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import uk.ac.ebi.intact.editor.controller.BaseController;
import uk.ac.ebi.intact.editor.services.reviewer.ReviewerAvailabilityService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.user.User;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;
import uk.ac.ebi.intact.jami.utils.UserUtils;

import javax.annotation.Resource;
import javax.faces.event.ActionEvent;
import javax.faces.event.ComponentSystemEvent;
import java.util.Collection;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope( "conversation.access" )
@ConversationName( "reviewer" )
public class ReviewerAvailabilityController extends BaseController {

    private Collection<User> reviewers;
    private Collection<User> complexReviewers;

    @Resource(name = "reviewerAvailabilityService")
    private transient ReviewerAvailabilityService reviewerAvailabilityService;

    public ReviewerAvailabilityController() {
    }

    public void loadData(ComponentSystemEvent evt) {
        reviewers = getReviewerAvailabilityService().loadAllReviewers();
        complexReviewers = getReviewerAvailabilityService().loadAllComplexReviewers();
    }

    public void save(ActionEvent evt) {
        try {
            getReviewerAvailabilityService().getIntactDao().getUserContext().setUser(getCurrentUser());
            getReviewerAvailabilityService().saveUsers(this.reviewers);
            addInfoMessage("Saved", "The reviewers' availability has been updated");
        } catch (SynchronizerException e) {
            addErrorMessage("Cannot save reviewer availabilities ", e.getCause() + ": " + e.getMessage());
        } catch (FinderException e) {
            addErrorMessage("Cannot save reviewer availabilities ", e.getCause() + ": " + e.getMessage());
        } catch (PersisterException e) {
            addErrorMessage("Cannot save reviewer availabilities ", e.getCause() + ": " + e.getMessage());
        } catch (Throwable e) {
            addErrorMessage("Cannot save reviewer availabilities ", e.getCause() + ": " + e.getMessage());
        }

        try {
            getReviewerAvailabilityService().getIntactDao().getUserContext().setUser(getCurrentUser());
            getReviewerAvailabilityService().saveUsers(this.complexReviewers);
            addInfoMessage("Saved", "The complex reviewers' availability has been updated");
        } catch (SynchronizerException e) {
            addErrorMessage("Cannot save complex reviewer availabilities ", e.getCause() + ": " + e.getMessage());
        } catch (FinderException e) {
            addErrorMessage("Cannot save complex reviewer availabilities ", e.getCause() + ": " + e.getMessage());
        } catch (PersisterException e) {
            addErrorMessage("Cannot save complex reviewer availabilities ", e.getCause() + ": " + e.getMessage());
        } catch (Throwable e) {
            addErrorMessage("Cannot save complex reviewer availabilities ", e.getCause() + ": " + e.getMessage());
        }
    }

    public ReviewerWrapper wrapReviewer(User user) {
        return new ReviewerWrapper(user);
    }

    public Collection<User> getReviewers() {
        return reviewers;
    }

    public Collection<User> getComplexReviewers() {
        return complexReviewers;
    }

    public class ReviewerWrapper {
        private User reviewer;

        private ReviewerWrapper(User reviewer) {
            this.reviewer = reviewer;
        }

        public int getAvailability() {
            return UserUtils.getReviewerAvailability(reviewer);
        }

        public void setAvailability(int value) {
            UserUtils.setReviewerAvailability(reviewer, value);
        }
    }

    public ReviewerAvailabilityService getReviewerAvailabilityService() {
        if (this.reviewerAvailabilityService == null){
             this.reviewerAvailabilityService = ApplicationContextProvider.getBean("reviewerAvailabilityService");
        }
        return reviewerAvailabilityService;
    }
}
