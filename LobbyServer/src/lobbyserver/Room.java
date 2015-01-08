package lobbyserver;
import java.util.concurrent.*;
import messages.EnumTipusSales;

public class Room {

	public String nom;
	EnumTipusSales tipusSala;
	int maxJugadors;
	int numJugadors = 0;
	
	BlockingQueue <Player> jugadors = new LinkedBlockingQueue <Player>();
	
	
	public Room(String nom, EnumTipusSales tipusSala, int maxJugadors){
		this.nom = nom;
		this.tipusSala = tipusSala;
		this.maxJugadors = maxJugadors;
	}
}
