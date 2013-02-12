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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class BlockingConnectionStateListener implements ConnectionStateListener {

	private final CountDownLatch disconnectLatch;
	private final CountDownLatch connectLatch;

	public BlockingConnectionStateListener(int latchCount) {
		disconnectLatch = new CountDownLatch(latchCount);
		connectLatch = new CountDownLatch(latchCount);
	}

	public BlockingConnectionStateListener() {
		this(1);
	}

	@Override
	public void onOpen(EventBus eventBus, ServerInfo serverInfo) {
		connectLatch.countDown();
	}

	@Override
	public void onClose(EventBus eventBus, ServerInfo serverInfo) {
		disconnectLatch.countDown();
	}

	public void awaitDisconnect() throws InterruptedException {
		disconnectLatch.await(10, TimeUnit.SECONDS);
	}

	public void awaitConnection() throws InterruptedException {
		connectLatch.await(10, TimeUnit.SECONDS);
	}
}
