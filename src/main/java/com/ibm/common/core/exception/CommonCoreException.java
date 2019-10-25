/**
 * 
 */
package com.ibm.common.core.exception;

/**
 * common-core异常类
 * 
 * @author LiuBaoWen
 *
 */
public class CommonCoreException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 869592584118790062L;

	/** 错误编码 */
	private String errorCode;

	/**
	 * @param message
	 */
	public CommonCoreException(String message) {
		super(message);
	}

	/**
	 * @param errorCode
	 */
	public CommonCoreException(String errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	/**
	 * 
	 * @param errorCode
	 * @param message
	 * @param cause
	 */
	public CommonCoreException(String errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	/**
	 * @param cause
	 */
	public CommonCoreException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public CommonCoreException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public CommonCoreException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/** get 错误编码 */
	public String getErrorCode() {
		return errorCode;
	}

}
