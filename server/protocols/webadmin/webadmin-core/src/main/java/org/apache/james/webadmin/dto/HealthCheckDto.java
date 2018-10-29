package org.apache.james.webadmin.dto;

import org.apache.james.core.healthcheck.ComponentName;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.net.UrlEscapers;

@JsonPropertyOrder({"componentName", "escapedComponentName"})
public class HealthCheckDto {
	
	private ComponentName componentName;
	
	public HealthCheckDto(ComponentName componentName) {
		this.componentName = componentName;
	}
	
	public String getComponentName() {
		return componentName.getName();
	}
	
	public String getEscapedComponentName() {
		return UrlEscapers.urlPathSegmentEscaper().escape(componentName.getName());
	}
	
}
