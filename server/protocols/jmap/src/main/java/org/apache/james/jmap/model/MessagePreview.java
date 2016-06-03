package org.apache.james.jmap.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.james.jmap.model.message.IndexableMessage;
import org.jsoup.Jsoup;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

public class MessagePreview {
    
    public static final String NO_BODY = "(Empty)";
    public static final int MAX_PREVIEW_LENGTH = 256;

    public static String from(IndexableMessage message) {
        Preconditions.checkNotNull(message);
        
        return message.getBodyText()
            .filter(text -> !text.isEmpty())
            .map(MessagePreview::getPreview)
            .orElse(NO_BODY);
    }
    
    @VisibleForTesting static String getPreview(String body) {
        String bodyWithoutTag = Jsoup.parse(body).text();
        return StringUtils.abbreviate(bodyWithoutTag, MAX_PREVIEW_LENGTH);
    }

}
