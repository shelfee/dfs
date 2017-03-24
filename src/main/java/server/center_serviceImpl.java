package server;
import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static server.center.LoginTimes;
import static server.center.NodeLoad;

/**
 * Created by shelfee on 2016/12/11.
 */
public class center_serviceImpl extends UnicastRemoteObject implements center_service{


    public static center_service instance;
    public static final Object lock = new Object();


    public center_serviceImpl() throws RemoteException {
        super();
    }


    public synchronized int processLogin(String serverHost) throws RemoteException {

        if(NodeLoad.get(serverHost) != null){
            int load = NodeLoad.get(serverHost);
            NodeLoad.put(serverHost, load + 1);
            LoginTimes += 1;
            //save load
            try {
                PrintWriter pw = null;
                pw = new PrintWriter(new File("logintime.log"));
                pw.printf("%d", LoginTimes);
                pw.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            return LoginTimes;
        }
        return -1;
    }

    public boolean addNode(String hostip, String keep, String service) throws RemoteException {
        String service_socket = hostip + ":" + service;
        String keep_socket = hostip + ":" + keep;
        if(NodeLoad.containsKey(service_socket) || center.Keeper.containsKey(service_socket)){
            return false;
        }
        else{
            String hostName = "rmi://" + keep_socket +"/keeper";
            try {
                data_server_keeper keeper = (data_server_keeper) Naming.lookup(hostName);
                synchronizeNode(keeper);
                center.Keeper.put(service_socket, keeper);
                NodeLoad.put(service_socket, 0);
                System.out.println("New data server supply service:" + service_socket);
            } catch (NotBoundException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            return true;
        }
    }

    public long addFile(String fileName, byte[]data) throws RemoteException {
        long time = System.currentTimeMillis();
        try {
            if(util.file_byte.ByteArray2File(data,"center/" + fileName)){
                synchronized (lock) {
                    File file = new File("center/" + fileName);
                    file.setLastModified(time);
                }
                Iterator<Map.Entry<String, data_server_keeper>> it = center.Keeper.entrySet().iterator();
                while(it.hasNext()){
                    Map.Entry<String, data_server_keeper> entry = it.next();
                    try {
                        entry.getValue().alive();
                        entry.getValue().addFile(fileName, data, time);
                    }catch (Exception e){
                        System.out.println(entry.getKey() + " dead!");
                        it.remove();
                        NodeLoad.remove(entry.getKey());
                    }
                }
                return time;
            }
            else
                return -1;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }

    }

    public boolean removeFile(String fileName) throws RemoteException {
        Iterator<Map.Entry<String, data_server_keeper>> it = center.Keeper.entrySet().iterator();
        boolean flag = true;
        while(it.hasNext()){
            Map.Entry<String, data_server_keeper> entry = it.next();
            try {
                entry.getValue().alive();
                flag = flag && entry.getValue().removeFile(fileName);
            }catch (Exception e){
                System.out.println(entry.getKey() + " dead!");
                it.remove();
                NodeLoad.remove(entry.getKey());
            }
        }

        synchronized (lock) {
            File file = new File("center/" + fileName);
            if(!file.exists())
                return false;
            file.delete();
            center.Keeper.remove(fileName);
            return true;
        }
    }

    public long checkFile(String fileName) throws RemoteException {
        File file = new File("center/" + fileName);
//        System.out.println(file.getAbsolutePath());
//        System.out.println(file.isFile());
        if(file.isFile()){
            return file.lastModified();
        }
        return -1;
    }

    public byte[] getFile(String fileName) throws RemoteException {
        return util.file_byte.File2ByteArray("center/" + fileName);
    }


    public String allocate() throws RemoteException {
        Iterator<Map.Entry<String, Integer>> it = center.NodeLoad.entrySet().iterator();
        boolean tag = false;
        String k = "";
        while(it.hasNext()){
            Map.Entry<String, Integer>entry = it.next();
            if(entry.getValue() < 5){
                tag = true;
                k = entry.getKey();
                if(entry.getValue() > 0)
                    return entry.getKey();
            }
        }
        if(tag)
            return k;
        return null;
    }

    public boolean synchronizeNode(data_server_keeper keeper) throws RemoteException {
        if(keeper == null)
            return false;
        try {
            HashMap<String, Long> report = keeper.reportFiles();
            for (Map.Entry<String, Long> entry1 : report.entrySet()) {
                File tf = new File("center/" + entry1.getKey());
                if (tf.isFile()) {
                    if (tf.lastModified() != entry1.getValue()) {
                        keeper.removeFile(entry1.getKey());
                        byte[] data = util.file_byte.File2ByteArray("center/" + entry1.getKey());
                        keeper.addFile(entry1.getKey(), data, tf.lastModified());
                    }
                } else {
                    keeper.removeFile(entry1.getKey());
                }
            }
            File centerDir = new File("center/");
            for (File file : centerDir.listFiles()) {
                if (!report.containsKey(file.getName())) {
                    byte[] data = util.file_byte.File2ByteArray("center/" + file.getName());
                    //System.out.println(file.getName());
                    keeper.addFile(file.getName(), data, file.lastModified());
                }

            }
            return true;
        }catch (Exception e){
            return false;
        }
    }

    public void openNode() throws RemoteException {
        Iterator<String[]>it = center.RemoteMachines.iterator();
        if(it.hasNext()) {
            String[]machine = it.next();
            util.RemoteExecuteCommand rm = new util.RemoteExecuteCommand(machine[0], machine[1], machine[2]);
            InetAddress addr = null;
            try {
                addr = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            String ip = addr.getHostAddress();//获得本机Ip
            //System.out.println(rm.executeSuccess("ls"));
            String cmd = "java -jar data_server.jar " + ip + " " + machine[3] + " " + machine[4];
            System.out.println("localip:" + ip);
            System.out.println(cmd);
            System.out.println("Call:" + machine[0]);
            rm.execute(cmd);
            it.remove();
        }
    }

    public synchronized boolean closeNode(String place) throws RemoteException {
        data_server_keeper keeper = center.Keeper.get(place);
        if(keeper == null)
            return false;
        int l = center.NodeLoad.get(place);
        center.Keeper.remove(place);
        center.NodeLoad.remove(place);
        String []entry = new String[5];
        entry[0] = place.split(":")[0];
        entry[4] = place.split(":")[1];
        entry[1] = center.id_keys.get(place)[0];
        entry[2] = center.id_keys.get(place)[1];
        entry[3] = center.port_map.get(place);
        if(keeper.closeNode()){

            center.RemoteMachines.add(entry);
            return true;
        }
        else{
            center.Keeper.put(place, keeper);
            center.NodeLoad.put(place, l);
            return false;
        }
    }

    public synchronized boolean alive(String hostip, String keep, String service) throws RemoteException {
        if(!center.NodeLoad.containsKey(hostip))
            addNode(hostip, keep, service);
        return true;
    }

    public static center_service getInstance()throws RemoteException{
        if (null == instance){
            synchronized (lock){
                instance = new center_serviceImpl();
            }
        }
        return instance;
    }
}
