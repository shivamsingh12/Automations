package com.company.ErrorHandlingToolRegistry;

import java.util.List;
import static com.company.ErrorHandlingToolRegistry.ErrorHandlingToolRegistryApplication.*;


public class Instance {
    private String name;
    private String status;
    private static List<String> validInstancesName = List.of("SCB","SCC","SCE");

    public Instance(String name, String status) {
        this.name = name;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public static boolean validateInstanceName(String instanceName){
        if (validInstancesName.contains(instanceName)) return true;
        else return false;
    }

    @Override
    public String toString() {
        return "Instance{" +
                "name='" + name + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
