
/**
 * Nakov Chat Server
 * (c) Svetlin Nakov, 2002
 * http://www.nakov.com
 *
 * Nakov Chat Server is multithreaded chat server. It accepts multiple clients
 * simultaneously and serves them. Clients can send messages to the server.
 * When some client send a message to the server, this message is dispatched
 * to all the clients connected to the server.
 *
 * The server consists of two components - "server core" and "client handlers".
 *
 * The "server core" consists of two threads:
 *   - NakovChatServer - accepts client connections, creates client threads to
 * handle them and starts these threads
 *   - ServerDispatcher - waits for a messages and sends arrived messages to
 * all the clients connected to the server
 *
 * The "client handlers" consist of two threads:
 *   - ClientListener - listens for message arrivals from the socket and
 * forwards them to the ServerDispatcher thread
 *   - ClientSender - sends messages to the client
 *
 * For each accepted client, a ClientListener and ClientSender threads are
 * created and started. A ClientInfo object is also created to contain the
 * information about the client. Also the ClientInfo object is added to the
 * ServerDispatcher's clients list. When some client is disconnected, is it
 * removed from the clients list and both its ClientListener and ClientSender
 * threads are interrupted.
 *
 *
 * NakovChatServer class is entry point for the program. It opens a server
 * socket, starts the dispatcher thread and infinitely accepts client connections,
 * creates threads for handling them and starts these threads.
 */
 
import java.net.*;
import java.io.*;
 
public class NakovChatServer
{
    public static final int LISTENING_PORT = 2002;
 
    public static void main(String[] args)
    {
        // Open server socket for listening
        ServerSocket serverSocket = null;
        try {
           serverSocket = new ServerSocket(LISTENING_PORT);
           System.out.println("NakovChatServer started on port " + LISTENING_PORT);
        } catch (IOException se) {
           System.err.println("Can not start listening on port " + LISTENING_PORT);
           se.printStackTrace();
           System.exit(-1);
        }
 
        // Start ServerDispatcher thread
        ServerDispatcher serverDispatcher = new ServerDispatcher();
        serverDispatcher.start();
 
        // Accept and handle client connections
        while (true) {
           try {
               Socket socket = serverSocket.accept();
               ClientInfo clientInfo = new ClientInfo();
               clientInfo.mSocket = socket;
               ClientListener clientListener =
                   new ClientListener(clientInfo, serverDispatcher);
               ClientSender clientSender =
                   new ClientSender(clientInfo, serverDispatcher);
               clientInfo.mClientListener = clientListener;
               clientInfo.mClientSender = clientSender;
               clientListener.start();
               clientSender.start();
               serverDispatcher.addClient(clientInfo);
           } catch (IOException ioe) {
               ioe.printStackTrace();
           }
        }
    }
 
}
 
 
/**
 * Nakov Chat Server
 * (c) Svetlin Nakov, 2002
 *
 * ServerDispatcher class is purposed to listen for messages received
 * from clients and to dispatch them to all the clients connected to the
 * chat server.
 */
 
import java.net.*;
import java.util.*;
 
public class ServerDispatcher extends Thread
{
    private Vector mMessageQueue = new Vector();
    private Vector mClients = new Vector();
 
    /**
     * Adds given client to the server's client list.
     */
    public synchronized void addClient(ClientInfo aClientInfo)
    {
        mClients.add(aClientInfo);
    }
 
    /**
     * Deletes given client from the server's client list
     * if the client is in the list.
     */
    public synchronized void deleteClient(ClientInfo aClientInfo)
    {
        int clientIndex = mClients.indexOf(aClientInfo);
        if (clientIndex != -1)
           mClients.removeElementAt(clientIndex);
    }
 
    /**
     * Adds given message to the dispatcher's message queue and notifies this
     * thread to wake up the message queue reader (getNextMessageFromQueue method).
     * dispatchMessage method is called by other threads (ClientListener) when
     * a message is arrived.
     */
    public synchronized void dispatchMessage(ClientInfo aClientInfo, String aMessage)
    {
        Socket socket = aClientInfo.mSocket;
        String senderIP = socket.getInetAddress().getHostAddress();
        String senderPort = "" + socket.getPort();
        aMessage = senderIP + ":" + senderPort + " : " + aMessage;
        mMessageQueue.add(aMessage);
        notify();
    }
 
    /**
     * @return and deletes the next message from the message queue. If there is no
     * messages in the queue, falls in sleep until notified by dispatchMessage method.
     */
    private synchronized String getNextMessageFromQueue()
    throws InterruptedException
    {
        while (mMessageQueue.size()==0)
           wait();
        String message = (String) mMessageQueue.get(0);
        mMessageQueue.removeElementAt(0);
        return message;
    }
 
    /**
     * Sends given message to all clients in the client list. Actually the
     * message is added to the client sender thread's message queue and this
     * client sender thread is notified.
     */
    private synchronized void sendMessageToAllClients(String aMessage)
    {
        for (int i=0; i<mClients.size(); i++) {
           ClientInfo clientInfo = (ClientInfo) mClients.get(i);
           clientInfo.mClientSender.sendMessage(aMessage);
        }
    }
 
    /**
     * Infinitely reads messages from the queue and dispatch them
     * to all clients connected to the server.
     */
    public void run()
    {
        try {
           while (true) {
               String message = getNextMessageFromQueue();
               sendMessageToAllClients(message);
           }
        } catch (InterruptedException ie) {
           // Thread interrupted. Stop its execution
        }
    }
 
}
 
 
/**
 * Nakov Chat Server
 * (c) Svetlin Nakov, 2002
 *
 * ClientInfo class contains information about a client, connected to the server.
 */
 
import java.net.Socket;
 
public class ClientInfo
{
    public Socket mSocket = null;
    public ClientListener mClientListener = null;
    public ClientSender mClientSender = null;
}
 
 
/**
 * Nakov Chat Server - (c) Svetlin Nakov, 2002
 *
 * ClientListener class is purposed to listen for client messages and
 * to forward them to ServerDispatcher.
 */
 
import java.io.*;
import java.net.*;
 
public class ClientListener extends Thread
{
    private ServerDispatcher mServerDispatcher;
    private ClientInfo mClientInfo;
    private BufferedReader mIn;
 
    public ClientListener(ClientInfo aClientInfo, ServerDispatcher aServerDispatcher)
    throws IOException
    {
        mClientInfo = aClientInfo;
        mServerDispatcher = aServerDispatcher;
        Socket socket = aClientInfo.mSocket;
        mIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }
 
    /**
     * Until interrupted, reads messages from the client socket, forwards them
     * to the server dispatcher's queue and notifies the server dispatcher.
     */
    public void run()
    {
        try {
           while (!isInterrupted()) {
               String message = mIn.readLine();
               if (message == null)
                   break;
               mServerDispatcher.dispatchMessage(mClientInfo, message);
           }
        } catch (IOException ioex) {
           // Problem reading from socket (communication is broken)
        }
 
        // Communication is broken. Interrupt both listener and sender threads
        mClientInfo.mClientSender.interrupt();
        mServerDispatcher.deleteClient(mClientInfo);
    }
 
}
 
 
/**
 * Nakov Chat Server - (c) Svetlin Nakov, 2002
 *
 * Sends messages to the client. Messages are stored in a message queue. When
 * the queue is empty, ClientSender falls in sleep until a new message is
 * arrived in the queue. When the queue is not empty, ClientSender sends the
 * messages from the queue to the client socket.
 */
 
import java.io.*;
import java.net.*;
import java.util.*;
 
public class ClientSender extends Thread
{
    private Vector mMessageQueue = new Vector();
 
    private ServerDispatcher mServerDispatcher;
    private ClientInfo mClientInfo;
    private PrintWriter mOut;
 
    public ClientSender(ClientInfo aClientInfo, ServerDispatcher aServerDispatcher)
    throws IOException
    {
        mClientInfo = aClientInfo;
        mServerDispatcher = aServerDispatcher;
        Socket socket = aClientInfo.mSocket;
        mOut = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
    }
 
    /**
     * Adds given message to the message queue and notifies this thread
     * (actually getNextMessageFromQueue method) that a message is arrived.
     * sendMessage is called by other threads (ServeDispatcher).
     */
    public synchronized void sendMessage(String aMessage)
    {
        mMessageQueue.add(aMessage);
        notify();
    }
 
    /**
     * @return and deletes the next message from the message queue. If the queue
     * is empty, falls in sleep until notified for message arrival by sendMessage
     * method.
     */
    private synchronized String getNextMessageFromQueue() throws InterruptedException
    {
        while (mMessageQueue.size()==0)
           wait();
        String message = (String) mMessageQueue.get(0);
        mMessageQueue.removeElementAt(0);
        return message;
    }
 
    /**
     * Sends given message to the client's socket.
     */
    private void sendMessageToClient(String aMessage)
    {
        mOut.println(aMessage);
        mOut.flush();
    }
 
    /**
     * Until interrupted, reads messages from the message queue
     * and sends them to the client's socket.
     */
    public void run()
    {
        try {
           while (!isInterrupted()) {
               String message = getNextMessageFromQueue();
               sendMessageToClient(message);
           }
        } catch (Exception e) {
           // Commuication problem
        }
 
        // Communication is broken. Interrupt both listener and sender threads
        mClientInfo.mClientListener.interrupt();
        mServerDispatcher.deleteClient(mClientInfo);
    }
 
}
Client For Nakov Chat Server
 
/**
 * Nakov Chat Client
 * (c) Svetlin Nakov, 2002
 * http://www.nakov.com
 *
 * NakovChatClient connects to Nakov Chat Server and prints all the messages
 * received from the server. It also allows the user to send messages to the
 * server. NakovChatClient thread reads messages and print them to the standard
 * output. Sender thread reads messages from the standard input and sends them
 * to the server.
 */
 
import java.io.*;
import java.net.*;
 
public class NakovChatClient
{
    public static final String SERVER_HOSTNAME = "localhost";
    public static final int SERVER_PORT = 2002;
 
    public static void main(String[] args)
    {
        BufferedReader in = null;
        PrintWriter out = null;
        try {
           // Connect to Nakov Chat Server
           Socket socket = new Socket(SERVER_HOSTNAME, SERVER_PORT);
           in = new BufferedReader(
               new InputStreamReader(socket.getInputStream()));
           out = new PrintWriter(
               new OutputStreamWriter(socket.getOutputStream()));
           System.out.println("Connected to server " +
              SERVER_HOSTNAME + ":" + SERVER_PORT);
        } catch (IOException ioe) {
           System.err.println("Can not establish connection to " +
               SERVER_HOSTNAME + ":" + SERVER_PORT);
           ioe.printStackTrace();
           System.exit(-1);
        }
 
        // Create and start Sender thread
        Sender sender = new Sender(out);
        sender.setDaemon(true);
        sender.start();
 
        try {
           // Read messages from the server and print them
            String message;
           while ((message=in.readLine()) != null) {
               System.out.println(message);
           }
        } catch (IOException ioe) {
           System.err.println("Connection to server broken.");
           ioe.printStackTrace();
        }
 
    }
}
 
class Sender extends Thread
{
private PrintWriter mOut;
 
public Sender(PrintWriter aOut)
{
        mOut = aOut;
}
 
    /**
     * Until interrupted reads messages from the standard input (keyboard)
     * and sends them to the chat server through the socket.
     */
public void run()
{
try {
BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
while (!isInterrupted()) {
String message = in.readLine();
mOut.println(message);
mOut.flush();
}
} catch (IOException ioe) {
            // Communication is broken
}
}
}

/* exemple FSM
         State design pattern - an FSM with two states and one event
(distributed transition logic - logic in the derived state classes)
Create a "wrapper" class that models the state machine
The wrapper class contains an array of state concrete objects
The wrapper class contains an index to its "current" state
The wrapper class contains a state transition table
All client requests are simply delegated to the current state object
Create a state base class that makes the concrete states interchangeable
The State base class specifies default behavior for all messages
The State derived classes only override the messages they need to
// 1. Create a "wrapper" class that models the state machine
class FSM {
   private State[] states     = { new A(), new B(), new C() }; // 2. states
   private int[][] transition = { {2,1,0}, {0,2,1}, {1,2,2} }; // 4. transitions
   private int     current    = 0;                             // 3. current
   private void next( int msg ) { current = transition[current][msg]; }

   // 5. All client requests are simply delegated to the current state object
   public void on()  { states[current].on();   next( 0 ); }
   public void off() { states[current].off();  next( 1 ); }
   public void ack() { states[current].ack();  next( 2 ); }
}

// 6. Create a state base class that makes the concrete states interchangeable
abstract class State {
   public void on()  { System.out.println( "error" ); }  // 7. The State base 
   public void off() { System.out.println( "error" ); }  //    class specifies 
   public void ack() { System.out.println( "error" ); }  //    default behavior
}

class A extends State {
   public void on()  { System.out.println( "A + on  = C" ); }
   public void off() { System.out.println( "A + off = B" ); }
   public void ack() { System.out.println( "A + ack = A" ); }
}

class B extends State {
   public void on()  { System.out.println( "B + on  = A" ); }
   public void off() { System.out.println( "B + off = C" ); }
}

class C extends State {
   // 8. The State derived classes only override the messages they need to
   public void on()  { System.out.println( "C + on  = B" ); }
}

public class StateDemo {
   public static void main( String[] args ) {
      FSM   fsm  = new FSM();
      int[] msgs = { 2, 1, 2, 1, 0, 2, 0, 0 };
      for (int i=0; i < msgs.length; i++)
         if      (msgs[i] == 0) fsm.on();
         else if (msgs[i] == 1) fsm.off();
         else if (msgs[i] == 2) fsm.ack();
}  }
A + ack = A                                         on  off ack
A + off = B                                        +---+---+---+
error                                            A | C | B | A |
B + off = C                                        +---+---+---+
C + on  = B                                      B | A | C |   |
error                                              +---+---+---+
B + on  = A                                      C | B |   |   |
A + on  = C                                        +---+---+---+

*/
