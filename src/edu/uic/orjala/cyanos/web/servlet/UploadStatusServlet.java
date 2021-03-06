package edu.uic.orjala.cyanos.web.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import edu.uic.orjala.cyanos.DataException;
import edu.uic.orjala.cyanos.web.Job;
import edu.uic.orjala.cyanos.web.JobManager;
import edu.uic.orjala.cyanos.web.UploadForm;
import edu.uic.orjala.cyanos.web.listener.CyanosSessionListener;
import edu.uic.orjala.cyanos.web.upload.UploadJob;

/**
 * Servlet implementation class UploadStatusServlet
 */
public class UploadStatusServlet extends UploadServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}
	
	public void handleRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		// If status request. Send the status of the current job.
		PrintWriter out = res.getWriter();
		HttpSession thisSession = req.getSession();


		if ( req.getParameter("results") != null ) {
			UploadJob job = getUploadJob(thisSession);
			if ( job.getOutput() != null ) {
				res.setContentType("text/plain");
				out.println(job.getOutput());
				out.close();
				return;
			}
		} else if ( req.getParameter("sheet") != null ) {
			RequestDispatcher disp = this.getServletContext().getRequestDispatcher("/upload/sheet.jsp");
			disp.forward(req, res);
		} else if ( req.getParameter("jobid") != null ) {
			res.setContentType("text/plain");
			JobManager manager = CyanosSessionListener.getJobManager(req);
			Job job = manager.getJob(req.getParameter("jobid"));
			try { 
				if ( job == null ) { job = Job.loadJob(getSQLData(req), req.getParameter("jobid")); }
			} catch (DataException | SQLException e) {
				throw new ServletException(e);
			}
			if ( job != null ) {
				if ( req.getServletPath().equals("/upload/results") ) {
					if ( job.getOutput() != null )
						out.print(job.getOutput());
				} else 
					out.print(getStatus(job));
			}
			out.close();
			return;
		} else {
			res.setContentType("text/plain");
			Job job = UploadServlet.getUploadJob(thisSession);
		
			if ( job != null ) {
				if ( req.getServletPath().equals("/upload/results") ) {
					if ( job.getOutput() != null )
						out.println(job.getOutput());
				} else 
					out.print(getStatus(job));				
			} else {
				UploadForm myForm = (UploadForm) thisSession.getAttribute(UPLOAD_FORM);	
				out.print(getStatus(myForm));
			}			
		}
		out.close();
		return;	
	}
	
	private static String getStatus(UploadForm form) {
		if ( form == null ) {
			return "ERROR";
		} else if ( form.isDone() ) {
			return "DONE";
		} else if ( form.isWorking()) {
			return String.format("%.0f", form.status() * 100);
		} else {
			return "STOP";
		}		
	}

	private static String getStatus(Job job) {
		if ( job == null ) {
			return "ERROR";
		} else if ( job.isDone() ) {
			return "DONE";
		} else if ( job.isWorking()) {
			return String.format("%.0f", job.getProgress() * 100);
		} else {
			return "STOP";
		}		
	}

}
