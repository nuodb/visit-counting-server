package com.nuodb.quick;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * Implements {@link VisitorInfo} using a simple in memory Map (equivalent to a
 * Dictionary). An instance of this class will only be created and used by
 * Spring if the {@code in-memory} profile is enabled.
 */
@Repository
@Profile("in-memory")
public class InMemoryVisitorInfo implements VisitorInfo {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private Map<String, Integer> visitors = new HashMap<>();

	public InMemoryVisitorInfo() {
		logger.info("Using in-memory storage map for visitor counts");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int previousVisits(String ipAddress) {
		Integer visitCount = visitors.get(ipAddress);
		return visitCount == null ? 0 : visitCount;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addVisit(String ipAddress) {
		Integer visitCount = visitors.get(ipAddress);

		if (visitCount == null)
			visitors.put(ipAddress, 1);
		else
			visitors.put(ipAddress, visitCount + 1);
	}

	@Override
	public StorageSetup storageSetup() {
		return StorageSetup.IN_MEMORY;
	}
}
