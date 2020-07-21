package org.apache.james.jmap.model

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Interval.Closed

object StatusCode {
  type ErrorStatus = Int Refined Closed[100, 511]
}
