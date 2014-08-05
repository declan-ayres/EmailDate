import java.util.Properties;

import org.joda.time.DateTime;

import com.$314e.bhrestapi.BHRestApi;
import com.$314e.bhrestapi.BHRestUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ActiveCandidates {

	private static Properties properties = new Properties();

	private static ObjectNode token;
	private static String restToken;
	private static BHRestApi.Entity entityApi;

	public static void main(String[] args) throws Exception {

		properties.load(MassUpdates.class
				.getResourceAsStream("/local.properties"));
		token = BHRestUtil.getRestToken("71eb9c1a-27e1-4bb6-8c60-8f835cc51651",
				"lU5yFm9ypiLPctFGzidBaXYV7c53Drie",
				properties.getProperty("BH_USER"),
				properties.getProperty("BH_PASSWORD"));

		restToken = token.get("BhRestToken").asText();
		entityApi = BHRestUtil.getEntityApi(token);

		int staying = 0;
		int switching = 0;

		DateTime date = new DateTime();
		long cutoff = date.minusDays(90).getMillis();

		int start = 0;
		ObjectNode candidates = entityApi.search(
				BHRestApi.Entity.ENTITY_TYPE.CANDIDATE, restToken,
				"isDeleted:0 AND status:Currently Looking ",
				"id, dateAdded, customDate1, customDate2, customDate3, status",
				"+id", 500, start);

		System.out.println(candidates);

		int count = 0;

		for (int i = 0; i < candidates.path("total").asInt(); i++) {

			if (i % 500 == 0 && i != 0) {
				start += 500;
				count = 0;
				candidates = entityApi
						.search(BHRestApi.Entity.ENTITY_TYPE.CANDIDATE,
								restToken,
								" isDeleted:0 AND status:Currently Looking",
								"id, dateAdded, customDate1, customDate2, customDate3, status",
								"+id", 500, start);
			}

			if (candidates.path("data").get(count).path("dateAdded").asLong() > cutoff
					|| candidates.path("data").get(count).path("customDate1")
							.asLong() > cutoff
					|| candidates.path("data").get(count).path("customDate2")
							.asLong() > cutoff
					|| candidates.path("data").get(count).path("customDate3")
							.asLong() > cutoff) {
				staying++;

			} else {

				switching++;
			}
			System.out.println(i);
			count++;
		}
		System.out.println("Statuses switching: " + switching);
		System.out.println("Statuses staying: " + staying);
	}

}
