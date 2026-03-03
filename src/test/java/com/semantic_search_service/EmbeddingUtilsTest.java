package com.semantic_search_service;

import com.semantic_search_service.service.embedding.EmbeddingUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class EmbeddingUtilsTest {

    @Test
    void l2Normalized_producesUnitVector(){
        float [] input = {3.0f, 4.0f};
        float [] result = EmbeddingUtils.l2Normalized(input);

        assertThat(result[0]).isEqualTo(0.6f, within(1e-6f));
        assertThat(result[1]).isEqualTo(0.8f, within(1e-6f));
    }

    @Test
    void l2Normalized_resultHasUnitLength(){
        float [] input = {3.0f, 4.0f};
        float [] result = EmbeddingUtils.l2Normalized(input);

        double norm = 0.0;
        for (float f: result) norm += (double) f * f;
        assertThat(Math.sqrt(norm)).isCloseTo(1.0, within(1e-6));
    }

    @Test
    void l2Normalized_zeroVector_returnsZeroVector(){
        float [] input = {0.0f, 0.0f, 0.0f};

        float [] result = EmbeddingUtils.l2Normalized(input);
        assertThat(result).containsExactly(0.0f, 0.0f, 0.0f);
    }

    @Test
    void l2Normalize_doesNotMutateInput(){
        float [] input = {3.0f, 4.0f};
        EmbeddingUtils.l2Normalized(input);
        assertThat(input).containsExactly(3.0f, 4.0f);
    }

    @Test
    void l2Normalize_alreadyNormalized_isStable(){
        float [] input = {3.0f, 4.0f};
        float [] result = EmbeddingUtils.l2Normalized(input);

        assertThat(result[0]).isCloseTo(0.6f, within(1e-6f));
        assertThat(result[1]).isCloseTo(0.8f, within(1e-6f));
    }

    @Test
    void l2Normalize_singleElement_becomesOne(){
        float [] input = {5.0f};
        float [] result = EmbeddingUtils.l2Normalized(input);

        assertThat(result[0]).isEqualTo(1.0f, within(1e-6f));
    }
}
