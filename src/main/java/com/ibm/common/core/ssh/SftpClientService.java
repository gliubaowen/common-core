package com.ibm.common.core.ssh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

/**
 * sftp工具类
 * 
 * @author LiuBaoWen
 *
 */
//@Component
public class SftpClientService implements AutoCloseable {

	private static Logger logger = LoggerFactory.getLogger(SftpClientService.class);

	private ChannelSftp sftp;

	private Session session;

	/** SFTP 登录用户名 */
	@Value("${ssh.username}")
	private String username;
	/** SFTP 登录密码 */
	@Value("${ssh.password}")
	private String password;
	/** 私钥 */
	private String privateKey;
	/** SFTP 服务器地址 */
	@Value("${ssh.host}")
	private String host;
	/** SFTP 端口 */
	@Value("${ssh.port}")
	private int port = 22;
	/** 超时时间 */
	private int timeout = 1000;

	/**
	 * 构造基于密码认证的sftp对象
	 * 
	 * @author LiuBaoWen
	 * @param username
	 * @param password
	 * @param host
	 * @param port
	 */
	public void setSftpProperties(String username, String password, String host, int port) {
		this.username = username;
		this.password = password;
		this.host = host;
		this.port = port;
	}

	/**
	 * 构造基于秘钥认证的sftp对象
	 * 
	 * @author LiuBaoWen
	 * @param username
	 * @param host
	 * @param port
	 * @param privateKey
	 */
	public void setSftpProperties(String username, String host, int port, String privateKey) {
		this.username = username;
		this.host = host;
		this.port = port;
		this.privateKey = privateKey;
	}

	/**
	 * 登录sftp服务器
	 * 
	 * @author LiuBaoWen
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
			logger.error("sftp login failed", e);
			e.printStackTrace();
		}
	}

	/**
	 * 关闭连接 server
	 * 
	 * @author LiuBaoWen
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
	 * 判断目录是否存在
	 * 
	 * @param directory
	 * @return
	 */
	public boolean isDirExist(String directory) {
		boolean isDirExistFlag = false;
		try {
			SftpATTRS sftpATTRS = sftp.lstat(directory);
			isDirExistFlag = true;
			return sftpATTRS.isDir();
		} catch (Exception e) {
			if (e.getMessage().toLowerCase().equals("no such file")) {
				isDirExistFlag = false;
			}
		}
		return isDirExistFlag;
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
	 * 获取srcPath路径下以regex格式指定的文件列表
	 * 
	 * @param sftp
	 * @param srcPath sftp服务器上的目录
	 * @param regex   需要匹配的文件名
	 * @return
	 * @throws SftpException
	 */
	@SuppressWarnings("unchecked")
	public static List<String> listFiles(ChannelSftp sftp, String srcPath, String regex) throws SftpException {
		List<String> fileList = new ArrayList<String>();
		sftp.cd(srcPath); // 如果srcPath不是目录则会抛出异常
		if ("".equals(regex) || regex == null) {
			regex = "*";
		}
		Vector<LsEntry> sftpFile = sftp.ls(regex);
		String fileName = null;
		for (LsEntry lsEntry : sftpFile) {
			fileName = lsEntry.getFilename();
			fileList.add(fileName);
		}
		return fileList;
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