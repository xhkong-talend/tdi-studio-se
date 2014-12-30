package org.talend.mq;

import javax.jms.JMSException;

import junit.framework.TestCase;

import org.junit.Test;

public class TestSharedActiveMQConn extends TestCase {
	
	
	
	@Test
	public void testSameConnNAme() throws JMSException {
		assertTrue(SharedActiveMQConnection.getMQConnection("tcp://localhost:61616", "", "", "conn")==SharedActiveMQConnection.getMQConnection("tcp://localhost:61616", "", "", "conn"));
	}
	@Test
	public void testDiffConnNAme() throws JMSException {
		assertFalse(SharedActiveMQConnection.getMQConnection("tcp://localhost:61616", "", "", "conn1")==SharedActiveMQConnection.getMQConnection("tcp://localhost:61616", "", "", "conn2"));
	}

}
