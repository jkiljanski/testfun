package org.testfun.jee.runner.jaxrs;


import javax.ws.rs.ClientErrorException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.*;

import static org.fest.assertions.Assertions.assertThat;

public class RestRequest {

    private WebTarget webTarget;

    private MediaType contentType = MediaType.APPLICATION_XML_TYPE;
    private MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    private Object body;

    private Response.Status expectedStatus;

    private String expectedLocationUri;

    public RestRequest(String uri, int port) {
        UriBuilder path = UriBuilder.fromUri("http://localhost").port(port).path(uri);
        webTarget = ClientBuilder.newBuilder().build().target(path.build());
    }

    public RestRequest accept(MediaType acceptMediaType) {
        contentType = acceptMediaType;
        return this;
    }

    public RestRequest header(String key, Object value) {
        headers.add(key, value);
        return this;
    }

    public RestRequest body(Object body) {
        this.body = body;
        return this;
    }

    public RestRequest queryParam(String param, Object value) {
        webTarget = webTarget.queryParam(param, value);
        return this;
    }

    public RestRequest expectStatus(Response.Status expectedStatus) {
        this.expectedStatus = expectedStatus;
        return this;
    }

    public RestRequest expectLocation(String expectedLocationUri) {
        this.expectedLocationUri = expectedLocationUri;
        return this;
    }

    public String get() {
        return doHttpMethod("GET");
    }

    public String put() {
        return doHttpMethod("PUT");
    }

    public String post() {
        return doHttpMethod("POST");
    }

    public String delete() {
        return doHttpMethod("DELETE");
    }

    private String doHttpMethod(String method) {
        Response response;
        try {
            if (body != null) {
                response = webTarget.request().headers(headers).build(method, Entity.entity(body, contentType)).invoke();
            } else {
                response = webTarget.request().headers(headers).build(method).invoke();
            }
        } catch (Exception e) {
            throw new JaxRsException(method + " failed", e);
        }

        assertExpectedStatus(response);
        assertLocation(response);
        return toString(response);
    }

    @SuppressWarnings("unchecked")
    private String toString(Response response) {
        Response.Status responseStatus = Response.Status.fromStatusCode(response.getStatus());

        if (responseStatus == Response.Status.NO_CONTENT) {
            return null;
        }
        if (responseStatus.getFamily() != Response.Status.Family.SUCCESSFUL && responseStatus != expectedStatus) {
            throw new ClientErrorException(response);
        } else {
            return response.readEntity(String.class);
        }
    }

    private void assertExpectedStatus(Response response) {
        if (expectedStatus != null) {
            Response.Status responseStatus = Response.Status.fromStatusCode(response.getStatus());

            assertThat(responseStatus)
                    .as("Expected response with status " + expectedStatus.getStatusCode() + " (" + expectedStatus + ") but got response with status " + response.getStatus() + " (" + responseStatus + ")")
                    .isEqualTo(expectedStatus);
        }
    }

    private void assertLocation(Response response) {
        if (expectedLocationUri != null) {
            String actualLocation = response.getLocation().toString();
            assertThat(actualLocation).
                    as("Expected location '" + expectedLocationUri + "' but got " + actualLocation).
                    contains(expectedLocationUri);
        }
    }

}
