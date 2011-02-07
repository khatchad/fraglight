/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglightevaluator.util;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.*;

/**
 * @author raffi
 * 
 */
public class PostMan {

	private static final String SMTP_HOST_NAME = "smtp.cse.ohio-state.edu";

	private static boolean debug = false;

	public static void postMail(String subject, String message, String from,
			String... recipients) {

		try {
			//Set the host smtp address
			Properties props = new Properties();
			props.put("mail.smtp.host", SMTP_HOST_NAME);

			// create some properties and get the default Session
			Session session = Session.getDefaultInstance(props, null);
			session.setDebug(debug);

			// create a message
			Message msg = new MimeMessage(session);

			// set the from and to address
			InternetAddress addressFrom = new InternetAddress(from);
			msg.setFrom(addressFrom);

			InternetAddress[] addressTo = new InternetAddress[recipients.length];
			for (int i = 0; i < recipients.length; i++) {
				addressTo[i] = new InternetAddress(recipients[i]);
			}
			msg.setRecipients(Message.RecipientType.TO, addressTo);

			// Optional : You can also set your custom headers in the Email if you Want
			msg.addHeader("MyHeaderName", "myHeaderValue");

			// Setting the Subject and Content Type
			msg.setSubject(subject);
			msg.setContent(message, "text/plain");
			Transport.send(msg);
		}
		catch (MessagingException e) {
			System.err.println("Can't send mail.");
		}
	}
}