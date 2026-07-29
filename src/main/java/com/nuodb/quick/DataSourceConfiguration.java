package com.nuodb.quick;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import com.nuodb.quick.VisitorInfo.StorageSetup;

/**
 * Dedicated Spring configuration, just to detect how the DataSource has been
 * configured.  Also checks the {@link DataSource} is valid.
 */
@Configuration
@Profile("jdbc")
public class DataSourceConfiguration {

	private Logger logger = LoggerFactory.getLogger("com.nuodb.quick.DataSourceConfiguration");

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
	public DataSourceConfiguration(Environment env, DataSource dataSource, ConfigurableApplicationContext context) {
		String url = env.getProperty("spring.datasource.url");
		logger.info("Database URL = {}", url);

		// Check connection is possible 
		try (Connection conn = dataSource.getConnection()) {
			; // OK
		} catch (SQLException e) {
			logger.error("Unable to connect to database: {}", e.getLocalizedMessage());
			context.close();
			throw new RuntimeException("Unable to connect to database: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Get the storage setup that was used.
	 * 
	 * @return Either {@link StorageSetup#NUO_DBAAS} or
	 *         {@link StorageSetup#NUO_SPRING}.
	 */
	public StorageSetup getStorageSetup() {
		String adminHost = System.getenv("NUODB_ADMIN_ENDPOINT");
		storageSetup = adminHost != null ? StorageSetup.NUO_DBAAS : StorageSetup.NUO_SPRING;
		return storageSetup;
	}
}
