import java.util.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;

public class Server {
    
    public static HashMap<String, clientThread> clients = new HashMap<>();
    public static HashMap<String, Set<String>> roomUserMap = new HashMap<>();
    public static HashMap<String, Set<String>> userroomMap = new HashMap<>();
    
	public static void main(String[] args) throws Exception {
        
        int portNumber = 1234;        
        ServerSocket serversock = new ServerSocket(portNumber);
        System.out.println("Server Running on port number " + portNumber);
        
        serverCLIReader s = new serverCLIReader();
        s.start();
 
        String messageFromClient, messageToClient = "";
        while(true) {
            try{
                Socket sock = serversock.accept(); // Client connected to a server.
                DataInputStream inputstream = new DataInputStream(sock.getInputStream());
                PrintStream outputstream = new PrintStream(sock.getOutputStream());
                if((messageFromClient = inputstream.readLine()) != null){
                    if(messageFromClient.contains( "create")) {
                        String name = messageFromClient.split(" ")[1];
                        clients.put(name, new clientThread(name, sock));
                        clients.get(name).start();
                        
                        messageToClient = "Welcome " + name + "!";
                        outputstream.println(messageToClient);
                    }
                }
                outputstream.flush();
            } catch(Exception e){
                break;
            }
            
        }
        serversock.close();
    }
    
    public static class serverCLIReader extends Thread{
        @Override
        public void run(){
            String inFromOperator;
            BufferedReader keyRead = new BufferedReader(new InputStreamReader(System.in));
            while(true){
                try{
                	inFromOperator = keyRead.readLine();
                    if(inFromOperator.startsWith("quit")) {
                    	closeDown();
                        System.exit(0);
                    }
                } catch(Exception e){
                    
                }
            }
        }
        
        public void closeDown() {
            for(String key : clients.keySet()){
                clients.get(key).opstream.println("Server OFF! Please try later.");
                clients.get(key).opstream.println("quit");
                clients.get(key).opstream.close();
                try {
                    clients.get(key).ipstream.close();
                    clients.get(key).clientSock.close();
                } catch (Exception ex) {
                    
                }
            }
        }
    }

	public static class clientThread extends Thread{
	    String clientName;
	    Socket clientSock = null;
	    PrintStream opstream = null;
	    DataInputStream ipstream = null;
	    
	    public clientThread(String name, Socket clientSocket) {
	        this.clientSock = clientSocket;
	        this.clientName = name;
	    }
	
	    @Override
	    public void run() {
	        try {
	            ipstream = new DataInputStream(clientSock.getInputStream());
	            opstream = new PrintStream(clientSock.getOutputStream());
	            System.out.println("Client " + clientName + " connected");
	            String name = ipstream.readLine().trim();
	            synchronized(this){
	                while (true) {
	                    String line = ipstream.readLine();
						String[] lineArr = line.split(" ");
	                    if(lineArr.length > 0){
		                    String operation = lineArr[0].toLowerCase();
		                    //System.out.println(line);
		                    //Regular Text operations
		                    switch (operation) {
		                        case "group":
		                            groupMessage(line);
		                            break;
		                        case "private":
		                            privateMessage(line);
		                            break;
		                        case "create":
		                        	createRoom(line);
		                        	break;
		                        case "join":
		                        	joinRoom(line);
		                        	break;
		                        case "listrooms":
		                        	listRooms();
		                        	break;
		                        case "listmembers":
		                        	listMembers(line);
		                        	break;
		                        case "leaveroom":
		                        	leaveRoom(line);
		                        	break;
		                        default:
		                        	if (line.startsWith("quit")) {
				                    	cliendThreadCloseDown();
				                    	Thread.currentThread().stop();
				                    }
		                        	invalidCommand();
		                            break;
		                    }
		                    
		                    if (line.startsWith("quit")) {
		                    	cliendThreadCloseDown();
		                    	Thread.currentThread().stop();
		                    }
	                    }
	                }
	            }
	        } catch (Exception e) {
	        	cliendThreadCloseDown();
	        }
	    }
	    
	    // Invalid command 
	    public void invalidCommand() {
	    	opstream.println("Invalid command! Please enter proper command");
	    }
	    
	    // cliendThread close Down logic
	    public void cliendThreadCloseDown() {
	    	try {
	            opstream.println("See you later! " + clientName);
		    	System.out.println(clientName + " disconnected!");
	            userroomMap.remove(clientName);
	            for(String room: roomUserMap.keySet()) {
	          	  if(roomUserMap.get(room).contains(clientName)) {
	          		  roomUserMap.get(room).remove(clientName);
	          		  for(String key: roomUserMap.get(room))
	        				clients.get(key).opstream.println(clientName + " left the room " + room);
	          	  }
	            }
	            clients.remove(clientName);
	            opstream.close();
	            ipstream.close();
	            clientSock.close();
	        } catch (Exception ex) {
                
            }
	    }
	    
	    // Client can create a room
	    // Command: create <ROOMNAME/>
	    public void createRoom(String s){
	    	String room = s.split(" ")[1];
	    	
	    	if(!roomUserMap.containsKey(room)) {
	    		roomUserMap.put(room, new HashSet<String>());
		    	opstream.println("Room " + room + " created");
		        System.out.println("Room created [" + room + "]");
	    	}else opstream.println("Room " + room + " already exits");
	    }
	    
	    // Client can join in single or multiple rooms
	    // Command: join <ROOMNAME/>
	    public void joinRoom(String s){
	    	String room = s.split(" ")[1];
	    	        
	    	if(!roomUserMap.containsKey(room)) {
	    		opstream.println("Room " + room + " doesn't exist!");
	    		return;
	    	}else if(!userroomMap.containsKey(clientName)) {
	    		userroomMap.put(clientName, new HashSet<String>());
	    	}else if(roomUserMap.get(room).contains(clientName) || userroomMap.get(clientName).contains(room)) {
	    		opstream.println("You are already in the room: " + room);
	    		return;
	    	}	    	
	    	userroomMap.get(clientName).add(room);
    		roomUserMap.get(room).add(clientName);
    		opstream.println("Joined to Room: " + room); 
    		for(String key: roomUserMap.get(room)) {
    			if(!key.equals(this.clientName)){
    				clients.get(key).opstream.println(clientName + " joined the room " + room);
	            }
			}
    		System.out.println(clientName + " joined room [" + room + "]");
	    }
	    
	    // Client can leave any room
	    // Command: leaveroom <ROOMNAME/>
	    public void leaveRoom(String s) {
	    	String room = s.split(" ")[1];
	    	if(!roomUserMap.containsKey(room)) {
	    		opstream.println("Room " + room + " doesn't exist!");
	    		return;
	    	}

	    	if(userroomMap.containsKey(clientName) && userroomMap.get(clientName).contains(room)) {
	    		userroomMap.get(clientName).remove(room);
    			roomUserMap.get(room).remove(clientName);
    			opstream.println("You left room " + room);
    			for(String key: roomUserMap.get(room)) {
    				clients.get(key).opstream.println(clientName + " left the room " + room);
    			}
    			System.out.println(clientName + " left the room " + room);
    			return;
	    	}
	    	opstream.println("You are not member of room " + room);
	    }
	    
	    // Client can list members of any room
	    // Command: listmembers <ROOMNAME/>
	    public void listMembers(String s) {
	    	String room = s.split(" ")[1];
	    	if(!roomUserMap.containsKey(room)) {
	    		opstream.println("Room " + room + " doesn't exist!");
	    		return;
	    	}
	    	for(String client: roomUserMap.get(room)) opstream.println(client);
	    }
	    
	    // client can list rooms
	    //command: listrooms
	    public void listRooms() {
	    	for(String s: roomUserMap.keySet()) opstream.println(s);
	    } 
	    
	    // Client can send message to room(group). Also can send distinct messages to multiple rooms
	    // Command: group <ROOMNAME/> <MESSAGE/>
	    public void groupMessage(String s){
	        String room = s.split(" ")[1];
	        String message = this.clientName + ":" + s.substring(s.indexOf(room));
	        if(!roomUserMap.get(room).contains(this.clientName)) {
	        	opstream.println("You are not member of this room " + room);
	        	return;
	        }
	        roomUserMap.get(room).forEach((key) -> {
	            if(key.equals(this.clientName)){
	                clients.get(key).opstream.println("Message Sent");
	            }
	            clients.get(key).opstream.println(message);
	        });
	        System.out.println(this.clientName + " braodcasted message");
	    }
	    
	    // Client can send private message to any client
	    // Command: private <CLIENTNAME/> <MESSAGE/>
	    public void privateMessage(String s){
	        String user = s.split(" ")[1];
	        String message = this.clientName + ":" + s.substring(s.indexOf(user) + user.length());
	        
	        if(clients.containsKey(user)) clients.get(user).opstream.println(message);
	        else {
	        	opstream.println("Client " + user + " doesn't exist "); 
	        	return;
	        }
	        clients.get(clientName).opstream.println("Message Sent");
	        System.out.println(clientName + " sent private message to [" + user + "]");
	    }
	}
}