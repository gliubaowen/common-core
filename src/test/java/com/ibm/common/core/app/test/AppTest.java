package com.ibm.common.core.app.test;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSON;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
    }
    
    public void test() {
    	List<String> linkedListTemp = new LinkedList<String>();
    	for (int i = 0; i < 10; i++) {
    		String sizeCode = "123456";
    		StringUtils.isBlank(sizeCode);
    		StringUtils.isEmpty(sizeCode);
    		//以尺寸编码为主键进行去重
    		if (!linkedListTemp.contains(sizeCode)) {
    			linkedListTemp.add(sizeCode);
    		}
		}
    	System.out.println(JSON.toJSONString(linkedListTemp));
    }
}
