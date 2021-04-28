package com.github.distribute.lock.redis;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

/**
 * redis?????????? 
 * @author linbingwen
 *
 */
public class OptimisticLockTest {

	public static void main(String[] args) throws InterruptedException {
		 long starTime=System.currentTimeMillis();
		
		 initPrduct();
		 initClient();
		 printResult();
		 
		long endTime=System.currentTimeMillis();
		long Time=endTime-starTime;
		System.out.println("???¨°?????¡À???? "+Time+"ms");   

	}
	
	/**
	 * ?????¨¢??
	 */
	public static void printResult() {
		Jedis jedis = RedisUtil.getInstance().getJedis();
		Set<String> set = jedis.smembers("clientList");

		int i = 1;
		for (String value : set) {
			System.out.println("??" + i++ + "?????????¡¤??"+value + " ");
		}

		RedisUtil.returnResource(jedis);
	}

	/*
	 * ???????????????????¡¤
	 */
	public static void initClient() {
		ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
		int clientNum = 10000;// ???????¡ì????
		for (int i = 0; i < clientNum; i++) {
			cachedThreadPool.execute(new ClientThread(i));
		}
		cachedThreadPool.shutdown();
		
		while(true){  
	            if(cachedThreadPool.isTerminated()){  
	                System.out.println("?¨´???????????¨¢??????");  
	                break;  
	            }  
	            try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}    
	        }  
	}

	/**
	 * ?????????¡¤????
	 */
	public static void initPrduct() {
		int prdNum = 100;// ???¡¤????
		String key = "prdNum";
		String clientList = "clientList";// ?????????¡¤????????¡À¨ª
		Jedis jedis = RedisUtil.getInstance().getJedis();

		if (jedis.exists(key)) {
			jedis.del(key);
		}
		
		if (jedis.exists(clientList)) {
			jedis.del(clientList);
		}

		jedis.set(key, String.valueOf(prdNum));// ??????
		RedisUtil.returnResource(jedis);
	}

}

/**
 * ????????
 * 
 * @author linbingwen
 *
 */
class ClientThread implements Runnable {
	Jedis jedis = null;
	String key = "prdNum";// ???¡¤?¡Â?¨¹
	String clientList = "clientList";//// ?????????¡¤????????¡À¨ª?¡Â?¨¹
	String clientName;

	public ClientThread(int num) {
		clientName = "¡À¨¤??=" + num;
	}

	public void run() {
		try {
			Thread.sleep((int)(Math.random()*5000));// ???¨²????????
		} catch (InterruptedException e1) {
		}
		while (true) {
			System.out.println("????:" + clientName + "?????????¡¤");
			jedis = RedisUtil.getInstance().getJedis();
			try {
				jedis.watch(key);
				int prdNum = Integer.parseInt(jedis.get(key));// ?¡À?¡ã???¡¤????
				if (prdNum > 0) {
					Transaction transaction = jedis.multi();
					transaction.set(key, String.valueOf(prdNum - 1));
					List<Object> result = transaction.exec();
					if (result == null || result.isEmpty()) {
						System.out.println("¡À???????????:" + clientName + "???????????¡¤");// ??????watch-key¡À????????????¨°??????????¡Á¡Â¡À?????
					} else {
						jedis.sadd(clientList, clientName);// ???????¡¤????????
						System.out.println("????????????:" + clientName + "???????¡¤");
						break;
					}
				} else {
					System.out.println("¡À?????????????0??????:" + clientName + "???????????¡¤");
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				jedis.unwatch();
				RedisUtil.returnResource(jedis);
			}

		}
	}

}
