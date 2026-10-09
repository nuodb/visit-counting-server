package com.nuodb.quick;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * Extends {@link JdbcVisitorInfo} to create a data source for each TE and
 * explicitly connect to it. Uses the load-balancer connection property
 * {@code LBQuery}, adding {@code LBQuery=random(start_id(n))} where 'n' is the
 * start-id of each TE.
 * <p>
 * An instance of this class will only be created and used by Spring if the
 * {@code jdbc-lb} profile is enabled.
 */
@Repository
@Profile("jdbc-lb")
public class JdbcVisitorInfoWithLB extends JdbcVisitorInfo {

	/* - - - - - - - - - - S Q L S T A T E M E N T S - - - - - - - - - - */

	/** SQL to count the number of TEs */
	private static final String COUNT_TXN_NODES_SQL = //
			"SELECT count(*) FROM System.Nodes WHERE Type = 'Transaction'";

	/** SQL to get the start-id of each TE */
	private static final String GET_TXN_NODE_START_IDS_SQL = //
			"SELECT startId FROM System.Nodes WHERE Type = 'Transaction'";

	/* - - - - - - - - - - E R R O R M E S S A G E S - - - - - - - - - - */

	private static final String FAILED_SETTING_UP_DATASOURCES_ERROR_MSG = //
			"[{}] Failed setting up load-balanced data sources (aborting): {}";

	private Logger logger = LoggerFactory.getLogger(getClass());
	private DataSource[] dataSourceArray;
	private Random randomGenerator = new Random(System.currentTimeMillis());
	private int numTEs = 1;

	/**
	 * When an instance is created, it attempts to define the Visits table. Fails
	 * quietly if the table already exists.
	 * 
	 * @param dataSource
	 */
	public JdbcVisitorInfoWithLB(DataSource dataSource, DataSourceConfiguration dataSourceConfiguration) {
		super(dataSource, dataSourceConfiguration);

		try (Connection conn = dataSource.getConnection()) {
			PreparedStatement stmt = conn.prepareStatement(COUNT_TXN_NODES_SQL);
			ResultSet rs = stmt.executeQuery();

			if (rs.next())
				numTEs = rs.getInt(1);

			if (numTEs > 1) {
				dataSourceArray = new DataSource[numTEs];
				stmt = conn.prepareStatement(GET_TXN_NODE_START_IDS_SQL);
				rs = stmt.executeQuery();
				int i = 0;

				while (rs.next()) {
					dataSourceArray[i++] = dataSourceConfiguration.newDataSource(rs.getInt(1));
				}
			} else if (storageSetup == StorageSetup.NUO_LOCAL || System.getenv("SYSTEMDRIVE") != null) {
				dataSourceArray = new DataSource[3];
				dataSourceArray[0] = dataSourceConfiguration.newDataSource(2);
				dataSourceArray[1] = dataSourceConfiguration.newDataSource(2);
				dataSourceArray[2] = dataSourceConfiguration.newDataSource(2);
				numTEs = 3;
			}
		} catch (SQLException e) {
			if (!e.getLocalizedMessage().contains("already exists")) {
				logger.error(FAILED_SETTING_UP_DATASOURCES_ERROR_MSG, e.getClass().getSimpleName(),
						e.getLocalizedMessage());
				System.exit(-1);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataSource getDataSource() {
		if (dataSourceArray != null) {
			int nextDataSource = randomGenerator.nextInt(numTEs);
			logger.info("Using data source #{}", nextDataSource);
			return dataSourceArray[nextDataSource];
		} else {
			logger.info("Using default data source");
			return super.getDataSource();
		}
	}
}
