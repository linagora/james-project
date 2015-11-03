package org.apache.james.jmap;

import javax.servlet.Filter;

import org.apache.james.jmap.api.AccessTokenManager;
import org.apache.james.jmap.crypto.JamesSignatureHandlerModule;
import org.apache.james.jmap.crypto.SignatureHandler;
import org.apache.james.jmap.utils.ZonedDateTimeProvider;
import org.apache.james.user.api.UsersRepository;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

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
            bindConstant().annotatedWith(Names.named("tokenExpirationInMs")).to(100L);
        }
    }

    public TestServer(UsersRepository usersRepository, ZonedDateTimeProvider zonedDateTimeProvider) {
        this.usersRepository = usersRepository;
        this.zonedDateTimeProvider = zonedDateTimeProvider;
    }

    private Server server;

    public void start() throws Exception {
        Injector injector = Guice.createInjector(Modules.override(new JMAPCommonModule())
                .with(new JMAPModuleTest()));
        accessTokenManager = injector.getInstance(AccessTokenManager.class);
        initJamesSignatureHandler(injector);

        server = new Server(JMAPAuthenticationTest.RANDOM_PORT);

        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);

        AuthenticationServlet authenticationServlet = injector.getInstance(AuthenticationServlet.class);
        ServletHolder servletHolder = new ServletHolder(authenticationServlet);
        handler.addServletWithMapping(servletHolder, "/*");

        AuthenticationFilter authenticationFilter = new AuthenticationFilter(accessTokenManager);
        Filter getAuthenticationFilter = new BypassOnPostFilter(authenticationFilter);
        FilterHolder authenticationFilterHolder = new FilterHolder(getAuthenticationFilter);
        handler.addFilterWithMapping(authenticationFilterHolder, "/*", null);

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
        return ((ServerConnector) server.getConnectors()[0]).getLocalPort();
    }
    
    public AccessTokenManager getAccessTokenManager() {
        return accessTokenManager;
    }
}