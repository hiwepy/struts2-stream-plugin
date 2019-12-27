package org.apache.struts2.stream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.struts2.stream.utils.FilemimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opensymphony.xwork2.ActionInvocation;
import com.oreilly.servlet.ServletUtils;

/**
 * 
 * @className	： FilePathResult
  * @description: 文件结果流处理，结合COS的稳定与struts2的便利，实现文件的下载，从而替换直接访问文件的方式
 * <b>Example:</b>
 * <pre>
 * <!-- START SNIPPET: example -->
 *   &lt;result name="success" type="file"&gt;
 *   &lt;param name="inputName"&gt;inputPath&lt;/param&gt;
 *   &lt;param name="contentType"&gt;image/jpeg&lt;/param&gt;
 *   &lt;param name="contentCharSet"&gt;UTF-8&lt;/param&gt;
 *   &lt;param name="contentFilePath"&gt;${path}&lt;/param&gt;
 *   &lt;param name="contentDisposition"&gt;attachment;filename="document.jpg"&lt;/param&gt;
 * &lt;/result&gt;
 * <!-- END SNIPPET: example -->
 * </pre>
 * @author 		： <a href="https://github.com/hiwepy">hiwepy</a>
 * @date		： 2017年9月12日 下午10:55:53
 * @version 	V1.0
 */
public class FilePathResult extends StreamResultSupport {

	private static final long serialVersionUID = -1468409635999059850L;
	protected static final Logger LOG = LoggerFactory.getLogger(FilePathResult.class);
	protected String inputName = "inputFilePath";
	
	/**
	 * @see com.StreamResultSupport.struts2.result.types.AbstractStreamResult#doExecute(java.lang.String,com.opensymphony.xwork2.ActionInvocation)
	 */
	protected void doResultExecute(String finalLocation, ActionInvocation invocation,HttpServletRequest request,HttpServletResponse response) throws Exception {
		
		if (inputFilePath == null) {
			String msg = ("Can not find a output file path with the name ["+ inputName + "] in the invocation stack. " + "Check the <param name=\"inputName\"> tag specified for this action.");
			LOG.error(msg);
			throw new IllegalArgumentException(msg);
		}
		
		inputFile = new File(inputFilePath);
		if(inputFile.isDirectory() || !inputFile.exists() ){
			String msg = ("Can not find a file from path ["+ inputFilePath + "].");
			LOG.error(msg);
			throw new FileNotFoundException(msg);
		}
		
		// 输出流
		OutputStream output = null; 
		try {
			
			//根据文件名称获取对应的响应类型
			contentType  = FilemimeUtils.getFileMimeType(inputFile.getAbsolutePath());
			// Set the content type
			if (contentCharSet != null && !contentCharSet.equals("")) {
				response.setContentType(conditionalParse(contentType , invocation) + ";charset=" + contentCharSet);
			} else {
				response.setContentType(conditionalParse(contentType,invocation));
			}
			// Set the content-disposition
			response.addHeader("Content-Disposition", getContentDisposition(request, true, FilenameUtils.getName(inputFile.getAbsolutePath())));
			// Set the content length
			response.setContentLength(Long.valueOf(inputFile.length()).intValue());
			 
			// Copy input to output
			LOG.info("Use COS to write file +++ START +++");
        	// Get the outputstream
			output = response.getOutputStream();
            //采用COS提供的ServletUtils类完成文件下载  
            //在ServletUtils中一共提供了7个静态方法，可以实现不同场景的文件下载以及其它需求  
            //其中使用returnFile()可以下载本地的文件，使用returnURL()可以下载网络上的文件  
            //ServletUtils.returnURL(new URL("http://29.duote.org/javadmscgj.exe"), response.getOutputStream()); 
            ServletUtils.returnFile(inputFilePath,output);
            LOG.info("Use COS to write file  +++ END +++");
            
		} finally {
			// Flush
            output.flush();
            // delete file if allow clear
            if (allowClear && inputFile != null && inputFile.exists() && inputFile.isFile()) {
            	inputFile.delete();
            }
		}
	}
	
	

}
