package com.company.ErrorHandlingToolRegistry;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import static com.company.ErrorHandlingToolRegistry.ErrorHandlingToolRegistryApplication.*;


import java.util.*;

@Service
public class ErrorHandlingRegistryService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void setInstance(Instance instance, String name){
        if(!(Instance.validateInstanceName(name) && instance.getName().equals(name))){
            return;
        }
        String getInstanceSql = String.format("UPDATE ECP_Handling_Tool_Status.dbo.Tool_Status SET %s = '%s' WHERE Server_Name = 8958;",instance.getName(),instance.getStatus());
        jdbcTemplate.execute(getInstanceSql);
    }

    public Instance getInstance(String name){
        if(!Instance.validateInstanceName(name)){
            return new Instance(null,null);
        }
        String getInstanceQuerySql = String.format("SELECT %s FROM ECP_Handling_Tool_Status.dbo.Tool_Status WHERE Server_Name = 8958",name);
        Map<String, Object> row = jdbcTemplate.queryForMap(getInstanceQuerySql);
        return new Instance(name,row.get(name).toString());
    }

    public List<Instance> getAllInstances(){
        List<Instance> instances = new ArrayList<>();
        instances.add(getInstance("SCB"));
        instances.add(getInstance("SCC"));
        instances.add(getInstance("SCE"));
        return instances;
    }
}
