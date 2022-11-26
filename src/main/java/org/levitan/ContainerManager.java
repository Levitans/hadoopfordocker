package org.levitan;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ContainerManager {
    private final static String home;
    private final static String imageName;
    private final static String masterHostname;
    private final static String slaveHostname;
    private final static String containerHadoopHome;
    private final static String networkName;
    private final static String containerCreateCMD;

    static {
        // 读取配置文件
        try {
            Properties properties = new Properties();
            properties.load(ContainerManager.class.getClassLoader().getResourceAsStream("container.properties"));
            home = properties.getProperty("dataVolumeHome");
            imageName = properties.getProperty("imageName");
            masterHostname = properties.getProperty("masterNodeHostname");
            slaveHostname = properties.getProperty("slaveNodeHostname");
            containerHadoopHome = properties.getProperty("containerHadoopHome");
            networkName = properties.getProperty("networkName");
            containerCreateCMD = properties.getProperty("containerCreatCMD");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("传入参数为空！");
            System.exit(0);
        }
        switch (args[0]) {
            case "init":
                if (isInit()) {
                    System.out.println("请勿重复初始化");
                    return;
                }
                exec(new String[]{"/bin/bash", "-c", "mkdir -p "+ home +"/etc"});
                exec(new String[]{"/bin/bash", "-c", "cp -r ./lib/etc "+ home});
                break;
            case "master":      // 创建master容器
                if (!isInit()) {
                    System.out.println("还未初始化！！！");
                    return;
                }
                if (getHosts().contains(masterHostname)) {
                    System.out.println("当前系统中已经有"+masterHostname+"容器\n" +
                            "所以本次操作没有创建新的"+masterHostname+"容器");
                    return;
                }
                try(FileWriter containerIDWriter = new FileWriter("CONTAINERID", true)) {
                    String ID = createContainer(masterHostname);
                    containerIDWriter.write(masterHostname+";"+ID+"\n");
                } catch (IOException e) {
                    System.err.println("创建爱你slave容器时出现异常："+e.getLocalizedMessage());
                }
                break;
            case "slave":    // 创建slave容器
                if(!isInit()) {
                    System.out.println("还未初始化！！！");
                    return;
                }
                if(getHosts().contains(slaveHostname+"1")) {
                    System.out.println("当前系统中已经有"+slaveHostname+"容器\n" +
                            "所以本次操作没有创建新的"+slaveHostname+"容器");
                    return;
                }
                int number = Integer.parseInt(args[1]);
                String workersPath = exec(new String[]{"/bin/bash", "-c", "cd "+home+";pwd"})+"/etc/hadoop/workers";
                try(
                    FileWriter workersWriter = new FileWriter(workersPath);     // 向workers文件中写入从节点
                    FileWriter containerIDWriter = new FileWriter("CONTAINERID", true)  // 向CONTAINERID文件中写入从节点信息
                ) {
                    for (int i = 0; i < number; i++) {
                        String hostname = slaveHostname + (i + 1);
                        String ID = createContainer(hostname);
                        containerIDWriter.write(hostname+";"+ID+"\n");
                        workersWriter.write(hostname+"\n");
                    }
                } catch (IOException e) {
                    System.err.println("创建slave容器时出现异常："+e.getLocalizedMessage());
                }
                break;
            case "start":   // 开启所有容器
                for(String name : getHosts()) {
                    exec(new String[]{"/bin/bash", "-c", "docker start "+name});
                }
                break;
            case "stop":    // 关闭所有容器
                for(String name : getHosts()) {
                    exec(new String[]{"/bin/bash", "-c", "docker stop "+name});
                }
                break;
            case "removeall":   // 删除所有容器以及容器在本地的数据
                for(String name : getHosts()) {
                    exec(new String[]{"/bin/bash", "-c", "docker rm -f "+name});
                }
                exec(new String[]{"/bin/bash", "-c", "rm -rf "+ home});
                clearContainerIDFile();     // 清空CONTAINERID文件中的内容
                break;
        }
    }

    public static String createContainer(String hostname) throws IOException {
        String localHome = home+"/"+hostname;
        Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", "mkdir -p "+localHome+"/tmp"+" "+localHome+"/logs"});
        String cmd = String.format(containerCreateCMD,
                networkName, hostname, hostname,
                home, containerHadoopHome,
                localHome, containerHadoopHome,
                localHome, containerHadoopHome,
                imageName);
        System.out.println("使用以下命令创建 "+hostname+" 容器：\n"+cmd);
        String[] cmdList = {"/bin/bash", "-c", cmd};
        return exec(cmdList);
    }

    /**
     * 执行shell命令并返回命令打印的信息
     * @param cmdList 需要执行的命令
     * @return 返回命令打印的信息
     */
    public static String exec(String[] cmdList) {
        BufferedReader rb = null;
        try {
            Process process = Runtime.getRuntime().exec(cmdList);
            rb = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line=rb.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
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

    private static void clearContainerIDFile() {
        try(FileWriter fileWriter = new FileWriter("CONTAINERID")) {
            fileWriter.write("");
        } catch (Exception e) {
            System.err.println("清除ContainerID文件时出现异常");
        }
    }

    private static List<String> getHosts() {
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

    public static boolean isInit() {
        // 如果文件存在则返回1不存在返回0
        String localHome = exec(new String[]{"/bin/bash", "-c", "ls "+home+" &>/dev/null && echo 1 || echo 0"});
        return localHome.equals("1");
    }
}