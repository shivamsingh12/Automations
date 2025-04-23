package org.shivam.springbootserverlearn.topic;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TopicController {

    @RequestMapping("/topics")
    public List<Topic> getAllTopic(){
        return List.of(new Topic("python","learn Python"),new Topic("java","learn Java"));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/topic/{id}")
    public Topic getTopic(@PathVariable("id") String id){
        return new Topic(id,id+"topic");
    }

    @RequestMapping("/")
    public String root(){
        return "<html><head><title>poopoo</title></head><body> boyd of pooppo</body></html>";
    }
}
