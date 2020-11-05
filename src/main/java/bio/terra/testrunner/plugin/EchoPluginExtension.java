package bio.terra.testrunner.plugin;

/**
 * A Gradle Plugin Extension for Echoing messages.
 *
 * <p>Used as a POC.
 */
public class EchoPluginExtension {
  String msg = "echo";

  public String getMsg() {
    return msg;
  }

  public void setMsg(String msg) {
    this.msg = msg;
  }
}
