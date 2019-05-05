
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Scanner;

public class Client {
    
    public static void main(String[] args) {
        System.out.print("Masukkan nama node sekarang : ");
        Scanner scan = new Scanner(System.in);
        String cmd = scan.nextLine();
        MainClass main = new MainClass(cmd);
        main.startAlgorithm();
    }
}

class MainClass {
    
    private final int receiverPort = 8090;
    private String thisNode;
    private ArrayList<Process> neighbor;
    private ThreadEventListener list;
    
    public MainClass(String thisNode) {
        this.thisNode = thisNode;
    }
    
    public void startAlgorithm() {
        boolean isExit = false;
//initializing event listener
        neighbor = new ArrayList<Process>();
        list = new ThreadEventListener() {
            @Override
            public void print(String m) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
                LocalDateTime now = LocalDateTime.now();
                System.out.println(dtf.format(now) + "> " + m);
            }
            
        };
        //assign thread with listener and port
        Thread thread = new Thread(new Listener(list, receiverPort));
        //run the thread
        thread.start();
        list.print("Current ip address : " + NetworkUtil.getCurrentEnvironmentNetworkIp());
        
        while (!isExit) {

//read user input
            Scanner scan = new Scanner(System.in);
            System.out.print("command > ");
            String cmd = scan.nextLine();
            //split each command by whitespce
            String args[] = cmd.split(" ");
            switch (args[0]) {
                case "connect":
                    //check if 3 argument is passed
                    if (args.length > 3) {
                        try {
                            //correct
                            String pName = args[1];
                            String ipAddr = args[2];
                            int cost = Integer.valueOf(args[3]);
                            list.print("Menyambungkan ke " + ipAddr + ":" + receiverPort);
                            //connect to neighbor
                            Socket connect = new Socket(ipAddr, receiverPort);
                            if (connect.isConnected()) {
                                list.print("Berhasil tersambung.");
                                Process p = new Process(pName, connect, cost, thisNode);
                                neighbor.add(p);
                                list.print(p.toString() + " ditambahkan sebagai tetangga");
                            } else {
                                list.print("Gagal tersambung.");
                            }
                        } catch (IOException ex) {
                            list.print("(1a) error " + ex.getMessage());
                        }
                    } else {
                        list.print("(1b) Error ex : connect 192.168.100.16 20");
                    }
                    break;
                case "q": {
                    isExit = true;
                }
                break;
                case "send": {
                    //send to all neighbor
                    if (!neighbor.isEmpty()) {
                        for (Process p : neighbor) {
                            //send to all neighbors
                            send(p);
                        }
                    } else {
                        list.print("Belum ada node tetangga");
                    }
                    
                }
                break;
                default:
                    list.print("Perintah tidak dikenal.");
                    break;
            }
        }
    }
    
    private void send(Process p) {
        //create new thread
        Thread threadSender = new Thread() {
            @Override
            public void run() {
                try {
                    ObjectOutputStream oos = new ObjectOutputStream(p.getSocket().getOutputStream());
                    list.print("Mengirim data ke node " + p.getName());
                    oos.writeObject(neighbor);
                    oos.flush();
                    oos.close();
                } catch (IOException ioe) {
                    list.print("1cbc error " + ioe.getMessage());
                }
            }
            
        };
        threadSender.start();
    }
}

interface ThreadEventListener {
    
    void print(String m);
    
}

class Process implements Serializable {
    
    private String name;
    private Socket s;
    private int cost;
    private String source;
    
    public Process(String name, Socket s, int cost, String src) {
        this.name = name;
        this.s = s;
        this.cost = cost;
        this.source = src;
    }
    
    public String getSource() {
        return source;
    }
    
    public int getCost() {
        return cost;
    }
    
    public String getName() {
        return name;
    }
    
    public Socket getSocket() {
        return s;
    }
    
    @Override
    public String toString() {
        return this.name + s.getInetAddress().getHostAddress() + ":" + s.getPort();
    }
    
}

//Network Util digunakan untuk mendapatkan ip address sekarang dalam jaringan.
final class NetworkUtil {
    
    private static String currentHostIpAddress;
    
    public static String getCurrentEnvironmentNetworkIp() {
        if (currentHostIpAddress == null) {
            Enumeration<NetworkInterface> netInterfaces = null;
            try {
                netInterfaces = NetworkInterface.getNetworkInterfaces();
                
                while (netInterfaces.hasMoreElements()) {
                    NetworkInterface ni = netInterfaces.nextElement();
                    Enumeration<InetAddress> address = ni.getInetAddresses();
                    while (address.hasMoreElements()) {
                        InetAddress addr = address.nextElement();
                        //                      log.debug("Inetaddress:" + addr.getHostAddress() + " loop? " + addr.isLoopbackAddress() + " local? "
                        //                            + addr.isSiteLocalAddress());
                        if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress()
                                && !(addr.getHostAddress().indexOf(":") > -1)) {
                            currentHostIpAddress = addr.getHostAddress();
                        }
                    }
                }
                if (currentHostIpAddress == null) {
                    currentHostIpAddress = "127.0.0.1";
                }
                
            } catch (SocketException e) {
//                log.error("Somehow we have a socket error acquiring the host IP... Using loopback instead...");
                currentHostIpAddress = "127.0.0.1";
            }
        }
        return currentHostIpAddress;
    }
}

/*---------------------------------------SERVER--------------------------------------------*/
class Listener implements Runnable {
    
    private ThreadEventListener ev;
    private int port;
    
    public Listener(ThreadEventListener e, int p) {
        this.ev = e;
        this.port = p;
        ev.print("Receiver listens on " + NetworkUtil.getCurrentEnvironmentNetworkIp() + ":" + port);
    }
    
    @Override
    public void run() {
        ev.print("Listening...");
        boolean listening = true;
        
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (listening) {
                new KKMultiServerThread(serverSocket.accept(), ev).start();
            }
        } catch (IOException e) {
            ev.print("(1f) error Could not listen on port " + port);
            System.exit(-1);
        }
    }
    
}

class KKMultiServerThread extends Thread {
    
    private Socket socket = null;
    private ThreadEventListener ev;
    private ArrayList<Process> neighbor;
    
    public KKMultiServerThread(Socket socket, ThreadEventListener e) {
        super("MultiServer");
        this.socket = socket;
        ev = e;
    }
    
    public void run() {
        
        try {
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            neighbor = (ArrayList<Process>) in.readObject();
            ev.print("node size :" + neighbor.size());
            socket.close();
            in.close();
        } catch (IOException ioe) {
            ev.print("(1e) error " + ioe.getMessage());
        } catch (ClassNotFoundException ex) {
            ev.print("(1e) error " + ex.getMessage());
        }
        
    }
}
