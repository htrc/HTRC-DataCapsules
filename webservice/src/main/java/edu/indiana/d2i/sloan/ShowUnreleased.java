package edu.indiana.d2i.sloan;

import edu.indiana.d2i.sloan.bean.*;
import edu.indiana.d2i.sloan.db.*;
import edu.indiana.d2i.sloan.exception.*;
import edu.indiana.d2i.sloan.exception.ResultExpireException;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ruili on 2/25/17.
 */

@Path("/showunreleased")
public class ShowUnreleased {

    private static Logger logger = Logger.getLogger(ShowReleased.class);


    @GET
    @Produces(MediaType.APPLICATION_JSON)

    public Response getResourcePost(@Context HttpHeaders httpHeaders,
                                    @Context HttpServletRequest httpServletRequest) {
        String userName = httpServletRequest.getHeader(Constants.USER_NAME);

        try {
            List<ResultInfoBean> res = DBOperations.getInstance().getUnreleased();
            //have res for return
            return Response.status(200).entity(new ResultInfoResponseBean(res)).build();

            } catch (SQLException e) {
                e.printStackTrace();
                logger.error(e.getMessage(), e);
                return Response.status(500)
                        .entity(new ErrorBean(500, e.getMessage())).build();
            } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Response.status(500)
                    .entity(new ErrorBean(500, e.getMessage())).build();
        }

    }



}

