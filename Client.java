import java.net.Socket;
import java.io.IOException;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;

public class Client{
     
    static Socket sock = null;
    static PrintStream outToServer = null;
    static InputStream ipstream = null;
    static OutputStream opstream = null;
    
    static BufferedReader inFromClient = null;
    static DataInputStream inFromServer = null;
    static boolean closed = false;
    static String clientName = "client" + (int)(Math.random() * 1000);
    
    public static void main(String[] args) throws Exception{
        int portNumber = 1234;
        
        sock = new Socket("localhost", portNumber);

        opstream = sock.getOutputStream(); 
        outToServer = new PrintStream(opstream);
        outToServer.println("create " + clientName);
        outToServer.flush();
        outToServer.println();
 
        ipstream = sock.getInputStream();
        inFromServer = new DataInputStream(ipstream);
        inFromClient = new BufferedReader(new InputStreamReader(System.in));
        
        recieveMessageAndPrint print = new recieveMessageAndPrint();
        print.start();
        
        ReadAndSend read = new ReadAndSend();
        read.start();
    }
    
    static class ReadAndSend extends Thread{       
        @Override
        public void run(){
            String messageToServer;
            while(true){
                try{
                	messageToServer = inFromClient.readLine();
                    outToServer.println(messageToServer);
                    if(messageToServer.equals("quit")) System.exit(0);                    
                    outToServer.flush();
                } catch(Exception e){
                    break;
                }
            }
        }
    }
    
    static class recieveMessageAndPrint extends Thread{
        @Override
        public void run() {
            String messageFromServer;
            while(true) {
                try{
                	messageFromServer = inFromServer.readLine();
                    if(messageFromServer.startsWith("quit")) System.exit(0);
                    System.out.println(messageFromServer);
                } catch(Exception e) {
                    System.err.println("Exception: Server Crashed! Exiting to handle server crash gracefully");
                    System.exit(0);
                }
            }
        }
    }
}