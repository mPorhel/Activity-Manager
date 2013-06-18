package org.activitymgr.core;

import java.io.InputStream;
import java.util.Properties;

import junit.framework.TestCase;
import junit.framework.TestResult;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public abstract class AbstractModelTestCase extends TestCase {

	/** Logger */
	private static Logger log = Logger.getLogger(AbstractModelTestCase.class);

	/** Model manager */
	private ModelMgr modelMgr;

	protected void setUp() throws Exception {
		// Initialisation des logs et chargement de la config
		PropertyConfigurator.configure(AbstractModelTestCase.class.getResource("tests.properties"));
		Properties props = new Properties();
		InputStream in = AbstractModelTestCase.class.getResourceAsStream("tests.properties");
		props.load(in);
		in.close();

		// Préfixe de config à utiliser
		String cfg = props.getProperty("dbconfig");
		// Initialisation de la connexion à la base de données
		String jdbcDriver = props.getProperty(cfg + "." + "driver");
		String jdbcUrl = props.getProperty(cfg + "." + "url");
		String jdbcUser = props.getProperty(cfg + "." + "user");
		String jdbcPassword = props.getProperty(cfg + "." + "password");
		modelMgr = new ModelMgr();
		modelMgr.initialize(
				jdbcDriver,
				jdbcUrl,
				jdbcUser,
				jdbcPassword
			);

	}
	
    /**
     * @see junit.framework.Test#run(junit.framework.TestResult)
     */
    public void run(final TestResult testResult) {
        log.error("");
        log.error("");
        log.error("");
        log.error("**********************************************************");
        log.error("*** STARTING TEST : '" + getName() + "'");
        log.error("**********************************************************");
        try {
            super.run(testResult);
        } finally {
            log.error("Test '" + getName() + "' done.");
        }
    }

	protected void tearDown() throws Exception {
		modelMgr.closeDatabaseAccess();
	}

	protected ModelMgr getModelMgr() {
		return modelMgr;
	}
}
