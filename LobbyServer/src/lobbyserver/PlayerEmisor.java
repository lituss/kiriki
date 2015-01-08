package lobbyserver;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PlayerEmisor implements Runnable {
//private Socket socket;
private ObjectOutputStream objectOutput;
private Player player;
private BlockingQueue <Object> missatges = new LinkedBlockingQueue <Object>();
private Boolean interruption = false;

	PlayerEmisor (Socket socket, ObjectOutputStream objectOutput, Player player){
		//this.socket = socket;
		this.objectOutput = objectOutput;
		this.player = player;
	}
	public synchronized void messageEnqueue(Object object){
		missatges.add(object);
		notify();
	}
	private synchronized Object messageDequeue(){
		Object aux;
		
		while(missatges.size()==0)
			try {
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		aux = missatges.remove();
		return aux;
	}
	private synchronized void messageEnvia(Object missatge){
		try {
			objectOutput.writeObject(missatge);
			objectOutput.flush();
		} catch (IOException e) {
			// sha tancat el socket
			//e.printStackTrace();
			interrupt();
		}
	}
	private synchronized void interrupt (){
		interruption = true;
		player.playerReceptor.remoteInterrupt();
		player.lobby.removePlayer(player);
		
		// pararem el emisor i el receptor i eliminarem el usuari
		
	}
	public synchronized void remoteInterrupt(){
		interruption = true;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (!isInterrupted()) {
			Object missatge = messageDequeue();
			messageEnvia(missatge);
		}
	}
	private boolean isInterrupted() {
		// TODO Auto-generated method stub
		return interruption;
	}

}
