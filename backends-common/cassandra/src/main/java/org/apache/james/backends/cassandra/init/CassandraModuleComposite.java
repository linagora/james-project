package org.apache.james.backends.cassandra.init;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.apache.james.backends.cassandra.components.CassandraIndex;
import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.backends.cassandra.components.CassandraTable;
import org.apache.james.backends.cassandra.components.CassandraType;

public class CassandraModuleComposite implements CassandraModule {

    private final ImmutableList<CassandraTable> tables;
    private final ImmutableList<CassandraIndex> index;
    private final ImmutableList<CassandraType> types;

    public CassandraModuleComposite(CassandraModule... modules) {
        tables = Arrays.stream(modules)
            .flatMap(module -> module.moduleTables().stream())
            .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
        index = Arrays.stream(modules)
            .flatMap(module -> module.moduleIndex().stream())
            .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
        types = Arrays.stream(modules)
            .flatMap(module -> module.moduleTypes().stream())
            .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
    }

    @Override
    public List<CassandraTable> moduleTables() {
        return tables;
    }

    @Override
    public List<CassandraIndex> moduleIndex() {
        return index;
    }

    @Override
    public List<CassandraType> moduleTypes() {
        return types;
    }
}
