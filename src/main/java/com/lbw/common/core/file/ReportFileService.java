package com.lbw.common.core.file;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.lbw.common.core.ftp.FtpClientService;

/**
 * 报表文件服务
 * 
 * @author LiuBaoWen
 *
 */
//@Service
public class ReportFileService {

	private Logger logger = LoggerFactory.getLogger(ReportFileService.class);

	@Autowired
	FtpClientService ftpClientService;

	/**
	 * 上传报表文件到ftp
	 * 
	 * @author LiuBaoWen
	 * @param ftpWorkDir    ftp工作目录
	 * @param ftpFileName   ftp文件
	 * @param localFilePath 本地文件
	 * @return
	 */
	public boolean uploadReport(String ftpWorkDir, String ftpFileName, String localFilePath) {
		logger.info("上传报表开始时间:{}", new Date());
		long startUploadTimeMillis = System.currentTimeMillis();
		logger.info("上传报表文件：{},到ftp目录：{},重命名为：{}", localFilePath, ftpWorkDir, ftpFileName);
		File localFile = new File(localFilePath);
		try {
			ftpClientService.connect();
			if (!ftpClientService.changeWorkingDirectory(ftpWorkDir)) {
				ftpClientService.makeDirs(ftpWorkDir);
				ftpClientService.changeWorkingDirectory(ftpWorkDir);
			}
			ftpClientService.upload(ftpFileName, localFile);
		} catch (IOException e) {
			logger.error("上传文件：{} 到ftp失败,msg:{},error:{}", localFilePath, e.getMessage(), e.getStackTrace());
			return false;
		} finally {
			ftpClientService.disconnect();
		}
		long endUploadTimeMillis = System.currentTimeMillis();
		logger.info("上传报表结束时间:{},耗时：{} 秒", new Date(), (endUploadTimeMillis - startUploadTimeMillis) / 1000);
		return true;
	}

	/**
	 * 从ftp下载报表文件
	 * 
	 * @author LiuBaoWen
	 * @param ftpWorkDir    ftp工作目录
	 * @param ftpFileName   ftp文件
	 * @param localFilePath 本地文件
	 * @return
	 */
	public boolean downloadReport(String ftpWorkDir, String ftpFileName, String localFilePath) {
		logger.info("下载报表开始时间:{}", new Date());
		long startDownldTimeMillis = System.currentTimeMillis();
		logger.info("从ftp目录：{} ,下载文件：{} ,本地重命名为：{}", ftpWorkDir, ftpFileName, localFilePath);
		File localFile = new File(localFilePath);
		try {
			ftpClientService.connect();
			ftpClientService.changeWorkingDirectory(ftpWorkDir);
			ftpClientService.download(ftpFileName, localFile);
		} catch (IOException e) {
			logger.error("从ftp下载文件:{}失败,msg:{},error:{}", ftpFileName, e.getMessage(), e.getStackTrace());
			return false;
		} finally {
			ftpClientService.disconnect();
		}
		long endDownldTimeMillis = System.currentTimeMillis();
		logger.info("上传报表结束时间:{},耗时：{} 秒", new Date(), (endDownldTimeMillis - startDownldTimeMillis) / 1000);
		return true;
	}

	/**
	 * 刪除ftp报表文件
	 * 
	 * @author LiuBaoWen
	 * @param ftpFileName
	 * @return
	 */
	public boolean deleteFtpReportFile(String ftpFileNameStr) {
		String ftpFileName = ftpFileNameStr;
		logger.info("从ftp删除报表文件：{} 开始", ftpFileName);
		try {
			ftpClientService.connect();
			ftpClientService.deleteFile(ftpFileName);
		} catch (IOException e) {
			logger.error("从ftp删除报表文件:{} 失败,msg:{},error:{}", ftpFileName, e.getMessage(), e.getStackTrace());
			return false;
		} finally {
			ftpClientService.disconnect();
		}
		logger.info("从ftp删除报表文件：{} 结束", ftpFileName);
		return true;
	}

	/**
	 * 递归刪除ftp报表目录(目录非空也会删除)
	 * 
	 * @author LiuBaoWen
	 * @param ftpFilePath
	 * @return
	 */
	public boolean deleteFtpReportDir(String ftpFilePathStr) {
		String ftpFilePath = ftpFilePathStr;
		logger.info("从ftp删除报表目录：{} 开始", ftpFilePath);
		try {
			ftpClientService.connect();
			this.deleteFtpFiles(ftpFilePath);
		} catch (IOException e) {
			logger.error("从ftp删除报表目录:{} 失败,msg:{},error:{}", ftpFilePath, e.getMessage(), e.getStackTrace());
			return false;
		} finally {
			ftpClientService.disconnect();
		}
		logger.info("从ftp删除报表目录：{} 结束", ftpFilePath);
		return true;
	}

	/**
	 * 递归删除文件(包括文件夹)
	 * 
	 * @author LiuBaoWen
	 * @param ftpFilePath
	 * @throws IOException
	 */
	private void deleteFtpFiles(String ftpFilePath) throws IOException {
		logger.info("递归删除报表目录：{} 开始", ftpFilePath);
		FTPFile[] listFileAndDirs = ftpClientService.listFileAndDirs(ftpFilePath);
		if (listFileAndDirs != null) {
			for (int i = 0; i < listFileAndDirs.length; i++) {
				FTPFile ftpFile = listFileAndDirs[i];
				String ftpFileName = ftpFilePath + "/" + ftpFile.getName();
				if (ftpFile.isFile()) {
					ftpClientService.deleteFile(ftpFileName);
					logger.info("递归删除报表文件：{}", ftpFileName);
				} else if (ftpFile.isDirectory()) {
					this.deleteFtpFiles(ftpFileName);
				}
			}
		}
		ftpClientService.deleteDirectory(ftpFilePath);
		logger.info("递归删除报表目录：{} 结束", ftpFilePath);
	}

	/**
	 * 删除本地报表文件
	 * 
	 * @author LiuBaoWen
	 * @param localFileName
	 * @return
	 */
	public boolean deleteLocalReportFile(String localFileNameStr) {
		String localFileName = localFileNameStr;
		logger.info("删除本地报表文件：{} 开始", localFileName);
		File file = new File(localFileName);
		boolean isDelete = false;
		if (file.isFile()) {
			isDelete = file.delete();
		} else {
			logger.info("本地报表：{},不是文件", localFileName);
		}
		logger.info("删除本地报表文件：{},是否成功：{}", localFileName, isDelete);
		logger.info("删除本地报表文件：{} 结束", localFileName);
		return isDelete;
	}

	/**
	 * 强制删除本地报表目录(目录非空也删除)
	 * 
	 * @author LiuBaoWen
	 * @param localFileName
	 * @return
	 */
	public boolean deleteLocalReportDir(String localFilePathStr) {
		String localFilePath = localFilePathStr;
		logger.info("删除本地报表目录：{} 开始", localFilePath);
		try {
			FileUtils.deleteDirectory(new File(localFilePath));
		} catch (IOException e) {
			logger.error("删除本地报表目录：{} 失败,msg:{},error:{}", localFilePath, e.getMessage(), e.getStackTrace());
		}
		logger.info("删除本地报表目录：{} 结束", localFilePath);
		return true;
	}

}
