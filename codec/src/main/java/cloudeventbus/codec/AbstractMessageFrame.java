package cloudeventbus.codec;

import io.netty.buffer.ByteBuf;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class AbstractMessageFrame implements Frame {
	private final String subject;
	private final String replySubject;
	private final ByteBuf body;

	public AbstractMessageFrame(String subject, String replySubject, ByteBuf body) {
		this.subject = subject;
		this.replySubject = replySubject;
		this.body = body;
	}

	public String getSubject() {
		return subject;
	}

	public String getReplySubject() {
		return replySubject;
	}

	public ByteBuf getBody() {
		return body;
	}
}
