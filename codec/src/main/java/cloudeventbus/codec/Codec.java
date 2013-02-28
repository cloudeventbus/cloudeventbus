/*
 *   Copyright (c) 2012 Mike Heath.  All rights reserved.
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
package cloudeventbus.codec;

import cloudeventbus.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.MessageBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundByteHandler;
import io.netty.channel.ChannelOutboundMessageHandler;
import io.netty.channel.CombinedChannelDuplexHandler;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class Codec extends CombinedChannelDuplexHandler
        implements ChannelInboundByteHandler, ChannelOutboundMessageHandler<Frame> {

	public static final byte[] DELIMITER = new byte[] {'\r', '\n'};

	public Codec() {
		this(Constants.DEFAULT_MAX_MESSAGE_SIZE);
	}

	public Codec(int maxMessageSize) {
		init(new Decoder(maxMessageSize), new Encoder());
	}

	private Decoder decoder() {
		return (Decoder) stateHandler();
	}

	private Encoder encoder() {
		return (Encoder) operationHandler();
	}

	@Override
	public ByteBuf newInboundBuffer(ChannelHandlerContext ctx) throws Exception {
		return decoder().newInboundBuffer(ctx);
	}

	@Override
	public void freeInboundBuffer(ChannelHandlerContext ctx) throws Exception {
		decoder().freeInboundBuffer(ctx);
	}

	@Override
	public void discardInboundReadBytes(ChannelHandlerContext ctx) throws Exception {
		decoder().discardInboundReadBytes(ctx);
	}

	@Override
	public MessageBuf<Frame> newOutboundBuffer(ChannelHandlerContext ctx) throws Exception {
		return encoder().newOutboundBuffer(ctx);
	}

	@Override
	public void freeOutboundBuffer(ChannelHandlerContext ctx) throws Exception {
		encoder().freeOutboundBuffer(ctx);
	}
}
