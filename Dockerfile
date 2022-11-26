FROM centos:7.8.2003
MAINTAINER levitan<Levitan1165@outlook.com>

# root用户密码
ARG rootPwd=hadoop123
# hadoop用户密码
ARG hadoopPwd=123456

# java归档文件在主机中的路径
ARG javaLocalPath=./lib/jdk-8u171-linux-x64.tar.gz
# java归档文件解压后的名字
ARG javaAfterName=jdk1.8.0_171

# hadoop归档文件在主机中的路径
ARG hadoopLocalPath=./lib/hadoop-3.1.3.tar.gz
# hadoop归档文件解压后的名字
ARG hadoopAfterName=hadoop-3.1.3

# 创建hadoop用户以及修改root用户和hadoop用户的密码
RUN echo "root:${rootPwd}" | chpasswd root && \
    useradd hadoop && \
    echo "hadoop:${hadoopPwd}" | chpasswd hadoop

# 设置中文环境
ENV LC_ALL=en_US.UTF-8
ENV LANG=en_US.UTF-8

# 设置Continer时区
ENV TZ Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ENV MYPATH /usr/local
WORKDIR $MYPATH


# 安装vim编辑器 net-tools SSH服务 java8需要的lib库(glibc.i686)
RUN yum -y install vim && \
    yum -y install net-tools && \
    yum -y install openssl openssh-server openssh-clients && \
    yum -y install glibc.i686 && \
    mkdir /usr/local/java


# 配置SSH服务
RUN ssh-keygen -q -t rsa -b 2048 -f /etc/ssh/ssh_host_rsa_key -N '' && \
    ssh-keygen -q -t ecdsa -f /etc/ssh/ssh_host_ecdsa_key -N '' && \
    ssh-keygen -t dsa -f /etc/ssh/ssh_host_ed25519_key -N '' && \
    echo "#以下参数是Docker构建镜像时自动配置的" &&\
    echo "Port 22" >> /etc/ssh/ssh_config && \
    echo "ListenAddress 0.0.0.0" >> /etc/ssh/sshd_config && \
    echo "ListenAddress ::" >> /etc/ssh/sshd_config && \
    chown hadoop /etc/ssh/* && \
    echo -n "#!" >> /etc/profile.d/ssh-start.sh && \
    echo "/bin/bash" >> /etc/profile.d/ssh-start.sh && \
    echo "/usr/sbin/sshd -D" >> /etc/profile.d/ssh-start.sh
# 生成hadoop用户密钥，并配置自身免密登陆
RUN su hadoop -c "ssh-keygen -t rsa -f ~/.ssh/id_rsa -N ''" 2> /dev/null && \
    su hadoop -c "cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys" && \
    su hadoop -c "chmod 644 ~/.ssh/authorized_keys"

# 添加jdk
ADD ${javaLocalPath} /usr/local/java/
# 配置java环境变量
ENV JAVA_HOME /usr/local/java/${javaAfterName}
ENV PATH $PATH:$JAVA_HOME/bin

# 添加hadoop-3.1.3
ADD ${hadoopLocalPath} /usr/local
# 配置hadoop环境变量
ENV HADOOP_HOME /usr/local/${hadoopAfterName}
ENV PATH $PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin

# 将hadoop的所有者和所属组修改为hadoop用户
RUN chown -R hadoop $HADOOP_HOME && \
			chgrp -R hadoop $HADOOP_HOME

# 指定Hadoop用户执行该镜像
USER hadoop

CMD /bin/bash
