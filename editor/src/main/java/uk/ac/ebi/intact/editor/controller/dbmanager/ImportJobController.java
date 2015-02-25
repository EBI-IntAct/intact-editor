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
package uk.ac.ebi.intact.editor.controller.dbmanager;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.*;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.jami.batch.MIBatchJobManager;
import uk.ac.ebi.intact.dataexchange.dbimporter.writer.AbstractIntactDbImporter;
import uk.ac.ebi.intact.editor.controller.BaseController;
import uk.ac.ebi.intact.editor.services.dbmanager.BatchJobService;
import uk.ac.ebi.intact.editor.services.dbmanager.DbImportService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;

import javax.annotation.Resource;
import javax.faces.component.UIParameter;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ComponentSystemEvent;
import java.io.File;
import java.util.*;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope( "conversation.access" )
@ConversationName( "dbmanager" )
@Lazy
public class ImportJobController extends BaseController {

    @Resource( name = "psiMIJobManager" )
    private transient MIBatchJobManager psiMIJobManager;

    @Resource( name = "intactJobExplorer" )
    private transient JobExplorer jobExplorer;

    @Resource( name = "intactJobLauncher" )
    private transient JobLauncher intactJobLauncher;

    @Resource( name = "batchJobService" )
    private transient BatchJobService batchJobService;

    @Resource( name = "dbImportService" )
    private transient DbImportService dbImportService;

    private static final Log log = LogFactory.getLog(ImportJobController.class);

    private List<JobExecution> runningJobEvidence = Collections.EMPTY_LIST;
    private List<JobExecution> runningJobComplex = Collections.EMPTY_LIST;
    private List<JobExecution> completedJobEvidence = Collections.EMPTY_LIST;
    private List<JobExecution> completedJobComplex = Collections.EMPTY_LIST;

    public ImportJobController() {
    }

    public void load( ComponentSystemEvent event ) {

        if (!FacesContext.getCurrentInstance().isPostback()) {

            log.debug( "Load job summary" );
            List<JobInstance> existingJobs1 = getJobInstances("interactionMixImport");
            List<JobInstance> existingJobs2 = getJobInstances("complexImport");
            this.runningJobEvidence = getRunningJobExecutions("interactionMixImport");
            this.runningJobComplex = getRunningJobExecutions("complexImport");
            this.completedJobComplex = new ArrayList<JobExecution>();
            this.completedJobEvidence = new ArrayList<JobExecution>();

            for (JobInstance jobEvidence : existingJobs1){
                List<JobExecution> allExecutions = getJobExecutions(jobEvidence.getId());
                this.completedJobEvidence.addAll(CollectionUtils.subtract(allExecutions, runningJobEvidence));
            }
            for (JobInstance jobComplex : existingJobs2){
                List<JobExecution> allExecutions = getJobExecutions(jobComplex.getId());
                this.completedJobComplex.addAll(CollectionUtils.subtract(allExecutions, runningJobComplex));
            }
        }
    }

    public List<String> getJobNames() {
        return new ArrayList<String>(getPsiMIJobManager().getJobOperator().getJobNames());
    }

    public void restart( ActionEvent evt ) {

        if (!evt.getComponent().getChildren().isEmpty()){
            UIParameter param = ( UIParameter ) evt.getComponent().getChildren().iterator().next();

            long executionId = ( Long ) param.getValue();

            try {
                getPsiMIJobManager().restartJob(executionId);

                addInfoMessage( "Job restarted", "Execution ID: " + executionId );
                // remove old job instance
                getBatchJobService().deleteJob(executionId);

            } catch ( JobInstanceAlreadyCompleteException e ) {
                addErrorMessage( "Job is already complete "+e.getMessage(), "Execution ID: " + executionId );
                e.printStackTrace();
            } catch ( NoSuchJobExecutionException e ) {
                addErrorMessage( "Job execution does not exist "+e.getMessage(), "Execution ID: " + executionId );
                e.printStackTrace();
            } catch ( NoSuchJobException e ) {
                addErrorMessage( "Job does not exist "+e.getMessage(), "Execution ID: " + executionId );
                e.printStackTrace();
            } catch ( JobRestartException e ) {
                addErrorMessage( "Problem restarting job "+e.getMessage(), "Execution ID: " + executionId );
                e.printStackTrace();
            } catch ( JobParametersInvalidException e ) {
                addErrorMessage( "Job parameters are invalid "+e.getMessage(), "Execution ID: " + executionId );
                e.printStackTrace();
            } catch (NoSuchJobInstanceException e) {
                addErrorMessage("Job instance does not exist "+e.getMessage(), "Execution ID: " + executionId);
            }
        }
    }

    public void acceptImport( ActionEvent evt ) {

        if (!evt.getComponent().getChildren().isEmpty()){
            UIParameter param = ( UIParameter ) evt.getComponent().getChildren().iterator().next();

            long executionId = ( Long ) param.getValue();

            JobExecution execution = getJobExplorer().getJobExecution(executionId);

            if (execution != null){
                JobParameters params = execution.getJobParameters();
                if (params != null){
                    String jobId= params.getString("MIJobId");

                    try {
                        getDbImportService().acceptImport(jobId);

                        getBatchJobService().deleteJob(executionId);

                        addInfoMessage( "Job accepted, import annotations removed", "Execution ID: " + executionId );
                    } catch (Throwable e) {
                        addErrorMessage("Cannot accept job import "+e.getMessage(), "Execution ID: " + executionId);
                    }
                }
            }
        }
    }

    public boolean isJobEvidence(JobExecution execution){
        if (execution == null){
            return false;
        }
        return "interactionMixImport".equals(execution.getJobInstance().getJobName());
    }

    public void discardImport( ActionEvent evt ) {

        if (!evt.getComponent().getChildren().isEmpty()){
            UIParameter param = ( UIParameter ) evt.getComponent().getChildren().iterator().next();

            long executionId = ( Long ) param.getValue();

            JobExecution execution = getJobExplorer().getJobExecution(executionId);

            if (execution != null){
                JobParameters params = execution.getJobParameters();
                if (params != null){
                    String jobId= params.getString("MIJobId");

                    try {
                        getDbImportService().deleteImportedFeatures(jobId);

                        getDbImportService().deleteImportedParticipants(jobId);

                        getDbImportService().deleteImportedInteractions(jobId);

                        getDbImportService().deleteImportedExperiments(jobId);

                        getDbImportService().deleteImportedPublications(jobId);

                        getDbImportService().deleteImportedOrganisms(jobId);

                        getDbImportService().deleteImportedSources(jobId);

                        getDbImportService().deleteImportedCvs(jobId);

                        getBatchJobService().deleteJob(executionId);

                        addInfoMessage( "Job cleared, import objects deleted", "Execution ID: " + executionId );
                    } catch (Throwable e) {
                        addErrorMessage("Cannot clear job import "+e.getMessage(), "Execution ID: " + executionId+", "+ExceptionUtils.getFullStackTrace(e));
                    }

                    String errorFileName = params.getString("error.file");
                    String fileName = params.getString("input.file");

                    File file = new File(fileName);
                    if (file.exists()){
                        file.delete();
                    }
                    File errorFile = new File(errorFileName);
                    if (errorFile.exists()){
                        errorFile.delete();
                    }
                }
            }
        }
    }

    public void stopAndDiscardImport( ActionEvent evt ) {

        if (!evt.getComponent().getChildren().isEmpty()){
            UIParameter param = ( UIParameter ) evt.getComponent().getChildren().iterator().next();

            long executionId = ( Long ) param.getValue();

            try {
                getPsiMIJobManager().getJobOperator().stop(executionId);

                addInfoMessage( "Job stopped", "Execution ID: " + executionId );
            } catch ( NoSuchJobExecutionException e ) {
                addErrorMessage( "Job does not exist: "+e.getMessage(), "Execution ID: " + executionId );
                e.printStackTrace();
            } catch ( JobExecutionNotRunningException e ) {
                addErrorMessage( "Job is not running anymore: "+e.getMessage(), "Execution ID: " + executionId );
                e.printStackTrace();
            }

            JobExecution execution = getJobExplorer().getJobExecution(executionId);

            if (execution != null){
                JobParameters params = execution.getJobParameters();
                if (params != null){
                    String jobId= params.getString("MIJobId");

                    try {
                        getDbImportService().deleteImportedFeatures(jobId);

                        getDbImportService().deleteImportedParticipants(jobId);

                        getDbImportService().deleteImportedInteractions(jobId);

                        getDbImportService().deleteImportedExperiments(jobId);

                        getDbImportService().deleteImportedPublications(jobId);

                        getDbImportService().deleteImportedOrganisms(jobId);

                        getDbImportService().deleteImportedSources(jobId);

                        getDbImportService().deleteImportedCvs(jobId);

                        getBatchJobService().deleteJob(executionId);

                        addInfoMessage( "Job cleared, import objects deleted", "Execution ID: " + executionId );
                    } catch (Throwable e) {
                        addErrorMessage("Cannot clear job import "+e.getMessage(), "Execution ID: " + executionId+", "+ExceptionUtils.getFullStackTrace(e));
                    }

                    String errorFileName = params.getString("error.file");
                    String fileName = params.getString("input.file");

                    File file = new File(fileName);
                    if (file.exists()){
                        file.delete();
                    }
                    File errorFile = new File(errorFileName);
                    if (errorFile.exists()){
                        errorFile.delete();
                    }
                }
            }
        }
    }

    public void stop( ActionEvent evt ) {
        UIParameter param = ( UIParameter ) evt.getComponent().getChildren().iterator().next();
        long executionId = ( Long ) param.getValue();

        try {
            getPsiMIJobManager().getJobOperator().stop(executionId);

            addInfoMessage( "Job stopped", "Execution ID: " + executionId );
        } catch ( NoSuchJobExecutionException e ) {
            addErrorMessage( "Job does not exist: "+e.getMessage(), "Execution ID: " + executionId );
            e.printStackTrace();
        } catch ( JobExecutionNotRunningException e ) {
            addErrorMessage( "Job is not running anymore: "+e.getMessage(), "Execution ID: " + executionId );
            e.printStackTrace();
        }
    }

    public List<JobExecution> getRunningJobExecutions( String jobName ) {
        return new ArrayList<JobExecution>( getJobExplorer().findRunningJobExecutions(jobName) );
    }

    public String extractJobId( JobExecution execution ) {
        if (execution != null){
           JobParameters param = execution.getJobParameters();
            if (param != null){
                return param.getString("MIJobId");
            }
        }
        return null;
    }

    public List<JobInstance> getJobInstances( String jobName ) {
        return getJobExplorer().getJobInstances(jobName, 0, 10);
    }

    public List<JobExecution> getJobExecutions( Long jobInstanceId ) {
        if ( jobInstanceId > 0 ) {
            JobInstance jobInstance = getJobExplorer().getJobInstance(jobInstanceId);
            return getJobExplorer().getJobExecutions(jobInstance);
        }
        return new ArrayList<JobExecution>();
    }

    public List<StepExecution> getStepExecutions( JobExecution jobExecution ) {
        return new ArrayList<StepExecution>( jobExecution.getStepExecutions() );
    }

    public List<Throwable> getFailureExceptions( JobExecution jobExecution ) {

        return new ArrayList<Throwable>( jobExecution.getAllFailureExceptions() );
    }

    public List<Map.Entry<String,String>> getImportStatistics( JobExecution jobExecution ) {
        Collection<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        Map<String,String> statistics = new HashMap<String, String>();
        if (!stepExecutions.isEmpty()){
            StepExecution firstStep = stepExecutions.iterator().next();
            ExecutionContext stepContext = firstStep.getExecutionContext();

            for (Map.Entry<String, Object> entry : stepContext.entrySet()){
                // persisted count
                if (entry.getKey().startsWith(AbstractIntactDbImporter.PERSIST_MAP_COUNT)){
                    statistics.put(entry.getKey().substring(entry.getKey().lastIndexOf(".")+1), Integer.toString((Integer)entry.getValue()));
                }
            }
        }
        return new ArrayList<Map.Entry<String, String>>(statistics.entrySet());
    }

    public boolean hasJobFailed( JobExecution jobExecution ) {
        return jobExecution.getExitStatus().equals(ExitStatus.FAILED);
    }

    public String printFullStackTrace( Throwable e ) {
        return ExceptionUtils.getFullStackTrace(e);
    }

    public MIBatchJobManager getPsiMIJobManager() {
        if (this.psiMIJobManager == null){
            this.psiMIJobManager = ApplicationContextProvider.getBean("psiMIJobManager");
        }
        return psiMIJobManager;
    }

    public JobExplorer getJobExplorer() {
        if (this.jobExplorer == null){
            this.jobExplorer = ApplicationContextProvider.getBean("intactJobExplorer");
        }
        return jobExplorer;
    }

    public JobLauncher getIntactJobLauncher() {
        if (this.intactJobLauncher == null){
            this.intactJobLauncher = ApplicationContextProvider.getBean("intactJobLauncher");
        }
        return intactJobLauncher;
    }

    public BatchJobService getBatchJobService() {
        if (this.batchJobService == null){
            this.batchJobService = ApplicationContextProvider.getBean("batchJobService");
        }
        return batchJobService;
    }

    public DbImportService getDbImportService() {
        if (this.dbImportService == null){
            this.dbImportService = ApplicationContextProvider.getBean("dbImportService");
        }
        return dbImportService;
    }

    public List<JobExecution> getRunningJobEvidence() {
        return runningJobEvidence;
    }

    public List<JobExecution> getRunningJobComplex() {
        return runningJobComplex;
    }

    public List<JobExecution> getCompletedJobEvidence() {
        return completedJobEvidence;
    }

    public List<JobExecution> getCompletedJobComplex() {
        return completedJobComplex;
    }
}