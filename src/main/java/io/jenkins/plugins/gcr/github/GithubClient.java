package io.jenkins.plugins.gcr.github;

import io.jenkins.plugins.gcr.PluginConfiguration;
import io.jenkins.plugins.gcr.models.PluginEnvironment;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class GithubClient {

    private PluginEnvironment environment;

    private String accessToken;

    private String githubUrl;

    private HttpClient httpClient;

    public GithubClient(PluginEnvironment environment, String githubUrl, String accessToken) {
        this(environment, githubUrl, accessToken, HttpClientBuilder.create().build());
    }

    public GithubClient(PluginEnvironment environment, String githubUrl, String accessToken, HttpClient httpClient) {
        this.httpClient = httpClient;
        this.environment = environment;
        this.accessToken = accessToken;
        this.githubUrl = githubUrl;
    }


    public void sendCommitStatus(GithubPayload githubPayload) throws GithubClientException {

        String path = String.format("/repos/%s/statuses/%s", environment.getPullRequestRepository(), environment.getGitHash());


        URI uri = buildUri(path);
        HttpPost postRequest = new HttpPost(uri);

        try {
            StringEntity entity = new StringEntity(githubPayload.toJSONString());
            postRequest.setEntity(entity);
        } catch (UnsupportedEncodingException ex) {
            throw new GithubClientException("Issue with encoding of github payload", ex);
        }

        ResponseHandler<GithubResponse> responseHandler = (HttpResponse response) -> {
            InputStream stream = response.getEntity().getContent();
            String string = IOUtils.toString(stream);
            System.out.println(string);

            int status = response.getStatusLine().getStatusCode();

            if(status == HttpStatus.SC_CREATED) {
                return new GithubResponse(true, "");
            }

            String body = IOUtils.toString(response.getEntity().getContent());
            String message = String.format("[%d] %s", status, body);
            return new GithubResponse(false, message);
        };

        try {
            GithubResponse result = this.httpClient.execute(postRequest, responseHandler);
            if (!result.isSuccess()) {
                String message = String.format("Bad HTTP result for url %s. Error message:  ", postRequest.getURI().toString());
                throw new GithubClientException(message);
            }
        } catch (IOException ex) {
            throw new GithubClientException("IOException during request");
        }
    }

    private URI buildUri(String path) throws GithubClientException {
        URIBuilder builder = new URIBuilder();
        builder.setScheme("https");

        if (this.isCustomUrlValid()) {
            builder.setHost(this.cleanedHost());
        } else {
            builder.setHost("api.github.com");
        }

        if (this.isCustomUrlValid()) {
            path = "/api/v3".concat(path);
        }

        builder.setPath(path);
        builder.setParameter("access_token", accessToken);

        try {
            return builder.build();
        } catch (URISyntaxException ex) {
            throw new GithubClientException("URI builder syntax was malformed", ex);
        }
    }

    private boolean isCustomUrlValid() {
        return this.githubUrl != null && this.githubUrl.startsWith("https://");
    }

    private String cleanedHost() {
        if (this.githubUrl.startsWith("https://")) {
            return this.githubUrl.replaceFirst("https://", "");
        }
        return this.githubUrl;
    }

}
