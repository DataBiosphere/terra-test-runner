package bio.terra.testrunner.runner.version;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

// Builder for git versions
@SuppressFBWarnings(
    value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
    justification = "This POJO class is used for easy serialization to JSON using Jackson.")
public class GitVersion {
  public String remoteOriginUrl;
  public String branch;
  public String refHeadCommit;
  public String shortRefHeadCommit;

  public GitVersion(Builder builder) {
    this.remoteOriginUrl = builder.remoteOriginUrl;
    this.branch = builder.branch;
    this.refHeadCommit = builder.refHeadCommit;
    this.shortRefHeadCommit = builder.shortRefHeadCommit;
  }

  public static class Builder {
    private String remoteOriginUrl;
    private String branch;
    private String refHeadCommit;
    private String shortRefHeadCommit;

    public Builder remoteOriginUrl(String remoteOriginUrl) {
      this.remoteOriginUrl = remoteOriginUrl;
      return this;
    }

    public Builder branch(String branch) {
      this.branch = branch;
      return this;
    }

    public Builder refHeadCommit(String refHeadCommit) {
      this.refHeadCommit = refHeadCommit;
      return this;
    }

    public Builder shortRefHeadCommit(String shortRefHeadCommit) {
      this.shortRefHeadCommit = shortRefHeadCommit;
      return this;
    }

    public GitVersion build() {
      return new GitVersion(this);
    }
  }
}
