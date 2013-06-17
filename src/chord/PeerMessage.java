package chord;
import java.io.Serializable;

import model.Slice;

public class PeerMessage implements Serializable {
	private static final long serialVersionUID = -8361483433000719806L;

	public enum Type {
		NOTIFY, FIND_SUCCESSOR, SUCCESSOR, FIND_PREDECESSOR, PREDECESSOR, GET_SLICE, SEND_SLICE
	}

	public Type type;
	public long nodeIdentifier;						// FIND_SUCCESSOR, TEXT
	public int fingerTableIndex;					// FIND_SUCCESSOR, SUCCESSOR
	public PeerInformation peer;					// NOTIFY, FIND_SUCCESSOR, SUCCESSOR, FIND_PREDECESSOR, PREDECESSOR, GET_SLICE, SEND_SLICE
    public Slice slice;                             // SEND_SLICE
    public int slice_num;                           // SEND_SLICE, GET_SLICE
    public int slice_version;                       // SEND_SLICE, GET_SLICE
    public boolean transfer;                        // SEND_SLICE, GET_SLICE
	
	public PeerMessage(PeerInformation origin, long nodeIdentifier, int fingerTableIndex) {
		type = Type.FIND_SUCCESSOR;
		peer = origin;
		this.nodeIdentifier = nodeIdentifier;
		this.fingerTableIndex = fingerTableIndex;
	}
	
	public PeerMessage(int fingerTableIndex, PeerInformation successor) {
		type = Type.SUCCESSOR;
		this.fingerTableIndex = fingerTableIndex;
		this.peer = successor;
	}
	
	public PeerMessage(Type type, PeerInformation peer) {
		this.type = type;
		switch (type) {
		case NOTIFY:
		case PREDECESSOR:
		case FIND_PREDECESSOR:
			this.peer = peer;
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

    public PeerMessage(PeerInformation origin, long nodeIdentifier, int slice_num, int slice_version, boolean transfer) {
        type = Type.GET_SLICE;
        peer = origin;
        this.nodeIdentifier = nodeIdentifier;
        this.slice_num = slice_num;
        this.slice_version = slice_version;
        this.transfer = transfer;
    }
	
	public PeerMessage(PeerInformation origin, long nodeIdentifier, Slice slice, boolean transfer) {
		type = Type.SEND_SLICE;
        peer = origin;
        this.nodeIdentifier = nodeIdentifier;
		this.slice = slice;
        this.slice_num = slice.getNumber();
        this.slice_version = slice.getVersion();
        this.transfer = transfer;
	}
}
