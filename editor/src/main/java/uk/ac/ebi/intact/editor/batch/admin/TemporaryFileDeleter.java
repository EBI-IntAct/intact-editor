package uk.ac.ebi.intact.editor.batch.admin;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This job listener will delete any temporary file created with job id successful
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>17/12/14</pre>
 */

public class TemporaryFileDeleter implements StepExecutionListener {

    private List<String> filesToDeletes;

    public List<String> getFilesToDeletes() {
        return filesToDeletes;
    }

    public void setFilesToDeletes(List<String> filesToDeletes) {
        this.filesToDeletes = filesToDeletes;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        String errorFile = stepExecution.getJobParameters().getString("error.file");
        String file = stepExecution.getJobParameters().getString("input.file");
        this.filesToDeletes = new ArrayList<String>();
        if (errorFile != null){
            this.filesToDeletes.add(errorFile);
        }
        if (file != null){
            this.filesToDeletes.add(file);
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.getExitStatus() != null && stepExecution.getExitStatus().equals(ExitStatus.COMPLETED)){
            for (String fileName : filesToDeletes){
                File file = new File(fileName);
                if (file.exists()){
                    file.delete();
                }
            }
        }
        return stepExecution.getExitStatus();
    }
}
