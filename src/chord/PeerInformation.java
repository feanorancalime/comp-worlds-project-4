package chord;
import java.io.Serializable;
import java.net.InetAddress;

/**
 * The PeerInformation from class.
 */
public class PeerInformation implements Serializable {
	private static final long serialVersionUID = 2731791060100962454L;

	public long chordID;
	public InetAddress networkID;
	public int port;
	
	public String toString() {
		return "[" + chordID + "]" + (networkID == null ? " ???" : networkID) + ":" + port;
	}
}