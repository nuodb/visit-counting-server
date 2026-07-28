package com.nuodb.quick;

import java.net.InetAddress;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A trivial HTTP REST controller that returns the same response for any
 * request:
 * <p>
 * "{@code Hello from <ip-addr> - <visit-count-info> (<configuration-info>)}".
 */
@RestController
public class VisitorCountingController {

	/**
	 * The Spring application context.
	 * <p>
	 * This holds all the Singleton beans that were defined by Spring Boot, or found
	 * in {@link Configuration} classes, or by checking the current directory for
	 * classes annotated with {@link Service}, {@link Component} or
	 * {@link Repository}. Spring creates instances of them all and stores them in
	 * its context.
	 * <p>
	 * Closing the context (see {@link #shutdown()}) shuts down Spring and
	 * terminates the application.
	 */
	@Autowired
	private ApplicationContext context;

	private Logger logger = LoggerFactory.getLogger(getClass());
	private VisitorInfo visitorInfo;
	private String myAddress = "Unknown";

	public VisitorCountingController(VisitorInfo visitorInfo) {
		this.visitorInfo = visitorInfo;

		// Get IP address to show which container is handling request
		try {
			InetAddress inetAddress = InetAddress.getLocalHost();
			logger.info("Running on IP: " + inetAddress.getHostAddress());

			myAddress = "IP " + inetAddress.getHostAddress();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Handle any HTTP request, GET, POST or otherwise.
	 * 
	 * @return Always returns "{@code Hello from <ip-addr> - <visit-count-info>}".
	 */
	@RequestMapping("/")
	public String hello(HttpServletRequest httpRequest) {
		String source = httpRequest.getRemoteAddr();
		logger.info("Processing a request from {}", source);

		int prevVisits = visitorInfo.previousVisits(source);
		visitorInfo.addVisit(source);
		String visitDetails = "";

		switch (prevVisits) {
		case 0:
			visitDetails = "this is your first visit";
			break;

		case 1:
			visitDetails = "you have been here once before";
			break;

		case 2:
			visitDetails = "you have been here twice before";
			break;

		default:
			visitDetails = "you have been here " + prevVisits + " times before";
			break;
		}

		return "Hello from " + myAddress + " - " + visitDetails + " (using " + visitorInfo.storageSetup().details()
				+ ')';
	}

	/**
	 * Handle a request to /kill. Closes the application context which in turn shuts
	 * down Spring and any threads it has created (in this case Tomcat's HTTP
	 * request listener threads) thus terminating the application.
	 * 
	 * @return Always returns "{@code Hello from <ip-addr> - <visit-count-info>}".
	 */
	@RequestMapping("/kill")
	public String shutdown() {
		logger.warn("Application shutdown requested, terminating");
		Date timeNow = new Date();

		// Close the context in a separate thread so this method can return a response
		// before the application shuts down.
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				((ConfigurableApplicationContext) context).close();
			}
		});

		t.start();
		return "Application terminating (" + timeNow + ')';
	}

//	@RequestMapping("/error")
//	public String error() {
//		return "Stuffed up!";
//	}

}
