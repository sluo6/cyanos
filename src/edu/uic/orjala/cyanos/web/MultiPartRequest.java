/**
 * 
 */
package edu.uic.orjala.cyanos.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;

/**
 * HttpServletRequestWrapper designed to handle multipart requests <code>enctype="multipart/form-data"</code>.
 * This encoding type is used to allow upload of files and is not parsed by Tomcat v5.5.
 * 
 * @author George Chlipala
 *
 */
public class MultiPartRequest extends HttpServletRequestWrapper {
	
	private static final String ATTR_PARAMETERS = "multipart-params";
	private static final String ATTR_UPLOADS = "multipart-files";

	private Map<String, String[]> formValues = null;
	private Map<String, List<FileUpload>> uploadItems = null;

	/**
	 * Will check if the request is a multipart request and will create a new MultiPartRequest object to wrap the request.
	 * Otherwise will return the original request object unmodified.
	 * 
	 * @param request HttpServletRequest object
	 * @return HttpServletRequest object (MultiPartRequest object if a multipart request)
	 * @throws ServletException
	 * @throws IOException
	 */
	public static HttpServletRequest parseRequest(HttpServletRequest request) throws ServletException, IOException {
		if ( request instanceof MultiPartRequest ) 
			return request;
		if ( ServletFileUpload.isMultipartContent(request)) 
			return new MultiPartRequest(request);
		else
			return request;
	}
	
	public static MultiPartRequest genRequest(HttpServletRequest request) throws ServletException, IOException {
		if ( request instanceof MultiPartRequest ) 
			return (MultiPartRequest) request;
		else 
			return new MultiPartRequest(request);		
	}

	/**
	 * @param request
	 * @throws IOException 
	 * @throws ServletException 
	 */
	protected MultiPartRequest(HttpServletRequest request) throws ServletException, IOException {
		super(request);
		if ( ServletFileUpload.isMultipartContent(request)) {
			if ( request.getAttribute(ATTR_PARAMETERS) != null && request.getAttribute(ATTR_UPLOADS) != null ) {
				this.formValues = (Map<String, String[]>) request.getAttribute(ATTR_PARAMETERS);
				this.uploadItems = (Map<String, List<FileUpload>>) request.getAttribute(ATTR_UPLOADS);
			} else
				this.parseMultipartReq(request);
		} else {
			this.formValues = request.getParameterMap();
		}
	}

	protected void parseMultipartReq(HttpServletRequest req) throws ServletException, IOException {		
		this.formValues = new Hashtable<String, String[]>();
		this.uploadItems = new Hashtable<String, List<FileUpload>>();
		try {
			ServletFileUpload upload = new ServletFileUpload();
			FileItemIterator iter = upload.getItemIterator(req);
			while (iter.hasNext() ) {
				FileItemStream anItem = iter.next();
				String thisField = anItem.getFieldName();
				if ( anItem.isFormField()) {
					if ( formValues.containsKey(thisField) ) {
						String[] vals = formValues.get(thisField);
						int next = vals.length;
						vals[next] = Streams.asString(anItem.openStream());
						formValues.put(thisField, vals);
					} else {
						String[] vals = { Streams.asString(anItem.openStream()) };
						formValues.put(thisField, vals);
					}
				} else {
					FileUpload file = new FileUpload(anItem);
					if ( file.getStream().available() > 0 ) {
						if ( ! uploadItems.containsKey(thisField) ) {
							uploadItems.put(thisField, new ArrayList<FileUpload>());
						}
						uploadItems.get(thisField).add(file);
					}
				}
			}
			this.setAttribute(ATTR_PARAMETERS, this.formValues);
			this.setAttribute(ATTR_UPLOADS, this.uploadItems);
		} catch (FileUploadException e) {
			throw new ServletException("COULD NOT PARSE UPLOAD", e);
		}
	}

	/**
	 * Get a FileUpload for the parameter name 
	 * 
	 * @param param
	 * @return
	 */
	public FileUpload getUpload(String param) {
		return this.getUpload(param, 0);
	}

	/**
	 * Get a FileUpload for the parameter name
	 * 
	 * @param param
	 * @param index
	 * @return
	 */
	public FileUpload getUpload(String param, int index) {
		if ( this.uploadItems != null && this.uploadItems.containsKey(param) )
			return (uploadItems.get(param)).get(index);
		else 
			return null;
	}

	public int getUploadCount(String param) {
		if ( this.uploadItems != null && this.uploadItems.containsKey(param) )
			return this.uploadItems.get(param).size();
		else
			return 0;
	}

	public List<FileUpload> getUploads(String param) {
		if ( this.uploadItems != null && this.uploadItems.containsKey(param))
			return this.uploadItems.get(param);
		else
			return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequestWrapper#getParameter(java.lang.String)
	 */
	@Override
	public String getParameter(String name) {
		String[] values = this.getParameterValues(name);
		if ( values != null ) 
			return values[0];
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequestWrapper#getParameterMap()
	 */
	@Override
	public Map<String, String[]> getParameterMap() {
		return this.formValues;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequestWrapper#getParameterNames()
	 */
	@Override
	public Enumeration<String> getParameterNames() {
		if ( formValues instanceof Hashtable )
			return ((Hashtable<String, String[]>)formValues).keys();
		else {
			Vector<String> keyVec = new Vector<String>(this.formValues.keySet());
			return keyVec.elements();
		}
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequestWrapper#getParameterValues(java.lang.String)
	 */
	@Override
	public String[] getParameterValues(String name) {
		return this.formValues.get(name);
	}


}
