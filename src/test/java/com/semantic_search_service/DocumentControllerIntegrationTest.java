package com.semantic_search_service;

import com.semantic_search_service.controller.DocumentController;
import com.semantic_search_service.controller.HealthController;
import com.semantic_search_service.controller.SearchController;
import com.semantic_search_service.controller.dto.CreateDocumentResponse;
import com.semantic_search_service.controller.dto.DocumentResponse;
import com.semantic_search_service.controller.dto.SearchResponse;
import com.semantic_search_service.controller.dto.SearchResultItem;
import com.semantic_search_service.domain.DocumentStatus;
import com.semantic_search_service.exception.ResourceNotFoundException;
import com.semantic_search_service.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {DocumentController.class, SearchController.class, HealthController.class})
@ActiveProfiles("test")
class DocumentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;


    @Test
    void ping_returns200AndOK() throws Exception {
        mockMvc.perform(get("/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("Ok")); // "Ok" not "OK"
    }

    @Test
    void createDocument_returns201_withValidRequest() throws Exception {
        when(documentService.create(any()))
                .thenReturn(new CreateDocumentResponse(1L, DocumentStatus.READY));

        mockMvc.perform(post("/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "title": "Test Title",
                              "content": "Test content for the document",
                              "metadata": {}
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    void createDocument_returns400_whenTitleIsBlank() throws Exception {
        mockMvc.perform(post("/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "content": "Some content"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDocument_returns400_whenContentIsMissing() throws Exception {
        mockMvc.perform(post("/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "A title with no content"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDocument_returns400_whenMetadataIsNotAnObject() throws Exception {
        mockMvc.perform(post("/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Title",
                                  "content": "Content",
                                  "metadata": "this is a string not an object"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    //getById
    @Test
    void getDocument_returns200_whenFound() throws Exception {
        DocumentResponse response = new DocumentResponse(1L, "Test Title", "Test Content", null);
        when(documentService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/documents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Title"))
                .andExpect(jsonPath("$.content").value("Test Content"));
    }

    @Test
    void getDocument_returns404_whenNotFound() throws Exception {
        when(documentService.getById(99L))
                .thenThrow(new ResourceNotFoundException("Document not found: 99"));

        mockMvc.perform(get("/documents/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    // UPDATE
    @Test
    void updateDocument_returns200_withValidRequest() throws Exception {
        DocumentResponse updated = new DocumentResponse(1L, "Updated Title", "Updated Content", null);
        when(documentService.update(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/documents/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Title",
                                  "content": "Updated Content"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.content").value("Updated Content"));
    }

    @Test
    void updateDocument_returns404_whenNotFound() throws Exception {
        when(documentService.update(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Document not found: 99"));

        mockMvc.perform(put("/documents/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Title",
                                  "content": "Content"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateDocument_returns400_whenTitleIsBlank() throws Exception {
        mockMvc.perform(put("/documents/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "content": "Some content"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    //delete
    @Test
    void deleteDocument_returns204_whenFound() throws Exception {
        doNothing().when(documentService).delete(1L);

        mockMvc.perform(delete("/documents/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void deleteDocument_returns404_whenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Document not found: 99"))
                .when(documentService).delete(99L);

        mockMvc.perform(delete("/documents/99"))
                .andExpect(status().isNotFound());
    }

    //search
    @Test
    void search_returns200_withValidRequest() throws Exception {
        SearchResultItem item = new SearchResultItem(
                1L, "Title", "Content", null, 0.1, 0.9, 0.95);
        SearchResponse response = new SearchResponse(0, 5, 1, List.of(item));
        when(documentService.search(any())).thenReturn(response);

        mockMvc.perform(post("/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "vector databases",
                                  "page": 0,
                                  "size": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Title"))
                .andExpect(jsonPath("$.items[0].score").value(0.95));
    }

    @Test
    void search_returns400_whenQueryIsBlank() throws Exception {
        mockMvc.perform(post("/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "",
                                  "page": 0,
                                  "size": 5
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_returns400_whenMinScoreExceedsOne() throws Exception {
        mockMvc.perform(post("/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "something",
                                  "page": 0,
                                  "size": 5,
                                  "minScore": 1.5
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_returns400_whenPageIsNegative() throws Exception {
        mockMvc.perform(post("/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "something",
                                  "page": -1,
                                  "size": 5
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}