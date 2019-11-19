package edu.indiana.d2i.sloan;

import edu.indiana.d2i.sloan.bean.ErrorBean;
import edu.indiana.d2i.sloan.bean.ResultInfoBean;
import edu.indiana.d2i.sloan.bean.ReviewInfoBean;
import edu.indiana.d2i.sloan.bean.VmUserRole;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.utils.EmailUtil;
import edu.indiana.d2i.sloan.utils.ResultUtils;
import edu.indiana.d2i.sloan.utils.RolePermissionUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

/**
 * param:
 *   resultid: identifier of the record to be deleted
*/

@Path("/deleteresult")
public class DeleteResult {
    private static Logger logger = Logger.getLogger(DeleteResult.class);

    @DELETE
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteResult(
            @FormParam("resultid") String resultid,
            @Context HttpHeaders httpHeaders,
            @Context HttpServletRequest httpServletRequest) {

        if (resultid == null) {
            logger.error("result id is null!");
            return Response.status(400).entity(new ErrorBean(400, "Resultid is null")).build();
        }

        try {
            ResultInfoBean resultInfoBean = DBOperations.getInstance().getResultInfo(resultid);
            if(resultInfoBean.getStatus().equals("Pending")) {
                return Response.status(400).entity(
                        new ErrorBean(400, "Cannot delete results in Pending status!")).build();
            }

            //move result file to backup directory
            ResultUtils.backupResultFile(resultid);
            //update the DB
            DBOperations.getInstance().updateResultAsDeleted(resultid);
            return Response.status(200).entity("Result with " + resultid + " successfully deleted!").build();
        } catch (NoItemIsFoundInDBException e) {
            logger.error("No Result with id " + resultid + " is found!", e);
            return Response.status(404).entity("No Result with id " + resultid + " is found!").build();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            return Response.status(500).
                    entity(new ErrorBean(500, "Internal error - " + e.getMessage())).build();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Response.status(500).entity(new ErrorBean(500, e.getMessage())).build();
        }

    }

}