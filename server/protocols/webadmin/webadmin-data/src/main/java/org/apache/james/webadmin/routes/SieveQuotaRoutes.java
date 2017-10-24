package org.apache.james.webadmin.routes;

import com.google.common.base.Preconditions;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.james.sieverepository.api.SieveRepository;
import org.apache.james.sieverepository.api.exception.QuotaNotFoundException;
import org.apache.james.webadmin.Constants;
import org.apache.james.webadmin.Routes;
import org.apache.james.webadmin.utils.JsonExtractException;
import org.apache.james.webadmin.utils.JsonExtractor;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Service;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import static org.apache.james.webadmin.Constants.SEPARATOR;

@Api(tags = "SieveQuota")
@Path(SieveQuotaRoutes.ROOT_PATH)
@Produces("application/json")
public class SieveQuotaRoutes implements Routes {

    static final String ROOT_PATH = "/sieve/quota";
    private static final String USER_ID = "userId";
    private static final String USER_SIEVE_QUOTA_PATH = ROOT_PATH + SEPARATOR + ":" + USER_ID;
    private static final String REQUESTED_SIZE = "requestedSize";
    private static final Logger LOGGER = LoggerFactory.getLogger(SieveQuotaRoutes.class);

    private final SieveRepository sieveRepository;
    private final JsonTransformer jsonTransformer;
    private final JsonExtractor<Long> jsonExtractor;
    private Service service;

    @Inject
    public SieveQuotaRoutes(final SieveRepository sieveRepository, final JsonTransformer jsonTransformer) {
        this.sieveRepository = sieveRepository;
        this.jsonTransformer = jsonTransformer;
        this.jsonExtractor = new JsonExtractor<>(Long.class);
    }

    @Override
    public void define(final Service service) {
        this.service = service;

        defineGetGlobalSieveQuota();
        defineUpdateGlobalSieveQuota();
        defineRemoveGlobalSieveQuota();

        defineGetPerUserSieveQuota();
        defineUpdatePerUserSieveQuota();
        defineRemovePerUserSieveQuota();
    }

    @GET
    @ApiOperation(value = "Reading global sieve quota size")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = Long.class),
            @ApiResponse(code = 404, message = "Global sieve quota not set."),
            @ApiResponse(code = 500, message = "Internal server error - Something went bad on the server side.")
    })
    public void defineGetGlobalSieveQuota() {
        service.get(ROOT_PATH, (request, response) -> {
            try {
                final long sieveQuota = sieveRepository.getQuota();
                response.status(200);
                return sieveQuota;
            } catch (QuotaNotFoundException e) {
                LOGGER.info("Global sieve quota not set", e);
                response.status(404);
            }
            return Constants.EMPTY_BODY;
        }, jsonTransformer);
    }

    @PUT
    @ApiOperation(value = "Update global sieve quota size")
    @ApiImplicitParams({
            @ApiImplicitParam(required = true, dataType = "long", name = REQUESTED_SIZE, paramType = "body")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = Long.class),
            @ApiResponse(code = 400, message = "The body is not a positive integer."),
            @ApiResponse(code = 500, message = "Internal server error - Something went bad on the server side.")
    })
    public void defineUpdateGlobalSieveQuota() {
        service.put(ROOT_PATH, (request, response) -> {
            try {
                final Long requestedSize = extractRequestedQuotaSizeFromRequest(request);
                sieveRepository.setQuota(requestedSize);
                response.status(200);
                return sieveRepository.getQuota();
            } catch (JsonExtractException e) {
                LOGGER.info("Malformed JSON", e);
                response.status(400);
            } catch (IllegalArgumentException e) {
                LOGGER.info("Requested quota size in not a positive integer", e);
                response.status(400);
            }
            return Constants.EMPTY_BODY;
        }, jsonTransformer);
    }

    @DELETE
    @ApiOperation(value = "Removes global sieve quota")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Global sieve quota removed."),
            @ApiResponse(code = 404, message = "Global sieve quota not set."),
            @ApiResponse(code = 500, message = "Internal server error - Something went bad on the server side.")
    })
    public void defineRemoveGlobalSieveQuota() {
        service.delete(ROOT_PATH, (request, response) -> {
            try {
                sieveRepository.removeQuota();
                response.status(204);
            } catch (QuotaNotFoundException e) {
                LOGGER.info("Global sieve quota not set", e);
                response.status(404);
            }
            return Constants.EMPTY_BODY;
        });
    }

    @GET
    @Path(value = ROOT_PATH + "/{" + USER_ID + "}")
    @ApiImplicitParams({
            @ApiImplicitParam(required = true, dataType = "string", name = USER_ID, paramType = "path")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = Long.class),
            @ApiResponse(code = 404, message = "User sieve quota not set."),
            @ApiResponse(code = 500, message = "Internal server error - Something went bad on the server side.")
    })
    public void defineGetPerUserSieveQuota() {
        service.get(USER_SIEVE_QUOTA_PATH, (request, response) -> {
            final String userId = request.params(USER_ID);
            try {
                final long userQuota = sieveRepository.getQuota(userId);
                response.status(200);
                return userQuota;
            } catch (QuotaNotFoundException e) {
                LOGGER.info("User sieve quota not set", e);
                response.status(404);
            }
            return Constants.EMPTY_BODY;
        }, jsonTransformer);
    }

    @PUT
    @Path(value = ROOT_PATH + "/{" + USER_ID + "}")
    @ApiImplicitParams({
            @ApiImplicitParam(required = true, dataType = "string", name = USER_ID, paramType = "path"),
            @ApiImplicitParam(required = true, dataType = "long", name = REQUESTED_SIZE, paramType = "body")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = Long.class),
            @ApiResponse(code = 400, message = "The body is not a positive integer."),
            @ApiResponse(code = 500, message = "Internal server error - Something went bad on the server side.")
    })
    public void defineUpdatePerUserSieveQuota() {
        service.put(USER_SIEVE_QUOTA_PATH, (request, response) -> {
            final String userId = request.params(USER_ID);
            try {
                final Long requestedSize = extractRequestedQuotaSizeFromRequest(request);
                sieveRepository.setQuota(userId, requestedSize);
                response.status(200);
                return sieveRepository.getQuota(userId);
            } catch (JsonExtractException e) {
                LOGGER.info("Malformed JSON", e);
                response.status(400);
            } catch (IllegalArgumentException e) {
                LOGGER.info("Requested quota size in not a positive integer", e);
                response.status(400);
            }
            return Constants.EMPTY_BODY;
        }, jsonTransformer);
    }

    @DELETE
    @Path(value = ROOT_PATH + "/{" + USER_ID + "}")
    @ApiImplicitParams({
            @ApiImplicitParam(required = true, dataType = "string", name = USER_ID, paramType = "path")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "User sieve quota removed."),
            @ApiResponse(code = 404, message = "User sieve quota not set."),
            @ApiResponse(code = 500, message = "Internal server error - Something went bad on the server side.")
    })
    public void defineRemovePerUserSieveQuota() {
        service.delete(USER_SIEVE_QUOTA_PATH, (request, response) -> {
            final String userId = request.params(USER_ID);
            try {
                sieveRepository.removeQuota(userId);
                response.status(204);
            } catch (QuotaNotFoundException e) {
                LOGGER.info("User sieve quota not set", e);
                response.status(404);
            }
            return Constants.EMPTY_BODY;
        });
    }

    private Long extractRequestedQuotaSizeFromRequest(final Request request) throws JsonExtractException {
        final Long requestedSize = jsonExtractor.parse(request.body());
        Preconditions.checkArgument(requestedSize >= 0, "Requested quota size have to be a positive integer");
        return requestedSize;
    }
}
