package cloudeventbus.codec;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
class FrameTypes {

	public static final String AUTHENTICATE  = "A";
	public static final String SEND =          "D";
	public static final String ERROR =         "E";
	public static final String GREETING =      "G";
	public static final String PING =          "I";
	public static final String PONG =          "J";
	public static final String MESSAGE =       "M";
	public static final String OK =            "O";
	public static final String PUBLISH =       "P";
	public static final String AUTH_RESPONSE = "R";
	public static final String SUBSCRIBE =     "S";
	public static final String UNSUBSCRIBE =   "U";
	public static final String SERVER_READY =  "Y";

}
