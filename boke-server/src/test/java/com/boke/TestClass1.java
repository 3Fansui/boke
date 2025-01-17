package com.boke;

import com.boke.service.impl.ResourceServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class TestClass1 {
    @Autowired
    ResourceServiceImpl resourceService;
    @Test
    public void test1(){
        resourceService.importSwagger();
    }

}
