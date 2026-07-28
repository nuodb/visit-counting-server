package com.nuodb.quick;

/**
 * Interface for a very minimal IP address tracker.
 */
public interface VisitorInfo {

	/**
	 * Defines the 3 possible ways implementations can be configured.
	 */
	public enum StorageSetup {
		/** Using an in-memory map to store visit info. */
		IN_MEMORY("in-memory map storage"),

		/**
		 * Uses Visits table in a NuoDB database created by NuoDBaaS (or running locally
		 * but configured using the same environment variables).
		 */
		NUO_DBAAS("NuoDBaaS defined storage"),

		/**
		 * Uses Visits table in a NuoDB database running locally and configured using
		 * Spring properties from {@code application.properties}.
		 */
		NUO_SPRING("NuoDB DataSource configured by Spring");

		private String details;

		private StorageSetup(String details) {
			this.details = details;
		}

		public String details() {
			return details;
		}
	}

	/**
	 * Fetch the number of times an HTTP request from this IP address has been seen.
	 * 
	 * @param ipAddress The source IP address of an incoming HTTP request.
	 * 
	 * @return The count if found, or zero if this IP address has not been seen
	 *         before.
	 */
	public int previousVisits(String ipAddress);

	/**
	 * Increment the visit count for the specified address.
	 * 
	 * @param ipAddress The source IP address of an incoming HTTP request.
	 */
	public void addVisit(String ipAddress);

	/**
	 * How was the internal storage of visit information configured?
	 * 
	 * @return Storage setup.
	 */
	public StorageSetup storageSetup();
}
