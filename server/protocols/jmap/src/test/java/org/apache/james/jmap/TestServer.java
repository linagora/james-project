package org.apache.james.jmap;

import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;

import org.apache.james.http.jetty.Configuration;
import org.apache.james.http.jetty.JettyHttpServer;
import org.apache.james.jmap.api.AccessTokenManager;
import org.apache.james.jmap.crypto.JamesSignatureHandlerModule;
import org.apache.james.jmap.crypto.SignatureHandler;
import org.apache.james.jmap.utils.ZonedDateTimeProvider;
import org.apache.james.user.api.UsersRepository;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.google.inject.servlet.ServletModule;
import com.google.inject.util.Modules;

public class TestServer {

    private UsersRepository usersRepository;
    private ZonedDateTimeProvider zonedDateTimeProvider;
    private AccessTokenManager accessTokenManager;

    private class JMAPModuleTest extends ServletModule {

        @Override
        protected void configureServlets() {
            install(new JamesSignatureHandlerModule());
            bind(UsersRepository.class).toInstance(usersRepository);
            bind(ZonedDateTimeProvider.class).toInstance(zonedDateTimeProvider);
            bindConstant().annotatedWith(Names.named("tokenExpirationInMs")).to(TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS));
        }
    }

    public TestServer(UsersRepository usersRepository, ZonedDateTimeProvider zonedDateTimeProvider) {
        this.usersRepository = usersRepository;
        this.zonedDateTimeProvider = zonedDateTimeProvider;
    }

    private JettyHttpServer server;

    public void start() throws Exception {
        Injector injector = Guice.createInjector(Modules.override(new JMAPCommonModule())
                .with(new JMAPModuleTest()));
        accessTokenManager = injector.getInstance(AccessTokenManager.class);
        initJamesSignatureHandler(injector);

        AuthenticationServlet authenticationServlet = injector.getInstance(AuthenticationServlet.class);
        AuthenticationFilter authenticationFilter = new AuthenticationFilter(accessTokenManager);
        Filter getAuthenticationFilter = new BypassOnPostFilter(authenticationFilter);
        server = JettyHttpServer.create(Configuration.builder()
                .serve("/*").with(authenticationServlet)
                .filter("/*").with(getAuthenticationFilter)
                .randomPort()
                .build());

        server.start();
    }

    private void initJamesSignatureHandler(Injector injector) throws Exception {
        SignatureHandler signatureHandler = injector.getInstance(SignatureHandler.class);
        signatureHandler.init();
   }

    public void stop() throws Exception {
        server.stop();
    }
    
    public int getLocalPort() {
        return server.getPort();
    }
    
    public AccessTokenManager getAccessTokenManager() {
        return accessTokenManager;
    }
}