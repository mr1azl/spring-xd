/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.server;

import javax.sql.DataSource;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.health.VanillaHealthIndicator;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.xd.dirt.post.DelegatingHandlerMapping;
import org.springframework.xd.dirt.util.ConfigLocations;

/**
 * Beans defined and imported here are in the global parent context, hence available to the entire hierarchy, including
 * Admins, Containers, and Modules.
 *
 * @author David Turanski
 * @author Mark Fisher
 */
@EnableAutoConfiguration(exclude = { ServerPropertiesAutoConfiguration.class, BatchAutoConfiguration.class,
		JmxAutoConfiguration.class })
@ImportResource("classpath:" + ConfigLocations.XD_CONFIG_ROOT + "batch/batch.xml")
@EnableBatchProcessing
public class ParentConfiguration {

	@Bean
	public DelegatingHandlerMapping delegatingHandlerMapping() {
		return new DelegatingHandlerMapping();
	}

	@Bean
	public MBeanServerFactoryBean mbeanServer() {
		MBeanServerFactoryBean factoryBean = new MBeanServerFactoryBean();
		factoryBean.setLocateExistingServerIfPossible(true);
		return factoryBean;
	}

	@Configuration
	@Profile("cloud")
	protected static class CloudFoundryConfiguration {

		@Bean
		public DataSource dataSource() {
			Cloud cloud = cloud();
			return cloud.getServiceConnector("mysql", DataSource.class, null);
		}

		@Bean
		@Profile("rabbit")
		public ConnectionFactory rabbitConnectionFactory() {
			Cloud cloud = cloud();
			return cloud.getServiceConnector("rabbit", ConnectionFactory.class, null);
		}

		@Bean
		protected Cloud cloud() {
			CloudFactory cloudFactory = new CloudFactory();
			Cloud cloud = cloudFactory.getCloud();
			return cloud;
		}
	}

	@Bean
	@ConditionalOnExpression("${endpoints.health.enabled:true}")
	public HealthEndpoint<Object> healthEndpoint() {
		return new HealthEndpoint<Object>(new VanillaHealthIndicator());
	}

	@ConditionalOnExpression("${XD_JMX_ENABLED:true}")
	@EnableMBeanExport(defaultDomain = "xd.parent")
	protected static class JmxConfiguration {

		@Bean(name = "XDParentMBean")
		public IntegrationMBeanExporter integrationMBeanExporter() {
			IntegrationMBeanExporter exporter = new IntegrationMBeanExporter();
			exporter.setDefaultDomain("xd.parent");
			return exporter;
		}
	}
}
