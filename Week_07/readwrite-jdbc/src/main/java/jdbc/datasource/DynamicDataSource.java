package jdbc.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import java.util.Map;

public class DynamicDataSource extends AbstractRoutingDataSource {



    /**
     * sql session与线程挂钩
     */
    private static final ThreadLocal<Boolean> MULTI_DATA_SOURCES_THREAD_LOCAL = new ThreadLocal<>();

    static {
        MULTI_DATA_SOURCES_THREAD_LOCAL.set(false);
    }

    /**
     * @param defaultMultiDataSources 默认数据源
     * @param targetDataSources       目标数据源
     */
    public DynamicDataSource(MultiDataSources defaultMultiDataSources, Map<Object, Object> targetDataSources){
        super.setDefaultTargetDataSource(defaultMultiDataSources);
        super.setTargetDataSources(targetDataSources);
        super.afterPropertiesSet();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return getDatasourceId();
    }

    public static void setReadOnly(boolean readonly) {
        MULTI_DATA_SOURCES_THREAD_LOCAL.set(readonly);
    }

    public static Boolean getDatasourceId() {
        return MULTI_DATA_SOURCES_THREAD_LOCAL.get();
    }

}
