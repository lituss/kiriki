package lobbyserver;

public class Server {

	public static Lobby lobby = new Lobby();
	public static sqlDB sql = new sqlDB();
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ThreadPooledServer server = new ThreadPooledServer(9000,lobby);
		
		
		new Thread(server).start();

		try {
		    Thread.sleep(2000 * 1000);
		} catch (InterruptedException e) {
		    e.printStackTrace();
		}
		System.out.println("Stopping Server");
		server.stop();
	}

}
