package com.deloitte.code.generator;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Condition {

	private String operatorValue;
	
	private String ifValue;
	
	private String setValue;
	
	public String getOperatorValue() {
		return operatorValue;
	}

	public void setOperatorValue(String operatorValue) {
		this.operatorValue = operatorValue;
	}

	public String getIfValue() {
		return ifValue;
	}

	public void setIfValue(String ifValue) {
		this.ifValue = ifValue;
	}

	public String getSetValue() {
		return setValue;
	}

	public void setSetValue(String setValue) {
		this.setValue = setValue;
	}

	public boolean isStatic() {
		return isStatic;
	}

	public void setStatic(boolean isStatic) {
		this.isStatic = isStatic;
	}

	@JsonProperty("isStatic")
	private boolean isStatic;
	
}
