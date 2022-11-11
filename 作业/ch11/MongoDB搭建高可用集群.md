# MongoDB搭建高可用集群

## 1.下载社区版 MongoDB 4.1.3

下载地址：https://www.mongodb.com/download-center#community

```
wget https://fastdl.mongodb.org/linux/mongodb-linux-x86_64-rhel70-4.1.3.tgz
```

## 2.将压缩包解压即可
```
mkdir /usr/local/hero/
tar -zxvf mongodb-linux-x86_64-rhel70-4.1.3.tgz -C /usr/local/hero/
```

```
vim /etc/profile

export MONGO_HOME=/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3
export PATH=$MONGO_HOME/bin:$PATH

source /etc/profile
```

## 3. 复制集搭建 

### 配置脚本

```sh
# 初始化集群数据文件存储目录和日志文件
mkdir -p /data/mongo/logs
mkdir -p /data/mongo/data/server1
mkdir -p /data/mongo/data/server2
mkdir -p /data/mongo/data/server3


touch /data/mongo/logs/server1.log
touch /data/mongo/logs/server2.log
touch /data/mongo/logs/server3.log

# 创建集群配置文件目录
mkdir /root/mongocluster
```

#### 1）主节点配置 mongo_37017.conf  

```properties
tee /root/mongocluster/mongo_37017.conf <<-'EOF'
# 主节点配置
dbpath=/data/mongo/data/server1
bind_ip=0.0.0.0
port=37017
fork=true
logpath=/data/mongo/logs/server1.log
# 集群名称
replSet=heroMongoCluster
EOF
```

#### 2）从节点1配置 mongo_37018.conf  

```properties
tee /root/mongocluster/mongo_37018.conf <<-'EOF'
dbpath=/data/mongo/data/server2
bind_ip=0.0.0.0
port=37018
fork=true
logpath=/data/mongo/logs/server2.log
replSet=heroMongoCluster
EOF
```

#### 3）从节点2配置 mongo_37019.conf  

```properties
tee /root/mongocluster/mongo_37019.conf <<-'EOF'
dbpath=/data/mongo/data/server3
bind_ip=0.0.0.0
port=37019
fork=true
logpath=/data/mongo/logs/server3.log
replSet=heroMongoCluster
EOF
```

#### 4）初始化节点配置

启动集群脚本

```bash
tee /root/mongocluster/start-mongo-cluster.sh <<-'EOF'
#! /bin/bash
clear
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongod -f /root/mongocluster/mongo_37017.conf
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongod -f /root/mongocluster/mongo_37018.conf
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongod -f /root/mongocluster/mongo_37019.conf 
echo "start mongo cluster..."
ps -ef | grep mongodb
EOF

chmod 755 /root/mongocluster/start-mongo-cluster.sh
```

****关闭集群脚本****

```bash
tee /root/mongocluster/stop-mongo-cluster.sh <<-'EOF'
#! /bin/bash
clear
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongod --shutdown -f /root/mongocluster/mongo_37017.conf
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongod --shutdown -f /root/mongocluster/mongo_37018.conf
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongod --shutdown -f /root/mongocluster/mongo_37019.conf
echo "stop mongo cluster..."
ps -ef | grep mongodb
EOF
chmod 755 /root/mongocluster/stop-mongo-cluster.sh
```

#### 5）初始化集群命令

启动三个节点 然后进入Primary 节点 运行如下命令：  

```sh
mongo --host=192.168.56.10 --port=37017
mongo --host=192.168.56.10 --port=37018
mongo --host=192.168.56.10 --port=37019
```

```sql
var cfg ={"_id":"heroMongoCluster",
            "protocolVersion" : 1,
            "members":[
                {"_id":1,"host":"192.168.56.10:37017","priority":10},
                {"_id":2,"host":"192.168.56.10:37018"}
            ]
        }
rs.initiate(cfg)
rs.slaveOk()
```

#### 6）测试复制集

```SQL
rs.status()
```

两个节点的状态如下：

![](.\图片\复制集17、18.png)

增加节点

```sql
# 增加节点
rs.add("192.168.56.10:37019")
```

![](.\图片\增加19节点.png)

删除节点

```sql
# 删除slave 节点
rs.remove("192.168.56.10:37019")
```

![](.\图片\删除19节点.png)

进入主节点并从插入数据（37017）

```
show dbs
use hero
db.goods.insertMany([
   { item: "journal", qty: 25, size: { h: 14, w: 21, uom: "cm" }, status: "A" },
   { item: "notebook", qty: 50, size: { h: 8.5, w: 11, uom: "in" }, status: "A" },
   { item: "paper", qty: 100, size: { h: 8.5, w: 11, uom: "in" }, status: "D" },
   { item: "planner", qty: 75, size: { h: 22.85, w: 30, uom: "cm" }, status: "D" },
   { item: "postcard", qty: 45, size: { h: 10, w: 15.25, uom: "cm" }, status: "A" },
   { item: "postcard", qty: 55, size: { h: 10, w: 15.25, uom: "cm" }, status: "C" }
]);
```

![](.\图片\17节点主节点增加数据.png)

从节点查看数据

```
show dbs
use hero
db.goods.find()
```

![](.\图片\18从节点查看数据.png)

## 4. 分片集群的搭建过程  

```sh
# 初始化集群数据文件存储目录和日志文件
mkdir -p /data/mongo/config1
mkdir -p /data/mongo/config2
mkdir -p /data/mongo/config3
# 初始化日志文件
touch /data/mongo/logs/config1.log
touch /data/mongo/logs/config2.log
touch /data/mongo/logs/config3.log
# 创建集群配置文件目录
mkdir /root/mongoconfig
# 创建配置文件
```

### 1）配置并启动config 节点集群 

节点1 config-17017.conf  

```properties
tee /root/mongoconfig/config-17017.conf <<-'EOF'
# 数据库文件位置
dbpath=/data/mongo/config1
#日志文件位置
logpath=/data/mongo/logs/config1.log
# 以追加方式写入日志
logappend=true
# 是否以守护进程方式运行
fork = true
bind_ip=0.0.0.0
port = 17017
# 表示是一个配置服务器
configsvr=true
#配置服务器副本集名称
replSet=configsvr
EOF
```

节点2 config-17018.conf  

```properties
tee /root/mongoconfig/config-17018.conf <<-'EOF'
# 数据库文件位置
dbpath=/data/mongo/config2
#日志文件位置
logpath=/data/mongo/logs/config2.log
# 以追加方式写入日志
logappend=true
# 是否以守护进程方式运行
fork = true
bind_ip=0.0.0.0
port = 17018
# 表示是一个配置服务器
configsvr=true
#配置服务器副本集名称
replSet=configsvr
EOF
```

节点3 config-17019.conf  

```properties
tee /root/mongoconfig/config-17019.conf <<-'EOF'
# 数据库文件位置
dbpath=/data/mongo/config3
#日志文件位置
logpath=/data/mongo/logs/config3.log
# 以追加方式写入日志
logappend=true
# 是否以守护进程方式运行
fork = true
bind_ip=0.0.0.0
port = 17019
# 表示是一个配置服务器
configsvr=true
#配置服务器副本集名称
replSet=configsvr
EOF
```

启动集群脚本

```bash
tee /root/mongoconfig/start-mongo-config.sh <<-'EOF'
#! /bin/bash
clear
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongod -f /root/mongoconfig/config-17017.conf
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongod -f /root/mongoconfig/config-17018.conf
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongod -f /root/mongoconfig/config-17019.conf
echo "start mongo config cluster..."
ps -ef | grep mongodb
EOF
chmod 755 /root/mongoconfig/start-mongo-config.sh
```

关闭集群脚本

```bash
tee /root/mongoconfig/stop-mongo-config.sh  <<-'EOF'
#! /bin/bash
clear
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongod --shutdown -f /root/mongoconfig/config-17017.conf
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongod --shutdown -f /root/mongoconfig/config-17018.conf
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongod --shutdown -f /root/mongoconfig/config-17019.conf
echo "stop mongo config cluster..."
ps -ef | grep mongodb
EOF
chmod 755 /root/mongoconfig/stop-mongo-config.sh
```

配置节点集群

```bash
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongo --host=192.168.56.10 --port=17017
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongo --host=192.168.56.10 --port=17018
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongo --host=192.168.56.10 --port=17019
```

```sql
use admin
var cfg ={"_id":"configsvr",
        "members":[
            {"_id":1,"host":"192.168.56.10:17017"},
            {"_id":2,"host":"192.168.56.10:17018"},
            {"_id":3,"host":"192.168.56.10:17019"}]
        };
rs.initiate(cfg)
rs.status()
```

![](.\图片\config集群.png)

### 2）配置 shard1和shard2集群

##### shard1集群搭建37017到37019

```sh
# 1）初始化集群数据文件存储目录和日志文件
mkdir -p /data/mongo/datashard/server1
mkdir -p /data/mongo/datashard/server2
mkdir -p /data/mongo/datashard/server3

mkdir /data/mongo/logs/datashard
touch /data/mongo/logs/datashard/server1.log
touch /data/mongo/logs/datashard/server2.log
touch /data/mongo/logs/datashard/server3.log

mkdir /root/mongoshard

# 2）主节点配置 mongo_37017.conf
tee /root/mongoshard/mongo_37017.conf <<-'EOF'
# 主节点配置
dbpath=/data/mongo/datashard/server1
bind_ip=0.0.0.0
port=37017
fork=true
logpath=/data/mongo/logs/datashard/server1.log
# 集群名称
replSet=shard1
shardsvr=true
EOF

# 2）从节点1配置 mongo_37018.conf  
tee /root/mongoshard/mongo_37018.conf <<-'EOF'
dbpath=/data/mongo/datashard/server2
bind_ip=0.0.0.0
port=37018
fork=true
logpath=/data/mongo/logs/datashard/server2.log
replSet=shard1
shardsvr=true
EOF

# 3）从节点2配置 mongo_37019.conf
tee /root/mongoshard/mongo_37019.conf <<-'EOF'
dbpath=/data/mongo/datashard/server3
bind_ip=0.0.0.0
port=37019
fork=true
logpath=/data/mongo/logs/datashard/server3.log
replSet=shard1
shardsvr=true
EOF
```

启动集群脚本

```bash
tee /root/mongoshard/start-mongo-shard1.sh <<-'EOF'
#! /bin/bash
clear
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongod -f /root/mongoshard/mongo_37017.conf
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongod -f /root/mongoshard/mongo_37018.conf
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongod -f /root/mongoshard/mongo_37019.conf 
echo "start mongo shard1 cluster..."
ps -ef | grep mongodb
EOF

chmod 755 /root/mongoshard/start-mongo-shard1.sh
```

关闭集群脚本

```bash
tee /root/mongoshard/stop-mongo-shard1.sh <<-'EOF'
#! /bin/bash
clear
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongod --shutdown -f /root/mongoshard/mongo_37017.conf
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongod --shutdown -f /root/mongoshard/mongo_37018.conf
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongod --shutdown -f /root/mongoshard/mongo_37019.conf
echo "stop mongo shard1 cluster..."
ps -ef | grep mongodb
EOF
chmod 755 /root/mongoshard/stop-mongo-shard1.sh
```

初始化集群命令

启动三个节点 然后进入 Primary 节点 运行如下命令：  

```sh
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongo --host=192.168.56.10 --port=37017
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongo --host=192.168.56.10 --port=37018
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongo --host=192.168.56.10 --port=37019
```

```sql
var cfg ={"_id":"shard1",
            "protocolVersion" : 1,
            "members":[
                {"_id":1,"host":"192.168.56.10:37017"},
                {"_id":2,"host":"192.168.56.10:37018"},
                {"_id":3,"host":"192.168.56.10:37019"}
            ]
        }
rs.initiate(cfg)
rs.slaveOk()
rs.status()
```

![](.\图片\shard1集群.png)

##### shard2集群搭建47017到47019

```sh
# 1）初始化集群数据文件存储目录和日志文件
mkdir -p /data/mongo/datashard/server4
mkdir -p /data/mongo/datashard/server5
mkdir -p /data/mongo/datashard/server6

touch /data/mongo/logs/datashard/server4.log
touch /data/mongo/logs/datashard/server5.log
touch /data/mongo/logs/datashard/server6.log

# 2）主节点配置 mongo_47017.conf
tee /root/mongoshard/mongo_47017.conf <<-'EOF'
# 主节点配置
dbpath=/data/mongo/datashard/server4
bind_ip=0.0.0.0
port=47017
fork=true
logpath=/data/mongo/logs/datashard/server4.log
# 集群名称
replSet=shard2
shardsvr=true
EOF

# 2）从节点1配置 mongo_47018.conf  
tee /root/mongoshard/mongo_47018.conf <<-'EOF'
dbpath=/data/mongo/datashard/server5
bind_ip=0.0.0.0
port=47018
fork=true
logpath=/data/mongo/logs/datashard/server5.log
replSet=shard2
shardsvr=true
EOF

# 3）从节点2配置 mongo_47019.conf
tee /root/mongoshard/mongo_47019.conf <<-'EOF'
dbpath=/data/mongo/datashard/server6
bind_ip=0.0.0.0
port=47019
fork=true
logpath=/data/mongo/logs/datashard/server6.log
replSet=shard2
shardsvr=true
EOF
```

启动集群脚本

```bash
tee /root/mongoshard/start-mongo-shard2.sh <<-'EOF'
#! /bin/bash
clear
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongod -f /root/mongoshard/mongo_47017.conf
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongod -f /root/mongoshard/mongo_47018.conf
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongod -f /root/mongoshard/mongo_47019.conf 
echo "start mongo shard2 cluster..."
ps -ef | grep mongodb
EOF

chmod 755 /root/mongoshard/start-mongo-shard2.sh
```

关闭集群脚本

```bash
tee /root/mongoshard/stop-mongo-shard2.sh <<-'EOF'
#! /bin/bash
clear
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongod --shutdown -f /root/mongoshard/mongo_47017.conf
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongod --shutdown -f /root/mongoshard/mongo_47018.conf
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongod --shutdown -f /root/mongoshard/mongo_47019.conf
echo "stop mongo shard2 cluster..."
ps -ef | grep mongodb
EOF
chmod 755 /root/mongoshard/stop-mongo-shard2.sh
```

初始化集群命令

启动三个节点 然后进入 Primary 节点 运行如下命令：  

```sh
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongo --host=192.168.56.10 --port=47017
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongo --host=192.168.56.10 --port=47018
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongo --host=192.168.56.10 --port=47019
```

```sql
var cfg ={"_id":"shard2",
            "protocolVersion" : 1,
            "members":[
                {"_id":1,"host":"192.168.56.10:47017"},
                {"_id":2,"host":"192.168.56.10:47018"},
                {"_id":3,"host":"192.168.56.10:47019"}
            ]
        }
rs.initiate(cfg)
rs.slaveOk()
rs.status()
```

![](.\图片\shard2集群.png)

### 3）配置和启动路由节点

```sh
touch /data/mongo/logs/route.log
```

route-27017.conf

```properties
tee /root/mongoshard/route-27017.conf <<-'EOF'
port=27017
bind_ip=0.0.0.0
fork=true
logpath=/data/mongo/logs/route.log
configdb=configsvr/192.168.56.10:17017,192.168.56.10:17018,192.168.56.10:17019
EOF
```

启动路由节点使用 mongos （注意不是mongod）  

```sh
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongos -f /root/mongoshard/route-27017.conf
```

![](.\图片\路由设置.png)

### 4）mongos（路由）中添加分片节点

进入路由mongos  节点

```sh
/usr/local/hero/mongodb-linux-x86_64-rhel70-4.1.3/bin/mongo --port 27017
```

```
sh.status()
```

![](.\图片\初始状态.png)

```sql
sh.addShard("shard1/192.168.56.10:37017,192.168.56.10:37018,192.168.56.10:37019");
sh.addShard("shard2/192.168.56.10:47017,192.168.56.10:47018,192.168.56.10:47019");
sh.status()
```

![](.\图片\增加分片后的状态.png)

### 5）开启数据库和集合分片(指定片键) 

继续使用mongos完成分片开启和分片大小设置

```sql
# 为数据库开启分片功能
use admin
db.runCommand( { enablesharding :"myRangeDB"});
# 为指定集合开启分片功能
db.runCommand( { shardcollection : "myRangeDB.coll_shard",key : {_id: 1} } )
```

向集合中插入数据测试

通过路由循环向集合中添加数  

```sql
use myRangeDB;
for(var i=1;i<= 1000;i++){
    db.coll_shard.insert({"name":"test"+i,salary:(Math.random()*20000).toFixed(2)});
}
```

![](.\图片\分片方式1.png)

使用hash分片

```sql
use admin
db.runCommand({"enablesharding":"myHashDB"})
db.runCommand({"shardcollection":"myHashDB.coll_shard","key":{"_id":"hashed"}})
```

```sql
use myHashDB;
for(var i=1;i<= 1000;i++){
    db.coll_shard.insert({"name":"test"+i,salary:(Math.random()*20000).toFixed(2)});
}
```

查看分片情况

```sql
use myHashDB;
db.coll_shard.find();
```

![](.\图片\hash分片shard1.png)

![](，

### 6）验证分片效果

代码验证

```
use admin
db.runCommand({"enablesharding":"mytest"})
db.runCommand({"shardcollection":"mytest.employee","key":{"_id":"hashed"}})
```

