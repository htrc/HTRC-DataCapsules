/*******************************************************************************
 * Copyright 2014 The Trustees of Indiana University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package edu.indiana.d2i.sloan;

import edu.indiana.d2i.sloan.bean.ErrorBean;
import edu.indiana.d2i.sloan.bean.ResultBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.exception.ResultExpireException;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.*;
import java.sql.SQLException;

/**
 * param:
 *   resultid: identifier of a specific entry
 *
 *
 *   return: data field of the given resultid
 */

@Path("/download")
public class DownloadResult {
	private static Logger logger = Logger.getLogger(DownloadResult.class);
	
	private class ResultOutputStream implements StreamingOutput {
		private final InputStream input;
		
		public ResultOutputStream(InputStream input) {
			this.input = input;
		}
		
		@Override
		public void write(OutputStream output) throws IOException, WebApplicationException
		{
			byte[] buf = new byte[1024];
			int length = 0;
			while ((length = input.read(buf)) != -1) {
				output.write(buf, 0, length);
			}
		}
	}
	
	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)ra
	public Response getResourcePost(
		@QueryParam("randomid") String randomid,
		@QueryParam("filename") String filename,
		@Context HttpHeaders httpHeaders,
		@Context HttpServletRequest httpServletRequest)
	{


		if (randomid == null) {
			return Response.status(400)
				.entity(new ErrorBean(400, "Invalid download URL!")).build();
		}


		try {
			ResultBean result = DBOperations.getInstance().getResult(randomid);

			logger.info("Result with " + randomid + " is being downloaded.");
			
			// check if result expires

			long currentT = new java.util.Date().getTime();
			long startT = result.getStartdate().getTime();
			long span = Configuration.getInstance().getLong(
				Configuration.PropertyName.RESULT_EXPIRE_IN_SECOND);
			if (span > 0 && ((currentT-startT)/1000) > span) 
				throw new ResultExpireException(randomid + " expires!");			


			writeFile(randomid, fetchData(randomid),"default");


			return Response.status(200).entity(new ResultOutputStream(result.getInputstream())).build();

			//	header("Content-Type", "application/zip").
			//	header("Content-Disposition", String.format("filename=\"%s.zip\"", filename)).build();


		} catch (NoItemIsFoundInDBException e) {
			logger.error("Invalid " + randomid + " attempts", e);
			return Response.status(404).entity("Invalid download URL!").build();
		} catch (ResultExpireException e) {
			logger.error(e.getMessage(), e);
			return Response.status(404).entity("Result expires!").build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500).entity("Internal error.").build();
		}
	}


	//fetch content from db to write into output file
	public String fetchData(String resultid)
			throws java.text.ParseException, SQLException, NoItemIsFoundInDBException, IOException
	{

		ResultBean result =  DBOperations.getInstance().getResult(resultid);

		InputStream dataField = result.getInputstream();
		char[] buffer = new char[1024];

		StringBuilder out = new StringBuilder();
		Reader in = new InputStreamReader(dataField, "UTF-8");
		int l= in.read(buffer,0,buffer.length);
		while(l >= 0){
			out.append(buffer,0,l);
			l=in.read(buffer, 0, l);
		}

		//Output target path?
		return out.toString();
	}

	public void writeFile(String resultid, String content,
						  String destination	/*default*/)
	{



		String filename = String.format("resuld", resultid, ".txt");
		try {
			PrintWriter out = new PrintWriter(filename);
			out.println(content);

		} catch (FileNotFoundException e) {
			logger.error(e.getMessage(),e);
		}
	}




}
