package bio.terra.testrunner.secrets;

public class Secrets {
  public String getVaultAddr() {
    return System.getenv("INPUT_VAULT_ADDR");
  }
}
