package julianusiv.discordwhitelist.data;

public class UserVerificationResult {
    public UserVerificationResult(WhitelistEntry entry, boolean isValid, boolean isSuccess) {
        this.entry = entry;
        this.isValid = isValid;
        this.isSuccess = isSuccess;
    }
    
    private WhitelistEntry entry;
    private boolean isValid;
    private boolean isSuccess;
    
    public WhitelistEntry getEntry() {
        return entry;
    }
    
    public boolean isValid() {
        return isValid;
    }

    public boolean isSuccess() {
        return isSuccess;
    }
}
