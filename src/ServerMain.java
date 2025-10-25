import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;

class UserDataStructure{
    public String password;
}

class FileWriter{
    public static HashMap<String, HashMap<String, String>> table = new HashMap<>();

    // Required referrances:
    public static final ObjectMapper jsonmapper = new ObjectMapper();
    public static final File database = new File("Database");
    public static final File datatable = new File("Database/table.json");

    public static synchronized void dataWriter(String username, HashMap<String, String> data){
        if (data != null) table.put(username, data);
    }

    public static synchronized String fileReader(String username){
        HashMap<String, String> regduser = table.get(username);

        if (regduser != null) return regduser.get("password");
        
        try{
            HashMap<String, UserDataStructure> userdata = jsonmapper.readValue(datatable, 
            jsonmapper.getTypeFactory().constructMapType(
                HashMap.class, String.class, UserDataStructure.class
            ));

            for (String name: userdata.keySet()){
                regduser = new HashMap<>();
                regduser.put("password", userdata.get(name).password);
                table.put(username, regduser);
            }

            UserDataStructure user = userdata.get(username);

            if (user != null) return user.password;
        }
        catch (IOException e){
            e.printStackTrace();
        }

        return null;
    }

    public static void fileWriter(){

        try{
            jsonmapper.writerWithDefaultPrettyPrinter().writeValue(datatable, table);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void dirInit(){
        if (! database.exists()) database.mkdirs();
    }
}

class ClientConnection extends Thread{
    private Scanner input;
    private PrintWriter output;
    private Socket client;
    private String username;

    @Override
    public void run(){
        System.out.println("Client communication initiated: " + this.client.getInetAddress());
        String pswd;
        username = input.nextLine();
        ClientData clientdata = new ClientData();

        while (true){
            System.out.println("Waiting for request...");
            String request;

            try{
                request = input.nextLine();
            }
            catch (NoSuchElementException e){
                System.out.println("Client disconnected normally: " + client.getInetAddress());
                System.out.println((e.getMessage()));

                break;
            }
            catch (IllegalStateException e){
                System.out.println("Client disconnected unexpectedly: " + client.getInetAddress());
                System.out.println(e.getMessage());

                break;
            }

            System.out.println("Requested: " + request);

            if (request.equals("EXIT")){
                FileWriter.dataWriter(username, clientdata.getData());
                System.out.println("Client disconnected: " + this.client.getInetAddress());

                break;
            }

            switch (request){
                case "STOREREGISTERINFO":

                if (FileWriter.fileReader(username) != null){
                    output.println("~ACC-EXISTS");
                    continue;
                }

                else output.println("~OK");

                pswd = input.nextLine();
                clientdata.dataSetter(username, pswd);
                System.out.println("Client data stored successfully: " + client.getInetAddress());
                break;
                
                case "GETLOGININFO":
                HashMap<String, String> data = clientdata.getData();

                if (data == null){
                    String getpswd = FileWriter.fileReader(username);

                    if (getpswd == null) output.println("~NO-ACC-FOUND");

                    else output.println(getpswd);
                }

                else output.println(data.get("password"));
                System.out.println("Client data sent successfully: " + client.getInetAddress());
                break;
            }
        }
    }

    ClientConnection(Socket client) throws Exception{
        this.client = client;
        this.input = new Scanner(this.client.getInputStream());
        this.output = new PrintWriter(this.client.getOutputStream(), true);
    }
}

public class ServerMain{
    
    public static void main(String args[]){
        // Finally:
        // The data stored in the FileWriter.table will be written into the database
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            FileWriter.fileWriter();
        }));
        FileWriter.dirInit();
        
        try (ServerSocket server = new ServerSocket(5500)){
            System.out.println("Server started at port: 5500");

            while (true){
                System.out.println("Waiting for clients to connect...");
                Socket client = server.accept();
                System.out.println("Client connected: " + client.getInetAddress());

                ClientConnection connection = new ClientConnection(client);
                connection.start();
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
