package com.amazonaws.services.timestream;

import software.amazon.awssdk.services.timestreamwrite.TimestreamWriteClient;
import software.amazon.awssdk.services.timestreamwrite.model.*;
import software.amazon.awssdk.services.timestreamwrite.paginators.ListDatabasesIterable;
import software.amazon.awssdk.services.timestreamwrite.paginators.ListTablesIterable;

import java.util.ArrayList;
import java.util.List;

import static com.amazonaws.services.timestream.Main.DATABASE_NAME;
import static com.amazonaws.services.timestream.Main.TABLE_NAME;

public class CrudAndSimpleIngestionExample {
    public static final long HT_TTL_HOURS = 24L;
    public static final long CT_TTL_DAYS = 7L;

    TimestreamWriteClient timestreamWriteClient;

    public CrudAndSimpleIngestionExample(TimestreamWriteClient client) {
        this.timestreamWriteClient = client;
    }

    public void createDatabase() {
        System.out.println("Creating database");
        CreateDatabaseRequest request = CreateDatabaseRequest.builder().databaseName(DATABASE_NAME).build();
        try {
            timestreamWriteClient.createDatabase(request);
            System.out.println("Database [" + DATABASE_NAME + "] created successfully");
        } catch (ConflictException e) {
            System.out.println("Database [" + DATABASE_NAME + "] exists. Skipping database creation");
        }
    }

    public void describeDatabase() {
        System.out.println("Describing database");
        final DescribeDatabaseRequest describeDatabaseRequest = DescribeDatabaseRequest.builder()
                .databaseName(DATABASE_NAME).build();
        try {
            DescribeDatabaseResponse response = timestreamWriteClient.describeDatabase(describeDatabaseRequest);
            final Database databaseRecord = response.database();
            final String databaseId = databaseRecord.arn();
            System.out.println("Database " + DATABASE_NAME + " has id " + databaseId);
        } catch (final Exception e) {
            System.out.println("Database doesn't exist = " + e);
            throw e;
        }
    }

    public void listDatabases() {
        System.out.println("Listing databases");
        ListDatabasesRequest request = ListDatabasesRequest.builder().maxResults(2).build();
        ListDatabasesIterable listDatabasesIterable = timestreamWriteClient.listDatabasesPaginator(request);
        for(ListDatabasesResponse listDatabasesResponse : listDatabasesIterable) {
            final List<Database> databases = listDatabasesResponse.databases();
            databases.forEach(database -> System.out.println(database.databaseName()));
        }
    }

    public void updateDatabase(String kmsKeyId) {

        if (kmsKeyId == null) {
            System.out.println("Skipping UpdateDatabase because KmsKeyId was not given");
            return;
        }

        System.out.println("Updating database");

        UpdateDatabaseRequest request = UpdateDatabaseRequest.builder()
                .databaseName(DATABASE_NAME)
                .kmsKeyId(kmsKeyId)
                .build();
        try {
            timestreamWriteClient.updateDatabase(request);
            System.out.println("Database [" + DATABASE_NAME + "] updated successfully with kmsKeyId " + kmsKeyId);
        } catch (ResourceNotFoundException e) {
            System.out.println("Database [" + DATABASE_NAME + "] does not exist. Skipping UpdateDatabase");
        } catch (Exception e) {
            System.out.println("UpdateDatabase failed: " + e);
        }
    }

    public void createTable() {
        System.out.println("Creating table");

        final RetentionProperties retentionProperties = RetentionProperties.builder()
                .memoryStoreRetentionPeriodInHours(HT_TTL_HOURS)
                .magneticStoreRetentionPeriodInDays(CT_TTL_DAYS).build();
        final CreateTableRequest createTableRequest = CreateTableRequest.builder()
                .databaseName(DATABASE_NAME).tableName(TABLE_NAME).retentionProperties(retentionProperties).build();

        try {
            timestreamWriteClient.createTable(createTableRequest);
            System.out.println("Table [" + TABLE_NAME + "] successfully created.");
        } catch (ConflictException e) {
            System.out.println("Table [" + TABLE_NAME + "] exists on database [" + DATABASE_NAME + "] . Skipping database creation");
        }
    }

    public void updateTable() {
        System.out.println("Updating table");

        final RetentionProperties retentionProperties = RetentionProperties.builder()
                .memoryStoreRetentionPeriodInHours(HT_TTL_HOURS)
                .magneticStoreRetentionPeriodInDays(CT_TTL_DAYS).build();
        final UpdateTableRequest updateTableRequest = UpdateTableRequest.builder()
                .databaseName(DATABASE_NAME).tableName(TABLE_NAME).retentionProperties(retentionProperties).build();

        timestreamWriteClient.updateTable(updateTableRequest);
        System.out.println("Table updated");
    }

    public void describeTable() {
        System.out.println("Describing table");
        final DescribeTableRequest describeTableRequest = DescribeTableRequest.builder()
                .databaseName(DATABASE_NAME).tableName(TABLE_NAME).build();
        try {
            DescribeTableResponse response = timestreamWriteClient.describeTable(describeTableRequest);
            String tableId = response.table().arn();
            System.out.println("Table " + TABLE_NAME + " has id " + tableId);
        } catch (final Exception e) {
            System.out.println("Table " + TABLE_NAME + " doesn't exist = " + e);
            throw e;
        }
    }

    public void listTables() {
        System.out.println("Listing tables");
        ListTablesRequest request = ListTablesRequest.builder().databaseName(DATABASE_NAME).maxResults(2).build();
        ListTablesIterable listTablesIterable = timestreamWriteClient.listTablesPaginator(request);
        for(ListTablesResponse listTablesResponse : listTablesIterable) {
            final List<Table> tables = listTablesResponse.tables();
            tables.forEach(table -> System.out.println(table.tableName()));
        }
    }

    public void writeRecords() {
        System.out.println("Writing records");
        // Specify repeated values for all records
        List<Record> records = new ArrayList<>();
        final long time = System.currentTimeMillis();

        List<Dimension> dimensions = new ArrayList<>();
        final Dimension region = Dimension.builder().name("region").value("us-east-1").build();
        final Dimension az = Dimension.builder().name("az").value("az1").build();
        final Dimension hostname = Dimension.builder().name("hostname").value("host1").build();

        dimensions.add(region);
        dimensions.add(az);
        dimensions.add(hostname);

        Record cpuUtilization = Record.builder()
                .dimensions(dimensions)
                .measureValueType(MeasureValueType.DOUBLE)
                .measureName("cpu_utilization")
                .measureValue("13.5")
                .time(String.valueOf(time)).build();

        Record memoryUtilization = Record.builder()
                .dimensions(dimensions)
                .measureValueType(MeasureValueType.DOUBLE)
                .measureName("memory_utilization")
                .measureValue("40")
                .time(String.valueOf(time)).build();

        records.add(cpuUtilization);
        records.add(memoryUtilization);

        WriteRecordsRequest writeRecordsRequest = WriteRecordsRequest.builder()
                .databaseName(DATABASE_NAME).tableName(TABLE_NAME).records(records).build();

        try {
            WriteRecordsResponse writeRecordsResponse = timestreamWriteClient.writeRecords(writeRecordsRequest);
            System.out.println("WriteRecords Status: " + writeRecordsResponse.sdkHttpResponse().statusCode());
        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
    }

    public void writeRecordsWithCommonAttributes() {
        System.out.println("Writing records with extracting common attributes");
        // Specify repeated values for all records
        List<Record> records = new ArrayList<>();
        final long time = System.currentTimeMillis();

        List<Dimension> dimensions = new ArrayList<>();
        final Dimension region = Dimension.builder().name("region").value("us-east-1").build();
        final Dimension az = Dimension.builder().name("az").value("az1").build();
        final Dimension hostname = Dimension.builder().name("hostname").value("host1").build();

        dimensions.add(region);
        dimensions.add(az);
        dimensions.add(hostname);

        Record commonAttributes = Record.builder()
                .dimensions(dimensions)
                .measureValueType(MeasureValueType.DOUBLE)
                .time(String.valueOf(time)).build();

        Record cpuUtilization = Record.builder()
                .measureName("cpu_utilization")
                .measureValue("13.5").build();
        Record memoryUtilization = Record.builder()
                .measureName("memory_utilization")
                .measureValue("40").build();

        records.add(cpuUtilization);
        records.add(memoryUtilization);

        WriteRecordsRequest writeRecordsRequest = WriteRecordsRequest.builder()
                .databaseName(DATABASE_NAME)
                .tableName(TABLE_NAME)
                .commonAttributes(commonAttributes)
                .records(records).build();

        try {
            WriteRecordsResponse writeRecordsResponse = timestreamWriteClient.writeRecords(writeRecordsRequest);
            System.out.println("writeRecordsWithCommonAttributes Status: " + writeRecordsResponse.sdkHttpResponse().statusCode());
        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
    }

    public void deleteTable() {
        System.out.println("Deleting table");
        final DeleteTableRequest deleteTableRequest = DeleteTableRequest.builder()
                .databaseName(DATABASE_NAME).tableName(TABLE_NAME).build();
        try {
            DeleteTableResponse response =
                    timestreamWriteClient.deleteTable(deleteTableRequest);
            System.out.println("Delete table status: " + response.sdkHttpResponse().statusCode());
        } catch (final ResourceNotFoundException e) {
            System.out.println("Table " + TABLE_NAME + " doesn't exist = " + e);
            throw e;
        } catch (final Exception e) {
            System.out.println("Could not delete table " + TABLE_NAME + " = " + e);
            throw e;
        }
    }

    public void deleteDatabase() {
        System.out.println("Deleting database");
        final DeleteDatabaseRequest deleteDatabaseRequest = DeleteDatabaseRequest.builder().databaseName(DATABASE_NAME).build();
        try {
            DeleteDatabaseResponse response =
                    timestreamWriteClient.deleteDatabase(deleteDatabaseRequest);
            System.out.println("Delete database status: " + response.sdkHttpResponse().statusCode());
        } catch (final ResourceNotFoundException e) {
            System.out.println("Database " + DATABASE_NAME + " doesn't exist = " + e);
            throw e;
        } catch (final Exception e) {
            System.out.println("Could not delete Database " + DATABASE_NAME + " = " + e);
            throw e;
        }
    }
}
