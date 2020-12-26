package io.kimmking.rpcfx.demo.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class LocalRegistryConfiguration {
    private Map<String, List<String>> localRegistry = new ConcurrentHashMap<>();

    /**
     * 默认构造函数，这里作为DEMO，写死一个"注册表"
     */
    public LocalRegistryConfiguration() {
        List<String> redGroup = new CopyOnWriteArrayList<>();
        redGroup.add("http://localhost:8081/red/");
        redGroup.add("http://localhost:8082/red/");
        localRegistry.putIfAbsent("red", redGroup);

        List<String> blueGroup = new CopyOnWriteArrayList<>();
        blueGroup.add("http://localhost:8083/blue/");
        blueGroup.add("http://localhost:8084/blue/");
        localRegistry.putIfAbsent("blue", blueGroup);
    }

    public List<String> getGroup(String groupName) {
        return localRegistry.get(groupName);
    }

    public Set<String> getGroupName() {
        return localRegistry.keySet();
    }
}
