package org.talend.mq;

import java.util.Hashtable;

import javax.jms.JMSException;

import junit.framework.TestCase;

import org.junit.Test;

import com.ibm.mq.MQException;

public class TestSharedWebShpereMQConn extends TestCase {
	private static Hashtable<String,Object> properties = new java.util.Hashtable<String,Object>();
	static{
		properties.put("hostname", "localhost");
		properties.put("port", Integer.valueOf("1414"));
		properties.put("channel", "TALEND.CH");
		properties.put("CCSID", new Integer(1208));
		properties.put("transport", "MQSeries");
	}
	
	
	@Test
	public void testSameConnNAme() throws JMSException, MQException {
		assertTrue(SharedWebSphereMQConnection.getMQConnection("TALEND", properties, "conn")==SharedWebSphereMQConnection.getMQConnection("TALEND", properties, "conn"));
	}
	@Test
	public void testDiffConnNAme() throws JMSException, MQException {
		assertTrue(SharedWebSphereMQConnection.getMQConnection("TALEND", properties, "conn1")!=SharedWebSphereMQConnection.getMQConnection("TALEND", properties, "conn2"));
	}

}
