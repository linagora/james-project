package org.apache.james.webadmin.routes;

import org.apache.james.sieverepository.api.ScriptSummary;
import org.apache.james.sieverepository.api.SieveRepository;
import org.apache.james.sieverepository.api.exception.DuplicateException;
import org.apache.james.sieverepository.api.exception.IsActiveException;
import org.apache.james.sieverepository.api.exception.QuotaExceededException;
import org.apache.james.sieverepository.api.exception.QuotaNotFoundException;
import org.apache.james.sieverepository.api.exception.ScriptNotFoundException;
import org.apache.james.sieverepository.api.exception.StorageException;
import org.joda.time.DateTime;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySieveQuotaRepository implements SieveRepository {

    private boolean isGlobalQuotaSet = false;
    private long globalQuota = 0L;

    private Map<String, Long> userQuota = new ConcurrentHashMap<>();

    @Override
    public void haveSpace(final String user, final String name, final long size) throws QuotaExceededException, StorageException {

    }

    @Override
    public void putScript(final String user, final String name, final String content) throws StorageException, QuotaExceededException {

    }

    @Override
    public List<ScriptSummary> listScripts(final String user) throws StorageException {
        return null;
    }

    @Override
    public DateTime getActivationDateForActiveScript(final String user) throws StorageException, ScriptNotFoundException {
        return null;
    }

    @Override
    public InputStream getActive(final String user) throws ScriptNotFoundException, StorageException {
        return null;
    }

    @Override
    public void setActive(final String user, final String name) throws ScriptNotFoundException, StorageException {

    }

    @Override
    public InputStream getScript(final String user, final String name) throws ScriptNotFoundException, StorageException {
        return null;
    }

    @Override
    public void deleteScript(final String user, final String name) throws ScriptNotFoundException, IsActiveException, StorageException {

    }

    @Override
    public void renameScript(final String user, final String oldName, final String newName) throws ScriptNotFoundException, DuplicateException, StorageException {

    }

    @Override
    public boolean hasQuota() throws StorageException {
        return isGlobalQuotaSet;
    }

    @Override
    public long getQuota() throws QuotaNotFoundException, StorageException {
        if (!isGlobalQuotaSet) {
            throw new QuotaNotFoundException();
        }
        return globalQuota;
    }

    @Override
    public void setQuota(final long quota) throws StorageException {
        this.globalQuota = quota;
        this.isGlobalQuotaSet = true;
    }

    @Override
    public void removeQuota() throws QuotaNotFoundException, StorageException {
        if (!isGlobalQuotaSet) {
            throw new QuotaNotFoundException();
        }
        globalQuota = 0L;
        isGlobalQuotaSet = false;
    }

    @Override
    public boolean hasQuota(final String user) throws StorageException {
        return userQuota.containsKey(user);
    }

    @Override
    public long getQuota(final String user) throws QuotaNotFoundException, StorageException {
        final Long quotaValue = userQuota.get(user);
        if (quotaValue == null) {
            throw new QuotaNotFoundException();
        }
        return quotaValue;
    }

    @Override
    public void setQuota(final String user, final long quota) throws StorageException {
        userQuota.put(user, quota);
    }

    @Override
    public void removeQuota(final String user) throws QuotaNotFoundException, StorageException {
        final Long quotaValue = userQuota.get(user);
        if (quotaValue == null) {
            throw new QuotaNotFoundException();
        }
        userQuota.remove(user);
    }
}
