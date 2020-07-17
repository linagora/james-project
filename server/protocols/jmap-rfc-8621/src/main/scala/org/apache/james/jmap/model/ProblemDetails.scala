package org.apache.james.jmap.model

import org.apache.james.jmap.model.RequestLevelErrorType.ErrorTypeIdentifier

/**
 * Problem Details for HTTP APIs within the JMAP context
 * https://tools.ietf.org/html/rfc7807
 * see https://jmap.io/spec-core.html#errors
 */
case class ProblemDetails(`type`: ErrorTypeIdentifier, status: Int, limit: Option[String], detail : String)
