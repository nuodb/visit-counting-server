package com.nuodb.quick;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.context.annotation.Bean;

/**
 * Relatively minimal Spring Boot configured, single REST URL, web-app for
 * demonstrating deployment in containers (and optionally Kubernetes).
 * <p>
 * Each visit from a given IP address is recorded and counted. The response to
 * any HTTP request is to return the IP Address of this server (application) and
 * a message indicating how many times the source IP address (of the incoming
 * HTTP request) as been seen before.
 * <p>
 * The application can run using a in-memory transient storage (a Java Map,
 * equivalent to a Dictionary) or using persistent storage (a table in NuoDB).
 * Which storage is chosen depends on the Spring Profile specified - see
 * {@link InMemoryVisitorInfo} and {@link JdbcVisitorInfo},
 * 
 * @author Paul Chapman
 */
@SpringBootApplication
public class VisitorCountingServerApplication {

	/**
	 * Logs a message when the Spring Application Context is closed.
	 */
	public static class TerminateBean {

		/**
		 * Spring (like TomEE) calls PreDestroy methods on shutdown.
		 */
		@PreDestroy
		public void onDestroy() {
			LOGGER.warn("Spring Container is destroyed!");
		}
	}

	/** Class SLF4J logger. Spring Boot enables SLF4J over Logback by default. */
	private static Logger LOGGER = LoggerFactory.getLogger(VisitorCountingServerApplication.class);

	@SuppressWarnings("unused")
	private final VisitorCountingController visitorCountingController;

	/**
	 * Application entry-point - called {@code main} in Java by convention.
	 * <p>
	 * For the benefit of non-Java and/or non Spring developers ...
	 * <p>
	 * Your application is assumed to consist of a number of key application object
	 * instances (also known as components). Spring refers to them as "beans"
	 * because Java called them Java Beans way back when Java was a new language and
	 * the coffee analogy was "cool".
	 * <p>
	 * Spring's job is to create all these beans and also the relationships between
	 * them (that is some beans have dependencies on other beans). Spring creates
	 * them in the correct order to make this work. (If you have used TomEE, it can
	 * do the same setup, but is overkill for an application this basic).
	 * <p>
	 * This code invokes {@code SpringApplication.run} - one of the "magic" entry
	 * points that makes Spring Boot do its thing.
	 * <p>
	 * Spring Boot is an add-on to Spring to make Spring easier to use. Essentially
	 * it is a "bean" definition generator, in this case it will define the
	 * DataSource and the Spring Boot Actuator (which automatically sets up
	 * monitoring endpoints for a Web application).
	 * <p>
	 * Most importantly, Spring Boot initializes Spring itself. In particular it
	 * tells Spring to look for annotated classes in the current package and any
	 * sub-packages. Classes annotated with {@code @Configuration},
	 * {@code @Service}, {@code @Component} or {@code @Repository} are automatically
	 * marked by Spring as beans to be created. Spring also notes any methods in
	 * those classes annotated with {@code Bean} as ways to create more Spring
	 * Beans.
	 * <p>
	 * Spring then iterates over all the bean definitions it knows about and creates
	 * instances of those "beans". By default a single instance of each class is
	 * created. If the constructor of a class has arguments (dependencies), Spring
	 * performs <i>dependency injection</i> to pass in instances of the required
	 * type (which must be defined as Spring Beans in their own right). Spring
	 * stored all singelton the beans it creates in a container called the
	 * Application Context.
	 * <p>
	 * Classes annotated with {@code @Profile} are only instantiated if the
	 * specified profile is active. The default profile of this application is
	 * {@code in-memory} - visitor information will be stored in a simple in-memory
	 * map (dictionary) - see {@link InMemoryVisitorInfo} and
	 * {@code application.properties}.
	 * <p>
	 * An alternative can be specified on the command line using
	 * {@code -Dspring.profiles.active=xxx}. Specifying the profile {@code jdbc}
	 * causes the application to use NuoDB to store visit information - see
	 * {@link JdbcVisitorInfo}.
	 * 
	 * @param args Array of command line arguments. Never null, may be empty. One
	 *             argument may be specified - a port number to listen on for
	 *             incoming HTTP requests.
	 */
	public static void main(String[] args) {

		// If a PEM is specified in the environment (typically for a NuoDBaaS database)
		// specify the extra connection-properties required as a System property. Spring
		// will add them to the end of the database URL (see 'application.properties').
		String pem = System.getenv("NUODB_CA_PEM");

		if (pem != null) {
			System.setProperty("PEM_INFO",
					"&trustedCertificates=${NUODB_CA_PEM}&verifyHostname=false&allowSRPFallback=false");
		}

		// Default port (8080 is the default port the Tomcat servlet system uses).
		String port = "8080";

		// Handle command line argument, if specified
		switch (args.length) {
		case 0:
			break; // Nothing to do, defaults to 8080

		case 1:
			// Check for and save port number
			String arg0 = args[0];

			try {
				Integer.parseInt(arg0);
				LOGGER.info("Port " + arg0 + " requested");
				System.setProperty("server.port", arg0);
				port = arg0;
			} catch (NumberFormatException e) {
				LOGGER.error("Expecting a port number, but got '" + arg0 + '\'');
				LOGGER.error("Usage: java -jar hello-server.jar [<port-num>]");
				System.exit(-1);
			}
			break;

		default:
			LOGGER.error("{} command-line arguments invalid. One optional argument only (port number).", args.length);
			LOGGER.error("Usage: java -jar hello-server.jar [<port-num>]");
			System.exit(-1);
		}

		// Log system environment
		LOGGER.info("System Environment ...");
		Map<String, String> env = new TreeMap<>(System.getenv());

		for (Map.Entry<String, String> entry : env.entrySet())
			LOGGER.info("    " + entry.getKey() + "=" + entry.getValue());

		// Run Spring Boot which in turn invokes Spring. Remember:
		//
		// 1. Spring Boot is a "bean" definition generator - defining the most commonly
		// used beans with default configurations.
		//
		// 2. The bean definitions are then processed by Spring to create actual bean
		// instances, which in turn make your application work.
		LOGGER.info("Server starting, will listen on port " + port);

		File tempDir = new File("/tmp"); // *nix

		if (!tempDir.exists()) {
			tempDir = new File("/temp"); // Windows

			if (!tempDir.exists()) {
				tempDir = new File("."); // Windows
			}
		}

		// Use the builder to setup Spring Boot to run a web-application using this
		// class as the initial configuration class.
		SpringApplicationBuilder app = new SpringApplicationBuilder(VisitorCountingServerApplication.class)
				.web(WebApplicationType.SERVLET);

		// Save the current process id to a file before starting Spring Boot
		File pidFile = new File(tempDir, "visit-counting-server.pid");
		app.build().addListeners(new ApplicationPidFileWriter(pidFile));

		// Start Spring Boot, which in turn runs Spring by creating a Spring application
		// context. Among other things, this starts embedded Tomcat web server which
		// runs up multiple threads listening for HTTP requests on the specified port.
		// The application will keep running until all Tomcat's threads have terminated.
		try {
			app.run();
		} catch (Exception e) {
			if (e.getCause() != null)
				e = (Exception) e.getCause();

			LOGGER.error("Application terminating due to {}: {}", e.getClass().getSimpleName(),
					e.getLocalizedMessage());
			return;
		}

		// Log the process id if possible
		String pid;

		try {
			pid = new String(Files.readAllBytes(pidFile.toPath()));
			LOGGER.info("Application initialized. Process id = {}", pid);
		} catch (IOException e) {
			LOGGER.info("Application initialized.");
		}

		// Although main() ends here, the application will continue as long as Tomcat is
		// running (and its HTTP request listener threads are running).
	}

	/**
	 * Create an instance, saving the controller (which will handle HTTP requests).
	 * Spring automatically sets up the underlying Tomcat server to pass HTTP
	 * requests to classes annotated with either {@code Controller} and
	 * {@code RestController}. In this very simplified example all requests are
	 * handled by this one single controller.
	 * 
	 * @param visitorCountingController
	 */
	public VisitorCountingServerApplication(VisitorCountingController visitorCountingController) {
		this.visitorCountingController = visitorCountingController;
	}

	/**
	 * Allow logging on exit - see {@link TerminateBean} for details.
	 * 
	 * @return A singleton instance.
	 */
	@Bean
	public TerminateBean getTerminateBean() {
		return new TerminateBean();
	}
}
