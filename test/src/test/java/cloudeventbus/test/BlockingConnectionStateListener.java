/*
 *   Copyright (c) 2013 Mike Heath.  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package cloudeventbus.test;

import cloudeventbus.client.ConnectionStateListener;
import cloudeventbus.client.EventBus;
import cloudeventbus.client.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class BlockingConnectionStateListener implements ConnectionStateListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(BlockingConnectionStateListener.class);

	private final CountDownLatch disconnectLatch;
	private final CountDownLatch connectLatch;
	private final CountDownLatch connectionFailedLatch;

	public BlockingConnectionStateListener(int latchCount) {
		disconnectLatch = new CountDownLatch(latchCount);
		connectLatch = new CountDownLatch(latchCount);
		connectionFailedLatch = new CountDownLatch(latchCount);
	}

	public BlockingConnectionStateListener() {
		this(1);
	}

	@Override
	public void onOpen(EventBus eventBus, ServerInfo serverInfo) {
		LOGGER.debug("onOpen()");
		connectLatch.countDown();
	}

	@Override
	public void onClose(EventBus eventBus, ServerInfo serverInfo) {
		LOGGER.debug("onClose()");
		disconnectLatch.countDown();
	}

	@Override
	public void onConnectionFailed(EventBus eventBus) {
		LOGGER.debug("onConnectionFailed");
		connectionFailedLatch.countDown();
	}

	public void awaitDisconnect() throws InterruptedException {
		LOGGER.debug("Awaiting disconnect.");
		Assert.assertTrue(disconnectLatch.await(10, TimeUnit.SECONDS), "Timed out waiting for connection to close.");
		LOGGER.debug("Disconnected ok.");
	}

	public void awaitConnection() throws InterruptedException {
		LOGGER.debug("Awaiting connection.");
		Assert.assertTrue(connectLatch.await(10, TimeUnit.SECONDS), "Timed out waiting for connection to be ready.");
		LOGGER.debug("Connected ok.");
	}

	public void awaitConnectionFailed() throws InterruptedException {
		LOGGER.debug("Awaiting connection failed.");
		Assert.assertTrue(connectLatch.await(10, TimeUnit.SECONDS), "Timed out waiting for connection to fail.");
		LOGGER.debug("Connection failed as expected.");
	}
}
