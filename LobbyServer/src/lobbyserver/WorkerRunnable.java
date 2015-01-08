package lobbyserver;


import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.io.*;

import messages.*;


	public class WorkerRunnable implements Runnable{

	    protected Socket clientSocket = null;
	    protected String serverText   = null;
	    protected EnumEstats estat = EnumEstats.INICIAL;
	    protected InputStream input = null;
	    protected OutputStream output = null;
	    protected ObjectInputStream objectInput ;
	    protected ObjectOutputStream objectOutput;
	    protected Lobby lobby;

	    public WorkerRunnable(Socket clientSocket, String serverText, Lobby lobby) {
	        this.clientSocket = clientSocket;
	        this.serverText   = serverText;
	        this.lobby = lobby;
	    }

	    public void run() {
	        try {
	            input  = clientSocket.getInputStream();
	            objectInput = new ObjectInputStream(input);
	            output = clientSocket.getOutputStream();
	            objectOutput = new ObjectOutputStream(output);
	            estatInicial();
	            
	            /*long time = System.currentTimeMillis();
	            /output.write(("HTTP/1.1 200 OK\n\nWorkerRunnable: " +
	                    this.serverText + " - " +
	                    time +
	                    "").getBytes());
	            output.close();
	            input.close();
	            System.out.println("Request processed: " + time);
	            */
	        } catch (IOException e) {
	            //report exception somewhere.
	            e.printStackTrace();
	        }
	    }
	    
	    void estatInicial()	{
	    	try {
				Object aux = objectInput.readObject();
	    	
				if (aux instanceof CSlogin) { onLogin((CSlogin)aux);}
				else if (aux instanceof  CSjoin) {  onJoin ((CSjoin)aux);}
	   
	    	
			} catch (Exception e) {e.printStackTrace();
				// TODO: handle exception
			}
	    }
		void onLogin (CSlogin cslogin)
		{
			SClogged sclogged = new SClogged();
		//	Player player;
			
			if (lobby.sql.login(cslogin.user, cslogin.pass))
			{
				sclogged.login = EnumJoin.OK.ordinal();
				try {
					new Player(cslogin.user,clientSocket,objectInput,objectOutput,lobby);
					objectOutput.writeObject(sclogged);
					objectOutput.writeObject(lobby.listRooms());
					objectOutput.writeObject(lobby.listLobbyPlayers());
					objectOutput.flush();
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else 
			{
				if (lobby.sql.existeixUsuari(cslogin.user)) sclogged.login = EnumJoin.KO_BAD_PASS.ordinal();
				else sclogged.login = EnumJoin.KO_BAD_USER.ordinal();
			
				try {
					objectOutput.writeObject(sclogged);
					objectOutput.flush();
					estatFinal();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			
		}
	    void onJoin( CSjoin csjoin)
	    {
	    	SCjoined scjoined = new SCjoined();
	    	if (lobby.sql.join(csjoin.user, csjoin.pass)) {
	    		scjoined.join = EnumJoin.OK.ordinal();
	    		try{
	    		objectOutput.writeObject(scjoined);
				objectOutput.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	}
	    	
	    	else {
	    		scjoined.join = EnumJoin.KO_BAD_USER.ordinal();
	    	}
	    	try {
				objectOutput.writeObject(scjoined);
				objectOutput.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	
	    	estatFinal();	
	   	}
	    	
	    
	    void estatFinal(){
	    	try{
	    
	    
	    	objectOutput.close();
	    	output.close();
	    	objectInput.close();
	    	input.close();
	    	clientSocket.close();
	    	} catch (IOException e) {e.printStackTrace();}
	    }
	}

