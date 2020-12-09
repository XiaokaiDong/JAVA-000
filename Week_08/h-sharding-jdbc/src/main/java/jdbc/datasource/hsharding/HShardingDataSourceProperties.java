package jdbc.datasource.hsharding;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
@EqualsAndHashCode
@NoArgsConstructor
public class HShardingDataSourceProperties extends DataSourceProperties implements Cloneable {
    private List<String> urls;
    private List<String> userNames;
    private List<String> passwords;

    private int numOfDatabases;
    private int numOfTablesPerDatabase;

    private String shardingAlgorithm;
    private String shardingKey;
    private String tableName;

    private List<DataSourceProperties> dataSourcePropertiesList = new CopyOnWriteArrayList<>();

    /**
     * 产生多个普通的DataSourceProperties
     */
    @PostConstruct
    void initAllDataSourceProperties() throws CloneNotSupportedException {

        for (int i = 0; i < urls.size(); i++) {
            DataSourceProperties dataSourceProperties = (DataSourceProperties)this.clone();
            dataSourceProperties.setUrl(urls.get(i));
            dataSourceProperties.setUsername(userNames.get(i));
            dataSourceProperties.setPassword(passwords.get(i));
            dataSourcePropertiesList.add(dataSourceProperties);
        }

    }


}
