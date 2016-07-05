package org.apache.james.jmap.model;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import nl.jqno.equalsverifier.EqualsVerifier;

public class BlobIdTest {

    @Test
    public void shouldNotAllowEmptyString() {
        assertThatThrownBy(() -> BlobId.of("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldNotAllowNullInput() {
        assertThatThrownBy(() -> BlobId.of(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void shouldCreateInstanceWhenSimpleString() {
        assertThat(BlobId.of("simple string")).extracting(BlobId::getRawValue).containsExactly("simple string");
    }
    
    @Test
    public void shouldRespectJavaBeanContract() {
        EqualsVerifier.forClass(BlobId.class).verify();
    }
}
