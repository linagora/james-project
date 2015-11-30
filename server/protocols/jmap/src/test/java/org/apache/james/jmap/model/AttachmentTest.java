package org.apache.james.jmap.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.Test;

public class AttachmentTest {

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenBlobIdIsNull() {
        Attachment.builder().build();
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenTypeIsNull() {
        Attachment.builder().blobId("blobId").build();
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenNameIsNull() {
        Attachment.builder().blobId("blobId").type("type").build();
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenSizeIsNull() {
        Attachment.builder().blobId("blobId").type("type").name("name").build();
    }
    
    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenBlobIdIsEmpty() {
        Attachment.builder().blobId("").type("type").name("name").size(123).build();
    }
    
    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenTypeIsEmpty() {
        Attachment.builder().blobId("blobId").type("").name("name").size(123).build();
    }
    
    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenNameIsEmpty() {
        Attachment.builder().blobId("blobId").type("type").name("").size(123).build();
    }
    
    @Test
    public void buildShouldWorkWhenMandatoryFieldsArePresent() {
        Attachment expected = new Attachment("blobId", "type", "name", 123, Optional.empty(), false, Optional.empty(), Optional.empty());
        Attachment tested = Attachment.builder()
            .blobId("blobId")
            .type("type")
            .name("name")
            .size(123)
            .build();
        assertThat(tested).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void buildShouldWorkWithAllFieldsSet() {
        Attachment expected = new Attachment("blobId", "type", "name", 123, Optional.of("cid"), true, Optional.of(456L), Optional.of(789L));
        Attachment tested = Attachment.builder()
            .blobId("blobId")
            .type("type")
            .name("name")
            .size(123)
            .cid("cid")
            .isInline(true)
            .width(456)
            .height(789)
            .build();
        assertThat(tested).isEqualToComparingFieldByField(expected);
    }

}
