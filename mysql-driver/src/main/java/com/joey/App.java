package com.joey;

import com.joey.rmi.impl.MysqlDriver;
import com.joey.spi.IDatabaseDriver;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {


        //java spi (service provider interface)
        ServiceLoader<IDatabaseDriver> serviceLoader = ServiceLoader.load(IDatabaseDriver.class);

        Iterator<IDatabaseDriver> iterator =  serviceLoader.iterator();

        while (iterator.hasNext()){

            IDatabaseDriver databaseDriver = iterator.next();
            System.out.println(databaseDriver.connet("localhost"));
        }


//        IDatabaseDriver databaseDriver = new MysqlDriver();
//        System.out.println(databaseDriver.connet("localhost"));


    }
}
