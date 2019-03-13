package gradleTest.zookeepertest;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.junit.Before;
import org.junit.Test;

import com.alibaba.fastjson.JSONObject;

public class SampleZkClient {

	private static final String connectString = "10.3.22.57:2181";
    private static final int sessionTimeout = 2000;

    /** 信号量，阻塞程序执行，用于等待zookeeper连接成功，发送成功信号 */
    /*一旦不加锁，会因为连接zookeeper需要10s，而程序执行需要5s，故程序执行到向zookeeper节点写数据时
    ，zookeeper还没有连接上，因此程序而报错
    */
    static final CountDownLatch connectedSemaphore = new CountDownLatch(1);
    ZooKeeper zkClient = null;
    Stat stat = new Stat();

    @Before
    public void testInit() throws Exception{
        zkClient = new ZooKeeper(connectString, sessionTimeout, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                // 获取事件的状态
                Event.KeeperState keeperState = event.getState();
                Event.EventType eventType = event.getType();
                // 如果是建立连接
                if (Event.KeeperState.SyncConnected == keeperState) {
                    if (Event.EventType.None == eventType) {
                        // 如果建立连接成功，则发送信号量，让后续阻塞程序向下执行
                        System.out.println("zk 建立连接");
                        connectedSemaphore.countDown();
                    }
                }
            }
        });

        // 进行阻塞
        connectedSemaphore.await();
        System.out.println("..");
    }
    
    /**
     * 创建节点
     * @throws KeeperException
     * @throws InterruptedException
     * 一、节点类型
     * 1.PERSISTENT                持久化节点
     * 2.PERSISTENT_SEQUENTIAL     顺序自动编号持久化节点，这种节点会根据当前已存在的节点数自动加 1
     * 3.EPHEMERAL                 临时节点， 客户端session超时这类节点就会被自动删除
     * 4.EPHEMERAL_SEQUENTIAL      临时自动编号节点
     * 二、
     * 
     */
    @Test
    public void testCreate() throws KeeperException, InterruptedException {
        // 参数1：要创建的节点的路径 参数2：节点大数据 参数3：节点的权限 参数4：节点的类型
        String nodeCreated = zkClient.create("/pms.wuliusys.com/mysql_prod_rl", "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        //上传的数据可以是任何类型，但都要转成byte[]
        System.err.println(nodeCreated);
    }
    
    /**
     * 获取所有父节点名称
     * @throws Exception
     */
    @Test
    public void getChildren() throws Exception {
        List<String> children = zkClient.getChildren("/", true);
        for (String child : children) {
            System.out.println(child);
        }
        Thread.sleep(Long.MAX_VALUE);
        zkClient.close();
    }
    
    /**
     * 获取某个节点数据
     * @throws Exception
     */
    @Test
    public void getNodeData() throws Exception {
    	String host = new String(zkClient.getData("/pms.wuliusys.com/oracle_prod_rl_host", true, null));
    	String username = new String(zkClient.getData("/pms.wuliusys.com/oracle_prod_rl_userName", true, null));
    	String password = new String(zkClient.getData("/pms.wuliusys.com/oracle_prod_rl_password", true, null));
    	System.out.println(host);
    	System.out.println(username);
    	System.out.println(password);
    }
    
    /**
     * 给节点添加数据
     * @throws Exception
     */
    @Test
    public void setNodeData() throws Exception {
    	JSONObject json = new JSONObject();
    	json.put("url", "jdbc:mysql://10.230.4.240:3307/seller?autoReconnect=true&allowMultiQueries=true&useUnicode=true&characterEncoding=UTF-8");
    	json.put("username", "seller_prog");
    	json.put("password", "d67349db");
    	json.put("initialSize", 10);
    	json.put("maxActive", 100);
    	json.put("maxWait", 60000);
    	json.put("testWhileIdle", true);
    	json.put("timeBetweenEvictionRunsMillis", 60000);
    	json.put("minEvictableIdleTimeMillis", 25200000);
    	json.put("removeAbandoned", true);
    	json.put("removeAbandonedTimeout", 1800);
    	json.put("logAbandoned", true);
    	json.put("logAbandoned", true);
    	json.put("poolPreparedStatements", "false");
    	json.put("connectionProperties", "config.decrypt=false");
    	zkClient.setData("/pms.wuliusys.com/mysql_prod_rl", json.toJSONString().getBytes(),-1);
    }
    
    /**
     * 设置节点权限
     * @throws Exception
     */
    @Test
    public void setNodeAcl() throws Exception {
    	zkClient.setACL("/test1", ZooDefs.Ids.OPEN_ACL_UNSAFE, -1);
    }
    
    
}
