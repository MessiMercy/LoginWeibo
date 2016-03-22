package com.LoginWeibo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import sun.misc.BASE64Encoder;

public class Weibo {

	public static void main(String[] args) throws IOException {
		CookieStore store = new BasicCookieStore();
		CloseableHttpClient client = com.DemoTest.getInstanceClient().setDefaultCookieStore(store).build();
		String preLoginUrl = "http://login.sina.com.cn/sso/prelogin.php?"
				+ "entry=weibo&callback=sinaSSOController.preloginCallBack&"
				+ "su=&rsakt=mod&client=ssologin.js(v1.4.18)&_=" + System.currentTimeMillis();
		HttpGet get = new HttpGet(preLoginUrl);
		get.setHeader("Referer", "http://weibo.com/login.php");// get登录预处理页面，获得相关参数。
		CloseableHttpResponse response = client.execute(get);
		String preLoginHtml = EntityUtils.toString(response.getEntity(), "utf-8");
		// response.close();
		String preLoginJson = htmlToJson(preLoginHtml);
		JsonElement json = new JsonParser().parse(preLoginJson);
		// System.out.println(json.getAsJsonObject().get("nonce"));
		// System.out.println(json.getAsJsonObject().get("pubkey"));
		String rsaPub = json.getAsJsonObject().get("pubkey").getAsString();
		String nonce = json.getAsJsonObject().get("nonce").getAsString();
		String rsakv = json.getAsJsonObject().get("rsakv").getAsString();
		String pw = "****";// 密码为微博实现的rsa2加密
		String userName = "****";// 用户名为base64加密
		String serverTime = json.getAsJsonObject().get("servertime").getAsString();
		String encodedPw = getEncodedPw(rsaPub, pw, nonce, serverTime);
		String encodedUserName = new BASE64Encoder().encode(userName.getBytes());
		HttpPost post = new HttpPost("http://login.sina.com.cn/sso/login.php?client=ssologin.js(v1.4.18)");// 登录url
		post.setHeader("Referer", "http://weibo.com/login.php");
		post.setHeader("host", "login.sina.com.cn");
		List<BasicNameValuePair> postDict = new ArrayList<>();
		postDict.add(new BasicNameValuePair("encoding", "UTF-8"));
		postDict.add(new BasicNameValuePair("entry", "weibo"));
		postDict.add(new BasicNameValuePair("from", ""));
		postDict.add(new BasicNameValuePair("gateway", "1"));
		postDict.add(new BasicNameValuePair("nonce", nonce));
		postDict.add(new BasicNameValuePair("pagerefers",
				"http://passport.weibo.com/visitor/visitor?entry=miniblog&a=enter&url=http%3A%2F%2Fweibo.com%2F&domain=.weibo.com&ua=php-sso_sdk_client-0.6.14&_rand=1458040399.6273"));
		postDict.add(new BasicNameValuePair("prelt", "115"));
		postDict.add(new BasicNameValuePair("pwencode", "rsa2"));
		postDict.add(new BasicNameValuePair("returntype", "META"));
		postDict.add(new BasicNameValuePair("rsakv", rsakv));
		postDict.add(new BasicNameValuePair("savestate", "7"));
		postDict.add(new BasicNameValuePair("servertime", System.currentTimeMillis() / 1000 + ""));
		postDict.add(new BasicNameValuePair("service", "miniblog"));
		postDict.add(new BasicNameValuePair("sp", encodedPw));
		postDict.add(new BasicNameValuePair("sr", "1680*1050"));
		postDict.add(new BasicNameValuePair("su", encodedUserName));
		postDict.add(new BasicNameValuePair("url",
				"http://weibo.com/ajaxlogin.php?framelogin=1&callback=parent.sinaSSOController.feedBackUrlCallBack"));
		postDict.add(new BasicNameValuePair("useticket", "1"));
		postDict.add(new BasicNameValuePair("vsnf", "1"));
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(postDict, "utf-8");
		post.setEntity(entity);
		CloseableHttpResponse response2 = client.execute(post);
		String responseHtml = EntityUtils.toString(response2.getEntity(), "gbk");
		// printResult(responseHtml, false);
		// System.out.println(responseHtml);
		String locationUrl = getLocationgUrl(responseHtml);
		HttpGet get3 = new HttpGet(locationUrl);
		get3.setHeader("Referer", "http://login.sina.com.cn/sso/login.php?client=ssologin.js(v1.4.18)");
		client.execute(get3);// 需要额外访问在post成功后返回的locationurl，获得关键cookie，否则无法成功访问页面。(在访问需要抓取页面时，服务器会检查有没有名为RSF的cookie。)
		// response2.close();
		HttpGet get2 = new HttpGet("http://weibo.com/zard921?refer_flag=0000015012_&from=feed&loc=avatar");// 可以正常访问页面了
		get2.setHeader("Referer", "http://weibo.com/mygroups?gid=3733382226197678&wvr=6&leftnav=1");
		CloseableHttpResponse response3 = client.execute(get2);
		System.out.println(response3.getStatusLine().getStatusCode() + "------------------");
		String myHtml = EntityUtils.toString(response3.getEntity(), "gb2312");
		printResult(myHtml, false);
		List<Cookie> list = store.getCookies();
		for (Cookie cookie : list) {
			System.out.println(cookie.toString());
		}
		System.out.println("done!");
	}

	static String getLocationgUrl(String str) {
		Pattern pattern = Pattern.compile("location\\.replace\\(.+?\\)");
		Matcher matcher = pattern.matcher(str);
		String temp = "";
		if (matcher.find()) {
			temp = matcher.group();
		}
		int leftIndex = 18;
		int rightIndex = temp.length() - 2;
		return temp.substring(leftIndex, rightIndex);

	}

	/**
	 * 将相关参数传入js，利用Java自带js引擎加密密码 其中的js文件为将密码处理的js下载到本地经过编辑处理得来
	 */
	static String getEncodedPw(String rsaPub, String pw, String nonce, String serverTime) {
		String encodedPw = "";
		ScriptEngineManager sem = new ScriptEngineManager();
		ScriptEngine engine = sem.getEngineByName("javascript");
		try {
			engine.eval(new FileReader(new File("weibo.js")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (ScriptException e) {
			e.printStackTrace();
		}
		if (engine instanceof Invocable) {
			Invocable in = (Invocable) engine;
			try {
				encodedPw = in.invokeFunction("get_pass", rsaPub, pw, nonce, serverTime).toString();
				// get_pass方法为自己写的，根据加密方式，自行添加方法传入相关参数并将加密后的pw取出
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (ScriptException e) {
				e.printStackTrace();
			}
		}
		return encodedPw;
	}

	static String htmlToJson(String str) {
		int leftIndex = str.indexOf("(") + 1;
		int rightIndex = str.lastIndexOf(")");
		return str.substring(leftIndex, rightIndex);

	}

	static void printResult(String resource, boolean b) {
		File resultFile = new File("test.txt");
		if (!resultFile.exists()) {
			try {
				resultFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		FileWriter raf = null;
		try {
			raf = new FileWriter(resultFile, b);
			raf.write(resource + "\r\n");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				raf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
