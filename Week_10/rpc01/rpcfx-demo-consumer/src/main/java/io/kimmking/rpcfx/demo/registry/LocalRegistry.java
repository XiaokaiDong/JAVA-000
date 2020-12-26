package io.kimmking.rpcfx.demo.registry;

import io.kimmking.rpcfx.registry.RpcfxRegistryCenter;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class LocalRegistry implements RpcfxRegistryCenter {
    @Autowired
    private LocalRegistryConfiguration registryConfiguration;

    private Map<String, Integer> currentIndex = new ConcurrentHashMap<>();
    private Map<String, Integer> groupSize = new ConcurrentHashMap<>();

    @PostConstruct
    public void initIndexArray(){
        Set<String> groupNames = registryConfiguration.getGroupName();
        for (String groupName : groupNames) {
            currentIndex.putIfAbsent(groupName, 0);
            groupSize.putIfAbsent(groupName, registryConfiguration.getGroup(groupName).size());
        }
    }

    @Override
    public String getUrl(String group, String version) {
        String dstUrl = null;
        List<String> groupUrl = registryConfiguration.getGroup(group);
        if (groupUrl != null) {
            dstUrl = groupUrl.get(ThreadLocalRandom.current().nextInt() % groupSize.get(group));
        }
        return dstUrl + version + "/";
    }
}
