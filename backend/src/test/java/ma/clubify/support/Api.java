package ma.clubify.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Appelle l'API comme le ferait un frontend : par le contrat, jamais par les
 * services. Les tests de F01 se lisent donc comme des parcours réels.
 */
@Component
public class Api {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    /** Authentifie un utilisateur sans second facteur et retourne son jeton d'accès. */
    public String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new LoginBody(email, password, null))))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(body);
        return node.path("tokens").path("accessToken").asString();
    }

    public ResultActions loginRaw(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new LoginBody(email, password, null))));
    }

    public ResultActions loginRaw(String email, String password, String deviceToken) throws Exception {
        return mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new LoginBody(email, password, deviceToken))));
    }

    public ResultActions getAs(String token, String path) throws Exception {
        return mvc.perform(bearer(get("/api/v1" + path), token));
    }

    public ResultActions send(String token, MockHttpServletRequestBuilder builder, Object payload)
            throws Exception {
        builder.contentType(MediaType.APPLICATION_JSON);
        if (payload != null) {
            builder.content(json.writeValueAsString(payload));
        }
        return mvc.perform(bearer(builder, token));
    }

    public MockMvc mvc() {
        return mvc;
    }

    public ObjectMapper json() {
        return json;
    }

    private static MockHttpServletRequestBuilder bearer(MockHttpServletRequestBuilder b, String token) {
        return token == null ? b : b.header("Authorization", "Bearer " + token);
    }

    private record LoginBody(String email, String password, String deviceToken) {
    }
}
