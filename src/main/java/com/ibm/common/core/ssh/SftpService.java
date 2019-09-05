package com.ibm.common.core.ssh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

/**
 * 类说明 sftp工具类
 */
@Service
public class SftpService implements AutoCloseable {

	@SuppressWarnings("unused")
	private static Logger logger = LoggerFactory.getLogger(SftpService.class);

	private ChannelSftp sftp;

	private Session session;

	/** SFTP 登录用户名 */
	@Value("#{APP_PROP['ftp.pos.user']}")
	private String username = "root";
	/** SFTP 登录密码 */
	@Value("#{APP_PROP['ftp.pos.password']}")
	private String password = "password";
	/** 私钥 */
	private String privateKey;
	/** SFTP 服务器地址 */
	@Value("#{APP_PROP['ftp.pos.host']}")
	private String host = "10.3.13.109";
	/** SFTP 端口 */
	@Value("#{APP_PROP['ftp.pos.port']}")
	private int port = 22;
	// ftp 路径
	@Value("#{APP_PROP['ftp.pos.path']}")
	private String path;
	// 超时时间
	private int timeout = 1000;

	/**
	 * 构造基于密码认证的sftp对象
	 */
	public void setSftpProperties(String username, String password, String host, int port) {
		this.username = username;
		this.password = password;
		this.host = host;
		this.port = port;
	}

	/**
	 * 构造基于秘钥认证的sftp对象
	 */
	public void setSftpProperties(String username, String host, int port, String privateKey) {
		this.username = username;
		this.host = host;
		this.port = port;
		this.privateKey = privateKey;
	}

	/**
	 * 连接sftp服务器
	 */
	public void login() {
		try {
			JSch jsch = new JSch();
			if (privateKey != null) {
				// 设置私钥
				jsch.addIdentity(privateKey);
			}
			session = jsch.getSession(username, host, port);
			if (password != null) {
				session.setPassword(password);
			}
			Properties config = new Properties();
			config.put("StrictHostKeyChecking", "no");

			session.setConfig(config);
			session.setTimeout(timeout);
			session.connect();
			logger.debug("sftp session connected");
			logger.debug("opening channel");
			Channel channel = session.openChannel("sftp");
			channel.connect();
			logger.debug("connected successfully");
			sftp = (ChannelSftp) channel;
		} catch (JSchException e) {
			logger.error("sftp login failed",e);
			e.printStackTrace();
		}
	}

	/**
	 * 关闭连接 server
	 */
	public void logout() {
		if (sftp != null) {
			if (sftp.isConnected()) {
				sftp.disconnect();
			}
		}
		if (session != null) {
			if (session.isConnected()) {
				session.disconnect();
			}
		}
	}

	/**
	 * 将输入流的数据上传到sftp作为文件。文件完整路径=basePath+directory
	 * 
	 * @param basePath     服务器的基础路径
	 * @param directory    上传到该目录
	 * @param sftpFileName sftp端文件名
	 * @param input        输入流
	 */
	public void upload(String basePath, String directory, String sftpFileName, InputStream input) throws SftpException {
		try {
			sftp.cd(basePath);
			sftp.cd(directory);
		} catch (SftpException e) {
			// 目录不存在，则创建文件夹
			String[] dirs = directory.split("/");
			String tempPath = basePath;
			for (String dir : dirs) {
				if (null == dir || "".equals(dir)) {
					continue;
				}
				tempPath += "/" + dir;
				try {
					sftp.cd(tempPath);
				} catch (SftpException ex) {
					sftp.mkdir(tempPath);
					sftp.cd(tempPath);
				}
			}
		}
		// 上传文件
		sftp.put(input, sftpFileName);
	}

	/**
	 * 下载文件。
	 * 
	 * @param directory    下载目录
	 * @param downloadFile 下载的文件
	 * @param saveFile     存在本地的路径
	 */
	public void download(String directory, String downloadFile, String saveFile)
			throws SftpException, FileNotFoundException {
		if (directory != null && !"".equals(directory)) {
			sftp.cd(directory);
		}
		File file = new File(saveFile);
		sftp.get(downloadFile, new FileOutputStream(file));
	}

	/**
	 * 判断远程SFTP服务器上是否存在某个文件
	 * 
	 * @param directory 目录
	 * @param fileName  文件名
	 * @return 是否存在
	 */
	public boolean isExists(String directory, String fileName) {
		boolean isHave = false;
		try {
			sftp.cd(directory);
			SftpATTRS attrs = sftp.stat(fileName);
			if (attrs != null) {
				isHave = true;
			}
		} catch (Exception e) {
		}
		return isHave;
	}

	/**
	 * 下载文件
	 * 
	 * @param directory    下载目录
	 * @param downloadFile 下载的文件名
	 * @return 字节数组
	 */
	public byte[] download(String directory, String downloadFile) throws SftpException, IOException {
		if (directory != null && !"".equals(directory)) {
			sftp.cd(directory);
		}
		InputStream is = sftp.get(downloadFile);
		byte[] fileData = IOUtils.toByteArray(is);
		return fileData;
	}

	/**
	 * 删除文件
	 * 
	 * @param directory  要删除文件所在目录
	 * @param deleteFile 要删除的文件
	 */
	public void delete(String directory, String deleteFile) throws SftpException {
		sftp.cd(directory);
		sftp.rm(deleteFile);
	}

	/**
	 * 列出目录下的文件
	 * 
	 * @param directory 要列出的目录
	 */
	public Vector<?> listFiles(String directory) throws SftpException {
		return sftp.ls(directory);
	}

	/**
	 * 自动关闭资源
	 * 
	 * @author LiuBaoWen
	 * @see java.lang.AutoCloseable#close()
	 * @throws Exception
	 */
	@Override
	public void close() throws Exception {
		if (sftp != null) {
			sftp.disconnect();
		}
		if (session != null) {
			session.disconnect();
		}
	}

}