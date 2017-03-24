package server;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.*;

/**
 * Created by shelfee on 2016/12/11.
 */
public class center implements Serializable{
    public static Map<String, Integer> NodeLoad = Collections.synchronizedMap(new HashMap<String, Integer>());
    public static Map<String, data_server_keeper> Keeper = Collections.synchronizedMap(new HashMap<String, data_server_keeper>());
    public static HashSet<String[]>RemoteMachines = new HashSet<String[]>();
    public static Map<String, String[]>id_keys = new HashMap<String, String[]>();
    public static Map<String, String>port_map = new HashMap<String, String>();
    public static int LoginTimes = 0;
    public static void init(){
        try {
                Scanner getConf = new Scanner(new File("nodes.conf"));
                int i = 0;
                while(getConf.hasNext()) {
                    i ++;
                    String line = getConf.nextLine();
                String[] ele1 = line.split(",");
                if(ele1.length == 5) {
                    RemoteMachines.add(ele1);
                    String[] e1 = {ele1[1], ele1[2]};
                    String k = ele1[0] + ":" + ele1[4];
                    port_map.put(k, ele1[3]);
                    id_keys.put(k, e1);
                }
            }
            System.out.println("Read Data Server configration:" + String.valueOf(i) + " data servers available.");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    public static void readLog(){
        File centerDir = new File("center/");
        if (!centerDir.exists()){
            centerDir.mkdirs();
        }
        File log = new File("logintime.log");
        if(log.isFile()){
            try {

                Scanner loginput = new Scanner(log);
                LoginTimes = loginput.nextInt();
                System.out.println("Recover the history login times from log file:" + String.valueOf(LoginTimes) + "times.");
                loginput.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{

            PrintWriter wr = null;
            try {
                wr = new PrintWriter(new File("logintime.log"));
                wr.printf("%d", LoginTimes);
                wr.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }
    }
    public static void main(String argv[]) {
        init();
        InetAddress addr = null;
        File centerDir = new File("center/");
        readLog();
        try {
            addr = InetAddress.getLocalHost();
            String ip = addr.getHostAddress();//获得本机Ip
            int port = 8010;
            String hostName = "rmi://" + ip + ":" + String.valueOf(port) + "/center";
            LocateRegistry.createRegistry(port);
            center_service centerService = center_serviceImpl.getInstance();
            Naming.rebind(hostName, centerService);
            int i = 0;
            while(true){

                //maintain data_server size open and close
                if(i % 10 == 0){

                    Iterator<Map.Entry<String ,Integer>> ki = NodeLoad.entrySet().iterator();
                    LinkedList<String> freeNodes = new LinkedList<String>();
                    while(ki.hasNext()){
                        Map.Entry<String ,Integer> entry = ki.next();
                        if(entry.getValue() == 0){
                            freeNodes.add(entry.getKey());
                        }
                    }
                    if(freeNodes.size() > 1){
                        freeNodes.removeLast();
                        String deleteip = null;
                        for(Iterator<String> ni = freeNodes.iterator(); ni.hasNext();) {
                            deleteip = ni.next();
                            centerService.closeNode(deleteip);
                        }
                    }
                    if(freeNodes.size() == 0){
                        centerService.openNode();
                    }
                    System.out.println("The data servers' load:" + NodeLoad.toString());
                }
                //heartbeat test
                Iterator<Map.Entry<String, data_server_keeper>> it = Keeper.entrySet().iterator();
                while(it.hasNext()){
                    Map.Entry<String, data_server_keeper> entry = it.next();
                    try {
                        entry.getValue().alive();
                        NodeLoad.put(entry.getKey(), entry.getValue().serverLoad());
                    }catch (Exception e){
                        System.out.println(entry.getKey() + " dead!");
                        it.remove();
                        NodeLoad.remove(entry.getKey());
                    }
                }
                //synchronzie file
                if(i % 10 == 1){
                    it = Keeper.entrySet().iterator();
                    File[]files = centerDir.listFiles();
                    HashMap<String, Long>centerFiles = new HashMap<String, Long>();
                    for(File file: files){
                        centerFiles.put(file.getName(), file.lastModified());
                    }
                    while(it.hasNext()){
                        Map.Entry<String, data_server_keeper> entry = it.next();
                        if(!centerService.synchronizeNode(entry.getValue())){
                            System.out.println(entry.getKey() + " dead!");
                            it.remove();
                            NodeLoad.remove(entry.getKey());
                        }
                    }
                }
                i = (i + 1) % 1000000;
                Thread.sleep(1000);
            }
        }catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
