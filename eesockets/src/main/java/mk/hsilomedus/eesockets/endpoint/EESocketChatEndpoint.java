
package mk.hsilomedus.eesockets.endpoint;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;


@ServerEndpoint("/chat")
public class EESocketChatEndpoint {
  
  private static Set<Session> conns = java.util.Collections.synchronizedSet(new HashSet<Session>());
  private static Map<Session, String> nickNames = new ConcurrentHashMap<Session, String>();
  
  private Session currentSession;
  public EESocketChatEndpoint() {
    System.out.println("Constructed!");
  }
  
  @OnOpen
  public void onOpen (Session session) {
     System.out.println ("WebSocket opened: "+session.getId());
     conns.add(session);
     this.currentSession = session;
  }

  @OnMessage
  public void onMessage (String message) {
    System.out.println("Received: " + message);
    if (!nickNames.containsKey(currentSession)) {
      //No nickname has been assigned by now
      //the first message is the nickname
      //escape the " character first
      message = message.replace("\"", "\\\"");
      
      //broadcast all the nicknames to him
      for (String nick : nickNames.values()) {
        try {
          currentSession.getBasicRemote().sendText("{\"addUser\":\"" + nick + "\"}");
        } catch (IOException e) {
          System.out.println("Error when sending addUser message");          
        }
      }
      
      //Register the nickname with the 
      nickNames.put(currentSession, message);
      
      //broadcast him to everyone now
      String messageToSend = "{\"addUser\":\"" + message + "\"}";
      for (Session sock : conns) {
        try {
          sock.getBasicRemote().sendText(messageToSend);
        } catch (IOException e) {
          System.out.println("Error when sending broadcast addUser message");
        }
      }
    } else {
      //Broadcast the message
      String messageToSend = "{\"nickname\":\"" + nickNames.get(currentSession)
          + "\", \"message\":\"" + message.replace("\"", "\\\"") +"\"}";
      for (Session sock : conns) {
        try {
          sock.getBasicRemote().sendText(messageToSend);
        } catch (IOException e) {
          System.out.println("Error when sending message message");
        }
      }
    }
  } 
  
  @OnClose
  public void onClose (Session session, CloseReason reason) {
    String nick = nickNames.get(session);
    conns.remove(session);
    nickNames.remove(session);
    if (nick!= null) {
      removeUser(nick);
    }    
     System.out.println ("Closing a WebSocket due to "+reason.getReasonPhrase());
  }
  
  @OnError
  public void onError (Session session, Throwable throwable) {
    String nick = nickNames.get(session);
    conns.remove(session);
    nickNames.remove(session);
    if (nick!= null) {
      removeUser(nick);
    }
  }
  
  private void removeUser(String username) {
    String messageToSend = "{\"removeUser\":\"" + username + "\"}";
    for (Session sock : conns) {
      try {
        sock.getBasicRemote().sendText(messageToSend);
      } catch (IOException e) {
        System.out.println("IO exception when sending remove user message");
      }
    }
  }

}
