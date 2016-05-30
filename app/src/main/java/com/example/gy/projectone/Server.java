package com.example.gy.projectone;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class Server {
    public final String KEY_STATE = "STATE" ;
    public final String KEY_USER_NAME = "USER_NAME" ;
    public final String KEY_USER_PWD = "USER_PWD" ;
    public final String KEY_RSSI = "RSSI" ;
    public final String KEY_UUID = "UUID" ;
    public final String KEY_MAJOR = "MAJOR" ;
    public final String KEY_MINOR = "MINOR" ;
    public final int VALUE_SEND_IBEACON = 1 ;
    public final int VALUE_LOGIN = 2 ;

    static int port = 8766;

    public Server(){
        String Rssi = "", Uuid = "", Major = "", Minor = "";
        String fromClientString;
        String[] data;
        ServerSocket SS = null;
        try {
            SS = new ServerSocket(port);
            System.out.println("Server is created.");
            while(true){
                System.out.println("Waiting for client...");
                Socket serverSocket = SS.accept();
                System.out.println("Connect from = " + serverSocket.getInetAddress().getHostAddress() );
                System.out.println("Client port = " + serverSocket.getPort()) ;

                DataInputStream fromClient = new DataInputStream( serverSocket.getInputStream() );
                //DataOutputStream outToClient = new DataOutputStream( serverSocket.getOutputStream());

                while(serverSocket.isBound()){
                    try{
                        fromClientString = fromClient.readUTF();
                        JSONObject fromClientJSONObject = new JSONObject(fromClientString);
                        switch( fromClientJSONObject.getInt(KEY_STATE) ) {
                            case VALUE_SEND_IBEACON:
                                System.out.println("Rssi = " + fromClientJSONObject.get(KEY_RSSI));
                                System.out.println("Uuid = " + fromClientJSONObject.get(KEY_UUID));
                                System.out.println("Major = " + fromClientJSONObject.get(KEY_MAJOR) + " Minor = " + fromClientJSONObject.get(KEY_MINOR));
                                break;
                            case VALUE_LOGIN:
                                System.out.println("User Name = " + fromClientJSONObject.get(KEY_USER_NAME));
                                System.out.println("PassWord = " + fromClientJSONObject.get(KEY_USER_PWD));
                                break;
                        }
                    }
                    catch (IOException e) {
                        serverSocket.close();
                        break;
                    }
                    catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                System.out.println( "Client has closed the connection." );
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        // TODO Auto-generated method stub
        Server server = new Server();
    }
}

