package com.saasstarter.tenancy;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class WorkspaceAdministrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void ownerCanManageMemberRolesButMemberCannotGrantAdmin() throws Exception {
        String ownerToken = register("owner-admin@example.com");
        String memberToken = register("workspace-member@example.com");
        JsonNode workspace = createWorkspace(ownerToken);

        MvcResult added = mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/members", workspace.get("id").asText())
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"workspace-member@example.com","role":"MEMBER"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("MEMBER"))
            .andReturn();
        String memberId = objectMapper.readTree(added.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}/members/{memberId}", workspace.get("id").asText(), memberId)
                .header("Authorization", "Bearer " + memberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"role":"ADMIN"}
                    """))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}/members/{memberId}", workspace.get("id").asText(), memberId)
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"role":"ADMIN"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                    "email", email, "password", "SecurePass123", "firstName", "Test", "lastName", "User"))))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private JsonNode createWorkspace(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/workspaces")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"SaaS Team","workspaceType":"ORGANIZATION"}
                    """))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
