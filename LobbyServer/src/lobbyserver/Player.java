

package lobbyserver;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.locks.*;

public class Player {
	String alias;
	private EnumEstats estat;
	private Room room;
	PlayerEmisor playerEmisor;
	PlayerReceptor playerReceptor;
	Lobby lobby;
	static Lock bloqueja;
	
	public String getAlias() {
		return alias;
	}
	public Player (String alias,Socket socket,ObjectInputStream objectInput, ObjectOutputStream objectOutput, Lobby lobby){
		this.alias = alias;
		this.playerEmisor = new PlayerEmisor(socket, objectOutput, this);
		this.playerReceptor = new PlayerReceptor(socket, objectInput,this);
		this.lobby = lobby;
		setEstat(EnumEstats.LOGIN);
		
	}
	synchronized void tractaMissatge(Object missatge){
		
	}
	public Room getRoom() {
		return room;
	}
	public void setRoom(Room room) {
		this.room = room;
	}
	public EnumEstats getEstat() {
		return estat;
	}
	public void setEstat(EnumEstats estat) {
		this.estat = estat;
	}

}
