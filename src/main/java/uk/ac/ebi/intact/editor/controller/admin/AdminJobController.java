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
package uk.ac.ebi.intact.editor.controller.admin;

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
import uk.ac.ebi.intact.jami.ApplicationContextProvider;

import javax.annotation.Resource;
import javax.faces.component.UIParameter;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ComponentSystemEvent;
import java.util.*;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope( "conversation.access" )
@ConversationName( "admin" )
@Lazy
public class AdminJobController extends BaseController {

    @Resource( name = "psiMIJobManager" )
    private transient MIBatchJobManager psiMIJobManager;

    @Resource( name = "intactJobExplorer" )
    private transient JobExplorer jobExplorer;

    @Resource( name = "intactJobLauncher" )
    private transient JobLauncher intactJobLauncher;

    private static final Log log = LogFactory.getLog(AdminJobController.class);

    public AdminJobController() {
    }

    public void load( ComponentSystemEvent event ) {

        if (!FacesContext.getCurrentInstance().isPostback()) {

            log.debug( "Load job summary" );
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
            } catch ( JobInstanceAlreadyCompleteException e ) {
                addErrorMessage( "Job is already complete "+e.getMessage(), "Execution ID: " + executionId );
                e.printStackTrace();
            } catch ( NoSuchJobExecutionException e ) {
                addErrorMessage( "Job execution does not exist"+e.getMessage(), "Execution ID: " + executionId );
                e.printStackTrace();
            } catch ( NoSuchJobException e ) {
                addErrorMessage( "Job does not exist"+e.getMessage(), "Execution ID: " + executionId );
                e.printStackTrace();
            } catch ( JobRestartException e ) {
                addErrorMessage( "Problem restarting job"+e.getMessage(), "Execution ID: " + executionId );
                e.printStackTrace();
            } catch ( JobParametersInvalidException e ) {
                addErrorMessage( "Job parameters are invalid"+e.getMessage(), "Execution ID: " + executionId );
                e.printStackTrace();
            } catch (NoSuchJobInstanceException e) {
                addErrorMessage("Job instance does not exist"+e.getMessage(), "Execution ID: " + executionId);
                e.printStackTrace();
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
            addErrorMessage( "Job does not exist "+e.getMessage(), "Execution ID: " + executionId );
            e.printStackTrace();
        } catch ( JobExecutionNotRunningException e ) {
            addErrorMessage( "Job is not running anymore "+e.getMessage(), "Execution ID: " + executionId );
            e.printStackTrace();
        }
    }

    public List<JobExecution> getRunningJobExecutions( String jobName ) {
        return new ArrayList<JobExecution>( getJobExplorer().findRunningJobExecutions(jobName) );
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

    public Map<String,String> getImportStatistics( JobExecution jobExecution ) {
        Collection<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        Map<String,String> statistics = new HashMap<String, String>();
        if (!stepExecutions.isEmpty()){
            StepExecution firstStep = stepExecutions.iterator().next();
            ExecutionContext stepContext = firstStep.getExecutionContext();

            for (Map.Entry<String, Object> entry : stepContext.entrySet()){
                // persisted count
                if (entry.getKey().startsWith(AbstractIntactDbImporter.PERSIST_MAP_COUNT)){
                    statistics.put(entry.getKey().substring(entry.getKey().lastIndexOf("_")+1), Integer.toString((Integer)entry.getValue()));
                }
            }
        }
        return statistics;
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
}