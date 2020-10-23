FROM adoptopenjdk/openjdk11:ubuntu-jre-nightly

# COPY terra-test-runner binaries when ready
# COPY build/terra-test-runner.jar .

# set entrypoint command
ENTRYPOINT [ "java", "cp", "terra-test-runner.jar" ]