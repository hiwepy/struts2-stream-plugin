package org.apache.struts2.stream;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.result.StrutsResultSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.util.ValueStack;

@SuppressWarnings("serial")
public abstract class StreamResultSupport extends StrutsResultSupport {

	protected static Logger LOG = LoggerFactory.getLogger(StreamResultSupport.class);
	public static final String DEFAULT_PARAM = "inputName";

	protected String contentType = "text/plain";
	protected String contentLength;
	protected String contentDisposition = "inline";
	protected String contentCharSet = "UTF-8";
	protected String inputName = "inputBytes";
	protected InputStream inputStream;
	protected File inputFile;
	protected byte[] inputBytes;
	protected String inputFilePath;
	protected int bufferSize = 1024;
	protected boolean allowCaching = true;
	protected boolean allowClear = false;

	public StreamResultSupport() {
		super();
	}

	public StreamResultSupport(InputStream in) {
        this.inputStream = in;
    }
	
	public StreamResultSupport(File in) {
		this.inputFile = in;
	}
	
	public StreamResultSupport(String in) {
		this.inputFilePath = in;
	}
	
	public StreamResultSupport(byte[] in) {
		this.inputBytes = in;
	}

	/**
	 * @return Returns the whether or not the client should be requested to
	 *         allow caching of the data stream.
	 */
	public boolean getAllowCaching() {
		return allowCaching;
	}

	/**
	 * Set allowCaching to <tt>false</tt> to indicate that the client should be
	 * requested not to cache the data stream. This is set to <tt>false</tt> by
	 * default
	 * 
	 * @param allowCaching Enable caching.
	 */
	public void setAllowCaching(boolean allowCaching) {
		this.allowCaching = allowCaching;
	}

	/**
	 * @return Returns the bufferSize.
	 */
	public int getBufferSize() {
		return (bufferSize);
	}

	/**
	 * @param bufferSize  The bufferSize to set.
	 */
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	/**
	 * @return Returns the contentType.
	 */
	public String getContentType() {
		return (contentType);
	}

	/**
	 * @param contentType  The contentType to set.
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * @return Returns the contentLength.
	 */
	public String getContentLength() {
		return contentLength;
	}

	/**
	 * @param contentLength The contentLength to set.
	 */
	public void setContentLength(String contentLength) {
		this.contentLength = contentLength;
	}

	/**
	 * @return Returns the Content-disposition header value.
	 */
	public String getContentDisposition() {
		return contentDisposition;
	}

	/**
	 * @param contentDisposition the Content-disposition header value to use.
	 */
	public void setContentDisposition(String contentDisposition) {
		this.contentDisposition = contentDisposition;
	}

	/**
	 * @return Returns the charset specified by the user
	 */
	public String getContentCharSet() {
		return contentCharSet;
	}

	/**
	 * @param contentCharSet  the charset to use on the header when sending the stream
	 */
	public void setContentCharSet(String contentCharSet) {
		this.contentCharSet = contentCharSet;
	}

	/**
	 * @return Returns the inputName.
	 */
	public String getInputName() {
		return (inputName);
	}

	/**
	 * @param inputName The inputName to set.
	 */
	public void setInputName(String inputName) {
		this.inputName = inputName;
	}

	public boolean isAllowClear() {
		return allowClear;
	}

	public void setAllowClear(boolean allowClear) {
		this.allowClear = allowClear;
	}

	/**
	 * 
	 * @description	： 扩展struts自带的doExecute
	 * @author 		： <a href="https://github.com/hiwepy">hiwepy</a>
	 * @date 		：2017年9月12日 下午10:55:27
	 * @param finalLocation
	 * @param invocation
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	protected abstract void doResultExecute(String finalLocation,ActionInvocation invocation,HttpServletRequest request,HttpServletResponse response) throws Exception;

	
	protected boolean isHeadOfRange(HttpServletRequest request) {
		return request.getHeader("Range") != null;
	}
	
	protected String getRangeBytes(HttpServletRequest request) {
		return request.getHeader("Range").replaceAll("bytes=", "").trim();
	}
	
	protected void setContentRange(HttpServletRequest request,HttpServletResponse response,long fileLength,String rangeBytes,long beginPoint,long endPoint,long rangeLength){
		response.setContentLength(Long.valueOf(fileLength).intValue());
		//response.addHeader("Content-Length", String.valueOf(fileLength));
		
		// 需要使用断点续传下载
		if (isHeadOfRange(request)) {
			LOG.debug("断点续传下载.");
			// 设置状态 HTTP/1.1 206 Partial Content
			response.setStatus(javax.servlet.http.HttpServletResponse.SC_PARTIAL_CONTENT);
			// 通知客户端允许断点续传，响应格式为：Accept-Ranges: bytes（默认是“none”，可以不指定）
			response.setHeader("Accept-Ranges", "bytes");
			// 设置Content-Range
			StringBuilder crBuf = new StringBuilder("bytes ");
			rangeBytes = getRangeBytes(request);
			// 判断 Range 字串模式
			if (rangeBytes.endsWith("-")) {
				LOG.debug("开区间下载；如：快车下载.");
				//计算开始点
				rangeBytes = StringUtils.substringBefore(rangeBytes, "-");
				// 起点长度
				beginPoint = Long.parseLong(rangeBytes);
				// 响应的字节总量
				rangeLength = fileLength - beginPoint + 1;
				// Content-Range 
				crBuf.append(rangeBytes).append("-").append(beginPoint - 1) .append("/").append(fileLength);
			} else {
				LOG.debug("闭区间下载；如：迅雷下载.");
				//计算开始结束点
				String num1 = StringUtils.substringBefore(rangeBytes, "-");
				String num2 = StringUtils.substringAfter(rangeBytes, "-");
				// 起点长度
				beginPoint = Long.parseLong(num1);
				// 终点长度
				endPoint = Long.parseLong(num2);
				// 响应的字节总量
				rangeLength = endPoint - beginPoint + 1;
				// Content-Range 
				crBuf.append(rangeBytes).append("/").append(fileLength);
			}
			// Content-Range: bytes [文件块的开始字节]-[文件的总大小 - 1]/[文件的总大小]
			response.setHeader("Content-Range", crBuf.toString());
			// 普通下载
		} else {
			LOG.debug("普通下载.");
			// 默认返回 HTTP/1.1 200 OK
			rangeLength = fileLength; // 客户端要求全文下载
		}
	}
	
	/**
	 * @see org.apache.struts2.dispatcher.StrutsResultSupport#doExecute(java.lang.String,
	 *      com.opensymphony.xwork2.ActionInvocation)
	 */
	protected void doExecute(String finalLocation, ActionInvocation invocation) throws Exception {

		// Override any parameters using values on the stack
		resolveParamsFromStack(invocation.getStack(), invocation);

		// Find the Response in context
		// Find the Response in context
		HttpServletResponse response = (HttpServletResponse) invocation.getInvocationContext().get(HTTP_RESPONSE);
		HttpServletRequest request = (HttpServletRequest) invocation.getInvocationContext().get(HTTP_REQUEST);

		// Set the content type
		if (contentCharSet != null && !contentCharSet.equals("")) {
			response.setContentType(conditionalParse(contentType, invocation)+ ";charset=" + contentCharSet);
		} else {
			response.setContentType(conditionalParse(contentType, invocation));
		}

		// Set the content length
		if (contentLength != null) {
			String _contentLength = conditionalParse(contentLength, invocation);
			int _contentLengthAsInt = -1;
			try {
				_contentLengthAsInt = Integer.parseInt(_contentLength);
				if (_contentLengthAsInt >= 0) {
					response.setContentLength(_contentLengthAsInt);
				}
			} catch (NumberFormatException e) {
				if (LOG.isWarnEnabled()) {
					LOG.warn("failed to recongnize "+ _contentLength+ " as a number, contentLength header will not be set",e);
				}
			}
		}

		// Set the content-disposition
		if (contentDisposition != null) {
			response.addHeader("Content-Disposition", conditionalParse(contentDisposition, invocation));
		}

		// Set the cache control headers if neccessary
		if (!allowCaching) {
			response.addHeader("Pragma", "no-cache");
			response.addHeader("Cache-Control", "no-cache");
		}

		if (inputStream == null) {
			// Find the inputstream from the invocation variable stack
			inputStream = (InputStream) invocation.getStack().findValue(conditionalParse(inputName, invocation));
		} else if (inputFile == null) {
			// Find the inputFile from the invocation variable stack
			inputFile = (File) invocation.getStack().findValue(conditionalParse(inputName, invocation));
		} else if (inputFilePath == null) {
			// Find the inputFilePath from the invocation variable stack
			inputFilePath = invocation.getStack().findString(conditionalParse(inputName, invocation));
		} else if (inputBytes == null) {
			// Find the byte[] from the invocation variable stack
			inputBytes = (byte[]) invocation.getStack().findValue(conditionalParse(inputName, invocation));
		}
		this.doResultExecute(finalLocation, invocation, request , response);
	}

	/**
	 * Tries to lookup the parameters on the stack. Will override any existing parameters
	 * 
	 * @param stack The current value stack
	 */
	protected void resolveParamsFromStack(ValueStack stack, ActionInvocation invocation) {
		String disposition = stack.findString("contentDisposition");
		if (disposition != null) {
			setContentDisposition(disposition);
		}

		String contentType = stack.findString("contentType");
		if (contentType != null) {
			setContentType(contentType);
		}

		String inputName = stack.findString("inputName");
		if (inputName != null) {
			setInputName(inputName);
		}

		String contentLength = stack.findString("contentLength");
		if (contentLength != null) {
			setContentLength(contentLength);
		}

		Integer bufferSize = (Integer) stack.findValue("bufferSize",
				Integer.class);
		if (bufferSize != null) {
			setBufferSize(bufferSize.intValue());
		}

		String allowCaching = stack.findString("allowCaching");
		if (allowCaching != null) {
			setAllowCaching(Boolean.getBoolean(allowCaching));
		}

		String allowClear = stack.findString("allowClear");
		if (allowClear != null) {
			setAllowClear(Boolean.getBoolean(allowClear));
		}

		if (contentCharSet != null) {
			contentCharSet = conditionalParse(contentCharSet, invocation);
		} else {
			contentCharSet = stack.findString("contentCharSet");
		}

	}
	
	/**
	 * 方法用途和描述: 获取内容描述
	 * @param name
	 * @return
	 * <br>attachment：附件
	 * <br>inline：内联（意指打开时在浏览器中打开）
	 * @throws UnsupportedEncodingException 
	 */
	public String getContentDisposition(HttpServletRequest request,boolean attachment,String fileName) throws UnsupportedEncodingException {
		return (new StringBuilder((attachment?"attachment":"inline")+";filename=\"").append(getEncodeFileName(request,fileName)).append("\"")).toString();
	}
	
	/**
	 * 
	 * @description	： 关于下载中文文件名的问题，不同浏览器需要使用不同的编码，下载前要在Java中进行文件名编码
	 *  			 在多数浏览器中使用 UTF8 ，而在 firefox 和 safari 中使用 ISO8859-1 。
	 *  			 经测试在 IE、Firefox、Chorme、Safari、Opera 上都能正常显示中文文件名（只测试了较新的浏览器）
	 * @author 		： <a href="https://github.com/hiwepy">hiwepy</a>
	 * @date 		：2017年9月12日 下午10:55:18
	 * @param request
	 * @param name
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public String getEncodeFileName(HttpServletRequest request, String name) throws UnsupportedEncodingException {
		String agent = request.getHeader("USER-AGENT").toLowerCase();
		if (agent != null && agent.indexOf("firefox") < 0 && agent.indexOf("safari") < 0) {
			return URLEncoder.encode(name, "UTF8");
		}
		return new String(name.getBytes("UTF-8"), "ISO8859-1");
	}

}
