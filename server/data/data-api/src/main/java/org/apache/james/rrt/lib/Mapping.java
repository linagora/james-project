package org.apache.james.rrt.lib;


public interface Mapping {

    enum Type { Regex, Domain, Error, Address };
    
    Type getType();
    
    String asString();

    boolean hasDomain();

    Mapping appendDomain(String domain);

    String getErrorMessage();

}