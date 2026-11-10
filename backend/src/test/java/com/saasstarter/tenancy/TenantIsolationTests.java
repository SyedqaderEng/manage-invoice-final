package com.saasstarter.tenancy;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantIsolationTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void userCannotReadAnotherUsersWorkspace() throws Exception {
        String userAToken = register("owner@example.com", "Owner", "Account", "SecurePass123");
        JsonNode userAWorkspace = createWorkspace(userAToken, "User A Workspace");

        String userBToken = register("other@example.com", "Other", "User", "SecurePass123");

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/members", userAWorkspace.get("id").asText())
                .header("Authorization", "Bearer " + userBToken))
            .andExpect(status().isForbidden());
    }

    private String register(String email, String firstName, String lastName, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                    "email", email, "password", password, "firstName", firstName, "lastName", lastName))))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private JsonNode createWorkspace(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/workspaces")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("name", name, "workspaceType", "ORGANIZATION"))))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
