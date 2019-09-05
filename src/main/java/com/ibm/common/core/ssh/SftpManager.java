/**
 * 
 */
package com.ibm.common.core.ssh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * 利用JSch包实现SFTP下载、上传文件的类
 * 
 * @author LiuBaoWen
 */
public class SftpManager {
	
	private static final Logger logger = LoggerFactory.getLogger(SftpManager.class);
	public static final String SFTP_PROTOCAL = "sftp";

	private ChannelSftp sftp = null;
	private Session sshSession = null;
	
	@Value("")
	private String host;//服务器连接ip
	@Value("")
    private String username;//用户名
	@Value("")
    private String password;//密码
	@Value("")
    private int port = 22;//端口号
	
	
	/**
	 * Password authorization
	 * 
	 * @param host     主机IP
	 * @param username 主机登陆用户名
	 * @param password 主机登陆密码
	 * @param port     主机ssh登陆端口，如果port <= 0取默认值(22)
	 * @return sftp
	 * @throws Exception
	 * @see http://www.jcraft.com/jsch/
	 */
	public static ChannelSftp connect(String host, String username, String password, int port) throws Exception {
		Channel channel = null;
		ChannelSftp sftp = null;
		JSch jsch = new JSch();

		Session session = createSession(jsch, host, username, port);
		// 设置登陆主机的密码
		session.setPassword(password);
		// 设置登陆超时时间
		session.connect(15000);
		logger.info("Session connected to " + host + ".");
		try {
			// 创建sftp通信通道
			channel = (Channel) session.openChannel(SFTP_PROTOCAL);
			channel.connect(1000);
			logger.info("Channel created to " + host + ".");
			sftp = (ChannelSftp) channel;
		} catch (JSchException e) {
			logger.error("exception when channel create.", e);
		}
		return sftp;
	}

	/**
	 * Private/public key authorization (加密秘钥方式登陆)
	 * 
	 * @param username   主机登陆用户名(user account)
	 * @param host       主机IP(server host)
	 * @param port       主机ssh登陆端口(ssh port), 如果port<=0, 取默认值22
	 * @param privateKey 秘钥文件路径(the path of key file.)
	 * @param passphrase 密钥的密码(the password of key file.)
	 * @return sftp
	 * @throws Exception
	 * @see http://www.jcraft.com/jsch/
	 */
	public static ChannelSftp connect(String username, String host, int port, String privateKey, String passphrase)
			throws Exception {
		Channel channel = null;
		ChannelSftp sftp = null;
		JSch jsch = new JSch();

		// 设置密钥和密码 ,支持密钥的方式登陆
		if (StringUtils.isNotEmpty(privateKey)) {
			if (StringUtils.isNotEmpty(passphrase)) {
				// 设置带口令的密钥
				jsch.addIdentity(privateKey, passphrase);
			} else {
				// 设置不带口令的密钥
				jsch.addIdentity(privateKey);
			}
		}
		Session session = createSession(jsch, host, username, port);
		// 设置登陆超时时间
		session.connect(15000);
		logger.info("Session connected to " + host + ".");
		try {
			// 创建sftp通信通道
			channel = (Channel) session.openChannel(SFTP_PROTOCAL);
			channel.connect(1000);
			logger.info("Channel created to " + host + ".");
			sftp = (ChannelSftp) channel;
		} catch (JSchException e) {
			logger.error("exception when channel create.", e);
		}
		return sftp;
	}

	/**
	 * upload all the files to the server<br/>
	 * 将本地文件名为 srcFile 的文件上传到目标服务器, 目标文件名为 dest,<br/>
	 * 若 dest为目录，则目标文件名将与srcFile文件名相同. 采用默认的传输模式： OVERWRITE
	 * 
	 * @param sftp
	 * @param srcFile 本地文件的绝对路径
	 * @param dest    目标文件的绝对路径
	 */
	public static void upload(ChannelSftp sftp, String srcFile, String dest) {
		try {
			File file = new File(srcFile);
			if (file.isDirectory()) {
				sftp.cd(srcFile);
				for (String fileName : file.list()) {
					sftp.put(srcFile + SystemUtils.FILE_SEPARATOR + fileName, dest);
				}
			}
			sftp.put(srcFile, dest);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * upload all the files to the server<br/>
	 * 将fileList中的本地文件上传到目标服务器, 目标目录路径为 destPath,<br/>
	 * destPath必须是目录，目标文件名将与源文件名相同. 采用默认的传输模式： OVERWRITE
	 * 
	 * @param sftp
	 * @param fileList 要上传到目标服务器的文件的绝对路径
	 * @param destPath 目标文件的绝对路径, 一定是目录, 如果目录不存在则自动创建
	 * @throws SftpException
	 */
	public static void upload(ChannelSftp sftp, List<String> fileList, String destPath) throws SftpException {
		try {
			sftp.cd(destPath);
		} catch (Exception e) {
			sftp.mkdir(destPath);
		}
		for (String srcFile : fileList) {
			upload(sftp, srcFile, destPath);
		}
	}

	/**
	 * 使用sftp下载文件
	 * 
	 * @param sftp
	 * @param srcPath  sftp服务器上源文件的路径, 必须是目录
	 * @param saveFile 下载后文件的存储路径, 若为目录, 则文件名将与目标服务器上的文件名相同
	 * @param srcfile  目标服务器上的文件, 不能为目录
	 */
	public static void download(ChannelSftp sftp, String srcPath, String saveFile, String srcfile) {
		try {
			sftp.cd(srcPath);
			File file = new File(saveFile);
			if (file.isDirectory()) {
				sftp.get(srcfile, new FileOutputStream(file + SystemUtils.FILE_SEPARATOR + srcfile));
			} else {
				sftp.get(srcfile, new FileOutputStream(file));
			}
		} catch (Exception e) {
			logger.error("download file: {} error", srcPath + SystemUtils.FILE_SEPARATOR + srcfile, e);
		}
	}

	/**
	 * 使用sftp下载目标服务器上某个目录下指定类型的文件, 得到的文件名与 sftp服务器上的相同
	 * 
	 * @param sftp
	 * @param srcPath   sftp服务器上源目录的路径, 必须是目录
	 * @param savePath  下载后文件存储的目录路径, 一定是目录, 如果不存在则自动创建
	 * @param fileTypes 指定类型的文件, 文件的后缀名组成的字符串数组
	 */
	public static void download(ChannelSftp sftp, String srcPath, String savePath, String... fileTypes) {
		List<String> fileList = new ArrayList<String>();
		try {
			sftp.cd(srcPath);
			createDir(savePath);
			if (fileTypes.length == 0) {
				// 列出服务器目录下所有的文件列表
				fileList = listFiles(sftp, srcPath, "*");
				downloadFileList(sftp, srcPath, savePath, fileList);
				return;
			}
			for (String type : fileTypes) {
				fileList = listFiles(sftp, srcPath, "*" + type);
				parseAndUpdateDB(sftp, srcPath, savePath, fileList);
			}
		} catch (Exception e) {
			logger.error(
					"download all file in path = '" + srcPath + "' and type in " + Arrays.asList(fileTypes) + " error",
					e);
		}

	}

	private static File createDir(String savePath) throws Exception {
		File localPath = new File(savePath);
		if (!localPath.exists() && !localPath.isFile()) {
			if (!localPath.mkdir()) {
				throw new Exception(localPath + " directory can not create.");
			}
		}
		return localPath;
	}

	/**
	 * sftp下载目标服务器上srcPath目录下所有指定的文件.<br/>
	 * 若本地存储路径下存在与下载重名的文件,仍继续下载并覆盖该文件.<br/>
	 * 
	 * @param sftp
	 * @param savePath 文件下载到本地存储的路径,必须是目录
	 * @param fileList 指定的要下载的文件名列表
	 * @throws SftpException
	 * @throws FileNotFoundException
	 */
	public static void downloadFileList(ChannelSftp sftp, String srcPath, String savePath, List<String> fileList)
			throws SftpException, FileNotFoundException {
		sftp.cd(srcPath);
		for (String srcFile : fileList) {
			logger.info("srcFile: " + srcFile);
			String localPath = savePath + SystemUtils.FILE_SEPARATOR + srcFile;
			sftp.get(srcFile, localPath);
		}
	}

	/**
	 * sftp下载目标服务器上所有指定的文件, 并解析文件的内容.<br/>
	 * 若本地存储路径下存在与下载重名的文件, 则忽略(不下载)该文件.<br/>
	 * 
	 * @param sftp
	 * @param srcPath  sftp上源文件的目录
	 * @param savePath 文件下载到本地存储的路径,必须是目录
	 * @param fileList 指定的要下载的文件列表
	 * @throws FileNotFoundException
	 * @throws SftpException
	 */
	private static void parseAndUpdateDB(ChannelSftp sftp, String srcPath, String savePath, List<String> fileList)
			throws FileNotFoundException, SftpException {
		sftp.cd(srcPath);
		for (String srcFile : fileList) {
			String localPath = savePath + SystemUtils.FILE_SEPARATOR + srcFile;
			File localFile = new File(localPath);
			// savePath路径下已有文件与下载文件重名, 忽略这个文件
			if (localFile.exists() && localFile.isFile()) {
				continue;
			}

			logger.info("start downloading file: [" + srcFile + "], parseAndUpdate to DB");
			sftp.get(srcFile, localPath);
			// updateDB(localFile);
		}
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
	 * 删除文件
	 * 
	 * @param dirPath 要删除文件所在目录
	 * @param file    要删除的文件
	 * @param sftp
	 * @throws SftpException
	 */
	public static void delete(String dirPath, String file, ChannelSftp sftp) throws SftpException {
		String now = sftp.pwd();
		sftp.cd(dirPath);
		sftp.rm(file);
		sftp.cd(now);
	}

	/**
	 * Disconnect with server
	 */
	public static void disconnect(ChannelSftp sftp) {
		try {
			if (sftp != null) {
				if (sftp.isConnected()) {
					sftp.disconnect();
				} else if (sftp.isClosed()) {
					logger.info("sftp is closed already");
				}
				if (null != sftp.getSession()) {
					sftp.getSession().disconnect();
				}
			}
		} catch (JSchException e) {
			// Ignore
		}

	}

	private static Session createSession(JSch jsch, String host, String username, int port) throws Exception {
		Session session = null;
		if (port <= 0) {
			// 连接服务器，采用默认端口
			session = jsch.getSession(username, host);
		} else {
			// 采用指定的端口连接服务器
			session = jsch.getSession(username, host, port);
		}
		// 如果服务器连接不上，则抛出异常
		if (session == null) {
			throw new Exception(host + "session is null");
		}
		// 设置第一次登陆的时候提示，可选值：(ask | yes | no)
		session.setConfig("StrictHostKeyChecking", "no");
		return session;
	}

}
