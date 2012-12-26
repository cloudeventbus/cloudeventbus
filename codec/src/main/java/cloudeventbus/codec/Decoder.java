package cloudeventbus.codec;

import cloudeventbus.Constants;
import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class Decoder extends ByteToMessageDecoder<Frame> {

	private static final ByteBuf DELIMITER = Unpooled.copiedBuffer(new byte[]{'\r', '\n'});

	private final int maxMessageSize;

	protected Decoder() {
		this(Constants.DEFAULT_MAX_MESSAGE_SIZE);
	}

	protected Decoder(int maxMessageSize) {
		this.maxMessageSize = maxMessageSize;
	}

	@Override
	public Frame decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
		final int frameLength = indexOf(in, DELIMITER);
		// Frame hasn't been fully read yet.
		if (frameLength < 0) {
			return null;
		}
		// Empty frame, discard and continue decoding
		if (frameLength == 0) {
			in.skipBytes(DELIMITER.capacity());
			return decode(ctx, in);
		}
		final String command = in.readBytes(frameLength).toString(CharsetUtil.UTF_8);
		in.skipBytes(DELIMITER.capacity());
		final String[] parts = command.split("\\s+");
		final String frameType = parts[0];
		switch (frameType) {
			case FrameTypes.AUTH_RESPONSE:
				assertArguments(3, parts.length, "authentication response");
				final String certificate = parts[1];
				final String salt = parts[2];
				final String digitalSignature = parts[3];
				return new AuthenticationResponseFrame(certificate, salt, digitalSignature);

			case FrameTypes.AUTHENTICATE:
				assertArguments(1, parts.length, "authentication request");
				final String challenge = parts[1];
				return new AuthenticationRequestFrame(challenge);
		}
	}

	private void assertArguments(int expectedArguments, int framePartsLenth, String frameName) {
		final int actualArguments = framePartsLenth - 1;
		if (actualArguments != expectedArguments) {
			throw new DecodingException("Expected " + frameName + " to have " + expectedArguments + " arguments. It has " + actualArguments + ".");
		}
	}

	/**
	 * Returns the number of bytes between the readerIndex of the haystack and
	 * the first needle found in the haystack.  -1 is returned if no needle is
	 * found in the haystack.
	 * <p/>
	 * Copied from {@link io.netty.handler.codec.DelimiterBasedFrameDecoder}.
	 */
	private int indexOf(ByteBuf haystack, ByteBuf needle) {
		for (int i = haystack.readerIndex(); i < haystack.writerIndex(); i++) {
			int haystackIndex = i;
			int needleIndex;
			for (needleIndex = 0; needleIndex < needle.capacity(); needleIndex++) {
				if (haystack.getByte(haystackIndex) != needle.getByte(needleIndex)) {
					break;
				} else {
					haystackIndex++;
					if (haystackIndex == haystack.writerIndex() &&
							needleIndex != needle.capacity() - 1) {
						return -1;
					}
				}
			}

			if (needleIndex == needle.capacity()) {
				// Found the needle from the haystack!
				return i - haystack.readerIndex();
			}
		}
		return -1;
	}

}
