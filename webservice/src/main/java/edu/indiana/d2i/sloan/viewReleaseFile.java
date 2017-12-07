package edu.indiana.d2i.sloan;
import edu.indiana.d2i.sloan.bean.ErrorBean;
import edu.indiana.d2i.sloan.bean.ResultBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.ParseException;


/**
 * Deprecated: use DownloadResult API instead
 * param:
 *   resultid: identified of entry in result table
 *
 *   return: create output stream of data field of the given resultid
 */

@Deprecated
@Path("/viewreleasefile")
public class viewReleaseFile {

    private static Logger logger = Logger.getLogger(viewReleaseFile.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response viewReleasedFile(@QueryParam("resultid") String resultid,
                                    @Context HttpHeaders httpHeaders,
                                    @Context HttpServletRequest httpServletRequest) throws SQLException, NoItemIsFoundInDBException, ParseException, IOException {

        if(resultid == null) {
            return Response.status(204).entity(new ErrorBean(204, "Resultid is null!")).build();
        }

        try {
            ResultBean res = DBOperations.getInstance().viewRleaseFile(resultid);

            InputStream in = res.getInputstream();
            BufferedReader br = null;
            StringBuilder sb = new StringBuilder();
            String line;
            br = new BufferedReader(new InputStreamReader(in));
            while((line = br.readLine())!=null){
                sb.append(line);
            }
            return Response.status(200).entity(sb.toString()).build();
            } catch (SQLException e) {
                e.printStackTrace();
                logger.error(e.getMessage(), e);
                return Response.status(500)
                    .entity(new ErrorBean(500, e.getMessage())).build();
            }
    }
}
