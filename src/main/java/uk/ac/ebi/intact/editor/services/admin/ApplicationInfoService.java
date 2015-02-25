package uk.ac.ebi.intact.editor.services.admin;

import com.google.common.collect.Maps;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.Source;
import uk.ac.ebi.intact.editor.config.EditorConfig;
import uk.ac.ebi.intact.editor.config.property.*;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.context.IntactConfiguration;
import uk.ac.ebi.intact.jami.dao.DbInfoDao;
import uk.ac.ebi.intact.jami.model.extension.IntactInteractor;
import uk.ac.ebi.intact.jami.model.extension.IntactPolymer;
import uk.ac.ebi.intact.jami.model.extension.IntactSource;
import uk.ac.ebi.intact.jami.model.meta.Application;
import uk.ac.ebi.intact.jami.model.meta.DbInfo;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtJAPI;

import javax.annotation.PostConstruct;
import javax.faces.model.SelectItem;
import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 */
@Service
@Scope( BeanDefinition.SCOPE_PROTOTYPE )
public class ApplicationInfoService extends AbstractEditorService {
    private static final Log log = LogFactory.getLog(ApplicationInfoService.class);
    private Map<Class, PropertyConverter> supportedPrimitiveConverter = Maps.newHashMap();

    public ApplicationInfoService() {
    }

    @PostConstruct
    public void init() {
        supportedPrimitiveConverter.put( java.lang.Boolean.TYPE, ApplicationContextProvider.getBean( BooleanPropertyConverter.class ) );
        supportedPrimitiveConverter.put( java.lang.Short.TYPE, ApplicationContextProvider.getBean( ShortPropertyConverter.class ) );
        supportedPrimitiveConverter.put( java.lang.Integer.TYPE, ApplicationContextProvider.getBean( IntegerPropertyConverter.class ) );
        supportedPrimitiveConverter.put( java.lang.Long.TYPE, ApplicationContextProvider.getBean( LongPropertyConverter.class ) );
        supportedPrimitiveConverter.put( java.lang.Float.TYPE, ApplicationContextProvider.getBean( FloatPropertyConverter.class ) );
        supportedPrimitiveConverter.put( java.lang.Double.TYPE, ApplicationContextProvider.getBean( DoublePropertyConverter.class ) );
        supportedPrimitiveConverter.put( java.lang.Character.TYPE, ApplicationContextProvider.getBean( CharPropertyConverter.class ) );
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED, readOnly = true)
    public ApplicationInfo getCurrentApplicationInfo() {
        String uniprotJapiVersion = UniProtJAPI.factory.getVersion();

        final DbInfoDao infoDao = getIntactDao().getDbInfoDao();

        String schemaVersion = getDbInfoValue(infoDao, DbInfo.SCHEMA_VERSION);
        String lastUniprotUpdate = getDbInfoValue(infoDao, DbInfo.LAST_PROTEIN_UPDATE);
        String lastCvUpdate = getDbInfoValue(infoDao, DbInfo.LAST_CV_UPDATE_PSIMI);

        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        PrintStream ps = new PrintStream(baos);
        String databaseCounts ="";
        try{
            printDatabaseCounts(ps);
            databaseCounts = baos.toString().replaceAll("\n","<br/>");
        }
        finally {
            ps.close();
        }

        return new ApplicationInfo(uniprotJapiVersion, schemaVersion, lastUniprotUpdate, lastCvUpdate, databaseCounts);
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED, readOnly = true)
    public Application reloadApplication(String ac) {
        return getIntactDao().getApplicationDao().getByAc(ac);
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public void saveApplicationProperties(Application application) throws SynchronizerException, FinderException, PersisterException {
        // attach dao to transaction manager to clear cache after commit
        attachDaoToTransactionManager();

        updateIntactObject(application, getIntactDao().getApplicationDao());
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public Application persistConfig(Application application, EditorConfig editorConfig, IntactConfiguration jamiConfiguration) throws SynchronizerException, FinderException, PersisterException {
        // attach dao to transaction manager to clear cache after commit
        attachDaoToTransactionManager();

        // update the Application object with the latest changes, since the last time the config was loaded
        if (log.isInfoEnabled()) log.info("Persisting configuration for application: "+application.getKey());

        Application dbApp = getIntactDao().getApplicationDao().getByKey(application.getKey());
        if (dbApp != null) {
            synchronizeApplication( application, dbApp );
            application = dbApp;
        }

        persistEditorConfigProperties(editorConfig, application);
        persistIntactConfigProperties(jamiConfiguration, application);

        // persist the application object
        updateIntactObject(application, getIntactDao().getApplicationDao());

        return getIntactDao().getApplicationDao().getByKey(application.getKey());
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED)
    public IntactSource synchronizeDefaultSource(Source defaultSource) throws SynchronizerException, FinderException, PersisterException {
        // attach dao to transaction manager to clear cache after commit
        attachDaoToTransactionManager();

        if (log.isInfoEnabled()) log.info("Persisting or retrieving default source: "+defaultSource);

        return synchronizeIntactObject(defaultSource, getIntactDao().getSynchronizerContext().getSourceSynchronizer(), true);
    }

    @Transactional(value = "jamiTransactionManager", propagation = Propagation.REQUIRED, readOnly = true)
    public List<SelectItem> getAvailableInstitutions() {
        Collection<IntactSource> allSources = getIntactDao().getSourceDao().getAll();
        List<SelectItem> institutionItems = new ArrayList<SelectItem>(allSources.size());

        for (IntactSource source : allSources){
            if (source.getShortName().equalsIgnoreCase("intact")){
                institutionItems.add(0, new SelectItem(source, source.getShortName(), source.getFullName()));
            }
            else{
                institutionItems.add(new SelectItem(source, source.getShortName(), source.getFullName()));
            }
        }
        return institutionItems;
    }

    public List<Map.Entry<String,DataSource>> getDataSources() {
        return new ArrayList<Map.Entry<String,DataSource>>(
                ApplicationContextProvider.getApplicationContext().getBeansOfType(DataSource.class).entrySet());
    }

    public List<Map.Entry<String,PlatformTransactionManager>> getTransactionManagers() {
        return new ArrayList<Map.Entry<String,PlatformTransactionManager>>(
                ApplicationContextProvider.getApplicationContext().getBeansOfType(PlatformTransactionManager.class).entrySet());
    }

    public String[] getBeanNames() {
        return ApplicationContextProvider.getApplicationContext().getBeanDefinitionNames();
    }

    private void persistApplicationProperty(String key, String value, Application application){

        uk.ac.ebi.intact.jami.model.meta.ApplicationProperty applicationProperty = application.getProperty(key);

        if (applicationProperty != null) {
            // set the property
            applicationProperty.setValue(value);

            if (log.isDebugEnabled()) {
                log.debug("Loading field (db): "+key+"="+value);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Loading field (beanWithConfig object): "+key+"="+value);
            }

            // create the property
            applicationProperty = new uk.ac.ebi.intact.jami.model.meta.ApplicationProperty(key, value);
            application.addProperty(applicationProperty);
        }
    }

    private void persistIntactConfigProperties(IntactConfiguration jamiConfiguration, Application application) {

        persistApplicationProperty("intactConfig.localCvPrefix",jamiConfiguration.getLocalCvPrefix(), application);
        persistApplicationProperty("intactConfig.acPrefix",jamiConfiguration.getAcPrefix(), application);
        persistApplicationProperty("intactConfig.defaultInstitution",jamiConfiguration.getDefaultInstitution().getShortName(), application);
    }

    private void persistEditorConfigProperties(EditorConfig editorConfig, Application application) {
        if (editorConfig.getInstanceName() != null){
            persistApplicationProperty("editorConfig.instanceName",editorConfig.getInstanceName(), application);
        }
        if (editorConfig.getLogoUrl() != null){
            persistApplicationProperty("editorConfig.logoUrl",editorConfig.getLogoUrl(), application);
        }
        if (editorConfig.getGoogleUsername() != null){
            persistApplicationProperty("editorConfig.googleUsername",editorConfig.getGoogleUsername(), application);
        }
        if (editorConfig.getGooglePassword() != null){
            persistApplicationProperty("editorConfig.googlePassword",editorConfig.getGooglePassword(), application);
        }
        persistApplicationProperty("editorConfig.defaultStoichiometry",Integer.toString(editorConfig.getDefaultStoichiometry()), application);

        if (editorConfig.getDefaultCurationDepth() != null){
            persistApplicationProperty("editorConfig.defaultCurationDepth",editorConfig.getGooglePassword(), application);
        }
        persistApplicationProperty("editorConfig.revertDecisionTime",Integer.toString(editorConfig.getRevertDecisionTime()), application);

        if (editorConfig.getTheme() != null){
            persistApplicationProperty("editorConfig.theme",editorConfig.getTheme(), application);
        }

    }

    private String getDbInfoValue(DbInfoDao infoDao, String key) {
        String value;
        uk.ac.ebi.intact.jami.model.meta.DbInfo dbInfo = infoDao.get(key);
        if (dbInfo != null) {
            value = dbInfo.getValue();
        } else {
            value = "<unknown>";
        }
        return value;
    }

    private void printDatabaseCounts(PrintStream ps) {

        ps.println("Publications: "+ getIntactDao().getPublicationDao().countAll());
        ps.println("Experiments: "+ getIntactDao().getExperimentDao().countAll());
        ps.println("Interactors: "+ getIntactDao().getInteractorDao(IntactInteractor.class).countAll());
        ps.println("\tInteractions: "+ getIntactDao().getInteractionDao().countAll());
        ps.println("\tComplexes: "+ getIntactDao().getComplexDao().countAll());
        ps.println("\tTotal Polymers: " + getIntactDao().getPolymerDao(IntactPolymer.class).countAll());
        ps.println("\t\tProteins: "+ getIntactDao().getProteinDao().countAll());
        ps.println("\t\tNucleic Acids: "+ getIntactDao().getNucleicAcidDao().countAll());
        ps.println("\tSmall molecules: " + getIntactDao().getBioactiveEntityDao().countAll());
        ps.println("\tTotal molecules: " + getIntactDao().getMoleculeDao().countAll());
        ps.println("\tInteractor pool: " + getIntactDao().getInteractorPoolDao().countAll());
        ps.println("Components: "+ getIntactDao().getParticipantEvidenceDao().countAll());
        ps.println("Features: "+ getIntactDao().getFeatureEvidenceDao().countAll());
        ps.println("Complex participants: "+ getIntactDao().getModelledParticipantDao().countAll());
        ps.println("Complex Features: "+ getIntactDao().getModelledFeatureDao().countAll());
        ps.println("CvObjects: "+ getIntactDao().getCvTermDao().countAll());
        ps.println("BioSources: "+ getIntactDao().getOrganismDao().countAll());
        ps.println("Institutions: "+ getIntactDao().getSourceDao().countAll());

    }

    /**
     * Synchronize all attributes of source onto target.
     * @param source the source application
     * @param target the target application
     */
    private void synchronizeApplication( Application source, Application target ) {
        target.setDescription( source.getDescription() );

        // create in target missing properties and update existing ones
        for ( uk.ac.ebi.intact.jami.model.meta.ApplicationProperty sourceProperty : source.getProperties() ) {
            final uk.ac.ebi.intact.jami.model.meta.ApplicationProperty targetProperty = target.getProperty( sourceProperty.getKey() );
            if( targetProperty != null ) {
                targetProperty.setValue( sourceProperty.getValue() );
            } else {
                target.addProperty( sourceProperty );
            }
        }
    }

    public class ApplicationInfo implements Serializable {

        private String uniprotJapiVersion;
        private String schemaVersion;
        private String lastUniprotUpdate;
        private String lastCvUpdate;
        private String databaseCounts;

        private ApplicationInfo(String uniprotJapiVersion, String schemaVersion, String lastUniprotUpdate, String lastCvUpdate, String databaseCounts) {
            this.uniprotJapiVersion = uniprotJapiVersion;
            this.schemaVersion = schemaVersion;
            this.lastUniprotUpdate = lastUniprotUpdate;
            this.lastCvUpdate = lastCvUpdate;
            this.databaseCounts = databaseCounts;
        }

        public String getUniprotJapiVersion() {
            return uniprotJapiVersion;
        }

        public String getSchemaVersion() {
            return schemaVersion;
        }

        public String getLastUniprotUpdate() {
            return lastUniprotUpdate;
        }

        public String getLastCvUpdate() {
            return lastCvUpdate;
        }

        public String getDatabaseCounts() {
            return databaseCounts;
        }
    }
}
