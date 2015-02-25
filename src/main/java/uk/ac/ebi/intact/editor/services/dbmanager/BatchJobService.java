package uk.ac.ebi.intact.editor.services.dbmanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.sql.DataSource;

/**
 * Import service
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>23/01/15</pre>
 */
@Service
public class BatchJobService {
    private static final String SQL_DELETE_BATCH_STEP_EXECUTION_CONTEXT = "DELETE FROM BATCH_STEP_EXECUTION_CONTEXT WHERE STEP_EXECUTION_ID IN (SELECT STEP_EXECUTION_ID FROM BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID = ";
    private static final String SQL_DELETE_BATCH_STEP_EXECUTION = "DELETE FROM BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID = ";
    private static final String SQL_DELETE_BATCH_JOB_EXECUTION_CONTEXT = "DELETE FROM BATCH_JOB_EXECUTION_CONTEXT WHERE JOB_EXECUTION_ID = ";
    private static final String SQL_DELETE_BATCH_JOB_EXECUTION_PARAMS = "DELETE FROM BATCH_JOB_EXECUTION_PARAMS WHERE JOB_EXECUTION_ID = ";
    private static final String SQL_DELETE_BATCH_JOB_EXECUTION = "DELETE FROM BATCH_JOB_EXECUTION where JOB_EXECUTION_ID = ";
    private static final String SQL_DELETE_BATCH_JOB_INSTANCE = "DELETE FROM BATCH_JOB_INSTANCE WHERE JOB_INSTANCE_ID NOT IN (SELECT JOB_INSTANCE_ID FROM BATCH_JOB_EXECUTION)";

    private final Logger logger = LoggerFactory.getLogger(BatchJobService.class.getName());

    private JdbcTemplate jdbcTemplate;
    @Resource(name = "jamiJNDIDataSource")
    private DataSource dataSource;

    protected JdbcTemplate getJdbcTemplate() {
        if (jdbcTemplate == null){
            jdbcTemplate = new JdbcTemplate(dataSource);
        }
        return jdbcTemplate;
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public void deleteJob(Long jobId){
        int rowCount = getJdbcTemplate().update(SQL_DELETE_BATCH_STEP_EXECUTION_CONTEXT + "'"+jobId+"')");
        logger.info("Deleted rows number from the BATCH_STEP_EXECUTION_CONTEXT table: {}", rowCount);
        rowCount = getJdbcTemplate().update(SQL_DELETE_BATCH_STEP_EXECUTION+ "'"+jobId+"'");
        logger.info("Deleted rows number from the BATCH_STEP_EXECUTION table: {}", rowCount);
        rowCount = getJdbcTemplate().update(SQL_DELETE_BATCH_JOB_EXECUTION_CONTEXT+ "'"+jobId+"'");
        logger.info("Deleted rows number from the BATCH_JOB_EXECUTION_CONTEXT table: {}", rowCount);
        rowCount = getJdbcTemplate().update(SQL_DELETE_BATCH_JOB_EXECUTION_PARAMS+ "'"+jobId+"'");
        logger.info("Deleted rows number from the BATCH_JOB_EXECUTION_PARAMS table: {}", rowCount);
        rowCount = getJdbcTemplate().update(SQL_DELETE_BATCH_JOB_EXECUTION+ "'"+jobId+"'");
        logger.info("Deleted rows number from the BATCH_JOB_EXECUTION table: {}", rowCount);
        rowCount = getJdbcTemplate().update(SQL_DELETE_BATCH_JOB_INSTANCE);
        logger.info("Deleted rows number from the BATCH_JOB_INSTANCE table", rowCount);
    }
}
