package com.joey;

import com.joey.hello.IhelloService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws IOException {

        ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext("classpath:*.xml");

        context.start();


        IhelloService ihelloService = (IhelloService) context.getBean("helloService");

        String result = ihelloService.sayHello("joey");

        System.out.println(result);

        System.in.read();

    }
}
