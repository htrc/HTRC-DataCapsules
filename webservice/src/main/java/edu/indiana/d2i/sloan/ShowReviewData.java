package edu.indiana.d2i.sloan;

import edu.indiana.d2i.sloan.Constants;
import edu.indiana.d2i.sloan.ShowReleased;
import edu.indiana.d2i.sloan.bean.*;
import edu.indiana.d2i.sloan.db.DBOperations;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by ruili on 4/12/17.
 */

@Path("/showreviewdata")
public class ShowReviewData {
    private static Logger logger = Logger.getLogger(ShowReviewData.class);


    @GET
    @Produces(MediaType.APPLICATION_JSON)

    public Response getResourcePost(@Context HttpHeaders httpHeaders,
                                    @Context HttpServletRequest httpServletRequest) throws SQLException {
        String userName = httpServletRequest.getHeader(Constants.USER_NAME);


        try {

            List<ReviewInfoBean> res = DBOperations.getInstance().getReviewData();
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