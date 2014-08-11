import java.io.File;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import com.$314e.bhrestapi.BHRestApi;
import com.$314e.bhrestapi.BHRestUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;

/**
 * 
 * @author declan.ayres
 *
 */
public class EmailDate {

	private static final String APP_NAME = "Gmail API Quickstart";
	private static final File DATA_STORE_DIR = new File(
			System.getProperty("user.home"), ".store/gmail");
	private static final String USER = "me";
	private static final JsonFactory JSON_FACTORY = JacksonFactory
			.getDefaultInstance();
	private static FileDataStoreFactory dataStoreFactory;
	private static HttpTransport httpTransport;
	private static Gmail gmail;

	public static void main(String[] args) throws Exception {

		// Authorization of bullhorn Api
		final String CLIENT_ID = "71eb9c1a-27e1-4bb6-8c60-8f835cc51651";
		final String CLIENT_SECRET = "lU5yFm9ypiLPctFGzidBaXYV7c53Drie";

		Properties properties = new Properties();
		ObjectNode token;
		String restToken;
		BHRestApi.Entity entityApi;

		BHRestApi.Auth entityApi2 = BHRestUtil.getAuthApi();
		properties.load(BHRestApi.class
				.getResourceAsStream("/local.properties"));
		token = BHRestUtil.getRestToken(
				properties.getProperty("CLIENT_ID", CLIENT_ID),
				properties.getProperty("CLIENT_SECRET", CLIENT_SECRET),
				properties.getProperty("BH_USER"),
				properties.getProperty("BH_PASSWORD"));
		restToken = token.get("BhRestToken").asText();
		entityApi = BHRestUtil.getEntityApi(token);

		// Get all the candidates by search
		int start = 0;
		ObjectNode candidates = entityApi.search(
				BHRestApi.Entity.ENTITY_TYPE.CANDIDATE, restToken,
				"isDeleted:0 AND NOT status:archive",
				"email, email2, email3, customDate1, id", "+id", 500, start);

		System.out.println(candidates);

		ObjectNode updates;

		ObjectNode toUpdate;

		// Authorization of Gmail API
		httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);

		final GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
				JSON_FACTORY, new FileReader(
						"/Users/Declan.Ayres/Downloads/client_secret.json"));

		if (clientSecrets.getDetails().getClientId().startsWith("Enter")
				|| clientSecrets.getDetails().getClientSecret()
						.startsWith("Enter ")) {
			System.out
					.println("Enter Client ID and Secret from https://code.google.com/apis/console/?api=plus "
							+ "into plus-cmdline-sample/src/main/resources/client_secrets.json");
			System.exit(1);
		}

		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				httpTransport,
				JSON_FACTORY,
				"9523858024-1s828b0rlffn957r80ch50vivsequ7cl.apps.googleusercontent.com",
				"2T56GjlkKVpKU5rKSJi0ZWSj", Collections
						.singleton(GmailScopes.GMAIL_MODIFY))
				.setDataStoreFactory(dataStoreFactory).build();

		Credential auth = new AuthorizationCodeInstalledApp(flow,
				new LocalServerReceiver()).authorize("user");

		System.out.println(auth.getExpiresInSeconds());

		final Credential credential = auth;

		gmail = new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
				.setApplicationName(APP_NAME).build();
		
		//date to be updated
		DateFormat dateFormat = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss ZZZZ");
		Date parsedDate;
		
		
		//searches emails with query date from one month ago
		Date queryDate = new Date();
		queryDate.setMonth(queryDate.getMonth()-1);
		DateFormat queryFormat = new SimpleDateFormat("yyyy/MM/dd");
		String queryDateString = queryFormat.format(queryDate);
		
		
		long time;

		int i = 1;
		final String useremail = "declan.ayres@314ecorp.com";
		ListMessagesResponse list = gmail.users().messages()
				.list("bullhorn@314ecorp.com")
				.setQ("in:inbox from:" + "(+" + useremail + ") after:" + queryDateString)
				.execute();
		String query = "in:inbox from:" + "(+" + useremail + ") after:" + queryDateString;

		int count = 0;

		// Run through each candidate
		for (int index = 0; index < candidates.path("total").intValue(); index++) {
			System.out.println(index);

			if (index % 500 == 0 && index != 0) {
				start += 500;
				count = 0;
				candidates = entityApi.search(
						BHRestApi.Entity.ENTITY_TYPE.CANDIDATE, restToken,
						"isDeleted:0 AND NOT status:archive",
						"email, email2, email3, customDate1, id", "+id", 500,
						start);
			}
			final String email = candidates.path("data").get(count)
					.path("email").asText();
			final String email2 = candidates.path("data").get(count)
					.path("email2").asText();
			final String email3 = candidates.path("data").get(count)
					.path("email3").asText();

			// find out which emails the candidate has
			if (email != "" && email2 == ""
					&& (email3 == "" || email3.equals("Not Applicable"))) {
				query = "in:inbox from:" + "(+" + email + ") after:" + queryDateString;
				list = gmail
						.users()
						.messages()
						.list("bullhorn@314ecorp.com")
						.setQ("in:inbox from:" + "(+" + email
								+ ") after:" + queryDateString).execute();
			} else if (email != "" && email2 != ""
					&& (email3 == "" || email3.equals("Not Applicable"))) {
				query = "in:inbox (from:" + "(+" + email + ") OR from:" + "(+"
						+ email2 + ")) after:" + queryDateString;
				list = gmail
						.users()
						.messages()
						.list("bullhorn@314ecorp.com")
						.setQ("in:inbox (from:" + "(+" + email + ") OR from:"
								+ "(+" + email2 + ")) after:" + queryDateString)
						.execute();
			} else if (email != "" && email2 == "" && email3 != "") {
				query = "in:inbox (from:" + "(+" + email + ") OR from:" + "(+"
						+ email3 + ")) after:" + queryDateString;
				list = gmail
						.users()
						.messages()
						.list("bullhorn@314ecorp.com")
						.setQ("in:inbox (from:" + "(+" + email + ") OR from:"
								+ "(+" + email3 + ")) after:" + queryDateString)
						.execute();
			} else if (email != "" && email2 != "" && email3 != "") {
				query = "in:inbox (from:" + "(+" + email + ") OR from:" + "(+"
						+ email2 + ") OR from:" + "(+" + email3
						+ ")) after:" + queryDateString;
				list = gmail
						.users()
						.messages()
						.list("bullhorn@314ecorp.com")
						.setQ("in:inbox (from:" + "(+" + email + ") OR from:"
								+ "(+" + email2 + ") OR from:" + "(+" + email3
								+ ")) after:" + queryDateString).execute();
			} else if (email == "" && email2 == ""
					&& (email3 == "" || email3.equals("Not Applicable"))) {
				System.out.println(candidates.path("data").get(count)
						.path("id"));
				System.out.println(" ");
				count++;

				continue;
			} else {
				query = "in:inbox (from:" + "(+" + email + ") OR from:" + "(+"
						+ email2 + ") OR from:" + "(+" + email3
						+ ")) after:" + queryDateString;
				list = gmail
						.users()
						.messages()
						.list("bullhorn@314ecorp.com")
						.setQ("in:inbox (from:" + "(+" + email + ") OR from:"
								+ "(+" + email2 + ") OR from:" + "(+" + email3
								+ ")) after:" + queryDateString).execute();
			}

			// Copy the list contents into a for-loop-readable arraylist

			List<Message> messages = new ArrayList<Message>();
			while (list.getMessages() != null) {
				messages.addAll(list.getMessages());
				if (list.getNextPageToken() != null) {
					String pageToken = list.getNextPageToken();
					list = gmail.users().messages()
							.list("bullhorn@314ecorp.com").setQ(query)
							.setPageToken(pageToken).execute();
				} else {
					break;
				}
			}

			// Check to see whether candidate has any messages

			if (list != null) {

				System.out.println(messages);

				// Go through the messages of the candidate

				for (final Message msgId : messages) {
					System.out.println(candidates.path("data").get(count)
							.path("id"));
					System.out.println("############################    " + i++
							+ "    #############################");

					final Message message = gmail.users().messages()
							.get("bullhorn@314ecorp.com", msgId.getId())
							.setFormat("full").execute();

					final MessagePartHeader to = message
							.getPayload()
							.getHeaders()
							.stream()
							.filter(h -> (h.getName().equals("To") && h
									.getValue().toLowerCase()
									.contains("bullhorn@314ecorp.com"))
									|| (h.getName().equalsIgnoreCase("Cc") && h
											.getValue().toLowerCase()
											.contains("bullhorn@314ecorp.com")))
							.findFirst().orElse(null);
					final MessagePartHeader from = message
							.getPayload()
							.getHeaders()
							.stream()
							.filter(h -> h.getName().equals("From")
									&& h.getValue().toLowerCase()
											.contains(email)).findFirst()
							.orElse(null);
					final MessagePartHeader from2 = message
							.getPayload()
							.getHeaders()
							.stream()
							.filter(h -> h.getName().equals("From")
									&& h.getValue().toLowerCase()
											.contains(email2)).findFirst()
							.orElse(null);
					final MessagePartHeader from3 = message
							.getPayload()
							.getHeaders()
							.stream()
							.filter(h -> h.getName().equals("From")
									&& h.getValue().toLowerCase()
											.contains(email3)).findFirst()
							.orElse(null);

					final MessagePartHeader date = message.getPayload()
							.getHeaders().stream()
							.filter(h -> h.getName().equals("Date"))
							.findFirst().orElse(null);

					// Convert the date to format the candidate's date

					if (date.getValue().charAt(0) != 'S'
							&& date.getValue().charAt(0) != 'M'
							&& date.getValue().charAt(0) != 'T'
							&& date.getValue().charAt(0) != 'W'
							&& date.getValue().charAt(0) != 'F') {
						dateFormat = new SimpleDateFormat(
								"dd MMM yyyy HH:mm:ss ZZZZ");
					} else if (date.getValue().charAt(8) == '0'
							|| date.getValue().charAt(8) == '1') {

						dateFormat = new SimpleDateFormat(
								"EEE, dd MM yyyy HH:mm:ss");

					} else {
						dateFormat = new SimpleDateFormat(
								"EEE, dd MMM yyyy HH:mm:ss ZZZZ");
					}

					parsedDate = dateFormat.parse(date.getValue());
					System.out.println(parsedDate.getTime());
					System.out.println(parsedDate);

					time = parsedDate.getTime();
					// Adjust the date to match local timezone
					switch (date.getValue().charAt(
							(int) date.getValue().length() - 3)) {
					case '0':
						time = (parsedDate.getTime() - 25200000);
						break;
					case '1':
						time = (parsedDate.getTime() - 21600000);
						break;
					case '2':
						time = (parsedDate.getTime() - 18000000);
						break;
					case '3':
						time = (parsedDate.getTime() - 14400000);
						break;
					case '4':
						time = (parsedDate.getTime() - 10800000);
						break;
					case '5':
						time = (parsedDate.getTime() - 7200000);
						break;
					case '6':
						time = (parsedDate.getTime() - 3600000);
						break;
					case '7':
						time = parsedDate.getTime();
						break;
					default:
						time = parsedDate.getTime();
						break;

					}
					// Update the candidate's most recent inbound email with the
					// date, represented as epoch milliseconds
					toUpdate = entityApi.get(
							BHRestApi.Entity.ENTITY_TYPE.CANDIDATE, restToken,
							candidates.path("data").get(count).path("id")
									.intValue(), "customDate1");

					updates = entityApi.update(
							BHRestApi.Entity.ENTITY_TYPE.CANDIDATE, restToken,
							candidates.path("data").get(count).path("id")
									.intValue(), ((ObjectNode) toUpdate
									.path("data")).put("customDate1", time));

					if (from != null) {
						System.out.println(from.toPrettyString());
					}

					if (from2 != null) {
						System.out.println(from2.toPrettyString());
					}

					if (from3 != null) {
						System.out.println(from3.toPrettyString());
					}

					if (to != null) {
						System.out.println(to.toPrettyString());
					}

					System.out.println(date.toPrettyString());
					break;
				}
			}
			System.out.println(candidates.path("data").get(count).path("id"));
			System.out.println(" ");
			count++;
		}

		/*
		 * for (final Message msgId : gmail.users().messages().list(USER)
		 * .setQ("in:inbox (from:" + useremail + " OR to:" + useremail +
		 * ")  after:2014/7/9") .execute().getMessages()) {
		 * System.out.println("############################    " + i++ +
		 * "    #############################");
		 * 
		 * final Message message = gmail.users().messages().get(USER,
		 * msgId.getId()).setFormat("full").execute();
		 * 
		 * final MessagePartHeader to =
		 * message.getPayload().getHeaders().stream().filter(h ->
		 * (h.getName().equals ("To") &&
		 * h.getValue().toLowerCase().contains(useremail)) ||
		 * (h.getName().equalsIgnoreCase("Cc") &&
		 * h.getValue().toLowerCase().contains(useremail)))
		 * .findFirst().orElse(null); final MessagePartHeader from =
		 * message.getPayload().getHeaders().stream().filter(h ->
		 * h.getName().equals ("From") &&
		 * h.getValue().toLowerCase().contains("mimi.curiel@314ecorp.com"
		 * )).findFirst().orElse(null); final MessagePartHeader date =
		 * message.getPayload().getHeaders().stream().filter(h ->
		 * h.getName().equals ("Date")).findFirst().orElse(null);
		 * 
		 * if (to != null && from !=null){
		 * System.out.println(to.toPrettyString());
		 * System.out.println(from.toPrettyString()); }
		 * 
		 * 
		 * System.out.println(date.toPrettyString()); }
		 */

	}

}
