package org.levitan;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ContainerManager {
    private final static String HOME = System.getProperty("user.home")+"/docker/hadoop/";
    private final static String IMAGESNAME = "hadoop:1.0";

    public static void main(String[] args) throws IOException {
        switch (args[0]) {
            case "init":
                exec(new String[]{"/bin/bash", "-c", "mkdir -p "+HOME+"etc/"});
                exec(new String[]{"/bin/bash", "-c", "cp -r ./lib/etc "+HOME});
                break;
            case "master":      // 创建master容器
                if(getHost().contains("master")) {
                    System.out.println("当前系统中已经有master容器\n" +
                            "所以本次操作没有创建新的master容器");
                    return;
                }
                saveContainerID("master", createMaster());
                break;
            case "slave":    // 创建slave容器
                if(getHost().contains("slave1")) {
                    System.out.println("当前系统中已经有slave容器\n" +
                            "所以本次操作没有创建新的slave容器");
                    return;
                }
                int number = Integer.parseInt(args[1]);
                try(FileWriter writer = new FileWriter(HOME+"etc/hadoop/workers")) {
                    for (int i = 0; i < number; i++) {
                        String name = "slave" + (i + 1);
                        saveContainerID(name, createSlave(name));
                        writer.write(name+"\n");
                    }
                } catch (IOException e) {
                    System.err.println("创建slave容器时出现异常："+e.getLocalizedMessage());
                }
                break;
            case "start":
                for(String name : getHost()) {
                    exec(new String[]{"/bin/bash", "-c", "docker start "+name});
                }
                break;
            case "stop":
                for(String name : getHost()) {
                    exec(new String[]{"/bin/bash", "-c", "docker stop "+name});
                }
                break;
            case "removeall":
                for(String name : getHost()) {
                    exec(new String[]{"/bin/bash", "-c", "docker rm -f "+name});
                    exec(new String[]{"/bin/bash", "-c", "rm -rf "+HOME+name});
                }
                clearContainerIDFile();
                break;
        }
    }

    public static String createMaster() throws IOException {
        String tmp = HOME + "master/tmp/dfs";
        String logs = HOME + "master/logs";
        Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", "mkdir -p " + tmp + " " + logs});
        String cmd = "docker run -d --network hadoop --name master -h master " +
                "-v " + HOME + "etc/hadoop:/usr/local/hadoop-3.1.3/etc/hadoop " +
                "-v " + HOME + "master/tmp:/usr/local/hadoop-3.1.3/tmp " +
                "-v " + HOME + "master/logs:/usr/local/hadoop-3.1.3/logs " +
                IMAGESNAME;
        String[] cmdList = {"/bin/bash", "-c", cmd};
        return exec(cmdList);
    }

    public static String createSlave(String name) throws IOException {
        String tmp = HOME + name + "/tmp";
        String logs = HOME + name + "/logs";
        Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", "mkdir -p " + tmp + " " + logs});

        String slaveCMD = "docker run -d --network hadoop --name " + name + " -h "+name+ " " +
                "-v " + HOME + "etc/hadoop:/usr/local/hadoop-3.1.3/etc/hadoop " +
                "-v " + HOME + name + "/tmp:/usr/local/hadoop-3.1.3/tmp " +
                "-v " + HOME + name + "/logs:/usr/local/hadoop-3.1.3/logs " +
                IMAGESNAME;
        String[] cmdList = {"/bin/bash", "-c", slaveCMD};
        return exec(cmdList);
    }

    /**
     * 执行shell命令
     * @param cmdList
     * @return
     */
    public static String exec(String[] cmdList) {
        BufferedReader rb = null;
        try {
            Process process = Runtime.getRuntime().exec(cmdList);
            rb = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return rb.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (rb != null) {
                try{
                    rb.close();
                } catch (IOException e) {
                    System.err.println("流关闭失败");
                }
            }
        }
    }

    /**
     * 将容器的名字和ID保存到文件中
     * @param name
     * @param ID
     */
    public static void saveContainerID(String name, String ID) {
        FileWriter writer = null;
        try {
            writer = new FileWriter("CONTAINERID", true);
            writer.write(name+";"+ID+"\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    System.err.println("关闭流时出现异常");
                }
            }
        }
    }

    private static void clearContainerIDFile() {
        try(FileWriter fileWriter = new FileWriter("CONTAINERID")) {
            fileWriter.write("");
        } catch (Exception e) {
            System.err.println("清除ContainerID文件时出现异常");
        }
    }

    private static List<String> getHost() {
        List<String> hosts = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("CONTAINERID"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                hosts.add(line.split(";")[0]);
            }
        } catch (IOException e) {
            System.err.println("读取ContainerID时出现异常");
        }
        return hosts;
    }
}