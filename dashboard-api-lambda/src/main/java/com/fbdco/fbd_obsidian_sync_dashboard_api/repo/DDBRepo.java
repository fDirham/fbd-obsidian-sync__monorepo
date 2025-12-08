package com.fbdco.fbd_obsidian_sync_dashboard_api.repo;

import com.fbdco.fbd_obsidian_sync_dashboard_api.model.UserDDBItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

@Repository
public class DDBRepo {
    private final String tableName;
    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @SuppressWarnings("unused")
    public DDBRepo(
            DynamoDbClient dynamoDbClient,
            @Value("${dynamodb.table}") String dynamoDbTable
            ) {
        this.dynamoDbEnhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
        this.tableName = dynamoDbTable;
    }

    public UserDDBItem getUserById(String id) {
        DynamoDbTable<UserDDBItem> table = dynamoDbEnhancedClient.table(this.tableName,
                TableSchema.fromBean(UserDDBItem.class));

        return table.getItem(r -> r.key(k -> k.partitionValue(id)));
    }


    public void putUser(UserDDBItem user) throws DynamoDbException {
        DynamoDbTable<UserDDBItem> table = dynamoDbEnhancedClient.table(this.tableName,
                TableSchema.fromBean(UserDDBItem.class));
        table.putItem(user);
    }

    public void deleteUser(UserDDBItem user) throws DynamoDbException {
        DynamoDbTable<UserDDBItem> table = dynamoDbEnhancedClient.table(this.tableName,
                TableSchema.fromBean(UserDDBItem.class));
        table.deleteItem(user);
    }
}
