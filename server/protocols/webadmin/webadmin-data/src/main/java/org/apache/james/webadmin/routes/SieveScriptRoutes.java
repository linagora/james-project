/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.webadmin.routes;

import static org.apache.james.webadmin.Constants.SEPARATOR;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import org.apache.james.core.User;
import org.apache.james.sieverepository.api.ScriptContent;
import org.apache.james.sieverepository.api.ScriptName;
import org.apache.james.sieverepository.api.SieveRepository;
import org.apache.james.sieverepository.api.exception.QuotaExceededException;
import org.apache.james.sieverepository.api.exception.ScriptNotFoundException;
import org.apache.james.sieverepository.api.exception.StorageException;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.webadmin.Constants;
import org.apache.james.webadmin.Routes;
import org.apache.james.webadmin.utils.ErrorResponder;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.eclipse.jetty.http.HttpStatus;

import com.github.fge.lambdas.Throwing;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import spark.HaltException;
import spark.Request;
import spark.Response;
import spark.Service;

@Api(tags = "SieveScript")
public class SieveScriptRoutes implements Routes {

    public static final String ROOT_PATH = "/sieve";
    public static final String SCRIPT = "scripts";
    private static final String USER_NAME = "userName";
    private static final String SCRIPT_NAME = "scriptName";
    private static final String ACTIVATE_PARAMS = "activate";
    private static final String USER_SCRIPT_PATH = Joiner.on(SEPARATOR)
        .join(ROOT_PATH, ":" + USER_NAME, SCRIPT, ":" + SCRIPT_NAME);

    private final SieveRepository sieveRepository;
    private final UsersRepository usersRepository;
    private final JsonTransformer jsonTransformer;

    @Inject
    public SieveScriptRoutes(SieveRepository sieveRepository, UsersRepository usersRepository, JsonTransformer jsonTransformer) {
        this.sieveRepository = sieveRepository;
        this.usersRepository = usersRepository;
        this.jsonTransformer = jsonTransformer;
    }

    @Override
    public String getBasePath() {
        return ROOT_PATH;
    }

    @Override
    public void define(Service service) {
        defineAddActiveSieveScript(service);
    }

    @PUT
    @ApiOperation(value = "Upload a new Sieve Script")
    @Path(value = ROOT_PATH + "/{" + USER_NAME + "}/" + SCRIPT + "/{" + SCRIPT_NAME + "}")
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.NO_CONTENT_204, message = "OK"),
        @ApiResponse(code = HttpStatus.NOT_FOUND_404,
            message = "Invalid put Sieve script for non existent user")
    })
    @ApiImplicitParams({
        @ApiImplicitParam(
            required = false,
            paramType = "query parameter",
            dataType = "Boolean",
            defaultValue = "None",
            example = "?activate=true",
            value = "If present, automatically activating the script.")
    })
    public void defineAddActiveSieveScript(Service service) {
        service.put(USER_SCRIPT_PATH, this::addActiveSieveScript, jsonTransformer);
    }

    private Object addActiveSieveScript(Request request, Response response) throws UsersRepositoryException, QuotaExceededException, StorageException, ScriptNotFoundException {
        User user = extractUser(request);
        ScriptName script = extractScriptName(request);
        sieveRepository.putScript(user, script, extractSieveScriptFromRequest(request));
        if (isActivate(request.queryParams(ACTIVATE_PARAMS))) {
            sieveRepository.setActive(user, script);
        }
        response.status(HttpStatus.NO_CONTENT_204);
        return Constants.EMPTY_BODY;
    }

    private User extractUser(Request request) throws UsersRepositoryException {
        return Optional.ofNullable(request.params(":" + USER_NAME))
            .filter(Throwing.predicate(userName -> !Strings.isNullOrEmpty(userName) && usersRepository.contains(userName)))
            .map(User::fromUsername)
            .orElseThrow(() -> throw404withInvalidArgument("Invalid put Sieve script for non existent user"));
    }

    private ScriptName extractScriptName(Request request) {
        return Optional.ofNullable(request.params(":" + SCRIPT_NAME))
            .map(String::trim)
            .filter(scriptName -> !Strings.isNullOrEmpty(scriptName))
            .map(ScriptName::new)
            .orElseThrow(() -> throw404withInvalidArgument("Invalid Sieve script name"));
    }

    private ScriptContent extractSieveScriptFromRequest(Request request) {
        String script = request.body();
        Preconditions.checkNotNull(script);
        return new ScriptContent(script);
    }

    private boolean isActivate(String activateParam) {
        return Optional.ofNullable(activateParam)
            .map(Boolean::parseBoolean)
            .orElse(false);
    }

    private HaltException throw404withInvalidArgument(String message) {
        throw ErrorResponder.builder()
            .statusCode(HttpStatus.NOT_FOUND_404)
            .type(ErrorResponder.ErrorType.INVALID_ARGUMENT)
            .message(message)
            .haltError();
    }
}
