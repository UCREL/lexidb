package uk.ac.lancs.ucrel.server;

import uk.ac.lancs.ucrel.corpus.CorpusAccessor;
import uk.ac.lancs.ucrel.file.system.FileUtils;
import uk.ac.lancs.ucrel.parser.TextParser;
import uk.ac.lancs.ucrel.rmi.result.InsertResult;
import uk.ac.lancs.ucrel.rmi.result.KwicResult;
import uk.ac.lancs.ucrel.rmi.result.Result;
import uk.ac.lancs.ucrel.rmi.Server;
import uk.ac.lancs.ucrel.sort.FrequencyComparator;
import uk.ac.lancs.ucrel.sort.LexicalComparator;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerImpl implements Server {

    public boolean shutdown = false;
    private Date startTime;
    private String dataPath;
    private CorpusAccessor ca;
    private List<int[]> last;
    private int lastPos, lastContext, lastPageLength, lastSortPos, lastSortType, regexMatches, nextPeer;
    private long lastTime;
    private String lastSearchTerm;
    private ExecutorService es = Executors.newCachedThreadPool();
    private InsertResult lastInsert;
    private Path rawTempPath;
    private Properties props;
    private Map<String, Server> peers;
    private boolean available = false;

    public ServerImpl(Properties props) {
        this.startTime = new Date();
        this.props = props;
        dataPath = props.getProperty("server.data.path");
        peers = new HashMap<String, Server>();
        es.execute(() -> connectToPeers());
        try {
            ca = new CorpusAccessor(Paths.get(dataPath));
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public void setAvailable(boolean available){
        this.available = available;
    }

    public boolean isAvailable(){
        return available;
    }

    public void notify(String server) throws RemoteException {
        try {
            System.out.println("Notification received: " + server);
            if(!peers.keySet().contains(server))
                connectToPeer(server, false);
        } catch (Exception e){
            System.err.println("Could not connect to peer " + server + ": " + e.getMessage());
        }
    }

    private void connectToPeer(String server, boolean notify) throws RemoteException, NotBoundException {
        String[] bits = server.split(":");
        String host = bits[0];
        int port = Integer.parseInt(bits[1]);
        Registry r = LocateRegistry.getRegistry(host, port);
        Remote tmp = r.lookup("serv");
        Server s = null;
        if (tmp instanceof Server)
            s = (Server) tmp;
        if (s != null && !this.equals(s)) {
            peers.put(server, s);
            if(notify)
                s.notify(props.getProperty("server.host") + ":" + props.getProperty("server.port"));
            System.out.println("Connected to new peer: " + server);
        }
    }

    private void connectToPeers() {
        String[] peerList = props.getProperty("server.peers").split(" ");
        while(!available){
            try {
                Thread.sleep(1000);
            } catch (Exception e){

            }
        }
        for (String server : peerList) {
            try {
                connectToPeer(server, true);
            } catch (Exception e) {
                System.out.println("Could not connect to peer " + server);
            }
        }
        while(available){
            try {
                Thread.sleep(5000);
            } catch (Exception e){

            }
            for(String server : peers.keySet()){
                Server s = peers.get(server);
                try {
                    if (!s.isAvailable()) {
                        peers.remove(server);
                        System.out.println(server + " no longer avaialbe.");
                    }
                } catch (Exception e){
                    peers.remove(server);
                    System.out.println("Connection to " + server + " lost.");
                }
            }
        }
    }

    public Result status() throws RemoteException {
        StringBuilder sb = new StringBuilder();
        sb.append("Server status:\n\n");
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String name = "unknown";
        try {
            name = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {

        }
        sb.append("\tstarted:\t").append(df.format(startTime)).append("\n");
        sb.append("\thostname:\t").append(name).append("\n");
        String wordCount = NumberFormat.getInstance().format(ca.getWordCount());
        sb.append("\twords:\t\t").append(wordCount).append("\n");
        String typeCount = NumberFormat.getInstance().format(ca.getWordTypeCount());
        sb.append("\ttypes:\t\t").append(typeCount);
        return new Result(sb.toString());
    }

    public boolean isShutdown() throws RemoteException {
        return shutdown;
    }

    public void shutdown() throws RemoteException {
        shutdown = true;
        es.shutdown();
    }

    public void insertRun(Path p) {
        System.out.println("Inserting from " + p.toString());
        try {
            lastInsert = new InsertResult(".", false);
            long start = System.currentTimeMillis();
            TextParser tp = new TextParser(Paths.get(dataPath));
            tp.parse(p);
            ca = new CorpusAccessor(Paths.get(dataPath));
            long end = System.currentTimeMillis();
            lastTime = (end - start);
            lastInsert = new InsertResult("\nInserted completed in " + (end - start) + "ms.", true);
        } catch (Exception e) {
            List<String> errors = new ArrayList<String>();
            errors.add(e.getMessage());
            errors.add(e.getCause().getMessage());
            lastInsert = new InsertResult("\nInsert failed!", true);
        }
    }

    public boolean sendRaw(String filename, byte[] data) throws RemoteException {
        try {
            if (rawTempPath == null)
                rawTempPath = Files.createTempDirectory("discodb_raw");
            FileUtils.write(Paths.get(rawTempPath.toString(), filename), data);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean distributeRaw() throws RemoteException {
        try {
            Files.walkFileTree(rawTempPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String[] peerList = peers.keySet().toArray(new String[0]);
                    nextPeer++;
                    nextPeer = nextPeer % (peerList.length + 1);
                    if (nextPeer < peerList.length) {
                        Server s = peers.get(peerList[nextPeer]);
                        s.sendRaw(file.getFileName().toString(), Files.readAllBytes(file));
                        Files.delete(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            return true;
        } catch (Exception e){
            return false;
        }
    }

    public InsertResult insert() throws RemoteException {
        for(String server : peers.keySet()){
            peers.get(server).insertLocal();
        }
        insertLocal();
        return lastInsert;
    }

    public InsertResult insertLocal() throws RemoteException {
        System.out.println("Inserting local files");
        es.execute(() -> insertRun(rawTempPath));
        lastInsert = new InsertResult("\nInserting. Please wait...", false);
        return lastInsert;
    }

    public InsertResult lastInsert() throws RemoteException {
        return lastInsert;
    }

    public Result kwic(String searchTerm, int context, int limit, int sortType, int sortPos, int order, int pageLength) throws RemoteException {
        System.out.println("Search for " + searchTerm);
        try {
            long start = System.currentTimeMillis();
            this.lastSearchTerm = searchTerm;
            this.lastContext = context;
            this.lastTime = sortType;
            this.lastSortType = sortType;
            this.lastSortPos = sortPos;
            this.lastPageLength = pageLength;
            if (!isRegex(searchTerm)) {
                last = searchResults(searchTerm, context, limit);
                regexMatches = 0;
            } else {
                last = regexResults(searchTerm, context, limit);
            }
            lastPos = 0;
            sortResults(sortType, sortPos, order, context);
            long end = System.currentTimeMillis();
            lastTime = end - start;
            return getResults();

        } catch (Exception e) {
            e.printStackTrace();
            return new Result("Failed to find \"" + searchTerm + "\": " + e.getMessage());
        }
    }

    private boolean isRegex(String s) {
        return s.matches("^.*[^a-zA-Z ].*$");
    }

    private void sortResults(int sortType, int sortPos, int order, int context) {
        if (sortType == 0)
            return;
        else if (sortType == 1) {
            Collections.sort(last, new LexicalComparator(context, sortPos));
        } else if (sortType == 2) {
            Collections.sort(last, new FrequencyComparator(context, sortPos, last));
        }

        if (order < 0)
            Collections.reverse(last);
    }

    public Result it() throws RemoteException {
        return getResults();
    }

    private List<int[]> searchResults(String keyword, int context, int limit) {
        try {
            return ca.search(keyword, context, limit);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<int[]>();
        }
    }

    private List<int[]> regexResults(String regex, int context, int limit) {
        try {
            List<String> matches = ca.regex(regex);
            regexMatches = matches.size();
            return ca.search(matches, context, limit);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<int[]>();
        }
    }

    private String getSortString() {
        StringBuilder sb = new StringBuilder();
        if (lastSortType == 0)
            return sb.toString();
        sb.append(" (sorted ");
        if (lastSortType == 1)
            sb.append("lexically ");
        else if (lastSortType == 2)
            sb.append("by frequency ");
        sb.append("on position ").append(lastSortPos).append(")");
        return sb.toString();
    }

    private String getRegexString() {
        StringBuilder sb = new StringBuilder();
        if (regexMatches > 0)
            sb.append(" (regex matched ").append(regexMatches).append(" word types)");
        return sb.toString();
    }

    private Result getResults() {
        List<String> page = new ArrayList<String>();
        for (int i = lastPos; i < last.size() && i < (lastPos + lastPageLength); i++) {
            page.add(ca.getLineAsString(last.get(i)));
        }
        lastPos += lastPageLength;
        String header = "Found " + NumberFormat.getInstance().format(last.size()) + " results for \"" + lastSearchTerm + "\"";
        header += getRegexString();
        header += getSortString();
        return new KwicResult(header, lastTime, page, (lastPos - lastPageLength + 1), lastPos, last.size(), lastContext);
    }

    public Properties getProperties() throws RemoteException {
        return props;
    }

    public boolean equals(Server s) throws RemoteException {
        boolean sameHost = props.getProperty("server.host").equals(s.getProperties().getProperty("server.host"));
        boolean samePort = props.getProperty("server.port").equals(s.getProperties().getProperty("server.port"));
        return sameHost && samePort;
    }
}
