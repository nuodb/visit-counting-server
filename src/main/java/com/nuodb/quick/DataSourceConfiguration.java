package com.nuodb.quick;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import com.nuodb.quick.VisitorInfo.StorageSetup;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Dedicated Spring configuration, just to setup the data source. Overrides
 * Spring Boot's default DataSource setup. An instance of this configuration
 * will only be created and used if the {@code jdbc} profile is enabled.
 */
@Configuration
@Profile("jdbc")
public class DataSourceConfiguration {

	/**
	 * The name of NuoDB's JDBC driver class.
	 */
	public static final String NUODB_DRIVER_CLASS_NAME = "com.nuodb.jdbc.Driver";

	private Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Spring's own Environment.
	 */
	private Environment env;

	/**
	 * Save how the JDBC setup was configured - using Spring properties defined in
	 * {@code application.properties} file or environment variables (set by NuoDBaaS
	 * in your container or manually to emulate such an environment in tests).
	 */
	private StorageSetup storageSetup;

	/**
	 * Save the Spring Environment for use by {@link #dataSource()}.
	 * 
	 * @param env The Spring environment - includes all the properties in
	 *            {@code application.properties}.
	 */
	public DataSourceConfiguration(Environment env) {
		this.env = env;
		logger.info(" >>> DataSourceConfiguration");
	}

	/**
	 * This overrides Spring Boot's default setup, so we can configure with or
	 * without NuoDBaaS. With NuoDBaaS we use environment variables, without we
	 * fetch properties from the Spring Environment. By default Spring invokes any
	 * method annotated by {@code @Bean} just once.
	 * 
	 * @return An Hikari DataSource
	 */
	@Bean
	public DataSource datasource() {

		logger.info(" >>> Create data source");

		// Look for one of the environment variables NuoDBaaS provides
		String adminHost = System.getenv("NUODB_ADMIN_ENDPOINT");
		String url;
		String username;
		String password;

		if (adminHost != null) {
			// Using NuoDBaaS - get configuration from the environment;
			String cert = System.getenv("NUODB_CA_PEM");
			String dbName = System.getenv("NUODB_DB_NAME");
			url = "jdbc:com.nuodb://" + adminHost + '/' + dbName
					+ (cert == null ? "" : "?" + "trustedCertificate=" + cert);
			username = System.getenv("NUODB_DB_USER");
			password = System.getenv("NUODB_DB_PASSWORD");
			logger.info(" >>> Using NuoDBaaS environment variables");
			storageSetup = StorageSetup.NUO_DBAAS;
		} else {
			// Do what Spring Boot does - use the properties in 'application.properties'.
			// You must have a database called 'demo' running on your local machine.
			url = env.getProperty("spring.datasource.url");
			password = env.getProperty("spring.datasource.password");
			username = env.getProperty("spring.datasource.username");
			logger.info(" >>> Using Spring datasource properties");
			storageSetup = StorageSetup.NUO_SPRING;
		}

		// Create a Hikari datasource - other DataSources are supported.
		HikariConfig hconfig = new HikariConfig();
		hconfig.setDriverClassName(NUODB_DRIVER_CLASS_NAME);
		hconfig.setJdbcUrl(url);
		hconfig.setUsername(username);
		hconfig.setPassword(password);

		DataSource ds = new HikariDataSource(hconfig);
		logger.info(" >>> Returning a data source: {} for {}@{}", ds, username, url);
		return ds;
	}

	/**
	 * Get the storage setup that was used.
	 * 
	 * @return Either {@link StorageSetup#NUO_DBAAS} or
	 *         {@link StorageSetup#NUO_SPRING}.
	 */
	public StorageSetup getStorageSetup() {
		return storageSetup;
	}
}
