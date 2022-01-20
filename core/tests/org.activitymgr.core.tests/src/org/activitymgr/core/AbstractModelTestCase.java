package org.activitymgr.core;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import junit.framework.TestCase;
import junit.framework.TestResult;

import org.activitymgr.core.dto.IDTOFactory;
import org.activitymgr.core.model.CoreModelModule;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.util.DbHelper;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;

public abstract class AbstractModelTestCase extends TestCase implements
		Provider<Connection> {

	/** Logger */
	private static Logger log = Logger.getLogger(AbstractModelTestCase.class);

	private static BasicDataSource datasource;
	static {
		// Ensure DbHelper.class is loaded (otherwise, raises an exception in the other thread)
		@SuppressWarnings("unused")
		Class<?> c = DbHelper.class;
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					Connection con = datasource.getConnection();
					if (DbHelper.isEmbeddedHsqlOrH2(con, datasource.getUrl())) {
						DbHelper.shutdowHsqlOrH2(con);
					}
					datasource.close();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		});

		// Initialisation des logs et chargement de la config
		Properties props = new Properties();
		InputStream in = AbstractModelTestCase.class
				.getResourceAsStream("tests.properties");
		try {
			props.load(in);
			in.close();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		PropertyConfigurator.configure(props);

		// Préfixe de config à utiliser
		String cfg = props.getProperty("dbconfig");
		// Initialisation de la connexion à la base de données
		String jdbcDriver = props.getProperty(cfg + "." + "driver");
		String jdbcUrl = props.getProperty(cfg + "." + "url");
		String jdbcUser = props.getProperty(cfg + "." + "user");
		String jdbcPassword = props.getProperty(cfg + "." + "password");

		// Database connection
		datasource = new BasicDataSource();
		datasource.setDriverClassName(jdbcDriver);
		datasource.setUrl(jdbcUrl);
		datasource.setUsername(jdbcUser);
		datasource.setPassword(jdbcPassword);
		datasource.setDefaultAutoCommit(false);
	}

	/** Model manager */
	private IModelMgr modelMgr;

	/** The test transaction */
	private Connection tx;

	/** Guice injector */
	private Injector injector;

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		tx = datasource.getConnection();

		// Create Guice injector
		List<Module> modules = getGuiceModules();
		injector = Guice.createInjector(modules);

		// Retrieve model manager instance
		final IModelMgr modelMgr = injector.getInstance(IModelMgr.class);
		this.modelMgr = (IModelMgr) Proxy.newProxyInstance(
				AbstractModelTestCase.class.getClassLoader(),
				new Class<?>[] { IModelMgr.class }, new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method,
							Object[] args) throws Throwable {
						try {
							Object result = method.invoke(modelMgr, args);
							tx.commit();
							return result;
						} catch (InvocationTargetException t) {
							t.getCause().printStackTrace();
							tx.rollback();
							throw t.getCause();
						}
					}
				});

		// Inject in the test itself
		injector.injectMembers(this);
		
		// Recreate tables
		if (dropTablesBeforeTest())
			getModelMgr().createTables();
	}

	protected boolean dropTablesBeforeTest() {
		return true;
	}
	
	/**
	 * 
	 * @return
	 */
	protected List<Module> getGuiceModules() {
		ArrayList<Module> modules = new ArrayList<Module>();
		modules.add(new CoreModelModule());
		modules.add(new AbstractModule() {
			@Override
			protected void configure() {
				bind(Connection.class).toProvider(
						AbstractModelTestCase.this);
			}
		});
		return modules;
	}

	/**
	 * @return the injector instance.
	 */
	protected Injector getInjector() {
		return injector;
	}

	@Override
	public Connection get() {
		return tx;
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
		tx.close();
	}

	protected IModelMgr getModelMgr() {
		return modelMgr;
	}

	protected IDTOFactory getFactory() {
		return injector.getInstance(IDTOFactory.class);
	}

	protected Calendar cal(int year, int month, int day) {
		Calendar start = Calendar.getInstance(Locale.FRANCE);
		start.set(Calendar.YEAR, year);
		start.set(Calendar.MONTH, month - 1);
		start.set(Calendar.DAY_OF_MONTH, day);
		return start;
	}

}
