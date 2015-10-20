package org.apache.james.jmap;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.apache.james.jmap.JMAPServlet.RawRequest;
import org.apache.james.jmap.JMAPServlet.Request;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class JMAPServletTest {

    @Test
    public void unserializedRawRequestsShouldReturnAListOfObjects() throws JsonParseException, JsonMappingException, IOException {
        assertThat(new JMAPServlet().unserialize("[\"getAccounts\", {}, \"#0\"]"))
        .isExactlyInstanceOf(RawRequest[].class);
    }
    
}
