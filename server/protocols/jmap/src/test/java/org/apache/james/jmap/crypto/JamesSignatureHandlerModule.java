package org.apache.james.jmap.crypto;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.jmap.JMAPConfiguration;
import org.apache.james.protocols.lib.KeystoreLoader;

import com.google.inject.AbstractModule;

public class JamesSignatureHandlerModule extends AbstractModule {
    
    @Singleton
    private static class KeystoreFileSystem implements FileSystem {
        @Override
        public InputStream getResource(String url) throws IOException {
            return ClassLoader.getSystemResourceAsStream("keystore");
        }

        @Override
        public File getFile(String fileURL) throws FileNotFoundException {
            return null;
        }

        @Override
        public File getBasedir() throws FileNotFoundException {
            return null;
        }
    }
    
    @Singleton
    private static class TestJamesSignatureHandler extends JamesSignatureHandler {

        @Inject
        TestJamesSignatureHandler(KeystoreLoader keystoreLoader) {
            super(keystoreLoader, new JMAPConfiguration("keystore", "james72laBalle"));
        }

        @Override
        public void init() throws Exception {
            super.init();
        }
    }

    @Override
    protected void configure() {
        bind(FileSystem.class).to(KeystoreFileSystem.class);
        bind(SignatureHandler.class).to(TestJamesSignatureHandler.class);
    }
}
