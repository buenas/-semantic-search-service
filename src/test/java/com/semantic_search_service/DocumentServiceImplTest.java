package com.semantic_search_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semantic_search_service.controller.dto.CreateDocumentRequest;
import com.semantic_search_service.controller.dto.CreateDocumentResponse;
import com.semantic_search_service.controller.dto.DocumentResponse;
import com.semantic_search_service.controller.dto.UpdateDocumentRequest;
import com.semantic_search_service.domain.Document;
import com.semantic_search_service.domain.DocumentStatus;
import com.semantic_search_service.exception.ResourceNotFoundException;
import com.semantic_search_service.repository.DocumentRepository;
import com.semantic_search_service.service.embedding.EmbeddingClient;
import com.semantic_search_service.service.impl.DocumentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private EmbeddingClient embeddingClient;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DocumentServiceImpl documentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void injectObjectMapper() throws Exception {
        var field = DocumentServiceImpl.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(documentService, objectMapper);
    }

    //create
    @Test
    void create_savesDocumentAndReturnsReady() {
        CreateDocumentRequest request = new CreateDocumentRequest();
        request.setTitle("Test Title");
        request.setContent("Test Content");

        Document saved = buildDocument(1L, "Test Title", "Test Content", DocumentStatus.PENDING);
        when(documentRepository.save(any(Document.class))).thenReturn(saved);
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.6f, 0.8f});
        doReturn(1).when(jdbcTemplate).update(contains("status = 'READY'"), anyString(), eq(1L));

        CreateDocumentResponse response = documentService.create(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(DocumentStatus.READY);
        verify(documentRepository).save(any(Document.class));
        verify(embeddingClient).embed("Test Title\n\nTest Content");
    }

    @Test
    void create_embedsCorrectText() {
        CreateDocumentRequest request = new CreateDocumentRequest();
        request.setTitle("My Title");
        request.setContent("My Content");

        Document saved = buildDocument(1L, "My Title", "My Content", DocumentStatus.PENDING);
        when(documentRepository.save(any())).thenReturn(saved);
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.6f, 0.8f});
        doReturn(1).when(jdbcTemplate).update(contains("status = 'READY'"), anyString(), eq(1L));

        documentService.create(request);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(embeddingClient).embed(textCaptor.capture());
        assertThat(textCaptor.getValue()).isEqualTo("My Title\n\nMy Content");
    }

    @Test
    void create_whenEmbeddingFails_marksDocumentFailed() {
        CreateDocumentRequest request = new CreateDocumentRequest();
        request.setTitle("Title");
        request.setContent("Content");

        Document saved = buildDocument(1L, "Title", "Content", DocumentStatus.PENDING);
        when(documentRepository.save(any())).thenReturn(saved);
        when(embeddingClient.embed(anyString())).thenThrow(new RuntimeException("OpenAI is down"));
        // Match the FAILED update: (sql, errorMessage, id)
        doReturn(1).when(jdbcTemplate).update(contains("status = 'FAILED'"), anyString(), eq(1L));

        assertThatThrownBy(() -> documentService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Embedding failed");

        verify(jdbcTemplate).update(contains("status = 'FAILED'"), eq("OpenAI is down"), eq(1L));
    }

    //getById
    @Test
    void getById_returnsDocument_whenFound() {
        Document doc = buildDocument(1L, "My Title", "My Content", DocumentStatus.READY);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

        DocumentResponse response = documentService.getById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("My Title");
        assertThat(response.getContent()).isEqualTo("My Content");
    }

    @Test
    void getById_throwsResourceNotFoundException_whenNotFound() {
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    //update
    @Test
    void update_updatesFieldsAndReturnsDocument() {
        Document existing = buildDocument(1L, "Old Title", "Old Content", DocumentStatus.READY);
        Document updated  = buildDocument(1L, "New Title", "New Content", DocumentStatus.READY);

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(existing))
                .thenReturn(Optional.of(updated));
        when(documentRepository.save(any())).thenReturn(existing);
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.6f, 0.8f});
        doReturn(1).when(jdbcTemplate).update(contains("status = 'READY'"), anyString(), eq(1L));

        UpdateDocumentRequest request = new UpdateDocumentRequest();
        request.setTitle("New Title");
        request.setContent("New Content");

        DocumentResponse response = documentService.update(1L, request);

        assertThat(response.getTitle()).isEqualTo("New Title");
        assertThat(response.getContent()).isEqualTo("New Content");
        verify(embeddingClient).embed("New Title\n\nNew Content");
    }

    @Test
    void update_throwsResourceNotFoundException_whenNotFound() {
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateDocumentRequest request = new UpdateDocumentRequest();
        request.setTitle("Title");
        request.setContent("Content");

        assertThatThrownBy(() -> documentService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verifyNoInteractions(embeddingClient);
    }

    //delete
    @Test
    void delete_deletesDocument_whenFound() {
        Document doc = buildDocument(1L, "My Title", "My Content", DocumentStatus.READY);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));
        documentService.delete(1L);
        verify(documentRepository).deleteById(1L);
    }

    @Test
    void delete_throwsResourceNotFoundException_whenNotFound() {
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> documentService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(documentRepository, never()).deleteById(any());
    }

    private Document buildDocument(Long id, String title, String content, DocumentStatus status) {
        Document doc = new Document();
        doc.setId(id);
        doc.setTitle(title);
        doc.setContent(content);
        doc.setStatus(status);
        return doc;
    }
}