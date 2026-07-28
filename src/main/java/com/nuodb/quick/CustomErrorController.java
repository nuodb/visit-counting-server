package com.nuodb.quick;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SPring Boot's default error handler generates a deliberately ugly, minimal,
 * so-called white-label error page. The intent is to force you to implement
 * your own with the right look and feel. This is not much better, but is an
 * example of how it is done.
 */
@RestController
public class CustomErrorController implements ErrorController {

	private static final String PATH = "/error";

	@RequestMapping(value = PATH)
	public String error(HttpServletRequest request) {
		Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		String uri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

		if (status != null) {
			Integer statusCode = Integer.valueOf(status.toString());

			if (statusCode == HttpStatus.NOT_FOUND.value()) {
				return "HTTP ERROR 404 - Page not found";
			}

			if (statusCode == HttpStatus.METHOD_NOT_ALLOWED.value()) {
				if ("/actuator/shutdown".equals(uri))
					return "HTTP ERROR 405 - Actuator shutdown option not enabled in this application";
				else
					return "HTTP ERROR 405 - Access violation: " + uri;
			}

			if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
				return "HTTP ERROR 500 - Internal error in the server";
			}

			HttpStatus httpStatus = HttpStatus.resolve(statusCode);

			if (httpStatus != null)
				return "HTTP ERROR " + statusCode + httpStatus.getReasonPhrase();
			else
				return "HTTP ERROR " + statusCode;
		}

		return "UNKNOWN HTTP ERROR"; // Should never happen
	}

}