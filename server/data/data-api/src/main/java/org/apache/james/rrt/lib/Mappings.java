package org.apache.james.rrt.lib;

import org.apache.james.rrt.lib.Mapping.Type;

import com.google.common.base.Optional;

public interface Mappings extends Iterable<Mapping> {

    boolean contains(String mapping);

    int size();

    Mappings remove(String mapping);

    boolean isEmpty();

    Iterable<String> asStrings();
    
    String serialize();

    boolean contains(Type type);
    
    Mappings select(Type type);

    Mappings exclude(Type type);

    Mapping getError();

    Optional<Mappings> toOptional();

    Mappings union(Mappings mappings);
}