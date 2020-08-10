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
package com.datastax.driver.core;

import static com.datastax.driver.core.Assertions.assertThat;
import static org.testng.Assert.fail;

import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.datastax.driver.core.schemabuilder.SchemaBuilder;
import com.datastax.driver.core.schemabuilder.TableOptions;
import com.datastax.driver.core.utils.CassandraVersion;
import org.testng.SkipException;
import org.testng.annotations.Test;

@CassandraVersion(
    value = "4.0.0-alpha1",
    description = "Additional Write Policy is for Cassandra 4.0+")
public class AdditionalWritePolicyTest extends CCMTestsSupport {

  private void cleanup(String tableName) {
    session().execute(String.format("DROP TABLE IF EXISTS %s", tableName));
  }

  @Test(groups = "short")
  public void should_create_table_with_additional_write_policy_default() {
    String test_table = "awp_default";
    session()
        .execute(
            SchemaBuilder.createTable(test_table)
                .addPartitionKey("pk", DataType.text())
                .addColumn("data", DataType.text()));
    assertThat(
            cluster()
                .getMetadata()
                .getKeyspace(keyspace)
                .getTable(test_table)
                .getOptions()
                .getAdditionalWritePolicy())
        .matches("99p|99PERCENTILE");
    cleanup(test_table);
  }

  @Test(groups = "short")
  public void should_create_table_with_additonal_write_policy_percentile() {
    String test_table = "awp_percentile";
    session()
        .execute(
            SchemaBuilder.createTable(test_table)
                .addPartitionKey("pk", DataType.text())
                .addColumn("data", DataType.text())
                .withOptions()
                .additionalWritePolicy(SchemaBuilder.additionalWritePolicyPercentile(44)));
    assertThat(
            cluster()
                .getMetadata()
                .getKeyspace(keyspace)
                .getTable(test_table)
                .getOptions()
                .getAdditionalWritePolicy())
        .matches("44p|44PERCENTILE");
    cleanup(test_table);
  }

  @Test(groups = "short")
  public void should_create_table_with_additonal_write_policy_millisecs() {
    String test_table = "awp_millisecs";
    session()
        .execute(
            SchemaBuilder.createTable(test_table)
                .addPartitionKey("pk", DataType.text())
                .addColumn("data", DataType.text())
                .withOptions()
                .additionalWritePolicy(SchemaBuilder.additionalWritePolicyMillisecs(350)));
    assertThat(
            cluster()
                .getMetadata()
                .getKeyspace(keyspace)
                .getTable(test_table)
                .getOptions()
                .getAdditionalWritePolicy())
        .matches("350(\\.0)?ms");
    cleanup(test_table);
  }

  @Test(groups = "short")
  public void should_create_table_with_additonal_write_policy_never() {
    VersionNumber dseVersion = ccm().getDSEVersion();
    if (dseVersion != null && dseVersion.compareTo(VersionNumber.parse("6.8")) >= 0) {
      throw new SkipException("Value NEVER is illegal in DSE 6.8");
    }
    String test_table = "awp_never";
    session()
        .execute(
            SchemaBuilder.createTable(test_table)
                .addPartitionKey("pk", DataType.text())
                .addColumn("data", DataType.text())
                .withOptions()
                .additionalWritePolicy(SchemaBuilder.additionalWritePolicyNever()));
    assertThat(
            cluster()
                .getMetadata()
                .getKeyspace(keyspace)
                .getTable(test_table)
                .getOptions()
                .getAdditionalWritePolicy())
        .isEqualTo("NEVER");
    cleanup(test_table);
  }

  @Test(groups = "short")
  public void should_create_table_with_additonal_write_policy_always() {
    String test_table = "awp_always";
    session()
        .execute(
            SchemaBuilder.createTable(test_table)
                .addPartitionKey("pk", DataType.text())
                .addColumn("data", DataType.text())
                .withOptions()
                .additionalWritePolicy(SchemaBuilder.additionalWritePolicyAlways()));
    assertThat(
            cluster()
                .getMetadata()
                .getKeyspace(keyspace)
                .getTable(test_table)
                .getOptions()
                .getAdditionalWritePolicy())
        .isEqualTo("ALWAYS");
    cleanup(test_table);
  }

  @Test(groups = "short")
  public void should_fail_to_create_table_with_invalid_additonal_write_policy() {
    String test_table = "awp_invalid";
    try {
      session()
          .execute(
              SchemaBuilder.createTable(test_table)
                  .addPartitionKey("pk", DataType.text())
                  .addColumn("data", DataType.text())
                  .withOptions()
                  .additionalWritePolicy(new TableOptions.AdditionalWritePolicyValue("'ALL'")));
      fail("Should not be able to create table with invlaid 'additional_write_policy': 'ALL'");
    } catch (InvalidQueryException iqe) {
      VersionNumber dseVersion = ccm().getDSEVersion();
      if (dseVersion != null && dseVersion.compareTo(VersionNumber.parse("6.8")) >= 0) {
        assertThat(iqe).hasMessageContaining("value ALL is not legal");
      } else {
        assertThat(iqe)
            .hasMessageContaining("Invalid value")
            .hasMessageContaining("ALL")
            .hasMessageContaining("for option");
      }
    } finally {
      cleanup(test_table);
    }
  }
}
