/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pinot.segment.local.segment.index.loader.defaultcolumn;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.pinot.segment.local.segment.creator.SegmentTestUtils;
import org.apache.pinot.segment.local.segment.creator.impl.SegmentCreationDriverFactory;
import org.apache.pinot.segment.local.segment.index.SegmentMetadataImplTest;
import org.apache.pinot.segment.local.segment.index.loader.IndexLoadingConfig;
import org.apache.pinot.segment.local.segment.store.SegmentLocalFSDirectory;
import org.apache.pinot.segment.spi.creator.SegmentGeneratorConfig;
import org.apache.pinot.segment.spi.creator.SegmentIndexCreationDriver;
import org.apache.pinot.segment.spi.index.metadata.SegmentMetadataImpl;
import org.apache.pinot.segment.spi.store.SegmentDirectory;
import org.apache.pinot.spi.data.FieldSpec;
import org.apache.pinot.spi.data.Schema;
import org.apache.pinot.spi.utils.ReadMode;
import org.apache.pinot.util.TestUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class BaseDefaultColumnHandlerTest {
  private static final String AVRO_DATA = "data/test_data-mv.avro";

  private File _indexDir;
  private File _segmentDirectory;
  private SegmentMetadataImpl _committedSegmentMetadata;
  private SegmentDirectory.Writer _writer;

  @BeforeMethod
  public void setUp()
      throws Exception {
    _indexDir = Files.createTempDirectory(SegmentMetadataImplTest.class.getName() + "_segmentDir").toFile();

    final String filePath =
        TestUtils.getFileFromResourceUrl(SegmentMetadataImplTest.class.getClassLoader().getResource(AVRO_DATA));

    // intentionally changed this to TimeUnit.Hours to make it non-default for testing
    final SegmentGeneratorConfig config = SegmentTestUtils
        .getSegmentGenSpecWithSchemAndProjectedColumns(new File(filePath), _indexDir, "daysSinceEpoch", TimeUnit.HOURS,
            "testTable");
    config.setSegmentNamePostfix("1");
    config.setTimeColumnName("daysSinceEpoch");
    final SegmentIndexCreationDriver driver = SegmentCreationDriverFactory.get(null);
    driver.init(config);
    driver.build();
    _segmentDirectory = new File(_indexDir, driver.getSegmentName());
    _committedSegmentMetadata = new SegmentMetadataImpl(_segmentDirectory);
    _writer = new SegmentLocalFSDirectory(_indexDir, _committedSegmentMetadata, ReadMode.mmap).createWriter();
  }

  @AfterMethod
  public void tearDown() {
    FileUtils.deleteQuietly(_segmentDirectory);
  }

  @Test
  public void testComputeDefaultColumnActionMapForCommittedSegment() {
    // Dummy IndexLoadingConfig
    IndexLoadingConfig indexLoadingConfig = new IndexLoadingConfig();

    // Same schema
    Schema schema0 =
        new Schema.SchemaBuilder().setSchemaName("testTable").addSingleValueDimension("column1", FieldSpec.DataType.INT)
            .addSingleValueDimension("column2", FieldSpec.DataType.INT)
            .addSingleValueDimension("column3", FieldSpec.DataType.STRING)
            .addSingleValueDimension("column4", FieldSpec.DataType.STRING)
            .addSingleValueDimension("column5", FieldSpec.DataType.STRING)
            .addSingleValueDimension("column6", FieldSpec.DataType.INT)
            .addSingleValueDimension("column7", FieldSpec.DataType.INT)
            .addSingleValueDimension("column8", FieldSpec.DataType.INT)
            .addSingleValueDimension("column9", FieldSpec.DataType.INT)
            .addSingleValueDimension("column10", FieldSpec.DataType.INT)
            .addSingleValueDimension("column13", FieldSpec.DataType.INT)
            .addSingleValueDimension("count", FieldSpec.DataType.INT)
            .addSingleValueDimension("daysSinceEpoch", FieldSpec.DataType.INT)
            .addSingleValueDimension("weeksSinceEpochSunday", FieldSpec.DataType.INT).build();

    BaseDefaultColumnHandler defaultColumnHandler =
        new V3DefaultColumnHandler(_segmentDirectory, _committedSegmentMetadata, indexLoadingConfig, schema0, _writer);
    Assert.assertEquals(defaultColumnHandler.computeDefaultColumnActionMap(), Collections.EMPTY_MAP);

    // Add single-value dimension in the schema
    Schema schema1 =
        new Schema.SchemaBuilder().setSchemaName("testTable").addSingleValueDimension("column1", FieldSpec.DataType.INT)
            .addSingleValueDimension("column2", FieldSpec.DataType.INT)
            .addSingleValueDimension("column3", FieldSpec.DataType.STRING)
            .addSingleValueDimension("column4", FieldSpec.DataType.STRING)
            .addSingleValueDimension("column5", FieldSpec.DataType.STRING)
            .addSingleValueDimension("column6", FieldSpec.DataType.INT)
            .addSingleValueDimension("column7", FieldSpec.DataType.INT)
            .addSingleValueDimension("column8", FieldSpec.DataType.INT)
            .addSingleValueDimension("column9", FieldSpec.DataType.INT)
            .addSingleValueDimension("column10", FieldSpec.DataType.INT)
            .addSingleValueDimension("column11", FieldSpec.DataType.INT) // add column11
            .addSingleValueDimension("column13", FieldSpec.DataType.INT)
            .addSingleValueDimension("count", FieldSpec.DataType.INT)
            .addSingleValueDimension("daysSinceEpoch", FieldSpec.DataType.INT)
            .addSingleValueDimension("weeksSinceEpochSunday", FieldSpec.DataType.INT).build();
    defaultColumnHandler =
        new V3DefaultColumnHandler(_segmentDirectory, _committedSegmentMetadata, indexLoadingConfig, schema1, _writer);
    Assert.assertEquals(defaultColumnHandler.computeDefaultColumnActionMap(),
        ImmutableMap.of("column11", BaseDefaultColumnHandler.DefaultColumnAction.ADD_DIMENSION));

    // Add multi-value dimension in the schema
    Schema schema2 =
        new Schema.SchemaBuilder().setSchemaName("testTable").addSingleValueDimension("column1", FieldSpec.DataType.INT)
            .addSingleValueDimension("column2", FieldSpec.DataType.INT)
            .addSingleValueDimension("column3", FieldSpec.DataType.STRING)
            .addSingleValueDimension("column4", FieldSpec.DataType.STRING)
            .addSingleValueDimension("column5", FieldSpec.DataType.STRING)
            .addSingleValueDimension("column6", FieldSpec.DataType.INT)
            .addSingleValueDimension("column7", FieldSpec.DataType.INT)
            .addSingleValueDimension("column8", FieldSpec.DataType.INT)
            .addSingleValueDimension("column9", FieldSpec.DataType.INT)
            .addSingleValueDimension("column10", FieldSpec.DataType.INT)
            .addMultiValueDimension("column11", FieldSpec.DataType.INT) // add column11
            .addSingleValueDimension("column13", FieldSpec.DataType.INT)
            .addSingleValueDimension("count", FieldSpec.DataType.INT)
            .addSingleValueDimension("daysSinceEpoch", FieldSpec.DataType.INT)
            .addSingleValueDimension("weeksSinceEpochSunday", FieldSpec.DataType.INT).build();
    defaultColumnHandler =
        new V3DefaultColumnHandler(_segmentDirectory, _committedSegmentMetadata, indexLoadingConfig, schema2, _writer);
    Assert.assertEquals(defaultColumnHandler.computeDefaultColumnActionMap(),
        ImmutableMap.of("column11", BaseDefaultColumnHandler.DefaultColumnAction.ADD_DIMENSION));

    // Add metric in the schema
    Schema schema3 =
        new Schema.SchemaBuilder().setSchemaName("testTable").addSingleValueDimension("column1", FieldSpec.DataType.INT)
            .addSingleValueDimension("column2", FieldSpec.DataType.INT)
            .addSingleValueDimension("column3", FieldSpec.DataType.STRING)
            .addSingleValueDimension("column4", FieldSpec.DataType.STRING)
            .addSingleValueDimension("column5", FieldSpec.DataType.STRING)
            .addSingleValueDimension("column6", FieldSpec.DataType.INT)
            .addSingleValueDimension("column7", FieldSpec.DataType.INT)
            .addSingleValueDimension("column8", FieldSpec.DataType.INT)
            .addSingleValueDimension("column9", FieldSpec.DataType.INT)
            .addSingleValueDimension("column10", FieldSpec.DataType.INT)
            .addSingleValueDimension("column13", FieldSpec.DataType.INT)
            .addSingleValueDimension("count", FieldSpec.DataType.INT)
            .addSingleValueDimension("daysSinceEpoch", FieldSpec.DataType.INT)
            .addSingleValueDimension("weeksSinceEpochSunday", FieldSpec.DataType.INT)
            .addMetric("column11", FieldSpec.DataType.INT).build(); // add column11
    defaultColumnHandler =
        new V3DefaultColumnHandler(_segmentDirectory, _committedSegmentMetadata, indexLoadingConfig, schema3, _writer);
    Assert.assertEquals(defaultColumnHandler.computeDefaultColumnActionMap(),
        ImmutableMap.of("column11", BaseDefaultColumnHandler.DefaultColumnAction.ADD_METRIC));

    // Add metric in the schema
    Schema schema4 =
        new Schema.SchemaBuilder().setSchemaName("testTable").addSingleValueDimension("column1", FieldSpec.DataType.INT)
            .addSingleValueDimension("column2", FieldSpec.DataType.INT)
            .addSingleValueDimension("column3", FieldSpec.DataType.STRING)
            .addSingleValueDimension("column4", FieldSpec.DataType.STRING)
            .addSingleValueDimension("column5", FieldSpec.DataType.STRING)
            .addSingleValueDimension("column6", FieldSpec.DataType.INT)
            .addSingleValueDimension("column7", FieldSpec.DataType.INT)
            .addSingleValueDimension("column8", FieldSpec.DataType.INT)
            .addSingleValueDimension("column9", FieldSpec.DataType.INT)
            .addSingleValueDimension("column10", FieldSpec.DataType.INT)
            .addSingleValueDimension("column13", FieldSpec.DataType.INT)
            .addSingleValueDimension("count", FieldSpec.DataType.INT)
            .addSingleValueDimension("daysSinceEpoch", FieldSpec.DataType.INT)
            .addSingleValueDimension("weeksSinceEpochSunday", FieldSpec.DataType.INT)
            .addDateTime("column11", FieldSpec.DataType.INT, "1:HOURS:EPOCH", "1:HOURS").build(); // add column11
    defaultColumnHandler =
        new V3DefaultColumnHandler(_segmentDirectory, _committedSegmentMetadata, indexLoadingConfig, schema4, _writer);
    Assert.assertEquals(defaultColumnHandler.computeDefaultColumnActionMap(),
        ImmutableMap.of("column11", BaseDefaultColumnHandler.DefaultColumnAction.ADD_DATE_TIME));

    // Do not remove non-autogenerated column in the segmentMetadata
    Schema schema5 =
        new Schema.SchemaBuilder().setSchemaName("testTable").addSingleValueDimension("column1", FieldSpec.DataType.INT)
            .addSingleValueDimension("column3", FieldSpec.DataType.STRING) // remove column2
            .addSingleValueDimension("column4", FieldSpec.DataType.STRING)
            .addSingleValueDimension("column5", FieldSpec.DataType.STRING)
            .addSingleValueDimension("column6", FieldSpec.DataType.INT)
            .addSingleValueDimension("column7", FieldSpec.DataType.INT)
            .addSingleValueDimension("column8", FieldSpec.DataType.INT)
            .addSingleValueDimension("column9", FieldSpec.DataType.INT)
            .addSingleValueDimension("column10", FieldSpec.DataType.INT)
            .addSingleValueDimension("column13", FieldSpec.DataType.INT)
            .addSingleValueDimension("count", FieldSpec.DataType.INT)
            .addSingleValueDimension("daysSinceEpoch", FieldSpec.DataType.INT)
            .addSingleValueDimension("weeksSinceEpochSunday", FieldSpec.DataType.INT).build();
    defaultColumnHandler =
        new V3DefaultColumnHandler(_segmentDirectory, _committedSegmentMetadata, indexLoadingConfig, schema5, _writer);
    Assert.assertEquals(defaultColumnHandler.computeDefaultColumnActionMap(), Collections.EMPTY_MAP);

    // Do not update non-autogenerated column in the schema
    Schema schema6 =
        new Schema.SchemaBuilder().setSchemaName("testTable").addSingleValueDimension("column1", FieldSpec.DataType.INT)
            .addSingleValueDimension("column2", FieldSpec.DataType.STRING) // update datatype
            .addSingleValueDimension("column3", FieldSpec.DataType.STRING)
            .addSingleValueDimension("column4", FieldSpec.DataType.STRING)
            .addSingleValueDimension("column5", FieldSpec.DataType.STRING)
            .addSingleValueDimension("column6", FieldSpec.DataType.INT)
            .addSingleValueDimension("column7", FieldSpec.DataType.INT)
            .addSingleValueDimension("column8", FieldSpec.DataType.INT)
            .addSingleValueDimension("column9", FieldSpec.DataType.INT)
            .addSingleValueDimension("column10", FieldSpec.DataType.INT)
            .addSingleValueDimension("column13", FieldSpec.DataType.INT)
            .addSingleValueDimension("count", FieldSpec.DataType.INT)
            .addSingleValueDimension("daysSinceEpoch", FieldSpec.DataType.INT)
            .addSingleValueDimension("weeksSinceEpochSunday", FieldSpec.DataType.INT).build();
    defaultColumnHandler =
        new V3DefaultColumnHandler(_segmentDirectory, _committedSegmentMetadata, indexLoadingConfig, schema6, _writer);
    Assert.assertEquals(defaultColumnHandler.computeDefaultColumnActionMap(), Collections.EMPTY_MAP);
  }
}
