package controllers;

import com.dietsodasoftware.yail.oauth2.client.InfusionsoftOauthToken;
import com.dietsodasoftware.yail.xmlrpc.client.YailClient;
import com.dietsodasoftware.yail.xmlrpc.client.YailProfile;
import com.dietsodasoftware.yail.xmlrpc.model.Contact;
import com.dietsodasoftware.yail.xmlrpc.model.Tag;
import com.dietsodasoftware.yail.xmlrpc.model.TagAssignment;
import com.dietsodasoftware.yail.xmlrpc.service.data.DataServiceCountOperation;
import com.dietsodasoftware.yail.xmlrpc.service.data.DataServiceQueryOperation;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ning.http.util.Base64;
import play.*;
import play.cache.Cache;
import play.libs.F;
import play.libs.Json;
import play.libs.WS;
import play.mvc.*;

import java.util.concurrent.Callable;

public class Application extends Controller {

    /* you'll want to move these into a properties file, ala build.properties */
    public static String CLIENT_ID = "va4mbf37dzknudx98hekekzf";
    public static String SIGN_IN_URI = "https://signin.infusionsoft.com/app/oauth/authorize?";
    public static String REDIRECT_URI = "https://mattlmbp:9443/authorize";
    public static String RESPONSE_TYPE = "code";
    public static String REQUEST_SCOPE = "full";

    public static String TOKEN_URI = "https://api.infusionsoft.com/token";
    public static int CACHE_DURATION_IN_SECONDS = 60;

    /**
     * Returns the Infusionsoft OAuth sign-in URL
     * @return
     */
    public static Result getSignInUrl() {
        /* build something that looks like this:
         *
         *  https://signin.infusionsoft.com/app/oauth/authorize?client_id=va4mbf37dzknudx98hekekzf
         *      &redirect_uri=https://mattlmbp:9443/authorize
         *      &response_type=code&scope=read+write+execute
         */

        final StringBuilder url = new StringBuilder();
        url.append(SIGN_IN_URI);
        url.append("client_id=");
        url.append(CLIENT_ID);
        url.append("&redirect_uri=");
        url.append(REDIRECT_URI);
        url.append("&response_type=");
        url.append(RESPONSE_TYPE);
        url.append("&scope=");
        url.append(REQUEST_SCOPE);

        return ok(url.toString());
    }

    /**
     * Handles incoming redirect from Infusionsoft OAuth
     *
     * @param scope
     * @param code
     * @return
     */
    public static Result authorize(String scope, String code) {
        /* redirect back to the client so that he can do what he needs to do w/ the token */
        String url = "https://mattlmbp:9443/#/oauth_orize?";
        url += "scope=" + scope;
        url += "&code=" + code;

        return redirect(url);
    }

    /**
     * I promise - I'll give you a token
     *
     * @param code
     * @return
     */
    public static F.Promise<Result> token(String code) {
        /* encode our Base Authentication - for the Authentication header */
        final String baseAuth = "Basic " + Base64.encode("va4mbf37dzknudx98hekekzf:MEHFw3BusK".getBytes());

        /* what are we posting?  This: */
        final StringBuilder content = new StringBuilder();
        content.append("grant_type=authorization_code");
        content.append("&client_id=va4mbf37dzknudx98hekekzf");
        content.append("&redirect_uri=https://mattlmbp:9443/authorize");
        content.append("&code=" + code);

        /* post the content and get the token from the response */
        final F.Promise<Result> authorization = WS.url(TOKEN_URI)
                .setHeader("Authorization", baseAuth)
                .setContentType("application/x-www-form-urlencoded; charset=utf-8")
                .post(content.toString())
                .map(new F.Function<WS.Response, Result>() {

                    /* called when the WS call returns w/ our token */
                    public Result apply(WS.Response response) {
                        final String token = response.asJson().findValue("access_token").asText();
                        final ObjectNode authTokenJson = Json.newObject();
                        authTokenJson.put("icon2014Token", token);

                        return ok(authTokenJson);
                    }
                }
        );

        return authorization;
    }

    /**
     * Retrieve all available tags - results are cached for a duration of 60 seconds
     *
     * @param oAuthToken
     * @return
     */
    public static Result getTags(final String oAuthToken) {
        /* get a token and grab a YAIL Client */
        final InfusionsoftOauthToken token = getOAuthToken(oAuthToken);
        final YailProfile profile = YailProfile.usingOAuth2Token(token);
        final YailClient client = profile.getClient();
        ObjectNode tags = null;

        try {
            /* if they're in the cache, pull them from the cache.  Otherwise, go fetch em' */
            tags = Cache.getOrElse("tags", new Callable<ObjectNode>() {
                @Override
                public ObjectNode call() throws Exception {
                    return queryTag(client);
                }
            }, CACHE_DURATION_IN_SECONDS);
        }
        catch(Exception ex) {
            Logger.error("Unable to obtain tags in Application.getTags from cache");
            Logger.error(ex.getMessage());
        }

        return ok(tags);
    }

    /**
     * Private helper used to query for tags.  Returns a JSON Object Node
     *
     * @param client
     * @return
     */
    private static ObjectNode queryTag(YailClient client) {
        final ObjectNode results = Json.newObject();
        final ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);

        try {
            final DataServiceQueryOperation<Tag> query = new DataServiceQueryOperation<Tag>(Tag.class)
                    .fieldLike(Contact.Field.Id, "", DataServiceQueryOperation.Like.before)
                    .setPage(0)
                    .setLimit(10)
                    .orderBy(Tag.Field.GroupName)
                    .ascending();

            /* build some JSON that's easy to work with */
            for(Tag tag : client.call(query, 10)) {
                final ObjectNode result = Json.newObject();
                result.put("id", tag.getFieldValue(Tag.Field.Id).toString());
                result.put("name", tag.getFieldValue(Tag.Field.GroupName).toString());
                arrayNode.add(result);
            }
        }
        catch(Exception ex) {
            Logger.error("Unable to obtain contacts in Application.queryContact");
            Logger.error(ex.getMessage());
        }

        results.put("tags", arrayNode);
        return results;
    }

    /**
     * Retrieve all contacts that have the specified tag - results are cached for a duration of 60 seconds
     *
     * @param oAuthToken
     * @return
     */
    public static Result getContactsForTag(final String oAuthToken, final String tagId, final Integer page) {
        /* get a token and grab a YAIL Client */
        final InfusionsoftOauthToken token = getOAuthToken(oAuthToken);
        final YailProfile profile = YailProfile.usingOAuth2Token(token);
        final YailClient client = profile.getClient();
        ObjectNode contacts = null;

        try {
            /* if they're in the cache, pull them from the cache.  Otherwise, go fetch em' */
            contacts = Cache.getOrElse("contacts-tag:" + tagId + "-page:" + page, new Callable<ObjectNode>() {
                @Override
                public ObjectNode call() throws Exception {
                    return queryContactsForTag(client, tagId, page);
                }
            }, CACHE_DURATION_IN_SECONDS);
        }
        catch(Exception ex) {
            Logger.error("Unable to obtain contacts in Application.getContactsForTag from cache");
            Logger.error(ex.getMessage());
        }

        return ok(contacts);
    }

    /**
     * Private helper used to query for contacts.  Returns a JSON Object Node
     *
     * @param client
     * @return
     */
    private static ObjectNode queryContactsForTag(YailClient client, String tagId, Integer page) {
        final ObjectNode results = Json.newObject();
        final ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);

        /* set a wildcard if no value is provided */
        if(tagId == null || "null".equals(tagId) || tagId.isEmpty()) {
            tagId = "%";
        }

        try {
            /* Presently, YAIL exposes joined contact fields only via an instantiated Object */
            final TagAssignment t = TagAssignment.builder().build();
            final DataServiceQueryOperation<TagAssignment> query = new DataServiceQueryOperation<TagAssignment>(TagAssignment.class)
                    .fieldEquals(TagAssignment.Field.GroupId, tagId)
                    .setPage(page)
                    .setLimit(10)
                    .orderBy(t.joinedContactField(Contact.Field.FirstName))
                    .ascending();

            /* build some JSON that's easy to work with */
            for(TagAssignment tagAssignment : client.call(query, 10)) { /* timeout after 10 seconds */
                ObjectNode result = Json.newObject();
                result.put("firstName", tagAssignment.getFieldValue(tagAssignment.joinedContactField(Contact.Field.FirstName)).toString());
                result.put("lastName", tagAssignment.getFieldValue(tagAssignment.joinedContactField(Contact.Field.LastName)).toString());
                result.put("email", tagAssignment.getFieldValue(tagAssignment.joinedContactField(Contact.Field.Email)).toString());
                result.put("tagName", tagAssignment.getFieldValue(TagAssignment.Field.ContactGroup).toString());

                arrayNode.add(result);
            }
        }
        catch(Exception ex) {
            Logger.error("Unable to obtain contacts in Application.queryContact");
            Logger.error(ex.getMessage());
        }

        results.put("contacts", arrayNode);
        return results;
    }

    /**
     * Retrieve all contacts that have the specified tag - results are cached for a duration of 60 seconds
     *
     * @param oAuthToken
     * @return
     */
    public static Result getCountOfContactsForTag(final String oAuthToken, final String tagId) {
        /* get a token and grab a YAIL Client */
        final InfusionsoftOauthToken token = getOAuthToken(oAuthToken);
        final YailProfile profile = YailProfile.usingOAuth2Token(token);
        final YailClient client = profile.getClient();
        ObjectNode contacts = null;

        try {
            /* if they're in the cache, pull them from the cache.  Otherwise, go fetch em' */
            contacts = Cache.getOrElse("contacts-count:" + tagId, new Callable<ObjectNode>() {
                @Override
                public ObjectNode call() throws Exception {
                    return queryCountOfContactsForTag(client, tagId);
                }
            }, CACHE_DURATION_IN_SECONDS);
        }
        catch(Exception ex) {
            Logger.error("Unable to obtain contacts in Application.getContactsForTag from cache");
            Logger.error(ex.getMessage());
        }

        return ok(contacts);
    }

    public static ObjectNode queryCountOfContactsForTag(YailClient client, String tagId) {
        ObjectNode result = Json.newObject();

        /* set a wildcard if no value is provided */
        if(tagId == null || "null".equals(tagId) || tagId.isEmpty()) {
            tagId = "%";
        }

        try {
            final DataServiceCountOperation countQuery = new DataServiceCountOperation(TagAssignment.class).fieldEquals(TagAssignment.Field.GroupId, tagId);
            final Integer count = client.call(countQuery, 10);

            result.put("count", count);
        }
        catch(Exception ex) {
            Logger.error("Unable to obtain count of contacts in Application.queryCountOfContactsForTag");
            Logger.error(ex.getMessage());
        }

        return result;
    }

    /**
     * Creates a YAIL InfusionsofOauthToken out of the provided String
     *
     * @param tokenStr
     * @return
     */
    private static InfusionsoftOauthToken getOAuthToken(final String tokenStr) {
        final InfusionsoftOauthToken token = new InfusionsoftOauthToken() {
            @Override
            public String getToken() {
                return tokenStr;
            }
        };

        return token;
    }
}
