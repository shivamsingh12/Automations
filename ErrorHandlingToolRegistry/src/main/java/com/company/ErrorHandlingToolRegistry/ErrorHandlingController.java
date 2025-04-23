package com.company.ErrorHandlingToolRegistry;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import static com.company.ErrorHandlingToolRegistry.ErrorHandlingToolRegistryApplication.*;
import java.util.Collection;
import java.util.List;

@RestController
public class ErrorHandlingController {

    @Autowired
    private ErrorHandlingRegistryService errorHandlingRegistryService;

    @PutMapping("/instance/{name}")
    public void setInstance(@RequestBody Instance instance, @PathVariable String name){
        errorHandlingRegistryService.setInstance(instance, name);
    }

    @GetMapping("/instance/{name}")
    public Instance getInstance(@PathVariable("name") String name){
        return errorHandlingRegistryService.getInstance(name);
    }

    @CrossOrigin
    @GetMapping("/instances")
    public List<Instance> getAllInstance(){
        return errorHandlingRegistryService.getAllInstances();
    }

    @GetMapping("/instancesUI")
    public String getAllInstanceUI(){
        return "<html>\n" +
                "\t<head> <style>\n" +
                "      table,\n" +
                "      th,\n" +
                "      td {\n" +
                "        padding: 30px;\n" +
                "        border: 1px solid black;\n" +
                "        border-collapse: collapse;\n" +
                "      }\n" +
                "    </style>"+
                "<script>\n" +
                "\n" +
                "\t\t</script>\n" +
                "\t</head>\n" +
                "\t<body onLoad = 'loadFunction()'> \n" +
                "\t\t\t<div id = \"valueHolder\"></div>\n" +
                "\t\t\t<script>\n" +
                "\t\t\tlet loadFunction = ()=>{\n" +
                "\t\t\t\t\tlet body = document.getElementsByTagName('body')[0];\n" +
                "\t\t\t\t\tif(body){\n" +
                "\t\t\t\t\tlet valueHolderElement = document.getElementById('valueHolder')\n" +
                "\t\t\t\t\t\tlet a = setInterval(()=>{\n" +
                "    fetch('http://RAGHVENKMV2:8080/instances')\n" +
                "  .then((response) => response.json())\n" +
                "  .then((data) => {valueHolderElement.innerHTML =\n" +
                "`<table>\n" +
                "        <tr>\n" +
                "            <th>${data[0].name}</th>\n" +
                "            <th>${data[1].name}</th>\n" +
                "            <th>${data[2].name}</th>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "            <td>${data[0].status}</td>\n" +
                "            <td>${data[1].status}</td>\n" +
                "            <td>${data[2].status}</td>\n" +
                "        </tr>\n" +
                "    </table>`})\n" +
                "},1000)\n" +
                "\n" +
                "\t\t\t\t\t\t}\n" +
                "\t\t\t\t\t}\n" +
                "\t\t</script>\n" +
                "\t</body>\n" +
                "</html>";
    }

}
