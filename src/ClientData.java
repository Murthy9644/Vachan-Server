import java.util.HashMap;

public class ClientData{

    // Global things for this class:
    public String username;
    public String pswd;
    private HashMap<String, String> userdata;
    
    public void dataWrite(){
        this.userdata.put("password", this.pswd);
    }

    public HashMap<String, String> getData(){
        return this.userdata;
    }

    public void dataSetter(String username, String pswd){
        this.userdata = new HashMap<>();
        this.username = username;
        this.pswd = pswd;

        this.dataWrite();
    }
    
    ClientData(String username, String pswd){        
        this.username = username;
        this.pswd = pswd;

        this.dataWrite();
    }

    ClientData(){
        this.username = null;
        this.pswd = null;
        this.userdata = null;
    }
}