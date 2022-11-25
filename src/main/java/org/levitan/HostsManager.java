package org.levitan;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HostsManager {
    private final String FILEPATH = "/etc/hosts";
    private final String STAR = "# The following IPs are automatically managed by the program";
    private final String END = "# The above IP is a hadoop cluster";

    private final RandomAccessFile randomFile;


    public static void main(String[] args) {
        if(args[0].equals("load")) {
            HostsManager hostsManager = new HostsManager();
            hostsManager.addLimit();
            hostsManager.loadContainerIDFile();
            hostsManager.close();
        } else if (args[0].equals("clear")) {
            HostsManager hostsManager = new HostsManager();
            hostsManager.clear();
            hostsManager.close();
        }
    }

    public HostsManager() {
        try {
            randomFile = new RandomAccessFile(FILEPATH, "rw");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void addLimit() {
        try {
            randomFile.seek(randomFile.length());
            randomFile.writeBytes("\n\n\n");
            randomFile.writeBytes(STAR+"\n");
            randomFile.writeBytes(END);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addIP(String host) {
        try {
            long seek = search(END)-END.length();
            randomFile.setLength(seek);
            randomFile.writeBytes(host+"\n");
            randomFile.writeBytes(END);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @param str:需要查找的字符串
     * @return
     */
    public long search(String str) {
        long seek = -1;
        try {
            randomFile.seek(0);
            String line;
            while ((line=randomFile.readLine()) != null) {
                if (line.contains(str)) {
                    seek = randomFile.getFilePointer();
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return seek;
    }

    public void clear() {
        long seek = search(STAR)-STAR.length();
        try {
            randomFile.setLength(seek-4);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            randomFile.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadContainerIDFile() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("CONTAINERID"));
            String line;
            while ((line = reader.readLine()) != null) {
                String name = line.split(";")[0];
                String ID = line.split(";")[1];
                String IP = getIP(ID);
                addIP(IP+" "+name);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    System.err.println("关闭流时出现异常");
                }
            }
        }
    }

    private static String getIP(String containerID) throws IOException {
        String reg = "([0-9]{1,3}(.?)){3}[0-9]";
        Pattern pattern = Pattern.compile(reg);

        String[] cmd = {"/bin/bash", "-c", "docker inspect "+containerID};
        Process process = Runtime.getRuntime().exec(cmd);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line=reader.readLine()) != null) {
            if(line.contains("IPAddress")) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    return matcher.group();
                }
            }
        }
        return null;
    }
}
