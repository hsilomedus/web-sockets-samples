package mk.hsilomedus.websockets.samples.javawebsocket;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;


public class Main extends WebSocketServer {

  /** The web socket port number */
  private static int PORT = 8887;
  
  private Set<WebSocket> conns;
  private Map<WebSocket, String> nickNames;
  
  /**
   * Creates a new WebSocketServer with the wildcard IP accepting all connections.
   */
  public Main() {
    super(new InetSocketAddress(PORT));
    conns = new HashSet<>();
    nickNames = new HashMap<>();
  }
  
  /** 
   * Method handler when a new connection has been opened. 
   */
  @Override
  public void onOpen(WebSocket conn, ClientHandshake handshake) {
    conns.add(conn);
    System.out.println("New connection from " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
  }

  /** 
   * Method handler when a connection has been closed.
   */
  @Override
  public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    String nick = nickNames.get(conn);
    conns.remove(conn);
    nickNames.remove(conn);
    if (nick!= null) {
      removeUser(nick);
    }
    System.out.println("Closed connection to " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
  }

  /** 
   * Method handler when a message has been received from the client.
   */
  @Override
  public void onMessage(WebSocket conn, String message) {
    System.out.println("Received: " + message);
    if (!nickNames.containsKey(conn)) {
      //No nickname has been assigned by now
      //the first message is the nickname
      //escape the " character first
      message = message.replace("\"", "\\\"");
      
      //broadcast all the nicknames to him
      for (String nick : nickNames.values()) {
        conn.send("{\"addUser\":\"" + nick + "\"}");
      }
      
      //Register the nickname with the 
      nickNames.put(conn, message);
      
      //broadcast him to everyone now
      String messageToSend = "{\"addUser\":\"" + message + "\"}";
      for (WebSocket sock : conns) {
        sock.send(messageToSend);
      }
    } else {
      //Broadcast the message
      String messageToSend = "{\"nickname\":\"" + nickNames.get(conn)
          + "\", \"message\":\"" + message.replace("\"", "\\\"") +"\"}";
      for (WebSocket sock : conns) {
        sock.send(messageToSend);
      }
    }
  }
  

  /** 
   * Method handler when an error has occured.
   */
  @Override
  public void onError(WebSocket conn, Exception ex) {
    String nick = nickNames.get(conn);
    conns.remove(conn);
    nickNames.remove(conn);
    if (nick!= null) {
      removeUser(nick);
    }
    System.out.println("ERROR from " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
  }
  
  private void removeUser(String username) {
    String messageToSend = "{\"removeUser\":\"" + username + "\"}";
    for (WebSocket sock : conns) {
      sock.send(messageToSend);
    }
  }
  
  
  /**
   * Main method.
   */
  public static void main(String[] args) {
    Main server = new Main();
    server.start();
  }
  
}
