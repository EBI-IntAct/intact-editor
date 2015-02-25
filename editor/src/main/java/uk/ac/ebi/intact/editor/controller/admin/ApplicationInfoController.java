package uk.ac.ebi.intact.editor.controller.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.PlatformTransactionManager;
import psidev.psi.mi.jami.model.Source;
import uk.ac.ebi.intact.editor.config.EditorConfig;
import uk.ac.ebi.intact.editor.controller.BaseController;
import uk.ac.ebi.intact.editor.services.admin.ApplicationInfoService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.context.IntactConfiguration;
import uk.ac.ebi.intact.jami.model.extension.IntactSource;
import uk.ac.ebi.intact.jami.model.meta.Application;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import javax.annotation.Resource;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller("applicationInfo")
@Scope("session")
@Lazy
public class ApplicationInfoController extends BaseController {

    private String uniprotJapiVersion;
    private String schemaVersion;
    private String lastUniprotUpdate;
    private String lastCvUpdate;
    private String databaseCounts;

    private static final Log log = LogFactory.getLog(ApplicationInfoController.class);

    @Resource(name = "defaultApp")
    private Application application;

    @Resource(name ="applicationInfoService")
    private transient ApplicationInfoService applicationInfoService;

    @Resource(name ="intactJamiConfiguration")
    private transient IntactConfiguration intactConfiguration;

    @Resource(name = "editorConfig")
    private transient EditorConfig editorConfig;

    private boolean isInitialised = false;

    public ApplicationInfoController() {
    }

    public synchronized void init() {
        ApplicationInfoService.ApplicationInfo appInfo = getApplicationInfoService().getCurrentApplicationInfo();
        uniprotJapiVersion = appInfo.getUniprotJapiVersion();
        schemaVersion = appInfo.getSchemaVersion();
        lastUniprotUpdate = appInfo.getLastUniprotUpdate();
        lastCvUpdate = appInfo.getLastCvUpdate();
        databaseCounts = appInfo.getDatabaseCounts();
        isInitialised = true;
    }

    public void saveApplicationProperties(ActionEvent evt) {
        // lock application
        synchronized (this.application){
            try {
                getApplicationInfoService().getIntactDao().getUserContext().setUser(getCurrentUser());
                getApplicationInfoService().saveApplicationProperties(this.application);
            } catch (SynchronizerException e) {
                addErrorMessage("Cannot save application details ", e.getCause() + ": " + e.getMessage());
                log.error("Cannot save application details ", e);
            } catch (FinderException e) {
                addErrorMessage("Cannot save application details ", e.getCause() + ": " + e.getMessage());
                log.error("Cannot save application details ", e);
            } catch (PersisterException e) {
                addErrorMessage("Cannot save application details ", e.getCause() + ": " + e.getMessage());
                log.error("Cannot save application details ", e);
            }catch (Throwable e) {
                addErrorMessage("Cannot save application details ", e.getCause() + ": " + e.getMessage());
                log.error("Cannot save application details ", e);
            }
        }
    }

    public void persistConfig(ActionEvent evt) {
        // lock application
        synchronized (this.application){
            try {
                getApplicationInfoService().getIntactDao().getUserContext().setUser(getCurrentUser());
                this.application = getApplicationInfoService().persistConfig(this.application, getEditorConfig(), getIntactConfiguration());
            } catch (SynchronizerException e) {
                addErrorMessage("Cannot save application details ", e.getCause()+": "+e.getMessage());
                log.error("Cannot save application details ", e);
            } catch (FinderException e) {
                addErrorMessage("Cannot save application details ", e.getCause() + ": " + e.getMessage());
                log.error("Cannot save application details ", e);
            } catch (PersisterException e) {
                addErrorMessage("Cannot save application details ", e.getCause() + ": " + e.getMessage());
                log.error("Cannot save application details ", e);
            }catch (Throwable e) {
                addErrorMessage("Cannot save application details ", e.getCause() + ": " + e.getMessage());
                log.error("Cannot save application details ", e);
            }
        }
    }

    public List<SelectItem> getAvailableInstitutions() {
        return getApplicationInfoService().getAvailableInstitutions();
    }

    public List<Map.Entry<String,DataSource>> getDataSources() {
        return getApplicationInfoService().getDataSources();
    }

    public List<Map.Entry<String,PlatformTransactionManager>> getTransactionManagers() {
        return getApplicationInfoService().getTransactionManagers();
    }

    public String[] getBeanNames() {
        return getApplicationInfoService().getBeanNames();
    }

    public synchronized String getUniprotJapiVersion() {
        if (!isInitialised){
           init();
        }
        return uniprotJapiVersion;
    }

    public List<String> getSystemPropertyNames() {
        return new ArrayList<String>(System.getProperties().stringPropertyNames());
    }

    public String getSystemProperty(String propName) {
        return System.getProperty(propName);
    }

    public synchronized String getSchemaVersion() {
        if (!isInitialised){
            init();
        }
        return schemaVersion;
    }

    public synchronized String getLastUniprotUpdate() {
        if (!isInitialised){
            init();
        }
        return lastUniprotUpdate;
    }

    public synchronized String getLastCvUpdate() {
        if (!isInitialised){
            init();
        }
        return lastCvUpdate;
    }

    public synchronized String getDatabaseCounts() {
        if (!isInitialised){
            init();
        }
        return databaseCounts;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public ApplicationInfoService getApplicationInfoService() {
        if (this.applicationInfoService == null){
            this.applicationInfoService = ApplicationContextProvider.getBean("applicationInfoService");
        }
        return applicationInfoService;
    }

    public IntactConfiguration getIntactConfiguration() {
        if (this.intactConfiguration == null){
            this.intactConfiguration = ApplicationContextProvider.getBean("intactJamiConfiguration");
        }
        return intactConfiguration;
    }

    public EditorConfig getEditorConfig() {
        if (this.editorConfig == null){
            this.editorConfig = ApplicationContextProvider.getBean("editorConfig");
        }
        return editorConfig;
    }

    public synchronized IntactSource getDefaultInstitution(){
        Source defaultSource = getIntactConfiguration().getDefaultInstitution();
        if (!(defaultSource instanceof IntactSource) || ((IntactSource)defaultSource).getAc() == null){
            try {
                getApplicationInfoService().getIntactDao().getUserContext().setUser(getCurrentUser());
                defaultSource = getApplicationInfoService().synchronizeDefaultSource(defaultSource);
                setDefaultInstitution((IntactSource)defaultSource);
            } catch (SynchronizerException e) {
                addErrorMessage("Cannot save default source "+defaultSource, e.getCause()+": "+e.getMessage());

            } catch (FinderException e) {
                addErrorMessage("Cannot save default source "+defaultSource, e.getCause() + ": " + e.getMessage());
            } catch (PersisterException e) {
                addErrorMessage("Cannot save default source "+defaultSource, e.getCause() + ": " + e.getMessage());
            }catch (Throwable e) {
                addErrorMessage("Cannot save default source "+defaultSource, e.getCause() + ": " + e.getMessage());
            }
        }

        return (IntactSource)defaultSource;
    }

    public synchronized void setDefaultInstitution(IntactSource source){
        getIntactConfiguration().setDefaultInstitution(source);
    }
}
