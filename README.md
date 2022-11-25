## 一、项目介绍
项目目的很简单，使用Docker搭建一个Hadoop集群。

此项目可以快速的让我们在本地搭建一个Hadoop集群而且需要的代价很低 *（即CPU、内存和磁盘空间）* 并且让我们可以方便的控制Hadoop集群。

**注意：这个项目只是为了解决在学习Hadoop时搭建 *Hadoop实验集群* 耗费电脑资源的问题。**

**如果你是打算搭建常规使用的Hadoop集群那这个项目并不适合你，项目中的一些操作是会清除HDFS中的数据！！！**

## 二、项目背景
我最开始学习Hadoop是通过在自己电脑的上三台虚拟机来搭建Hadoop集群，这应该也是大部分开始学Hadoop的人搭建实验环境的方式。

用虚拟机搭建Hadoop集群这种方式是可行的但是需要付出的代价就是三台虚拟机占用了我56G的磁盘空间（我虚拟机中除了Hadoop还有kafka、flume、mysql等等）
。但是！56G还是每什么，毕竟现在电脑的磁盘都比较大。让我最难受的是对电脑内存的消耗！！！
我的电脑是16G内存，在学习写MapReduce程序时电脑同时开3台虚拟机、IDEA和edge内存直接跑满。

在我学习了Docker后我发现Docker可以很好的解决上面的痛点，所以产生了这个项目。

## 三、使用方法
> 以下命令都是在当前项目位置下执行
> 
> 一定要按步骤执行，不然很容易出现问题

### 1. 准备Java和Hadoop
> 可点此下载
> [hadoop-3.1.3.tar.gz](http://archive.apache.org/dist/hadoop/core/hadoop-3.1.3/hadoop-3.1.3.tar.gz)，
> [jdk-8u171-linux-x64.tar.gz](https://repo.huaweicloud.com/java/jdk/8u171-b11/jdk-8u171-linux-x64.tar.gz)
> 将下载好的 *hadoop-3.1.3.tar.gz* 和 *jdk-8u171-linux-x64.tar.gz* 两个文件放入当前项目的lib文件夹中
> 
> 如果使用其他版本的hadoop和JDK需要在Dockerfile进行修改，否则构建后的镜像会有问题

### 2. 使用Dockerfile构建镜像
```shell
# 在当前项目目录下执行以下命令构架镜像
docker build -t hadoop:1.0 .
```
### 3. 新建一个docker网络
```shell
docker network create hadoop
```

### 4. 打包本项目
```shell
mvn pakage
```

### 5. 初始化环境
```shell
# 执行以下命令初始化环境
java -cp ./target/hadoop-for-docker.jar org.levitan.ContainerManager init
```

### 6. 创建master容器
```shell
java -cp ./target/hadoop-for-docker.jar org.levitan.ContainerManager master
```

### 7. 创建指定数量的slave容器
```shell
# 以下以创建5个slave容器为例
java -cp ./target/hadoop-for-docker.jar org.levitan.ContainerManager slave 5
```
### 8. 添加集群的IP映射
```shell
# 以下指令会向/etc/hosts文件中添加Hadoop集群的IP映射
# 执行请前先将/etc/hosts文件的权限改为666（使用root用户执行 chomd 666 /etc/hosts）
java -cp ./target/hadoop-for-docker.jar org.levitan.HostsManager load
```

> 到此为止你的电脑上就有了由1个主节点和5个从节点构成的Hadoop集群
> 
> > 后续只需要进入master容器（`docker exec -it master /bin/bash`）格式化名称节点（`hdfs namenode -format`）就可以开启Hadoop集群

## 四、其他命令
### 停止Hadoop集群容器
```shell
java -cp ./target/hadoop-for-docker.jar org.levitan.ContainerManager stop
```

### 开启Hadoop集群容器
```shell
java -cp ./target/hadoop-for-docker.jar org.levitan.ContainerManager start
```

### 删除所有Hadoop集群容器
> 注意！！！这里不仅会删除docker中的Hadoop集群容器还会删除Hadoop集群在本地的数据
```shell
java -cp ./target/hadoop-for-docker.jar org.levitan.ContainerManager removeall
```

### 删除Hadoop集群容器的IP映射
```shell
java -cp ./target/hadoop-for-docker.jar org.levitan.HostsManager clear
```