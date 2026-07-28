package com.nuodb.quick;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * Implements {@link VisitorInfo} using a {@code Visits} table in a NuoDB
 * database. This is a deliberately simple use of NuoDB with a single table as
 * the point is to demonstrate NuoDBaaS, not NuoDB.
 * <p>
 * An instance of this class will only be created and used by Spring if the
 * {@code jdbc} profile is enabled.
 */
@Repository
@Profile("jdbc")
public class JdbcVisitorInfo implements VisitorInfo {

	/* - - - - - - - - - - S Q L    S T A T E M E N T S - - - - - - - - - - */

	/** SQL to create the Visits table */
	private static final String CREATE_VISIT_TABLE_SQL = //
			"CREATE TABLE Visits(Id BIGINT GENERATED ALWAYS AS IDENTITY, "
					+ "Source STRING NOT NULL, Count INT NOT NULL)";

	/**
	 * SQL to count the number of previous visits for the specified source
	 * (requester IP address).
	 */
	private static final String SELECT_GET_VISIT_COUNT_SQL = //
			"SELECT count FROM Visits WHERE source = ?";

	/**
	 * SQL to find a given source (requester IP address) in the Visits table, if it
	 * exists.
	 */
	private static final String FIND_SOURCE_SQL = //
			"SELECT source FROM Visits WHERE source = ? FOR UPDATE";

	/** SQL to add a new source (requester IP address) to the Visits table */
	private static final String ADD_NEW_SOURCE_SQL = //
			"INSERT INTO Visits(source, count) VALUES(?, ?)";

	/**
	 * SQL to update an existing source (requester IP address) in the Visits table
	 * by incrementing its visit counter.
	 */
	private static final String UPDATE_VISIT_COUNT_SQL = //
			"UPDATE Visits SET count = count + 1 WHERE source = ?";

	/* - - - - - - - - - - E R R O R    M E S S A G E S - - - - - - - - - - */
	
	
	private static final String FAILED_GETTING_VISIT_COUNT_ERROR_MSG = //
			"[{}] Failed getting visit count for {}: {}";

	private static final String FAILED_SAVING_VISIT_COUNT_ERROR_MSG = //
			"[{}] Failed {} visit count for {}: {}";

	private static final String FAILED_CREATING_VISIT_TABLE_ERROR_MSG = //
			"[{}] Failed creating visit table (aborting): {}";

	
	private Logger logger = LoggerFactory.getLogger(getClass());
	private DataSource dataSource;
	private StorageSetup storageSetup;

	/**
	 * When an instance is created, it attempts to define the Visits table. Fails
	 * quietly if the table already exists.
	 * 
	 * @param dataSource
	 */
	public JdbcVisitorInfo(DataSource dataSource, DataSourceConfiguration dataSourceConfiguration) {
		this.dataSource = dataSource;
		logger.info("Using persistent storage for visitor counts");

		try (Connection conn = dataSource.getConnection()) {
			PreparedStatement stmt = conn.prepareStatement(CREATE_VISIT_TABLE_SQL);
			stmt.executeUpdate();
		} catch (SQLException e) {
			if (!e.getLocalizedMessage().contains("already exists")) {
				logger.error(FAILED_CREATING_VISIT_TABLE_ERROR_MSG, e.getClass().getSimpleName(),
						e.getLocalizedMessage());
				System.exit(-1);
			}
		}

		storageSetup = dataSourceConfiguration.getStorageSetup();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int previousVisits(String ipAddress) {
		int visitCount = -1;

		try (Connection conn = dataSource.getConnection()) {
			PreparedStatement stmt = conn.prepareStatement(SELECT_GET_VISIT_COUNT_SQL);
			stmt.setString(1, ipAddress);
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				visitCount = rs.getInt(1);
			} else {
				visitCount = 0;
			}
		} catch (SQLException e) {
			logger.error(FAILED_GETTING_VISIT_COUNT_ERROR_MSG, e.getClass().getSimpleName(), ipAddress,
					e.getLocalizedMessage());
		}

		return visitCount;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addVisit(String ipAddress) {
		logger.info("add new visit");
		String action = "getting";

		try (Connection conn = dataSource.getConnection()) {
			PreparedStatement stmt = conn.prepareStatement(FIND_SOURCE_SQL);
			stmt.setString(1, ipAddress);
			ResultSet rs = stmt.executeQuery();
			PreparedStatement stmt2 = null;
			boolean exists = rs.next();
			logger.info("{} exists = {}", ipAddress, exists);

			if (exists) {
				logger.info("DO UPDATE");
				stmt2 = conn.prepareStatement(UPDATE_VISIT_COUNT_SQL);
				stmt2.setString(1, ipAddress);
				action = "updating";
			} else {
				logger.info("DO INSERT");
				stmt2 = conn.prepareStatement(ADD_NEW_SOURCE_SQL);
				stmt2.setString(1, ipAddress);
				stmt2.setInt(2, 1);
				action = "inserting";
			}

			int rowsModified = stmt2.executeUpdate();
			logger.info("{} row(s) changed by {} SQL", rowsModified, action);
		} catch (

		SQLException e) {
			logger.error(FAILED_SAVING_VISIT_COUNT_ERROR_MSG, e.getClass().getSimpleName(), action, ipAddress,
					e.getLocalizedMessage());
		}

	}

	@Override
	public StorageSetup storageSetup() {
		return storageSetup;
	}
}
