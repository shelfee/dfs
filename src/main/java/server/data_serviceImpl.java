package server;

import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

/**
 * Created by shelfee on 2016/12/11.
 */
public class data_serviceImpl extends UnicastRemoteObject implements data_service {
    public static data_service instance;
    public static final Object lock = new Object();

    public static center_service centerServer = null;
    protected data_serviceImpl(String centerIP) throws RemoteException {
        super();
        String hostName = "rmi://" + centerIP + ":8010/center";
        try {
            Thread.sleep(1100);
            centerServer = (center_service) Naming.lookup(hostName);
            centerServer.addNode(this.getIP(), data_server.keep_port, data_server.service_port);
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public String login(String id) throws RemoteException {

        //System.out.println(data_server.clients);
        try {
            String client = RemoteServer.getClientHost();
            InetAddress ia = InetAddress.getByName(client);
            String clientip = ia.getHostAddress() ;
            if(!data_server.clients.containsKey(clientip + id) && data_server.clients.size() < 5) {
                data_server.clients.put(clientip + id, System.currentTimeMillis());
                int t = centerServer.processLogin(this.getIP() + ":" + data_server.service_port);
                return "LoginTimes:" + String.valueOf(t) + "\nClient IP:" + clientip;
            }
            else {
                if(data_server.clients.size() >= 5)
                    return "Server is full!";
                else
                    return "Repeated Login!";

            }
        } catch (ServerNotActiveException e) {
            e.printStackTrace();
            return "ServerNotActiveException!";
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "UnknownHostException!";
        }


    }

    public boolean exit(String id) throws RemoteException {
        try {
            String client = RemoteServer.getClientHost();
            InetAddress ia = InetAddress.getByName(client);
            String clientip = ia.getHostAddress();

            if(data_server.clients.containsKey(clientip + id)) {
                data_server.clients.remove(clientip + id);
                return true;
            }
            else {
                return false;
            }
        } catch (ServerNotActiveException e) {
            e.printStackTrace();
            return false;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        }

    }

    public boolean removeFile(String fileName, String id) throws RemoteException{
        String clientip = "";
        try {
            String client = RemoteServer.getClientHost();
            InetAddress ip = InetAddress.getByName(client);
            clientip = ip.getHostAddress();
            if(!data_server.clients.containsKey(clientip + id))
                return false;
            else
                data_server.clients.put(clientip + id, 0);
        } catch (ServerNotActiveException e) {
            e.printStackTrace();
            data_server.clients.put(clientip + id, System.currentTimeMillis());
            return false;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            data_server.clients.put(clientip + id, System.currentTimeMillis());
            return false;
        }
        boolean flag = centerServer.removeFile(fileName);
        data_server.clients.put(clientip + id, System.currentTimeMillis());
        return flag;
    }

    public boolean upload(String fileName, byte[] data, String id) throws RemoteException {
        if(data == null)
            return false;
        String clientip = "";

        try {
            String client = RemoteServer.getClientHost();
            clientip = InetAddress.getByName(client).getHostAddress();
            if(!data_server.clients.containsKey(clientip + id))
                return false;
            else
                data_server.clients.put(clientip + id, 0);
        } catch (ServerNotActiveException e) {
            e.printStackTrace();
            data_server.clients.put(clientip + id, System.currentTimeMillis());
            return false;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            data_server.clients.put(clientip + id, System.currentTimeMillis());
            return false;
        }
        long timestamp = centerServer.addFile(fileName, data);
        if(timestamp < 0) {
            data_server.clients.put(clientip + id, System.currentTimeMillis());
            return false;
        }
        fileName = data_server.rootpath + fileName;
        File file = new File(fileName);
        data_server.clients.put(clientip + id, System.currentTimeMillis());
        return true;

    }

    public byte[] download(String fileName, String id) throws RemoteException{
        String clientip = null;
        byte[]data = null;
        try {
            String client = RemoteServer.getClientHost();
            InetAddress ip = InetAddress.getByName(client);
            clientip  = ip.getHostAddress();
            if(!data_server.clients.containsKey(clientip + id))
                return null;
            else
                data_server.clients.put(clientip + id, 0);
        } catch (ServerNotActiveException e) {
            e.printStackTrace();
            return null;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
        long checktime = centerServer.checkFile(fileName);
        File file = new File(fileName);
        if(checktime < 0) {
            file.delete();
            data_server.clients.put(clientip + id, System.currentTimeMillis());
            return data;
        }
        if(file.isFile()){
            try {
                if (checktime != file.lastModified()) {

                    file.delete();
                    data = centerServer.getFile(fileName);
                    util.file_byte.ByteArray2File(data, data_server.rootpath + fileName);
                    file = new File(data_server.rootpath + fileName);
                    file.setLastModified(checktime);
                }
                else{
                    data = util.file_byte.File2ByteArray(data_server.rootpath + fileName);
                }

                data_server.clients.put(clientip + id, System.currentTimeMillis());
                return data;
            } catch (IOException e) {
                e.printStackTrace();
                data_server.clients.put(clientip + id, System.currentTimeMillis());
                return null;
            }

        }
        else{
            data = centerServer.getFile(fileName);
            try {
                util.file_byte.ByteArray2File(data, data_server.rootpath + fileName);
                file = new File(data_server.rootpath + fileName);
                file.setLastModified(checktime);
            } catch (IOException e) {
                e.printStackTrace();
            }
            data_server.clients.put(clientip + id, System.currentTimeMillis());
            return data;
        }
    }


    public String showDir(String id) throws RemoteException {
        String clientip = null;
        try {
            String client = RemoteServer.getClientHost();
            System.out.println(client);
            InetAddress ia = InetAddress.getByName(client);
            clientip  = ia.getHostAddress();
            if(!data_server.clients.containsKey(clientip + id))
                return null;
            else
                data_server.clients.put(clientip + id, 0);
        } catch (ServerNotActiveException e) {
            e.printStackTrace();
            return null;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }

        String s = getDir(0, new File(data_server.rootpath));
        data_server.clients.put(clientip + id, System.currentTimeMillis());
        return s;
    }

    public String getDir(int indent, File file) throws RemoteException {
        StringBuffer s = new StringBuffer();
        for(int i =0;i < indent;i ++)
           s.append("-");
        s.append(file.getName() + "\n");

        if (file.isDirectory()) {
           File[] files = file.listFiles();
           for (int i = 0; i < files.length; i++) {
               s.append(getDir(indent + 4, files[i]));
           }
        }
        return s.toString();
    }

    public String getIP() throws UnknownHostException {
        InetAddress addr = InetAddress.getLocalHost();
        return addr.getHostAddress();
    }

    public Date getTime() {
        return new Date(System.currentTimeMillis());
    }
    public static data_service getInstance(String centerIP)throws RemoteException{
        if (null == instance){
            synchronized (lock){
                instance = new data_serviceImpl(centerIP);
            }
        }
        return instance;
    }
}
