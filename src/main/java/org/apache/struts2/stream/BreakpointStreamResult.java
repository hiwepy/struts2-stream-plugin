package org.apache.struts2.stream;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opensymphony.xwork2.ActionInvocation;

/**
 * 
 * @className	： BreakpointStreamResult
 * @description: 输入流处理;支持断点续传文件输出流，对StreamResult做了增强，支持断点续传方式（多线程）下载同时也支持普通方式（单线程）下载
 * <b>Example:</b>
 * <pre>
 * <!-- START SNIPPET: example -->
 *   &lt;result name="success" type="file"&gt;
 *   &lt;param name="inputName"&gt;inputStream&lt;/param&gt;
 *   &lt;param name="contentType"&gt;image/jpeg&lt;/param&gt;
 *   &lt;param name="contentCharSet"&gt;UTF-8&lt;/param&gt;
 *   &lt;param name="contentDisposition"&gt;attachment;filename="document.jpg"&lt;/param&gt;
 *   &lt;param name="bufferSize"&gt;1024&lt;/param&gt;
 * &lt;/result&gt;
 * <!-- END SNIPPET: example -->
 * </pre>
 * @author 		： <a href="https://github.com/vindell">vindell</a>
 * @date		： 2017年9月12日 下午10:56:30
 * @version 	V1.0
 */
public class BreakpointStreamResult extends StreamResultSupport {

	private static final long serialVersionUID = -256643510497634924L;
	protected static Logger LOG = LoggerFactory.getLogger(BreakpointStreamResult.class);
	
	/**
	 * @see com.StreamResultSupport.struts2.result.types.AbstractStreamResult#doExecute(java.lang.String,com.opensymphony.xwork2.ActionInvocation)
	 */
	@Override
	protected void doResultExecute(String finalLocation, ActionInvocation invocation,HttpServletRequest request,HttpServletResponse response) throws Exception {
		 
		if (inputStream == null) {
			String msg = ("Can not find a java.io.InputStream with the name [" + inputName + "] in the invocation stack. " + "Check the tag specified for this action.");
			LOG.error(msg);
			throw new IllegalArgumentException(msg);
		}
		if (contentLength == null || contentLength.length() < 1) {
			throw new IllegalArgumentException("支持断点续传时，[Content-Length]不能为空.");
		}
		
		// 输出流
		OutputStream output = null; 
		// 缓存输出流
		OutputStream buffOutput = null;
		try {
			
			// 范围字符串（比如“bytes=27000-”或者“bytes=27000-39000”的内容）
			String rangeBytes = ""; 
			// 起点长度（比如bytes=27000-39000，则这个值为27000）
			long beginPoint = 0; 
			// 终点长度（比如bytes=27000-39000，则这个值为39000）
			long endPoint = 0; 
			// 响应的字节总量（endPoint - beginPoint + 1）
			long rangeLength = 0; 
			// Set the content length
			String _contentLength = conditionalParse(contentLength, invocation);
			// 要下载的文件大小
			long fileLength = Integer.parseInt(_contentLength);
			//计算传输内容开始结束位，如果是断点下载则设置 Accept-Ranges, Content-Range
			this.setContentRange(request, response, fileLength, rangeBytes, beginPoint, endPoint, rangeLength);
			// 输出流
			output = response.getOutputStream();
			// 缓存输出流
			buffOutput = new BufferedOutputStream(output);
			// 缓存区
			byte[] outBuff = new byte[bufferSize];
			// 读取长度
			int readLen = 0;
			// 跳过已下载字节
			inputStream.skip(beginPoint);
			// 闭区间处理
			if (endPoint > 0) {
				LOG.debug("闭区间下载开始...");
				int readBufSize = (int) Math.min(bufferSize, rangeLength);
				long pFrom = beginPoint;
				while (pFrom < endPoint) {
					readLen = inputStream.read(outBuff, 0, readBufSize);
					pFrom += readBufSize;
					readBufSize = (int) Math.min(readBufSize, endPoint - pFrom + 1);
					buffOutput.write(outBuff, 0, readLen);
				}
				// 开区间处理
			} else {
				LOG.debug("开区间下载开始...");
				while ((readLen = inputStream.read(outBuff, 0, bufferSize)) != -1) {
					buffOutput.write(outBuff, 0, readLen);
				}
			}
			buffOutput.flush();
		} catch (IOException e) {
			// 忽略（迅雷等下载工具，支持多线程下载，但有些线程会被中途取消，导致异常。）
			/**
			 * 在写数据的时候 对于 ClientAbortException 之类的异常
			 * 是因为客户端取消了下载，而服务器端继续向浏览器写入数据时， 抛出这个异常，这个是正常的。 尤其是对于迅雷这种吸血的客户端软件。
			 * 明明已经有一个线程在读取 bytes=1275856879-1275877358，
			 * 如果短时间内没有读取完毕，迅雷会再启第二个、第三个。。。线程来读取相同的字节段， 直到有一个线程读取完毕，迅雷会 KILL
			 * 掉其他正在下载同一字节段的线程， 强行中止字节读出，造成服务器抛 ClientAbortException。
			 * 所以，我们忽略这种异常
			 */
			String name = e.getClass().getName();
		    if(name==null || name.indexOf("ClientAbortException")<0){
		    	LOG.debug(e.getMessage(), e);
		    }
		} catch (Exception e) {
			// 其他异常记录日志
			LOG.error(e.getMessage(), e);
		} finally {
			// Flush
            output.flush();
			IOUtils.closeQuietly(buffOutput);
			IOUtils.closeQuietly(inputStream);
		}
	}

}