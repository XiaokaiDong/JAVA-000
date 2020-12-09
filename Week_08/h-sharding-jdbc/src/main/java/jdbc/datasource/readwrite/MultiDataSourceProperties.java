package jdbc.datasource.readwrite;

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
public class MultiDataSourceProperties extends DataSourceProperties implements Cloneable {
    private List<String> urls;

    private boolean readOnly = false;


    private List<DataSourceProperties> dataSourcePropertiesList = new CopyOnWriteArrayList<>();

    /**
     * 产生多个普通的DataSourceProperties
     */
    @PostConstruct
    void initAllDataSourceProperties() throws CloneNotSupportedException {
        for (String url: urls) {
            DataSourceProperties dataSourceProperties = (DataSourceProperties)this.clone();
            dataSourceProperties.setUrl(url);
            dataSourcePropertiesList.add(dataSourceProperties);

        }

    }


}
