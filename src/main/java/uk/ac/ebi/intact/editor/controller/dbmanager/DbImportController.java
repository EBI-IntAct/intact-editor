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

import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.primefaces.model.UploadedFile;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.jami.batch.MIBatchJobManager;
import psidev.psi.mi.jami.commons.MIFileUtils;
import uk.ac.ebi.intact.editor.controller.BaseController;
import uk.ac.ebi.intact.editor.controller.UserSessionController;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.user.User;

import javax.annotation.Resource;
import javax.faces.event.ActionEvent;
import java.io.File;
import java.io.IOException;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope( "conversation.access" )
@ConversationName( "dbmanager" )
public class DbImportController extends BaseController {

    @Resource( name = "psiMIJobManager" )
    private transient MIBatchJobManager psiMIJobManager;

    @Resource( name = "intactJobLauncher" )
    private transient JobLauncher intactJobLauncher;

    private UploadedFile uploadedFile;
    private String jobId = null;

    @Autowired
    private UserSessionController userSessionController;

    public DbImportController() {
    }

    public void launchFileImport(ActionEvent evt) {
        if (this.uploadedFile != null && this.uploadedFile.getFileName() != null){
            File[] files = saveUploadedFileTemporarily();
            if (files != null){
                try {
                    JobParametersBuilder builder = new JobParametersBuilder();
                    this.jobId = "interactionMixImport_"+System.currentTimeMillis();
                    String eMail = "intact-dev@ebi.ac.uk";
                    User user = userSessionController.getCurrentUser();
                    String userLogin = null;
                    if (user != null && user.getEmail() != null){
                        eMail = user.getEmail();
                        userLogin = user.getLogin();
                    }

                    getIntactJobLauncher().run((Job) ApplicationContextProvider.getBean("interactionMixImport"),
                            builder.addString("MIJobId", jobId).addString("input.file", files[0].getAbsolutePath())
                                    .addString("error.file", files[1].getAbsolutePath()).addString("email.recipient", eMail)
                                    .addString("user.login", userLogin).toJobParameters()
                    );

                    addInfoMessage( "Job started", "Job ID: " + jobId );
                } catch ( JobParametersInvalidException e ) {
                    addErrorMessage( "Invalid job parameters", "Job Param: " + "input.file="+files[0].getAbsolutePath()+"error.file"+files[1].getAbsolutePath() );
                    e.printStackTrace();
                    jobId = null;
                } catch (JobExecutionAlreadyRunningException e) {
                    addErrorMessage( "Job already running", "Job Param: " + "input.file="+files[0].getAbsolutePath()+"error.file"+files[1].getAbsolutePath());
                    e.printStackTrace();
                    jobId = null;
                } catch (JobRestartException e) {
                    addErrorMessage( "Job cannot be restarted", "Job Param: " + "input.file="+files[0].getAbsolutePath()+"error.file"+files[1].getAbsolutePath());
                    e.printStackTrace();
                    jobId = null;
                } catch (JobInstanceAlreadyCompleteException e) {
                    addErrorMessage( "Job already finished", "Job Param: " + "input.file="+files[0].getAbsolutePath()+"error.file"+files[1].getAbsolutePath());
                    e.printStackTrace();
                    jobId = null;
                }
            }
            else{
                addErrorMessage("Could not upload file "+uploadedFile.getFileName(), "Import failed");
                jobId = null;
            }
        }
        else{
            addErrorMessage("Could not upload file", "Import failed");
            jobId = null;
        }
    }

    public void launchComplexFileImport(ActionEvent evt) {
        if (this.uploadedFile != null && this.uploadedFile.getFileName() != null){
            File[] files = saveUploadedFileTemporarily();
            if (files != null){
                try {
                    JobParametersBuilder builder = new JobParametersBuilder();
                    this.jobId = "complexImport_"+System.currentTimeMillis();
                    String eMail = "intact-dev@ebi.ac.uk";
                    User user = userSessionController.getCurrentUser();
                    String userLogin = null;
                    if (user != null && user.getEmail() != null){
                        eMail = user.getEmail();
                        userLogin = user.getLogin();
                    }

                    getIntactJobLauncher().run((Job)ApplicationContextProvider.getBean("complexImport"),
                            builder.addString("MIJobId", jobId).addString("input.file", files[0].getAbsolutePath())
                                    .addString("error.file", files[1].getAbsolutePath()).addString("email.recipient", eMail)
                                    .addString("user.login", userLogin).toJobParameters());
                    addInfoMessage( "Job started", "Job ID: " + jobId );
                } catch ( JobParametersInvalidException e ) {
                    addErrorMessage( "Invalid job parameters", "Job Param: " + "input.file="+files[0].getAbsolutePath()+"error.file"+files[1].getAbsolutePath() );
                    e.printStackTrace();
                    jobId = null;
                } catch (JobExecutionAlreadyRunningException e) {
                    addErrorMessage( "Job already running", "Job Param: " + "input.file="+files[0].getAbsolutePath()+"error.file"+files[1].getAbsolutePath());
                    e.printStackTrace();
                    jobId = null;
                } catch (JobRestartException e) {
                    addErrorMessage( "Job cannot be restarted", "Job Param: " + "input.file="+files[0].getAbsolutePath()+"error.file"+files[1].getAbsolutePath());
                    e.printStackTrace();
                    jobId = null;
                } catch (JobInstanceAlreadyCompleteException e) {
                    addErrorMessage( "Job already finished", "Job Param: " + "input.file="+files[0].getAbsolutePath()+"error.file"+files[1].getAbsolutePath());
                    e.printStackTrace();
                    jobId = null;
                }
            }
            else{
                addErrorMessage("Could not upload file "+uploadedFile.getFileName(), "Import failed");
                jobId = null;
            }
        }
        else{
            addErrorMessage("Could not upload file", "Import failed");
            jobId = null;
        }
    }

    private Job getJob( String jobName ) {
        return ( Job ) getSpringContext().getBean( jobName );
    }

    public MIBatchJobManager getPsiMIJobManager() {
        if (this.psiMIJobManager == null){
            this.psiMIJobManager = ApplicationContextProvider.getBean("psiMIJobManager");
        }
        return psiMIJobManager;
    }

    public JobLauncher getIntactJobLauncher() {
        if (this.intactJobLauncher == null){
            this.intactJobLauncher = ApplicationContextProvider.getBean("intactJobLauncher");
        }
        return intactJobLauncher;
    }

    public UploadedFile getUploadedFile() {
        return uploadedFile;
    }

    public void setUploadedFile(UploadedFile uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

    public String getJobId() {
        return jobId;
    }

    private File[] saveUploadedFileTemporarily(){
        try {
            String fileName = System.currentTimeMillis()+"_"+this.uploadedFile.getFileName();
            String errorFileName = fileName+"_errors";
            File file = MIFileUtils.storeAsTemporaryFile(this.uploadedFile.getInputstream(), fileName, null);
            File errorFile = File.createTempFile(errorFileName, "txt");
            return new File[]{file, errorFile};
        } catch (IOException e) {
            addErrorMessage("Cannot upload file " + this.uploadedFile.getFileName(), e.getCause() + ": " + e.getMessage());
        }
        return null;
    }
}
