/*
 * Copyright 2015-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.zookeeper.discovery.configclient;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.config.client.ConfigServerInstanceProvider;
import org.springframework.cloud.zookeeper.discovery.ZookeeperDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class ZookeeperConfigServerBootstrapperTests {

	private TestingServer testingServer;

	private ConfigurableApplicationContext context;

	private int port;

	@BeforeEach
	public void init() throws Exception {
		port = SocketUtils.findAvailableTcpPort();
		testingServer = new TestingServer(port);
	}

	@AfterEach
	public void after() throws Exception {
		if (context != null) {
			context.close();
		}
		if (testingServer != null) {
			testingServer.close();
		}
	}

	@Test
	public void notEnabledDoesNotAddInstanceProviderFn() {
		new SpringApplicationBuilder(TestConfig.class)
				.properties("--server.port=0", "spring.cloud.service-registry.auto-registration.enabled=false",
						"spring.cloud.zookeeper.connect-string=localhost:" + port)
				.addBootstrapper(registry -> registry.addCloseListener(event -> {
					ConfigServerInstanceProvider.Function providerFn = event.getBootstrapContext()
							.get(ConfigServerInstanceProvider.Function.class);
					assertThat(providerFn).as("ConfigServerInstanceProvider.Function was created when it shouldn't")
							.isNull();
				})).run().close();
	}

	@Test
	public void enabledAddsInstanceProviderFn() {
		AtomicReference<ZookeeperDiscoveryClient> bootstrapDiscoveryClient = new AtomicReference<>();
		context = new SpringApplicationBuilder(TestConfig.class)
				.properties("--server.port=0", "spring.cloud.config.discovery.enabled=true",
						"spring.cloud.zookeeper.connect-string=localhost:" + port,
						"spring.cloud.service-registry.auto-registration.enabled=false")
				.addBootstrapper(registry -> registry.addCloseListener(event -> {
					ConfigServerInstanceProvider.Function providerFn = event.getBootstrapContext()
							.get(ConfigServerInstanceProvider.Function.class);
					assertThat(providerFn).as("ConfigServerInstanceProvider.Function was not created when it should.")
							.isNotNull();
					bootstrapDiscoveryClient.set(event.getBootstrapContext().get(ZookeeperDiscoveryClient.class));
				})).run();

		ZookeeperDiscoveryClient discoveryClient = context.getBean(ZookeeperDiscoveryClient.class);
		assertThat(discoveryClient == bootstrapDiscoveryClient.get()).isTrue();
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class TestConfig {

	}

}
