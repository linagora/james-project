

package org.apache.james.sieverepository.file;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.filesystem.api.FileSystemFixture;
import org.apache.james.sieverepository.api.SieveRepository;
import org.apache.james.sieverepository.lib.AbstractSieveRepositoryTest;
import org.junit.After;
import org.junit.Before;

public class SieveFileRepositoryTest extends AbstractSieveRepositoryTest {

    private static final String SIEVE_ROOT = FileSystem.FILE_PROTOCOL + "sieve";

    private FileSystem fileSystem;

    @Override
    @Before
    public void setUp() throws Exception {
        this.fileSystem = FileSystemFixture.TEMP_FILE_SYSTEM.get();
        super.setUp();
    }

    @Override
    protected SieveRepository createSieveRepository() throws Exception {
        return new SieveFileRepository(fileSystem);
    }

    @After
    public void tearDown() throws Exception {
        File root = fileSystem.getFile(SIEVE_ROOT);
        // Remove files from the previous test, if any
        if (root.exists()) {
            FileUtils.forceDelete(root);
        }
    }
}
