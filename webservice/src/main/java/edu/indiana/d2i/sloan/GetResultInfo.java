package edu.indiana.d2i.sloan;


import edu.indiana.d2i.sloan.bean.ErrorBean;
import edu.indiana.d2i.sloan.bean.ResultInfoBean;
import edu.indiana.d2i.sloan.bean.ResultInfoResponseBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ruili on 5/29/17.
 */

@Path("/getresultinfo")
public class GetResultInfo{
    private static Logger logger = Logger.getLogger(ShowReleased.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResourcePost(@QueryParam("resultid") String resultid,
                                    @Context HttpHeaders httpHeaders,
                                    @Context HttpServletRequest httpServletRequest) throws SQLException, NoItemIsFoundInDBException, ParseException, IOException {
        String userName = httpServletRequest.getHeader(Constants.USER_NAME);
        //String resultid = httpServletRequest.getHeader("resultid");

        if(resultid == null) {
            return Response.status(400).entity(new ErrorBean(400, "This result does not exist!")).build();
        }

        try {

            List<ResultInfoBean> res = new ArrayList<ResultInfoBean>();
            res.add(DBOperations.getInstance().getResultInfo(resultid));
            return Response.status(200).entity(new ResultInfoResponseBean(res)).build();

        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
            return Response.status(500)
                    .entity(new ErrorBean(500, e.getMessage())).build();
        }
    }
}
