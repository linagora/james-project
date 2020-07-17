package org.apache.james.jmap.model

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uri
import eu.timepit.refined.auto._

object RequestLevelErrorType {
  type ErrorTypeIdentifier = String Refined Uri
  val UNKNOWN_CAPABILITY: ErrorTypeIdentifier = "urn:ietf:params:jmap:error:unknownCapability"
  val NOT_JSON: ErrorTypeIdentifier = "urn:ietf:params:jmap:error:notJSON"
  val NOT_REQUEST: ErrorTypeIdentifier = "urn:ietf:params:jmap:error:notRequest"
  val LIMIT: ErrorTypeIdentifier = "urn:ietf:params:jmap:error:limit"
}
