package org.apache.james.rrt.lib;

import org.apache.james.rrt.api.RecipientRewriteTable;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;


public class MappingImpl implements Mapping {

    private static final String ADDRESS_PREFIX = "";


    public static MappingImpl of(String mapping) {
        return new MappingImpl("", mapping);
    }
    
    public static MappingImpl address(String mapping) {
        return new MappingImpl(ADDRESS_PREFIX, mapping);
    }

    public static MappingImpl regex(String mapping) {
        return new MappingImpl(RecipientRewriteTable.REGEX_PREFIX, mapping);
    }

    public static MappingImpl error(String mapping) {
        return new MappingImpl(RecipientRewriteTable.ERROR_PREFIX, mapping);
    }

    public static MappingImpl domain(String mapping) {
        return new MappingImpl(RecipientRewriteTable.ALIASDOMAIN_PREFIX, mapping);
    }
    
    private final String mapping;

    private MappingImpl(String prefix, String mapping) {
        Preconditions.checkNotNull(mapping);
        this.mapping = prefix + mapping;
    }
    
    @Override
    public String asString() {
        return mapping;
    }
    
    @Override
    public boolean hasDomain() {
        return mapping.contains("@");
    }
    
    @Override
    public Mapping appendDomain(String domain) {
        Preconditions.checkNotNull(domain);
        return new MappingImpl("", mapping + "@" + domain);
    }
    
    @Override
    public Type getType() {
        if (mapping.startsWith(RecipientRewriteTable.ALIASDOMAIN_PREFIX)) {
            return Type.Domain;
        } else if (mapping.startsWith(RecipientRewriteTable.REGEX_PREFIX)) {
            return Type.Regex;
        } else if (mapping.startsWith(RecipientRewriteTable.ERROR_PREFIX)) {
            return Type.Error;
        } else {
            return Type.Address;
        }
    }
    
    @Override
    public String getErrorMessage() {
        Preconditions.checkState(mapping.startsWith(RecipientRewriteTable.ERROR_PREFIX));
        return mapping.substring(RecipientRewriteTable.ERROR_PREFIX.length());
    }
    
    @Override
    public boolean equals(Object other) {
        if (other instanceof MappingImpl) {
            MappingImpl otherMapping = (MappingImpl) other;
            return Objects.equal(mapping, otherMapping.mapping);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(mapping);
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(getClass()).add("mapping", mapping).toString();
    }
    
}