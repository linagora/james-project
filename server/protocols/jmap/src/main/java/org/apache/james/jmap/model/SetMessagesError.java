package org.apache.james.jmap.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.james.jmap.model.MessageProperties.MessageProperty;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;

public class SetMessagesError extends SetError {

    public static SetMessagesError.Builder builder() {
        return new Builder();
    }
    
    public static class Builder extends SetError.Builder {
        
        private List<BlobId> attachmentsNotFound;

        private Builder() {
            super();
            attachmentsNotFound = new ArrayList<>();
        }

        @Override
        public Builder description(String description) {
            return (Builder) super.description(description);
        }
        
        @Override
        public Builder properties(MessageProperty... properties) {
            return (Builder) super.properties(properties);
        }
        
        @Override
        public Builder properties(Set<MessageProperty> properties) {
            return (Builder) super.properties(properties);
        }
        
        @Override
        public Builder type(String type) {
            return (Builder) super.type(type);
        }
        
        public Builder attachmentsNotFound(BlobId... attachmentIds) {
            return attachmentsNotFound(Arrays.asList(attachmentIds));
        }
        
        public Builder attachmentsNotFound(List<BlobId> attachmentIds) {
            this.attachmentsNotFound.addAll(attachmentIds);
            return this;
        }
        
        @Override
        public SetError build() {
            return new SetMessagesError(super.build(), ImmutableList.copyOf(attachmentsNotFound));
        }
    }

    private ImmutableList<BlobId> attachmentsNotFound;
    
    public SetMessagesError(SetError setError, ImmutableList<BlobId> attachmentsNotFound) {
        super(setError);
        this.attachmentsNotFound = attachmentsNotFound;
    }
    
    @JsonSerialize
    public ImmutableList<BlobId> getAttachmentsNotFound() {
        return attachmentsNotFound;
    }
}
