/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tarantool.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.tqe.configuration.grpc.GrpcConfiguration;
import org.testcontainers.containers.tqe.configuration.grpc.TLSParams;

class GrpcTlsConfigurationTest {

  @Test
  void testReadsTlsParamsOfGrpcOptions() throws Exception {
    Path config =
        Paths.get(
            Objects.requireNonNull(
                    GrpcTlsConfigurationTest.class
                        .getClassLoader()
                        .getResource("tqe3/simple-config/tls-grpc.yml"))
                .toURI());

    GrpcConfiguration parsed = ConfigurationUtils.readGrpcFromFile(config);

    TLSParams tls = parsed.getGrpcOptions().orElseThrow().getTls().orElseThrow();
    Assertions.assertAll(
        () -> Assertions.assertEquals(true, tls.getEnabled().orElseThrow()),
        () -> Assertions.assertEquals("/certs/server.crt", tls.getCertFile().orElseThrow()),
        () -> Assertions.assertEquals("/certs/server.key", tls.getKeyFile().orElseThrow()),
        () -> Assertions.assertEquals("/certs/ca.crt", tls.getCaFile().orElseThrow()),
        () ->
            Assertions.assertEquals("ECDHE-RSA-AES256-GCM-SHA384", tls.getCiphers().orElseThrow()));
  }
}
