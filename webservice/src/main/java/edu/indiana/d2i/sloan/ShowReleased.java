package edu.indiana.d2i.sloan;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import edu.indiana.d2i.sloan.bean.*;
import edu.indiana.d2i.sloan.db.*;
import edu.indiana.d2i.sloan.exception.*;
import edu.indiana.d2i.sloan.exception.ResultExpireException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * param: null
 *
 * return: All resultids and their correspondent information which has been released(Released)
 */

@Path("/showreleased")
public class ShowReleased {

    private static Logger logger = LoggerFactory.getLogger(ShowReleased.class);


    @GET
    @Produces(MediaType.APPLICATION_JSON)

    public Response showReleased(@Context HttpHeaders httpHeaders,
                                    @Context HttpServletRequest httpServletRequest) throws SQLException {

        try {

            List<ReviewInfoBean> res = DBOperations.getInstance().getReleased();
            //have res for return
            return Response.status(200).entity(new ReviewInfoResponseBean(res)).build();

        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
            return Response.status(500)
                    .entity(new ErrorBean(500, e.getMessage())).build();
        }




    }

}