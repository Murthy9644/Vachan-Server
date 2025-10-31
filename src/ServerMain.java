import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    public static synchronized void dataSetter(String username, HashMap<String, String> data){
        if (data != null) table.put(username, data);
    }

    public static synchronized HashMap<String, HashMap<String, String>> allDataGetter(){
        return table;
    }

    public static synchronized String specFileReader(String username){
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

    public static void fileReader(){

        try{
            HashMap<String, UserDataStructure> userdata = jsonmapper.readValue(datatable, 
            jsonmapper.getTypeFactory().constructMapType(
                HashMap.class, String.class, UserDataStructure.class
            ));

            for (String name: userdata.keySet()){
                HashMap<String, String> regduser = new HashMap<>();
                regduser.put("password", userdata.get(name).password);
                table.put(name, regduser);
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
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

        else fileReader();
    }
}

class ClientConnection extends Thread{
    private Scanner input;
    private ObjectInputStream objectinput;
    private PrintWriter output;
    private ObjectOutputStream objectoutput;
    private Socket client;
    private String username;

    private void closeThings(){
        try{
            this.input.close();
            this.objectinput.close();
            this.output.close();
            this.objectoutput.close();
            this.client.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

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
                this.closeThings();

                break;
            }
            catch (IllegalStateException e){
                System.out.println("Client disconnected unexpectedly: " + client.getInetAddress());
                System.out.println(e.getMessage());
                this.closeThings();

                break;
            }

            System.out.println("Requested: " + request);

            if (request.equals("EXIT")){
                System.out.println("Client disconnected: " + this.client.getInetAddress());
                this.closeThings();

                break;
            }

            switch (request){
                case "STOREREGISTERINFO":

                if (FileWriter.specFileReader(username) != null){
                    output.println("~ACC-EXISTS");
                    continue;
                }

                else output.println("~OK");

                pswd = input.nextLine();
                clientdata.dataSetter(username, pswd);
                FileWriter.dataSetter(username, clientdata.getData());
                System.out.println("Client data stored successfully: " + client.getInetAddress());
                break;
                
                case "GETLOGININFO":
                HashMap<String, String> data = clientdata.getData();

                if (data == null){
                    String getpswd = FileWriter.specFileReader(username);

                    if (getpswd == null) output.println("~NO-ACC-FOUND");

                    else output.println(getpswd);
                }

                else output.println(data.get("password"));
                System.out.println("Client data sent successfully: " + client.getInetAddress());
                break;

                case "GETUSERNAMES":
                String searchstr = input.nextLine();
                String[] searchset = FileWriter.allDataGetter().keySet().toArray(new String[0]);
                List<String> searchres = new ArrayList<>();

                for (String str: searchset){

                    if (str.toLowerCase().startsWith(searchstr.toLowerCase())){
                        searchres.add(str);
                    }
                }

                searchres.sort(String::compareToIgnoreCase);
                String searchres_arr[] = searchres.toArray(new String[0]);

                try{
                    objectoutput.writeObject(searchres_arr);
                    objectoutput.flush();
                }
                catch (IOException e){
                    e.printStackTrace();
                    output.println("~ERROR");
                }
            }
        }
    }

    ClientConnection(Socket client) throws Exception{
        this.client = client;
        this.input = new Scanner(this.client.getInputStream());
        this.objectinput = new ObjectInputStream(this.client.getInputStream());
        this.output = new PrintWriter(this.client.getOutputStream(), true);
        this.objectoutput = new ObjectOutputStream(this.client.getOutputStream()); // Need to use flush().
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
