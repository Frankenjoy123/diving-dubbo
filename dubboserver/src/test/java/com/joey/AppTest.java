package com.joey;

import static org.junit.Assert.assertTrue;

import com.joey.hello.IhelloService;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

    @Test
    public void test() throws NoSuchMethodException {
        Class clazz = AppTest.class;
        System.out.println(clazz.getConstructor());


        Class clazz2 = DefaultHelloService.class;
        System.out.println(clazz2.getConstructor(IhelloService.class));
    }

}
