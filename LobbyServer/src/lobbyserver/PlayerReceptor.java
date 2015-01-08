package lobbyserver;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class PlayerReceptor implements Runnable {
	//private Socket socket;
	private ObjectInputStream objectInput;
	private Player player;
	private Boolean interruption = false;
	
	PlayerReceptor(Socket socket,ObjectInputStream objectInput,Player player){
	//	this.socket = socket;
		this.objectInput = objectInput;
		this.player = player;
	}

	@Override
	public void run() {
		try {
	           while (!isInterrupted()) {
	               Object missatge = objectInput.readObject();
	               if (missatge == null)
	                   break;
	               player.tractaMissatge(missatge);
	           }
	        } catch (IOException ioex) {
	           // Problem reading from socket (communication is broken)
	        	interrupt();
	        } catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	}
	private boolean isInterrupted() {
		// TODO Auto-generated method stub
		return interruption;
	}

	private synchronized void interrupt (){
		interruption = true;
		player.playerEmisor.remoteInterrupt();
		player.lobby.removePlayer(player);
		
		// pararem el emisor i el receptor i eliminarem el usuari (desde el emisor, per ferho desde un sol lloc
		
	}
	public synchronized void remoteInterrupt(){
		interruption = true;
	}
}
