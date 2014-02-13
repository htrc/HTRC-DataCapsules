package edu.indiana.d2i.sloan;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.bean.ErrorBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;

@Path("/download")
public class DownloadResult {
	private static Logger logger = Logger.getLogger(DownloadResult.class);
	
	private class ResultOutputStream implements StreamingOutput {
		private final InputStream input;
		
		public ResultOutputStream(InputStream input) {
			this.input = input;
		}
		
		@Override
		public void write(OutputStream output) throws IOException,
				WebApplicationException {
			byte[] buf = new byte[1024];
			int length = 0;
			while ((length = input.read(buf)) != -1) {
				output.write(buf, 0, length);
			}
		}
	}
	
	
	@GET
	public Response getResourcePost(
		@QueryParam("randomid") String randomid,
		@Context HttpHeaders httpHeaders,
		@Context HttpServletRequest httpServletRequest) {
		if (randomid == null) {
			return Response.status(400)
				.entity(new ErrorBean(400, "Invalid download URL!")).build();
		}		
		
		try {
			InputStream input = DBOperations.getInstance().getResult(randomid);
			logger.info("Result with " + randomid + " is being downloaded.");
			
			return Response.ok(new ResultOutputStream(input)).
				header("Content-Type", "application/zip").
				header("Content-Disposition", "filename=\"result.zip\"").build();
		} catch (NoItemIsFoundInDBException e) {
			logger.error("Invalid " + randomid + " attempts", e);
			return Response.status(400)
				.entity(new ErrorBean(400, "Invalid download URL!")).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500)
					.entity(new ErrorBean(500, e.getMessage())).build();
		}
	}
}
