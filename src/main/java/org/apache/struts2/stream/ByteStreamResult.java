package org.apache.struts2.stream;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opensymphony.xwork2.ActionInvocation;
/**
 * 
 * @className	： ByteStreamResult
 * @description: 输出二进制数据
 * <b>Example:</b>
 *
 * <pre><!-- START SNIPPET: example -->
 * &lt;result name="success" type="byte"&gt;
 *   &lt;param name="allowCaching"&gt;false&lt;/param&gt;
 *   &lt;param name="inputName"&gt;inputBytes&lt;/param&gt;
 *   &lt;param name="contentType"&gt;image/jpeg&lt;/param&gt;
 *   &lt;param name="contentCharSet"&gt;UTF-8&lt;/param&gt;
 *   &lt;param name="contentFilePath"&gt;${contentFilePath}&lt;/param&gt;
 *   &lt;param name="contentDisposition"&gt;attachment;filename="document.jpg"&lt;/param&gt;
 * &lt;/result&gt;
 * <!-- END SNIPPET: example --></pre>
 * @author 		： <a href="https://github.com/vindell">vindell</a>
 * @date		： 2017年9月12日 下午10:56:12
 * @version 	V1.0
 */
public class ByteStreamResult extends StreamResultSupport {

    private static final long serialVersionUID = -1468409635999059850L;

    protected static final Logger LOG = LoggerFactory.getLogger(ByteStreamResult.class);

    protected String inputName = "inputBytes";

    /**
     * @see com.StreamResultSupport.struts2.result.types.AbstractStreamResult#doExecute(java.lang.String, com.opensymphony.xwork2.ActionInvocation)
     */
    protected void doResultExecute(String finalLocation, ActionInvocation invocation,HttpServletRequest request,HttpServletResponse response) throws Exception {

    	if (inputBytes == null) {
            String msg = ("Can not find a byte[] with the name [" + inputName + "] in the invocation stack. " +"Check the <param name=\"inputName\"> tag specified for this action.");
            LOG.error(msg);
            throw new IllegalArgumentException(msg);
        }
    	inputStream =  new ByteArrayInputStream(inputBytes);
    	
    	// 输出流
		OutputStream output = null; 
		// 缓存输出流
		OutputStream buffOutput = null;
        BufferedOutputStream bufferedOut = null;
        try {
            
        	LOG.debug("Streaming result [" + inputName + "] type=[" + contentType + "]  content-disposition=[" + contentDisposition + "] charset=[" + contentCharSet + "]");
            // Get the outputstream
            output = response.getOutputStream();
			// 缓存输出流
			buffOutput = new BufferedOutputStream(output);
			// 缓存区
			byte[] outBuff = new byte[bufferSize];
			int readLen = 0;
            // Copy input to output
        	LOG.debug("Streaming to output buffer +++ START +++");
        	while ((readLen = inputStream.read(outBuff, 0, bufferSize)) != -1) {
				buffOutput.write(outBuff, 0, readLen);
			}
        	buffOutput.flush();
			LOG.debug("Streaming to output buffer +++ END +++");
        } finally {
        	// Flush
            output.flush();
        	IOUtils.closeQuietly(bufferedOut);
			IOUtils.closeQuietly(output);
        }
    }

}




