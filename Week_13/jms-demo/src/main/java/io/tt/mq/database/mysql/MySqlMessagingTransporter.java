package io.tt.mq.database.mysql;

import io.tt.mq.database.transporter.DatabaseMessagingTransporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import javax.sql.DataSource;
import java.sql.Connection;

public class MySqlMessagingTransporter extends DatabaseMessagingTransporter {
    public MySqlMessagingTransporter(String urlBroker) {
        super(urlBroker);
    }

    @Autowired
    DataSource dataSource;

    @Override
    public void initConnectionFactory() {
        super.connectionFactory = dataSource;
    }


}
