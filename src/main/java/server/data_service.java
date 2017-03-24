package server;
import java.io.*;
import java.net.UnknownHostException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;

/**
 * Created by shelfee on 2016/12/11.
 */
public interface data_service extends Remote{
    public String login(String id)throws RemoteException;
    public boolean exit(String id) throws RemoteException;
    public boolean removeFile(String fileName, String id)throws RemoteException;
    public boolean upload(String fileName, byte[]data, String id)throws RemoteException;
    public byte[] download(String fileName,String id)throws RemoteException;
    public String showDir(String id)throws RemoteException;
    public Date getTime() throws RemoteException;
}
