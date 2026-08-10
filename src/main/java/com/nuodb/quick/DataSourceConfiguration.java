package com.nuodb.quick;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.nuodb.quick.VisitorInfo.StorageSetup;

/**
 * Dedicated Spring configuration, just to detect how the DataSource has been
 * configured. Also checks the {@link DataSource} is valid. Only used when the
 * Spring Profile "jdbc" is enabled.
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
	 * Check the data source is valid (possible to create a connection), but if not
	 * close the Spring application context. Also logs the value of the Spring Boot
	 * database URL property {@code spring.datasource.url}.
	 * 
	 * @param url        The database URL used to configure the data source (from
	 *                   {@code application.properties}).
	 * @param dataSource The data source defined by Spring Boot.
	 * @param context    The Spring application context.
	 */
	public DataSourceConfiguration(@Value("spring.datasource.url") String url, DataSource dataSource,
			ConfigurableApplicationContext context) {
		// String url = env.getProperty("spring.datasource.url");
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
