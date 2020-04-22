/*
 * Copyright DataStax, Inc.
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
package com.datastax.oss.driver.kubernetesTest.dseoperator;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfig;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.config.ProgrammaticDriverConfigLoaderBuilder;
import com.datastax.oss.driver.api.core.context.DriverContext;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.internal.core.config.typesafe.DefaultDriverConfigLoader;
import com.datastax.oss.driver.internal.core.config.typesafe.DefaultProgrammaticDriverConfigLoaderBuilder;
import com.datastax.oss.driver.internal.core.util.concurrent.CompletableFutures;
import com.typesafe.config.ConfigFactory;
import edu.umd.cs.findbugs.annotations.NonNull;

public class KubernetesTest
{
    private static final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    public static void main(String[] args)
    {

        ProgrammaticDriverConfigLoaderBuilder configLoaderBuilder = createDriverConfigLoader();
        configLoaderBuilder.withBoolean(DefaultDriverOption.RESOLVE_CONTACT_POINTS, true);

        CqlSession cqlSession = CqlSession.builder()
                .withConfigLoader(configLoaderBuilder.build())
                .addContactPoints(Collections.singletonList(
                InetSocketAddress.createUnresolved("cluster1-dc1-service.cass-operator", 9042))
        ).withLocalDatacenter("DC1-K8Demo").build();

        scheduler.scheduleAtFixedRate(() -> {
            System.out.println(cqlSession.getMetadata().getNodes());

            System.out.println("system.peers content:");
            ResultSet execute = cqlSession.execute(SimpleStatement.newInstance("select * from system.peers").setConsistencyLevel(ConsistencyLevel.QUORUM));
            System.out.println(execute.getExecutionInfo().getCoordinator().getEndPoint().resolve());
            System.out.println(execute.getExecutionInfo().getCoordinator().getBroadcastAddress());

            System.out.println(execute.all().stream().map(v -> v.getInetAddress("peer") + " " + v.getString("data_center") + " " + v.getUuid("host_id")).collect(Collectors.toList()));

            System.out.println("system.local content:");
            ResultSet execute1 = cqlSession.execute("select * from system.local");
            System.out.println(execute1.getExecutionInfo().getCoordinator().getEndPoint().resolve());
            System.out.println(execute1.getExecutionInfo().getCoordinator().getBroadcastAddress());
            System.out.println(execute1.all().stream().map(v -> v.getInetAddress("broadcast_address") + " " + v.getInetAddress("listen_address") + " " + v.getInetAddress("rpc_address") + " " + v.getUuid("host_id")).collect(Collectors.toList()));
        }, 0L, 500L,  TimeUnit.MILLISECONDS);

    }


    private static ProgrammaticDriverConfigLoaderBuilder createDriverConfigLoader()
    {
        return new DefaultProgrammaticDriverConfigLoaderBuilder(
                () -> {
                    ConfigFactory.invalidateCaches();
                    return ConfigFactory.defaultOverrides()
                            .withFallback(ConfigFactory.parseResources("application.conf"))
                            .withFallback(ConfigFactory.parseResources("application.json"))
                            .withFallback(ConfigFactory.defaultReference())
                            .resolve();
                },
                DefaultDriverConfigLoader.DEFAULT_ROOT_PATH)
        {
            @NonNull
            @Override
            public DriverConfigLoader build()
            {
                return new NonReloadableDriverConfigLoader(super.build());
            }
        };
    }

    private static class NonReloadableDriverConfigLoader implements DriverConfigLoader
    {

        private final DriverConfigLoader delegate;

        public NonReloadableDriverConfigLoader(DriverConfigLoader delegate)
        {
            this.delegate = delegate;
        }

        @NonNull
        @Override
        public DriverConfig getInitialConfig()
        {
            return delegate.getInitialConfig();
        }

        @Override
        public void onDriverInit(@NonNull DriverContext context)
        {
            delegate.onDriverInit(context);
        }

        @NonNull
        @Override
        public CompletionStage<Boolean> reload()
        {
            return CompletableFutures.failedFuture(
                    new UnsupportedOperationException("reload not supported"));
        }

        @Override
        public boolean supportsReloading()
        {
            return false;
        }

        @Override
        public void close()
        {
            delegate.close();
        }
    }
}
