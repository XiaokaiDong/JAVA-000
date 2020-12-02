package jdbc.datasource;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

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
