package bo.com.ganadero.shared.error;

import java.util.List;

public class SyncConflictException extends RuntimeException {
    private final ErrorCode code;
    private final Long localVersion;
    private final Long serverVersion;
    private final Object serverData;
    private final List<String> conflictingFields;
    private final String suggestedAction;

    public SyncConflictException(ErrorCode code, String message, Long localVersion, Long serverVersion,
                                 Object serverData, List<String> conflictingFields, String suggestedAction) {
        super(message);
        this.code = code;
        this.localVersion = localVersion;
        this.serverVersion = serverVersion;
        this.serverData = serverData;
        this.conflictingFields = conflictingFields;
        this.suggestedAction = suggestedAction;
    }

    public ErrorCode code() { return code; }
    public Long localVersion() { return localVersion; }
    public Long serverVersion() { return serverVersion; }
    public Object serverData() { return serverData; }
    public List<String> conflictingFields() { return conflictingFields; }
    public String suggestedAction() { return suggestedAction; }
}
