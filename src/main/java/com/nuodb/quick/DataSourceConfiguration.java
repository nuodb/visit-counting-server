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
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Dedicated Spring configuration, just to detect how the DataSource has been
 * configured. Also checks the {@link DataSource} is valid. Only used when the
 * Spring Profile "jdbc" is enabled.
 */
@Configuration
@Profile({"jdbc", "jdbc-lb"})
public class DataSourceConfiguration {

	private Logger logger = LoggerFactory.getLogger("com.nuodb.quick.DataSourceConfiguration");

	/**
	 * Save how the JDBC setup was configured - using Spring properties defined in
	 * {@code application.properties} file or environment variables (set by NuoDBaaS
	 * in your container or manually to emulate such an environment in tests).
	 */
	private StorageSetup storageSetup;

	private String url;

	private String user;

	private String pwd;

	/**
	 * Check the data source is valid (is able to create a connection), but if not
	 * close the Spring application context and throw a runtime exception. Also logs
	 * the value of the Spring Boot database URL property
	 * {@code spring.datasource.url}.
	 * 
	 * @param url        The database URL used to configure the data source (set by
	 *                   Spring from the {@code spring.datasource.url} property in
	 *                   {@code application.properties}).
	 * @param user       The user to connect as (set by Spring from the
	 *                   {@code spring.datasource.username} property in
	 *                   {@code application.properties}).
	 * @param pwd        The user's password (set by Spring from the
	 *                   {@code spring.datasource.password} property in
	 *                   {@code application.properties}).
	 * @param dataSource The data source auto-defined by Spring Boot using the
	 *                   properties in {@code application.properties}.
	 * @param context    The Spring application context.
	 */
	public DataSourceConfiguration(@Value("${spring.datasource.url}") String url, //
			@Value("${spring.datasource.username}") String user, @Value("${spring.datasource.password}") String pwd, //
			DataSource dataSource, ConfigurableApplicationContext context) {
		this.url = url;
		this.user = user;
		this.pwd = pwd;

		// NEVER log the password in a production application
		logger.info("Database URL = {} -> {}:{}", url, user, pwd);

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
	 * Get the storage setup that was used. A NuoDB database running locally, one
	 * provisioned by the NuoDB Component ({@code ds-nuodb}) or one by NuoDBaas
	 * ({@code ds-nuodbaas}).
	 * 
	 * @return One of {@link StorageSetup#NUO_COMPONENT} or
	 *         {@link StorageSetup#NUO_DBAAS} {@link StorageSetup#NUO_LOCAL}.
	 */
	public StorageSetup getStorageSetup() {
		if (System.getenv("NUODB_ADMIN_SERVICE") != null)
			storageSetup = StorageSetup.NUO_COMPONENT;
		else {
			String adminHost = System.getenv("NUODB_ADMIN_ENDPOINT");
			storageSetup = adminHost != null ? StorageSetup.NUO_DBAAS : StorageSetup.NUO_LOCAL;
		}
		return storageSetup;
	}

	/**
	 * Create a data source that explicitly connects only to the TE with the
	 * specified start-id.
	 * 
	 * @param startId The start-id of a TE.
	 * @return The new data source.
	 */
	public DataSource newDataSource(int startId) {
		HikariConfig config = new HikariConfig();
		String jdbcUrl = url + (url.indexOf('?') == -1 ? '?' : '&') + "LBQuery=random(start_id(" + startId + "))";
		config.setJdbcUrl(jdbcUrl);
		config.setUsername(user);
		config.setPassword(pwd);
		logger.info("New data source: {}", jdbcUrl);
		return new HikariDataSource(config);
	}
}
