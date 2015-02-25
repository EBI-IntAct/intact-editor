package uk.ac.ebi.intact.editor.controller.dbmanager;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.primefaces.model.LazyDataModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import uk.ac.ebi.intact.editor.application.SearchThreadConfig;
import uk.ac.ebi.intact.editor.controller.BaseController;
import uk.ac.ebi.intact.editor.controller.UserSessionController;
import uk.ac.ebi.intact.editor.services.search.SearchQueryService;
import uk.ac.ebi.intact.editor.services.summary.*;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.user.Role;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Search controller.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 2.0
 */
@Controller
@Scope( "conversation.access" )
@ConversationName ("dbmanager")
@SuppressWarnings("unchecked")
public class JobSummaryController extends BaseController {

    private static final Log log = LogFactory.getLog( JobSummaryController.class );

    @Resource(name = "searchQueryService")
    private transient SearchQueryService searchService;

    private String query;

    private int threadTimeOut = 10;

    private LazyDataModel<PublicationSummary> publications;

    private LazyDataModel<ExperimentSummary> experiments;

    private LazyDataModel<InteractionSummary> interactions;

    private LazyDataModel<MoleculeSummary> molecules;

    private LazyDataModel<CvSummary> cvobjects;

    private LazyDataModel<FeatureSummary> features;

    private LazyDataModel<OrganismSummary> organisms;

    private LazyDataModel<ParticipantSummary> participants;

    private LazyDataModel<ComplexSummary> complexes;

    private LazyDataModel<ParticipantSummary> modelledParticipants;

    private LazyDataModel<FeatureSummary> modelledFeatures;

    private List<Future> runningTasks;

    private boolean isPublicationSearchEnabled = false;
    private boolean isComplexSearchEnabled = false;

    private String objType = null;

    @Autowired
    private UserSessionController userSessionController;

    //////////////////
    // Constructors

    public JobSummaryController() {
    }

    ///////////////
    // Actions

    public void searchIfQueryPresent(ComponentSystemEvent evt) {
        if (!FacesContext.getCurrentInstance().isPostback()) {
            if (query != null && !query.isEmpty()) {
                doSearch();
            }
        }
    }

    private ExecutorService initExecutorService() {
        SearchThreadConfig threadConfig = (SearchThreadConfig) getSpringContext().getBean("searchThreadConfig");

        ExecutorService executorService = threadConfig.getExecutorService();

        if (runningTasks == null){
            runningTasks = new ArrayList<Future>();
        }
        else {
            runningTasks.clear();
        }
        return executorService;
    }

    private void refreshUserRoles() {
        if (userSessionController.hasRole(Role.ROLE_CURATOR) || userSessionController.hasRole(Role.ROLE_REVIEWER) ){
            isPublicationSearchEnabled = true;
        }
        else{
            isPublicationSearchEnabled = false;
        }
        if (userSessionController.hasRole(Role.ROLE_COMPLEX_CURATOR) || userSessionController.hasRole(Role.ROLE_COMPLEX_REVIEWER) ){
            isComplexSearchEnabled = true;
        }
        else{
            isComplexSearchEnabled = false;
        }
    }

    public String doSearch() {
        refreshUserRoles();

        log.info( "Searching for '" + query + "'..." );

        if ( !StringUtils.isEmpty( query ) ) {
            final String originalQuery = query.trim();
            String q = query;

            // TODO implement simple prefix for the search query so that one can aim at an AC, shortlabel, PMID...

            // Note: the search is NOT case sensitive !!!
            // Note: the search includes wildcards automatically
            final String finalQuery = q;

            ExecutorService executorService = initExecutorService();

            if (isPublicationSearchEnabled){
                Runnable runnablePub = new Runnable() {
                    @Override
                    public void run() {
                        publications = getSearchService().loadImportedPublication( originalQuery );
                    }
                };

                Runnable runnableExp = new Runnable() {
                    @Override
                    public void run() {
                        experiments = getSearchService().loadImportedExperiments( originalQuery );
                    }
                };

                Runnable runnableInt = new Runnable() {
                    @Override
                    public void run() {
                        interactions = getSearchService().loadImportedInteractions( originalQuery );
                    }
                };

                Runnable runnableFeatures = new Runnable() {
                    @Override
                    public void run() {
                        features = getSearchService().loadImportedFeatures( originalQuery );
                    }
                };
                Runnable runnableComponents = new Runnable() {
                    @Override
                    public void run() {
                        participants = getSearchService().loadImportedParticipants(originalQuery);
                    }
                };

                runningTasks.add(executorService.submit(runnablePub));
                runningTasks.add(executorService.submit(runnableExp));
                runningTasks.add(executorService.submit(runnableInt));
                runningTasks.add(executorService.submit(runnableFeatures));
                runningTasks.add(executorService.submit(runnableComponents));
            }

            Runnable runnableMol = new Runnable() {
                @Override
                public void run() {
                    molecules = getSearchService().loadImportedMolecules( originalQuery );
                }
            };

            Runnable runnableCvs = new Runnable() {
                @Override
                public void run() {
                    cvobjects = getSearchService().loadImportedCvs( originalQuery );
                }
            };

            Runnable runnableOrganisms = new Runnable() {
                @Override
                public void run() {
                    organisms = getSearchService().loadImportedOrganisms( originalQuery );
                }
            };

            runningTasks.add(executorService.submit(runnableMol));
            runningTasks.add(executorService.submit(runnableCvs));
            runningTasks.add(executorService.submit(runnableOrganisms));

            if (isComplexSearchEnabled){
                Runnable runnableComp = new Runnable() {
                    @Override
                    public void run() {
                        complexes = getSearchService().loadImportedComplexes( originalQuery );
                    }
                };

                Runnable runnablePart= new Runnable() {
                    @Override
                    public void run() {
                        modelledParticipants = getSearchService().loadImportedModelledParticipants(originalQuery);
                    }
                };

                Runnable runnableFeat = new Runnable() {
                    @Override
                    public void run() {
                        modelledFeatures = getSearchService().loadImportedModelledFeatures(originalQuery);
                    }
                };

                runningTasks.add(executorService.submit(runnableComp));
                runningTasks.add(executorService.submit(runnablePart));
                runningTasks.add(executorService.submit(runnableFeat));
            }

            checkAndResumeTasks();
        } else {
            resetSearchResults();
        }

        return "import.results";
    }

    private void checkAndResumeTasks() {

        for (Future f : runningTasks){
            try {
                f.get(threadTimeOut, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.error("The editor search task was interrupted, we cancel the task.", e);
                if (!f.isCancelled()){
                    f.cancel(true);
                }
            } catch (ExecutionException e) {
                log.error("The editor search task could not be executed, we cancel the task.", e);
                if (!f.isCancelled()){
                    f.cancel(true);
                }
            } catch (TimeoutException e) {
                log.error("Service task stopped because of time out " + threadTimeOut + "seconds.", e);

                if (!f.isCancelled()){
                    f.cancel(true);
                }
            }
        }

        runningTasks.clear();
    }

    private void resetSearchResults() {
        publications = null;
        experiments = null;
        interactions = null;
        molecules = null;
        cvobjects = null;
        complexes = null;
        modelledFeatures = null;
        modelledParticipants = null;
    }

    public boolean isEmptyQuery() {
        return StringUtils.isEmpty(query);
    }

    public boolean hasNoResults() {
        return (publications == null || publications.getRowCount() == 0)
                && (experiments == null || experiments.getRowCount() == 0)
                && (interactions == null || interactions.getRowCount() == 0)
                && (molecules == null || molecules.getRowCount() == 0)
                && (cvobjects == null || cvobjects.getRowCount() == 0)
                && (features == null || features.getRowCount() == 0)
                && (organisms == null || organisms.getRowCount() == 0)
                && (participants == null || participants.getRowCount() == 0)
                && (complexes == null || complexes.getRowCount() == 0)
                && (modelledParticipants == null || modelledParticipants.getRowCount() == 0)
                && (modelledFeatures == null || modelledFeatures.getRowCount() == 0);

    }


    ///////////////////////////
    // Getters and Setters

    public String getQuery() {
        return query;
    }

    public void setQuery( String query ) {
        this.query = query;
    }

    public LazyDataModel<PublicationSummary> getPublications() {
        return publications;
    }

    public LazyDataModel<ExperimentSummary> getExperiments() {
        return experiments;
    }

    public LazyDataModel<InteractionSummary> getInteractions() {
        return interactions;
    }

    public LazyDataModel<MoleculeSummary> getMolecules() {
        return molecules;
    }

    public LazyDataModel<CvSummary> getCvobjects() {
        return cvobjects;
    }

    public LazyDataModel<FeatureSummary> getFeatures() {
        return features;
    }

    public LazyDataModel<OrganismSummary> getOrganisms() {
        return organisms;
    }

    public LazyDataModel<ParticipantSummary> getParticipants() {
        return participants;
    }

    public LazyDataModel<ComplexSummary> getComplexes() {
        return complexes;
    }

    public LazyDataModel<ParticipantSummary> getModelledParticipants() {
        return modelledParticipants;
    }

    public LazyDataModel<FeatureSummary> getModelledFeatures() {
        return modelledFeatures;
    }

    public int getThreadTimeOut() {
        return threadTimeOut;
    }

    public void setThreadTimeOut(int threadTimeOut) {
        this.threadTimeOut = threadTimeOut;
    }

    public boolean isPublicationSearchEnabled() {
        return isPublicationSearchEnabled;
    }

    public boolean isComplexSearchEnabled() {
        return isComplexSearchEnabled;
    }

    public SearchQueryService getSearchService() {
        if (this.searchService == null){
            this.searchService = ApplicationContextProvider.getBean("searchQueryService");
        }
        return searchService;
    }

    public String getObjType() {
        return objType;
    }

    public void setObjType(String objType) {
        this.objType = objType;
    }
}
